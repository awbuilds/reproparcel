# Impact brief 0001 — Phase 0 feasibility

## Goal and phase

Serve Phase 0 of `docs/PRD.md`: prove the hard parts, establish the toolchain
and public-repository safety rules, and make a go/no-go decision without
claiming a shipped product.

## Affected callers, modules, schemas, policies, tests

- New disposable `spike/capture-lab`; no production SDK API yet.
- `AGENTS.md`, PRD, project state and handoff sanitation.
- Traceability and audit tooling.
- Capture, masking, secure-window and semantics feasibility tests.
- No schema v1 or public API exists yet.

## Failure, privacy, security, resource and license impact

- PixelCopy on a `FLAG_SECURE` window can report success; the pre-check is
  mandatory and the default is omission.
- No real user data is collected. All UI text and inputs are synthetic.
- No network, telemetry, backend, credentials or extra permissions.
- Emulator memory/CPU use is bounded; no physical-device performance claim.
- First-party code/docs use AGPL-3.0-only; no dependency is added to a shipped
  artifact yet. The spike uses AndroidX/Compose under Apache-2.0; notices are
  Phase 5 work.

## Compatibility and host behavior

- No host app integration exists. The spike is a separate application and does
  not modify any private adopter code.
- minSdk 26, compile/target 37; API 26/30/37 emulator tests passed.

## Smallest coherent change, acceptance and rollback

The smallest change is the disposable spike plus ADRs and evidence. Acceptance:
build passes, instrumentation passes on the three API levels, release smoke
captures/masks/omits correctly, and the semantics limitation is recorded.
Rollback: remove `spike/` and the Phase 0 docs; no production artifact or
host behavior is affected.
