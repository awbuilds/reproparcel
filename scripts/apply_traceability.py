#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Apply manual traceability dispositions to the generated ledger.

Overrides are keyed by PRD line number. Re-run the generator first if the PRD
text changed, then apply overrides.
"""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
LEDGER = ROOT / "docs" / "traceability" / "prd-lines.jsonl"
OVERRIDES = ROOT / "docs" / "traceability" / "overrides.json"
LOCK = ROOT / "docs" / "traceability" / "overrides.lock.json"


def main() -> int:
    overrides = json.loads(OVERRIDES.read_text(encoding="utf-8"))
    lock = json.loads(LOCK.read_text(encoding="utf-8")) if LOCK.is_file() else {}
    rows = []
    with LEDGER.open(encoding="utf-8") as handle:
        for line in handle:
            if line.strip():
                rows.append(json.loads(line))

    applied = 0
    for row in rows:
        override = overrides.get(str(row["line"]))
        if not override:
            continue
        expected = lock.get(str(row["line"]))
        if expected and expected != row["text_sha256"]:
            raise SystemExit(
                f"override line {row['line']} no longer matches the PRD text; review it"
            )
        row.update(override)
        applied += 1

    with LEDGER.open("w", encoding="utf-8") as handle:
        for row in rows:
            handle.write(json.dumps(row, ensure_ascii=False, sort_keys=True) + "\n")

    print(f"applied {applied} overrides to {LEDGER.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
