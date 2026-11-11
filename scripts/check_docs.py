#!/usr/bin/env python3
# SPDX-License-Identifier: AGPL-3.0-only
"""Check relative links and images in the current documentation set."""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DEFAULT_FILES = [
    "README.md",
    "CONTRIBUTING.md",
    "SECURITY.md",
    "CODE_OF_CONDUCT.md",
    "SUPPORT.md",
    "GOVERNANCE.md",
    "CITATION.cff",
    "docs/README.md",
    "docs/ARCHITECTURE.md",
    "docs/FAQ.md",
    "docs/ROADMAP.md",
    "docs/compatibility.md",
    "docs/performance.md",
    "docs/troubleshooting.md",
    "docs/privacy-policy-example.md",
    "docs/api-freeze.md",
    "docs/dependency-licenses.md",
    "docs/integration/desktop-tools.md",
    "docs/integration/evidence-adapters.md",
    "docs/security/threat-model.md",
    "docs/security/phase-2-capture-contract.md",
    "docs/traceability/README.md",
    "docs/assets/README.md",
]
LINK = re.compile(r"!?\[[^\]]*\]\(([^)\s]+)(?:\s+[\"'][^\"']*[\"'])?\)")
SCHEME = re.compile(r"^[a-zA-Z][a-zA-Z0-9+.-]*:")


def links_in(path: Path) -> list[tuple[int, str]]:
    text = path.read_text(encoding="utf-8")
    result: list[tuple[int, str]] = []
    in_fence = False
    for number, line in enumerate(text.splitlines(), start=1):
        stripped = line.lstrip()
        if stripped.startswith("```") or stripped.startswith("~~~"):
            in_fence = not in_fence
            continue
        if in_fence:
            continue
        for match in LINK.finditer(line):
            result.append((number, match.group(1)))
    return result


def main(argv: list[str]) -> int:
    files = [Path(item) for item in (argv or DEFAULT_FILES)]
    failures: list[str] = []
    checked = 0
    for relative in files:
        path = ROOT / relative if not relative.is_absolute() else relative
        if not path.is_file():
            failures.append(f"{relative}: file does not exist")
            continue
        for number, target in links_in(path):
            if target.startswith("#") or SCHEME.match(target):
                continue
            cleaned = target.split("#", 1)[0].split("?", 1)[0]
            if not cleaned:
                continue
            checked += 1
            resolved = (path.parent / cleaned).resolve()
            try:
                resolved.relative_to(ROOT.resolve())
            except ValueError:
                failures.append(f"{relative}:{number}: link escapes repository: {target}")
                continue
            if not resolved.exists():
                failures.append(f"{relative}:{number}: missing target: {target}")
    if failures:
        for failure in failures:
            print(f"docs-check: {failure}", file=sys.stderr)
        print(f"docs-check: FAIL ({len(failures)} issues, {checked} links checked)", file=sys.stderr)
        return 1
    print(f"docs-check: PASS ({checked} links checked in {len(files)} files)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
