# ReproParcel React Native Android reference

**Status: reference bridge only. Not built, linked or tested against a React
Native app in this repository.** Android v1 remains the stable scope; this is a
Phase 7 gated expansion.

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

- TypeScript and Kotlin source are present.
- No React Native app was built or run in this session; the bridge is
  **BLOCKED/unverified** until a real host app passes release tests.
- iOS is not provided "for free"; iOS is a separate Phase 8 effort.
