#!/usr/bin/env python3
# SPDX-License-Identifier: AGPL-3.0-only
"""Generate a resolved dependency inventory for notice review.

This is an inventory, not a legal opinion. It records the exact resolved
coordinates observed from Gradle/npm so the notice and license review can be
checked against real versions.
"""
from __future__ import annotations

import json
import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "docs" / "dependency-inventory.md"
COORD = re.compile(r"([A-Za-z0-9_.-]+):([A-Za-z0-9_.-]+):([A-Za-z0-9_.+-]+)")


def gradle_inventory() -> list[str]:
    output = subprocess.run(
        [
            "./gradlew",
            "--console=plain",
            ":reproparcel-core:dependencies",
            "--configuration",
            "runtimeClasspath",
            ":reproparcel-android:dependencies",
            "--configuration",
            "releaseRuntimeClasspath",
            ":reproparcel-okhttp:dependencies",
            "--configuration",
            "releaseRuntimeClasspath",
        ],
        cwd=ROOT,
        check=True,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
    ).stdout
    found: set[tuple[str, str, str]] = set()
    for line in output.splitlines():
        if not re.match(r"^\s*[+|\\]", line):
            continue
        for match in COORD.finditer(line):
            if match.group(1).startswith(("androidx", "com.", "org.", "junit", "tools.")):
                found.add((match.group(1), match.group(2), match.group(3)))
    return [f"{g}:{a}:{v}" for g, a, v in sorted(found)]


def pub_inventory(lock_path: Path) -> list[str]:
    if not lock_path.is_file():
        return []
    lines = lock_path.read_text(encoding="utf-8").splitlines()
    found: list[str] = []
    current: str | None = None
    for line in lines:
        match = re.match(r"^  ([a-zA-Z0-9_]+):$", line)
        if match:
            current = match.group(1)
            continue
        version_match = re.match(r'^    version: "?([0-9][^"\s]*)"?$', line)
        if current and version_match:
            found.append(f"{current} {version_match.group(1)}")
            current = None
    return found


def npm_inventory(lock_path: Path) -> list[str]:
    if not lock_path.is_file():
        return []
    data = json.loads(lock_path.read_text(encoding="utf-8"))
    packages = data.get("packages", {})
    found: set[tuple[str, str, str]] = set()
    for name, meta in packages.items():
        if not name.startswith("node_modules/"):
            continue
        package_name = name.removeprefix("node_modules/")
        version = meta.get("version")
        license_name = meta.get("license", "UNKNOWN")
        if version:
            found.add((package_name, version, str(license_name)))
    return [f"{n}@{v} ({lic})" for n, v, lic in sorted(found)]


def main() -> int:
    lines = [
        "# Resolved dependency inventory",
        "",
        "Generated from the actual Gradle/npm resolution. This is review evidence,",
        "not a legal opinion and not a substitute for the complete notice review.",
        "",
        "## Android / JVM runtime",
        "",
    ]
    lines += [f"- `{entry}`" for entry in gradle_inventory()]
    for label, lock in (
        ("Desktop", ROOT / "desktop/package-lock.json"),
        ("Relay", ROOT / "server/package-lock.json"),
        ("React Native host", ROOT / "react-native/ReproParcelHost/package-lock.json"),
    ):
        lines += ["", f"## {label}", ""]
        entries = npm_inventory(lock)
        lines += [f"- `{entry}`" for entry in entries] if entries else ["- No lockfile entries."]
    lines += ["", "## Flutter host (pub packages)", ""]
    pub_entries = pub_inventory(ROOT / "flutter/host_app/pubspec.lock")
    lines += [f"- `{entry}` (license per package review)" for entry in pub_entries] if pub_entries else ["- No pub lockfile entries."]
    OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"wrote {OUT.relative_to(ROOT)}: {len(lines)} lines")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
