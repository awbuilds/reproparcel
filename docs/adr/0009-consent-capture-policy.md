# ADR 0009 — Consent, capture policy and default collection

- Status: Accepted for Phase 1; production capture contract is Phase 2
- Date: 2026-11-11
- Deciders: ReproParcel maintainers

## Context

The reporter must never collect by default. Screenshot and semantic-text
collection are the highest-risk surfaces. A host must supply a policy before
they are enabled.

## Decision

1. Initialization and each optional collector are explicit. Loading a class,
   creating an activity or installing the library starts nothing.
2. `ReproParcelConfig.enabled` defaults to `false`. `EvidenceConfig` defaults
   every optional category to `false`.
3. Screenshot or semantic-text evidence without a `CapturePolicy` is an invalid
   configuration; initialization returns a typed failure and stays
   uninitialized.
4. Static screen/action IDs and nonunique build/device metadata may be enabled
   separately from image/semantic collection.
5. Sanitization occurs before buffering and again before persistence/export.
   The runtime drops unknown/unsafe attributes rather than storing arbitrary
   strings.
6. Privacy failure omits evidence. Encryption failure never falls back to
   plaintext. Export serialization failure never yields a partial parcel.
7. The image path must satisfy the Phase 2 capture contract, including
   `FLAG_SECURE` pre/post checks, frame/policy generation and fail-closed
   omission. Raw PixelCopy success is not privacy proof.

## Consequences

- The sample starts disabled and requires an explicit enable action.
- A host cannot accidentally turn on screenshots by installing the SDK.
- Phase 2 can add drafts and the reporter without changing the default-off
  policy.
