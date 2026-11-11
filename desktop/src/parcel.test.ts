// SPDX-License-Identifier: Apache-2.0
import test from "node:test";
import assert from "node:assert/strict";
import { rm } from "node:fs/promises";
import path from "node:path";
import { readParcel, ParcelError, jsonBudget, summarize } from "./parcel.js";
import { createParcel, tempDirectory } from "./test-fixtures.js";

test("reads a valid synthetic parcel and sanitizes the PNG", async () => {
  const directory = await tempDirectory();
  try {
    const file = await createParcel(directory);
    const parcel = await readParcel(file);
    assert.equal(parcel.manifest.reportId, "rpt_fixture_0001");
    assert.equal(parcel.events.length, 1);
    assert.equal(parcel.semantics?.nodes.length, 1);
    assert.equal(parcel.imageWidth, 10);
    assert.ok(parcel.imagePng);
    assert.equal(parcel.imagePng?.subarray(1, 4).toString("ascii"), "PNG");
    const summary = summarize(parcel);
    assert.equal(summary.reportId, "rpt_fixture_0001");
  } finally {
    await rm(directory, { recursive: true, force: true });
  }
});

test("reads the cross-writer synthetic golden parcel", async () => {
  const golden = path.resolve(process.cwd(), "../schema/fixtures/v1/valid/golden.reproparcel");
  const parcel = await readParcel(golden);
  assert.equal(parcel.manifest.reportId, "rpt_golden_0001");
  assert.equal(parcel.events.length, 1);
  assert.equal(parcel.semantics?.nodes.length, 1);
  assert.equal(parcel.imageWidth, 10);
});

test("supports a text-only parcel", async () => {
  const directory = await tempDirectory();
  try {
    const file = await createParcel(directory, { image: false });
    const parcel = await readParcel(file);
    assert.equal(parcel.imagePng, null);
    assert.equal(parcel.imageWidth, 0);
  } finally {
    await rm(directory, { recursive: true, force: true });
  }
});

test("rejects a checksum mismatch", async () => {
  const directory = await tempDirectory();
  try {
    const file = await createParcel(directory, { tamperReport: true });
    await assert.rejects(readParcel(file), (error: unknown) => {
      return error instanceof ParcelError && error.code === "CHECKSUM_MISMATCH";
    });
  } finally {
    await rm(directory, { recursive: true, force: true });
  }
});

test("rejects an unreferenced entry", async () => {
  const directory = await tempDirectory();
  try {
    const file = await createParcel(directory, { omitReportEntry: true });
    await assert.rejects(readParcel(file), (error: unknown) => {
      return error instanceof ParcelError && (error.code === "UNREFERENCED_ENTRY" || error.code === "MISSING_ENTRY");
    });
  } finally {
    await rm(directory, { recursive: true, force: true });
  }
});

test("rejects an unsupported extra entry", async () => {
  const directory = await tempDirectory();
  try {
    const file = await createParcel(directory, { extraEntry: "extra.bin" });
    await assert.rejects(readParcel(file), (error: unknown) => {
      return error instanceof ParcelError && error.code === "INVALID_PATH";
    });
  } finally {
    await rm(directory, { recursive: true, force: true });
  }
});

test("json budget rejects depth and node abuse", () => {
  assert.equal(jsonBudget("[".repeat(40) + "]".repeat(40)).ok, false);
  const many = `[${Array.from({ length: 10_001 }, () => "{}").join(",")}]`;
  assert.equal(jsonBudget(many).ok, false);
  assert.equal(jsonBudget('{"a":1}').ok, true);
});
