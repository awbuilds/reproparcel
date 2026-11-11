// SPDX-License-Identifier: AGPL-3.0-only
import { createHash } from "node:crypto";
import { existsSync } from "node:fs";
import { readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { Ajv2020 } from "ajv/dist/2020.js";
import addFormatsModule from "ajv-formats";
import { PNG } from "pngjs";
import yauzl, { type Entry, type ZipFile } from "yauzl";

export const LIMITS = {
  maxCompressedBytes: 5 * 1024 * 1024,
  maxUncompressedBytes: 10 * 1024 * 1024,
  maxEntries: 16,
  maxCompressionRatio: 100,
  maxJsonBytes: 2 * 1024 * 1024,
  maxJsonDepth: 32,
  maxJsonNodes: 10_000,
  maxImageLongEdge: 1600,
  maxDecodedImageBytes: 12 * 1024 * 1024,
  maxConcurrentDecodedImageBytes: 24 * 1024 * 1024,
} as const;

export type Manifest = {
  schemaVersion: number;
  reportId: string;
  createdAt: string;
  producer: { sdkVersion: string; platform: string; collectionConfigurationId: string };
  app: { packageName: string; versionName: string; versionCode: number; variant: string; gitCommit?: string | null };
  device: {
    apiLevel: number;
    manufacturer: string;
    model: string;
    windowWidthPx: number;
    windowHeightPx: number;
    densityDpi: number;
    locale: string;
    fontScale: number;
    orientation: string;
  };
  privacy: { policyVersion: string; redactionMode: string; reviewedCategories?: string[]; previewContentHash?: string | null };
  capture: {
    capturedAt: string;
    collectionModes: string[];
    triggerOffsetMs?: number | null;
    snapshotCutoffMs?: number | null;
    providers?: Array<{ providerId: string; status: { state: string; reason?: string | null; detail?: string | null } }>;
  };
  entries: Array<{ path: string; mediaType: string; uncompressedSize: number; sha256: string }>;
  limits: Record<string, number>;
};

export type Report = {
  title?: string;
  description: string;
  expectedBehavior?: string;
  actualBehavior?: string;
  reproSteps?: string | null;
  screenId?: string | null;
  evidence?: Array<{ state: string; reason?: string | null; detail?: string | null }>;
};

export type ReproEvent = {
  sequence: number;
  offsetMs: number;
  category: string;
  actionId?: string | null;
  screenId?: string | null;
  attributes?: Record<string, string>;
  outcome?: string | null;
};

export type Semantics = {
  nodes: Array<{
    nodeId: string;
    role: string;
    label?: string | null;
    testTag?: string | null;
    bounds: { left: number; top: number; right: number; bottom: number; coordinateSpace?: string };
    state?: Record<string, string>;
    parentId?: string | null;
    sensitive?: boolean;
  }>;
  truncated?: boolean;
  omission?: string | null;
};

export type ParcelData = {
  manifest: Manifest;
  report: Report;
  events: ReproEvent[];
  semantics: Semantics | null;
  imagePng: Buffer | null;
  imageWidth: number;
  imageHeight: number;
  entries: Map<string, Buffer>;
};

export class ParcelError extends Error {
  constructor(
    public readonly code: string,
    message: string,
    public readonly path?: string,
  ) {
    super(message);
    this.name = "ParcelError";
  }
}

const ALLOWED_PATHS = /^(manifest\.json|report\.json|events\.json|semantics\.json|attachments\/[A-Za-z0-9._-]+\.png)$/;
const SHA256 = /^[0-9a-f]{64}$/;
const isSymlink = (entry: Entry): boolean => {
  const mode = (entry.externalFileAttributes >>> 16) & 0xf000;
  return mode === 0xa000;
};

function openZip(file: string): Promise<ZipFile> {
  return new Promise((resolve, reject) => {
    yauzl.open(file, { lazyEntries: true, autoClose: true }, (error, zip) => {
      if (error || !zip) reject(error ?? new ParcelError("ZIP_OPEN_FAILED", "Unable to open ZIP"));
      else resolve(zip);
    });
  });
}

function openEntry(zip: ZipFile, entry: Entry): Promise<Buffer> {
  return new Promise((resolve, reject) => {
    zip.openReadStream(entry, (error, stream) => {
      if (error || !stream) {
        reject(error ?? new ParcelError("ENTRY_READ_FAILED", "Unable to read entry", entry.fileName));
        return;
      }
      const chunks: Buffer[] = [];
      let total = 0;
      stream.on("data", (chunk: Buffer) => {
        total += chunk.length;
        if (total > LIMITS.maxUncompressedBytes || total > entry.uncompressedSize + 1) {
          stream.destroy(new ParcelError("ENTRY_TOO_LARGE", "Entry exceeds the size budget", entry.fileName));
          return;
        }
        chunks.push(chunk);
      });
      stream.on("error", reject);
      stream.on("end", () => resolve(Buffer.concat(chunks)));
    });
  });
}

export function jsonBudget(text: string): { bytes: number; depth: number; nodes: number; ok: boolean } {
  const bytes = Buffer.byteLength(text, "utf8");
  let depth = 0;
  let maxDepth = 0;
  let nodes = 0;
  let inString = false;
  let escaped = false;
  for (const ch of text) {
    if (inString) {
      if (escaped) escaped = false;
      else if (ch === "\\") escaped = true;
      else if (ch === '"') inString = false;
      continue;
    }
    if (ch === '"') inString = true;
    else if (ch === "{" || ch === "[") {
      depth += 1;
      nodes += 1;
      if (depth > maxDepth) maxDepth = depth;
    } else if (ch === "}" || ch === "]") {
      if (depth > 0) depth -= 1;
    }
  }
  return {
    bytes,
    depth: maxDepth,
    nodes,
    ok: bytes <= LIMITS.maxJsonBytes && maxDepth <= LIMITS.maxJsonDepth && nodes <= LIMITS.maxJsonNodes,
  };
}

function parseJson<T>(buffer: Buffer, name: string): T {
  const text = buffer.toString("utf8");
  const budget = jsonBudget(text);
  if (!budget.ok) {
    throw new ParcelError("JSON_LIMIT", `JSON exceeds the parser budget (${name})`, name);
  }
  try {
    return JSON.parse(text) as T;
  } catch {
    throw new ParcelError("JSON_PARSE", `Invalid JSON (${name})`, name);
  }
}

function schemaRoot(): string {
  if (process.env.REPROPARCEL_SCHEMA_ROOT) return process.env.REPROPARCEL_SCHEMA_ROOT;
  const candidates = [
    path.resolve(process.cwd(), "schema/v1"),
    path.resolve(process.cwd(), "../schema/v1"),
    fileURLToPath(new URL("../../schema/v1", import.meta.url)),
  ];
  return candidates.find((candidate) => existsSync(candidate)) ?? candidates[0]!;
}

const ajv = new Ajv2020({ allErrors: true, strict: false });
const addFormats = addFormatsModule as unknown as (instance: Ajv2020) => Ajv2020;
addFormats(ajv);
let schemasPromise: Promise<Map<string, ReturnType<typeof ajv.compile>>> | null = null;

async function schemas(): Promise<Map<string, ReturnType<typeof ajv.compile>>> {
  if (!schemasPromise) {
    schemasPromise = (async () => {
      const root = schemaRoot();
      const result = new Map<string, ReturnType<typeof ajv.compile>>();
      for (const name of ["manifest.schema.json", "report.schema.json", "events.schema.json", "semantics.schema.json"]) {
        const text = await readFile(path.join(root, name), "utf8");
        result.set(name, ajv.compile(JSON.parse(text)));
      }
      return result;
    })();
  }
  return schemasPromise;
}

function assertSchema(name: string, value: unknown, compiled: Map<string, ReturnType<typeof ajv.compile>>): void {
  const validator = compiled.get(name);
  if (!validator) throw new ParcelError("SCHEMA_UNAVAILABLE", `Missing schema ${name}`);
  if (!validator(value)) {
    const details = (validator.errors ?? []).map((e: { instancePath: string; message?: string }) => `${e.instancePath} ${e.message}`).join("; ");
    throw new ParcelError("SCHEMA_VALIDATION", `Schema validation failed: ${details}`, name);
  }
}

export async function readParcel(file: string): Promise<ParcelData> {
  const zip = await openZip(file);
  const entries = new Map<string, Buffer>();
  const seen = new Set<string>();
  let compressedTotal = 0;
  let uncompressedTotal = 0;
  let entryCount = 0;

  await new Promise<void>((resolve, reject) => {
    zip.on("entry", async (entry: Entry) => {
      try {
        entryCount += 1;
        if (entryCount > LIMITS.maxEntries) throw new ParcelError("TOO_MANY_ENTRIES", "Too many ZIP entries");
        if (entry.fileName.endsWith("/")) throw new ParcelError("UNSUPPORTED_ENTRY", "Directories are not supported", entry.fileName);
        if (isSymlink(entry)) throw new ParcelError("SYMLINK", "Symlinks are rejected", entry.fileName);
        if (entry.isEncrypted()) throw new ParcelError("ENCRYPTED_ENTRY", "Encrypted entries are rejected", entry.fileName);
        if (
          entry.fileName.startsWith("/") ||
          entry.fileName.includes("\\") ||
          entry.fileName.includes("..") ||
          entry.fileName.includes("\0") ||
          !ALLOWED_PATHS.test(entry.fileName)
        ) {
          throw new ParcelError("INVALID_PATH", "Entry path is not allowlisted", entry.fileName);
        }
        if (seen.has(entry.fileName)) throw new ParcelError("DUPLICATE_ENTRY", "Duplicate ZIP entry", entry.fileName);
        seen.add(entry.fileName);
        compressedTotal += entry.compressedSize;
        uncompressedTotal += entry.uncompressedSize;
        if (compressedTotal > LIMITS.maxCompressedBytes || uncompressedTotal > LIMITS.maxUncompressedBytes) {
          throw new ParcelError("PARCEL_TOO_LARGE", "Parcel exceeds the compressed/uncompressed budget");
        }
        if (entry.compressedSize === 0 || entry.uncompressedSize / entry.compressedSize > LIMITS.maxCompressionRatio) {
          throw new ParcelError("COMPRESSION_BOMB", "Entry exceeds the compression ratio budget", entry.fileName);
        }
        entries.set(entry.fileName, await openEntry(zip, entry));
        zip.readEntry();
      } catch (error) {
        zip.close();
        reject(error);
      }
    });
    zip.on("end", () => resolve());
    zip.on("error", reject);
    zip.readEntry();
  });

  const manifestBuffer = entries.get("manifest.json");
  if (!manifestBuffer) throw new ParcelError("MISSING_MANIFEST", "manifest.json is missing");
  const manifest = parseJson<Manifest>(manifestBuffer, "manifest.json");
  const reportBuffer = entries.get("report.json");
  if (!reportBuffer) throw new ParcelError("MISSING_REPORT", "report.json is missing");
  const report = parseJson<Report>(reportBuffer, "report.json");
  const events = entries.has("events.json") ? parseJson<ReproEvent[]>(entries.get("events.json")!, "events.json") : [];
  const semantics = entries.has("semantics.json")
    ? parseJson<Semantics>(entries.get("semantics.json")!, "semantics.json")
    : null;

  const compiled = await schemas();
  assertSchema("manifest.schema.json", manifest, compiled);
  assertSchema("report.schema.json", report, compiled);
  assertSchema("events.schema.json", events, compiled);
  if (semantics) assertSchema("semantics.schema.json", semantics, compiled);

  if (manifest.schemaVersion !== 1) {
    throw new ParcelError("UNSUPPORTED_SCHEMA", `Unsupported schemaVersion ${manifest.schemaVersion}`);
  }
  if (manifest.entries.length + 1 > LIMITS.maxEntries) throw new ParcelError("TOO_MANY_ENTRIES", "Manifest references too many entries");
  const referenced = new Set(manifest.entries.map((e) => e.path));
  for (const name of entries.keys()) {
    if (name === "manifest.json") continue;
    if (!referenced.has(name)) throw new ParcelError("UNREFERENCED_ENTRY", "Entry is not referenced by the manifest", name);
  }
  for (const expected of manifest.entries) {
    if (!ALLOWED_PATHS.test(expected.path)) throw new ParcelError("INVALID_PATH", "Manifest path is not allowlisted", expected.path);
    if (!SHA256.test(expected.sha256)) throw new ParcelError("INVALID_CHECKSUM", "Invalid SHA-256 format", expected.path);
    if (!["application/json", "image/png"].includes(expected.mediaType)) {
      throw new ParcelError("UNSUPPORTED_MEDIA_TYPE", "Unsupported media type", expected.path);
    }
    const actual = entries.get(expected.path);
    if (!actual) throw new ParcelError("MISSING_ENTRY", "Referenced entry is missing", expected.path);
    if (actual.length !== expected.uncompressedSize) {
      throw new ParcelError("SIZE_MISMATCH", "Entry size does not match the manifest", expected.path);
    }
    const digest = createHash("sha256").update(actual).digest("hex");
    if (digest !== expected.sha256) throw new ParcelError("CHECKSUM_MISMATCH", "Entry checksum mismatch", expected.path);
  }

  const image = entries.get("attachments/screen.png") ?? null;
  let imageWidth = 0;
  let imageHeight = 0;
  let sanitizedImage: Buffer | null = null;
  if (image) {
    const parsed = parsePngDimensions(image);
    imageWidth = parsed.width;
    imageHeight = parsed.height;
    sanitizedImage = sanitizePng(image, parsed.width, parsed.height);
  }

  return {
    manifest,
    report,
    events,
    semantics,
    imagePng: sanitizedImage,
    imageWidth,
    imageHeight,
    entries,
  };
}

export function parsePngDimensions(buffer: Buffer): { width: number; height: number } {
  if (buffer.length < 24 || buffer.subarray(0, 8).toString("hex") !== "89504e470d0a1a0a") {
    throw new ParcelError("INVALID_PNG", "Invalid PNG signature");
  }
  const width = buffer.readUInt32BE(16);
  const height = buffer.readUInt32BE(20);
  if (width <= 0 || height <= 0 || Math.max(width, height) > LIMITS.maxImageLongEdge) {
    throw new ParcelError("IMAGE_DIMENSIONS", "Image exceeds the longest-edge budget");
  }
  const decoded = width * height * 4;
  if (!Number.isSafeInteger(decoded) || decoded > LIMITS.maxDecodedImageBytes) {
    throw new ParcelError("IMAGE_MEMORY", "Image exceeds the decoded-pixel budget");
  }
  return { width, height };
}

function sanitizePng(buffer: Buffer, width: number, height: number): Buffer {
  let png: PNG;
  try {
    png = PNG.sync.read(buffer);
  } catch {
    throw new ParcelError("INVALID_PNG", "PNG could not be decoded within limits");
  }
  if (png.width !== width || png.height !== height) {
    throw new ParcelError("INVALID_PNG", "PNG dimensions do not match IHDR");
  }
  return PNG.sync.write(png, { colorType: 6 });
}

export function summarize(parcel: ParcelData): Record<string, unknown> {
  return {
    reportId: parcel.manifest.reportId,
    createdAt: parcel.manifest.createdAt,
    app: parcel.manifest.app,
    device: parcel.manifest.device,
    title: parcel.report.title ?? "",
    descriptionLength: parcel.report.description.length,
    events: parcel.events.length,
    semanticNodes: parcel.semantics?.nodes.length ?? 0,
    image: parcel.imagePng ? { width: parcel.imageWidth, height: parcel.imageHeight } : null,
    omissions: parcel.report.evidence ?? [],
  };
}
