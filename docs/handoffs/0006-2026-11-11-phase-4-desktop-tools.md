# 0006 — 2026-11-11 — Phase 4 offline tools and read-only MCP

## Objective and scope

Implement the bounded offline validator/inspector, inert viewer and local
read-only MCP. Work is on `dev`; no release/tag/publication was performed.

## Starting state

Phase 2/3 commit `ce25c0a` on `dev`. Android reporter/drafts/export and evidence
adapters were implemented; desktop tooling did not exist.

## Work completed

- TypeScript/Node desktop package with streaming bounded ZIP reader, Ajv schema
  validation, JSON budgets and PNG dimension/decoded-byte checks.
- CLI `validate`, `inspect`, `render` and `mcp`.
- Inert HTML viewer with CSP, escaping, local image only and no scripts.
- Official MCP SDK server with the five read-only tools and explicit import
  root/report IDs.
- Tests for valid/corrupt/extra/unreferenced parcels, JSON abuse, viewer
  escaping, CLI behavior, repository path rejection and MCP protocol/tool list.
- Integration recipe, license warning and ADR 0013.

## Verification evidence

```text
cd desktop && npm test
  14 tests, 0 failures
  CLI validate/render, viewer CSP/escaping, MCP protocol/tools, bounded parser
scripts/ci.sh  PASS (Android + desktop)
```

## Privacy, reliability and compatibility impact

Report content is always untrusted data. The viewer executes nothing. MCP has no
write/execute/upload tool and accepts report IDs only. The reader rejects
traversal, symlinks, duplicates, unreferenced entries and oversized/deep data
before rendering. Client/model egress is explicitly outside the SDK control.

## Incomplete work and blockers

- Two real coding clients and the full Android-export → desktop-import flow are
  manual integration gates.
- Not every crafted symlink/permission case has a fixture.
- Alpha publication requires explicit approval; not performed.

## Next exact action

Run Phase 5 release hardening: R8 release matrix, Keystore/masking/export/import
on devices, performance measurements, API/schema freeze, Maven local/clean
consumer, docs/notices, independent review and final gate audit.

## Resume/rollback notes

Read `AGENTS.md`, `docs/PROJECT_STATE.md`, ADR 0013 and `desktop/README.md`.
Desktop code is optional tooling; it does not affect Android runtime. No
release/tag/`main` action is authorized.
