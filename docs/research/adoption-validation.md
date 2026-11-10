# Adoption validation kit and status

Status: **BLOCKED — no external interviews have been conducted.**
The implementation cannot fabricate adopter evidence.

## Goal

Confirm that a local-first, portable, agent-readable evidence parcel is
materially more useful than manual tickets or a hosted feedback product for
AGPL-compatible Android maintainers.

## Required sample

At least five potential adopters. At least three must identify a concrete use
case before Phase 0 is considered externally validated.

## Interview protocol

1. Explain the local-first model, AGPL-3.0-only implications and the absence of
   a mandatory backend.
2. Ask about the last three real defects they received: what was missing, what
   follow-up they requested and how they reproduced the issue.
3. Ask how they currently collect screenshots, build metadata, logs and screen
   context; note privacy constraints and redaction work.
4. Ask whether they use a coding agent and how evidence reaches it.
5. Show the synthetic parcel format and ask what would make it trustworthy,
   untrustworthy or too expensive to adopt.
6. Record only aggregate, synthetic-safe notes. No real reports, customer data,
   credentials or personal information.

## Questions

- Which evidence categories would you enable by default, and which require an
  explicit host policy?
- What would make you reject the SDK: AGPL obligations, binary size, setup
  cost, capture risk, privacy review or missing integrations?
- Would a file-based workflow fit your team, or is a self-hosted relay required?
- What is the minimum build/screen/action provenance you need to begin an
  investigation?
- Which desktop/MCP client would you use, and are you aware it may send content
  to a cloud model?

## Status table

| Item | Status | Evidence |
|---|---|---|
| Interview kit prepared | PASS | This document |
| Five interviews completed | BLOCKED | External human action required |
| Three concrete use cases recorded | BLOCKED | Depends on interviews |
| Competitive comparison reviewed | PASS | `docs/research/competitive-positioning.md` |

## Next action

The maintainer schedules five interviews and records aggregate answers here.
Do not mark G-01/G-02 or Phase 0 external validation as passed without real
participants.
