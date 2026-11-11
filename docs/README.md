# ReproParcel documentation

Start here if you are adopting, integrating, reviewing or contributing to
ReproParcel.

## Project

| Document | Purpose |
|---|---|
| [README](../README.md) | Overview, quick start, screenshots and status |
| [PRD](PRD.md) | Requirements, phases, gates and acceptance criteria |
| [Project state](PROJECT_STATE.md) | Current phase, evidence, blockers and next action |
| [Roadmap](ROADMAP.md) | Phase plan and remaining gates |
| [Architecture](ARCHITECTURE.md) | Modules, data flow and trust boundaries |
| [Compatibility](compatibility.md) | Toolchain, devices, limits and known gaps |
| [Performance](performance.md) | Measured startup, capture, jank and APK results |
| [FAQ](FAQ.md) | Adoption, privacy and release questions |
| [Governance](../GOVERNANCE.md) | Maintainer model, decisions and release approval |
| [Support](../SUPPORT.md) | Where to ask for help |
| [Community standards](../CODE_OF_CONDUCT.md) | Participation expectations |

## Integration

| Document | Purpose |
|---|---|
| [Evidence adapters](integration/evidence-adapters.md) | Breadcrumbs, logs, OkHttp and explicit semantics |
| [Desktop tools](integration/desktop-tools.md) | Validator, viewer and MCP recipes |
| [Privacy policy example](privacy-policy-example.md) | Host-facing disclosure starting point |
| [Troubleshooting](troubleshooting.md) | Capture, drafts, parcel and MCP failures |
| [API and schema freeze](api-freeze.md) | Artifacts, schema v1 and versioning |
| [Dependency licenses](dependency-licenses.md) | License review notes and constraints |
| [Dependency inventory](dependency-inventory.md) | Generated resolved coordinates |

## Security and privacy

| Document | Purpose |
|---|---|
| [Threat model](security/threat-model.md) | Assets, threats and required controls |
| [Capture contract](security/phase-2-capture-contract.md) | Production image-capture requirements |
| [Security policy](../SECURITY.md) | Private vulnerability reporting |
| [Public repository hygiene](../AGENTS.md) | Rules for safe public contributions |

## Verification

| Document | Purpose |
|---|---|
| [Final PRD audit](audits/final-audit.md) | Line-level PASS/BLOCKED disposition |
| [Phase audits](audits/) | Phase-specific audit records |
| [Evidence](evidence/) | Sanitized device, test and benchmark results |
| [Traceability ledger](traceability/prd-lines.jsonl) | One row per PRD line |
| [Handoffs](handoffs/) | Session-level implementation history |

## Design records

| Document | Purpose |
|---|---|
| [ADRs](adr/) | Architecture, capture, storage, export and relay decisions |
| [Phase decisions](decisions/) | Go/no-go and gate waivers |
| [Impact briefs](impact/) | Pre-change impact analysis |
| [Research](research/) | Toolchain, naming, competitive and adoption notes |
