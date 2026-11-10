# ADR 0014 — Self-hosted write-only relay

- Status: Accepted reference implementation for Phase 6; deployment blocked
- Date: 2026-11-11
- Deciders: ReproParcel maintainers

## Decision

- Ingestion is authenticated HTTPS in production; the reference binds loopback
  and expects a reviewed TLS reverse proxy.
- Project tokens are write-only and stored hashed. A separate admin token is
  required for read/delete. Stolen project credentials cannot read evidence.
- Evidence is encrypted at rest with AES-256-GCM and a server-side key.
- Quotas, per-minute rate limiting and `Idempotency-Key` bound abuse and retries.
- Audit logs never contain report content.
- Tracker credentials stay server-side. The first adapter is GitHub; create one
  issue with a private link, not a public issue with raw evidence.
- The relay is optional. Local file export and offline tools remain fully usable
  without it.
- No deployment, domain, credential or real data is part of this reference.

## Evidence

Five Node tests cover authenticated write-only ingestion, idempotent retry,
encrypted-at-rest bytes, project-token read denial, revocation, quota, audit
content exclusion and the injected GitHub adapter. These are local tests, not a
deployment or penetration test.
