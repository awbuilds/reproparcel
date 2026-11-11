# Compatibility and support matrix

Date: 2026-11-11
Status: verification baseline, not a forever-version promise.

## Tested toolchain

| Component | Version |
|---|---|
| Gradle wrapper | 9.7.1 |
| Android Gradle plugin | 9.5.1 |
| Kotlin / KGP | 2.4.21 |
| Compose BOM | 2026.09.00 |
| Compose UI / Material 3 | 1.12.1 / 1.4.0 |
| compileSdk / targetSdk | 37 |
| minSdk | 26 |
| JDK | 21 environment, Java 17 bytecode target |

## Tested devices

| API | Device | Tests |
|---|---|---|
| 26 | `rp-api26` emulator | 40 Android runtime, 11 reporter, 3 sample |
| 30 | `rp-api30` emulator | 40 Android runtime, 11 reporter, 3 sample |
| 37.2 | `Android_17` emulator | 40 Android runtime, 11 reporter, 3 sample; R8 release smoke |
| 31 | physical low/mid-range Android 12 device | 40 Android runtime, 11 reporter, 3 sample; release capture/draft/SAF export/desktop import; startup/capture benchmarks |

A current physical device and API 26/30 physical devices are not available.
The low/mid-range device's paired jank run used five runs of ~1,050 frames each
and passed the 5%/1 pp gate; a longer thermal soak and TalkBack manual matrix
remain open.

## Compose support

The launch line is the pinned stable Compose line above. A preceding Compose
line is not claimed until release-build tests pass. No claim covers every OEM or
Compose version.

## Limits

- Drafts: 10 reports, 25 MiB encrypted, 72-hour default TTL.
- Parcel: 5 MiB compressed, 10 MiB uncompressed, 16 entries, 100:1.
- Images: 1600 px longest edge, 12 MiB decoded per image, 24 MiB concurrent.
- JSON consumer: 2 MiB, depth 32, 10,000 structural nodes.
- Semantic snapshot: 500 nodes, depth 32, 512 KiB.

## Known limitations

- Automatic Compose/accessibility extraction is not shipped; explicit host
  registration is the v1 baseline.
- Foreground shake is optional and implemented; the manual entry point remains
  the guaranteed control.
- The full temporal frame-binding proof and several race fixtures remain
  outstanding; image capture is fail-closed but not certified for every surface.
- Release smoke uses the minified app with UI automation; the release JUnit
  test-APK path is unstable under R8.
- TalkBack manual verification and a longer thermal/ANR soak remain open.
