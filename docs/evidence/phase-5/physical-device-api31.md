# Physical low/mid-range device verification

Date: 2026-11-11
Device: motorola moto g(40) fusion (hanoip), Android 12 / API 31, arm64-v8a,
8 cores, 3.7 GiB RAM, 1080×2460 at 400 dpi. No device serial or personal data
is recorded.

## Passed on the physical device

```text
ANDROID_SERIAL=$PHYSICAL_DEVICE ./gradlew \
  :reproparcel-android:connectedDebugAndroidTest \
  :reproparcel-compose:connectedDebugAndroidTest \
  :sample-app:connectedDebugAndroidTest
BUILD SUCCESSFUL

runtime   37 tests, 0 failures
reporter  11 tests, 0 failures
sample     2 tests, 0 failures
```

Release-variant smoke:

- install and launch;
- Enable SDK;
- Open reporter;
- capture an eligible window;
- enter a synthetic description;
- open the image editor and reach `Exact final content`;
- save an encrypted draft (`Encrypted draft saved locally`);
- review a text-only report and export through SAF.

SAF export → desktop:

```text
validate: {"valid":true,"reportId":"3c454163-620b-4f33-9a63-c11b88299c4b"}
inspect:  "variant":"release", "apiLevel":31, "descriptionLength":23
render:   index.html
```

The pulled synthetic parcel was removed from the device after validation.

## Benchmark harness

The release `benchmark` variant and Macrobenchmark startup test ran on the
device. The first startup sample was:

```text
integrated median  558.7 ms
baseline median    539.9 ms   (simplified baseline, not the paired same-UI run)
```

This is **not acceptance evidence**: the baseline screen was not identical and
the paired uninitialized-same-app scenario had not run when the USB connection
dropped. A later run must record p95 and jank frame duration before any budget
claim.

## Pending because the device disconnected

- paired same-app SDK-uninitialized vs initialized startup;
- startup p95 (not just median);
- 5 × 1,000-frame reporter jank comparison against a disabled baseline;
- capture p95 and allocation on the physical device;
- thermal/battery notes;
- TalkBack manual verification;
- API 26/30 physical rows, if available.

No G-03 or stable-release claim is made from this partial evidence.
