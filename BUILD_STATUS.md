# Build status

## Smart Resource Multiplier 1.2.2 stable release

Smart Resource Multiplier `1.2.2` is the stable branding release for Minecraft Java Edition 26.2 and Fabric. It changes the public name, visible copy, build artifact name, deterministic package prefix, and maintained documentation without changing gameplay or compatibility identifiers.

- Version: `1.2.2`
- Publication latch: `release_ready=true`
- Published JAR: `smart-resource-multiplier-1.2.2.jar`
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

- The exact rebrand source tree `6f4e528a0e0e7f1b034ae566f7099930ec3306b6` was committed as `79d6be129d99ce15053f69cf6f242c0350e187b2`.
- Clean-checkout GitHub Actions run `33439853561` passed package validation, deterministic packaging tests, Mod Menu and copy regressions, core assertions, mapped JUnit tests, all dedicated-server GameTests, client GUI and authority GameTests, and the Java 25 Fabric build.
- Documentation-evidence commit `749f8b2ad76d88c5b60ec17d4187558d978c2c38` also passed the same clean-checkout build chain in run `33440180986` before stable promotion.
- The verified pre-release JAR was `smart-resource-multiplier-1.2.2.jar`, 602,015 bytes, 311 ZIP entries, SHA-256 `08fe1fb05684b336d71a0f3467a358594148556295624a2a3b42d40325a905fe`.
- JAR inspection confirmed public name `Smart Resource Multiplier`, version `1.2.2`, stable mod ID `smart_resource_drops`, no nested JARs, no GameTest/testmod/fixture entries, embedded `LICENSE_smart-resource-multiplier`, and the unchanged production icon SHA-256 `b8a56ed24db3a2e812271d69fd021a5756469ac0d649ebd7cc3f205d7d276694`.
- The publication workflow rebuilds from the release commit, repeats the complete validation chain, and publishes the final JAR, source archive, release bundle, build-status record, and checksums together.

## Published release history

Smart Resource Multiplier `1.2.2` supersedes Resource Multiplier `1.2.1`. The `1.2.1` evidence remains preserved in [`docs/releases/1.2.1.md`](docs/releases/1.2.1.md), and historical `1.2.0` evidence remains under `docs/archive/`.
