# ADR 0007 — Public API and runtime state contract

- Status: Accepted for Phase 1
- Date: 2026-11-11
- Deciders: ReproParcel maintainers

## Context

The SDK must be safe before initialization, idempotent, nonblocking and
host-navigation agnostic. A library class load must not start collection.

## Decision

The public runtime entry point is `ReproParcel` in `reproparcel-android`:

- `initialize(context, config, dispatchers)` validates config, is idempotent for
  the same config, returns `CONFIG_MISMATCH` for a different config, and never
  throws into the host for recoverable failures.
- `setEnabled(true/false)` explicitly starts or stops SDK-owned work. Disabling
  cancels the supervised scope. It does not delete drafts; deletion is separate.
- `showReporter(activity)` is main-thread checked. Without a usable foreground
  activity it returns `HostUnavailable`. In Phase 2 the Compose artifact exposes
  `ReproParcelCompose.showReporter(activity, surfaceId)`, which captures the
  eligible host window before opening `ReporterActivity`. The android-only
  stub remains `Unsupported` until a Compose launcher is registered; it never
  pretends a session started.
- `recordBreadcrumb` and `recordLog` accept only bounded, allowlisted structured
  fields. Unknown/unsafe attributes are dropped before buffering.
- `deleteLocalReports()` is an explicit operation. It is a safe no-op until
  encrypted drafts exist in Phase 2.
- `advanceSession(event)` exposes the deterministic session state machine for
  tests and Phase 2 integration.

Outcomes distinguish success, omission, failure, unsupported, host unavailable,
not initialized, disabled and cancellation. No empty catch, raw exception
message or success-after-failure is used.

## Consequences

- The sample initializes disabled and shows no active scope.
- Hosts can call the API before initialization and receive a typed result.
- The Phase 2 reporter can replace the `Unsupported` stub without changing the
  host contract.
- Public JVM API compatibility is checked by `apiCheck`; Android public surface
  is reviewed manually until a compatible API validator is available.
