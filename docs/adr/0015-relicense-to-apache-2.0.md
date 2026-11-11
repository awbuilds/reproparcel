# ADR 0015 — Relicense first-party code to Apache-2.0

- Status: Accepted
- Date: 2026-11-12
- Deciders: ReproParcel maintainers
- Supersedes: ADR 0006 license decision; PRD §17.1–17.2 license terms

## Context

The project was previously licensed under `AGPL-3.0-only` and documented strong
copyleft obligations for adopters. The maintainer has decided to use the
permissive **Apache License, Version 2.0** for first-party code and
documentation instead.

The repository has no release tag, GitHub Release, published Maven artifact or
deployed service. The copyright holder is the sole author of the committed
code, and there are no external contributors whose consent would be required.
Third-party dependencies are Apache-2.0, MIT, BSD or test-only EPL-1.0 and
remain under their own terms.

This is a maintainer licensing decision, not legal advice. Qualified review of
distribution, notice and trademark obligations remains a release gate.

## Decision

- License first-party source, schemas, tooling, server code and documentation
  under `Apache-2.0`.
- Ship the official Apache License 2.0 text in `LICENSE` and retain the
  first-party copyright and attribution statement in `NOTICE`.
- Update SPDX headers, POM license metadata, npm package metadata and current
  documentation to `Apache-2.0`.
- Keep historical handoffs, audits, research and decision records as written;
  this ADR records the supersession.
- Keep inbound contributions under the same license with Developer Certificate
  of Origin sign-off. No CLA or copyright assignment is introduced.
- Continue to reject dependencies with incompatible, noncommercial or unknown
  licensing.

## Consequences

- Proprietary applications may link and distribute ReproParcel subject to the
  Apache-2.0 conditions: retain copyright, license and NOTICE attribution, mark
  modified files, and comply with the patent and trademark terms.
- Apache-2.0 includes an express patent grant and has no copyleft or network
  source-offer requirement.
- No trademark license is granted. The project name and marks may be used only
  for accurate, non-endorsing references unless separately permitted.
- Anyone who obtained an earlier AGPL-licensed copy keeps the rights that copy
  granted; this decision cannot revoke them. No release or artifact was
  published under the earlier license.
- Release gates must review Apache-2.0 notice, attribution, patent and
  trademark obligations instead of AGPL distribution terms.
- The traceability ledger and project state record the new license; the legal
  review gate remains blocked until a qualified human completes it.

## References

- [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
- [ADR 0006](0006-public-repository-license-and-git.md)
- [NOTICE](../../NOTICE), [LICENSE](../../LICENSE)
- [PRD §17](../PRD.md)
