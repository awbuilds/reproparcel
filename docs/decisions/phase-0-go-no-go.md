# Phase 0 go/no-go decision

Date: 2026-11-11
Decision owner: implementation agent under the explicit session objective;
maintainer review is required before any release.
Status: **Phase 0 local feasibility gate passed for implementation; external
adopter validation BLOCKED; Phase 0 is not externally complete.**

## Evidence reviewed

- `docs/research/toolchain-baseline.md`
- `docs/adr/0002-capture-feasibility-and-temporal-privacy.md`
- `docs/adr/0003-semantics-strategy.md`
- `docs/security/threat-model.md`
- `docs/research/naming-and-namespace.md`
- `docs/research/adoption-validation.md`

## Decision

1. **Proceed with Phase 1** on the local implementation path.
2. **Retain image capture** only with mandatory host policy, `FLAG_SECURE`
   pre-check, attempt-generation invalidation and fail-closed omission. Raw
   PixelCopy success is not evidence of privacy.
3. **Ship explicit host-registered semantics** as the v1 baseline. Do not ship
   an automatic Compose/accessibility extractor without a later public-API,
   minified-release compatibility matrix.
4. **Keep the local-first, file-based scope.** No backend, crash capture,
   video, analytics, telemetry or automatic submission is added.
5. **Do not claim Phase 0 is externally validated.** Five interviews and three
   concrete use cases remain blocked on human participation. This is a local
   implementation waiver, not external validation.
6. **Do not claim a stable release.** Physical-device performance, the full
   privacy/security review, pilot and clean-consumer Maven verification remain
   unresolved. `docs/decisions/phase-0-gate-waiver.md` records the waiver and
   its consequences.

## Why this is safe

- API 26/30/37 emulator tests prove the basic PixelCopy/mask path, secure-window
  policy omission, rotation handling and Compose UI rendering.
- The minified release APK captured and masked successfully.
- The spike discovered a real privacy hazard (raw PixelCopy success on a secure
  window) before the SDK implementation, so the contract can fail closed.
- Semantics is the least proven area; the fallback is explicitly to host
  registration, which keeps the reporter useful without unstable hooks.

## Conditions that would stop or narrow the work

- A public surface cannot establish frame/policy consistency: disable image
  collection there and keep text-only reporting.
- Encryption/key access fails: use an in-memory draft or typed storage failure;
  never persist plaintext.
- A performance or privacy budget fails: fix it or record an evidence-backed ADR;
  do not relabel the release.
- External validation shows no demand: pause or pivot rather than adding
  speculative platform scope.

## Next exact action

Proceed to Phase 1: create the real module graph, schema v1, explicit runtime,
bounded policy/redaction and CI gates. Keep Phase 0 external items marked
`BLOCKED` in the traceability ledger and handoff.
