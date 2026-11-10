# ReproParcel

ReproParcel is a native Android Kotlin/Compose reporter that turns a user's bug
report into a portable, privacy-reviewed evidence parcel for humans and coding
agents.

**Status: Phase 0 local feasibility gate passed with explicit limitations; external validation and a stable release do not exist.**
The repository is under active implementation on `dev`. Do not assume that a
planned feature is shipped because it appears in the PRD.

## License warning

ReproParcel is licensed under **AGPL-3.0-only**. Linking or distributing this
SDK can impose copyleft obligations on a covered combined application. There is
no proprietary-linking exception. Read [LICENSE](LICENSE) and the
[PRD](docs/PRD.md) before adopting it. This is not legal advice.

## Current scope

- Local-first Android reporter: explicit capture, review, manual export.
- Encrypted app-private drafts and narrow FileProvider/SAF export.
- No mandatory backend, telemetry, ads, background recording or runtime LLM.
- Screenshot/semantic collection is off until the host supplies a policy.
- Explicit host-registered evidence is the baseline; automatic Compose
  extraction is not shipped.
- Read-only local tools and MCP are planned for Phase 4.

## Repository layout

```text
docs/            PRD, state, ADRs, handoffs, threat model, audits
schema/          versioned format and synthetic fixtures (Phase 1)
spike/           disposable Phase 0 feasibility lab
gradle/          version catalog and wrapper
desktop/         offline CLI/viewer/read-only MCP (Phase 4)
```

## Build (current foundation)

```sh
./gradlew :spike:capture-lab:assembleDebug
./gradlew :spike:capture-lab:connectedDebugAndroidTest
```

The Phase 0 spike is synthetic and disposable. It is not the SDK and must not be
published as one.

## Privacy

No real reports, screenshots, credentials or private application code belong in
this repository. See [AGENTS.md](AGENTS.md) and
[docs/security/threat-model.md](docs/security/threat-model.md).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Contributions are accepted under
AGPL-3.0-only with DCO sign-off. Security reports use the private channel in
[SECURITY.md](SECURITY.md), never a public issue with real evidence.
