// SPDX-License-Identifier: AGPL-3.0-only
import { createHash, createCipheriv, createDecipheriv, randomBytes, timingSafeEqual } from "node:crypto";
import { createServer, type IncomingMessage, type Server, type ServerResponse } from "node:http";
import { DatabaseSync } from "node:sqlite";

const MAX_BODY_BYTES = 5 * 1024 * 1024;
const MAX_REQUEST_PER_MINUTE = 30;

export type ServerOptions = {
  dbPath: string;
  masterKey: Buffer;
  adminToken: string;
  defaultQuotaPerDay?: number;
  now?: () => number;
};

export type IngestResult =
  | { ok: true; reportId: string; duplicate: boolean }
  | { ok: false; status: number; code: string };

type ProjectRow = { id: string; token_hash: string; quota_per_day: number; revoked: number };

function sha256(value: string): string {
  return createHash("sha256").update(value).digest("hex");
}

function safeEqual(left: string, right: string): boolean {
  const a = Buffer.from(left);
  const b = Buffer.from(right);
  return a.length === b.length && timingSafeEqual(a, b);
}

export class RelayStore {
  private readonly db: DatabaseSync;
  private readonly now: () => number;
  private readonly minuteCounts = new Map<string, { minute: number; count: number }>();

  constructor(private readonly options: ServerOptions) {
    if (options.masterKey.length !== 32) throw new Error("master key must be 32 bytes");
    this.now = options.now ?? (() => Date.now());
    this.db = new DatabaseSync(options.dbPath);
    this.db.exec(`
      PRAGMA journal_mode = WAL;
      CREATE TABLE IF NOT EXISTS projects (
        id TEXT PRIMARY KEY,
        token_hash TEXT NOT NULL,
        quota_per_day INTEGER NOT NULL,
        revoked INTEGER NOT NULL DEFAULT 0,
        created_at INTEGER NOT NULL
      );
      CREATE TABLE IF NOT EXISTS reports (
        id TEXT PRIMARY KEY,
        project_id TEXT NOT NULL,
        idempotency_key TEXT NOT NULL,
        nonce BLOB NOT NULL,
        ciphertext BLOB NOT NULL,
        size INTEGER NOT NULL,
        created_at INTEGER NOT NULL,
        UNIQUE(project_id, idempotency_key)
      );
      CREATE TABLE IF NOT EXISTS audit (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        event TEXT NOT NULL,
        project_id TEXT,
        report_id TEXT,
        code TEXT,
        at INTEGER NOT NULL
      );
    `);
  }

  provisionProject(projectId: string, token: string, quotaPerDay = this.options.defaultQuotaPerDay ?? 100): void {
    if (!/^[A-Za-z0-9_.-]{1,64}$/.test(projectId)) throw new Error("invalid project id");
    if (token.length < 32) throw new Error("token too short");
    if (safeEqual(token, this.options.adminToken)) throw new Error("project and admin tokens must differ");
    this.db.prepare(
      "INSERT OR REPLACE INTO projects (id, token_hash, quota_per_day, revoked, created_at) VALUES (?, ?, ?, 0, ?)",
    ).run(projectId, sha256(token), quotaPerDay, this.now());
    this.audit("project_provisioned", projectId, null, null);
  }

  revokeProject(projectId: string): void {
    this.db.prepare("UPDATE projects SET revoked = 1 WHERE id = ?").run(projectId);
    this.audit("project_revoked", projectId, null, null);
  }

  isAuthorized(projectId: string, token: string): ProjectRow | null {
    const row = this.db.prepare("SELECT * FROM projects WHERE id = ?").get(projectId) as ProjectRow | undefined;
    if (!row || row.revoked !== 0 || !safeEqual(row.token_hash, sha256(token))) return null;
    return row;
  }

  ingest(projectId: string, token: string, key: string, bytes: Buffer): IngestResult {
    const project = this.isAuthorized(projectId, token);
    if (!project) return { ok: false, status: 401, code: "UNAUTHORIZED" };
    if (!/^[A-Za-z0-9_.:-]{1,128}$/.test(key)) return { ok: false, status: 400, code: "INVALID_IDEMPOTENCY_KEY" };
    if (bytes.length === 0 || bytes.length > MAX_BODY_BYTES) return { ok: false, status: 413, code: "BODY_LIMIT" };

    const existing = this.db.prepare(
      "SELECT id FROM reports WHERE project_id = ? AND idempotency_key = ?",
    ).get(projectId, key) as { id: string } | undefined;
    if (existing) return { ok: true, reportId: existing.id, duplicate: true };

    const minute = Math.floor(this.now() / 60_000);
    const rate = this.minuteCounts.get(projectId);
    const count = rate && rate.minute === minute ? rate.count : 0;
    if (count >= MAX_REQUEST_PER_MINUTE) return { ok: false, status: 429, code: "RATE_LIMIT" };
    this.minuteCounts.set(projectId, { minute, count: count + 1 });

    const dayStart = this.now() - 24 * 60 * 60 * 1000;
    const daily = this.db.prepare(
      "SELECT COUNT(*) AS n FROM reports WHERE project_id = ? AND created_at >= ?",
    ).get(projectId, dayStart) as { n: number };
    if (daily.n >= project.quota_per_day) return { ok: false, status: 429, code: "QUOTA_EXCEEDED" };

    const reportId = randomBytes(16).toString("hex");
    const nonce = randomBytes(12);
    const cipher = createCipheriv("aes-256-gcm", this.options.masterKey, nonce);
    cipher.setAAD(Buffer.from(projectId));
    const ciphertext = Buffer.concat([cipher.update(bytes), cipher.final(), cipher.getAuthTag()]);
    this.db.prepare(
      "INSERT INTO reports (id, project_id, idempotency_key, nonce, ciphertext, size, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
    ).run(reportId, projectId, key, nonce, ciphertext, bytes.length, this.now());
    this.audit("report_ingested", projectId, reportId, "ACCEPTED");
    return { ok: true, reportId, duplicate: false };
  }

  readReport(adminToken: string, reportId: string): Buffer | null {
    if (!safeEqual(adminToken, this.options.adminToken)) return null;
    const row = this.db.prepare(
      "SELECT nonce, ciphertext FROM reports WHERE id = ?",
    ).get(reportId) as { nonce: Buffer; ciphertext: Buffer } | undefined;
    if (!row) return null;
    const tag = row.ciphertext.subarray(row.ciphertext.length - 16);
    const ciphertext = row.ciphertext.subarray(0, row.ciphertext.length - 16);
    const project = this.db.prepare("SELECT project_id FROM reports WHERE id = ?").get(reportId) as { project_id: string };
    const decipher = createDecipheriv("aes-256-gcm", this.options.masterKey, row.nonce);
    decipher.setAAD(Buffer.from(project.project_id));
    decipher.setAuthTag(tag);
    const plain = Buffer.concat([decipher.update(ciphertext), decipher.final()]);
    this.audit("report_read", project.project_id, reportId, "ADMIN");
    return plain;
  }

  deleteReport(adminToken: string, reportId: string): boolean {
    if (!safeEqual(adminToken, this.options.adminToken)) return false;
    const result = this.db.prepare("DELETE FROM reports WHERE id = ?").run(reportId);
    this.audit("report_deleted", null, reportId, "ADMIN");
    return result.changes > 0;
  }

  auditLog(): Array<{ event: string; projectId: string | null; reportId: string | null; code: string | null; at: number }> {
    return (this.db.prepare("SELECT * FROM audit ORDER BY id").all() as Array<Record<string, unknown>>).map((row) => ({
      event: String(row.event),
      projectId: row.project_id === null ? null : String(row.project_id),
      reportId: row.report_id === null ? null : String(row.report_id),
      code: row.code === null ? null : String(row.code),
      at: Number(row.at),
    }));
  }

  private audit(event: string, projectId: string | null, reportId: string | null, code: string | null): void {
    this.db.prepare("INSERT INTO audit (event, project_id, report_id, code, at) VALUES (?, ?, ?, ?, ?)").run(
      event,
      projectId,
      reportId,
      code,
      this.now(),
    );
  }
}

function json(response: ServerResponse, status: number, value: unknown): void {
  const body = JSON.stringify(value);
  response.writeHead(status, {
    "content-type": "application/json; charset=utf-8",
    "cache-control": "no-store",
    "content-length": Buffer.byteLength(body),
  });
  response.end(body);
}

async function readBody(request: IncomingMessage): Promise<Buffer> {
  const chunks: Buffer[] = [];
  let total = 0;
  for await (const chunk of request) {
    const buffer = chunk as Buffer;
    total += buffer.length;
    if (total > MAX_BODY_BYTES) throw new Error("body limit");
    chunks.push(buffer);
  }
  return Buffer.concat(chunks);
}

function bearer(request: IncomingMessage): string | null {
  const value = request.headers.authorization;
  if (!value?.startsWith("Bearer ")) return null;
  return value.slice("Bearer ".length);
}

export function createRelayServer(options: ServerOptions): { server: Server; store: RelayStore } {
  if (options.adminToken.length < 32) throw new Error("admin token too short");
  const store = new RelayStore(options);
  const server = createServer(async (request, response) => {
    try {
      const url = new URL(request.url ?? "/", "http://localhost");
      if (request.method === "GET" && url.pathname === "/health") {
        json(response, 200, { status: "ok" });
        return;
      }
      const ingest = url.pathname.match(/^\/v1\/projects\/([A-Za-z0-9_.-]+)\/reports$/);
      if (request.method === "POST" && ingest) {
        const token = bearer(request);
        const key = request.headers["idempotency-key"];
        if (!token || typeof key !== "string") {
          json(response, 401, { error: "UNAUTHORIZED" });
          return;
        }
        const body = await readBody(request);
        const result = store.ingest(ingest[1]!, token, key, body);
        if (!result.ok) {
          json(response, result.status, { error: result.code });
          return;
        }
        json(response, result.duplicate ? 200 : 201, { reportId: result.reportId, duplicate: result.duplicate });
        return;
      }
      const admin = url.pathname.match(/^\/v1\/admin\/reports\/([a-f0-9]{32})$/);
      if (admin) {
        const token = bearer(request);
        if (!token) {
          json(response, 401, { error: "UNAUTHORIZED" });
          return;
        }
        if (request.method === "GET") {
          const bytes = store.readReport(token, admin[1]!);
          if (!bytes) {
            json(response, 404, { error: "NOT_FOUND" });
            return;
          }
          response.writeHead(200, {
            "content-type": "application/octet-stream",
            "cache-control": "no-store",
            "content-length": bytes.length,
          });
          response.end(bytes);
          return;
        }
        if (request.method === "DELETE") {
          const deleted = store.deleteReport(token, admin[1]!);
          json(response, deleted ? 200 : 404, { deleted });
          return;
        }
      }
      json(response, 404, { error: "NOT_FOUND" });
    } catch (error) {
      const message = error instanceof Error && error.message === "body limit" ? "BODY_LIMIT" : "INTERNAL";
      json(response, message === "BODY_LIMIT" ? 413 : 500, { error: message });
    }
  });
  return { server, store };
}
