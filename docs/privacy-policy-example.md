# Privacy-policy integration example

This is an example, not legal advice. The host must review and adapt it.

## What ReproParcel collects

- User-written report title, description, expected/actual behavior and optional
  repro steps.
- Build metadata: package, version name/code, variant and optional commit.
- Device/app-window metadata: API level, manufacturer/model, window size,
  density, locale, font scale and orientation. No unique device identifier.
- Optional, host-policy-approved screenshot and explicitly registered semantic
  evidence.
- Optional host breadcrumbs, structured logs and network metadata (method,
  status, timing and safe route template only).

## What it never collects by default

- Screenshots or semantic text without a host capture policy.
- Passwords, editable values, clipboard, SMS, contacts, location, camera,
  microphone, advertising identifier or device serial.
- HTTP bodies, cookies, authorization headers, query strings or raw URLs.
- Global logcat, heap/database dumps or background screen recordings.

## User control

The user reviews the exact text and flattened image before export, can remove
optional evidence, and approves the artifact separately from choosing a
destination. No consent checkbox is preselected. Sharing cannot be recalled from
the recipient. Local drafts are encrypted and can be deleted by the user; a
previously shared file remains outside the SDK's control.

## Retention

Default drafts: 72 hours, at most 10 reports and 25 MiB encrypted. The host may
configure stricter values. Temporary share copies are best-effort cleaned after
24 hours. User-selected exports are outside the SDK retention scope.

## Warning

Screenshots and free text can contain sensitive data. The SDK does not guarantee
perfect detection or redaction. Application-specific testing and a legal/privacy
review are required before production enablement.
