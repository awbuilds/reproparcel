# ADR 0011 — Export, share and consent outcomes

- Status: Accepted for Phase 2
- Date: 2026-11-11
- Deciders: ReproParcel maintainers

## Decision

- Artifact approval and destination selection are separate steps.
- A content change invalidates prior approval. The reporter resets the review
  checkbox when the form, image selection or flattened content changes.
- The review screen shows the exact flattened PNG and text that will be
  exported. Text/image categories can be removed before approval.
- No consent checkbox is preselected.
- `FileSaved` is used only for a confirmed SAF write. The file may still be
  copied, backed up or shared by the user later; the SDK cannot recall it.
- `ShareLaunched` means the system chooser was opened. It does not claim that a
  receiving app accepted, received or opened the parcel.
- The SDK never reports "maintainer received it".
- Export serialization failure returns `ExportFailed`; no partial archive is
  presented as a valid parcel.
- FileProvider exposes only `cache/reproparcel/exports/` with temporary read
  grants. Temporary files are best-effort cleaned after 24 hours.
- The parcel itself is a bounded ZIP with a versioned manifest, allowlisted
  entries and SHA-256 checksums.

## Evidence

`ExportManagerTest` writes a parcel through a FileProvider URI and reads it
back; `ParcelWriterTest` verifies manifest entries and checksums. The UI tests
cover text-only review and encrypted draft save. The system chooser/SAF UI is
not automated; those remain manual release checks.
