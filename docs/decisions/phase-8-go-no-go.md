# Phase 8 decision

Date: 2026-11-11
Status: **Swift core reference implemented and tested on macOS; UIKit capture is
conditional and unverified on an iOS device; App Store/RN iOS not done.**

## Verified

- Swift package builds on macOS.
- 5 XCTest tests: image budget, allowlisted redaction, session transitions,
  manifest JSON round-trip with unknown fields, report round-trip.
- Codable models share the same schema names and limits.

## Not verified / blocked

- UIKit capture (`IOSCaptureController`) is behind `#if canImport(UIKit)` and was
  not built for iOS or tested on a simulator/device.
- iOS screen protection, multiple scenes, Keychain storage, accessibility,
  performance and App Store policy were not validated.
- React Native iOS bridge is not implemented.
- No Apple credentials, signing, TestFlight or App Store submission.

Do not claim iOS support or parity with Android.
