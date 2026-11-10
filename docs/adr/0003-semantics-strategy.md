# ADR 0003 — Semantic evidence strategy

- Status: Accepted for Phase 0; explicit host registration is the v1 baseline
- Date: 2026-11-11
- Deciders: ReproParcel maintainers

## Context

Compose semantics do not represent every UI element. Merged nodes, lazy
offscreen content, custom Canvas content, separate windows and WebViews impose
limits. AndroidX documents `RootForTest` as a test interface. Production
extraction through test hooks, reflection or hidden internals is prohibited.

The Phase 0 spike compared:

1. app-level public accessibility traversal using
   `View.createAccessibilityNodeInfo()` and `getChild`, and
2. test-only Compose semantics coverage using the Compose UI test API.

## Decision

Ship **explicit host-registered semantic descriptors and bounds** as the v1
baseline. Automatic Compose/accessibility extraction is not a v1 capability
unless a later public-API, minified-release compatibility matrix passes.

The v1 descriptor contract must:

- accept only allowlisted fields: role, static label, test tag, bounds, state,
  parent relation and an explicit sensitive/excluded flag;
- omit editable/password values and sensitive subtrees;
- record the coordinate space and capture dimensions for bounds;
- bound snapshots to 500 nodes, depth 32 and 512 KiB serialized;
- record truncation and omission reasons;
- never claim a complete UI tree.

## Phase 0 evidence

- App-level traversal returned an unsealed node; child traversal raised
  `IllegalStateException: Cannot perform this action on a not sealed instance`.
  The spike records this limitation rather than treating it as a complete tree.
- Test-only Compose semantics found the visible labels, synthetic input, custom
  Canvas content description and dialog content. That proves the semantics
  exist for test observation; it does not prove a production extraction path.
- Release-minified smoke confirmed the UI still renders and captures, but did
  not establish a production semantics extractor.

## Consequences

- The sample app and adopters register safe descriptors explicitly.
- The product must not market automatic semantic extraction as solved.
- A future automatic adapter needs a separate ADR, public-API evidence and a
  minified-release compatibility matrix; it remains optional.
