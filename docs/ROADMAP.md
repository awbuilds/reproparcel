# Roadmap and gates

ReproParcel is delivered in gated phases. A phase is not marked complete until
its required evidence exists. External gates are recorded as `BLOCKED`, not
silently passed.

## Current position

- **Phases 0–5:** implemented as the Android/desktop source candidate.
- **Phases 6–9:** public reference implementations exist; production/platform
  gates remain open.
- **Release:** none. No tag, GitHub Release, Maven Central publication or
  deployment exists.

The live line-level status is in
[`docs/audits/final-audit.md`](audits/final-audit.md) and
[`docs/traceability/prd-lines.jsonl`](traceability/prd-lines.jsonl).

## Phase summary

| Phase | Scope | State |
|---|---|---|
| 0 | Feasibility, toolchain, capture/secure-window spike, public-repo hygiene | Local evidence recorded; external adopter validation blocked |
| 1 | Module graph, schema v1, runtime, redaction, limits, CI | Complete locally |
| 2 | Reporter UI, capture, masks, encrypted drafts, export | Complete locally; manual accessibility/race matrix partially open |
| 3 | Breadcrumbs, logs, OkHttp metadata, explicit semantics | Complete locally |
| 4 | Validator, inspector, viewer, read-only MCP | Complete locally; one real client verified, second blocked |
| 5 | Release hardening, local Maven, physical matrix, audit | Local evidence recorded; publication and external gates blocked |
| 6 | Optional self-hosted relay and tracker adapter | Reference implementation; not deployed |
| 7 | React Native Android host | Reference host builds and runs; second consumer and Expo/Hermes matrix open |
| 8 | Native iOS Swift package | Simulator/device builds pass; physical-device and App Store gates blocked |
| 9 | Flutter adapter and optional extensions | Android plugin/host tested; broader platform and demand gates open |

## Release gates

### Local evidence complete

- schema v1, bounds, redaction, typed outcomes and parcel checks;
- encrypted drafts with key/tamper/quota/TTL tests;
- fail-closed capture and opaque masks;
- Compose reporter, exact preview and approval reset;
- offline validator/viewer/read-only MCP;
- API 26/30/31 physical/37 release image export and desktop import;
- startup, capture, memory, collector, trigger-to-composer and paired jank
  measurements;
- public-hygiene scan and final traceability audit.

### External or manual gates still open

- five adopter interviews and three concrete use cases;
- 20-defect pilot with the PRD threshold;
- two external 30-minute integrations;
- a second real MCP coding client;
- current physical device and physical API 26/30 rows;
- manual TalkBack matrix and longer thermal/ANR soak;
- iOS physical-device/Keychain/scenes/App Store validation;
- relay deployment, TLS, backups, notice delivery and independent review;
- legal/Apache-2.0/trademark review;
- Sonatype namespace, signed Maven Central publication and release approval.

## Principles for future work

1. Privacy and host stability take precedence over collecting more evidence.
2. No hidden collection, mandatory backend, telemetry or runtime model.
3. No automatic Compose extraction without a public-API, minified-release
   compatibility matrix.
4. No release claim without a green line-level audit and explicit maintainer
   authorization.
5. Every new adapter ships with a schema fixture, failure tests and honest
   limitations.

## How to propose a roadmap change

Open an issue describing the user problem, privacy impact, compatibility impact
and verification plan. Changes to load-bearing invariants require an ADR and
maintainer approval; see [GOVERNANCE.md](../GOVERNANCE.md).
