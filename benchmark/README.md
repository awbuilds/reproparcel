# Benchmark module

Macrobenchmark harness for the synthetic sample app. It compiles but has not
been run on a physical low/mid-range device.

```sh
./gradlew :benchmark:assembleDebug
# On a profileable release sample/device:
./gradlew :benchmark:connectedDebugAndroidTest
```

The PRD requires paired SDK-absent/disabled/enabled release builds, p95 startup,
capture memory, and 5×1,000-frame jank runs. Those measurements are **blocked**
until a physical device and release signing are available; this module is the
harness, not evidence that the budgets pass.
