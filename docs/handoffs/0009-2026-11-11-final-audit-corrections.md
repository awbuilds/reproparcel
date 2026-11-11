# 0009 — 2026-11-11 — Final audit corrections and source-candidate freeze

## Objective and scope

Address the independent final review findings, re-run the final traceability
audit, and leave an honest source candidate. No release/tag/publication/deploy
or `main` merge was performed.

## Corrections made after review

- Reporter review now shows title and repro steps; approval resets when any
  exported field changes.
- “New report” clears text, annotations, final image, image inclusion and
  generates a new report ID/created timestamp.
- Parcel payload now includes bounded breadcrumbs, logs, network metadata and
  explicit semantics; `screenId` is carried from the host surface.
- `deleteLocalReports()` now deletes encrypted drafts and export cache and clears
  buffers; no false success.
- Optional evidence APIs respect `structuredLogsEnabled`,
  `networkMetadataEnabled` and `semanticTextEnabled`; negative tests added.
- Drafts moved to `noBackupFilesDir`; AES key size explicitly 256.
- Capture checks decoded/image budgets, mask bounds, window identity, policy
  version and enabled state; synchronous PixelCopy failures recycle.
- `ReproParcelCompose.showReporter` enforces main thread, single-flight and an
  exception boundary.
- Parcel writer/validator enforce aggregate uncompressed/compressed budgets.
- Relay rejects equal project/admin tokens; desktop MCP warnings added; numeric
  viewer fields escaped.
- Public hygiene ignores Swift/Dart build caches, scans history for private
  terms, and passes on the final tree.
- Traceability generator now scores requirement-bearing tables/lists in §§7, 8,
  13, 14; final audit has 211 PASS / 63 BLOCKED / 0 UNREVIEWED / 0 IN_PROGRESS.

## Verification evidence

```text
scripts/ci.sh                              PASS
Android runtime API 37                     31 tests, 0 failures
Reporter API 37                            10 tests, 0 failures
Sample API 37                              2 tests, 0 failures
Core JVM                                   43 tests, 0 failures
OkHttp                                     2 tests, 0 failures
Desktop                                    15 tests, 0 failures
Relay                                      6 tests, 0 failures
Swift core                                 5 tests, 0 failures
Dart core                                  4 tests, 0 failures
public-hygiene --all                       PASS
prd_audit --final                          PASS (211 PASS / 63 BLOCKED)
```

## Privacy, reliability and compatibility impact

The review surfaced and fixed a consent bypass (unreviewed title/repro), a
cross-report state leak on “New report”, false draft deletion, optional-evidence
flags not enforced, draft backup eligibility, capture bitmap ownership and
aggregate parcel bounds. Remaining blockers are external/manual or platform
gates, not hidden code claims.

## Incomplete work and blockers

- Physical device performance/jank, TalkBack/manual accessibility, full race and
  process-death matrix.
- Two real MCP clients; full Android export → desktop import on device.
- React Native host build; Flutter plugin host; iOS device capture.
- Relay deployment/TLS/backups/source offer and legal review.
- Maven Central namespace/signing/publication; explicit release authorization.

## Next exact action

The maintainer reviews the source candidate and supplies/authorizes the external
gates. Do not tag, release, publish or merge `main` until then.

## Resume/rollback notes

Read `AGENTS.md`, `docs/PROJECT_STATE.md`, `docs/audits/final-audit.md`, handoffs
0008/0009 and the phase decisions. `dev` is the only branch; no tags exist. The
working tree must remain sanitized; do not commit real reports, credentials or
private application code.
