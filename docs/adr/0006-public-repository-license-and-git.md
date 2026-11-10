# ADR 0006 — Public repository, license and Git workflow

- Status: Accepted
- Date: 2026-11-11
- Deciders: ReproParcel maintainers

## Context

ReproParcel is developed in a public repository. The project license is
AGPL-3.0-only. The working branch is `dev`; `main` is not used for this
implementation and must not be merged or pushed by the implementation agent.
No release tag or publication is authorized until an explicit instruction.

## Decision

- Keep the canonical engineering contract at `AGENTS.md`, including the hard
  public-repository hygiene section.
- License first-party code and documentation under AGPL-3.0-only; include the
  official license text in `LICENSE`.
- Do not include personal information, private application code, private
  adopter identifiers, real reports/screenshots or credentials in any committed
  artifact.
- Keep implementation work on `dev`. Do not create, merge into or push `main`.
- Use a project-neutral Git identity.
- Do not create tags, GitHub Releases, package publications, deployments or a
  `0.0.1` release until the maintainer explicitly instructs it.
- Local `publishToMavenLocal` and local package packing are verification tools,
  not releases.

## Consequences

- Commit history and handoffs are safe to publish.
- Release preparation can be tested locally without implying publication.
- The maintainer controls release timing and any later branch promotion.
