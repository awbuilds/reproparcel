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

`CapturePerformanceTest`, 20 captures on the debug instrumentation build:

```text
p50 141–147 ms, p95 179–211 ms, max 229 ms
max scaled allocation 4,492,800 bytes (4.49 MB)
source window allocation 10,627,200 bytes
estimated peak 15,120,000 bytes (~14.4 MiB)
native heap delta max 15,153,712 bytes (~14.5 MiB)
```

Within the 2 s capture budget and 24 MiB incremental-peak budget. The scaled
image is also within 12 MiB. This is a debug build; release latency should be no
worse.

## Main-thread collector cost

1,000 calls each, debug instrumentation:

```text
breadcrumb p95 176–185 µs
log p95       130–137 µs
network p95   152–196 µs
```

All are well below the 4 ms per-slice target.

## Trigger-to-composer

In-app timestamp from `showReporter` request to reporter first composition,
capture disabled, one untimed warm-up then 10 measured runs:

```text
cold first launch 480 ms
steady-state p50 244 ms, p95 289 ms, max 289 ms
```

Steady-state p95 is within the 300 ms budget. The cold first launch is reported
separately and is not part of this steady-state measurement.

## Jank — paired 5 × 1,000 frames

Same ScrollActivity UI, SDK enabled vs disabled, 5 cold runs × 80 swipes:

```text
enabled:  5 runs, 5,249 frames (1,034–1,066 per run)
disabled: 5 runs, 5,344 frames (1,051–1,090 per run)

frameDurationCpuMs p95: enabled 10.95 ms, disabled 10.95 ms (0.0%)
frameOverrun p95:       enabled −2.66 ms, disabled −2.62 ms
deadline miss:          enabled 1.01%, disabled 0.95% (+0.06 pp)
```

This passes the 5% p95-duration and 1 percentage-point deadline-miss gate.

## APK size

Minified release APKs:

```text
sample with SDK     1,289,328 bytes
SDK-absent baseline   871,225 bytes
difference            418,103 bytes (about 0.40 MiB)
```

The integrated APK also contains the reporter UI, so the difference is a
conservative upper bound; it remains below the 1 MiB core+Android target.

## Post-benchmark stress check

After the benchmark runs, the release ScrollActivity with the SDK enabled was
driven through 40 rapid swipes. The process stayed alive (`pid` present), battery
temperature was 33.8 °C, GPU temperature 40.2–45.4 °C, and the event/log buffers
contained no `am_anr`, `am_crash`, `FATAL EXCEPTION` or `ANR in` entries.

This is a short stress check, not a multi-hour thermal/ANR soak.

## Still open

- longer thermal/ANR soak;
- TalkBack manual matrix;
- release-path capture timing (debug timing used here).
