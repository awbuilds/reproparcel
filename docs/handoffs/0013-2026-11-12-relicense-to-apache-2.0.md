# 0013 — 2026-11-12 — Relicense first-party code to Apache-2.0

## Objective and scope

Change the project license from `AGPL-3.0-only` to `Apache-2.0`, update the
binding text and every current metadata surface, and record the decision. No
runtime behavior, public API, release, tag or deployment changed.

## Starting state

`dev` at `853c76b`; 30 commits; CI, Docs and CodeQL green; no tags or releases.

## Work completed

- Replaced `LICENSE` with the official Apache License 2.0 text. The file is
  byte-identical to `https://www.apache.org/licenses/LICENSE-2.0.txt`
  (SHA-256 `cfc7749b…23d30`).
- Rewrote the first-party statement in `NOTICE`, added the copyright line and
  Apache notice/patent/trademark obligations, and kept the dependency inventory.
- Added `docs/adr/0015-relicense-to-apache-2.0.md`; marked the license decision
  in ADR 0006 as superseded while preserving its historical text.
- Amended the PRD in place (868 lines preserved): license table, audience,
  adoption steps, release gates, risks, fixed decisions, references and
  §17.1–17.2 now describe Apache-2.0. The amendment date and prior license are
  recorded on the license-decision line.
- Regenerated the 868-row traceability ledger, refreshed override lock hashes
  and updated license-related override evidence; final audit stays
  **231 PASS / 43 BLOCKED / 0 UNREVIEWED / 0 IN_PROGRESS**.
- Replaced `SPDX-License-Identifier: AGPL-3.0-only` with `Apache-2.0` in 120
  first-party source, test, build and script files.
- Updated POM license metadata in `core`, `android`, `compose` and `okhttp`;
  npm `license` fields and lockfile roots for desktop, relay and the React
  Native package.
- Updated README, FAQ, AGENTS, CONTRIBUTING, GOVERNANCE, SUPPORT, CITATION,
  CHANGELOG, ROADMAP, PROJECT_STATE, dependency-license notes and the desktop
  README.
- Replaced the AGPL badge with `license-apache-2.0.svg` and updated the social
  preview SVG and 1280×640 PNG.
- Left historical handoffs, audits, decisions, research, impact briefs and
  evidence unchanged; ADR 0015 records the supersession.

## Verification evidence

```text
./scripts/ci.sh
ci: PASS

python3 scripts/check_docs.py
docs-check: PASS (101 links checked in 23 files)

npx markdownlint-cli2@0.24.0 <documentation set>
Summary: 0 issues in 0 files

./scripts/public-hygiene.sh --staged
public-hygiene: PASS (--staged)

python3 scripts/prd_audit.py --final
{"BLOCKED": 43, "N/A": 594, "PASS": 231}
prd-audit: PASS

shasum -a 256 LICENSE /tmp/apache-2.0.txt
cfc7749b96f63bd31c3c42b5c471bf756814053e847c10f3eb003417bc523d30  both

social preview: 1280x640, ~258 KiB
```

During the session the environment restored `docs/PRD.md` and the traceability
files to their pre-change snapshot while CI was running. The edits were
reapplied from the same source, the ledger and lock were regenerated, and the
final audit was rerun; the results above are from the reapplied tree. Build
outputs were unaffected because those files are documentation-only.

An untracked `website/` tree appeared during the session. It is not part of
this change and was left untouched; `public-hygiene.sh --all` reports its
unreviewed binaries until its owner reviews and allowlists them.

## Privacy, reliability, API and license impact

- No runtime, capture, storage, schema or public API behavior changed.
- Apache-2.0 is permissive and includes an express patent grant. Proprietary
  linking is allowed subject to copyright/license/NOTICE retention, marking
  modified files, and patent/trademark terms. No trademark license is granted.
- Anyone who obtained an earlier AGPL-licensed copy retains the rights that copy
  granted. No release or artifact was published under the earlier license.
- External legal review of notices, patents and trademarks remains a blocked
  release gate; this change is a maintainer decision, not legal advice.
- Repository content remains synthetic and public-safe.

## Incomplete work and blockers

- The GitHub social preview must be re-uploaded after the commit; GitHub has no
  API for it.
- Qualified legal review, external adoption interviews, the pilot, physical
  device rows, iOS validation, relay deployment, Sonatype verification and
  publication remain blocked.
- No tag, release, Maven publication or deployment exists.

## Next exact action

Push `dev`, verify the repository license detection, re-upload
`docs/assets/brand/social-preview.png` in GitHub settings, and confirm the
contributors/CI state. Do not tag or release without explicit authorization.

## Resume/rollback notes

Revert this commit to return to AGPL-3.0-only; prior history and handoffs remain
valid records. The change involves no schema or data migration. Historical
documents still describe AGPL as the license in force at their date.
