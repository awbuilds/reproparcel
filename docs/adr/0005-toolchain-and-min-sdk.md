# ADR 0005 — Toolchain, minSdk and compatibility baseline

- Status: Accepted for Phase 0; revalidate at each upgrade
- Date: 2026-11-11
- Deciders: ReproParcel maintainers

## Decision

Pin the following verified baseline in one version catalog and Gradle wrapper:

| Component | Version / value | Evidence |
|---|---|---|
| Gradle wrapper | 9.7.1 | `./gradlew --version` |
| Android Gradle plugin | 9.5.1 | build success; supports API 37 |
| Kotlin / KGP | 2.4.21 | build success with Compose compiler plugin |
| Compose BOM | 2026.09.00 (UI 1.12.1, Material3 1.4.0) | resolved build |
| compileSdk / targetSdk | 37 | installed platform `android-37.0` |
| Build tools | 36.1.0 (minimum 36.0.0) | installed |
| minSdk | 26 | PixelCopy `Window` overload; API 26 tests passed |
| JDK | JDK 21 environment; Java 17 bytecode target (Gradle toolchain block to be added in Phase 1) | AGP 9.5 requires JDK 17+ |
| Test devices | API 26, API 30, API 37.2 emulators | instrumentation results |

No dynamic versions, no alpha Compose/Material libraries, no mandatory Hilt,
Room, Play Services, navigation library or native binary in the baseline.

## Evidence

- `./gradlew :spike:capture-lab:assembleDebug` — pass.
- `./gradlew :spike:capture-lab:assembleRelease` — pass with R8 and resource
  shrinking.
- `connectedDebugAndroidTest` — 10 tests, 0 failures on API 26, 30 and 37.
- Release-minified API 26 and API 37 smoke: capture, opaque mask and
  protected-window omission observed through the UI.

## Open items

- Physical low/mid-range device is not attached; performance and thermal
  evidence is blocked.
- A Gradle Java toolchain block is not yet declared; Phase 1 must pin it so the
  build does not depend on the machine's default JDK.
- API 26/30 device images are emulator-only; physical oldest-API verification
  remains required by the release matrix.
- Dependency-license and artifact-size accounting are Phase 1/5 work.
