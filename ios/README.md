# ReproParcel iOS reference

Swift package with schema models, limits, allowlisted redaction, session state
and a conditional UIKit capture reference.

```sh
swift test
xcodebuild -scheme ReproParcel -destination 'platform=iOS Simulator,name=iPhone 18 Pro,OS=27.0' build
xcodebuild -scheme ReproParcel -destination 'platform=iOS Simulator,name=iPhone 18 Pro,OS=27.0' test
xcodebuild -scheme ReproParcel -destination 'generic/platform=iOS' CODE_SIGNING_ALLOWED=NO build
```

The simulator/device builds and 5 tests pass. Physical-device capture, Keychain
drafts, multi-scene behavior, App Store rules and RN iOS remain **BLOCKED**.
