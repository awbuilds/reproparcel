# Phase 0 toolchain and compatibility baseline

Date: 2026-11-11

## Verified environment

| Item | Observed |
|---|---|
| OS | macOS arm64, 18 GiB RAM, 46 GiB free at start |
| JDK | Eclipse Temurin 21.0.12.1; Java 17 bytecode target |
| Gradle | wrapper 9.7.1 |
| Android Gradle plugin | 9.5.1 |
| Kotlin / KGP | 2.4.21 |
| Compose BOM | 2026.09.00 (UI 1.12.1, Material3 1.4.0) |
| compileSdk / targetSdk | 37 / 37 |
| Build tools | 36.1.0 |
| minSdk | 26 |
| Emulators | API 26 (Google APIs arm64), API 30 (Google APIs arm64), API 37.2 (Play Store arm64) |

## Build evidence

```text
./gradlew :spike:capture-lab:assembleDebug      PASS
./gradlew :spike:capture-lab:assembleRelease    PASS (R8 + resource shrinking)
```

The release APK measured 1.2 MiB unsigned for the disposable spike; this is not
an SDK overhead measurement.

## Device evidence

```text
ANDROID_SERIAL=emulator-5556 ./gradlew :spike:capture-lab:connectedDebugAndroidTest
  rp-api26(AVD) - 8.0.0   OK (10 tests)

ANDROID_SERIAL=emulator-5558 ./gradlew :spike:capture-lab:connectedDebugAndroidTest
  rp-api30(AVD) - 11      tests=10 failures=0

ANDROID_SERIAL=emulator-5554 ./gradlew :spike:capture-lab:connectedDebugAndroidTest
  Android_17(AVD) - 17    tests=10 failures=0
```

Covered tests:

- PixelCopy returns a bitmap on an eligible window.
- Opaque mask replaces the target pixels; unmasked pixels remain.
- `FLAG_SECURE` is omitted by policy.
- Rotation produces a landscape window capture after reacquiring the activity.
- App-level accessibility traversal reports the unsealed-node limitation.
- Test-only Compose semantics finds core content and dialog content.

## Release-minified smoke (API 26 and API 37)

Manual UI automation against the R8-minified signed spike APK:

```text
API 37: capture: SUCCESS 1080x2400 in 15ms; masked=#FF000000; saved=true
API 26: capture: SUCCESS 1080x1920 in 21ms; masked=#FF000000; saved=true
respect=true -> SECURE_WINDOW FLAG_SECURE is set; capture omitted
respect=false -> SUCCESS bytes=10368000 center=#FFFAFAFA
```

The third line is a privacy finding: raw PixelCopy can return `SUCCESS` on a
protected window. The SDK must pre-check `FLAG_SECURE` and fail closed.

## Gaps

- No physical low/mid-range device is attached; no startup, jank, thermal or
  physical oldest-API evidence.
- The release JUnit test-APK path was unstable under R8; release smoke used the
  minified app with UI automation. A stable release test harness is Phase 5 work.
- Multi-window, SurfaceView, WebView and mixed View/Compose capture remain
  unverified.
- APK/dependency-size and license accounting for real SDK artifacts are later
  phases.
