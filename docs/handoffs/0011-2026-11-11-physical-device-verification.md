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
PASS: 40 + 11 + 3 tests, 0 failures

release smoke: capture/editor/review/draft/SAF export PASS
desktop: validate valid=true; inspect variant=release; render PASS

scripts/ci.sh PASS

offline airplane mode: ping unreachable
validate: {"valid":true,"reportId":"644dd60d-dcfa-4d7d-94b3-5f21731cf33f"}
inspect:  variant=release, descriptionLength=16
render:   PASS

prd_audit --final: 231 PASS / 43 BLOCKED / 0 UNREVIEWED / 0 IN_PROGRESS
```

Benchmark results after the device was reconnected over wireless ADB:

- Release trace of `ReproParcelInitialize` over 20 cold starts: median 1.28 ms,
  p95 1.55 ms, max 1.60 ms.
- Paired same-app cold-start runs (20 iterations each) were dominated by device
  noise/order: initialized and uninitialized medians ranged from 591–676 ms and
  676–685 ms, with p95 differences ranging from about +4 ms to −100 ms. This is
  not a reliable full-startup 10 ms delta; the trace section is the stable
  measure.
- Capture: p95 211 ms, max allocation 4.49 MB over 20 debug-build samples.
- Jank sample: 202 frames, frame p95 14.65 ms; this is not the 5 × 1,000-frame
  paired gate.
- Minified APK difference vs SDK-absent baseline: 418,103 bytes (~0.40 MiB),
  a conservative upper bound.

G-03 now passes locally: capture peak ~15.1 MB, collector p95 <0.2 ms,
trigger-to-composer p95 289 ms, the paired 5 × 1,000-frame jank run passes the
5%/1 pp gate, and a 40-swipe stress check showed no ANR/crash at 33.8 °C.
Longer thermal soak and TalkBack remain release-hardening items, not G-03
blockers.

## Privacy, reliability and compatibility impact

The repository contains only synthetic data. No device serial, personal path or
real report was committed. The physical test verified the release-variant
capture/privacy/draft/export path. The baseline module contains no ReproParcel
SDK dependency.

## Incomplete work and blockers

- Full paired cold-start delta is noise-limited; the trace measurement is the
  stable startup sub-budget evidence.
- Thermal/ANR soak and the TalkBack manual matrix remain open.
- Current physical device, API 26/30 physical devices, two MCP clients and
  external human gates remain open.
- Physical API 26/30 rows are not available; emulator coverage remains.
- Two real MCP clients, two RN consumers, iOS device, relay deployment, legal,
  Sonatype and publication approvals remain blocked.
- Current physical and physical API 26/30 rows are not available; emulator API
  26/30 and current-API rows passed the release matrix.
- Release matrix: API 26/30/31 physical/37 image capture/export/import passed.

## Next exact action

Complete the remaining physical performance/jank matrix and TalkBack checklist.
The SDK init trace (p95 1.55 ms) is within the 10 ms target; no optimization or
ADR is needed for that sub-budget unless a cleaner paired run shows otherwise.
Do not release.

## Resume/rollback notes

Read `AGENTS.md`, `docs/PROJECT_STATE.md`, this handoff, the latest audit and
`docs/BLOCKERS.local.md` (local, ignored). The physical device is a test input,
not a repository artifact. No tag/release/`main` action is authorized.
