# Build status

## Smart Resource Multiplier 1.3.0+mc1.21.11 dual-loader backport

Smart Resource Multiplier `1.3.0+mc1.21.11` is the maintained Minecraft Java Edition 1.21.11 backport for both Fabric and NeoForge. Both builds use Java 21, share the same gameplay, configuration, commands, and GUI implementation, and keep small loader-specific adapters for lifecycle, networking, and placed-block provenance. Minecraft 26.2 remains the newest/default release on `main` and GitHub tag `v1.3.0` remains **Latest**.

- Version: `1.3.0+mc1.21.11`
- Publication latch: `release_ready=true`
- Release branch: `backport/1.21.11`
- Release tag: `v1.3.0+mc1.21.11`
- Fabric: Loader `0.19.5`, Fabric API `0.141.6+1.21.11`, optional Mod Menu `17.0.0`
- NeoForge: `21.11.45`, ModDevGradle `2.0.146`
- Java: `21`
- Fabric JAR: `smart-resource-multiplier-1.3.0+mc1.21.11.jar`
- NeoForge JAR: `smart-resource-multiplier-neoforge-1.3.0+mc1.21.11.jar`
- Website: `https://www.curseforge.com/minecraft/mc-mods/resource-multiplier`
- Issues: `https://github.com/chedidandrew/Resource-Multiplier/issues`
- Sources: `https://github.com/chedidandrew/Resource-Multiplier`
- Mod ID and datapack/network namespace: `smart_resource_drops`
- Config path and schema: `config/smart_resource_drops.json`, schema 3
- Commands: `/smartdrops` and `/smartdropsgui`
- Production icon: approved **SMART RESOURCE MULTIPLIER** diamond-mining artwork, `512x512` PNG, SHA-256 `db216ccd6058404de18f797ebb5be87a313899a27c3f1971fdf086b8637dc190`

## Compatibility decision

The Fabric and NeoForge builds intentionally preserve the same configuration fields, defaults, commands, GUI, block/entity/shearing multipliers, output budgets, permissions, and anti-duplication behavior. The General screen labels its independent controls **Multiply Block XP** and **Block XP Multiplier** on both loaders; Mob XP remains under **Entity Drops**.

Install exactly one loader-specific JAR. Fabric also requires Fabric API. Fabric-to-NeoForge placed-block provenance migration is supported and tested for the documented 1.21.11 path, but it is one-way because the loaders store that chunk data differently; back up a world before changing loaders and do not repeatedly move it between loaders.

## Verification state

- Package metadata, deterministic source packaging, Mod Menu integration, structured tooltips, edge cases, polish regressions, workflow policy, and 90 framework-independent core assertions pass.
- Fabric passes all 158 JUnit tests, all 65 required dedicated-server GameTests, the real client GUI/authority GameTest, and a clean Java 21 Loom build.
- NeoForge passes all 164 JUnit tests, all 64 required dedicated-server GameTests, a clean Java 21 ModDevGradle build, and the loader-isolation/JAR validator.
- A real NeoForge client confirms all nine Entity Categories rows and tag-based classifications.
- A separate NeoForge client/server pair confirms `/smartdropsgui`, non-operator read-only access, operator promotion, entity overrides and filters, shearing settings, root Apply, confirmed Reset, near-limit payloads, disconnect cleanup, six-channel reconnect, and fresh server-authoritative state.
- Physical client-only and server-only installation tests both connect and disconnect cleanly while unavailable configuration channels remain unavailable.
- A real 1,048,577-character configuration payload is rejected at the 1,048,576-character decoder limit; only the offending client disconnects, configuration and revision stay unchanged, and the server remains responsive.
- A hash-locked Fabric-authored Minecraft 1.21.11 chunk imports into native NeoForge placement provenance, saves through a real server, restarts in a second JVM, and remains visible to gameplay lookup.

## Verified artifacts

- Fabric JAR: 964,571 bytes, 330 ZIP entries (298 files), SHA-256 `DBF680EBF65BE9EDF97236339550F2A45821AFE5FA58ECAB24AA15AC88A517D4`.
- NeoForge JAR: 961,150 bytes, 336 ZIP entries (304 files), SHA-256 `45431B7DCD303C610CDC4E6BE35D6D76947FBA844ADC1545B84D52D0FB0C3887`.
- Inspection confirms public name `Smart Resource Multiplier`, version `1.3.0+mc1.21.11`, Java 21 bytecode, exact Minecraft 1.21.11 loader metadata, the approved icon bytes, the embedded MIT license, no nested dependencies, no test/probe fixtures, and no cross-loader implementation leakage.

## Distribution

The backport remains on `backport/1.21.11` and is published from tag `v1.3.0+mc1.21.11` as a stable but deliberately non-latest GitHub release. The guarded release workflow uses `make_latest: false`; it does not merge into or move `main`. CurseForge receives the Fabric and NeoForge JARs as two separate files with the matching loader selected.

The Minecraft 26.2 `1.3.0` source, release, and evidence remain on `main` and in [`docs/releases/1.3.0.md`](docs/releases/1.3.0.md). Earlier records remain under `docs/releases/`, `docs/verification/`, and `docs/archive/`.
