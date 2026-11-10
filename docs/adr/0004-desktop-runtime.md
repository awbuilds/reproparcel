# ADR 0004 — Desktop runtime and tooling

- Status: Proposed for Phase 1; Phase 0 recommendation
- Date: 2026-11-11
- Deciders: ReproParcel maintainers

## Context

The desktop deliverable must validate, inspect and render parcels offline and
expose a read-only MCP server. It must not run arbitrary report content or
assume a specific AI provider. Phase 1 must verify the actual MCP SDK interface
and dependency licenses before adopting it.

## Decision

Use a maintained Node.js LTS/TypeScript toolchain for `desktop/`, with separate
packages:

```text
desktop/packages/parcel-core   streaming ZIP reader, schema/limits, safe PNG
desktop/packages/cli           validate, inspect, render, mcp
desktop/packages/viewer        inert offline HTML renderer
desktop/packages/mcp           stdio read-only MCP server
```

Pinned dependencies, no dynamic versions. Prefer the official MCP TypeScript
SDK only after Phase 1 verifies its current protocol/tool interface. Use a
bounded ZIP reader, a JSON Schema validator, and a PNG decoder that supports
explicit decoded-size limits; reject unsupported compression and attachments.

## Constraints

- No network calls from the viewer or validator.
- No arbitrary file paths: MCP uses an explicit import root and report IDs.
- No write/execute/upload tools.
- Text is rendered inert with escaping and a restrictive CSP.
- Image dimensions and decoded-byte estimates are checked before decoding.
- The desktop package must be installable/packable without native binaries
  where practical; any native dependency needs an explicit license/size review.

## Alternatives considered

- Kotlin/JVM desktop CLI: shares language with core but duplicates parcel logic
  and makes the MCP/viewer packaging heavier. Remains a fallback if Node LTS
  cannot satisfy the security or packaging constraints.
- Python: viable for a CLI, but a second runtime adds packaging and dependency
  review cost; not chosen for the baseline.
