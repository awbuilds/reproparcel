# Changelog

All notable changes are documented here. The project follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and uses semantic
versioning once a release exists. There is currently **no release or tag**.

## [Unreleased]

### Added

- Android modules: `reproparcel-core`, `reproparcel-android`,
  `reproparcel-compose` and `reproparcel-okhttp`.
- Schema v1 with bounded JSON, ZIP, image and semantic limits; synthetic valid
  and adversarial fixtures; cross-writer golden parcel.
- Explicit runtime: idempotent initialization, default-off collection,
  enable/disable, supervised jobs and typed outcomes.
- Allowlisted breadcrumbs, structured logs, optional OkHttp metadata and
  explicit semantic registration.
- Compose Material 3 reporter with manual entry, image editor, opaque masks,
  exact-content review, approval reset and encrypted drafts.
- Android Keystore AES-256-GCM draft storage with TTL/quota, tamper and key
  failure tests, no plaintext fallback and explicit deletion.
- SAF/FileProvider export with distinct `FileSaved` and `ShareLaunched`
  outcomes.
- Release-minified capture/export/import flow on API 26/30/31 physical/37.
- Desktop bounded validator, inspector, inert viewer and five-tool read-only
  MCP server.
- Optional relay reference with write-only project tokens, AES-GCM at rest,
  idempotency, quotas, revocation, content-free audit and a server-side GitHub
  adapter.
- React Native Android reference host, Flutter Android plugin/host and Swift
  package with simulator tests.
- English and Spanish reporter strings; opt-in foreground shake entry.
- Fault-injection suite for denied capture, missing activity/sensor, revoked
  URI, storage failure, corrupt draft, provider errors, timeout, cancellation
  and disconnected network.
- Physical low/mid-range benchmark evidence: SDK init p95 1.55 ms, capture p95
  211 ms / ~15.1 MB peak, trigger-to-composer p95 289 ms, paired 5×~1,050-frame
  jank pass and APK delta ~0.40 MiB.
- Public-repository hygiene gate, dependency allowlist, resolved dependency
  inventory, CodeQL/Dependabot configuration and documentation checks.

### Changed

- Relicensed first-party code and documentation from `AGPL-3.0-only` to
  `Apache-2.0` (ADR 0015); POM/package metadata, SPDX headers, LICENSE, NOTICE,
  README and the social preview were updated.
- Reporter editor uses a fixed action footer so review/back remain reachable
  with tall captured images.
- Release sample derives its build variant from `FLAG_DEBUGGABLE`.
- Desktop schema resolution prefers the module-relative schema root.
- Drafts moved to `noBackupFilesDir`; optional evidence flags are enforced.
- The sample and docs now distinguish crash-free/perfect-redaction claims from
  measured behavior.

### Security

- Raw PixelCopy can report success on a protected window; the production path
  checks `FLAG_SECURE` before and after capture and fails closed.
- The reporter marks its own window secure; screenshots are blocked by design.
- Parcel readers reject traversal, symlinks, duplicates, unreferenced entries,
  unsupported media, oversized/deep JSON and oversized PNGs before rendering.
- MCP is read-only, report-ID based and warns that client/model egress is
  outside the SDK's control.

### Known limitations

- No stable release, tag, GitHub Release or Maven Central publication.
- External interviews, the 20-defect pilot and two external integrations are
  blocked on participants.
- One real MCP coding client is verified; a second client is blocked by its
  model/network environment.
- A current physical device, physical API 26/30 rows, manual TalkBack matrix and
  longer thermal/ANR soak remain open.
- iOS physical-device/Keychain/scenes/App Store validation is blocked.
- The relay is a reference implementation and is not deployed.
- Legal/Apache-2.0/trademark review, Sonatype namespace verification and signed
  publication remain blocked.

[Unreleased]: https://github.com/awbuilds/reproparcel/commits/dev
