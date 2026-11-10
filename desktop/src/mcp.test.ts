// SPDX-License-Identifier: AGPL-3.0-only
import test from "node:test";
import assert from "node:assert/strict";
import { rm } from "node:fs/promises";
import { ReportRepository } from "./mcp.js";
import { createParcel, tempDirectory } from "./test-fixtures.js";

test("repository lists and returns report IDs without arbitrary paths", async () => {
  const directory = await tempDirectory();
  try {
    await createParcel(directory);
    const repository = new ReportRepository(directory);
    const list = await repository.list();
    assert.equal(list.length, 1);
    assert.equal(list[0]?.reportId, "rpt_fixture_0001");

    const report = await repository.getReport("rpt_fixture_0001");
    assert.equal((report as { untrusted: boolean }).untrusted, true);

    const events = await repository.getEvents("rpt_fixture_0001");
    assert.equal((events as { untrusted: boolean }).untrusted, true);

    const semantics = await repository.getSemantics("rpt_fixture_0001");
    assert.equal((semantics as { untrusted: boolean }).untrusted, true);

    const attachment = await repository.getAttachment("rpt_fixture_0001");
    assert.equal(attachment?.mimeType, "image/png");
  } finally {
    await rm(directory, { recursive: true, force: true });
  }
});

test("repository rejects traversal-shaped report IDs", async () => {
  const directory = await tempDirectory();
  try {
    await createParcel(directory);
    const repository = new ReportRepository(directory);
    await assert.rejects(repository.getReport("../../etc/passwd"), /invalid report ID/);
  } finally {
    await rm(directory, { recursive: true, force: true });
  }
});
