# React Native Android host (reference)

Built with React Native 0.88.1. It initializes the bridge disabled, enables it
explicitly and records a safe synthetic breadcrumb. The release APK built and ran
on the API 37.2 emulator.

```sh
# Publish the local Maven candidate once from the repository root:
# ./scripts/release-local.sh
npm install
cd android
./gradlew :app:assembleRelease
```

Reporter launch, capture editor and exact-review screen were verified from this
host; see `docs/evidence/phase-7/rn-host.txt`. One consumer host is not the
Phase 7 two-consumer exit. No real report data is used.
