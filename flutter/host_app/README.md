# Flutter Android host (reference)

Minimal Flutter host that consumes `../reproparcel_flutter` as a path dependency.

```sh
flutter pub get
flutter build apk --debug
```

The APK builds and installs. Native MethodChannel capture is not implemented;
see `docs/evidence/phase-9/flutter-host.txt`.
