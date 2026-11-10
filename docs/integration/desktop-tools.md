# Desktop tools integration

## Validate before import

```sh
reproparcel validate report.reproparcel --json
```

A non-zero exit code or `valid:false` means the parcel was rejected before
extraction or rendering. Do not retry by bypassing the validator.

## Inspect

```sh
reproparcel inspect report.reproparcel --json
```

The JSON contains the report ID, app/device provenance, event count, semantic
node count, image dimensions and omission statuses.

## Render

```sh
reproparcel render report.reproparcel --out ./viewer
open ./viewer/index.html
```

The viewer escapes all report text, uses `default-src 'none'` with local images
only, executes no JavaScript and loads no external URL.

## Read-only MCP

```json
{
  "mcpServers": {
    "reproparcel": {
      "command": "node",
      "args": ["dist/cli.js", "mcp", "--root", "./imports"]
    }
  }
}
```

The server accepts report IDs, not arbitrary paths. It does not write, execute,
upload or create issues. Client/model egress is outside the SDK's control;
choose a local client for sensitive evidence.

## Two-client verification

The protocol suite is tested with the official MCP client SDK. Real coding
clients must be configured separately; record the client name/version and the
exact tools used. Do not claim two-client support without both sessions.
