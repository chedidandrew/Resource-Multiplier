# Build status

## Smart Resource Multiplier 1.21.9-1.21.10 compatibility candidates

This unpublished branch provides one unchanged Fabric JAR for Minecraft 1.21.9-1.21.10 and an exact NeoForge JAR for Minecraft 1.21.9. NeoForge 1.21.10 requires a separate artifact because the loader-facing block-removal hook changed between the two versions. Minecraft 26.2 remains the newest/default line on `main`.

- Fabric version: `1.3.2+mc1.21.9-1.21.10`
- Fabric baseline/range: Minecraft `1.21.9`, `>=1.21.9 <1.21.11`
- NeoForge version: `1.3.2+mc1.21.9`
- NeoForge baseline/range: Minecraft `[1.21.9]`, NeoForge `[21.9,21.10)`
- Publication latch: `release_ready=false`
- Java: `21`
- Mod/config identity: `smart_resource_drops`, `config/smart_resource_drops.json`, schema 3
- Commands: `/smartdrops`, `/smartdropsgui`

Fabric and NeoForge retain the same gameplay, configuration, command, GUI, permissions, network-policy, and safety-budget behavior. They remain separate JARs because loader entrypoints, networking, persistent chunk storage, and mixin integration are not binary-compatible.

## Verified evidence

- The unchanged Fabric candidate passed compilation, 164/164 unit tests, 65/65 runtime GameTests, GUI/authority smoke on both Minecraft versions with explicit value-centering and tooltip-deduplication checks, three-restart persistence, multiplayer/reconnect, and exact packaged-JAR server boots on both Minecraft 1.21.9 and 1.21.10.
- The exact NeoForge 1.21.9 candidate passed 170/170 unit tests, 64/64 runtime GameTests, GUI/category smoke with explicit value-centering and tooltip-deduplication checks, three-restart native persistence, Fabric-provenance migration with a native restart, multiplayer/reconnect, optional-channel matrices, oversized-wire rejection, and exact packaged-JAR server and physical-client probes.
- Both candidates passed metadata, Java 21 bytecode, loader-isolation, and artifact-contamination audits. Their frozen SHA-256 values are recorded in `compat/candidates/SHA256SUMS.txt`.
- NeoForge 1.21.10 is intentionally not claimed by this NeoForge JAR and will be built and verified as a separate exact candidate.

No candidate on this branch is authorized for publication yet. Manual player testing and the remaining NeoForge 1.21.10 lane must be completed before enabling the guarded release workflow.
