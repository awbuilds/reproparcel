# 0002 — 2026-11-11 — Phase 0 feasibility and repository foundation

## Objective and scope

Execute Phase 0 of `docs/PRD.md`: prove the hard capture/privacy/semantics
parts, establish the public-repository and toolchain baseline, record a go/no-go
decision and leave a traceable foundation for Phase 1. Phase 0 is **not
externally complete**; adopter interviews remain blocked and the local proceed
decision is recorded in `docs/decisions/phase-0-gate-waiver.md`. Work is on
`dev`; no release, tag or publication was performed.

## Starting state

Baseline commit `9ded595` on `dev`; there was no product source code, Gradle
build, Git repository or release. The PRD, AGENTS.md, project state and handoff
`0001` existed as documentation and were sanitized before the first commit.

## Work completed

- Added the public-repository hygiene hard rule to `AGENTS.md`; sanitized the
  PRD, project state and handoff `0001`; added `.gitignore` and
  `scripts/public-hygiene.sh`.
- Added the line-level PRD ledger (`docs/traceability/prd-lines.jsonl`), audit
  script, override mechanism and Phase 0 audit.
- Created the Gradle foundation: wrapper 9.7.1, AGP 9.5.1, Kotlin 2.4.21,
  Compose BOM 2026.09.00, minSdk 26, compile/target 37.
- Built the disposable `spike/capture-lab`: Compose screens, PixelCopy capture,
  opaque masking, protected-window policy, rotation, semantics observation and
  test-only Compose semantics coverage.
- Recorded ADRs 0001–0006, the threat-model draft, toolchain baseline, adoption
  interview kit, naming/namespace log, competitive summary and go/no-go decision.
- Recorded the key privacy finding: raw PixelCopy returned `SUCCESS` on a
  `FLAG_SECURE` window; the SDK must pre-check and omit.

## Verification evidence

```text
./gradlew :spike:capture-lab:assembleDebug      PASS
./gradlew :spike:capture-lab:assembleRelease    PASS (R8 + resource shrinking)

ANDROID_SERIAL=emulator-5556 ...connectedDebugAndroidTest
  rp-api26(AVD) - 8.0.0   OK (10 tests)
ANDROID_SERIAL=emulator-5558 ...connectedDebugAndroidTest
  rp-api30(AVD) - 11      tests=10 failures=0
ANDROID_SERIAL=emulator-5554 ...connectedDebugAndroidTest
  Android_17(AVD) - 17    tests=10 failures=0
```

Release-minified UI smoke:

```text
API 37: capture: SUCCESS 1080x2400 in 15ms; masked=#FF000000; saved=true
API 26: capture: SUCCESS 1080x1920 in 21ms; masked=#FF000000; saved=true
respect=true -> SECURE_WINDOW FLAG_SECURE is set; capture omitted
respect=false -> SUCCESS bytes=10368000 center=#FFFAFAFA
```

`python3 scripts/prd_audit.py --phase 0` → PASS with 37 PASS, 17 IN_PROGRESS,
12 BLOCKED and 2 GATED rows. IN_PROGRESS and BLOCKED rows are the honest
remaining Phase 0 work; they are not a complete external validation.
`scripts/public-hygiene.sh --all` → PASS. Retained evidence is under
`docs/evidence/phase-0/`.

## Privacy, reliability and compatibility impact

No real reports, screenshots, credentials or private adopter code were used.
No network, telemetry, backend, extra permission or production host change was
introduced. The spike is synthetic and separate from the private adopter.
Compatibility evidence is emulator-only; physical-device performance remains
unverified. The secure-window finding strengthens the fail-closed policy.

## Incomplete work and blockers

- Five adopter interviews and three concrete use cases: BLOCKED (human).
- Sonatype namespace, trademark/legal review and Maven publication: BLOCKED.
- Physical low/mid-range device, startup/jank/thermal evidence: BLOCKED.
- Release JUnit test-APK stability under R8: unresolved; release smoke used UI
  automation. Phase 5 must add a stable harness.
- Multi-window, SurfaceView, WebView, mixed View/Compose and scroll-race
  fixtures: deferred to Phase 2.
- Full generated third-party notices and independent security review: later
  phases. `NOTICE` and `SECURITY.md` are present; the private GitHub advisory
  setting still needs verification before publication.

## Next exact action

Start Phase 1: create the real module graph, schema v1 and explicit runtime —
`reproparcel-core`, `reproparcel-android`, `reproparcel-compose`,
`reproparcel-okhttp`, `sample-app`, `benchmark`, `schema/` and `desktop/`
skeletons — with tests for invalid configuration, no-default collection,
limits, redaction, cancellation and schema fixtures.

## Resume/rollback notes

Read `AGENTS.md`, `docs/PROJECT_STATE.md`, this handoff and ADRs 0001–0006 first.
The Phase 0 spike is disposable; removing `spike/` and the Phase 0 docs has no
production impact. Keep `dev` only; never create or push `main`. No tag,
release or package publication is authorized until an explicit instruction.
