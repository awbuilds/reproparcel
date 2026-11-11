# Flutter Android host (reference)

Minimal Flutter host that consumes `../reproparcel_flutter` as a path dependency.

```sh
# Publish the local Maven candidate once from the repository root:
# ./scripts/release-local.sh
flutter pub get
flutter build apk --debug
```

The APK builds and installs. The Android Flutter plugin forwards initialize,
enable, breadcrumb, log, delete and reporter calls; a device integration test
verifies them. See `docs/evidence/phase-9/flutter-host.txt`.
