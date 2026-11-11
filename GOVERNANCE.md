# Governance

ReproParcel is maintainer-led. The goal is a small, reviewable, privacy-first
project with explicit decisions rather than implicit conventions.

## Maintainers

Maintainers are responsible for:

- reviewing and merging changes;
- keeping the PRD, traceability ledger, compatibility matrix and handoffs
  accurate;
- protecting the fail-closed privacy and host-stability invariants;
- approving releases and any change to licensing or distribution;
- handling private security reports.

The current maintainer is the owner of the `awbuilds/reproparcel` repository.
Contributors do not gain release authority by submitting a pull request.

## Decision making

- Small changes use lazy consensus in an issue or pull request.
- Load-bearing changes require an ADR in [`docs/adr/`](docs/adr/):
  architecture, capture/privacy behavior, storage, export, schema meaning,
  licensing, release process or public API compatibility.
- A maintainer makes the final decision when consensus is not reached.
- Decisions and their evidence are recorded in the repository, not only in
  chat.

## Contribution certification

Contributions are accepted under **AGPL-3.0-only** and use the Developer
Certificate of Origin. Sign off each commit:

```sh
git commit -s
```

There is no copyright assignment or contributor license agreement. A new CLA,
relicensing or proprietary exception requires an explicit maintainer decision
and consistent legal review.

## Branch and release model

- The implementation branch is `dev`.
- Do not create, merge into or push `main` during the pre-release phase.
- Pull requests target `dev`.
- Tags, GitHub Releases, Maven Central publication and deployments require the
  maintainer's explicit authorization after the release gates pass.
- A release candidate must link to its audit, compatibility evidence and known
  limitations. Drafts are not releases.

## Review and verification

- Behavior changes need tests at the smallest relevant layer.
- Privacy, failure, cancellation and device-only checks cannot be replaced by
  JVM-only tests.
- A maintainer does not mark a check passed if it was not run.
- Public-repository hygiene, dependency allowlisting and the PRD audit are gate
  checks, not optional style checks.

## Security

Vulnerabilities are handled through GitHub's private vulnerability reporting.
Do not disclose sensitive evidence in a public issue. Confirmed privacy or host
stability defects can require disabling the affected feature until a patch is
available.

## Conflicts and changes to this document

Maintainers disclose relevant conflicts in the decision record. Changes to this
document use a pull request and, if they affect release authority or licensing,
an ADR.
