# ReproParcel — Current project state

Updated: 2026-11-11.

## Current stage

**Phases 0–9 reference implementations present; stable Android v1 NOT achieved.**
The Android runtime/reporter/capture/drafts/export, evidence adapters, desktop
validator/viewer/read-only MCP, local Maven candidate, relay reference and
RN/iOS/Flutter reference tracks are implemented or scaffolded and tested where
the local toolchain permits. External pilot/interviews, physical devices,
independent review, two real clients, legal review, deployment and Maven Central
remain blocked.

No release, tag, publication, deployment, `main` branch or private-adopter
integration exists.

## Source of truth

- [PRD](PRD.md) — document version 1.0; native Android Kotlin/Compose;
  local-first reviewed evidence parcels; read-only local agent consumption.
- [AGENTS.md](../AGENTS.md) — engineering contract, public-repository hygiene,
  exception boundaries, standards and handoff protocol.
- [Latest handoff](handoffs/0012-2026-11-11-open-source-presentation.md).
- [Traceability ledger](traceability/prd-lines.jsonl), [Phase 0 audit](audits/phase-0-audit.md),
  [Phase 1 audit](audits/phase-1-audit.md), [Phase 3 audit](audits/phase-3-audit.md) and
  [Final audit](audits/final-audit.md).

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
- Desktop bounded validator, CLI inspect/render, inert viewer and read-only MCP.
- CI: hygiene, dependency allowlist, compile, JVM/API checks, lint and sample
  debug/release builds.
- Local Maven candidate + clean consumer smoke; R8-minified release flow.
- Phase 6 relay reference; Phase 7 RN bridge source; Phase 8 Swift core; Phase 9
  Dart core.
- Final audit corrections: consent-surface fields, new-report reset, real
  draft/cache deletion, evidence in parcels, optional-flag enforcement,
  noBackup drafts, aggregate parcel bounds and capture identity checks.
- ADRs 0001–0014, threat-model draft, phase decisions and evidence.
- Open-source presentation: README, original brand assets, synthetic
  screenshots, documentation index, architecture/FAQ/roadmap, governance,
  support, security and contribution templates, issue forms, Dependabot, Docs
  and DCO workflows, repository description/topics/Discussions/secret scanning
  and a custom social preview.

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

Phase 2/3: 43 core JVM tests, 3 OkHttp tests, 40 Android runtime tests, 11
reporter tests and 3 sample tests on API 26/30/37. Phase 4: 16 desktop tests.
Phase 5: local four-artifact publication, clean consumer assemble, R8 release
flow and 1.2 MiB sample APK. Phase 6 relay: 6 Node tests. Phase 8 Swift: 5
XCTest tests. Phase 9 Dart: 4 tests + analyzer. Real hosts: RN release build + reporter flow,
Flutter debug APK + native-plugin device integration test, iOS simulator/device
build + 5 simulator tests. Fault injection covers denied capture, missing
activity/sensor, revoked URI, storage failure, corrupt draft, provider errors,
timeout, cancellation and disconnected network. An on-device release export was
validated, inspected and rendered by the desktop CLI. `scripts/ci.sh` passes.
Physical low/mid-range API 31: 40 runtime, 11 reporter and 3 sample tests pass;
release capture/editor/review/draft/SAF export and desktop import pass;
airplane-mode create → SAF export → offline desktop validate/inspect/render also
passes.
Benchmark pair completed on the physical device: startup trace p95 1.55 ms,
capture p95 211 ms / 15.1 MB peak, trigger-to-composer p95 289 ms, paired
5×~1,050-frame jank passes, APK delta ~0.40 MiB.
Phase 0: 10 capture tests on API 26/30/37 plus minified API 26/37 smoke. Exact
commands and evidence are in `docs/evidence/` and the latest handoff. External interviews, physical-device
tests, legal/trademark review, Sonatype verification, Maven publication,
independent security review, pilot and external integrations remain blocked.
Final audit: 231 PASS / 43 BLOCKED / 0 UNREVIEWED / 0 IN_PROGRESS. G-03 passes
locally on the physical low/mid-range device; current/API 26/30 physical rows and
external gates remain blocked. Release capture/export/import is verified on
API 26/30/37 emulators and the API 31 physical device. Emulator success is not
release evidence.

## Next exact action

Independent review and the final traceability audit are complete; the source
candidate is frozen on `dev`. The maintainer must supply or authorize the
external/physical/legal/publication gates. Do not tag, release, publish, deploy
or merge `main` without an explicit instruction.

## Safety boundaries

No real reports, financial data, screenshots, credentials or private application
code in the repository. No personal information in commits, docs or handoffs.
No commit/push to `main`, no tag, release, package publication, deployment or
private-adopter production change without explicit authorization.
