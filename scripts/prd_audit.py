#!/usr/bin/env python3
# SPDX-License-Identifier: AGPL-3.0-only
"""Audit the PRD traceability ledger.

Checks that every PRD line is present and that every line applicable to the
requested phase has a disposition. Exits non-zero on UNREVIEWED or FAIL.

Usage:
  scripts/prd_audit.py --phase 0
  scripts/prd_audit.py --final
"""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PRD = ROOT / "docs" / "PRD.md"
LEDGER = ROOT / "docs" / "traceability" / "prd-lines.jsonl"
OUT_DIR = ROOT / "docs" / "audits"


def load_ledger() -> list[dict]:
    records = []
    with LEDGER.open(encoding="utf-8") as handle:
        for line in handle:
            if line.strip():
                records.append(json.loads(line))
    return records


def applicable(record: dict, phase: int | None) -> bool:
    if phase is None:
        return True
    value = str(record.get("phase", "unassigned"))
    if value in {"unassigned", "15"}:
        return False
    if value in {"6-9"}:
        return phase >= 6
    if value.isdigit():
        return int(value) <= phase
    return False


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--phase", type=int)
    parser.add_argument("--final", action="store_true")
    args = parser.parse_args()
    if args.phase is None and not args.final:
        parser.error("provide --phase N or --final")

    lines = PRD.read_text(encoding="utf-8").splitlines()
    records = load_ledger()
    errors: list[str] = []

    if len(records) != len(lines):
        errors.append(f"ledger has {len(records)} rows; PRD has {len(lines)} lines")

    for record, text in zip(records, lines, strict=False):
        expected = hashlib.sha256(text.encode("utf-8")).hexdigest()
        if record["text_sha256"] != expected:
            errors.append(
                f"line {record['line']}: PRD text changed; regenerate the ledger"
            )

    scored = [r for r in records if applicable(r, args.phase)]
    counts: dict[str, int] = {}
    for record in scored:
        status = record.get("status", "UNREVIEWED")
        counts[status] = counts.get(status, 0) + 1
        if status in {"UNREVIEWED", "FAIL"}:
            errors.append(
                f"line {record['line']} [{record.get('phase')}]: {status} "
                f"({record.get('text', '')[:100]})"
            )
        if args.final and status in {"IN_PROGRESS", "GATED"}:
            errors.append(
                f"line {record['line']} [{record.get('phase')}]: {status} is not final "
                f"({record.get('text', '')[:100]})"
            )
        if (
            args.final
            and status == "N/A"
            and record.get("normative")
            and record.get("kind") in {"PROSE", "LIST", "TABLE"}
            and not record.get("notes")
        ):
            errors.append(
                f"line {record['line']}: normative N/A without a reason"
            )
        if status == "PASS" and not record.get("evidence"):
            errors.append(f"line {record['line']}: PASS without evidence")

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    label = "final" if args.final else f"phase-{args.phase}"
    out = OUT_DIR / f"{label}-audit.md"
    with out.open("w", encoding="utf-8") as handle:
        handle.write(f"# PRD audit — {label}\n\n")
        handle.write(f"- PRD lines: {len(lines)}\n")
        handle.write(f"- Ledger rows: {len(records)}\n")
        handle.write(f"- Applicable rows: {len(scored)}\n")
        handle.write(f"- Status counts: `{json.dumps(counts, sort_keys=True)}`\n\n")
        if errors:
            handle.write("## Gaps\n\n")
            for error in errors:
                handle.write(f"- {error}\n")
        else:
            handle.write("## Result\n\nNo UNREVIEWED or FAIL rows. Audit passed.\n")

    print(json.dumps(counts, sort_keys=True))
    print(f"wrote {out.relative_to(ROOT)}")
    if errors:
        print(f"prd-audit: FAIL ({len(errors)} gaps)", file=sys.stderr)
        return 1
    print("prd-audit: PASS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
