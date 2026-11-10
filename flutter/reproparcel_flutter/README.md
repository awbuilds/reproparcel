# ReproParcel Flutter reference

**Status: pure-Dart contracts are tested; no Flutter host/plugin build was
verified.** This is a Phase 9 gated expansion, not Android v1.

## What is present

- `lib/reproparcel.dart`: limits, image budget, allowlisted redaction, session
  state machine and report model. Tested with `dart test`.
- `lib/reproparcel_channel.dart`: reference `MethodChannel` API. It expects a
  native plugin that is not included/built here.

## Verification

```sh
dart pub get
dart analyze
dart test
```

Android/iOS Flutter plugin integration, release builds, platform views and
native capture remain **BLOCKED/unverified** until a real Flutter host passes
the same privacy/reliability gates.

## Constraints

- No automatic upload, telemetry or model call.
- No real report data in tests.
- Do not claim "works in Expo Go" or any Flutter platform support without a
  tested custom build.
