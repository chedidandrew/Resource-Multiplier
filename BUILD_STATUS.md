# Build status

## Smart Resource Multiplier 1.3.0 dual-loader release

Smart Resource Multiplier `1.3.0` is the stable Minecraft Java Edition 26.2 release for both Fabric and NeoForge. Both builds use Java 25, share the same gameplay/configuration/GUI implementation, and keep small loader-specific adapters for lifecycle, networking, commands, and placed-block provenance.

- Version: `1.3.0`
- Publication latch: `release_ready=true`
- Fabric: Loader `0.19.3`, Fabric API `0.158.0+26.2`
- NeoForge: `26.2.0.72`
- Fabric JAR: `smart-resource-multiplier-1.3.0.jar`
- NeoForge JAR: `smart-resource-multiplier-neoforge-1.3.0.jar`
- Website: `https://www.curseforge.com/minecraft/mc-mods/resource-multiplier`
- Issues: `https://github.com/chedidandrew/Resource-Multiplier/issues`
- Sources: `https://github.com/chedidandrew/Resource-Multiplier`
- Mod ID and datapack/network namespace: `smart_resource_drops`
- Config path and schema: `config/smart_resource_drops.json`, schema 3
- Commands: `/smartdrops` and `/smartdropsgui`
- Production icon: approved **SMART RESOURCE MULTIPLIER** diamond-mining artwork, `512x512` PNG, SHA-256 `db216ccd6058404de18f797ebb5be87a313899a27c3f1971fdf086b8637dc190`

## Compatibility decision

The Fabric and NeoForge builds intentionally preserve the same configuration fields, defaults, commands, GUI, block/entity/shearing multipliers, output budgets, permissions, and anti-duplication behavior. The General screen now labels its independent controls **Multiply Block XP** and **Block XP Multiplier** on both loaders; Mob XP remains under **Entity Drops**.

Updating an existing Fabric installation from `1.2.3` does not require resetting its configuration. Install exactly one loader-specific JAR. Fabric-to-NeoForge placed-block provenance migration is supported and tested, but it is one-way because the loaders store that chunk data differently; back up a world before changing loaders and do not repeatedly move it between loaders.

## Verification state

- Package metadata, deterministic source packaging, Mod Menu integration, structured tooltips, edge cases, polish regressions, workflow syntax, and 90 framework-independent core assertions pass.
- Fabric passes all 158 JUnit tests, all 66 required dedicated-server GameTests, the real client GUI/authority GameTest, and a clean Java 25 Loom build.
- NeoForge passes all 164 JUnit tests, all 65 required dedicated-server GameTests, a clean Java 25 ModDevGradle build, and the loader-isolation/JAR validator.
- A real NeoForge client confirms all nine Entity Categories rows and tag-based classifications.
- A separate NeoForge client/server pair confirms `/smartdropsgui`, non-operator read-only access, operator promotion, entity overrides and filters, shearing settings, root Apply, confirmed Reset, near-limit payloads, disconnect cleanup, six-channel reconnect, and fresh server-authoritative state.
- Physical client-only and server-only installation tests both connect and disconnect cleanly while unavailable configuration channels remain unavailable.
- A real 1,048,577-character configuration payload is rejected at the 1,048,576-character decoder limit; only the offending client disconnects, configuration and revision stay unchanged, and the server remains responsive.
- A captured Fabric-authored chunk imports into native NeoForge placement provenance, saves through a real server, restarts in a second JVM, and remains visible to gameplay lookup.

## Verified artifacts

- Fabric JAR: 959,473 bytes, 330 ZIP entries (298 files), SHA-256 `387fc2860d341a1bf9a2d49b27b74da1a1d40c6ac7663bf832e7d7ee52980691`.
- NeoForge JAR: 961,482 bytes, 336 ZIP entries (304 files), SHA-256 `75982f4c16a245f13f9e9536cbe8a9a9f00860cdac16fc70670bd1560ffbcfb8`.
- Inspection confirms public name `Smart Resource Multiplier`, version `1.3.0`, Java 25 bytecode, the approved icon bytes, the embedded MIT license, no nested dependencies, no test/probe fixtures, and no cross-loader implementation leakage.

## Distribution

CurseForge receives the Fabric and NeoForge JARs as two separate files with the matching loader selected. The stable source and verification records are promoted to GitHub `main`; no GitHub tag or GitHub Release is required for this CurseForge distribution.

The previous Fabric-only `1.2.3` evidence remains preserved in [`docs/releases/1.2.3.md`](docs/releases/1.2.3.md) and the earlier records under `docs/releases/`, `docs/verification/`, and `docs/archive/`.
