#!/usr/bin/env python3
# SPDX-License-Identifier: AGPL-3.0-only
"""Provisional dependency license/allowlist gate."""
from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ALLOWED_GROUPS = (
    "androidx",
    "com.android",
    "com.squareup.okhttp3",
    "com.squareup.okio",
    "org.jetbrains",
    "org.jetbrains.kotlin",
    "org.jetbrains.kotlinx",
    "org.slf4j",
    "com.networknt",
    "tools.jackson",
    "com.fasterxml.jackson",
    "com.ethlo.time",
    "junit",
    "org.hamcrest",
    "com.google.errorprone",
    "com.google.guava",
    "org.jspecify",
)
COORDINATE = re.compile(r"([A-Za-z0-9_.-]+):([A-Za-z0-9_.-]+):([A-Za-z0-9_.+-]+)")


def main() -> int:
    for required in ("LICENSE", "NOTICE"):
        if not (ROOT / required).is_file():
            print(f"missing {required}", file=sys.stderr)
            return 1

    catalog = (ROOT / "gradle" / "libs.versions.toml").read_text(encoding="utf-8")
    if re.search(r'(?:\+["\s]|latest\.|\[[0-9])', catalog):
        print("dynamic or range version found in the version catalog", file=sys.stderr)
        return 1

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
        ],
        cwd=ROOT,
        check=True,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
    ).stdout

    groups = set()
    for line in output.splitlines():
        if not re.match(r"^\s*[+|\\]", line):
            continue
        for match in COORDINATE.finditer(line):
            groups.add(match.group(1))

    unknown = sorted(
        group
        for group in groups
        if not any(group == prefix or group.startswith(prefix + ".") for prefix in ALLOWED_GROUPS)
    )
    if unknown:
        print("unreviewed dependency groups: " + ", ".join(unknown), file=sys.stderr)
        return 1

    print(f"dependency-check: PASS ({len(groups)} groups)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
