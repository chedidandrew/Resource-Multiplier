# Minecraft 1.21.4 compatibility lane

This lane is experimental and has not been published. Fabric and NeoForge remain
separate artifacts because their entrypoints, networking, placement storage, and
loader hooks are not binary-compatible. Both artifacts use Java 21 and version
`1.3.2+mc1.21.4`, and both declare Minecraft 1.21.4 only.

## Verified runtime matrix

| Gate | Fabric 1.21.4 | NeoForge 1.21.4 |
| --- | --- | --- |
| Production compile and unit tests | Pass | Pass |
| Required gameplay GameTests | 64/64 | 64/64 |
| Automated client GUI/category smoke | Pass | Pass |
| Dedicated-server multiplayer authority and reconnect | Pass | Pass |
| Placement persistence across restart and removal | Pass | Pass |
| Optional client-only/server-only installation | N/A | Pass |
| Oversized network-payload rejection | Not separately probed | Pass |
| Exact packaged-JAR dedicated-server boot | Pass | Pass |
| Exact packaged-JAR physical client probe | N/A | Pass |

Fabric's exact-JAR harness loaded the unchanged distributable on a real 1.21.4
dedicated server and confirmed that its SHA-256 remained unchanged. NeoForge's
packaged probes loaded the unchanged distributable on both the dedicated server
and physical client. Production mixin audits ran as part of both 64-test gameplay
suites.

Candidate hashes:

- Fabric: `4B84930EAFF19A7100F3AB9C9718AAB2C7D5A2A83A34CF177EAF945706088129`
- NeoForge: `1442F542FD3A68FB86F292AF629CF9B86A27B426E7F45A0713B5F6CF00C9E1F4`

Retained evidence:

- `compat/candidates/SHA256SUMS.txt` records both candidate hashes.
- `compat/evidence/1.21.4-runtime-gates.json` records the final-candidate hashes,
  UTC timestamps, persistence markers, multiplayer/optional/wire pass markers,
  transient-log hashes, and byte-identical NeoForge packaged server/client copies.
- `compat/fabric-exact-smoke/runtime/fabric-1.21.4/20260904-130538-59c4e943/evidence/smoke-result.json`
  records the exact Fabric candidate's clean dedicated-server boot and unchanged
  before/after hash.
- `py -3 tools/validate_package.py` passed all package metadata, JSON, icon, and
  124-source checks.
- The loader-isolation validators accepted 301 Fabric entries and 304 NeoForge
  entries with no test classes, wrong-loader metadata, or non-Java-21 bytecode.

The JAR files are retained locally in `compat/candidates/` but intentionally
ignored by Git. Release binaries should be attached to a release, not committed
to source control. `release_ready=false` remains set until a later release review.

## Version boundary

Minecraft 1.21.4 changed the `AbstractSelectionList` scroll API used by the
structured configuration GUI. This lane replaces the earlier
`clampScrollAmount()`/`getScrollAmount()` calls with the 1.21.4
`refreshScrollAmount()`/`scrollAmount()` API. Its metadata is intentionally not
widened to adjacent Minecraft versions; each additional version must pass this
same matrix with identical artifact bytes before it can be advertised.
