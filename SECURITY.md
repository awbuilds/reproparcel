# Security policy

## Reporting a vulnerability

Use GitHub's private vulnerability reporting / Security Advisories for
`awbuilds/reproparcel`. Do not open a public issue containing a real report,
screenshot, credential, customer data or exploitable detail.

If the private channel is not available, stop and ask the maintainer to enable
it before sharing details. Do not send sensitive evidence through a public
issue or a third-party upload service.

## Scope

In scope:

- privacy failures, secret leakage, unsafe capture or masking
- parcel parser/viewer/MCP path traversal, decompression bombs or code execution
- lifecycle, cancellation or exception paths that can crash the host
- insecure storage, FileProvider/SAF grants or backup exposure
- dependency or supply-chain issues in shipped artifacts

Out of scope:

- vulnerabilities in a host application's own code or policy
- reports that require a malicious in-process host to bypass its own policy
- denial of service against the maintainer's infrastructure
- the private application referenced only in planning; it is not part of this
  repository

## What to include

Provide a synthetic/minimal reproduction, affected version, impact, and the
smallest evidence needed to reproduce. Do not include real user reports,
screenshots, credentials or personal information. We may ask for a private
artifact only after agreeing on a safe channel.

## Disclosure

Coordinate disclosure with the maintainer. No bounty or response-time SLA is
promised. Confirmed evidence leaks or SDK-induced host crashes can require
disabling the affected feature and a patch.
