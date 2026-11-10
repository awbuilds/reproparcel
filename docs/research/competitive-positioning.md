# Competitive positioning summary

Date: 2026-11-11

## Established alternatives

- **Sentry Android** provides user feedback, shake-to-report and session-replay
  integration. It is real prior art and can be self-hosted with substantial
  infrastructure.
- **Instabug/Luciq, Shake and Gleap** are commercial feedback/diagnostics
  products with vendor accounts and recurring services.
- **Quash** is existing open-source mobile bug-reporting work; acknowledge it.
- **BugDrop and BugPin** are web screenshot/annotation tools, not native Android
  capture SDKs.
- **Flutter feedback packages** exist; ReproParcel is Android-first.
- **Manual screenshots and tickets** remain simple and universal.

## Intended difference

- Local-first, portable `.reproparcel` evidence without a mandatory backend.
- Explicit host policy and fail-closed privacy, with omission reasons.
- Build/screen/action provenance and a versioned schema.
- Offline validator, inspector, viewer and read-only MCP access.
- AGPL-3.0-only inspectable implementation.

## Honest boundary

The product must validate that portability and lightweight local operation are
valuable enough to justify another library. It does not claim to be the first or
only feedback tool, to guarantee reproduction, or to replace crash monitoring,
ANR profiling, log aggregation or product analytics.
