# 0007 — 2026-11-11 — Phase 5 release-candidate hardening

## Objective and scope

Harden the source candidate, verify local publication and the minified release
path, and record which stable gates remain blocked. No release/tag/Maven Central
publication/`main` merge was performed.

## Starting state

Phase 4 commit `b9c424f` on `dev`. Android reporter, drafts, export, adapters and
desktop tools existed.

## Work completed

- Local Maven publishing for core/android/compose/okhttp with POM license/SCM
  metadata; no external repository or signing.
- Clean consumer app resolved the four local artifacts and assembled.
- R8-minified sample release smoke: capture → editor → exact review → encrypted
  draft save.
- Added compatibility, performance, privacy-policy, troubleshooting, dependency
  license and API-freeze documents.
- Captured release/local evidence and Phase 5 decision.

## Verification evidence

```text
scripts/release-local.sh   PASS (four artifacts; clean consumer assembleDebug)
scripts/consumer-smoke.sh  PASS
sample-app release         PASS; 1.2 MiB APK
R8 release UI smoke        capture/editor/review/draft save PASS
scripts/ci.sh              PASS
```

## Privacy, reliability and compatibility impact

No external publication, tag, release or `main` merge. Release smoke used
synthetic data and debug signing. The local POM has AGPL-3.0-only license/SCM
metadata. Physical-device, independent review, pilot, two-client, legal and
Maven Central gates remain blocked.

## Incomplete work and blockers

- G-01/G-02/G-03/G-05/G-06 external/physical evidence.
- Full race/process-death/TalkBack matrix.
- Complete generated third-party notices and legal review.
- Maven Central namespace, signing and publication.
- Phases 6–9 reference implementations remain to be done under their gates.

## Next exact action

Implement the Phase 6–9 reference tracks under the recorded entry gates, then
rerun the final traceability audit. Do not release.

## Resume/rollback notes

Read `AGENTS.md`, `docs/PROJECT_STATE.md`, ADRs 0010–0013,
`docs/decisions/phase-5-go-no-go.md` and the evidence folder. Local artifacts
are disposable; no external state was changed. Never tag/release/`main` without
explicit instruction.
