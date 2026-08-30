# Resource Multiplier security policy

## Supported version

Security fixes are considered for the latest public release line. Unpublished source snapshots and superseded release lines do not carry a security-support commitment.

## Reporting

This source snapshot does not currently identify a verified project-owned private reporting channel. Do not post duplication-exploit steps, permission-bypass details, malformed-packet payloads, denial-of-service instructions, secrets, or private server/player data in a public issue or pull request. The project must not claim private vulnerability intake until a real maintainer-controlled route is configured and recorded here.

If the canonical repository visibly enables a private vulnerability-reporting facility in the future, use that facility and verify that this policy names it before sending sensitive details. Until then, retain the sensitive details; an ordinary public bug report may state only that a security-sensitive issue exists and provide non-sensitive version/context information. Include full Minecraft, Fabric Loader, Fabric API, and mod versions with any eventual private report.

Ordinary non-security bugs and exact mod-compatibility reports should use the repository issue forms. Redact player UUIDs, account identifiers, server addresses, access tokens, personal filesystem paths, and unrelated log content.

## Security model

- Gameplay configuration is server authoritative.
- Mutation commands and mutation payloads must verify operator permission server-side.
- Client values are never trusted without range and identifier validation.
- Placement provenance is world data and must not be accepted from the client.
- Queues, caches and packet payloads must remain bounded.
- Block entities and data-bearing containers remain protected unless a server explicitly opts in.
