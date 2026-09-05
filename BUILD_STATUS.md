# Build status

## Smart Resource Multiplier 1.21.9-1.21.10 combined release

This exact child branch joins the verified shared Fabric lane for Minecraft 1.21.9-1.21.10, exact NeoForge 1.21.9 source, and separately compiled exact NeoForge 1.21.10 source. NeoForge needs separate JARs because the loader-facing block-removal hook changed between those versions. Minecraft 26.2 remains the newest/default line on `main`.

- Fabric version: `1.3.2+mc1.21.9-1.21.10`
- Fabric baseline/range: Minecraft `1.21.9`, `>=1.21.9 <1.21.11`
- Parent NeoForge version: `1.3.2+mc1.21.9`, exact Minecraft `[1.21.9]`
- Child NeoForge version: `1.3.2+mc1.21.10`, exact Minecraft `[1.21.10]`
- Publication latch: `release_ready=true` on this final exact-child authorization commit
- Java: `21`
- Mod/config identity: `smart_resource_drops`, `config/smart_resource_drops.json`, schema 3
- Commands: `/smartdrops`, `/smartdropsgui`

All three artifacts retain the same gameplay, configuration, command, GUI, permissions, network-policy, and safety-budget behavior. Fabric and NeoForge remain separate JARs because loader entrypoints, networking, persistent chunk storage, and mixin integration are not binary-compatible.

## Verified evidence

- The shared Fabric candidate passed compilation, 164/164 unit tests, 65/65 runtime GameTests, GUI/authority smoke on both Minecraft versions with explicit value-centering and tooltip-deduplication checks, three-restart persistence, multiplayer/reconnect, and exact packaged-JAR server boots on both Minecraft 1.21.9 and 1.21.10.
- The exact NeoForge 1.21.9 candidate passed 170/170 unit tests, 64/64 runtime GameTests, GUI/category smoke with explicit value-centering and tooltip-deduplication checks, three-restart native persistence, Fabric-provenance migration with a native restart, multiplayer/reconnect, optional-channel matrices, oversized-wire rejection, and exact packaged-JAR server and physical-client probes.
- The exact NeoForge 1.21.10 candidate passed the matching 170/170 unit tests, 64/64 GameTests, GUI/category, persistence, migration, multiplayer, optional-channel, oversized-wire, packaged-server, and packaged-client gates.
- All candidates passed metadata, Java 21 bytecode, loader-isolation, and artifact-contamination audits. Their frozen local hashes are recorded in `compat/candidates/SHA256SUMS.txt`.
- Hands-on client acceptance passed on Fabric 1.21.9 and 1.21.10 and NeoForge 1.21.9 and 1.21.10.

The guarded `v1.3.2+mc1.21.9-1.21.10` workflow must run from the canonical `backport/1.21.10-neoforge` tip and requires the canonical `backport/1.21.9-1.21.10` tip as an ancestor. It rebuilds and publishes exactly the shared Fabric JAR and both exact NeoForge JARs with `make_latest: false`.
