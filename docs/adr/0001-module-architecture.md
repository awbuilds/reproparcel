# ADR 0001 — Module architecture and dependency direction

- Status: Accepted for Phase 0
- Date: 2026-11-11
- Deciders: ReproParcel maintainers
- Supersedes: none

## Context

ReproParcel must keep the host app stable while producing a portable evidence
parcel. Core contracts must not depend on Android, Compose, desktop tooling or
any model provider. Optional adapters must remain optional.

## Decision

Use the following Gradle modules and dependency direction:

```text
:reproparcel-core       Kotlin/JVM models, schema, policy, redaction, limits,
                        state machine, typed outcomes, parcel validation
:reproparcel-android    Android lifecycle, eligible-window capture, encrypted
                        drafts, FileProvider/SAF export
:reproparcel-compose    Material 3 reporter UI, host registration, privacy
                        region mapping, optional semantic adapter
:reproparcel-okhttp     Opt-in request/response metadata around the host's
                        OkHttp pipeline
:sample-app             Synthetic Compose and mixed View/Compose scenarios
:benchmark              Paired release-build performance and capture benchmarks
desktop/                Offline validator, inspector, viewer and read-only MCP
schema/                 JSON Schema, format contract, migration rules, fixtures
server/                 Future optional delivery; separate approval
spike/capture-lab       Disposable Phase 0 feasibility harness
```

`reproparcel-core` must not reference Android, Compose, desktop, network or LLM
APIs. `reproparcel-android` depends on core. `reproparcel-compose` depends on
android and core. `reproparcel-okhttp` depends on core only where it needs
metadata contracts; it must not depend on Compose. The sample app depends on
the public artifacts. Desktop consumers implement the same schema and share
fixtures, not runtime classes.

## Consequences

- A host can adopt only the artifacts it needs.
- The core can be tested on the JVM without an emulator.
- The Android runtime can be replaced without changing the parcel contract.
- The desktop tool can be implemented in another language if the Phase 1 ADR
  selects one; schema and fixtures remain the shared contract.
- No mandatory DI, database, network, Play Services or native binary dependency
  is introduced by the baseline.

## Alternatives considered

- Single Android library containing everything: rejected because it forces
  Compose and Android on every consumer and makes JVM contract tests harder.
- Kotlin Multiplatform for desktop reuse: deferred; Phase 1 selects the desktop
  runtime after verifying the actual MCP SDK and packaging constraints.
