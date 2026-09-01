# Build status

## Smart Resource Multiplier 1.2.3 stable icon release

Smart Resource Multiplier `1.2.3` is the stable icon-and-toolchain patch for Minecraft Java Edition 26.2 and Fabric. It packages the approved high-resolution full-name artwork and makes the Java 25 compiler requirement reproducible from an older supported Gradle JVM without changing gameplay or compatibility identifiers.

- Version: `1.2.3`
- Publication latch: `release_ready=true`
- Published JAR: `smart-resource-multiplier-1.2.3.jar`
- Website: `https://www.curseforge.com/minecraft/mc-mods/resource-multiplier`
- Issues: `https://github.com/chedidandrew/Resource-Multiplier/issues`
- Sources: `https://github.com/chedidandrew/Resource-Multiplier`
- Mod ID and datapack namespace: `smart_resource_drops`
- Config path: `config/smart_resource_drops.json`
- Commands: `/smartdrops` and `/smartdropsgui`
- Published `v1.2.3` icon: approved **SMART RESOURCE MULTIPLIER** diamond-mining artwork, `512x512` PNG, SHA-256 `db216ccd6058404de18f797ebb5be87a313899a27c3f1971fdf086b8637dc190`
- Published `v1.2.2` JAR icon: historical pre-refresh artwork, SHA-256 `b8a56ed24db3a2e812271d69fd021a5756469ac0d649ebd7cc3f205d7d276694`

## Compatibility decision

The icon, patch version, and build-toolchain selection change, but the stable Fabric mod ID, configuration schema and path, commands, datapack and network namespace, saved placement provenance, Java package, and gameplay logic do not. Existing worlds and settings require no migration. Players must remove the old JAR before installing the new JAR to avoid duplicate copies of the same mod ID.

## Verification state

- The exact `512x512` production icon commit `a8fe39a9d591d314ee4467ad8dae97edf84ac252` passed package validation, deterministic packaging tests, Mod Menu and copy regressions, core assertions, mapped JUnit tests, all dedicated-server GameTests, client GUI and authority GameTests, and the Java 25 Fabric build in GitHub Actions run `33466903620`.
- A local wrapper build launched from Temurin Java 21 automatically selected the installed Oracle JDK 25 toolchain, compiled successfully, passed mapped JUnit tests and all 66 required dedicated-server GameTests, and completed `clean build` without the prior `release version 25 not supported` error. The configured resolver provides a download fallback when no matching JDK is installed.
- The verified local release JAR is `smart-resource-multiplier-1.2.3.jar`, 942,420 bytes, 311 ZIP entries, SHA-256 `2aa2506aa9125947954a3fb39a980a71c24226db3dbfaa77ab0b8adca947edf9`.
- JAR inspection confirmed public name `Smart Resource Multiplier`, version `1.2.3`, stable mod ID `smart_resource_drops`, no nested JAR or GameTest/testmod/fixture entries, embedded `LICENSE_smart-resource-multiplier`, and the approved icon SHA-256 `db216ccd6058404de18f797ebb5be87a313899a27c3f1971fdf086b8637dc190`.
- Package validation locks the icon at `512x512` with SHA-256 `db216ccd6058404de18f797ebb5be87a313899a27c3f1971fdf086b8637dc190`; Fabric metadata retains `assets/smart_resource_drops/icon.png`.
- The guarded publication workflow rebuilds from the exact `v1.2.3` release commit, repeats the complete validation chain, and publishes the final JAR, source archive, release bundle, build-status record, and checksums together.

## Published release history

Smart Resource Multiplier `1.2.3` supersedes `1.2.2`. The `1.2.2` release and verification evidence remain preserved in [`docs/releases/1.2.2.md`](docs/releases/1.2.2.md) and [`docs/verification/1.2.2-release-publication.md`](docs/verification/1.2.2-release-publication.md); earlier evidence remains under `docs/releases/` and `docs/archive/`.
