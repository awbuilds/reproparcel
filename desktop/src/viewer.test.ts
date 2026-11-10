// SPDX-License-Identifier: AGPL-3.0-only
import test from "node:test";
import assert from "node:assert/strict";
import { readFile, rm, stat } from "node:fs/promises";
import path from "node:path";
import { readParcel } from "./parcel.js";
import { renderParcel } from "./viewer.js";
import { createParcel, tempDirectory } from "./test-fixtures.js";

test("renders inert HTML with CSP and escaped report text", async () => {
  const directory = await tempDirectory();
  try {
    const parcelFile = await createParcel(directory);
    const parcel = await readParcel(parcelFile);
    const output = path.join(directory, "viewer");
    await renderParcel(parcel, output);
    const html = await readFile(path.join(output, "index.html"), "utf8");
    assert.ok(html.includes("Content-Security-Policy"));
    assert.ok(html.includes("&lt;script&gt;alert(1)&lt;/script&gt;"));
    assert.ok(!html.includes("<script>alert(1)</script>"));
    assert.ok(html.includes("Approved image"));
    await stat(path.join(output, "screen.png"));
  } finally {
    await rm(directory, { recursive: true, force: true });
  }
});

test("text-only viewer does not point at a missing image", async () => {
  const directory = await tempDirectory();
  try {
    const parcelFile = await createParcel(directory, { image: false });
    const parcel = await readParcel(parcelFile);
    const output = path.join(directory, "viewer");
    await renderParcel(parcel, output);
    const html = await readFile(path.join(output, "index.html"), "utf8");
    assert.ok(html.includes("No approved image is present"));
    assert.ok(!html.includes('src="screen.png"'));
  } finally {
    await rm(directory, { recursive: true, force: true });
  }
});
