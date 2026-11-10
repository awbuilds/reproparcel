# ReproParcel threat model (Phase 0 draft)

- Status: Draft; must be reviewed independently before release
- Scope: Android SDK, reporter UI, encrypted drafts, parcel export, desktop
  validator/viewer and read-only MCP server
- Out of scope: future relay/server (Phase 6), iOS/React Native/Flutter
  reference implementations, host application security

## Assets

- User-written report text and approved screenshots
- Host screen/action identifiers, build metadata and approved diagnostics
- Encrypted draft files and Keystore keys
- Parcel contents and checksums
- Host app stability, lifecycle and permissions
- Maintainer and adopter credentials outside the parcel

## Trust boundaries

```text
Host app ── explicit API ──► SDK runtime ──► encrypted draft ──► parcel
                                                               │
                                              SAF / share grant │
                                                               ▼
                                             desktop validator/viewer
                                                       │
                                                       ▼
                                             read-only MCP client
                                                       │
                                             client-selected model (egress)
```

The host app is trusted to configure policy but not to bypass it silently.
Report text is always untrusted data. The MCP client is outside the SDK's
control and may forward content to a cloud model.

## Threats and controls

| Threat | Required control |
|---|---|
| Secret survives structured logging | Allowlisted attributes; no bodies, headers, query strings or raw URLs; seeded redaction tests; final validation before export |
| Screenshot mask misses shifted content | Window/coordinate contract; conservative padding; rotation/scroll tests; attempt token and policy/layout generation; fail-closed omission |
| Raw PixelCopy bypasses protected window | Explicit `FLAG_SECURE` pre-check, post-copy re-check and policy/window generation; raw PixelCopy may return `SUCCESS`, including in-process; omit by policy, never rely on decoder failure |
| Report tricks an agent with instructions | Every field treated as untrusted data; no executable tool responses; viewer escapes text; MCP returns data, never instructions |
| Crafted ZIP traverses disk or expands excessively | Path allowlist; reject absolute paths, traversal, symlinks and duplicates; entry/count/ratio limits; streaming validation before extraction; isolated destination |
| Viewer executes HTML/JavaScript | Text-only rendering; HTML escaping; strict CSP; no external scripts, URLs or active attachment formats |
| Draft exposed through backup/provider | Keystore-backed AES-GCM; backup exclusions; narrow FileProvider paths; temporary read grants |
| SDK leaks activity or retains user data | Lifecycle cancellation; weak/scoped host references; bounded buffers; deletion/TTL tests |
| Agent requests unrelated files | Explicit import root; report-ID lookup; attachment allowlist; symlink rejection; no arbitrary paths |
| Fake report supplies false provenance | Mark provenance as supplied/unverified; checksums detect corruption, not authenticity |
| Sensitive data enters saved state | No report text/images in Bundles, `rememberSaveable`, navigation args, thumbnails, temp/editor files or diagnostics; restore only nonsensitive IDs |
| Reporter/preview self-capture | Capture before overlays; exclude SDK-owned windows; secure reporter/preview windows; never include the SDK's own report text in a parcel |
| Exception escapes into the host | Typed SDK-owned boundaries; catch specific expected exceptions; rethrow cancellation; `finally` cleanup; no throw into host callbacks; fault injection |
| Duplicate/concurrent capture or export | Foreground/lifecycle gate; single-flight/attempt token; idempotent export; no duplicate export after rotation/process death |
| Unbounded raw-frame memory | Bound target dimensions and decoded bytes before allocation; avoid a second full copy; recycle only after a safe callback/abandon protocol; measure peak |
| Semantic subtree leak | Allowlist descriptors; omit editable/password content and the entire sensitive subtree; bound node count/depth/serialized bytes; test synthetic secrets |
| Repository/CI artifact leak | Scan contents, file names, binaries/EXIF and commit identities; require explicit binary allowlist; run in CI/pre-commit |
| Share outcome overstated | Distinguish `ShareLaunched` from `FileSaved`; never claim receiving-app completion or remote receipt |
| MCP egress exceeds user expectation | Warn before import/access that the client may send content to a cloud model; recommend a local client for sensitive evidence |

## Abuse and misuse cases

- A host enables collection without a privacy policy: the SDK remains off until
  a policy is supplied; invalid configuration yields a typed disabled state.
- A malicious parcel contains a huge PNG or deeply nested JSON: reject before
  allocation using bounded dimensions, decoded-byte estimates and parser budgets.
- A report contains a prompt injection: viewer/MCP return it as escaped data;
  no tool is derived from report content.
- A stolen device contains drafts: drafts are encrypted and TTL-bound; deletion
  removes local drafts but cannot recall already shared files.

## Verification required

- Seed synthetic secrets in text, input fields, headers/bodies, URLs, logs,
  labels and screenshot regions; assert zero leaks in parcel bytes and pixels.
- Adversarial ZIP corpus: traversal, absolute path, symlink, duplicate,
  unreferenced entry, unsupported type, compression bomb, oversized PNG,
  malformed JSON, deep JSON, truncated archive.
- Capture race fixtures: scroll, dialog, window replacement, rotation, policy
  change, cancellation, late callback.
- Release-minified device smoke for capture/mask/export/import.
- Independent review of the Android and desktop import surfaces before release.

## Limitations

This draft is not a legal or security certification. It does not prove that all
personal data is detected or that every host surface is safe. A successful
bitmap copy is not proof of privacy. The final threat model must be reviewed
with the release evidence and kept current.
