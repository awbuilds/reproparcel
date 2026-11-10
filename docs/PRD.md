# ReproParcel — Product Requirements Document

> **One-line vision:** Turn a real mobile user's bug report into a portable, privacy-reviewed evidence parcel that a developer or coding agent can understand without recreating the user's entire environment.
>
> **Product principle:** The reporter must never become the reason the host app fails, leaks data, or becomes slow.

| Field | Decision |
|---|---|
| Document version | 1.0 |
| Baseline date | 2026-11-11 |
| Status | Planning baseline; implementation has not started |
| Working product name | **ReproParcel** |
| Maintainer brand | awbuilds |
| First platform | Native Android; Kotlin; Jetpack Compose reporter UI |
| First adopter | A private host application (identity withheld), initially in a development build with synthetic data |
| First distribution | Maven Central Android libraries; separately packaged desktop tools |
| License | **GNU AGPL version 3.0 only — `AGPL-3.0-only`** |
| v1 operating model | Local capture, user review, manual export, local inspection, read-only agent access |
| Hosted service | Not required for v1; optional future delivery component |
| Authoritative scope | Phases 0–5 define Android v1; Phases 6–9 are gated expansion |

**Reading rules.** MUST means a release-blocking requirement; SHOULD means the default unless a documented reason overrides it; MAY means optional. Numbers below are proposed engineering acceptance targets, not measured product claims. A phase is complete only when its exit criteria have evidence. A future requirement is not a current capability.

**Authorization boundary.** This document specifies a project. It does not authorize creating public repositories, registering domains, publishing packages, installing an SDK into a private adopter's production environment, or changing its privacy policy. Those actions require their own approval.

---

## Contents

1. [Identity and naming](#1-identity-and-naming)
2. [Problem and community value](#2-problem-and-community-value)
3. [Vision, goals and success criteria](#3-vision-goals-and-success-criteria)
4. [Users and use cases](#4-users-and-use-cases)
5. [Scope and deliberate exclusions](#5-scope-and-deliberate-exclusions)
6. [User and developer journeys](#6-user-and-developer-journeys)
7. [Architecture and integration contracts](#7-architecture-and-integration-contracts)
8. [Evidence parcel specification](#8-evidence-parcel-specification)
9. [Functional requirements](#9-functional-requirements)
10. [Privacy, security and consent](#10-privacy-security-and-consent)
11. [Reliability and exception policy](#11-reliability-and-exception-policy)
12. [Current Android engineering standards](#12-current-android-engineering-standards)
13. [Performance and resource budgets](#13-performance-and-resource-budgets)
14. [Testing and release gates](#14-testing-and-release-gates)
15. [Phase-by-phase delivery](#15-phase-by-phase-delivery)
16. [Repository and agent working contract](#16-repository-and-agent-working-contract)
17. [Licensing and community governance](#17-licensing-and-community-governance)
18. [Risks and decision register](#18-risks-and-decision-register)
19. [Launch and maintenance](#19-launch-and-maintenance)
20. [Sources and verification boundaries](#20-sources-and-verification-boundaries)

## 1. Identity and naming

### 1.1 Recommended name

**ReproParcel** combines “repro” — information useful for reproducing a defect — and “parcel” — a portable package delivered to a human or an agent. It describes the artifact rather than promising automatic repair.

- Product: ReproParcel.
- Android public entry point: `ReproParcel` (proposed API name, not existing code).
- Portable file extension: `.reproparcel`, a ZIP container with a versioned manifest.
- Proposed repository: `awbuilds/reproparcel` (not created or reserved).
- Proposed Java/Kotlin package: `com.awbuilds.reproparcel`; package naming alone does not establish domain ownership.
- Proposed Maven group: `io.github.awbuilds`, **subject to successful Sonatype namespace verification**. GitHub organization namespaces are not automatically granted like personal GitHub namespaces.
- Proposed Android artifacts: `reproparcel-core`, `reproparcel-android`, `reproparcel-compose`, `reproparcel-okhttp`.
- Proposed desktop commands: `reproparcel inspect`, `reproparcel validate`, `reproparcel render`, `reproparcel mcp`. Their flags and output schemas must be designed and tested before implementation; these commands do not exist yet.

### 1.2 Preliminary availability, not legal clearance

Built-in-browser checks on 2026-11-11 found no repository matching the exact quoted name, no exact-name search-engine result, no package returned at the exact npm or pub.dev package endpoints, and a registry “not found” response for `reproparcel.dev`.

These observations **do not reserve a name, prove trademark availability, guarantee registrar purchase availability, or establish Maven publishing rights**. Complete name/trademark screening and Maven namespace verification in Phase 0 before release branding. Maven availability is not claimed here; its search endpoint proved unreliable during research.

## 2. Problem and community value

### 2.1 Problem statement

“Something is broken” reports usually lack the app build, affected screen, preceding actions and sanitized diagnostic context. Developers spend time asking follow-up questions; agents receive prose without enough grounding to identify the relevant source code. Screenshots may accidentally expose personal data, while always-on recording increases privacy and performance costs.

The useful contribution is **not another dashboard or a claim to be the first bug reporter**. It is a small, inspectable native Android SDK plus an open evidence format and local tools that work independently of a particular SaaS, model or IDE.

### 2.2 Current alternatives and honest positioning

| Alternative | What it already does | ReproParcel's intended difference |
|---|---|---|
| Sentry Android | Feedback forms, shake-to-report and integration with session replay; feedback can work with sufficiently recent self-hosted Sentry | Local-first parcel export without adopting a full observability backend; explicit report contract for local agent tools |
| Instabug/Luciq, Shake, Gleap | Commercial feedback and mobile diagnostics products | AGPL code, inspectable capture policy, no mandatory vendor account or recurring service |
| Quash | Existing open-source mobile bug-reporting work | Acknowledge prior art; compete on maintained native integration, privacy contracts, compatibility evidence and portability |
| BugDrop and BugPin | Web screenshot/annotation and issue-reporting tools | Native Android collection and Compose integration, not a script-tag web widget |
| Flutter feedback packages | Existing Flutter feedback UI | Android-first scope; no claim that Flutter lacks feedback tooling |
| Manual screenshots and tickets | Simple and universally available | Build provenance, structured evidence, explicit missing-data reasons and a consistent viewer |

**Correction to earlier discussion:** there is no defensible basis for saying that maintained open-source mobile feedback does not exist. Sentry is a substantial existing option. The product must validate that portability and lightweight local operation are valuable enough to justify another library.

### 2.3 Why awbuilds can execute this

A private Kotlin/Compose host application provides existing privacy-sensitive workflows and real release-build constraints. It is an appropriate integration proving ground, **not a source of real financial test fixtures**. The initial audience is AGPL-compatible apps and teams willing to satisfy copyleft requirements; proprietary SDK adoption is not assumed.

## 3. Vision, goals and success criteria

### 3.1 Full vision

A user reports a bug in context. ReproParcel captures only configured and permitted evidence, explains omissions, lets the user review exactly what will leave the device, and produces a self-contained parcel. A developer can inspect it offline. A coding agent can read structured evidence through a read-only adapter, verify build provenance and propose a change under the host project's normal approval rules.

Later, teams may deliver the same parcel through a self-hosted relay and issue integrations. The format remains usable without the relay. Future platform SDKs share the evidence contract while implementing capture natively.

### 3.2 v1 goals

| ID | Goal | Acceptance evidence |
|---|---|---|
| G-01 | Reduce avoidable diagnostic follow-up | In a pilot of at least 20 synthetic/tester-reported defects, at least 80% of reports identify build, screen, expected/actual behavior and useful evidence or explicit omission reasons |
| G-02 | Make integration achievable for one developer | At least two developers outside the implementation team integrate the published candidate into a sample app within 30 minutes following only the quick start; deeper capture adapters are measured separately |
| G-03 | Preserve host-app stability | Fault-injection suite shows no host crash attributable to tested recoverable SDK failures; no unresolved release-blocking reliability defect |
| G-04 | Prevent silent evidence leakage | Mandatory privacy pipeline; zero known seeded secret leaks in release fixtures; no raw-image export path or unreviewed automatic submission |
| G-05 | Enable useful agent consumption | Two different MCP-capable coding clients can inspect an exported report and identify the relevant synthetic sample-app screen/build without SDK-owned model calls |
| G-06 | Remain backend independent | Create, validate, view and inspect a parcel with network disconnected and without an account |
| G-07 | Establish maintainable public contracts | Versioned schema, public API checks, compatibility matrix, release notes, security reporting and reproducible handoffs |

Pilot counts are validation targets, not statistical proof. Publish the method and limitations. No first-party analytics are required to measure adoption or report quality; use opted-in pilot feedback and explicit test results.

### 3.3 North-star measure

**Actionable-report rate:** reports that allow a maintainer to begin a grounded investigation without asking for basic build/screen information, divided by reports reviewed in an opted-in pilot. Track privacy failures and SDK-induced regressions alongside this measure; increased evidence volume is not success if trust deteriorates.

### 3.4 Non-goals

No guarantee of perfect reproduction, zero crashes, automatic privacy detection, automatic fixes or a particular improvement percentage. No replacement for crash monitoring, ANR profiling, production log aggregation or product analytics. No AI inference on the phone or required cloud LLM.

## 4. Users and use cases

### 4.1 Personas

| Persona | Need | Constraints |
|---|---|---|
| Open-source Android maintainer | Receive useful reports without running a large backend | Small budget; limited maintenance time; AGPL-compatible distribution |
| Tester or ordinary app user | Explain a defect without knowing technical terminology | Accessible UI; understandable consent; poor connectivity; private information on screen |
| Mobile developer | Match a report to the right release and screen | Minified code; multiple build variants; incomplete UI trees |
| Coding-agent operator | Supply grounded evidence to the agent already used for development | No untrusted instructions treated as authority; no hidden model upload; controlled source access |
| Self-hosted team, later | Own delivery, retention and issue routing | Authentication, quota, backup and deletion responsibilities |

### 4.2 Primary use cases

**UC-01 — Visual defect.** A tester sees an overlapping button. They open Report a problem, capture an approved region, mark the overlap, state the expected behavior, review the parcel and export it. The developer receives bounds, device configuration and build metadata, not merely a screenshot.

**UC-02 — Action has no effect.** A tester taps Save and nothing happens. Explicit host-integrated breadcrumbs show the safe action ID and screen transition history. Network metadata may show a failed request if the host enabled the optional adapter. Entered form values are not recorded by default.

**UC-03 — Offline report.** A user creates a report without connectivity. An opted-in draft is retained locally in encrypted form. The user can later export it; no automatic upload occurs in v1. If secure storage is unavailable, the SDK offers an in-memory report rather than falling back to plaintext.

**UC-04 — Sensitive finance screen.** A private host application's development build uses synthetic transactions. In a production integration, finance-specific privacy policies exclude amounts, account data and original messages. If safe image masking cannot be established, the report remains useful with a description and approved metadata while omitting the image and semantic text.

**UC-05 — Agent-assisted triage.** A maintainer imports an approved parcel into a chosen local directory. A read-only MCP server returns the manifest, redacted events and approved image. The agent checks the commit, explains uncertainty and proposes a fix; tests and normal source-control review determine whether the fix is accepted.

**UC-06 — Evidence unavailable.** A protected window, unsupported Compose version or capture timeout prevents collection. The SDK returns a typed omission reason and keeps the report form usable. It does not bypass protection or claim a complete tree.

**UC-07 — SDK malfunction.** An injected collector, lifecycle callback or disk operation fails. The affected feature degrades safely; the host remains operational; SDK diagnostics record only sanitized error codes.

### 4.3 Deferred use cases

UC-08: self-hosted delivery and private issue routing (Phase 6). UC-09: React Native Android integration (Phase 7). UC-10: native iOS and React Native iOS evidence (Phase 8). UC-11: Flutter and constrained assisted replay (Phase 9). These are not prerequisites for Android v1.

## 5. Scope and deliberate exclusions

### 5.1 Release boundary

| Capability | Android v1, Phases 0–5 | Later |
|---|---|---|
| Native Android SDK and Compose reporter | Required | Maintain |
| Manual trigger; optional shake trigger | Required | Additional opt-in triggers |
| Screenshot capture, markup and opaque masking | Required only where the capture policy permits; omission is valid | Additional rendering surfaces after validation |
| Explicit safe screen/action breadcrumbs | Required | Framework-specific adapters |
| Structured host log sink | Required, opt-in | Additional logger adapters |
| OkHttp request metadata | Optional for consumers; required tested v1 adapter deliverable | Other network adapters |
| Host-registered semantic evidence | Required | More automated collection if safe and supported |
| Automatic Compose/accessibility snapshot | Capability-gated by Phase 0; never required to keep the basic reporter usable | Widen supported matrix only with evidence |
| Build provenance and missing-data reasons | Required | CI-assisted symbol association |
| Local draft, review and manual export | Required | User-consented background delivery |
| Local viewer, CLI and read-only MCP adapter | Required | More consumers |
| GitHub/Linear integration | Not required | Trusted relay or maintainer-side adapter |
| Fatal crash capture, ANR capture, video replay | Excluded | Separate future design, if justified |

### 5.2 Explicit exclusions

- No accessibility-service permission, cross-app observation, root, hidden Android APIs or forced accessibility state.
- No microphone, camera, SMS, contacts, location, advertising identifier, clipboard or device serial access.
- No global logcat reading, automatic heap dumps, database dumps, SMS exports or full HTTP bodies.
- No overriding the host's default uncaught-exception handler or consuming its exceptions.
- No screen recording or continuous screenshot history in v1.
- No embedded GitHub PAT, GitHub App private key, Linear secret or model API key in a distributable mobile SDK.
- No agent tool that executes report content, rewrites source, uploads evidence or creates issues automatically.
- No mandatory account, paid gate, telemetry, advertising or dependency on a proprietary capture backend.
- No adoption of an alpha Android library solely to market the SDK as “latest.”

## 6. User and developer journeys

### 6.1 Reporter flow

1. Host app enables reporting according to its settings and privacy notice. Nothing starts just because a library class is loaded.
2. User activates an accessible manual entry point or a configured foreground-only shake gesture.
3. A single report session begins. Eligible on-screen evidence is snapshotted before the reporter obscures it; collector failures become omissions.
4. Reporter shows description, expected behavior and actual behavior. User marks an allowed image with arrows, pen or opaque privacy masks.
5. User sees evidence categories, redacted image and text, capture limitations, the chosen export method and retention information. They can remove optional categories. For system sharing, explain that the receiving app/recipient is selected next; do not claim recipient-specific approval before the chooser opens.
6. Finalization revalidates policy and creates the exact export artifact. If its contents change after review, approval is invalidated and review repeats. Artifact approval and destination selection are separate steps.
7. User opens Android's system share UI, or selects a save destination with the Storage Access Framework. A cancelled SAF operation leaves the host unaffected. Generic sharing cannot reliably report receiving-app completion/cancellation or confirm receipt.
8. UI distinguishes “Share chooser opened” from a confirmed local “File saved.” It never reports “Maintainer received it” without a verified delivery contract. Local encrypted drafts can be deleted from the reporter.

No consent checkbox is preselected for external transfer. Reporting must remain accessible without shaking a device or annotating an image.

### 6.2 Developer integration

1. Review the AGPL obligations and compatibility notice before adopting the library.
2. Add only the artifacts needed. Initialize explicitly from the host's application lifecycle and register the current activity safely.
3. Provide build metadata and safe screen/action IDs. Configure evidence categories; screenshot/semantic text collection is off until a host capture/privacy policy is supplied.
4. Add a manual reporter entry point. Optionally register foreground shake detection, a structured log sink and the OkHttp metadata adapter.
5. Mark sensitive regions and excluded screens. Keep the policy close to the feature that owns the data.
6. Run the integration's privacy fixtures, minified-release smoke, lifecycle tests and resource budget checks.
7. Update the host privacy policy and, where applicable, store disclosures before enabling production collection/export. The host's existing privacy promises must not be weakened silently.

The baseline SDK must not force Hilt, Room, a navigation library, Google Play Services or a particular network stack into the host app.

### 6.3 Agent workflow

1. Maintainer deliberately imports a parcel. CLI validates structure, size, paths, checksums and supported schema.
2. Local MCP process exposes an explicit import root and read-only report IDs, not arbitrary filesystem paths.
3. Agent reads the report as **untrusted evidence**, not instructions. It checks build provenance before drawing source-level conclusions.
4. Unknown build, incomplete tree, guessed repro step or unavailable evidence is reported as uncertainty.
5. Code changes, commands, issue creation and model-provider uploads follow the existing project's permissions; the reporter grants none of those permissions. **Read-only MCP is not an egress guarantee:** the selected client may send returned text/images to its cloud model. Explain this before importing evidence, require the operator's informed choice, and recommend a genuinely local client/model for evidence that must not leave the machine.
6. A synthetic regression test and the project's ordinary build/review process validate any proposed fix.

## 7. Architecture and integration contracts

### 7.1 Dependency direction

```text
Host application
  ├─ explicit configuration + privacy policy + build metadata
  ├─ safe semantic registration + screen/action breadcrumbs
  ├─ optional structured log sink / OkHttp metadata adapter
  └─ reproparcel-compose reporter
       └─ reproparcel-android lifecycle, capture, secure drafts, export
            └─ reproparcel-core schema, limits, policy, redaction, outcomes

Reviewed .reproparcel file
  └─ local desktop CLI / offline viewer
       └─ read-only MCP adapter → user's existing coding client

Future only: approved parcel → authenticated relay → configured issue tracker
```

Core contracts must not depend on Android, Compose, desktop tooling, the relay or an LLM. Desktop consumers implement the same schema and share golden fixtures, not necessarily the same runtime language.

### 7.2 Modules and responsibilities

| Module | Responsibility | Must not do |
|---|---|---|
| `:reproparcel-core` | Versioned models, bounds, redaction policy, state transitions, validation and typed results; Kotlin/JVM | Touch Android APIs, start threads or perform network requests implicitly |
| `:reproparcel-android` | Lifecycle-aware sessions, eligible window capture, encrypted drafts, FileProvider/SAF export | Render host UI, bypass secure windows, retain activities strongly or collect forbidden data |
| `:reproparcel-compose` | Material 3 reporter UI, public host-registration modifiers, privacy region mapping and capability-gated semantic adapter | Require Compose test artifacts in production or assume a complete UI tree |
| `:reproparcel-okhttp` | Opt-in request/response metadata around the host's OkHttp pipeline | Read bodies, change request semantics, delay calls materially or capture SDK traffic recursively |
| `:sample-app` | Synthetic Compose and mixed View/Compose scenarios; controllable faults and privacy fixtures | Include real user reports, credentials or production financial data |
| `:benchmark` | Isolated paired release-build overhead and capture benchmarks | Ship in the host app |
| `desktop/` | Offline validation/inspection/viewer; read-only MCP; fixture conformance | Run arbitrary report content or assume a specific AI provider |
| `schema/` | JSON Schema, format contract, migration rules and synthetic golden parcels | Contain production evidence |
| `server/`, later | Optional ingestion, retention and routing | Become mandatory for local operation |

### 7.3 Public API behavior

Specify API signatures through an ADR and tested examples in Phase 1. The following are contracts, not claims about existing callable APIs:

- `initialize(config)` is explicit, idempotent and nonblocking. Invalid configuration yields a typed failure or disabled feature; it cannot terminate the host.
- `showReporter(activity)` is main-thread coordinated, lifecycle-aware and single-session. Calls without a usable foreground activity return `HostUnavailable`.
- `recordBreadcrumb(...)` accepts only bounded, safe structured fields; it never records arbitrary navigation arguments or entered values.
- `recordLog(...)` accepts structured sanitized events, not unrestricted exception text.
- `registerEvidenceProvider(...)` gives each provider a timeout, byte limit and feature-specific fallback. Host callback failures are isolated at their boundary.
- `setEnabled(false)` prevents new collection and stops sensors/jobs. Sensitive queued work is cancelled; removal of stored drafts is an explicit operation.
- `deleteLocalReports()` clears drafts and SDK caches without touching host data.
- Outcomes distinguish success, cancellation, omission, unsupported capability and recoverable failure. A disabled reporter is not a successful capture.
- No static initializer or automatic manifest provider may activate collection. Public SDK state must be safe before initialization.

### 7.4 Capture and semantics feasibility

PixelCopy's Window overload is available from API 26, motivating a **proposed library minSdk of 26**. A private adopter's higher minSdk is an adopter constraint, not a reason to exclude older library consumers. Validate the real dependency graph before freezing minSdk.

Compose semantics are not a complete representation of every UI element: merged nodes, lazy content, custom Canvas content, separate windows and WebViews impose limitations. AndroidX documents `RootForTest` as a test interface. A production feature must not quietly depend on test hooks, reflection or hidden internals.

Phase 0 compares public accessibility-node-provider access with public host-registered semantic descriptors and bounds. The shipping fallback is **explicit host registration**. Any automatic adapter is best-effort, reports its collection mode and must pass a minified-release compatibility matrix before being enabled. If it cannot do so safely, omit it from v1 and accurately narrow the marketing claim; do not block the local reporter or fabricate evidence.

Screenshot capture covers only approved host-app content. Separate windows, SurfaceView rendering and geometry transformations require explicit tests; unsupported regions/windows are excluded, not guessed. Screenshot detection is not screenshot capture. Android 14's detection callback has permission/notice behavior and is not a v1 dependency.

**Temporal privacy contract:** every asynchronous capture has a unique attempt token, active-window identity and host privacy/layout generation. A verified synchronization strategy must bind the captured pixels to the policy/bounds used for masking; pre/post geometry checks alone are not proof of frame identity. Scrolling, layout/content changes, dialogs, window replacement, policy changes, cancellation and late callbacks invalidate an attempt unless the strategy can establish consistency. Discard stale/unverifiable results before display or persistence. If public APIs cannot establish safe consistency on a surface, disable image collection there. Phase 2 must test dynamic races, not just static mask geometry.

### 7.5 Delivery and storage

v1 uses user-initiated manual export. Use app-private storage, narrow FileProvider paths with temporary read grants, or SAF for user-selected durable output. A system share grant is a transfer permission, not proof of delivery.

Secure draft storage uses Android Keystore-backed authenticated encryption with unique nonces and a reviewed key lifecycle. Exclude drafts and keys from backup/transfer rules. Prefer a small file/index implementation initially; Room is not mandatory. If encryption/key access fails, retain an in-memory draft or report a typed storage failure — **never persist plaintext as a fallback**.

Sensitive report text/images must not enter saved-instance-state Bundles, Compose `rememberSaveable`, navigation arguments, thumbnails, unencrypted editor/temp files, diagnostics or crash attachments. Restore only nonsensitive IDs pointing to authorized encrypted drafts; otherwise lose the draft safely. Reporter/preview-owned windows displaying evidence must use supported secure-window protection; preserve host flags and never weaken its capture policy. Configure sensitive text input to avoid autofill and personalized IME learning where supported, while explaining that third-party keyboards/OS behavior are outside the SDK's absolute control.

Local exported files belong to the user. Deleting an SDK draft cannot revoke a previously shared file, issue attachment, backup or remote copy.

## 8. Evidence parcel specification

### 8.1 Container

A `.reproparcel` file is a bounded ZIP container. Its root contains `manifest.json`; optional entries are referenced explicitly in the manifest. Paths use ASCII allowlisted names with no absolute paths, symlinks, duplicate entries or `..` traversal. Reader must validate before extraction and use an isolated destination.

```text
manifest.json
report.json
events.json
semantics.json             # optional, sanitized
attachments/screen.png     # optional, redacted and flattened
```

Attachments contain final approved pixels only. No original image, EXIF metadata, editable redaction layer, raw HTTP body or hidden original exists in the parcel. Empty or unavailable categories are represented with status/reason, not misleading empty arrays alone.

Consumers reject unreferenced archive entries, unsupported attachment types and mismatches between manifest and container. v1 supports schema-defined JSON and normalized PNG only, not SVG, HTML, scripts or arbitrary files. Before decoding an image, validate bounded dimensions and decoded-byte estimates with overflow-safe arithmetic; encoded ZIP/file size is not a raster-memory limit. Enforce a longest edge of 1600 px, ≤12 MiB decoded pixels per image and ≤24 MiB concurrently decoded pixels. Aggregate JSON parsing is capped at 2 MiB, depth 32 and 10,000 structural nodes. Render/import implementations must enforce these bounds before passing content to an uncontrolled decoder or viewer.

### 8.2 Required manifest fields

| Field | Purpose |
|---|---|
| `schemaVersion` | Integer format version; starts at 1, independent of SDK SemVer |
| `reportId` | Random per-report identifier; not a stable user/device identifier |
| `createdAt` | UTC RFC 3339 timestamp; clock trust limitations remain visible |
| `producer` | SDK version, platform and safe collection configuration identifier |
| `app` | Package, app version name/code, variant and optional git commit/build ID |
| `device` | API level, manufacturer/model, app-window dimensions, density, locale/font scale and orientation; no unique ID |
| `privacy` | Policy version, redaction mode, reviewed category set and preview/content binding |
| `capture` | Capture time, collection modes and per-provider status/omission reasons |
| `entries` | Allowlisted path, media type, uncompressed size and SHA-256 for each referenced non-manifest entry |
| `limits` | Effective producer bounds so consumers understand truncation |

Checksums detect corruption; they are not signatures or proof of origin. A malicious reporter can invent metadata. The manifest cannot hash itself recursively; a whole-parcel hash is computed externally after finalization when needed for consent binding or transport integrity.

### 8.3 Report, events and semantics

**Report:** title, description, expected behavior, actual behavior, optional user-written repro steps, safe current-screen ID and evidence statuses. Free text is untrusted and user-reviewed; stripping known patterns does not guarantee it contains no personal data.

**Events:** nonnegative monotonic milliseconds relative to one explicitly enabled foreground collection segment, with a random ephemeral collection ID. Record trigger offset and snapshot cutoff in `capture`, so pre-trigger five-minute history is not incorrectly anchored to a report session that starts at activation. Freeze the ring at the cutoff; later events are not retroactively added to the approved parcel. Include a bounded sequence number for stable ordering at equal offsets, category, safe action/screen ID, sanitized attributes and outcome. On process restart, begin a new segment rather than joining incompatible clocks. UTC anchoring is optional and may drift; session offsets are not absolute timestamps. Automatic capture records only instrumented observable events.

**Semantics:** approved node descriptors with per-snapshot IDs, optional explicitly registered stable test tags, role, allowlisted static label, bounds, state and parent relation. Password/editable values and sensitive subtree content are omitted. Bounds identify their coordinate space and capture dimensions. Unregistered arbitrary text is excluded by default. Node IDs are not guaranteed stable across launches.

**Provenance:** app commit is supplied at build time by the host and validated syntactically. Missing provenance is reported as `unknown`; never infer it from whichever checkout an agent has open. R8 mappings and source trees stay maintainer-side, not in the report.

### 8.4 Compatibility and retention

- Unknown optional fields are ignored safely; required-field violations reject import with actionable errors.
- Unknown major/schema versions are not treated as compatible. Readers report supported versions.
- Format v1 changes that alter meaning require a new schema version; additive optional fields need documented compatibility tests.
- Writers and readers share synthetic golden parcels and corruption/adversarial fixtures.
- No user report is automatically a repository fixture. Recreate its structure with invented data.
- Drafts default to 72-hour retention, at most 10 reports and 25 MiB aggregate encrypted storage; oldest drafts may be removed with visible policy disclosure.
- Shared temporary copies expire after a documented grant window, initially 24 hours. Grants may be revoked sooner on explicit deletion; cleanup is best-effort, not an exact execution-time promise.
- Persistent user-selected exports are outside the SDK retention scope.

## 9. Functional requirements

| ID | Requirement | Phase / acceptance |
|---|---|---|
| FR-01 | Explicit initialization, no-op before enablement, safe disable/delete | P1; race/idempotency tests |
| FR-02 | Accessible manual reporter; optional foreground shake | P2; TalkBack/device tests; no background sensors |
| FR-03 | One report session and guarded lifecycle transitions | P1–2; rotate, background, duplicate trigger, host destruction |
| FR-04 | Bounded PixelCopy image capture where policy allows | P0/P2; protected-window/timeout/geometry tests |
| FR-05 | Draw/arrow/opaque-mask markup, undo within editor; flattened export | P2; final-pixel and privacy tests |
| FR-06 | Description, expected/actual behavior, review, category removal | P2; validation and accessibility tests |
| FR-07 | Safe host-registered screen/action breadcrumbs | P3; no navigation arguments or input values captured |
| FR-08 | Structured, opt-in log sink | P3; secret fixtures and capacity tests |
| FR-09 | Optional OkHttp metadata artifact | P3; original network result unchanged on reporter failure |
| FR-10 | Explicit semantic registration; capability-gated automatic snapshot | P0/P3; accurate mode/coverage, release-build evidence |
| FR-11 | Build/device provenance with honest unknowns | P1/P3; no device identifiers; multi-variant fixtures |
| FR-12 | Typed evidence omission and SDK diagnostics | P1 onward; no raw exception message in UI or logs |
| FR-13 | Encrypted draft, retention, explicit deletion | P2; key failure, corruption and process-death tests |
| FR-14 | Reviewed versioned parcel export via share/SAF | P2–4; cancellation, preview binding and schema validation |
| FR-15 | Offline validator, inspector and safe viewer | P4; valid/corrupt/adversarial parcels |
| FR-16 | Local read-only MCP tools | P4; two clients, explicit root, no arbitrary file access or writes |
| FR-17 | License notices, sample apps, compatibility documentation | P0/P5; distribution/legal checklist |
| FR-18 | Task impact brief and end-of-session handoff | Every phase; repository process checks |
| FR-19 | Optional authenticated relay and tracker adapters | P6 only; approved contract, quotas and revocation |

v1 MCP surface SHOULD be limited to `list_reports`, `get_report`, `get_events`, `get_semantics` and `get_attachment`. Tool results include evidence availability and warnings. No shell execution, URL fetching, write tools or dynamic tool definitions from report content.

## 10. Privacy, security and consent

### 10.1 Default capture policy

1. Reporting and each optional collector activate only through explicit host configuration.
2. Screenshot and semantic-text collection are off until the host supplies a privacy/capture policy. Static screen/action IDs and nonunique build/device metadata can be enabled separately.
3. Logs, labels and attributes are sanitized **before** entering a ring buffer. Redaction runs again before persistence/export.
4. HTTP bodies, query strings, cookies, authorization headers, request IDs containing personal data and arbitrary URL path segments are not collected. Network capture uses status, method, timing and host-supplied safe route templates; raw URLs are excluded.
5. Editable/password text is never included. Sensitive descriptors/subtrees and their actions are omitted or represented by non-content omission markers.
6. Screenshot policy identifies eligible windows/regions and public/sensitive/excluded content. Unclassified or unsupported capture surfaces are omitted. A successful bitmap copy is not proof that the image is safe.
7. Sensitive regions use **opaque destructive masking**, not blur or translucent paint. Mask padding, transforms, scroll, frame/policy consistency and dynamic layout races must be tested. Mandatory host-policy masks form an irreversible sanitized editor base and cannot be undone; undo may affect only user-added annotations/masks without restoring prohibited pixels. Raw frames exist only briefly in memory, never in preview/saved state; Java/Kotlin cannot promise instantaneous secure erasure, so minimize their lifetime and copies.
8. If policy evaluation, region mapping or final redaction fails, omit the affected evidence. Privacy failure may reduce diagnostics; it must never increase collection.
9. Review shows the exact final image/text, export method and destination where known. For generic sharing, explain that the system chooser selects the recipient next; artifact approval is not recipient-specific confirmation. A content change invalidates the previous approval.

A host's public-label policy is an explicit trust boundary, not an automatic guarantee. Documentation must warn that screenshots and free text can still contain sensitive data, require application-specific testing and prohibit absolute “all PII is detected” claims.

### 10.2 Sensitive app integration

For a privacy-sensitive finance host: start in an isolated development variant with invented transactions. Original SMS, account/card references, bank balances and real financial screenshots are excluded from fixtures and diagnostics. Production enablement requires a reviewed policy, user-facing disclosure and explicit consent. Consent does not override the host's prohibited-data list.

### 10.3 Local and desktop threat model

| Threat | Required control |
|---|---|
| Secret survives structured logging | Allowlisted attributes, no bodies/raw URLs, seeded redaction tests, repeated final validation |
| Screenshot mask misses shifted content | Window/coordinate contract, conservative padding, rotation/scroll tests, fail-closed omission |
| Report tricks agent with instructions | Treat every report field as untrusted data; no executable tool responses; permission boundaries outside the parcel |
| Crafted ZIP traverses disk or expands excessively | Path allowlist, duplicate rejection, entry/count/ratio limits, streaming validation before extraction |
| Viewer executes HTML/JavaScript from report | Text-only rendering, HTML escaping, strict CSP, no external scripts/URLs or active attachment formats |
| Draft exposed through backup or content provider | Keystore-backed encryption, backup exclusions, narrowly scoped FileProvider, temporary read grants |
| SDK leaks activity or retains users' data | Lifecycle cancellation, weak/scoped host references, bounded buffers, deletion/TTL tests |
| Agent requests unrelated local files | Explicit import root, report-ID lookup, attachment allowlist, symlink rejection |
| A fake report supplies false provenance | Mark provenance as supplied/unverified; don't treat checksums as authenticity |

### 10.4 Future transport policy

A future relay uses authenticated HTTPS ingestion with short-lived, project-scoped, write-only credentials, rate limiting, quotas and revocation. A mobile ingestion credential is not a secret that can remain hidden in an APK; design for theft and abuse. Tracker credentials stay server-side or in a maintainer's local tool. No cleartext fallback or disabled TLS verification.

GitHub issue APIs do not provide a generic attachment-upload solution to assume. Phase 6 must verify actual interfaces read-only first and design approved private attachment storage/links. Public issues are never the default destination for user evidence. Deletion policies must explain copies already delivered to third parties.

## 11. Reliability and exception policy

### 11.1 Interpreting “try/catch everywhere” correctly

**Use try/catch everywhere a recoverable exception can enter an SDK-owned execution path or escape into the host:** initialization, platform/lifecycle callbacks, sensors, capture, URI handling, file/index operations, encryption, serialization, provider callbacks, network adapters, workers, desktop import and tool handlers.

Do not sprinkle empty catch blocks around pure operations, swallow coroutine cancellation, catch all fatal errors or wrap composable invocations in invalid try/catch constructs. Defensive boundaries plus constrained inputs, ownership, isolation and tests are the standard. A catch block cannot prevent ANRs, OS process kills, native aborts or all out-of-memory failures.

### 11.2 Mandatory behavior

- Every **SDK-owned** exception-producing boundary converts its recoverable failures to a typed result, diagnostic code and safe user-facing state; no empty catches and no success after a failed operation. Host operations retain their own contracts: in particular, an OkHttp `chain.proceed` response/exception is propagated unchanged, never converted to an SDK result or silently replaced. Only surrounding SDK collection work is isolated; host-provider callbacks explicitly opting into evidence collection use the documented typed provider contract.
- Rethrow `CancellationException` immediately; preserve structured coroutine cancellation. A generic `runCatching` is prohibited in suspend/cancellable paths unless cancellation handling is explicitly correct.
- Catch specific expected exceptions where practical. Use a final `Exception` boundary only at identified SDK/host callback seams; catch only types the subsystem can safely handle.
- Do not broadly catch `Throwable`, `Error`, `VirtualMachineError`, `OutOfMemoryError` or native signals to claim recovery. Specific optional linkage failures need an isolated, documented strategy; v1 avoids JNI dependencies.
- Cancellation and cleanup use `finally` and scoped ownership; cleanup failures must not replace the original outcome or leak a resource.
- Each optional collector can fail independently. If screenshot capture fails, retain description; if semantic collection fails, retain approved non-semantic evidence. If privacy fails, omit evidence; if serialization fails, do not export a misleading partial archive.
- Reporter callbacks must not throw into host callbacks. The OkHttp adapter must preserve the host's response/failure and avoid consuming streams.
- Use supervised SDK-owned jobs so one collector cannot cancel the host scope. Supervision does not handle exceptions by itself; child jobs still need result handling and a sanitized last-resort diagnostic hook.
- No blocking disk/network operations on the UI thread; try/catch does not make blocking safe.
- Production UI uses localized error resources and recoverable action states, never raw exception messages. Diagnostic records use safe codes/class names; causes may contain secrets and cannot be logged verbatim.
- Compose UI represents failures as state. Do not use try/catch around composable calls as an error-boundary mechanism. Validate arguments, isolate effects, keep rendering simple and test release behavior.

### 11.3 State machine

```text
Disabled → Ready → Capturing → Editing → Reviewing → Finalizing → Exportable
                       │          │           │             │
                       └─ omissions/error ────┴── retry/cancel ┘

Exportable → user-initiated save → Completed / Cancelled / ExportFailed
Exportable → user-initiated share → ShareLaunched / ExportFailed
Any active session → host destruction/disable → safe cancellation + cleanup
```

State changes and collector outcomes must be deterministic and testable. A lifecycle interruption cannot dispatch duplicate exports. `Completed` applies only to a confirmed local save/finalization. Generic sharing has a separate `ShareLaunched` outcome; receiving-app completion/cancellation and remote receipt remain unknown.

### 11.4 Reliability acceptance

Inject denied capture, disconnected network, missing activity, sensor failure, revoked URI access, corrupt draft, full disk, invalid config, provider exception, serialization failure, timeout and cancellation. Verify no host crash from these tested recoverable conditions, no privacy fallback, no leaked job/resource and accurate UI outcome. Known failures block stable release; the project never advertises an unconditional crash-free guarantee.

## 12. Current Android engineering standards

Use current **stable** Android tooling and principles, verified at Phase 0 and on dependency upgrades. Do not copy version numbers from an adopter without checking compatibility or force consumers onto a new compiler unnecessarily.

| Area | Required standard |
|---|---|
| Language/toolchain | Kotlin; supported stable AGP/Gradle/JDK combination; Gradle wrapper; one version catalog; pinned versions, no dynamic `+` dependencies |
| Android baseline | Proposed library minSdk 26; sample targets current stable SDK and current distribution policy; library does not silently raise host targetSdk |
| UI | Jetpack Compose + stable Material 3; shared semantic tokens; unidirectional data flow; immutable UI state |
| Lifecycle | Lifecycle-aware collection; appropriate ViewModels/state holders; no `GlobalScope`, leaked activities or manual detached host work |
| Concurrency | Structured coroutines; injected dispatchers/time sources; main-thread platform operations; off-main bounded CPU/IO; correct cancellation |
| Navigation | Reporter is host-navigation agnostic. A new Compose-only sample may use stable Navigation 3; support existing Navigation Compose via opt-in integration without forcing migration |
| Edge-to-edge | Correct status/navigation/IME/cutout insets; predictive back via supported APIs; don't bypass host window policy |
| Adaptive UI | Rotation, multi-window, resizable windows, tablets/foldables, changing density/font scale; capture relative to the actual app window |
| Accessibility | TalkBack, switch/keyboard navigation, localized descriptions, focus, adequate contrast, ≥48 dp targets and 200% font scale; no gesture-only reporting |
| Storage | App-private files, SAF/FileProvider; reviewed Keystore authenticated encryption; explicit backup exclusions; no broad storage permissions |
| Background work | No background collection. WorkManager only if future delivery/maintenance genuinely requires durable scheduling; no periodic worker just to keep SDK alive |
| Dependency design | Optional adapters; no mandatory DI/database/network framework; no Play Services or native binaries in v1 |
| API stability | Explicit visibility, semantic versions, binary/public-API checks, documented thread contracts and Kotlin/Compose compatibility |
| Release quality | R8-minified sample smoke, narrow consumer rules, resource/license accounting, release-device performance, lint and strict-mode development checks |
| Security | Least privilege; no unsafe exported components, trust-all TLS, embedded provider secrets, arbitrary WebView scripts or data collection by defaults |
| Documentation | Current integration examples; explain limitations and permission effects; every advertised feature maps to an acceptance test |

Navigation 3's stable status was verified during research (stable 1.2.1 on the viewed release page). This is a dated observation, not a required forever-version. Runtime semantics collection and support for advanced surfaces remain separately gated.

## 13. Performance and resource budgets

These are initial **proposed release thresholds**. Phase 0/early benchmarks must verify feasibility on a named low/mid-range physical device; any revision requires an ADR with evidence, not silently moving the goalposts.

| Metric | Initial target / bound |
|---|---|
| Disabled SDK | No sensor listener, capture, network job or retained report buffer; dependencies may still contribute binary/class-loading overhead |
| Enabled idle ring buffers | ≤1 MiB combined evidence buffer; capacity enforced by bytes and event count |
| Capture memory | ≤24 MiB incremental peak on benchmark scenario; image allocation bounded before capture; longest image edge ≤1600 px |
| Trigger to usable composer | p95 ≤300 ms, excluding slow image collection; placeholder/omission keeps UI usable |
| Image result | ≤2 s timeout per capture; at most one bounded retry for a documented transient result |
| UI-thread collector work | Target ≤4 ms per scheduled slice; long traversals chunked/omitted with limits rather than blocking frames |
| Startup overhead | Paired release-build p95 incremental startup ≤10 ms on the declared benchmark device; no synchronous IO |
| User-action history | Last 20 explicitly observed actions, maximum age 5 minutes; no text input values |
| Screen/diagnostic breadcrumbs | At most 50 safe events; maximum age 5 minutes |
| Structured logs | At most 200 events and 256 KiB; both limits apply |
| Network metadata | At most 50 entries and 128 KiB; bodies/headers/raw URLs excluded |
| Semantic snapshot | At most 500 nodes, bounded depth 32 and 512 KiB serialized; truncation recorded |
| Parcel | ≤5 MiB compressed, ≤10 MiB uncompressed, ≤16 entries; initial maximum per-entry compression ratio 100:1; unreferenced entries rejected |
| Consumer parsing/rendering | JSON ≤2 MiB, depth ≤32, ≤10,000 structural nodes; PNG longest edge ≤1600 px, decoded pixels ≤12 MiB per image / ≤24 MiB concurrently |
| Local drafts | ≤10 reports, ≤25 MiB encrypted total, default 72-hour TTL |
| APK overhead | Target ≤1 MiB added minified compressed size for core+Android runtime excluding Compose UI and optional adapters; report each artifact separately |

Use elapsed monotonic time for durations and injected clocks for tests. Measure APK/dependency-size and startup deltas against an otherwise identical **SDK-absent** build; an initialized-but-disabled SDK still contributes artifacts. Separately compare enabled collection with the same SDK installed but disabled to isolate runtime overhead. Measure frame regression, allocations and leaks with paired release builds and the same scripted interaction.

Initial jank gate: enabled collection must not increase p95 frame duration by more than 5% or the proportion of frames missing the device's current frame deadline by more than 1 percentage point over the paired disabled baseline, measured across at least 5 runs of at least 1,000 frames each on the declared device. Record refresh rate, repeat counts, thermal conditions and noise; revise the criterion only through an evidence-backed ADR. Averages alone are insufficient. Stable release has no unresolved SDK-caused ANR or failure of this gate.

## 14. Testing and release gates

### 14.1 Test layers

- **Pure JVM tests:** schema, validators, redaction, state machine, byte/count limits, clock handling, failure mapping and migrations.
- **Property/adversarial tests:** hostile strings, deeply nested JSON, duplicate ZIP entries, traversal, compression bombs, malformed timestamps and truncated files.
- **Android integration tests:** PixelCopy, secure windows, Keystore, providers, lifecycle, sharing, URI revocation and mixed View/Compose capture.
- **Compose UI tests:** accessibility, font scale, themes, focus, keyboard/IME, predictable back, rotation and annotation tools.
- **Release-device tests:** R8/minification, actual evidence generation, geometry/masking, oldest supported API and current stable API. Debug-only success is not release evidence.
- **Desktop conformance:** the same golden parcels across CLI/viewer/MCP; unsupported versions and malicious attachments rejected; all report text rendered inert.
- **Privacy fixtures:** synthetic secrets in input fields, query/header/body shapes, log attributes, sensitive labels and screenshot regions; validate both archive contents and flattened pixels.
- **Performance tests:** paired startup/capture/frame/resource benchmarks, bounded memory and leak checks.
- **Manual checks:** share targets, screen readers, permission/protection behavior and device-specific rendering. Document what automation cannot prove.

### 14.2 Stable release gates

1. All unit, contract and relevant integration suites pass; skipped device checks are explicitly listed and cannot satisfy a required gate.
2. Lint has no unreviewed new issue; compiler/deprecation warnings are fixed or explained with evidence. Do not blanket-regenerate baselines to make CI green.
3. Public API/serialization changes have compatibility evidence and correct versioning.
4. Release-minified sample passes capture, masking, export and import on the supported device/API matrix.
5. Fault injection and seeded privacy tests pass with no known leakage or unhandled recoverable SDK exception.
6. Performance budgets have recorded measurements; no known unresolved host leak or unacceptable frame/ANR regression.
7. SDK and desktop import threat model have focused independent review.
8. Dependency licenses, SPDX headers, third-party notices, source archives and AGPL distribution requirements are reviewed.
9. Maven coordinates are verified; artifact POM includes accurate license, SCM and project metadata; signed publication and downloaded-artifact smoke are checked.
10. Documentation reflects only shipped features; handoff, project state, compatibility matrix and release notes agree.
11. Every goal G-01–G-07 passes its threshold with evidence: G-01 has ≥80% qualifying reports across ≥20 pilot defects; G-02 has both external developers finish baseline integration within 30 minutes each; G-03/G-04 pass reliability/privacy gates; G-05 works in two clients; G-06 passes offline/no-account flow; G-07 has complete versioned contracts and governance. Record failures and the pilot method, not just participant counts.

Initial compatibility target: API 26, API 30 and current stable Android, with at least one physical low/mid-range device and one current device/emulator. Compose support covers the pinned launch stable line; add an immediately preceding supported line only after its release-build tests pass. Do not claim support for every Compose version or OEM.

## 15. Phase-by-phase delivery

### Delivery map

| Phase | Outcome | Release consequence |
|---|---|---|
| 0 | Feasibility, product validation and legal/technical decisions | No shipping claim; go/no-go evidence |
| 1 | Core schema, safe runtime and governance foundation | Internal runnable sample |
| 2 | Accessible local reporter and safe image export | Internal end-to-end demonstration |
| 3 | Bounded diagnostics and semantic evidence | Internal agent-useful parcels |
| 4 | Offline tools and read-only agent access | Public alpha candidate `0.1.0-alpha.1`, only if gates pass |
| 5 | Dogfooding, independent review and release hardening | Android + desktop stable `1.0.0` after all stable gates |
| 6 | Optional trusted delivery and tracker routing | Separately approved 1.x addition |
| 7 | React Native Android adapter | Separate platform compatibility release |
| 8 | Native iOS, then React Native iOS | Separate native capture project/gates |
| 9 | Flutter and carefully validated extensions | Demand-driven roadmap, not commitments |

No calendar estimate is asserted. A solo maintainer should work vertically through one safe end-to-end path, then widen collection. Every phase ends with a review, updated project state and numbered handoff. Public publication requires approval even when an artifact is technically ready.

### Phase 0 — Prove the hard parts before promising them

**Goal:** demonstrate that a privacy-safe native reporter and portable parcel are useful and technically viable without reliance on unstable production hooks.

**Work packages**

- Interview at least five potential AGPL-compatible adopters; compare their workflow with Sentry/manual tickets. Confirm the local/portable/agent-ready difference matters.
- Freeze provisional product naming only after preliminary trademark screening; verify Maven namespace with Sonatype. Do not purchase/reserve/publish without approval.
- Record exact AGPL-3.0-only intent and a documented adoption warning. Have distribution guidance reviewed before stable release.
- Build a disposable synthetic Compose spike for eligible PixelCopy capture, opaque masking, rotated bounds and protected-window omission.
- Compare public accessibility-provider evidence with host-registered semantic descriptors. Test current stable Compose in an R8-minified build on minimum/target APIs; no test dependencies or reflection in the shipping path.
- Try merged semantics, lazy offscreen items, editable fields, custom Canvas, dialogs and mixed View content. Record coverage; exclude unsupported surfaces.
- Verify toolchain/minSdk/API matrix and memory/latency estimates; establish benchmark baselines.
- Establish AGENTS.md, source-of-truth state, ADR numbering and handoff protocol before retained implementation.

**Deliverables:** feasibility ADRs, synthetic evidence samples, toolchain/compatibility baseline, threat-model draft, adoption validation summary and handoff.

**Exit / go-no-go**

- Safe basic reporter/capture/export path is feasible, and at least three interviewees identify a concrete use case.
- Every capture experiment reports tested build and limitations; no claim that automatic semantics is solved without release evidence.
- If automatic semantics is unsafe/unavailable, select explicit registration, retain the product and narrow the scope publicly.
- If privacy-safe capture or local usability is not achievable within budgets, reduce image scope or pause with documented reasons. Do not ship around a failed gate.

### Phase 1 — Establish core contracts and safe runtime

**Goal:** make every later feature obey one schema, policy and error model.

**Work packages**

- Create the minimal modules and synthetic sample; pin compatible stable dependencies through one catalog.
- Define schema v1, JSON Schema, size/depth limits, producer/reader behavior, omission codes and typed outcomes.
- Implement explicit configuration, idempotent initialization, safe no-op states, enable/disable, activity ownership and supervised lifecycle-scoped jobs.
- Implement policy classification, intake redaction and bounded rings. Establish no-default-collection tests.
- Supply host build metadata injection with honest unknowns; no machine/user paths in public artifacts.
- Add CI for compile, JVM tests, lint, API/schema checks, secret/license checks and synthetic fixture conformance.
- Add ADRs for capture capability, consent boundaries and chosen desktop runtime. A maintained LTS TypeScript/Node toolchain is a reasonable desktop default, but Phase 1 must inspect its actual MCP SDK interface and verify dependencies before adopting it.

**Deliverables:** tested core/runtime, schema spec, sample app skeleton, CI gates and documented API contracts.

**Exit:** invalid configuration/provider errors cannot terminate the sample; disabled mode has no active collectors; tests prove limits/redaction/cancellation; schema fixtures validate; current project state accurately identifies unfinished UI/capture work.

### Phase 2 — Ship one safe local reporter flow

**Goal:** a user can report, review and export an image-backed or text-only parcel without a backend.

**Work packages**

- Create Material 3 reporter UI with manual trigger, optional foreground shake, duplicate-trigger guard and lifecycle recovery.
- Implement eligible PixelCopy capture; capture before reporter overlays; check secure policy; handle timeout and cancellation.
- Register capture bounds/privacy regions; omit unsafe/unsupported surfaces; apply opaque masks before any image persistence.
- Implement pen/arrows, undo, destructive masking and final flattened image. Annotation is optional, not a prerequisite to send.
- Implement form, evidence selection, exact preview binding and final privacy validation.
- Implement encrypted drafts, key-failure behavior, quotas/retention/deletion and backup exclusions.
- Implement narrow FileProvider/SAF manual export and accurate local outcome messages.

**Deliverables:** offline end-to-end synthetic sample with valid parcel; lifecycle/accessibility/privacy tests; measured capture costs.

**Exit:** offline flow works; denied capture remains a usable text-only report; no raw image/report data leaks through disk, saved-state, editor undo or preview-window capture; scrolling/layout/dialog/cancellation/late-callback race fixtures discard stale or unverifiably mapped images; process death/full disk do not produce plaintext fallback or duplicate export; 200% text, TalkBack and rotation are usable; sharing never falsely claims receiving-app completion.

### Phase 3 — Add evidence, not surveillance

**Goal:** enrich approved reports enough to ground an investigation without making the SDK invasive.

**Work packages**

- Add explicit safe screen/action APIs and adapters that never collect arguments, form values or navigation stack contents implicitly.
- Add structured logging with allowlisted fields and before-buffer sanitization.
- Add separate OkHttp metadata adapter; exclude bodies, headers, raw URLs and SDK-owned traffic. Verify pass-through behavior and overhead.
- Add host-registered semantic descriptors/bounds and, only if Phase 0 passed, an automatic capability-gated adapter.
- Add provider omission/truncation status and snapshot/time/provenance consistency.
- Extend synthetic privacy fixtures to every evidence category and test malicious content as data.

**Deliverables:** agent-useful golden parcels, optional adapters, coverage matrix and per-feature integration documentation.

**Exit:** all evidence is bounded; unsupported semantics has an explicit reason; no input/finance/token fixtures escape; requests retain host behavior on success/failure/cancellation; an unfamiliar developer can identify the affected sample screen/build from the parcel.

### Phase 4 — Make the parcel independently useful

**Goal:** local developers and two different coding clients can consume the same file safely.

**Work packages**

- Implement streaming/bounded validator and CLI inspection with machine-readable JSON plus concise human-readable output.
- Implement an offline viewer that renders only escaped text and approved images, never arbitrary embedded HTML/scripts.
- Implement local read-only MCP with an explicit import root/report IDs, bounded results and no write/execute/upload tools.
- Verify the selected MCP SDK against actual read-only protocol/tool results; do not build against guessed schemas.
- Test hostile ZIPs, prompt-injection text, unsupported schema, symlinks, permission denial and missing attachments.
- Publish draft integration recipes and synthetic demo reports; include exact license/compatibility warning before installation instructions.

**Deliverables:** CLI/viewer/MCP packages, conformance fixtures, recorded two-client integration evidence and alpha candidate.

**Exit:** full capture → reviewed export → offline import → read-only agent inspection works; one client is not the sole test oracle; no arbitrary file read/execution path; hostile PNG dimensions/decoded-byte estimates, unreferenced entries, unsupported types, parser-depth/count and compression-ratio fixtures are rejected before dangerous allocation/rendering; declared consumer limits are enforced. Explain client/cloud-model egress before evidence access. All alpha claims link to actual tests and known limitations. A public alpha release is opt-in and requires publication approval.

### Phase 5 — Validate production suitability and cut Android v1

**Goal:** maintainers can rely on the documented contracts and accurately assess privacy/licensing costs.

**Work packages**

- Integrate into a private adopter's development build with synthetic data first. Keep its production package/data untouched until separately authorized.
- Run the 20-report pilot and external integration exercise; publish counts/method without real evidence.
- Review privacy and security independently; fix all release blockers and distinguish real-device verification from JVM-only checks.
- Validate release-minified APK capture, Keystore, masks, UI and budgets across the declared support matrix.
- Review AGPL adoption guidance, source delivery and dependency compatibility; use inbound contribution certification, not unrequested copyright assignment.
- Freeze schema/API v1, migration promises, documented limits and compatibility versions; finish quick start, sample, troubleshooting, privacy policy examples, notices and security-reporting instructions.
- Verify signed Maven publication/source/javadoc artifacts and desktop package manifests; install the downloaded candidate into a clean consumer.
- Publish only after the maintainer authorizes publication and the stable-release checklist is complete.

**Deliverables:** supported Android/desktop `1.0.0`, compatibility matrix, license/source artifacts, release notes, pilot summary, security review and next-phase handoff.

**Exit:** every gate in Section 14 and goal G-01–G-07 passes, including ≥80% qualifying reports among ≥20 pilot defects and both external baseline integrations within 30 minutes each; no unresolved critical privacy/security/reliability issue; measured resource/jank budgets; honest description of semantics/repro limitations; no sensitive reports committed. If gates do not pass, remain alpha/beta and document the blocker rather than relabeling maturity.

### Phase 6 — Optional delivery without vendor lock-in

**Entry condition:** Android v1 is stable and several actual adopters request shared delivery rather than file export. Write a separate transport/server design before coding.

**Goal:** receive approved parcels on a small self-hosted service and route private issues without putting tracker secrets into apps.

**Work packages:** verify tracker/API contracts read-only; select a low-complexity server/database stack and review its schema; authenticated HTTPS ingestion; project-scoped write-only credentials; abuse quotas; idempotency; encrypted-at-rest evidence; private links; retention/deletion; audit logs without report content; reliable user-consented retries only where needed; GitHub App/Linear credentials server-side; backups and source-download offer for AGPL-covered deployments.

**Exit:** revoked/stolen credentials cannot read evidence or exceed bounded impact; duplicate retry creates one report; destinations remain explicit; deletion and source-offer behavior tested; file export still works without the server. Do not claim a tiny VPS specification until measured. Start with one tracker, not both.

### Phase 7 — React Native, Android first

**Entry condition:** validated demand and native Android capture/API stability.

**Goal:** reuse Android runtime through a thin React Native module while preserving the same schema/policy.

**Work packages:** support current maintained React Native architecture; typed JS APIs; explicit native initialization; bridge-safe errors/cancellation; Hermes/release tests; lifecycle and mixed native/JS screens; JS breadcrumbs; unchanged privacy gates; an Expo development-build/config-plugin path if native code is required.

**Exit:** no “works in Expo Go” claim unless actually possible and tested; native dependencies generally require a custom development build. Android parcel conformance matches native SDK; two distinct consumer examples pass. React Native does not provide iOS capture “for free”; iOS remains Phase 8.

### Phase 8 — Native iOS, then React Native iOS

**Entry condition:** separate funded/maintained capacity and demonstrated demand; an iOS technical/privacy design is approved.

**Goal:** implement native Swift capture and scene/lifecycle/privacy behavior against the same portable schema.

**Work packages:** evaluate SwiftUI/UIKit capture and semantics limits, screen protection behavior, multiple scenes, structured concurrency, storage/Keychain, accessibility, supported iOS releases and App Store rules; build native samples first; then expose a React Native iOS module. Do not assume Android semantics or protection APIs have direct equivalents.

**Exit:** native iOS capabilities and omissions documented; supported-device/release privacy and performance gates pass; cross-platform golden parcels converge; source/license distribution guidance reviewed. Lack of complete SwiftUI tree access is reported honestly.

### Phase 9 — Demand-driven extensions

Candidates: Flutter adapter; additional logger/network adapters; maintainer-side issue helpers; reproducible synthetic test generation; constrained Maestro recipes; deduplication and richer provenance.

Each candidate requires actual adopter demand, a separate scope/design, no degradation of core privacy/reliability and dedicated maintainership. Automated repro steps are suggestions unless observed and validated. Any execution, source modification or external write requires explicit authorization. Fatal crash monitoring, video/audio, database capture and app-store publishing are separate products, not casual additions to this library.

## 16. Repository and agent working contract

### 16.1 Required repository files

| Path | Purpose |
|---|---|
| `AGENTS.md` | Mandatory, tool-agnostic engineering and safety contract; read before work |
| `docs/PRD.md` | This product baseline, placed here when the real repository is created |
| `docs/PROJECT_STATE.md` | Concise current phase, completed gates, active task, blockers, next action and latest handoff link |
| `docs/handoffs/NNNN-YYYY-MM-DD-topic.md` | Append-only work-session handoff records |
| `docs/adr/NNNN-topic.md` | Architectural/contract/privacy/license decisions and supersession links |
| `schema/` | Versioned format and synthetic conformance fixtures |
| `README.md` | Accurate quick start, license warning, current scope and verified examples |
| `LICENSE` | Exact official GNU AGPLv3 license text |
| `NOTICE` / third-party notices | Required preserved dependency notices and attributions |
| `CONTRIBUTING.md`, `SECURITY.md`, `CHANGELOG.md` | Contribution/review, private vulnerability reporting and releases |

Use uppercase **`AGENTS.md`** as the canonical filename, not competing `agents.md` and `AGENTS.md` files. Keep it concise; do not paste the entire handoff history into it. Project state links to the latest handoff and ADRs. The starter AGENTS.md accompanying this PRD contains the required contract.

### 16.2 Bigger-picture reasoning for every task

Before code, documentation, configuration or an adapter task, write a short impact brief in the task/PR:

1. Which goal/use case and phase does this serve?
2. Which callers, artifacts, schema readers, policies, consent surfaces and tests are affected?
3. Could it increase data collection, privileges, failure propagation, binary size or resource cost?
4. Does it preserve backward compatibility, privacy defaults, AGPL obligations and host behavior?
5. What is the smallest coherent change, acceptance evidence and safe rollback?

Think system-wide, implement within scope. Bigger-picture thinking is **not** permission to redesign unrelated parts, add speculative features or expand the license/permission surface. If a task conflicts with a load-bearing invariant, report the conflict and seek the consequential decision before changing it.

### 16.3 Session protocol

**Start:** read AGENTS.md, PRD sections relevant to the task, PROJECT_STATE.md, latest handoff and relevant ADRs; inspect working-tree state and actual implementation; never assume a handoff's success claim is proof.

**Work:** plan a bounded outcome; write failure/privacy tests before behavior changes; keep changes reviewable; record decisions and failing approaches; do not overlap another contributor's edits; preserve unrelated user work.

**End, including incomplete work:** run relevant checks; write one numbered handoff; update PROJECT_STATE.md with exact status and latest handoff; disclose unresolved failures/untested checks; do not mark the phase complete because code compiles. Do not commit, push, publish or alter permissions unless authorized.

### 16.4 Mandatory handoff contents

```markdown
# NNNN — YYYY-MM-DD — Work topic

## Objective and scope
Goal/use case, phase, task, authorization boundaries.

## Starting state
Branch/base commit if applicable; pre-existing changes; relevant prior handoff.

## Work completed
Files/components changed; observed behavior; important decisions/ADRs.

## Verification evidence
Exact commands or test names; build variant/device/tool versions;
results, exit codes/counts where available; checks not run and why.

## Privacy, reliability and compatibility impact
Collection/consent changes; typed failures/cancellation; schema/API/license impact.

## Incomplete work and blockers
Known failures and uncertainty; what is not safe to claim or ship.

## Next exact action
Concrete first step, relevant files/tests and dependencies; stop conditions.

## Resume/rollback notes
Safe continuation, pending approvals and reversible rollback approach.
```

Never include real screenshots, report bodies, personal paths, credentials or production evidence in handoffs. Record synthetic cases, categories and aggregate results. Concurrent contributors coordinate sequence numbers and update the latest-state pointer without overwriting each other. A resumed session verifies file state and reruns stale evidence when necessary.

## 17. Licensing and community governance

### 17.1 Exact license decision

The requested license is GNU AGPL 3.0. This baseline uses **`AGPL-3.0-only`**, not an unstated later-version option, permissive exception or dual license. A host application's own `AGPL-3.0-or-later` status does not automatically determine this new project's license. Changing “only” to “or-later” requires an explicit maintainer decision and consistent notices.

First-party SDK, tooling, server code, schemas and documentation use the declared project license; preserve third-party licenses/notices. Put official license text in LICENSE, use SPDX headers where appropriate, and accurate POM/package metadata. Do not paraphrase the license as the binding text or promise a legal outcome this PRD cannot establish.

### 17.2 SDK adoption implications

AGPL is strong copyleft. An Android app that links/includes this SDK may form a combined/derivative work and generally cannot assume it can remain proprietary merely because the SDK is an AAR. Distribution can require corresponding source, notices and compatible licensing for the covered combined work. **No proprietary-linking exception is granted.** Explain this prominently before installation; get qualified legal review for disputed integration/distribution cases.

AGPL Section 13 additionally requires a modified version supporting remote interaction to prominently offer corresponding source to users interacting with it over a network. It is not a rule that every program making any HTTP request must disclose all unrelated software, nor is it a ban on charging money. Preserve distribution/source rights and applicable notice obligations. A future hosted component must implement an appropriate source offer for the deployed covered version.

The data format is not a mechanism for granting the project copyright over a user's bug report. Users retain applicable rights in their submitted data. Independently implemented consumers and interpretation of code-linking boundaries require proper legal assessment; do not promise an AGPL exemption merely because a component is in another process.

### 17.3 Contribution model

- Default inbound rule: contributions are licensed under the same project license and certified with Developer Certificate of Origin sign-off.
- No copyright assignment or additional CLA is imposed by this PRD. The maintainer may propose one separately, with community/legal review.
- Every dependency's license is checked before adoption; retain MIT/Apache/OFL notices correctly. Reject incompatible/noncommercial/unknown-license dependencies until resolved.
- Changes require reviewed tests for behavior, privacy defaults, compatibility and failure handling. Collection expansion and public contracts require ADRs.
- Security reports must have a private channel selected before publication; never ask users to upload sensitive evidence to a public issue.
- Templates request synthetic/minimal reproductions and explain how to remove private data. Contributors must not “sanitize” production data into public history by assumption.

## 18. Risks and decision register

| ID | Risk / question | Response / decision gate |
|---|---|---|
| R-01 | Sentry already meets many adopters' needs | Phase 0 validates portability/local tooling value; pivot scope or stop if no demand |
| R-02 | Automatic Compose extraction relies on unstable hooks | Explicit registration is baseline; automatic adapter must pass public-API release matrix |
| R-03 | Screenshot privacy coverage is incomplete | Disabled until policy; conservative omission; opaque masks and geometry tests; no absolute PII promise |
| R-04 | “Agent-ready” is mistaken for automatic repair/replay | Read-only evidence and provenance; no execution tools; suggestions labeled uncertain |
| R-05 | SDK causes slowdown, leaks or callback failure | Bounds, structured concurrency, failure isolation, paired release benchmarks |
| R-06 | AGPL limits proprietary adopter pool | Intentional tradeoff; publish warning and validate AGPL-compatible audience |
| R-07 | Strong copyleft guidance is legally overstated | Official text plus qualified review; do not claim all networking triggers Section 13 |
| R-08 | First-version scope grows into an observability platform | No backend/crash/video/analytics requirement; one vertical flow; separate expansion designs |
| R-09 | MinSdk/toolchain/Compose support becomes stale | Record tested stable versions; optional modules; new compatibility only after evidence |
| R-10 | Public tracker integration exposes private reports or credentials | Deferred trusted delivery; verify real APIs; private routing and server-side secrets |
| R-11 | New brand has an undiscovered conflict | Preliminary search only; naming/trademark review before public branding |
| R-12 | Handoffs become stale or unverifiable | Single concise state pointer; commands/results; compare notes against actual files |

**Fixed decisions:** Android-first; local/manual export v1; no runtime LLM; no telemetry; fail-closed privacy; read-only agent adapter; AGPL-3.0-only; resumable work records.

**Evidence-gated decisions:** exact Maven namespace, stable toolchain versions, final minSdk/dependency matrix, automatic semantics scope, measured resource thresholds, desktop dependency choices and public naming clearance. Each has a Phase 0/1 owner (the maintainer) and an explicit exit gate; none is silently presumed solved.

## 19. Launch and maintenance

### 19.1 Launch package

README quick start; small synthetic sample; public schema; AGPL adoption warning; supported matrix; local viewer/CLI/MCP recipe; privacy-policy integration guidance; performance methodology; known limitations; private security reporting; contribution guide; release notes; source/license artifacts.

Show a real running sample and exported synthetic parcel, not a fabricated mock that implies capabilities not implemented. Explain automatic vs host-registered evidence, manual share vs verified delivery and data retained outside SDK control.

### 19.2 Post-release commitments

- Triage security/privacy failures before new feature requests. A confirmed evidence leak or SDK-induced host crash can require disabling the affected feature and a patch.
- Patch known critical supported-version defects promptly according to actual maintainer capacity; do not invent a commercial SLA.
- Review stable Android/Compose/Kotlin changes before widening support. Deprecations affecting behavior are not cosmetic.
- Version APIs/schema deliberately; publish migrations/deprecations and supported maintenance lines.
- Require the same privacy/performance/release gates for every platform adapter.
- Keep handoffs and project state current; preserve decision history with supersession rather than silent rewrites.
- Funded hosting or commercial services may be considered later, but may not secretly weaken the requested AGPL license or disable local use.

### 19.3 Definition of complete Android v1

A clean consumer can install the verified artifacts, enable the documented reporter safely, produce an approved parcel offline, validate/view it locally and expose its bounded evidence to two coding clients. Supported release devices pass reliability/privacy/performance checks. Licenses/source obligations and limitations are understandable before adoption. Every completed task has reproducible handoff evidence. Future platform/server features are unnecessary to achieve this definition.

## 20. Sources and verification boundaries

The following primary sources were read with the built-in browser during preparation. Dates/versions are snapshots; implementation must re-check maintained interfaces and supported versions.

1. [Sentry Android User Feedback](https://docs.sentry.io/platforms/android/user-feedback/) — feedback, shake and session-replay integration; self-hosted compatibility requirements.
2. [Sentry self-hosted requirements](https://develop.sentry.dev/self-hosted/) — existing self-hosting option and substantial infrastructure footprint; not a basis for denying it exists.
3. [Android PixelCopy reference](https://developer.android.com/reference/android/view/PixelCopy) — supported capture API and result/error handling.
4. [AndroidX RootForTest reference](https://developer.android.com/reference/kotlin/androidx/compose/ui/node/RootForTest) — explicitly test-oriented; production extraction requires separate validation.
5. [Navigation 3 release notes](https://developer.android.com/jetpack/androidx/releases/navigation3) — stable Compose navigation library; choose stable, not the displayed alpha dependency by accident.
6. [Android 14 screenshot detection](https://developer.android.com/about/versions/14/features/screenshot-detection) — detection is separate from capture and has permission/notice implications.
7. [GNU AGPLv3 official text](https://www.gnu.org/licenses/agpl-3.0.html) — binding license, distribution obligations and Section 13.
8. [Sonatype namespace registration](https://central.sonatype.org/register/namespace/) — namespace ownership/verification; organization namespaces are not automatically provisioned as personal identities.
9. [BugDrop repository](https://github.com/bugdrophq/bugdrop), [BugPin repository](https://github.com/aranticlabs/bugpin), [Quash repository](https://github.com/Oscorp-HQ/quash-max) — prior art; inspect current implementation/licenses before borrowing code.
10. [Exact-name GitHub check](https://github.com/search?type=repositories&q=%22reproparcel%22), [npm endpoint](https://registry.npmjs.org/reproparcel), [pub.dev endpoint](https://pub.dev/api/packages/reproparcel), [registry RDAP result](https://pubapi.registry.google/rdap/domain/reproparcel.dev) — preliminary naming observations only, not registration or trademark clearance.

**Verified locally:** A private host application is Kotlin/Compose with minSdk 30, existing numbered handoffs and AGPL-3.0-or-later notices. No adopter file was modified. Its full dependency compatibility, current release behavior, production privacy implications and this new SDK's feasibility were not tested during PRD writing.

**Not verified by this document:** working SDK/APIs, automatic production semantics extraction, measured memory/latency or adoption targets, legal clearance, Maven publishing access, a hosted relay, artifact publication or cross-platform capture. Those are the phased acceptance work, not hidden assumptions.
