# ReproParcel — Current project state

Updated: 2026-11-11.

## Current stage

**Phases 2–3 core flows implemented; Phase 4 next.**
The module graph, schema v1, runtime, reporter UI, capture controller, encrypted
drafts, export, breadcrumbs/logs, OkHttp metadata and explicit semantics are
implemented and tested on API 26/30/37 emulators. Phase 2/3 exit gaps (race
fixtures, process death, TalkBack, full golden matrix, measured overhead) are
recorded. The desktop CLI/viewer/read-only MCP is Phase 4. Phase 0 external
adopter validation, physical-device performance and legal/Maven gates remain
blocked.

No release, tag, publication, deployment, `main` branch or private-adopter
integration exists.

## Source of truth

- [PRD](PRD.md) — document version 1.0; native Android Kotlin/Compose;
  local-first reviewed evidence parcels; read-only local agent consumption.
- [AGENTS.md](../AGENTS.md) — engineering contract, public-repository hygiene,
  exception boundaries, standards and handoff protocol.
- [Latest handoff](handoffs/0005-2026-11-11-phase-3-adapters.md).
- [Traceability ledger](traceability/prd-lines.jsonl), [Phase 0 audit](audits/phase-0-audit.md),
  [Phase 1 audit](audits/phase-1-audit.md) and [Phase 3 audit](audits/phase-3-audit.md).

## Completed locally

- Public-repository sanitation and hard hygiene rule.
- Gradle wrapper 9.7.1, AGP 9.5.1, Kotlin 2.4.21, Compose BOM 2026.09.00,
  minSdk 26, compile/target 37.
- Phase 0 disposable capture spike with PixelCopy, opaque masking,
  protected-window omission, rotation and semantics observation.
- Phase 1 modules: core, android, compose, okhttp, sample app.
- Core schema v1, JSON budgets, limits, redaction, state machine, typed outcomes,
  parcel validation/writer and synthetic fixtures.
- Explicit runtime: idempotent init, default-off collection, enable/disable,
  supervised scope, bounded breadcrumbs/logs/network, explicit semantics and
  provider isolation.
- Compose reporter: form, image editor, review, encrypted drafts, SAF/FileProvider
  export and exact-preview binding.
- CI: hygiene, dependency allowlist, compile, JVM/API checks, lint and sample
  debug/release builds.
- ADRs 0001–0012, threat-model draft, phase decisions and evidence.

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

Phase 2/3: 40 core JVM tests, 2 OkHttp tests, 25 Android runtime tests, 2
reporter tests and 2 sample tests on API 26/30/37; `scripts/ci.sh` passes.
Phase 0: 10 capture tests on API 26/30/37 plus minified API 26/37 smoke. Exact
commands and evidence are in `docs/evidence/` and the latest handoff. External interviews, physical-device
tests, legal/trademark review, Sonatype verification, Maven publication,
independent security review, pilot and external integrations remain blocked.
Emulator success is not release evidence.

## Next exact action

Implement Phase 4: streaming/bounded parcel validator, CLI inspect/validate,
offline viewer, and read-only stdio MCP with an explicit import root. Then close
Phase 2/3 evidence gaps and run the Phase 5 release matrix.

## Safety boundaries

No real reports, financial data, screenshots, credentials or private application
code in the repository. No personal information in commits, docs or handoffs.
No commit/push to `main`, no tag, release, package publication, deployment or
private-adopter production change without explicit authorization.
