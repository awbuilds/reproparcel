# Phase 2 capture contract (non-negotiable)

Status: required before the production image path is enabled.

The Phase 0 spike is disposable and deliberately exposes a raw experiment. The
production `reproparcel-android` implementation must not copy the spike's
caller-controlled bypass or its single-check pattern.

## Required properties

1. **No caller bypass.** `FLAG_SECURE`, policy classification and all mandatory
   masks are enforced inside the SDK. Test-only raw access lives in `androidTest`,
   never in the public runtime.
2. **Window identity.** Snapshot the target window token/identity, the
   `FLAG_SECURE` state/generation, policy generation and layout generation.
   Re-check them after the copy and before persistence; mismatch means omission.
   A host can toggle `FLAG_SECURE` without changing policy or layout.
3. **Frame binding and coordinates.** Bind the attempt to a draw/frame generation
   (or a verified equivalent) and to an explicit coordinate space/inset
   transform. Invalidate on scroll, layout/content change, dialog/window
   replacement, rotation, policy change, cancellation and late callbacks.
   Pre/post geometry equality alone is not proof.
4. **Bounded allocation.** Longest edge ≤1600 px, decoded-image budget ≤12 MiB,
   incremental capture peak ≤24 MiB. Compute target size before allocation;
   avoid a second full-size copy; measure and record peak.
5. **Fail-closed masking.** Opaque-only color, non-empty intersecting region,
   conservative padding, full-region pixel assertions, destructive application
   to the sanitized base, no raw source retained.
6. **Cancellation ownership.** Do not recycle a bitmap while PixelCopy may still
   write to it. Use a request-owned buffer and an explicit abandon protocol; add
   timeout/late-callback tests.
7. **Exception boundary.** Typed capture/save/export outcomes; catch specific
   expected exceptions at the SDK/host seam; rethrow `CancellationException`;
   clean up in `finally`; never throw into a host callback.
8. **Lifecycle and single-flight.** Gate on a usable foreground activity; one
   session/attempt at a time; no duplicate export after rotation, backgrounding
   or process death.
9. **No raw persistence.** Raw frames live only briefly in memory; cache/temp
   files are excluded; PNG output is flattened and metadata-free.
10. **Self-capture exclusion.** Capture eligible host content before reporter
    overlays; never include reporter/preview evidence in the parcel.

## Required tests

- Secure dialog and flag-toggled-mid-capture omission.
- Scroll/dialog/window replacement/rotation/late-callback invalidation.
- Full-region mask pixels, transparent/out-of-bounds/empty rejection and saved
  PNG inspection.
- Timeout/cancellation without double recycle or native crash.
- Large-window allocation bound and peak-memory measurement.
- Password/editable semantics subtree omission with synthetic secrets.
- Self-capture exclusion for reporter/preview windows.
- Coordinate-space/inset mapping on rotation and multi-window.
- Disk-full, corrupt draft, revoked URI and serialization failure.
- Release-minified device run on API 26 and current stable.

## Review gate

Independent security review must confirm these properties before image capture
is advertised as production-ready. Until then, text-only reporting remains the
safe path.
