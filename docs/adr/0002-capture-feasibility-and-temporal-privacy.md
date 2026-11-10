# ADR 0002 — Capture feasibility and temporal privacy

- Status: Accepted for Phase 0; image capture is feasible with mandatory policy gating
- Date: 2026-11-11
- Deciders: ReproParcel maintainers

## Context

PixelCopy's `Window` overload is available from API 26. The product must bind
captured pixels to the policy and bounds used for masking. Pre/post geometry
checks alone are not proof of frame identity. A successful bitmap copy is not
proof that the image is safe.

## Decision

1. Use `PixelCopy.request(window, bitmap, listener, handler)` for eligible host
   windows. Do not use hidden APIs, reflection, `RootForTest` or test-only hooks
   in the shipping path.
2. Check `WindowManager.LayoutParams.FLAG_SECURE` and the host capture policy
   **before** calling PixelCopy. **Raw PixelCopy can return `SUCCESS` on a
   `FLAG_SECURE` window**; the Phase 0 spike observed `SUCCESS` with a
   `#FAFAFA` center pixel. Never rely on PixelCopy failure as a privacy check.
3. Every asynchronous attempt carries:
   - a unique attempt token,
   - the active window identity,
   - the host privacy-policy generation,
   - the layout/scroll generation when available.
4. Invalidate and discard an attempt on scroll, layout/content change, dialog
   or window replacement, policy change, cancellation, host destruction or a
   late callback. Pre/post geometry equality is not sufficient proof.
5. If a public API cannot establish safe consistency for a surface, omit image
   collection there. Text-only reporting remains available.
6. Apply host-policy masks as opaque, destructive pixels on the sanitized base
   before persistence. The editor's undo may affect only user-added
   annotations; it must not restore prohibited pixels.
7. Bound each capture to a 2-second timeout and at most one retry for a
   documented transient result. Recycle temporary bitmaps in `finally`/scoped
   cleanup and never persist a raw frame.
8. A capture failure returns a typed omission reason. It never fabricates a
   blank "successful" image and never blocks the report form.

## Phase 0 evidence

- Debug instrumentation passed on API 26, API 30 and API 37 (emulators):
  10 tests covering capture, full-region opaque masking, protected-window
  omission, rotation, raw-bypass observation and Compose semantics. The
  minified release smoke passed on API 26 and API 37.
- Release-minified APK smoke on API 37 (manual UI automation):
  `capture: SUCCESS 1080x2400 in 15ms; masked=#FF000000; saved=true`.
- Release-minified secure-window smoke:
  `respect=true -> SECURE_WINDOW FLAG_SECURE is set; capture omitted`.
- Raw PixelCopy on the protected window returned
  `respect=false -> SUCCESS bytes=10368000 center=#FFFAFAFA`, confirming that
  the pre-check is load-bearing.

## Limitations

The production implementation must satisfy
[the Phase 2 capture contract](../security/phase-2-capture-contract.md),
including post-copy `FLAG_SECURE`/policy re-checks, frame binding, bounded
allocation and typed exception boundaries. The spike's raw experiment is
test-only and must not be promoted into the SDK API.

- These are emulator results, not physical low/mid-range device evidence.
- Rotation was tested with a single-window Compose screen; multi-window,
  SurfaceView, WebView and mixed View/Compose surfaces still need dedicated
  tests in Phase 2.
- The spike did not prove frame identity against a scrolling content change;
  Phase 2 must implement the attempt-generation contract and race fixtures.
- The release JUnit test-APK path was unstable under R8; release smoke was
  performed with the minified app and UI automation. Phase 5 must add a stable
  release-device test harness.
