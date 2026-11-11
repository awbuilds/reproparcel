#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Validate the static website: links, assets, policy and status honesty.

Checks:
- required pages exist;
- relative links and resource references resolve inside website/;
- no resource (script, stylesheet, image, font, manifest) loads from a remote host;
- anchor hosts are limited to an explicit allowlist;
- every page has a description, a CSP meta tag, a skip link and one main landmark;
- the pre-release disclaimer is present.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path
from urllib.parse import unquote, urlparse

ROOT = Path(__file__).resolve().parent.parent
SITE = ROOT / "website"

REQUIRED = [
    "index.html",
    "about/index.html",
    "privacy/index.html",
    "terms/index.html",
    "changelog/index.html",
    "roadmap/index.html",
    "security/index.html",
    "contributing/index.html",
    "license/index.html",
    "support/index.html",
    "404.html",
    "docs/index.html",
    "docs/architecture/index.html",
    "docs/compatibility/index.html",
    "docs/performance/index.html",
    "docs/integration/index.html",
    "docs/troubleshooting/index.html",
    "docs/faq/index.html",
    "assets/css/site.css",
    "assets/js/site.js",
    "assets/img/logo.svg",
    "assets/img/favicon.svg",
    "assets/img/social-preview.png",
    "assets/fonts/archivo-latin.woff2",
    "assets/fonts/source-serif-4-latin.woff2",
    "assets/fonts/jetbrains-mono-latin.woff2",
    "assets/fonts/OFL-Archivo.txt",
    "assets/fonts/OFL-SourceSerif4.txt",
    "assets/fonts/OFL-JetBrainsMono.txt",
    "assets/screenshots/reporter-form.png",
    "assets/screenshots/reporter-editor.png",
    "assets/screenshots/reporter-review.png",
    "assets/screenshots/desktop-viewer.png",
    "robots.txt",
    "sitemap.xml",
    "site.webmanifest",
    "README.md",
]

# External hosts that may appear in <a href>. Resources must never be remote.
ALLOWED_HOSTS = {
    "github.com",
    "docs.github.com",
    "www.apache.org",
}

# Absolute URLs that must live on the project origin.
SITE_PREFIXES = (
    "https://awbuilds.github.io/reproparcel/",
)

ATTR = re.compile(r'(?P<attr>href|src)="(?P<url>[^"]+)"', re.IGNORECASE)
LINK_TAG = re.compile(r"<link\b[^>]*>", re.IGNORECASE)
REL = re.compile(r'rel="(?P<rel>[^"]+)"', re.IGNORECASE)
SCHEME = re.compile(r"^[a-zA-Z][a-zA-Z0-9+.-]*:")


def is_remote(url: str) -> bool:
    return bool(SCHEME.match(url) or url.startswith("//"))


def host_of(url: str) -> str:
    return urlparse(url).netloc.lower()


def allowed_anchor(url: str) -> bool:
    parsed = urlparse(url)
    if parsed.scheme not in {"http", "https"}:
        return False
    host = parsed.netloc.lower()
    if host not in ALLOWED_HOSTS:
        return False
    if host == "github.com":
        return parsed.path.startswith("/awbuilds/reproparcel")
    if host == "www.apache.org":
        return parsed.path.startswith("/licenses/")
    return True


def resolve(page: Path, target: str) -> Path | None:
    clean = unquote(target.split("#", 1)[0].split("?", 1)[0])
    if not clean:
        return page
    if clean.startswith("/"):
        # Absolute internal paths include the project base path.
        if not clean.startswith("/reproparcel/"):
            return None
        base = SITE / clean[len("/reproparcel/") :]
    else:
        base = (page.parent / clean).resolve()
    if base.is_dir() or (clean.endswith("/") and not base.suffix):
        base = base / "index.html"
    return base


def check_page(path: Path, text: str) -> list[str]:
    issues: list[str] = []
    relative = path.relative_to(ROOT)

    for match in ATTR.finditer(text):
        attr = match.group("attr").lower()
        url = match.group("url").strip()
        if not url or url.startswith("#") or url.startswith("data:"):
            continue
        if url.startswith("mailto:"):
            issues.append(f"{relative}: personal-contact scheme in {url}")
            continue
        if is_remote(url):
            if attr == "src":
                issues.append(f"{relative}: remote resource reference {url}")
            elif not allowed_anchor(url) and not url.startswith(SITE_PREFIXES):
                issues.append(f"{relative}: external link to non-allowlisted host {url}")
            continue
        target = resolve(path, url)
        if target is None:
            issues.append(f"{relative}: path escapes the website root: {url}")
            continue
        try:
            target.relative_to(SITE.resolve())
        except ValueError:
            issues.append(f"{relative}: path escapes the website root: {url}")
            continue
        if not target.exists():
            issues.append(f"{relative}: missing target {url}")

    # Resource-level guarantees.
    for tag in LINK_TAG.finditer(text):
        rel_match = REL.search(tag.group(0))
        if not rel_match:
            continue
        rel = rel_match.group("rel").lower()
        if rel in {"stylesheet", "icon", "manifest", "apple-touch-icon"}:
            href = re.search(r'href="([^"]+)"', tag.group(0))
            if href and is_remote(href.group(1)) and "canonical" not in rel:
                issues.append(f"{relative}: remote {rel} resource")

    lowered = text.lower()
    if "http-equiv=\"content-security-policy\"" not in lowered:
        issues.append(f"{relative}: missing CSP meta tag")
    if "class=\"skip-link\"" not in text:
        issues.append(f"{relative}: missing skip link")
    if "<main" not in lowered:
        issues.append(f"{relative}: missing main landmark")
    if "<meta name=\"description\"" not in lowered:
        issues.append(f"{relative}: missing meta description")
    if lowered.count("<h1") != 1:
        issues.append(f"{relative}: expected exactly one h1")
    if re.search(r"<script(?![^>]*\bsrc=)[^>]*>", lowered):
        issues.append(f"{relative}: inline script found")

    return issues


URL_REF = re.compile(r"url\(\s*[\"']?([^\"')]+)[\"']?\s*\)")


def check_css() -> list[str]:
    """The stylesheet may only reference local files and must not import remote CSS."""
    issues: list[str] = []
    css_path = SITE / "assets/css/site.css"
    if not css_path.is_file():
        return ["missing assets/css/site.css"]
    text = css_path.read_text(encoding="utf-8")
    if "http://" in text or "https://" in text:
        issues.append("website/assets/css/site.css: remote URL in stylesheet")
    for match in URL_REF.finditer(text):
        url = match.group(1).strip()
        if url.startswith("data:"):
            continue
        target = (css_path.parent / url).resolve()
        try:
            target.relative_to(SITE.resolve())
        except ValueError:
            issues.append(f"website/assets/css/site.css: path escapes website root: {url}")
            continue
        if not target.exists():
            issues.append(f"website/assets/css/site.css: missing asset {url}")
    return issues


def main() -> int:
    failures: list[str] = []

    for item in REQUIRED:
        if not (SITE / item).exists():
            failures.append(f"missing required file: website/{item}")

    if not SITE.is_dir():
        print("check-website: FAIL (website/ does not exist)", file=sys.stderr)
        return 1

    pages = sorted(SITE.rglob("*.html"))
    for page_path in pages:
        text = page_path.read_text(encoding="utf-8")
        failures.extend(check_page(page_path, text))

    failures.extend(check_css())

    home = (SITE / "index.html").read_text(encoding="utf-8")
    for phrase in ("Source candidate", "no stable release"):
        if phrase not in home:
            failures.append(f"website/index.html: missing status phrase '{phrase}'")

    privacy = (SITE / "privacy/index.html").read_text(encoding="utf-8")
    for phrase in ("No analytics", "No cookies", "egress guarantee"):
        if phrase.lower() not in privacy.lower():
            failures.append(f"website/privacy/index.html: missing '{phrase}'")

    terms = (SITE / "terms/index.html").read_text(encoding="utf-8")
    for phrase in ("Apache-2.0" if "Apache-2.0" in terms else "Apache", "Not legal advice"):
        if phrase.lower() not in terms.lower():
            failures.append(f"website/terms/index.html: missing '{phrase}'")

    if failures:
        for failure in failures:
            print(f"check-website: {failure}", file=sys.stderr)
        print(f"check-website: FAIL ({len(failures)} issues, {len(pages)} pages)", file=sys.stderr)
        return 1

    print(f"check-website: PASS ({len(pages)} pages, {len(REQUIRED)} required files)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
