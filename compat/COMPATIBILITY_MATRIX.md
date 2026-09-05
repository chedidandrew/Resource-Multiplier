# Minecraft 1.21.4 release matrix

Fabric and NeoForge remain separate artifacts because their entrypoints, networking, placement storage, and loader hooks are not binary-compatible. Both use Java 21 and version `1.3.2+mc1.21.4`.

| Gate | Fabric 1.21.4 | NeoForge 1.21.4 |
| --- | --- | --- |
| Production compile and unit tests | Pass | Pass |
| Required gameplay GameTests | 64/64 | 64/64 |
| Automated client GUI/category smoke | Pass | Pass |
| Multiplayer authority and reconnect | Pass | Pass |
| Placement persistence across restarts | Pass | Pass |
| Optional client/server installation | N/A | Pass |
| Oversized network-payload rejection | Not separately probed | Pass |
| Exact packaged-JAR dedicated-server boot | Pass | Pass |
| Exact packaged-JAR physical client probe | Production-source smoke | Pass |
| Manual in-game acceptance | Pass | Pass |

Candidate hashes:

- Fabric: `8A470D9BE6EDD6D1E8DF966F5162A6052C950F660FAFFD9698EA12D0F59528A9`
- NeoForge: `7B792370FF42F066DB024A06B1A00408C55192216EE7059A8285B2341EC20032`

The distributable JARs are retained locally in `compat/candidates/` and intentionally ignored by Git. Release binaries belong on the GitHub release and CurseForge, not in source control.

## Version boundary

Minecraft 1.21.4 changed the structured-list scrolling API. This lane is intentionally limited to 1.21.4; Minecraft 1.21.5 uses its own tested release lane.
