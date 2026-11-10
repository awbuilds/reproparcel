# API and schema freeze candidate

Date: 2026-11-11
Status: freezes proposed for the source candidate; not a published release.

## Artifacts

| Coordinate | Type |
|---|---|
| `io.github.awbuilds:reproparcel-core:0.0.1-SNAPSHOT` | JVM jar + sources |
| `io.github.awbuilds:reproparcel-android:0.0.1-SNAPSHOT` | AAR + sources |
| `io.github.awbuilds:reproparcel-compose:0.0.1-SNAPSHOT` | AAR + sources |
| `io.github.awbuilds:reproparcel-okhttp:0.0.1-SNAPSHOT` | AAR + sources |

Local repository verification only. Maven Central coordinates, signing and
publication remain blocked.

## Core API

The JVM public API is checked by `:reproparcel-core:apiCheck` against
`reproparcel-core/api/reproparcel-core.api`. Android/Compose public surfaces are
reviewed manually until a compatible API validator is available.

## Schema

Schema version 1 lives in `schema/v1/`. Required fields and unknown optional
field behavior are tested. Additive fields require compatibility fixtures;
meaning changes require a new schema version. No migration exists yet because
there is no prior stable schema.

## Versioning

- SDK SemVer is independent of `schemaVersion`.
- Release version is not set to `0.0.1` until the explicit maintainer tag
  instruction.
- Current development version is `0.0.1-SNAPSHOT`.
