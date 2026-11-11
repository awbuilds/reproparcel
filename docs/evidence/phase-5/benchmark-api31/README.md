# Physical benchmark evidence — low/mid-range API 31

Date: 2026-11-11
Device: motorola moto g(40) fusion (hanoip), Android 12 / API 31, arm64-v8a,
8 cores, 3.7 GiB RAM, 1080×2460 at 400 dpi. Battery at 98–100%, battery
temperature ~35 °C. No serial or network identifier is recorded.

## Startup

Release `benchmark` variant. Two paired approaches:

1. **Same app, same UI, custom intent** (`PairedStartupBenchmark`, 20 cold
   starts each): the SDK is initialized in one case and skipped in the other.
   Results were dominated by device noise/order:
   - run A: initialized median 591.2 ms, p95 668.3; uninitialized median
     675.9 ms, p95 726.4
   - run B: initialized median 660.2 ms, p95 709.6; uninitialized median
     685.3 ms, p95 760.1
   - counterbalanced separate runs: uninitialized-first median 675.7/p95 744.4;
     initialized-second median 618.5/p95 643.8
   The direction is inconsistent, so the full cold-start delta is not a
   reliable 10 ms measurement.
2. **Trace-section measurement of SDK initialization** in the release app
   (`StartupTraceBenchmark`, 20 cold starts): `ReproParcelInitialize` count=1
   per launch, sum median **1.28 ms**, p95 **1.55 ms**, max 1.60 ms.
   Total time-to-initial-display median 640.1 ms, p95 716.1 ms.

Conclusion for startup: the synchronous SDK initialization itself is well
below the 10 ms budget (p95 1.55 ms). The full cold-start comparison is too
noisy on this device to resolve a 10 ms difference; it is not evidence of a
regression.

## Capture

`CapturePerformanceTest` on the debug instrumentation build, 20 captures:

```text
p50 147 ms, p95 200 ms, max 215 ms
max allocation 4,492,800 bytes (4.49 MB)
```

Within the 2 s capture budget and 12 MiB decoded-image budget. This is a debug
build, so release latency should be no worse; a release-path timing remains
preferred.

## Jank

`ReporterJankBenchmark`, 5 runs of a reporter form scroll:

```text
frames: 202 total (about 40 per run)
frameDurationCpuMs: median 4.58 ms, p95 14.65 ms, p99 40.63 ms
frameOverrunMs:     median −5.68 ms, p95 14.27 ms, p99 37.67 ms
```

This is **not the PRD jank gate**: it is 202 frames, not 5 × 1,000, and there
is no paired SDK-disabled baseline. It shows the harness works, not that the
5%/1 pp frame-regression criterion passes.

## APK size

Minified release APKs:

```text
sample with SDK     1,289,328 bytes
SDK-absent baseline   871,225 bytes
difference            418,103 bytes (about 0.40 MiB)
```

The integrated APK also contains the reporter UI, so the difference is a
conservative upper bound; it remains below the 1 MiB core+Android target.

## Still open

- capture incremental peak memory (24 MiB) and UI-thread slice budget;
- trigger-to-composer p95;
- 5 × 1,000-frame paired disabled-baseline jank;
- thermal/ANR soak;
- TalkBack manual matrix.
