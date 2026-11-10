# ReproParcel relay — reference implementation

**Status: reference code only. Not deployed. No production credentials.**

A small self-hosted ingestion service for approved parcels. File export remains
fully functional without it.

## Security model

- Project-scoped **write-only** bearer token; tokens are stored as SHA-256
  hashes. A stolen project token cannot read evidence.
- Separate admin token for read/delete. Admin and project credentials must be
  different.
- AES-256-GCM at rest with a server-side 32-byte key; random nonce; project ID
  as AAD.
- Daily quota, per-minute rate limit, bounded request body.
- `Idempotency-Key` makes retries create one report.
- Audit rows contain event/project/report identifiers and safe codes only, never
  report content.
- Deletion is explicit. Backups and copies already delivered cannot be recalled.
- Tracker credentials remain server-side. The mobile app never receives a
  GitHub/Linear token.

## Run locally

```sh
cd server
npm ci
npm test
REPROPARCEL_MASTER_KEY="$(openssl rand -hex 32)" \
REPROPARCEL_ADMIN_TOKEN="$(openssl rand -hex 32)" \
REPROPARCEL_PROJECT_ID="demo" \
REPROPARCEL_PROJECT_TOKEN="$(openssl rand -hex 32)" \
REPROPARCEL_PORT=8080 \
npm start
```

The listener binds `127.0.0.1` only. Put it behind a reviewed authenticated TLS
reverse proxy before any real use. No cleartext fallback or disabled TLS
verification is acceptable.

## API

```text
GET  /health
POST /v1/projects/:projectId/reports
     Authorization: Bearer <project token>
     Idempotency-Key: <stable retry key>
     Body: application/octet-stream, bounded parcel bytes

GET    /v1/admin/reports/:reportId   (admin token)
DELETE /v1/admin/reports/:reportId   (admin token)
```

There is no project-token read endpoint.

## GitHub adapter

`GitHubAdapter` is a trusted-side helper that creates an issue with a private
link. It does not upload attachments by guessing an API. Issue creation, private
storage and deletion policy remain deployment-specific and require separate
approval. Public issues are never the default destination for user evidence.

## Not implemented

- Deployment, TLS termination, backups, database migration tooling.
- A production private-link UI.
- Linear adapter (Phase 6 starts with one tracker only).
- Any real adopter credential or data.
