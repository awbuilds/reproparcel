# Contributing to ReproParcel

Thanks for helping improve a privacy-first reporting tool. This repository is
public; every contribution must be safe to publish.

## Before you start

Read:

- [AGENTS.md](AGENTS.md) — engineering contract and public-repository rules;
- [docs/PRD.md](docs/PRD.md) — requirements and gates;
- [docs/PROJECT_STATE.md](docs/PROJECT_STATE.md) and the latest
  [handoff](docs/handoffs/);
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) and relevant ADRs.

For a behavior change, open an issue or write a short impact brief before
coding. Load-bearing changes need an ADR.

## License and sign-off

Contributions are accepted under **Apache-2.0**. Sign off each commit with
the Developer Certificate of Origin:

```sh
git commit -s
```

There is no copyright assignment or CLA. Do not add a conflicting license,
proprietary exception or dependency without maintainer review.

## Public repository hygiene

Do not include:

- personal information, real names, personal emails, local absolute paths,
  account/device identifiers, IP addresses or credentials;
- real user reports, screenshots, production data or private application code;
- private adopter names, package IDs, screenshots or identifiers.

Use synthetic data and generic descriptions. Run the hygiene gate before every
commit:

```sh
./scripts/public-hygiene.sh --staged
```

Screenshots and binary fixtures require explicit review and an entry in
`scripts/allowed-binary.txt`.

## Workflow

- Target the `dev` branch. Do not create, merge into or push `main`.
- Keep changes focused; do not mix unrelated refactors.
- Add tests before or with the behavior change.
- Update the traceability ledger and run the phase audit when claiming a gate.
- Do not tag, release, publish or deploy on the maintainer's behalf.

## Build and test

Prerequisites: JDK 21, Android SDK platform 37/build-tools 36.1.0, Node 24+ for
desktop/relay tests and Flutter/Dart for the optional host.

```sh
./scripts/ci.sh
```

Useful narrower commands:

```sh
./gradlew :reproparcel-core:test
./gradlew :reproparcel-android:connectedDebugAndroidTest
./gradlew :reproparcel-compose:connectedDebugAndroidTest
./gradlew :sample-app:connectedDebugAndroidTest
./gradlew :benchmark:connectedBenchmarkAndroidTest
(cd desktop && npm test)
(cd server && npm test)
(cd ios && swift test)
(cd flutter/reproparcel_flutter && dart test)
```

Device-only evidence cannot be replaced by JVM-only tests. Do not claim a check
passed if it was not run.

## Code style

- Kotlin: 4 spaces, explicit public API in `reproparcel-core`, no wildcard
  imports, no blocking IO on the main thread.
- Compose: Material 3, shared semantic tokens, 48 dp targets, 200% font scale,
  edge-to-edge insets, localized strings.
- TypeScript/Dart: 2 spaces, strict types, no dynamic where avoidable.
- Shell/Python: POSIX-friendly, explicit failure handling; scripts are
  SPDX-tagged.

Run `./gradlew lint` and the docs checks:

```sh
python3 scripts/check_docs.py
npx --yes markdownlint-cli2@0.24.0 README.md CONTRIBUTING.md SECURITY.md \
  CODE_OF_CONDUCT.md SUPPORT.md GOVERNANCE.md docs/README.md \
  docs/ARCHITECTURE.md docs/FAQ.md docs/ROADMAP.md docs/compatibility.md \
  docs/troubleshooting.md docs/privacy-policy-example.md docs/api-freeze.md \
  docs/dependency-licenses.md docs/integration/desktop-tools.md \
  docs/integration/evidence-adapters.md docs/security/threat-model.md \
  docs/security/phase-2-capture-contract.md docs/traceability/README.md \
  docs/performance.md docs/assets/README.md
```

## Tests and evidence

Match the test layer to the risk:

- JVM: schema, limits, redaction, state, parcel validation.
- Android instrumentation: lifecycle, capture, masks, drafts, export, provider
  failures and accessibility.
- Physical device: release/R8 flow, performance, jank and thermal checks.
- Desktop: hostile ZIP/JSON/PNG inputs, viewer escaping/CSP and MCP protocol.
- Adapters: pass-through, cancellation and failure isolation.

Privacy or security changes need negative tests and a note in the PR template.
Do not include real evidence in test fixtures, screenshots or logs.

## Pull requests

Use the pull-request template. Include the impact brief, exact verification
commands, privacy/security impact and rollback notes. A maintainer may ask for
an ADR, a smaller change or additional device evidence.

## Security

Report vulnerabilities through GitHub's private vulnerability reporting; see
[SECURITY.md](SECURITY.md). Never put sensitive evidence in a public issue,
commit, discussion or test fixture.

## Community

Participation expectations are in [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).
Questions and design discussion belong in
[GitHub Discussions](https://github.com/awbuilds/reproparcel/discussions).
