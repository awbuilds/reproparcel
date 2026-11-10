# ADR 0010 — Encrypted local draft storage

- Status: Accepted for Phase 2
- Date: 2026-11-11
- Deciders: ReproParcel maintainers

## Decision

Drafts use a small app-private file store, not Room:

```text
files/reproparcel/drafts/index.json       id, createdAtMs, encrypted size only
files/reproparcel/drafts/<id>/report.enc  AES-GCM
files/reproparcel/drafts/<id>/image.enc   AES-GCM (optional flattened PNG)
```

- Key: Android Keystore alias `reproparcel.draft.v1`, AES-256-GCM, randomized
  IV, no padding, AAD `reproparcel-draft-v1`.
- Each file begins with a versioned magic/format header and a bounded IV.
- Writes are atomic temp-file + rename + `fsync`; partial files are removed.
- Retention: default 72 hours, at most 10 reports and 25 MiB. Expired/oldest
  entries are pruned before save/load/list.
- The index contains no report text, title, screen ID or image content.
- Key/crypto failure returns `ENCRYPTION_UNAVAILABLE`; there is no plaintext
  fallback. The reporter may keep an in-memory draft only.
- Corrupt/tampered ciphertext returns a typed failure and is not rendered.
- Deletion is explicit and removes the local draft directory. It cannot revoke
  a file already shared, backed up or copied.
- The host must exclude app-private drafts from backup/transfer. The sample
  disables backup; integration guidance repeats this requirement.

## Evidence

Instrumented tests cover round-trip, encrypted file does not contain plaintext,
tampered ciphertext fails closed, deleted key fails closed, quota eviction and
TTL pruning. The full process-death and full-disk cases remain Phase 5/manual
work.
