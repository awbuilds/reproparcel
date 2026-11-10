# Phase 4 decision

Date: 2026-11-11
Status: **Desktop validator, inspector, viewer and read-only MCP implemented and
protocol-tested; two real coding clients and full end-to-end Android export are
not yet verified.**

## Verified locally

- Bounded streaming ZIP validation: count, size, ratio, traversal, duplicate,
  symlink, encryption and unreferenced-entry rejection.
- JSON/PNG limits before parse/decode; sanitized PNG re-encode.
- CLI `validate`, `inspect`, `render` and `mcp` with JSON/human output.
- Inert viewer with CSP, escaping, no scripts and no external URLs.
- Official MCP SDK protocol test: exactly five read-only tools, report-ID
  lookup, explicit root and untrusted-content warnings.
- Adversarial synthetic fixtures and CLI exit codes.
- Integration recipe and license warning.

## Gaps

- Two different real coding clients have not both inspected the same parcel.
- A full Android capture → reviewed export → desktop import flow on a device is
  not recorded.
- Symlink/permission-denial cases are implemented defensively but not all
  exercised with a crafted fixture.
- Alpha publication is not authorized and not performed.

These gaps block the alpha candidate claim, not the local tool implementation.
