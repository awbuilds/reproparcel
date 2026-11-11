# 0012 — 2026-11-11 — Open-source presentation and repository polish

## Objective and scope

Turn the public `dev` source candidate into a credible open-source project:
front-page README, original brand assets, synthetic screenshots, a navigable
document set, contribution/security/governance templates, repository metadata
and CI documentation checks. No release, tag, publication, deployment or `main`
branch was created.

## Starting state

`dev` at `204ede2` (`test: complete release matrix on api26 30 31 and 37`).
Final audit already recorded 231 PASS / 43 BLOCKED. The repository was public
with no description, topics, discussions or custom social preview.

## Work completed

### Front page and assets

- Rewrote `README.md`: banner, local badges, screenshots, problem statement,
  feature table, Mermaid architecture, module table, quick start (sample,
  runtime, desktop, MCP), privacy model, compatibility, measured performance,
  honest project status, document index, FAQ, contributing and license.
- Added original project-owned vector assets in `docs/assets/brand/`:
  `logo.svg`, `banner.svg`, `social-preview.svg` and the rendered
  `social-preview.png` (1280×640).
- Added seven static SVG badges in `docs/assets/badges/`; CI uses the GitHub
  Actions badge for this repository.
- Added reviewed screenshots in `docs/assets/screenshots/`:
  `reporter-form.png`, `reporter-editor.png`, `reporter-review.png` and
  `desktop-viewer.png`, plus `docs/assets/README.md`. All content is synthetic.
- Added the new binaries to `scripts/allowed-binary.txt`.
- Uploaded `social-preview.png` through the GitHub settings UI. The live
  `repository-images.githubusercontent.com` asset was downloaded and compared:
  identical SHA-256 (`694e0eea…b247`), 1280×640.

### Documentation

- Added `docs/README.md` (documentation index), `docs/ARCHITECTURE.md`,
  `docs/FAQ.md` and `docs/ROADMAP.md`.
- Rewrote `docs/performance.md` with the physical API 31 measurements and
  explicit limitations.
- Updated `docs/compatibility.md`, `docs/PROJECT_STATE.md` and `CHANGELOG.md`.
- Rewrote `CONTRIBUTING.md` and `SECURITY.md`; added `CODE_OF_CONDUCT.md`,
  `GOVERNANCE.md`, `SUPPORT.md` and `CITATION.cff`.
- Added `.editorconfig`, `.gitattributes`, `.markdownlint.json`,
  `.markdownlintignore` and `scripts/check_docs.py` (relative link/asset check).
- Added `.github/ISSUE_TEMPLATE/` (bug, feature, integration, config),
  `.github/PULL_REQUEST_TEMPLATE.md`, `.github/dependabot.yml` and workflows
  `docs.yml`, `dco.yml`, `codeql.yml`.

### Repository settings (via `gh` / GitHub UI)

- Description, 15 topics and Discussions enabled.
- Secret scanning, push protection, non-provider patterns, validity checks and
  Dependabot security updates enabled; private vulnerability reporting enabled.
- Labels `triage`, `integration`, `privacy`, `performance`, `capture`, `mcp`,
  `desktop` and `security` created for the issue forms.

### Product fixes found during screenshot capture

- The annotation editor laid the captured image under the header/toolbar and
  could push review/back off-screen. The image area is now scrollable and the
  action row is fixed below it, so review/back stay reachable with tall images.
- The sample copy now states that screenshot collection is enabled for the
  synthetic sample and semantic/network collection is disabled.
- `FLAG_SECURE` remains in the shipped reporter at
  `ReporterActivity.kt:91`. Screenshots came from a temporary local build with
  the flag removed for documentation only; that patch was reverted and is not
  committed.

### Evidence refresh

- Re-ran the full device matrix after the UI change: API 26 and API 30 emulators
  (40 runtime / 11 reporter / 3 sample each) and the physical API 31 device
  (11 reporter / 3 sample). Updated the corresponding
  `docs/evidence/phase-5/*.xml` files; device fields are
  `physical-device`/emulator names only.

## Verification evidence

```text
./scripts/ci.sh
ci: PASS

python3 scripts/check_docs.py
docs-check: PASS (101 links checked in 23 files)

npx --yes markdownlint-cli2@0.24.0 <documentation set>
Summary: 0 issues in 0 files

./scripts/public-hygiene.sh --all
public-hygiene: PASS (--all)

python3 scripts/prd_audit.py --final
{"BLOCKED": 43, "N/A": 594, "PASS": 231}
prd-audit: PASS

API 26 emulator: runtime 40/0, reporter 11/0, sample 3/0 failures
API 30 emulator: runtime 40/0, reporter 11/0, sample 3/0 failures
Physical API 31: reporter 11/0, sample 3/0 failures
```

Searches for absolute paths, IP addresses, device serials, private application
packages and the excluded conduct topic returned no project content. The only
package namespace found is `com.awbuilds.reproparcel.*`.

## Decisions

- Brand assets are original geometry; no third-party artwork or logos.
- Badges are local static SVGs so the README renders without third-party
  trackers; the only remote badge is this repository's own CI status.
- The social preview is uploaded manually because GitHub exposes no API for it.
- The documentation screenshot build is not shipped; the secure-window behavior
  is the real product behavior.
- OSS presentation contains no private adopter name, package ID, screenshot or
  identifier, and does not name the excluded conduct topic.

## Privacy, reliability, API and license impact

- No privacy or capture behavior changed. The only runtime change is editor
  layout; `FLAG_SECURE` and fail-closed capture are unchanged.
- No public API change was made.
- Repository content remains synthetic and public-safe; no credentials, personal
  data, absolute paths or real reports were added.
- License remains AGPL-3.0-only with DCO sign-off; no CLA, exception or
  relicensing was introduced.
- New GitHub workflows use least-privilege permissions and pinned major actions.

## Incomplete work and blockers

- All 43 audit rows that require humans, physical devices, external clients,
  legal review, Sonatype verification or publication remain `BLOCKED`.
- A current physical device, physical API 26/30 rows, the manual TalkBack matrix
  and a longer thermal/ANR soak remain open.
- iOS physical-device/App Store validation, relay deployment and a second real
  MCP client remain blocked.
- No release, tag, GitHub Release, Maven publication or deployment exists.

## Next exact action

The `dev` checkpoint is pushed. The maintainer must supply or authorize the
external/physical/legal/publication gates recorded in
`docs/audits/final-audit.md`. Do not tag, release, publish, deploy or create
`main` without an explicit instruction.

## Resume/rollback notes

Read `AGENTS.md`, `docs/PROJECT_STATE.md`, this handoff and the final audit.
Rollback is a revert of this checkpoint; no schema, data migration or external
state is involved. The only external state changed is GitHub repository metadata
and the social preview, which are reversible in repository settings.
