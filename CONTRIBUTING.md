# Contributing to ReproParcel

## License and sign-off

Contributions are accepted under **AGPL-3.0-only**. Sign off each commit with
the Developer Certificate of Origin (`git commit -s`). Do not add a copyright
assignment or a conflicting license without an explicit maintainer decision.

## Public repository hygiene

This repository is public. Do not include:

- personal information, real names, personal emails, local absolute paths,
  account/device identifiers or credentials;
- real user reports, screenshots, production data or private application code;
- private adopter names, package IDs, screenshots or identifiers.

Use synthetic data and generic descriptions. Run `scripts/public-hygiene.sh`
before committing.

## Workflow

- Target the `dev` branch. Do not create, merge into or push `main`.
- Read `AGENTS.md`, the PRD, `docs/PROJECT_STATE.md`, the latest handoff and
  relevant ADRs before work.
- Write a short impact brief and failing tests before behavior changes.
- Keep changes reviewable; do not mix unrelated refactors.
- Do not commit, tag, release or publish on the maintainer's behalf.
- Update the traceability ledger and run the phase audit when a phase gate is
  claimed.

## Tests

Use the smallest relevant test layer. Privacy, failure, cancellation,
compatibility and resource checks are required in proportion to the change.
Device-only evidence cannot be replaced by JVM-only tests. Do not claim a check
passed if it was not run.

## Security

Report vulnerabilities through the private channel in `SECURITY.md`. Never put
sensitive evidence in a public issue, commit or test fixture.
