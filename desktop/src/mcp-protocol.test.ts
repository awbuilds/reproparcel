// SPDX-License-Identifier: Apache-2.0
import test from "node:test";
import assert from "node:assert/strict";
import { rm } from "node:fs/promises";
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { InMemoryTransport } from "@modelcontextprotocol/sdk/inMemory.js";
import { createMcpServer } from "./mcp.js";
import { createParcel, tempDirectory } from "./test-fixtures.js";

test("MCP protocol exposes only the read-only report tools", async () => {
  const directory = await tempDirectory();
  const [clientTransport, serverTransport] = InMemoryTransport.createLinkedPair();
  const server = createMcpServer(directory);
  await server.connect(serverTransport);
  const client = new Client({ name: "reproparcel-test", version: "1.0.0" });
  await client.connect(clientTransport);
  try {
    await createParcel(directory);
    const tools = await client.listTools();
    assert.deepEqual(
      tools.tools.map((tool) => tool.name).sort(),
      ["get_attachment", "get_events", "get_report", "get_semantics", "list_reports"],
    );
    const listed = await client.callTool({ name: "list_reports", arguments: {} });
    const text = (listed.content as Array<{ type: string; text?: string }>)[0]?.text ?? "";
    assert.match(text, /rpt_fixture_0001/);

    const report = await client.callTool({
      name: "get_report",
      arguments: { report_id: "rpt_fixture_0001" },
    });
    const reportText = (report.content as Array<{ type: string; text?: string }>)[0]?.text ?? "";
    assert.match(reportText, /untrusted/);
  } finally {
    await client.close();
    await server.close();
    await rm(directory, { recursive: true, force: true });
  }
});
