// SPDX-License-Identifier: Apache-2.0
import { createHash } from "node:crypto";
import { createWriteStream } from "node:fs";
import { mkdtemp } from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import { PNG } from "pngjs";
import yazl from "yazl";

export type FixtureOptions = {
  tamperReport?: boolean;
  omitReportEntry?: boolean;
  extraEntry?: string;
  image?: boolean;
  reportDescription?: string;
  events?: unknown[];
  semantics?: unknown;
};

export async function tempDirectory(prefix = "reproparcel-"): Promise<string> {
  return mkdtemp(path.join(os.tmpdir(), prefix));
}

export function makePng(width = 10, height = 10): Buffer {
  const png = new PNG({ width, height });
  for (let i = 0; i < png.data.length; i += 4) {
    png.data[i] = 255;
    png.data[i + 1] = 255;
    png.data[i + 2] = 255;
    png.data[i + 3] = 255;
  }
  return PNG.sync.write(png);
}

function sha256(buffer: Buffer): string {
  return createHash("sha256").update(buffer).digest("hex");
}

export async function createParcel(directory: string, options: FixtureOptions = {}): Promise<string> {
  const report = {
    title: "Synthetic report",
    description: options.reportDescription ?? "Synthetic description <script>alert(1)</script>",
    expectedBehavior: "Expected",
    actualBehavior: "Actual",
    reproSteps: "Synthetic steps",
    screenId: "synthetic_screen",
    evidence: [{ state: "collected" }],
  };
  const events = options.events ?? [
    {
      sequence: 0,
      offsetMs: 0,
      category: "tap",
      actionId: "save",
      screenId: "synthetic_screen",
      attributes: { result: "no_effect" },
      outcome: "unknown",
    },
  ];
  const semantics = options.semantics ?? {
    nodes: [
      {
        nodeId: "n1",
        role: "screen",
        label: "Synthetic",
        bounds: { left: 0, top: 0, right: 100, bottom: 100 },
      },
    ],
    truncated: false,
  };
  const reportBytes = Buffer.from(JSON.stringify(report), "utf8");
  const eventBytes = Buffer.from(JSON.stringify(events), "utf8");
  const semanticBytes = Buffer.from(JSON.stringify(semantics), "utf8");
  const imageBytes = options.image === false ? null : makePng();

  const entries: Array<{ path: string; mediaType: string; bytes: Buffer }> = [
    { path: "report.json", mediaType: "application/json", bytes: reportBytes },
    { path: "events.json", mediaType: "application/json", bytes: eventBytes },
    { path: "semantics.json", mediaType: "application/json", bytes: semanticBytes },
  ];
  if (imageBytes) entries.push({ path: "attachments/screen.png", mediaType: "image/png", bytes: imageBytes });

  const manifestEntries = entries
    .filter((entry) => !(options.omitReportEntry && entry.path === "report.json"))
    .map((entry) => ({
      path: entry.path,
      mediaType: entry.mediaType,
      uncompressedSize: entry.bytes.length,
      sha256: options.tamperReport && entry.path === "report.json" ? "0".repeat(64) : sha256(entry.bytes),
    }));

  const manifest = {
    schemaVersion: 1,
    reportId: "rpt_fixture_0001",
    createdAt: "2026-11-11T00:00:00Z",
    producer: { sdkVersion: "0.0.1-SNAPSHOT", platform: "android", collectionConfigurationId: "fixture" },
    app: { packageName: "com.example.synthetic", versionName: "1.0.0", versionCode: 1, variant: "debug" },
    device: {
      apiLevel: 30,
      manufacturer: "Synthetic",
      model: "Fixture",
      windowWidthPx: 100,
      windowHeightPx: 100,
      densityDpi: 420,
      locale: "en-US",
      fontScale: 1,
      orientation: "portrait",
    },
    privacy: { policyVersion: "fixture-v1", redactionMode: "opaque" },
    capture: { capturedAt: "2026-11-11T00:00:01Z", collectionModes: ["manual"], providers: [] },
    entries: manifestEntries,
    limits: {
      maxCompressedBytes: 5242880,
      maxUncompressedBytes: 10485760,
      maxEntries: 16,
      maxCompressionRatio: 100,
      maxImageLongEdgePx: 1600,
      maxDecodedImageBytes: 12582912,
      maxConcurrentDecodedImageBytes: 25165824,
      maxJsonBytes: 2097152,
      maxJsonDepth: 32,
      maxJsonNodes: 10000,
    },
  };

  const file = path.join(directory, "fixture.reproparcel");
  const zip = new yazl.ZipFile();
  zip.addBuffer(Buffer.from(JSON.stringify(manifest), "utf8"), "manifest.json");
  for (const entry of entries) {
    const bytes =
      options.tamperReport && entry.path === "report.json"
        ? Buffer.from(JSON.stringify(report).replace("Synthetic", "Tampered!"), "utf8")
        : entry.bytes;
    zip.addBuffer(bytes, entry.path);
  }
  if (options.extraEntry) zip.addBuffer(Buffer.from("extra"), options.extraEntry);

  await new Promise<void>((resolve, reject) => {
    const output = createWriteStream(file);
    output.on("close", () => resolve());
    output.on("error", reject);
    zip.outputStream.pipe(output);
    zip.end();
  });
  return file;
}
