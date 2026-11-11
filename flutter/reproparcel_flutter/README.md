# ReproParcel Flutter reference

**Status: Dart contracts and the Android Flutter plugin are tested; the Android
host APK builds and a device integration test passes.** Flutter iOS and the
wider Phase 9 exit gates remain blocked.

## What is present

- `lib/reproparcel.dart`: limits, image budget, allowlisted redaction, session
  state machine and report model. Tested with `dart test`.
- `lib/reproparcel_channel.dart`: `MethodChannel` API.
- `android/`: real Android Flutter plugin forwarding initialize/enable/
  breadcrumb/log/delete/reporter to the Android runtime and Compose reporter.

## Verification

```sh
dart pub get
dart analyze
dart test
```

The Android plugin and host build are verified on API 37.2. Flutter iOS,
platform views, release performance and additional adapters remain
**BLOCKED/unverified**.

## Constraints

- No automatic upload, telemetry or model call.
- No real report data in tests.
- Do not claim "works in Expo Go" or any Flutter platform support without a
  tested custom build.
