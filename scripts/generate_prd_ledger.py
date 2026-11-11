#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Generate the line-level PRD traceability ledger.

Every physical line of docs/PRD.md receives one JSON object in
docs/traceability/prd-lines.jsonl. This is intentionally exhaustive: the
phase audit must account for every line, including headings and prose.
"""
from __future__ import annotations

import hashlib
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PRD = ROOT / "docs" / "PRD.md"
OUT = ROOT / "docs" / "traceability" / "prd-lines.jsonl"

SECTION_PHASE = {
    1: "0",
    2: "0",
    3: "5",
    4: "0",
    5: "1",
    6: "2",
    7: "1",
    8: "1",
    9: "unassigned",
    10: "1",
    11: "1",
    12: "1",
    13: "5",
    14: "5",
    15: "15",
    16: "0",
    17: "0",
    18: "0",
    19: "5",
    20: "0",
}

NORMATIVE = re.compile(
    r"\b(MUST|MUST NOT|SHOULD|SHOULD NOT|MAY|REQUIRED|Required|Proposed|"
    r"Phase \d|FR-\d{2}|G-0\d|UC-\d{2}|R-\d{2}|never|Never|do not|Do not)\b"
)
FR_PHASE = {
    "FR-01": "1",
    "FR-02": "2",
    "FR-03": "1",
    "FR-04": "2",
    "FR-05": "2",
    "FR-06": "2",
    "FR-07": "3",
    "FR-08": "3",
    "FR-09": "3",
    "FR-10": "3",
    "FR-11": "1",
    "FR-12": "1",
    "FR-13": "2",
    "FR-14": "2",
    "FR-15": "4",
    "FR-16": "4",
    "FR-17": "0",
    "FR-18": "0",
    "FR-19": "6",
}


def main() -> int:
    if not PRD.exists():
        print(f"missing {PRD}", file=sys.stderr)
        return 1
    lines = PRD.read_text(encoding="utf-8").splitlines()
    OUT.parent.mkdir(parents=True, exist_ok=True)

    section = 0
    heading = ""
    phase_section = None
    records = []
    for number, text in enumerate(lines, start=1):
        heading_match = re.match(r"^## (\d+)\.", text)
        if heading_match:
            section = int(heading_match.group(1))
            heading = text.lstrip("# ").strip()
        phase_match = re.match(r"^### Phase (\d)", text)
        if phase_match:
            phase_section = int(phase_match.group(1))
        elif text.startswith("### ") or text.startswith("## "):
            phase_section = None

        stripped = text.strip()
        if not stripped:
            kind = "BLANK"
        elif stripped.startswith("#"):
            kind = "HEADING"
        elif stripped.startswith("|"):
            kind = "TABLE"
        elif stripped.startswith("```"):
            kind = "FENCE"
        elif stripped.startswith("- ") or re.match(r"^\d+\. ", stripped):
            kind = "LIST"
        else:
            kind = "PROSE"

        identifiers = sorted(
            set(re.findall(r"\b(?:FR-\d{2}|G-0\d|UC-\d{2}|R-\d{2})\b", text))
        )
        normative = bool(NORMATIVE.search(text))
        phase = SECTION_PHASE.get(section, "unassigned")
        if phase_section is not None:
            phase = str(phase_section)
            # Work packages, deliverables and exit criteria under a phase
            # heading are obligations even when they do not contain MUST/SHOULD.
            if kind in {"LIST", "PROSE", "TABLE"}:
                normative = True
        # A Phase 0 comparison inside an earlier architecture section is Phase 0
        # acceptance work.
        if section == 7 and "Phase 0" in text:
            phase = "0"
            normative = True
        # Requirement-bearing tables and API/contract lists must not be hidden
        # in the narrative bucket.
        if section in {7, 8, 13, 14} and kind in {"TABLE", "LIST"}:
            normative = True
        if section == 7 and 265 <= number <= 287:
            normative = True
        for ident in identifiers:
            if ident in FR_PHASE:
                phase = FR_PHASE[ident]
                break
        # FR-04/FR-10 are labelled P0/P2 and P0/P3; score the earliest phase.
        phase_labels = re.findall(r"\bP(\d)\b", text)
        if phase_labels and identifiers:
            phase = min(phase_labels)
            normative = True

        if kind == "BLANK":
            status = "N/A"
            note = "blank line"
        elif kind == "HEADING":
            status = "N/A"
            note = "structural heading"
        elif kind == "FENCE":
            status = "N/A"
            note = "code-fence delimiter or example"
        elif not normative and kind in {"PROSE", "LIST", "TABLE"}:
            status = "N/A"
            note = "narrative/context; no independent acceptance test"
        else:
            status = "UNREVIEWED"
            note = ""

        records.append(
            {
                "line": number,
                "section": section,
                "heading": heading,
                "kind": kind,
                "phase": phase,
                "identifiers": identifiers,
                "normative": normative,
                "text_sha256": hashlib.sha256(text.encode("utf-8")).hexdigest(),
                "text": text,
                "status": status,
                "artifact": "",
                "evidence": "",
                "notes": note,
            }
        )

    with OUT.open("w", encoding="utf-8") as handle:
        for record in records:
            handle.write(json.dumps(record, ensure_ascii=False, sort_keys=True) + "\n")

    counts: dict[str, int] = {}
    for record in records:
        counts[record["status"]] = counts.get(record["status"], 0) + 1
    print(f"wrote {OUT.relative_to(ROOT)}: {len(records)} lines")
    print(json.dumps(counts, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
