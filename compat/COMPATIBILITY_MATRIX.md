# Minecraft 1.21.2–1.21.3 release matrix

Fabric and NeoForge remain separate artifacts because their entrypoints, networking, placement storage, and loader hooks are not binary-compatible. Both use Java 21 and version `1.3.2+mc1.21.2-1.21.3`.

| Gate | Fabric 1.21.2 | Fabric 1.21.3 | NeoForge 1.21.2 | NeoForge 1.21.3 |
| --- | --- | --- | --- | --- |
| Production compile and unit tests | Pass | Pass | Pass | Pass |
| Required gameplay GameTests | 64/64 | 64/64 | 64/64 | 64/64 |
| Automated client GUI smoke | Pass | Pass | Pass | Pass |
| Exact packaged-JAR server boot | Pass | Pass | Pass | Pass |
| Exact packaged-JAR client probe | Production-source smoke | Production-source smoke | Pass | Pass |
| Manual in-game acceptance | Pass | Pass | Pass | Pass |

Candidate hashes:

- Fabric: `3D69559273E30DAFF9B65FD1335EF0F499A52125743CC103DA46CC23AE2AC3CA`
- NeoForge: `468D86611B61FA1DE310A80A594C0B700F941E0FB49531522E68FCF5E15E6EA1`

The distributable JARs are retained locally in `compat/candidates/` and intentionally ignored by Git. Release binaries belong on the GitHub release and CurseForge, not in source control.

## Version boundary

Minecraft 1.21.4 changes the structured-list scrolling API, so this release intentionally stops at 1.21.3. Later versions use their own tested release lanes.
