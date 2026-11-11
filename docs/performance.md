# Performance and resource measurements

Date: 2026-11-11
Device: motorola moto g(40) fusion, Android 12 / API 31, arm64-v8a, 8 cores,
3.7 GiB RAM, 1080×2460 at 400 dpi. Release benchmark variant, wireless ADB,
98–100% battery, battery ~33.8–35 °C.

These are local measurements, not a universal performance guarantee.

## Results

| Metric | Result | Notes |
|---|---|---|
| SDK initialization | median 1.28 ms, p95 **1.55 ms**, max 1.60 ms | `ReproParcelInitialize` trace, 20 cold starts |
| Cold app startup | integrated median 591–676 ms | Full-startup delta was noise/order dominated |
| Capture latency | p50 141–147 ms, p95 **179–211 ms**, max 229 ms | 20 debug instrumentation samples |
| Capture allocation | scaled 4.49 MB; estimated peak **15.12 MB**; native delta 15.15 MB | Source window 10.63 MB + scaled image |
| Trigger-to-composer | steady-state p50 244 ms, p95 **289 ms** | Capture disabled, 10 runs after one warm-up; cold first launch 480 ms |
| Main-thread collector | breadcrumb 176–185 µs, log 130–137 µs, network 152–196 µs p95 | 1,000 calls each |
| Paired jank | 5 runs × 1,034–1,066 frames | enabled p95 10.95 ms vs disabled 10.95 ms; deadline miss 1.01% vs 0.95% (+0.06 pp) |
| APK delta | **+418,103 B (~0.40 MiB)** | minified sample vs SDK-absent baseline; conservative upper bound |

## Interpretation

- The synchronous SDK initialization is well below the proposed 10 ms startup
  sub-budget.
- Capture and trigger-to-composer are within the proposed 2 s and 300 ms
  budgets on this low/mid-range device.
- The paired jank result passes the 5% p95-duration and 1 percentage-point
  deadline-miss gates.
- The APK difference includes the reporter UI, so the SDK core/Android portion
  is below the reported ~0.40 MiB.

## Limitations

- Capture and collector measurements use debug instrumentation; release timing
  should be no worse but is not separately instrumented.
- The full cold-start comparison is too noisy on this device to resolve a 10 ms
  difference; the trace section is the stable SDK sub-budget measure.
- A current physical device, physical API 26/30 rows and a multi-hour
  thermal/ANR soak are not available.
- These measurements do not cover every OEM, Compose version, surface or
  accessibility setting.

Raw evidence: [benchmark-api31](evidence/phase-5/benchmark-api31/README.md).
