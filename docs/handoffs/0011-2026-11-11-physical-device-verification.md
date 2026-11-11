# 0011 — 2026-11-11 — Physical device verification and benchmark preparation

## Objective and scope

Run the Android release and performance matrix on a connected low/mid-range
device, add the paired benchmark scaffolding needed for G-03, and keep the
public repository sanitized. No release/tag/publication/deployment or `main`
merge was performed.

## Starting state

`dev` at `0511dcf` (final source-candidate state). Emulator API 26/30/37
evidence existed; physical-device and performance gates were blocked.

## Work completed

- Created the gitignored local blocker tracker `docs/BLOCKERS.local.md`;
  confirmed it is ignored by Git.
- Connected a physical low/mid-range device: motorola moto g(40) fusion,
  Android 12 / API 31, arm64-v8a, 3.7 GiB RAM, 1080×2460 400 dpi.
- Ran Android runtime (37), reporter (11) and sample (2) tests on the device:
  all passed.
- Ran the release smoke on the device: capture → editor → exact review →
  encrypted draft → SAF export.
- Pulled the export and validated/inspected/rendered it with the desktop CLI.
  The release parcel reports `variant: release` on API 31.
- Added `:sample-baseline`, a Compose app with the sample shell and no SDK
  dependency, plus a `benchmark` build type and Macrobenchmark startup/jank
  scenarios.
- Added a paired SDK-uninitialized startup scenario in the sample app using a
  benchmark-only intent extra.
- Added a physical-device capture latency/allocation test.
- Added the baseline and benchmark variants to CI.
- Closed G-06 locally: airplane-mode release create → SAF export → offline
  desktop validate/inspect/render on the API 37 emulator.

## Verification evidence

```text
ANDROID_SERIAL=$PHYSICAL_DEVICE ./gradlew \
  :reproparcel-android:connectedDebugAndroidTest \
  :reproparcel-compose:connectedDebugAndroidTest \
  :sample-app:connectedDebugAndroidTest
PASS: 37 + 11 + 2 tests, 0 failures

release smoke: capture/editor/review/draft/SAF export PASS
desktop: validate valid=true; inspect variant=release; render PASS

scripts/ci.sh PASS

offline airplane mode: ping unreachable
validate: {"valid":true,"reportId":"644dd60d-dcfa-4d7d-94b3-5f21731cf33f"}
inspect:  variant=release, descriptionLength=16
render:   PASS
```

Initial startup observation (not final): integrated median 558.7 ms, simplified
baseline median 539.9 ms. The delta is 18.8 ms, above the 10 ms target, but the
baseline UI was not identical, so this is not a G-03 result. The paired
same-app scenario is implemented but had not run when the device disconnected.

## Privacy, reliability and compatibility impact

The repository contains only synthetic data. No device serial, personal path or
real report was committed. The physical test verified the release-variant
capture/privacy/draft/export path. The baseline module contains no ReproParcel
SDK dependency.

## Incomplete work and blockers

- Device disconnected during the paired benchmark rerun; reconnect and authorize
  USB debugging to finish startup p95, jank, capture p95/allocation, thermal and
  TalkBack checks.
- Physical API 26/30 rows are not available; emulator coverage remains.
- Two real MCP clients, two RN consumers, iOS device, relay deployment, legal,
  Sonatype and publication approvals remain blocked.
- G-03 is not passed; the initial delta is above target and needs a fair paired
  measurement plus optimization/ADR if it remains above 10 ms.

## Next exact action

Reconnect and authorize the physical device, run the paired startup and jank
benchmarks plus capture performance and TalkBack checks, record aggregate
results, then decide whether SDK initialization needs optimization or an
evidence-backed ADR. Do not release.

## Resume/rollback notes

Read `AGENTS.md`, `docs/PROJECT_STATE.md`, this handoff, the latest audit and
`docs/BLOCKERS.local.md` (local, ignored). The physical device is a test input,
not a repository artifact. No tag/release/`main` action is authorized.
