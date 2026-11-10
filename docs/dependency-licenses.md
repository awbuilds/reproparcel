# Dependency license review

Date: 2026-11-11
Status: provisional; complete generated notices and legal review are release
blockers.

## Runtime groups observed

| Group | License (declared/expected) | Used by | Review |
|---|---|---|---|
| `androidx.*` | Apache-2.0 | Android runtime/reporter | Compatible; preserve notices |
| `org.jetbrains.kotlin*` | Apache-2.0 | Kotlin runtime/compiler | Compatible |
| `org.jetbrains.kotlinx:kotlinx-*` | Apache-2.0 | serialization/coroutines | Compatible |
| `com.squareup.okhttp3`, `okio` | Apache-2.0 | optional OkHttp adapter | Compatible |
| `junit:junit` | EPL-1.0 | tests only | Compatible test-only |
| `org.hamcrest` | BSD-3-Clause | tests only | Compatible test-only |
| `com.networknt:json-schema-validator` | Apache-2.0 | core tests only | Compatible |
| `tools.jackson`, `com.fasterxml.jackson` | Apache-2.0 | schema test/desktop | Compatible |
| `org.slf4j` | MIT | schema validator logging | Compatible |
| `com.ethlo.time:itu` | Apache-2.0 | schema validator | Compatible |
| `com.google.guava`, `org.jspecify`, errorprone annotations | Apache-2.0 / MIT | transitive/test | Compatible |
| Desktop `ajv`, `ajv-formats`, `pngjs`, `yauzl`, `yazl`, `@modelcontextprotocol/sdk`, `zod`, TypeScript tooling | MIT/Apache-2.0 | desktop only | Verified at selected versions; regenerate package notices |

## Gate

`python3 scripts/check_dependencies.py` rejects dynamic versions and unreviewed
top-level groups. It is a coarse gate, not a legal opinion.

## Still required

- Generate complete third-party notices for the shipped AARs and desktop
  packages.
- Confirm every transitive license from the actual resolved graph.
- Review AGPL compatibility and distribution/source obligations with qualified
  counsel before a stable release.
- Do not add a dependency with incompatible, noncommercial or unknown licensing.
