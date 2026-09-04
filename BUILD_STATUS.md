# Build status

## Smart Resource Multiplier 1.21.2-1.21.3 compatibility candidates

This unpublished branch provides separate Fabric and NeoForge JARs whose unchanged bytes support Minecraft 1.21.2 and 1.21.3. Minecraft 26.2 remains the newest/default line on `main`.

- Version: `1.3.2+mc1.21.2-1.21.3`
- Fabric Minecraft range: `>=1.21.2 <1.21.4`
- NeoForge Minecraft range: `[1.21.2,1.21.4)`
- Publication latch: `release_ready=false`
- Java: `21`
- Mod/config identity: `smart_resource_drops`, `config/smart_resource_drops.json`, schema 3
- Commands: `/smartdrops`, `/smartdropsgui`

Fabric and NeoForge retain the same gameplay, configuration, command, GUI, permissions, network-policy, and safety-budget behavior. They remain separate JARs because loader entrypoints, networking, persistent chunk storage, and mixin integration are not binary-compatible.

## Verified evidence

- Both loader candidates compile and pass their complete unit suites and exactly 64 project GameTests on the 1.21.2 baseline.
- The same unchanged candidate bytes pass the target-native runtime and GUI gates on both Minecraft 1.21.2 and 1.21.3.
- Fabric exact-JAR dedicated-server boots pass on both versions; NeoForge exact packaged-JAR server and physical-client probes pass on both versions.
- Candidate hashes and compatibility ranges are recorded in `compat/candidates/SHA256SUMS.txt` and `compat/COMPATIBILITY_MATRIX.md`.
- Artifact audits confirm Java 21 bytecode, correct loader metadata, and no test, source, nested-JAR, Minecraft, or opposite-loader contamination.

No candidate on this branch is authorized for publication yet. The guarded release workflow must remain disabled until the complete compatibility rollout has been manually accepted.
