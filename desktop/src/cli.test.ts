// SPDX-License-Identifier: AGPL-3.0-only
import test from "node:test";
import assert from "node:assert/strict";
import { execFile } from "node:child_process";
import { promisify } from "node:util";
import { rm } from "node:fs/promises";
import { createParcel, tempDirectory } from "./test-fixtures.js";

const exec = promisify(execFile);

test("CLI validates a synthetic parcel as JSON", async () => {
  const directory = await tempDirectory();
  try {
    const file = await createParcel(directory);
    const { stdout } = await exec(process.execPath, ["dist/cli.js", "validate", file, "--json"]);
    const parsed = JSON.parse(stdout);
    assert.equal(parsed.valid, true);
    assert.equal(parsed.reportId, "rpt_fixture_0001");
  } finally {
    await rm(directory, { recursive: true, force: true });
  }
});

test("CLI returns a non-zero exit for a corrupt parcel", async () => {
  const directory = await tempDirectory();
  try {
    const file = await createParcel(directory, { tamperReport: true });
    await assert.rejects(
      exec(process.execPath, ["dist/cli.js", "validate", file, "--json"]),
      (error: unknown) => {
        const value = error as { code?: number; stderr?: string };
        assert.equal(value.code, 1);
        assert.match(value.stderr ?? "", /CHECKSUM_MISMATCH|SIZE_MISMATCH/);
        return true;
      },
    );
  } finally {
    await rm(directory, { recursive: true, force: true });
  }
});

test("CLI renders an offline viewer", async () => {
  const directory = await tempDirectory();
  try {
    const file = await createParcel(directory);
    const output = `${directory}/viewer`;
    const { stdout } = await exec(process.execPath, ["dist/cli.js", "render", file, "--out", output]);
    assert.match(stdout, /Rendered/);
  } finally {
    await rm(directory, { recursive: true, force: true });
  }
});
