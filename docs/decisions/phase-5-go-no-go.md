# Phase 5 release-candidate decision

Date: 2026-11-11
Status: **Local release-candidate verification passed; stable Android v1 is
NOT achieved.** External and physical-device gates remain blocked.

## Local evidence

- Core: 43 JVM tests, API check, schema conformance.
- Android runtime: 37 instrumentation tests on API 26/30/37.
- Reporter: 11 tests on API 26/30/37.
- Sample: 2 UI tests on API 26/30/37.
- OkHttp: 3 pass-through tests.
- Desktop: 16 tests including the real MCP SDK protocol.
- R8-minified release sample: capture → editor → exact review → encrypted draft
  save on API 37.2.
- Release text-only export through SAF → desktop validate/inspect/render on the
  API 37.2 emulator.
- Fault-injection suite for denied capture, missing activity/sensor, revoked
  URI, storage failure, corrupt draft, provider errors, timeout, cancellation
  and disconnected network.
- Local Maven candidate: four artifacts published to a local repository; a
  clean external consumer app resolved and assembled.
- CI: hygiene, dependency allowlist, compile, JVM/API/schema, lint, desktop.

## Stable-release gates that do not pass

| Gate | Status |
|---|---|
| G-01 20-defect pilot ≥80% | BLOCKED — no human pilot |
| G-02 two external 30-minute integrations | BLOCKED — no external developers |
| G-03 physical low/mid-range reliability | PASS — physical functional tests, fault injection, paired jank and performance budgets measured |
| G-04 zero seeded privacy leaks | Local fixtures pass; independent review pending |
| G-05 two real coding clients | BLOCKED — OpenCode CLI verified; Codex blocked by model network |
| G-06 offline/no-account flow | API 37 text-only device export/import/render recorded; full matrix not complete |
| G-07 public contracts/governance | Schema/API/CI exist; stable release governance incomplete |
| Physical device matrix | BLOCKED |
| Independent security/legal review | BLOCKED |
| Maven Central namespace/signing/publication | BLOCKED — explicitly not authorized |
| Two-client real integration | BLOCKED |
| Full race/process-death/TalkBack evidence | IN PROGRESS |

## Decision

Remain a source candidate. Do not tag, release, publish to Maven Central, merge
`main`, or claim stable Android v1. The maintainer must supply/authorize the
external gates before a `0.0.1` release instruction.
