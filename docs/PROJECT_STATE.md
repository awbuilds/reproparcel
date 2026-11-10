# ReproParcel — Current project state

Updated: 2026-11-11.

## Current stage

**Phase 1 core gate passed locally; Phase 2 next.**
The module graph, schema v1, explicit runtime, redaction, bounded evidence and
CI gates are implemented and tested. Phase 1 is a foundation gate, not Android
v1: the reporter UI, capture pipeline, encrypted drafts and export are Phase 2;
the desktop tools are Phase 4. Phase 0 external adopter validation and physical
device evidence remain blocked.

No release, tag, publication, deployment, `main` branch or private-adopter
integration exists.

## Source of truth

- [PRD](PRD.md) — document version 1.0; native Android Kotlin/Compose;
  local-first reviewed evidence parcels; read-only local agent consumption.
- [AGENTS.md](../AGENTS.md) — engineering contract, public-repository hygiene,
  exception boundaries, standards and handoff protocol.
- [Latest handoff](handoffs/0003-2026-11-11-phase-1-core.md).
- [Traceability ledger](traceability/prd-lines.jsonl), [Phase 0 audit](audits/phase-0-audit.md)
  and [Phase 1 audit](audits/phase-1-audit.md).

## Completed locally

- Public-repository sanitation and hard hygiene rule.
- Gradle wrapper 9.7.1, AGP 9.5.1, Kotlin 2.4.21, Compose BOM 2026.09.00,
  minSdk 26, compile/target 37.
- Phase 0 disposable capture spike with PixelCopy, opaque masking,
  protected-window omission, rotation and semantics observation.
- Phase 1 modules: core, android, compose, okhttp, sample app.
- Core schema v1, JSON budgets, limits, redaction, state machine, typed outcomes,
  parcel validation and synthetic fixtures.
- Explicit runtime: idempotent init, default-off collection, enable/disable,
  supervised scope, bounded breadcrumbs/logs and provider isolation.
- CI: hygiene, dependency allowlist, compile, JVM tests, API check, lint and
  sample debug/release builds.
- ADRs 0001–0009, threat-model draft, Phase 0/1 go/no-go records and evidence.

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

Phase 1: 37 core JVM tests, 11 Android runtime tests on API 26/30/37, 2 sample
UI tests on API 26/30/37 and `scripts/ci.sh` pass. Phase 0: 10 capture tests on
API 26/30/37 plus minified API 26/37 smoke. Exact commands and evidence are in
`docs/evidence/` and the latest handoff. External interviews, physical-device
tests, legal/trademark review, Sonatype verification, Maven publication,
independent security review, pilot and external integrations remain blocked.
Emulator success is not release evidence.

## Next exact action

Begin Phase 2: write failing tests for the reporter flow, secure/denied capture,
opaque mask races, encrypted draft key failure, process death, SAF cancellation
and duplicate-export prevention. Implement `docs/security/phase-2-capture-contract.md`
before enabling image collection.

## Safety boundaries

No real reports, financial data, screenshots, credentials or private application
code in the repository. No personal information in commits, docs or handoffs.
No commit/push to `main`, no tag, release, package publication, deployment or
private-adopter production change without explicit authorization.
