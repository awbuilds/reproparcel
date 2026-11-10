# ADR 0013 — Offline reader, viewer and MCP trust boundary

- Status: Accepted for Phase 4
- Date: 2026-11-11
- Deciders: ReproParcel maintainers

## Decision

- The reader is streaming/bounded: it checks ZIP entry count, compressed and
  uncompressed totals, compression ratio, path allowlist, duplicate names,
  symlinks, encryption, unsupported media and unreferenced entries before
  exposing content.
- JSON is size/depth/node bounded before parsing. PNG dimensions and decoded
  bytes are bounded before decoding; decoded pixels are re-encoded to a
  metadata-stripped PNG for rendering.
- Checksums detect corruption, not authenticity. Provenance is shown as supplied
  and unverified.
- The viewer renders escaped text, local approved image only, a restrictive CSP
  and no scripts. It does not execute report content.
- MCP uses stdio, an explicit import root and report IDs only. It exposes exactly
  `list_reports`, `get_report`, `get_events`, `get_semantics` and
  `get_attachment`. No write/execute/upload, arbitrary paths or dynamic tools
  derived from report content.
- Every MCP result marks content untrusted and warns that the client/model may
  send it to a cloud service. Read-only is not an egress guarantee.
- The desktop tool validates with the same JSON Schema and synthetic fixtures
  as the Android writer; it is a separate implementation, not a shared runtime.

## Evidence

Node tests cover valid/corrupt/unreferenced/extra-entry parcels, JSON depth/node
abuse, viewer escaping/CSP, CLI validate/render behavior and the real MCP SDK
protocol with the five read-only tools. Two real coding clients remain a manual
integration gate.
