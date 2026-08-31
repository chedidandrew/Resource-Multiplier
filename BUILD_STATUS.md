# Build status

## Smart Resource Multiplier 1.2.2 source candidate

Smart Resource Multiplier `1.2.2` is the current branding-only source candidate for Minecraft Java Edition 26.2 and Fabric. It changes the public name, visible copy, build artifact name, deterministic package prefix, and maintained documentation without changing gameplay or compatibility identifiers.

- Version: `1.2.2`
- Publication latch: `release_ready=false`
- Candidate JAR: `smart-resource-multiplier-1.2.2.jar`
- Latest published JAR: `resource-multiplier-1.2.1.jar`
- Website: `https://www.curseforge.com/minecraft/mc-mods/resource-multiplier`
- Issues: `https://github.com/chedidandrew/Resource-Multiplier/issues`
- Sources: `https://github.com/chedidandrew/Resource-Multiplier`
- Mod ID and datapack namespace: `smart_resource_drops`
- Config path: `config/smart_resource_drops.json`
- Commands: `/smartdrops` and `/smartdropsgui`
- Icon: unchanged pending separate visual review

## Compatibility decision

The public name and artifact filename change, but the stable Fabric mod ID, configuration schema and path, commands, datapack and network namespace, saved placement provenance, and Java package do not. Existing worlds and settings require no migration. Players must remove the old JAR before installing the renamed JAR to avoid duplicate copies of the same mod ID.

## Verification state

- The pre-rebrand branch baseline passed GitHub Actions run `33429519848` for commit `1453879c2b52b591cd2a98cd502d6b437a839105`.
- The final `1.2.2` rebrand commit must pass package validation, deterministic packaging tests, Mod Menu and copy regressions, core assertions, mapped JUnit tests, all dedicated-server GameTests, client GUI and authority GameTests, and the Java 25 Fabric build.
- Final clean-checkout run, candidate JAR size, ZIP-entry count, and SHA-256 will be recorded after the completed rebrand commit passes CI.

## Published release history

Smart Resource Multiplier `1.2.2` is not yet a tagged release. The latest published release remains `1.2.1`, whose verified evidence is preserved in [`docs/releases/1.2.1.md`](docs/releases/1.2.1.md) and the existing GitHub Release. Historical `1.2.0` evidence remains under `docs/archive/`.
