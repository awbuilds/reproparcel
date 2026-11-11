// SPDX-License-Identifier: Apache-2.0
import test from "node:test";
import assert from "node:assert/strict";
import { mkdtemp, readdir, readFile, rm } from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import type { AddressInfo } from "node:net";
import { createRelayServer } from "./server.js";
import { GitHubAdapter } from "./github.js";

const ADMIN = "a".repeat(32);
const PROJECT_TOKEN = "p".repeat(32);

async function startRelay(quota = 100) {
  const directory = await mkdtemp(path.join(os.tmpdir(), "reproparcel-relay-"));
  const { server, store } = createRelayServer({
    dbPath: path.join(directory, "relay.sqlite"),
    masterKey: Buffer.alloc(32, 7),
    adminToken: ADMIN,
    defaultQuotaPerDay: quota,
  });
  store.provisionProject("project-1", PROJECT_TOKEN, quota);
  await new Promise<void>((resolve) => server.listen(0, "127.0.0.1", resolve));
  const port = (server.address() as AddressInfo).port;
  return {
    directory,
    store,
    server,
    base: `http://127.0.0.1:${port}`,
    async close() {
      await new Promise<void>((resolve) => server.close(() => resolve()));
      await rm(directory, { recursive: true, force: true });
    },
  };
}

test("write-only ingestion is authenticated, idempotent and encrypted at rest", async () => {
  const relay = await startRelay();
  try {
    const marker = "SYNTHETIC_SECRET_MARKER";
    const first = await fetch(`${relay.base}/v1/projects/project-1/reports`, {
      method: "POST",
      headers: {
        authorization: `Bearer ${PROJECT_TOKEN}`,
        "idempotency-key": "key-1",
      },
      body: marker,
    });
    assert.equal(first.status, 201);
    const firstJson = (await first.json()) as { reportId: string; duplicate: boolean };
    assert.equal(firstJson.duplicate, false);

    const duplicate = await fetch(`${relay.base}/v1/projects/project-1/reports`, {
      method: "POST",
      headers: {
        authorization: `Bearer ${PROJECT_TOKEN}`,
        "idempotency-key": "key-1",
      },
      body: marker,
    });
    assert.equal(duplicate.status, 200);
    const duplicateJson = (await duplicate.json()) as { reportId: string };
    assert.equal(duplicateJson.reportId, firstJson.reportId);

    const projectRead = await fetch(`${relay.base}/v1/admin/reports/${firstJson.reportId}`, {
      headers: { authorization: `Bearer ${PROJECT_TOKEN}` },
    });
    assert.equal(projectRead.status, 404);

    const adminRead = await fetch(`${relay.base}/v1/admin/reports/${firstJson.reportId}`, {
      headers: { authorization: `Bearer ${ADMIN}` },
    });
    assert.equal(adminRead.status, 200);
    assert.equal(await adminRead.text(), marker);

    const files = await readdir(relay.directory);
    for (const file of files) {
      const bytes = await readFile(path.join(relay.directory, file));
      assert.equal(bytes.includes(Buffer.from(marker)), false, `plaintext found in ${file}`);
    }

    const deleted = await fetch(`${relay.base}/v1/admin/reports/${firstJson.reportId}`, {
      method: "DELETE",
      headers: { authorization: `Bearer ${ADMIN}` },
    });
    assert.equal(deleted.status, 200);
    const missing = await fetch(`${relay.base}/v1/admin/reports/${firstJson.reportId}`, {
      headers: { authorization: `Bearer ${ADMIN}` },
    });
    assert.equal(missing.status, 404);
  } finally {
    await relay.close();
  }
});

test("revoked credentials cannot ingest or read", async () => {
  const relay = await startRelay();
  try {
    relay.store.revokeProject("project-1");
    const denied = await fetch(`${relay.base}/v1/projects/project-1/reports`, {
      method: "POST",
      headers: {
        authorization: `Bearer ${PROJECT_TOKEN}`,
        "idempotency-key": "key-2",
      },
      body: "synthetic",
    });
    assert.equal(denied.status, 401);
  } finally {
    await relay.close();
  }
});

test("daily quota is enforced", async () => {
  const relay = await startRelay(1);
  try {
    const headers = {
      authorization: `Bearer ${PROJECT_TOKEN}`,
      "idempotency-key": "key-a",
    };
    assert.equal((await fetch(`${relay.base}/v1/projects/project-1/reports`, { method: "POST", headers, body: "1" })).status, 201);
    const second = await fetch(`${relay.base}/v1/projects/project-1/reports`, {
      method: "POST",
      headers: { authorization: `Bearer ${PROJECT_TOKEN}`, "idempotency-key": "key-b" },
      body: "2",
    });
    assert.equal(second.status, 429);
  } finally {
    await relay.close();
  }
});

test("audit log contains identifiers but no report content", async () => {
  const relay = await startRelay();
  try {
    await fetch(`${relay.base}/v1/projects/project-1/reports`, {
      method: "POST",
      headers: { authorization: `Bearer ${PROJECT_TOKEN}`, "idempotency-key": "audit-key" },
      body: "do-not-log-this",
    });
    const audit = relay.store.auditLog();
    assert.ok(audit.some((entry) => entry.event === "report_ingested"));
    assert.equal(JSON.stringify(audit).includes("do-not-log-this"), false);
  } finally {
    await relay.close();
  }
});

test("project and admin credentials must differ", async () => {
  const directory = await mkdtemp(path.join(os.tmpdir(), "reproparcel-relay-"));
  try {
    const { store } = createRelayServer({
      dbPath: path.join(directory, "relay.sqlite"),
      masterKey: Buffer.alloc(32, 7),
      adminToken: PROJECT_TOKEN,
    });
    assert.throws(() => store.provisionProject("p", PROJECT_TOKEN), /must differ/);
  } finally {
    await rm(directory, { recursive: true, force: true });
  }
});

test("GitHub adapter sends the token only server-side and creates an issue", async () => {
  const calls: Array<{ url: string; init: RequestInit }> = [];
  const fakeFetch = (async (url: string | URL | Request, init?: RequestInit) => {
    calls.push({ url: String(url), init: init ?? {} });
    return new Response(JSON.stringify({ number: 7, html_url: "https://github.com/example/repo/issues/7", title: "Report" }), {
      status: 201,
      headers: { "content-type": "application/json" },
    });
  }) as typeof fetch;
  const adapter = new GitHubAdapter({
    token: "server-side-token",
    repository: "example/repo",
    fetchImpl: fakeFetch,
  });
  const issue = await adapter.createIssue("Report", "Private link: https://relay.example/private/abc");
  assert.equal(issue.number, 7);
  assert.equal(calls.length, 1);
  assert.match(calls[0]!.url, /repos\/example\/repo\/issues$/);
  assert.equal((calls[0]!.init.headers as Record<string, string>).authorization, "Bearer server-side-token");
  assert.match(String(calls[0]!.init.body), /Private link/);
});
