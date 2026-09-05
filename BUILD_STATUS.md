# Build status

## Smart Resource Multiplier 1.21.9-1.21.10 compatibility candidates

This unpublished branch keeps the verified unchanged Fabric lane for Minecraft 1.21.9-1.21.10 and provides the separately compiled exact NeoForge JAR for Minecraft 1.21.10. NeoForge cannot share its 1.21.9 binary because the loader-facing block-removal hook changed between those versions. Minecraft 26.2 remains the newest/default line on `main`.

- Fabric version: `1.3.2+mc1.21.9-1.21.10`
- Fabric baseline/range: Minecraft `1.21.9`, `>=1.21.9 <1.21.11`
- NeoForge version: `1.3.2+mc1.21.10`
- NeoForge baseline/range: Minecraft `[1.21.10]`, NeoForge `[21.10,21.11)`
- Publication latch: `release_ready=false`
- Java: `21`
- Mod/config identity: `smart_resource_drops`, `config/smart_resource_drops.json`, schema 3
- Commands: `/smartdrops`, `/smartdropsgui`

Fabric and NeoForge retain the same gameplay, configuration, command, GUI, permissions, network-policy, and safety-budget behavior. They remain separate JARs because loader entrypoints, networking, persistent chunk storage, and mixin integration are not binary-compatible.

## Verified evidence

- The unchanged Fabric candidate passed compilation, 164/164 unit tests, 65/65 runtime GameTests, GUI/authority smoke, three-restart persistence, multiplayer/reconnect, and exact packaged-JAR server boots on both Minecraft 1.21.9 and 1.21.10.
- The exact NeoForge 1.21.10 candidate passed 170/170 unit tests, 64/64 runtime GameTests, GUI/category smoke with explicit value-centering and tooltip-deduplication checks, three-restart native persistence, Fabric-provenance migration with a native restart, multiplayer/reconnect, client-only and server-only installation matrices, oversized-wire rejection, and exact packaged-JAR server and physical-client probes.
- The NeoForge candidate passed metadata, Java 21 bytecode, loader-isolation, and artifact-contamination audits. Its frozen 965351-byte JAR has SHA-256 `AE51D8DFFC384A91521BECA059163BF3A94FAB7B233C7C62246D3D2C1E886FC2`, recorded in `compat/candidates/SHA256SUMS.txt`.
- The exact four-argument `ServerPlayerGameMode#removeBlock` injection was exercised at runtime with `require=1` and `expect=1`; no additional production API boundary appeared between the audited 1.21.9 and 1.21.10 NeoForge sources.

The local candidate directory in this dedicated worktree contains only the exact NeoForge 1.21.10 JAR. No candidate on this branch is authorized for publication yet; `release_ready=false` remains in force pending manual player testing and release assembly.
