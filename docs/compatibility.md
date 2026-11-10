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
| 26 | `rp-api26` emulator | 25 Android runtime, 2 reporter, 2 sample |
| 30 | `rp-api30` emulator | 25 Android runtime, 2 reporter, 2 sample |
| 37.2 | `Android_17` emulator | 25 Android runtime, 2 reporter, 2 sample; R8 release smoke |

A physical low/mid-range device and a current physical device are **not
available**; physical performance, thermal and OEM rendering evidence is
blocked.

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
- Foreground shake is optional and not implemented; the manual entry point is
  the guaranteed control.
- The full temporal frame-binding proof and several race fixtures remain
  outstanding; image capture is fail-closed but not certified for every surface.
- The R8 release test-APK path is unstable; release smoke uses the minified app
  with UI automation.
