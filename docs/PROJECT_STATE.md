# ReproParcel — Current project state

Updated: 2026-11-11.

## Current stage

**Phase 0 local feasibility gate passed for implementation; Phase 1 next.**
Phase 0 is **not externally complete**: adopter interviews, minimum-API minified
Compose evidence, full semantics coverage and physical-device performance remain
blocked or in progress. The repository has a sanitized public baseline, a
working Gradle/Android foundation, a disposable capture feasibility spike and a
line-level PRD audit.

No release, tag, publication, deployment, `main` branch or private-adopter
integration exists.

## Source of truth

- [PRD](PRD.md) — document version 1.0; native Android Kotlin/Compose;
  local-first reviewed evidence parcels; read-only local agent consumption.
- [AGENTS.md](../AGENTS.md) — engineering contract, public-repository hygiene,
  exception boundaries, standards and handoff protocol.
- [Latest handoff](handoffs/0002-2026-11-11-phase-0-feasibility.md).
- [Traceability ledger](traceability/prd-lines.jsonl) and
  [Phase 0 audit](audits/phase-0-audit.md).

## Completed locally

- Public-repository sanitation and hard hygiene rule.
- Gradle wrapper 9.7.1, AGP 9.5.1, Kotlin 2.4.21, Compose BOM 2026.09.00,
  minSdk 26, compile/target 37.
- Disposable `spike/capture-lab` with PixelCopy, opaque masking, protected-window
  omission, rotation and semantics observation.
- ADRs 0001–0006, threat-model draft, toolchain baseline, adoption kit,
  naming/namespace log, competitive summary and go/no-go decision.
- Debug instrumentation: 10 tests, 0 failures on API 26, 30 and 37 emulators.
- Release-minified API 26 and API 37 smoke: capture, mask and secure-window
  omission passed.

## Key decisions

- Working name: ReproParcel; no trademark or Maven namespace clearance claimed.
- Exact license: AGPL-3.0-only; no proprietary-linking exception.
- Explicit host-registered semantics is the v1 baseline; automatic Compose
  extraction is not proven and is not shipped.
- Image capture requires host policy and a `FLAG_SECURE` pre-check. Raw
  PixelCopy can report success on a protected window.
- Manual local export in v1; no mandatory backend or runtime LLM.
- Work remains on `dev`; `main` is untouched. No release until explicitly told.

## Verification status

Markdown/schema audits, builds and emulator instrumentation are recorded in the
latest handoff and traceability ledger. Phase 0 external interviews, physical
device tests, legal/trademark review, Sonatype verification, Maven publication,
independent security review, pilot and external integrations remain unverified
or blocked. Emulator success is not release evidence.

## Next exact action

Begin Phase 1: create the real module graph, schema v1, explicit runtime,
bounded policy/redaction and CI gates. Write failing tests first for invalid
configuration, disabled/no-default collection, limits, redaction, cancellation
and schema fixtures.

## Safety boundaries

No real reports, financial data, screenshots, credentials or private application
code in the repository. No personal information in commits, docs or handoffs.
No commit/push to `main`, no tag, release, package publication, deployment or
private-adopter production change without explicit authorization.
