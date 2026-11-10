# Performance and resource measurements

Date: 2026-11-11
Status: **provisional emulator observations, not release budget evidence.**

## Environment

- API 37.2 arm64 emulator, 1080×2400, 420 dpi.
- Release-minified sample, debug-signed for local install.
- No physical low/mid-range device available.
- No paired SDK-absent baseline, thermal control or 5×1000-frame jank run yet.

## Observations

- Release sample APK: 1.2 MiB compressed (sample app, not an isolated SDK size).
- Cold/warm `am start -W` samples: 464/433/370 ms TotalTime.
- Capture controller: 2 s timeout, 100 ms settle before snapshot, one bounded
  image, downscale to 1600 px longest edge.
- Draft limit and parcel/image/JSON limits are enforced by code and tests.

## Not measured

- Incremental startup p95 ≤10 ms vs an SDK-absent app.
- Trigger-to-composer p95 ≤300 ms.
- Capture p95 ≤2 s and incremental peak ≤24 MiB on a named device.
- Frame p95/jank over 5 runs × 1,000 frames.
- APK overhead contribution by artifact.
- Thermal or low-memory behavior.

These are release blockers, not claims. Do not compare the emulator startup
numbers to the proposal budget.
