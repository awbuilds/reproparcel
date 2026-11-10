# Troubleshooting

## `Reporter` returns `NotInitialized` / `Disabled` / `Unsupported`

- Call `ReproParcel.initialize(context, config)` explicitly.
- `setEnabled(true)` is required; the default is disabled.
- For Compose hosts use `ReproParcelCompose.showReporter(activity, surfaceId)`
  after the runtime is enabled. The android-only `ReproParcel.showReporter` stub
  returns `Unsupported` until a Compose launcher is registered.

## Screenshot is omitted

- Confirm `CapturePolicy.screenshotEnabled = true` and the current surface ID is
  in `eligibleSurfaces`.
- A `FLAG_SECURE` window is always omitted; raw PixelCopy success is not proof
  of privacy.
- Changing layout/scroll during capture can invalidate the attempt.
- Very large windows can be omitted by the 24 MiB bound.
- A text-only report remains valid. Do not weaken the policy to force an image.

## Draft save fails

- `ENCRYPTION_UNAVAILABLE`: Keystore key invalidated or device lock changed.
  The reporter may keep an in-memory draft; there is no plaintext fallback.
- `LIMIT_EXCEEDED`: 10-report/25 MiB quota. Delete old drafts explicitly.
- `STORAGE_FAILED`: disk/IO failure. Check free space; do not retry as plaintext.

## Parcel rejected by the desktop tool

Run `reproparcel validate file.reproparcel --json`. Common codes:

- `CHECKSUM_MISMATCH` / `SIZE_MISMATCH`: corrupt or modified archive.
- `INVALID_PATH` / `SYMLINK` / `DUPLICATE_ENTRY`: hostile/unsupported ZIP.
- `UNREFERENCED_ENTRY`: manifest/container mismatch.
- `IMAGE_MEMORY` / `IMAGE_DIMENSIONS`: PNG exceeds consumer budget.
- `UNSUPPORTED_SCHEMA`: reader does not support the manifest version.

Do not bypass validation or extract into a shared directory.

## MCP returns no reports

- Start with `mcp --root <directory>` containing `.reproparcel` files.
- The server accepts report IDs, not arbitrary paths.
- Invalid/corrupt parcels are skipped by the indexer; validate them separately.
- Read-only MCP is not an egress guarantee; choose a local client for sensitive
  evidence.
