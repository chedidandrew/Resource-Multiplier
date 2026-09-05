# Minecraft 1.21.5 release matrix

Fabric and NeoForge remain separate artifacts because their entrypoints, networking, placement storage, and loader hooks are not binary-compatible. Both use Java 21 and version `1.3.2+mc1.21.5`.

| Gate | Fabric 1.21.5 | NeoForge 1.21.5 |
| --- | --- | --- |
| Production compile and unit tests | Pass | Pass |
| Required gameplay GameTests | 64/64 | 64/64 |
| Automated client GUI smoke | Pass | Pass |
| Placement persistence across restarts | Pass | Pass |
| Exact packaged-JAR dedicated-server boot | Pass | Pass |
| Exact packaged-JAR physical client probe | Production-source smoke | Pass |
| Metadata and loader-isolation audit | Pass | Pass |
| Manual in-game acceptance | Pass | Pass |

Candidate hashes:

- Fabric: `81DA2403F572833740FF5112C24702B3DCDB2ED3580758CFF3BB88FC984E2853`
- NeoForge: `BBC51138D4C5B6303DE8822CAC36CA56F42B1F082B691761E842C130EF9BFE46`

The distributable JARs are retained locally in `compat/candidates/` and intentionally ignored by Git. Release binaries belong on the GitHub release and CurseForge, not in source control.

## Version boundary

This lane is intentionally limited to Minecraft 1.21.5. Later Minecraft versions use their own tested release lanes because loader and game APIs change across the series.
