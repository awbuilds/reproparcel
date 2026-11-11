# ReproParcel React Native Android reference

**Status: reference bridge built and run in `react-native/ReproParcelHost`
(React Native 0.88.1, release APK).** Initialize/enable/breadcrumb/reporter are
verified on an API 37.2 emulator. The Phase 7 exit (second consumer example,
Hermes/lifecycle matrix, Expo config plugin) remains blocked.

## API

```ts
import {
  initialize,
  setEnabled,
  showReporter,
  recordBreadcrumb,
  deleteLocalReports,
} from "reproparcel-react-native";

await initialize({
  enabled: false,
  packageName: "com.example.app",
  versionName: "1.0.0",
  versionCode: 1,
  variant: "debug",
  gitCommit: null,
  capturePolicy: {
    policyVersion: "rn-policy-v1",
    screenshotEnabled: false,
    semanticTextEnabled: false,
    eligibleSurfaces: ["home"],
  },
});

await setEnabled(true);
await recordBreadcrumb({ category: "navigation", actionId: "open_home", screenId: "home" });
await showReporter("home");
```

The native module calls the same Android runtime and Compose reporter. It does
not bypass policy or expose a write/upload API.

## Integration requirements

- New Architecture-compatible React Native release.
- Host app includes `reproparcel-android` and `reproparcel-compose` from the
  local/published candidate.
- `ReproParcelPackage()` registered in the host package list.
- Hermes/release build, lifecycle, mixed native/JS screens and typed errors must
  be tested before any claim.

## Verification status

- Android release host build/install passed.
- Native initialize, enable and safe breadcrumb passed; reporter launch reached
  the form, capture editor and exact-review screen.
- One consumer host only; the Phase 7 exit still requires two distinct consumer
  examples, a Hermes/lifecycle matrix and an Expo config-plugin path.
- iOS is not provided "for free"; iOS is a separate Phase 8 effort.
