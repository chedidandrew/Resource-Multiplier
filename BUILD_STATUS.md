# Build status

## Smart Resource Multiplier 1.3.0+mc1.21.1 dual-loader backport

This branch targets Minecraft Java Edition 1.21.1 on Fabric and NeoForge. Minecraft 26.2 remains the newest/default line on `main`; the eventual `v1.3.0+mc1.21.1` release is deliberately non-latest.

- Version: `1.3.0+mc1.21.1`
- Publication latch: `release_ready=false`
- Branch/tag: `backport/1.21.1`, `v1.3.0+mc1.21.1`
- Fabric: Loader `0.19.5`, Fabric API `0.116.17+1.21.1`, optional Mod Menu `11.0.4`
- NeoForge: `21.1.249`, ModDevGradle `2.0.146`, FML `4`
- Java: `21`
- Mod/config identity: `smart_resource_drops`, `config/smart_resource_drops.json`, schema 3
- Commands: `/smartdrops`, `/smartdropsgui`

Both loader builds share gameplay, configuration, commands, GUI, network payload policy, permissions, and safety budgets. Loader-specific adapters handle lifecycle, networking, fake-player detection, and placed-block storage. The General screen uses the unambiguous **Multiply Block XP** and **Block XP Multiplier** labels; Mob XP remains under Entity Drops.

Minecraft 1.21.1 has no generic final-output shearing hook. This target therefore multiplies vanilla Sheep shearing only. Other special vanilla shearing rewards remain 1x, and arbitrary datapack-tagged shearables fail closed. Configuration identity remains compatible, but Minecraft world downgrades and cross-loader placed-block-data migration are not claimed; back up worlds before changing versions or loaders.

## Current evidence

- Package metadata/policy validation, Mod Menu integration, structured-tooltip checks, edge-case checks, polish regressions, the 90-assertion core runner, mapped compilation, and all 163 Java 21 unit tests pass locally.
- Fabric discovers, executes, and passes exactly 64 dedicated-server GameTests, including the real Fabric fake-player denial, exact 1.21.1 death/XP hooks, Sheep-only shearing boundaries, and the retry-hardened atomic configuration writer under OneDrive. Its native placement attachment also passes a three-fresh-JVM mark/save/restart/remove/save/restart persistence chain.
- NeoForge discovers, executes, and passes exactly 64 target-native GameTests. Its native placement attachment passes the same three-fresh-JVM persistence proof, and its real multiplayer authority/revision/reconnect test, both optional-channel installation matrices, and hostile oversized-wire rejection all pass.
- A clean NeoForge installation loads the production JAR by itself on a dedicated server and physical client. The packaged client opens production configuration/category UI from the candidate JAR and exits with a clean success marker.
- Fabric's target-native physical-client smoke opens the production GUI, verifies non-empty entity categories and child navigation, exercises edit/Back/dirty/root-Apply behavior, and checks the local/operator/non-operator authority models. Its separate-process client/server gate passes non-operator denial, operator Apply/Reset revision updates, disconnect cleanup, channel renegotiation, and reconnect.
- Both rebuilt playable candidates pass final-JAR validation: Fabric `ae03cc7e3d4c58bb464f7c1627cddb86cb03c5450d0da3be8924ae4515dfdc7a` (968,074 bytes) and NeoForge `5da95b341c7a1fdc6cd40a1e5cdd3d60ea9932c5f5d3b974f369cad4d668f4e5` (960,601 bytes). Release bundles remain intentionally locked until the final source commit is reviewed and `release_ready` is set true.
- A disposable tracked-source dry run created and validated the deterministic dual-loader source ZIP, both real playable JARs, checksums, package README/status record, and release bundle. Publication remains intentionally locked until the final source commit is reviewed and `release_ready` is set true.

Keep `release_ready=false` until every item in [`docs/PUBLIC_RELEASE_CHECKLIST.md`](docs/PUBLIC_RELEASE_CHECKLIST.md) passes on one final source commit. Do not publish artifacts from this intermediate state.
