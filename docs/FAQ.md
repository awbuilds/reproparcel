# Frequently asked questions

## Adoption

### Is there a hosted service or account?

No. Capture, drafts, export, validation, inspection, rendering and MCP all work
locally. The `server/` directory is an optional self-hosted reference relay and
is not deployed.

### Can I use ReproParcel in a proprietary app?

ReproParcel is **AGPL-3.0-only**. Linking or distributing it can impose copyleft
obligations on a covered combined application. There is no proprietary-linking
exception. Read [LICENSE](../LICENSE), [NOTICE](../NOTICE) and obtain your own
legal advice.

### Is a stable release available?

No. The repository is a source candidate on `dev`. It has no tag, GitHub Release
or Maven Central publication. See [Project state](PROJECT_STATE.md) and the
[final audit](audits/final-audit.md).

### Which Android versions are supported?

The proposed minimum is API 26. The tested matrix covers API 26, 30, 31
(physical low/mid-range) and 37. See [Compatibility](compatibility.md).

### Does it require Google Play services?

No. The core SDK does not depend on Play Services, a network stack, Hilt, Room or
a database. `reproparcel-okhttp` is optional.

### Can I adopt only some modules?

Yes. `reproparcel-core` is JVM-only, `reproparcel-android` adds the runtime,
`reproparcel-compose` adds the reporter, and `reproparcel-okhttp` is optional.

## Privacy

### Does it collect screenshots automatically?

No. Initialization and collection are explicit. Screenshot and semantic
collection require a host capture policy with an eligible surface.

### What happens on a protected window?

The capture is omitted with `SECURE_WINDOW`. A raw PixelCopy success is not
treated as privacy proof; the SDK never uses copy failure as its only check.

### Are masks reversible?

No. Host masks are applied as opaque pixels to the sanitized bitmap before the
editor. Undo only affects user-added annotations, not the protected base.

### Are real user reports stored in the repository?

Never. Tests and fixtures use synthetic data. Public issues, screenshots and
parcels must not contain real reports, credentials or private application
content.

### Is local data encrypted?

Drafts use AES-256-GCM with an Android Keystore key and are stored in
`noBackupFilesDir`. Key failure never falls back to plaintext. A file already
shared cannot be recalled.

### Is the MCP server safe for sensitive evidence?

It is read-only and accepts report IDs, not paths. It still cannot control the
MCP client or model provider: those may send returned content to a cloud model.
Use a local client/model for evidence that must not leave your machine.

## Capture and semantics

### Does it capture every UI surface?

No. Unsupported, protected or oversized surfaces are omitted. A text-only report
remains available.

### Does it extract the whole Compose tree automatically?

No. Explicit host-registered semantic descriptors are the v1 baseline.
Automatic extraction is not shipped until a public-API, minified-release
compatibility experiment passes.

### Can the host add its own evidence?

Yes. Hosts can register safe breadcrumbs, structured logs, OkHttp metadata and
explicit semantic nodes. Unknown or unsafe attributes are dropped before the
bounded buffers.

## Desktop and agents

### What is a `.reproparcel` file?

A bounded ZIP with a versioned manifest, report text, optional events,
semantics and one sanitized PNG. Every entry is listed with a media type, size
and SHA-256.

### How does the viewer handle HTML or scripts in a report?

The viewer escapes all report text, renders only local approved images and uses
a restrictive CSP. It executes no report content and loads no external URL.

### Which MCP tools exist?

`list_reports`, `get_report`, `get_events`, `get_semantics`, `get_attachment`.
There is no write, execute, upload or arbitrary-file tool.

### How do I report a vulnerability?

Use GitHub's private vulnerability reporting for `awbuilds/reproparcel`; see
[SECURITY.md](../SECURITY.md). Never put sensitive evidence in a public issue.

## Development

### Why is the branch `dev`?

The pre-release implementation intentionally works on `dev`. The repository does
not create, merge into or publish `main` during this phase. See
[Governance](../GOVERNANCE.md).

### How do I contribute?

Read [CONTRIBUTING.md](../CONTRIBUTING.md) and
[AGENTS.md](../AGENTS.md), use synthetic data, sign off commits and target
`dev`.

### Why are some audit rows BLOCKED?

External interviews, the pilot, external integrations, physical-device rows,
legal review, Sonatype verification and publication require people or accounts
outside the implementation. They are recorded as `BLOCKED` with the exact next
action rather than fabricated.
