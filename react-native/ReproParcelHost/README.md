# React Native Android host (reference)

Built with React Native 0.88.1. It initializes the bridge disabled, enables it
explicitly and records a safe synthetic breadcrumb. The release APK built and ran
on the API 37.2 emulator.

```sh
npm install
cd android
./gradlew :app:assembleRelease
```

Reporter launch from the bridge remains **unverified/BLOCKED** in this host; see
`docs/evidence/phase-7/rn-host.txt`. No real report data is used.
