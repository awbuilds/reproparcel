# 0008 — 2026-11-11 — Phase 6 relay and Phase 7–9 reference tracks

## Objective and scope

Implement the gated expansion tracks as public reference code under their
entry conditions. No deployment, publication, store submission or external
credential was created.

## Starting state

Phase 5 commit `25d2f38` on `dev`. Android v1 source candidate, desktop tools
and local Maven verification existed.

## Work completed

- Phase 6 Node relay: write-only project tokens, hashed at rest, AES-GCM
  encrypted evidence, idempotency, quota/rate limit, revocation, admin
  read/delete, content-free audit and GitHub adapter. 5 tests pass.
- Phase 7 React Native Android reference: typed TS API, Kotlin native module and
  package stub. Not built against a React Native host.
- Phase 8 Swift package: Codable schema models, limits, redaction, state machine
  and conditional UIKit capture. 5 XCTest tests pass on macOS.
- Phase 9 Flutter/Dart: pure contracts, channel reference, README. 4 Dart tests
  and analyzer pass; no plugin/host build.
- Decisions for Phases 6–9 and this handoff.

## Verification evidence

```text
server: npm test                 5 tests, 0 failures
ios: swift test                  5 tests, 0 failures
flutter: dart analyze            PASS
flutter: dart test               4 tests, 0 failures
```

Android/desktop/CI from Phase 5 remains the release-candidate baseline.

## Privacy, reliability and compatibility impact

Relay credentials are server-side and write-only; evidence is encrypted at rest
and audit rows contain no content. GitHub tokens never reach the app. RN/iOS/
Flutter are source references only; no support claim is made. No real data or
private application code was used.

## Incomplete work and blockers

- Phase 6 deployment/adopter demand/private-link UI/TLS review.
- Phase 7 real React Native release build and lifecycle tests.
- Phase 8 iOS simulator/device capture, Keychain, scenes, App Store review and
  RN iOS.
- Phase 9 Flutter plugin/native integration and demand.
- Stable Android v1 external gates remain blocked.

## Next exact action

Run independent review and the final traceability audit; close remaining evidence
gaps where possible. Do not release.

## Resume/rollback notes

Read `AGENTS.md`, `docs/PROJECT_STATE.md`, ADRs 0014 and the phase 6–9 decisions.
Reference tracks are isolated; removing them does not affect Android v1. No
tag/release/deployment/`main` action is authorized.
