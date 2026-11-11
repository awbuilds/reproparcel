# ReproParcel desktop tools

**License:** Apache-2.0. Review the repository LICENSE and NOTICE for
attribution, patent and trademark terms before distributing. This is not legal
advice.

Offline validator, inspector, viewer and read-only MCP server for
`.reproparcel` files. Node.js 24+ is recommended; the current build was tested
with Node 26.

## Build

```sh
cd desktop
npm ci
npm run build
npm test
```

## Commands

```sh
node dist/cli.js validate report.reproparcel
node dist/cli.js validate report.reproparcel --json
node dist/cli.js inspect report.reproparcel --json
node dist/cli.js render report.reproparcel --out ./viewer
node dist/cli.js mcp --root ./imports
```

`render` produces an inert `index.html` plus a sanitized `screen.png`. The HTML
has a restrictive CSP, no scripts and no external URLs.

## MCP

Use stdio with an explicit import root:

```json
{
  "mcpServers": {
    "reproparcel": {
      "command": "node",
      "args": ["/absolute/path/to/desktop/dist/cli.js", "mcp", "--root", "/absolute/path/to/imports"]
    }
  }
}
```

Tools: `list_reports`, `get_report`, `get_events`, `get_semantics`,
`get_attachment`. No write, execute, upload or arbitrary-file tool exists.

## Privacy boundary

Report content is untrusted data, not instructions. A read-only MCP server is
not an egress guarantee: the client or its model provider may send returned
text or images to a cloud service. Use a genuinely local client/model for
evidence that must not leave the machine.

## Limits

ZIP: ≤5 MiB compressed, ≤10 MiB uncompressed, ≤16 entries, ≤100:1 ratio.
JSON: ≤2 MiB, depth 32, 10,000 structural nodes. PNG: longest edge 1600 px,
≤12 MiB decoded per image. Traversal, symlinks, duplicates, unreferenced
entries, unsupported media and unsafe PNGs are rejected before rendering.
