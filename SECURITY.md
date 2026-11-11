# Security policy

## Reporting a vulnerability

Use GitHub's **private vulnerability reporting / Security Advisories** for
`awbuilds/reproparcel`:

<https://github.com/awbuilds/reproparcel/security/advisories/new>

Do not open a public issue containing a real report, screenshot, credential,
customer data or exploitable detail.

If the private channel is not available, stop and ask the maintainer to enable
it before sharing details. Do not send sensitive evidence through a public
issue, discussion or third-party upload service.

## Supported versions

There is no stable release. Security fixes target the current `dev` source
candidate. No backport or response-time SLA is promised.

## Scope

In scope:

- privacy failures, secret leakage, unsafe capture or masking;
- parcel parser/viewer/MCP path traversal, decompression bombs or code
  execution;
- lifecycle, cancellation or exception paths that can crash the host;
- insecure storage, FileProvider/SAF grants or backup exposure;
- dependency or supply-chain issues in shipped artifacts;
- read-only MCP tools that expose arbitrary files, writes or execution.

Out of scope:

- vulnerabilities in a host application's own code or policy;
- reports that require a malicious in-process host to bypass its own policy;
- denial of service against maintainer infrastructure;
- issues in a host application or a forked build.

## What to include

Provide a synthetic/minimal reproduction, affected commit or artifact, impact,
and the smallest evidence needed to reproduce. Do not include real user reports,
screenshots, credentials or personal information. We may ask for a private
artifact only after agreeing on a safe channel.

Useful details:

- exact command, build variant, API/device class;
- typed outcome or omission reason;
- whether the issue reproduces with the release/R8 build;
- whether report content, image pixels or metadata are exposed.

## Disclosure

Coordinate disclosure with the maintainer. No bounty is offered. Confirmed
evidence leaks or SDK-induced host crashes can require disabling the affected
feature and a patch before any public detail is published.
