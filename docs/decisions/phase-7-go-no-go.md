# Phase 7 decision

Date: 2026-11-11
Status: **Reference React Native Android host builds and runs; initialize,
enable, breadcrumb and reporter/capture/review were verified on API 37.2. The
Phase 7 exit is still BLOCKED for two consumer examples, Hermes/lifecycle matrix
and Expo config-plugin path.**

## Present

- Typed TS API for initialize/enable/reporter/breadcrumb/log/delete, with a
  shipped `lib/index.d.ts` and typed `ReporterResult`.
- Kotlin `NativeModule` wrapping the Android runtime and Compose reporter,
  dispatching reporter launch on the UI thread.
- Real host under `react-native/ReproParcelHost` and release build evidence.

## Not verified

- Only one consumer host; a second distinct consumer example is missing.
- No Hermes profile matrix, lifecycle/process-death matrix, mixed native/JS
  screen matrix or Expo config plugin.
- No iOS bridge; iOS is Phase 8.

Do not claim general React Native support or "Expo Go" compatibility. Two
consumer examples and the remaining matrix are required before Phase 7 passes.
