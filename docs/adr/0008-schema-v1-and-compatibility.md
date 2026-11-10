# ADR 0008 — Schema v1 and compatibility rules

- Status: Accepted for Phase 1
- Date: 2026-11-11
- Deciders: ReproParcel maintainers

## Context

The parcel is the durable contract between the Android writer, desktop reader,
viewer and MCP server. It must survive independent implementations and unknown
optional fields without silently accepting a different meaning.

## Decision

- `schemaVersion` starts at `1` and is independent of SDK SemVer.
- JSON Schema draft 2020-12 files live in `schema/v1/`.
- Unknown optional fields are ignored safely. Required-field violations reject
  import with actionable errors.
- Unknown major/schema versions are not compatible. Readers report supported
  versions.
- Additive optional fields require compatibility fixtures and tests. A meaning
  change requires a new schema version.
- Synthetic valid/invalid fixtures live under `schema/fixtures/v1/`; they are
  validated in JVM tests with a draft-2020-12 validator.
- The manifest is the container authority: every referenced entry has a path,
  media type, uncompressed size and SHA-256. Checksums detect corruption, not
  authenticity.
- Limits are constants in `reproparcel-core` and mirrored in the schema: JSON
  ≤2 MiB/depth 32/10,000 nodes; parcel ≤5 MiB compressed/≤10 MiB
  uncompressed/≤16 entries/100:1; images ≤1600 px/12 MiB decoded/24 MiB
  concurrent.
- A whole-parcel hash is computed externally after finalization when needed for
  consent binding or transport; the manifest cannot recursively hash itself.

## Consequences

- The Android writer and desktop reader share fixtures, not runtime classes.
- The schema can be implemented in another language without the Kotlin core.
- A schema change cannot be made silently; the ledger and fixtures make it
  reviewable.
