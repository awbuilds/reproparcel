# PRD traceability

`prd-lines.jsonl` contains one row per physical line of `docs/PRD.md`.

- `UNREVIEWED` — not yet dispositioned; blocks a phase gate when applicable.
- `IN_PROGRESS` — work exists but evidence is incomplete.
- `PASS` — implemented and verified; `evidence` is required.
- `FAIL` — applicable and not satisfied; blocks the phase.
- `BLOCKED` — requires an external human, credential, device or approval; the exact action is recorded.
- `GATED` — later phase whose entry condition has not been met.
- `N/A` — narrative, structural or otherwise has no independent acceptance test; `notes` explains why.

Regenerate after any PRD edit:

```sh
python3 scripts/generate_prd_ledger.py
python3 scripts/apply_traceability.py
```

`overrides.json` is keyed by PRD line number. If the PRD changes, review every
override against the regenerated line text before committing; a shifted line
can silently apply a disposition to the wrong requirement. The audit checks the
line text hash, but it cannot infer the intended requirement after a move.

Audit a phase:

```sh
python3 scripts/prd_audit.py --phase 0
```

A phase cannot advance while an applicable row is `UNREVIEWED` or `FAIL`.
