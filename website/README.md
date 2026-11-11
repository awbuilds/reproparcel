# ReproParcel website

The static project site: home, about, privacy policy, terms of service,
changelog, roadmap, security, contributing, license, support and the full
documentation section.

**No build step, no npm packages, no external requests.** Every page is
hand-written semantic HTML with one stylesheet and one small script. Fonts are
self-hosted; there are no CDNs, analytics, cookies or third-party embeds.

## Design system

The site is an evidence dossier: dark archive green, bone ink, stamped yellow
seals, hairline rules and paper exhibit plates. The visual language is
deliberate — see "Editing rules" before changing it.

| Role | Face | Source |
|---|---|---|
| Display and UI | Archivo (variable, `wght` + `wdth`) | Google Fonts, SIL OFL 1.1 |
| Long-form prose | Source Serif 4 (variable) | Adobe, SIL OFL 1.1 |
| Code, data, labels | JetBrains Mono (variable) | JetBrains, SIL OFL 1.1 |

Font files are the Latin subsets in `assets/fonts/` with their license texts.
They are loaded with `@font-face` and `font-display: swap`; no hosted font
stylesheet is requested at runtime.

## Preview locally

```sh
python3 -m http.server 8080 --directory website
```

Then open <http://localhost:8080/>.

## Deploy to GitHub Pages

The site is designed to publish `website/` as the Pages root.

1. In **Settings → Pages**, set **Source** to **GitHub Actions**.
2. Run the **Website** workflow (`.github/workflows/pages.yml`) manually, or
   enable its push trigger as described in the workflow comments.
3. The default project URL is <https://awbuilds.github.io/reproparcel/>.

If you publish at a different origin — for example a custom domain — update the
`SITE` constant used for canonical and social URLs, `website/sitemap.xml`, and
the absolute paths in `website/404.html`. Page-to-page links and asset
references are relative and keep working.

## Structure

```text
website/
  index.html                  home
  about/ privacy/ terms/      project and legal pages
  changelog/ roadmap/         change history and gates
  security/ contributing/     policy and community pages
  license/ support/           license summary and help
  docs/                       docs hub
  docs/<section>/             architecture, compatibility, performance,
                              integration, troubleshooting, faq
  404.html                    GitHub Pages fallback
  robots.txt sitemap.xml site.webmanifest
  assets/css/site.css         single stylesheet
  assets/js/site.js           theme, mobile nav, copy buttons
  assets/fonts/               self-hosted Archivo, Source Serif 4,
                              JetBrains Mono (Latin subsets) + OFL texts
  assets/img/                 logo, favicon, social preview
  assets/screenshots/         existing synthetic screenshots
```

## Content provenance

The pages summarize repository documents. The repository is authoritative when
they disagree:

| Page | Source |
|---|---|
| Home, About | `README.md`, `AGENTS.md`, `docs/PROJECT_STATE.md` |
| Privacy | `docs/privacy-policy-example.md`, `docs/security/threat-model.md` |
| Terms, License | `LICENSE`, `NOTICE`, `docs/adr/0015-relicense-to-apache-2.0.md` |
| Changelog | `CHANGELOG.md` |
| Roadmap | `docs/ROADMAP.md`, `docs/audits/final-audit.md` |
| Docs | `docs/ARCHITECTURE.md`, `docs/compatibility.md`, `docs/performance.md`, `docs/integration/`, `docs/troubleshooting.md`, `docs/FAQ.md` |
| Security | `SECURITY.md`, `docs/security/threat-model.md` |
| Contributing | `CONTRIBUTING.md`, `GOVERNANCE.md`, `CODE_OF_CONDUCT.md` |
| Support | `SUPPORT.md` |

When a source document changes, update the corresponding page in the same
change. Do not introduce a claim the repository does not support.

## Editing rules

- **Status honesty.** There is no stable release, tag or Maven Central
  publication. Keep the status strip and the "source candidate" language.
- **License.** The project is Apache-2.0. Do not describe it as copyleft, and
  do not describe it as obligation-free.
- **Privacy.** Do not add analytics, tracking pixels, external fonts, CDN
  scripts, embedded video or social widgets. `scripts/check_website.py`
  rejects non-local resource references and non-allowlisted external hosts.
- **Data.** No real reports, screenshots, credentials, personal information or
  private application references. Use synthetic content only.
- **Legal pages.** `privacy/` and `terms/` are plain-language project text, not
  legal advice. Legal review remains a release gate.
- **Accessibility.** Keep one `<h1>` per page, a skip link, visible focus, a
  logical heading order and 44 px minimum interactive targets.
- **Visual language.** Keep the dossier system: square corners (2 px), hairline
  rules, yellow seals for evidence states, serif for long-form reading, mono
  only for code, data and artifact labels. Do not add gradient text, pill
  buttons, icon-card grids, eyebrow labels above headings or decorative
  shadows. The redaction bars and the parcel object are the two signature
  elements; keep them meaningful, not ornamental.
- **Fonts.** Do not switch to a hosted font service or a system-display face.
  Update the `@font-face` files and their OFL texts together.

## Verification

```sh
python3 scripts/check_website.py
./scripts/public-hygiene.sh --all
python3 scripts/check_docs.py
```

`check_website.py` verifies internal links and assets, required pages, the CSP
meta tag, local-only resource references, the external-host allowlist and the
presence of the pre-release disclaimer.

## License

Apache-2.0. See `../LICENSE` and `../NOTICE`.
