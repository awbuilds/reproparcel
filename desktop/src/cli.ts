#!/usr/bin/env node
// SPDX-License-Identifier: Apache-2.0
import { readParcel, summarize, ParcelError } from "./parcel.js";
import { renderParcel } from "./viewer.js";
import { startMcpServer } from "./mcp.js";

function usage(): void {
  process.stdout.write(`reproparcel 0.0.1-SNAPSHOT

Usage:
  reproparcel validate <file.reproparcel> [--json]
  reproparcel inspect <file.reproparcel> [--json]
  reproparcel render <file.reproparcel> --out <directory>
  reproparcel mcp --root <import-directory>

All commands are offline. MCP is read-only and accepts report IDs, not
arbitrary filesystem paths.
`);
}

function fail(error: unknown, json: boolean): never {
  if (error instanceof ParcelError) {
    const payload = { valid: false, error: { code: error.code, message: error.message, path: error.path ?? null } };
    process.stderr.write(json ? `${JSON.stringify(payload)}\n` : `FAIL ${error.code}: ${error.message}${error.path ? ` (${error.path})` : ""}\n`);
  } else {
    const message = error instanceof Error ? error.message : "unknown failure";
    process.stderr.write(json ? `${JSON.stringify({ valid: false, error: { code: "UNEXPECTED", message } })}\n` : `FAIL: ${message}\n`);
  }
  process.exitCode = 1;
  throw new Error("__handled__");
}

async function main(): Promise<void> {
  const args = process.argv.slice(2);
  if (args.length === 0 || args.includes("--help") || args.includes("-h")) {
    usage();
    return;
  }
  const [command, file, ...rest] = args;
  const json = args.includes("--json");
  const outIndex = args.indexOf("--out");
  const rootIndex = args.indexOf("--root");

  try {
    if (command === "validate") {
      if (!file) throw new ParcelError("USAGE", "Missing parcel file");
      const parcel = await readParcel(file);
      if (json) {
        process.stdout.write(`${JSON.stringify({ valid: true, reportId: parcel.manifest.reportId })}\n`);
      } else {
        process.stdout.write(`VALID ${parcel.manifest.reportId}\n`);
      }
      return;
    }
    if (command === "inspect") {
      if (!file) throw new ParcelError("USAGE", "Missing parcel file");
      const parcel = await readParcel(file);
      const summary = summarize(parcel);
      if (json) {
        process.stdout.write(`${JSON.stringify({ valid: true, summary })}\n`);
      } else {
        process.stdout.write(`Report: ${summary.reportId}\n`);
        process.stdout.write(`Created: ${summary.createdAt}\n`);
        process.stdout.write(`Description length: ${summary.descriptionLength}\n`);
        process.stdout.write(`Events: ${summary.events}\n`);
        process.stdout.write(`Semantic nodes: ${summary.semanticNodes}\n`);
        process.stdout.write(`Image: ${summary.image ? `${parcel.imageWidth}x${parcel.imageHeight}` : "none"}\n`);
      }
      return;
    }
    if (command === "render") {
      if (!file || outIndex < 0 || !args[outIndex + 1]) {
        throw new ParcelError("USAGE", "render requires a file and --out <directory>");
      }
      const parcel = await readParcel(file);
      await renderParcel(parcel, args[outIndex + 1]);
      process.stdout.write(`Rendered ${args[outIndex + 1]}\n`);
      return;
    }
    if (command === "mcp") {
      if (rootIndex < 0 || !args[rootIndex + 1]) {
        throw new ParcelError("USAGE", "mcp requires --root <import-directory>");
      }
      await startMcpServer(args[rootIndex + 1]);
      return;
    }
    usage();
    process.exitCode = 2;
  } catch (error) {
    fail(error, json);
  }
}

main().catch((error) => {
  if (error instanceof Error && error.message === "__handled__") return;
  process.stderr.write(`FAIL: ${error instanceof Error ? error.message : "unknown"}\n`);
  process.exitCode = 1;
});
