# Naming and namespace verification log

Date: 2026-11-11

## Current facts

- Working product name: **ReproParcel**.
- Proposed repository: `https://github.com/awbuilds/reproparcel` — the remote
  exists and was empty at the start of implementation.
- Proposed package: `com.awbuilds.reproparcel`.
- Proposed Maven group: `io.github.awbuilds`, subject to Sonatype namespace
  verification.
- Proposed artifacts: `reproparcel-core`, `reproparcel-android`,
  `reproparcel-compose`, `reproparcel-okhttp`.
- Proposed desktop commands: `reproparcel inspect`, `reproparcel validate`,
  `reproparcel render`, `reproparcel mcp`.

## Checks performed

- GitHub repository lookup for the exact name returned the `awbuilds/reproparcel`
  remote; it is public and was empty.
- No claim is made about trademark clearance, domain availability or Maven
  publishing rights.
- No domain, package registry entry or Maven namespace was purchased or
  reserved.

## Blocked verification

| Check | Status | Required action |
|---|---|---|
| Trademark screening | BLOCKED | Qualified maintainer/legal review before public branding |
| Sonatype namespace ownership | BLOCKED | Verify `io.github.awbuilds` in Sonatype Central |
| Maven coordinate availability | BLOCKED | Verify after namespace ownership |
| Domain registration | NOT REQUESTED | Separate approval required |

## Rule

Do not publish, reserve or purchase anything based on preliminary search
results. Keep the working name until clearance is recorded.
