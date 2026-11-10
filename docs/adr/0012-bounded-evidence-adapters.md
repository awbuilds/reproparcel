# ADR 0012 — Bounded evidence adapters

- Status: Accepted for Phase 3
- Date: 2026-11-11
- Deciders: ReproParcel maintainers

## Decision

- Breadcrumbs accept only category, safe action/screen IDs, allowlisted
  attributes and a short outcome. They never accept navigation arguments or
  entered values.
- Logs accept only level, safe code and allowlisted attributes. Unknown keys
  and unsafe values are dropped before the bounded buffer.
- The OkHttp artifact records method, status, elapsed time and a host-supplied
  safe route template. It never reads bodies, headers, query strings or raw
  URLs. It excludes SDK-owned traffic by not being installed on SDK clients.
  `chain.proceed` responses/exceptions are returned unchanged; sink failures
  are isolated and reported only as a safe class name.
- Semantic evidence is explicit host registration. Sensitive nodes retain no
  label/test tag/state. Snapshots are bounded to 500 nodes, depth 32 and
  512 KiB; truncation is reported. Automatic Compose/accessibility extraction
  remains unshipped until a public-API minified-release matrix passes.
- Provider results carry collected/omitted/truncated/unsupported status and a
  typed omission reason.
- Buffers enforce both count and byte limits and are cleared on disable/reset.

## Evidence

- Core buffer/redaction tests; runtime unsafe-attribute tests.
- OkHttp `MetadataInterceptor` tests with MockWebServer: response body/code
  preserved, sink failure does not change the response, route template only.
- Semantic registration tests: invalid bounds rejected, sensitive label/test
  tag omitted, bounded snapshot.
- Provider timeout/byte/exception tests.
