# Schema v1

- `schemaVersion` starts at 1 and is independent of SDK SemVer.
- Unknown optional fields are ignored safely. Required-field violations reject
  import with actionable errors.
- Unknown major/schema versions are not treated as compatible.
- Additive optional fields require compatibility tests. Meaning changes require
  a new schema version.
- Synthetic fixtures live under `fixtures/v1/`. No real user report is ever a
  fixture.

The manifest is the container authority: it lists every referenced entry with
its media type, uncompressed size and SHA-256. Checksums detect corruption; they
are not signatures or proof of origin.
