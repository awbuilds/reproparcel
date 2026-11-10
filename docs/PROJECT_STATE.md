# ReproParcel — Current project state

Updated: 2026-11-11.

## Current stage

**Planning baseline only.** The requested PRD and starter AGENTS.md have been written. No product source code, runnable SDK, public repository, package, domain registration or hosted service exists as a result of this work. Phases 0–5 define Android v1; none has been executed.

## Source of truth

- [PRD](PRD.md) — document version 1.0; native Android Kotlin/Compose; local-first reviewed evidence parcels; read-only local agent consumption.
- [AGENTS.md](../AGENTS.md) — exception boundaries, latest stable Android principles, task impact briefs, privacy and resumable handoffs.
- [Latest handoff](handoffs/0001-2026-11-11-prd-baseline.md) — baseline preparation, checks and next actions.

When a real repository is created, move the PRD to `docs/PRD.md` and update links. Do not duplicate independent copies that can drift.

## Verification status

Markdown structural checks passed: balanced fences, valid internal table-of-contents anchors, ordered phases 0–9, unique functional requirement IDs and mandatory topics. Independent full-file content review completed; its seven required corrections and optional clarity improvements were applied. These are documentation checks, not product implementation tests.

Android feasibility, production semantics access, resource budgets, legal/naming clearance, Maven namespace ownership, clean artifact consumption and all platform/runtime tests remain unverified acceptance work.

## Important decisions

- Working name: ReproParcel; preliminary checks are not trademark clearance or registration.
- Exact license in this baseline: AGPL-3.0-only; no proprietary-linking exception. A host application's or-later option is not inherited automatically.
- Android first. Proposed library minSdk 26 requires dependency and device validation.
- Manual local export in v1; no mandatory backend or runtime LLM.
- Sentry is real prior art; portability/local agent evidence is the differentiator to validate.
- Explicit semantic registration is the safe baseline; automatic extraction requires a public-API, minified-release feasibility gate.
- Comprehensive try/catch at recoverable boundaries; cancellation rethrown, fatal failures not falsely promised recoverable.

## Next exact action

The maintainer reviews the completed written PRD, especially exact license, capture scope and phase gates. Implementation begins only under a subsequent authorized task; start with Phase 0 product validation and synthetic Android feasibility, not a broad scaffold or production adopter integration.

## Safety boundaries

No real financial reports/screenshots, secrets, provider identity or personal paths in future repository records. No commits, pushes, package publication, public repository/domain creation or private-adopter production changes were requested or performed as part of the baseline.
