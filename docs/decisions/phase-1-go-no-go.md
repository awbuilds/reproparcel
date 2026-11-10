# Phase 1 decision

Date: 2026-11-11
Status: **Core Phase 1 gate passed locally.** Phase 2 UI/capture/draft work is
explicitly unfinished and tracked as `IN_PROGRESS`.

## Evidence

- `reproparcel-core`: 37 JVM tests, 0 failures, including schema conformance,
  limits, redaction, state transitions and parcel validation.
- `reproparcel-android`: 11 instrumentation tests, 0 failures on API 26, 30 and
  37 emulators.
- `sample-app`: 2 UI tests, 0 failures on API 26, 30 and 37 emulators.
- `scripts/ci.sh`: PASS (public hygiene, dependency allowlist, compile, JVM
  tests, API check, lint, debug/release sample builds).
- Core public API dump: `reproparcel-core/api/reproparcel-core.api`.
- Schema v1 + valid/invalid fixtures under `schema/`.

## Decision

Proceed to Phase 2 for the reporter UI, capture pipeline, encrypted drafts and
export. Keep these restrictions:

- No reporter UI claim until Phase 2 tests pass.
- No image capture until the Phase 2 capture contract is implemented.
- No draft/export until encrypted storage and review binding exist.
- No desktop/MCP claim until Phase 4.
- No stable release, tag, Maven publication, `main` merge or private-adopter
  integration.

## Honest limitations

The audit still shows `IN_PROGRESS` rows for UI, masks, drafts, semantic
registration, OkHttp and desktop tools. Those are later-phase scope, not Phase 1
failures, but they mean Phase 1 is a foundation gate, not Android v1.
External adopter interviews, physical-device performance, legal review and
Sonatype verification remain blocked.
