# Build status

## Minecraft 1.20.1 dual-loader backport

Smart Resource Multiplier `1.3.1+mc1.20.1` is the maintained Minecraft Java Edition 1.20.1 backport for Fabric and legacy NeoForge/Forge 47. Minecraft 26.2 remains the newest/default line on `main`; this branch is intentionally released separately and must not replace GitHub's **Latest** release.

- Branch: `backport/1.20.1`
- Planned tag: `v1.3.1+mc1.20.1`
- Publication latch: `release_ready=true`
- Minecraft/runtime bytecode: Java `17` (Gradle/Loom launcher: Java `21`)
- Fabric Loader: `0.19.5`
- Fabric API: `0.92.12+1.20.1`
- Optional Mod Menu: `7.2.2`
- NeoForge artifact: `net.neoforged:forge:1.20.1-47.1.106`
- ModDevGradle legacy plugin: `2.0.146`
- Config identity: `config/smart_resource_drops.json`, schema 3
- Stable mod ID and namespace: `smart_resource_drops`

The shared gameplay, configuration, commands, GUI, bounded network protocol, and safety policies are compiled into both loader builds. Loader-specific code is limited to lifecycle, networking, persistent chunk data, platform events, and client integration. Install exactly one loader-specific JAR; Fabric also requires Fabric API.

## Current verification state

This branch contains the manually accepted 1.3.1 UI patch. The guarded release workflow reruns every clean, target-native gate before publication.

Completed during the backport:

- Java 17 compilation for Fabric and the NeoForge 47 legacy toolchain.
- Shared JUnit coverage, including strict fragmented-config wire decoding and transfer-admission bounds.
- Fabric dedicated-server GameTests and physical client/multiplayer harness implementation.
- NeoForge legacy metadata, SRG reobfuscation/refmap configuration, MixinExtras JarJar packaging, capability-based placed-block persistence, SimpleChannel networking, and loader-specific shearing hooks.
- A checksum-pinned official Forge 47.1.106 installer gate that boots the untouched NeoForge release JAR with Java 17 under the true `forgeserver` launch target, runs status/validation, persists the canonical config, and shuts down cleanly.
- Target-native Minecraft 1.20.1 tag/resource layouts and a Minecraft 1.20.1-authored wide GameTest structure.
- Guarded release workflow for exactly two loader-labelled JARs with `make_latest: false`.

The guarded release workflow performs these final publication checks on the exact tagged commit:

- Clean final Fabric unit, GameTest, physical GUI, physical multiplayer, packaging, and JAR-validation passes.
- Clean final NeoForge unit, GameTest, physical GUI/multiplayer, optional-installation, hostile-wire, production-server, packaging, and JAR-validation passes.
- Final hashes and sizes recorded from the two exact release JARs.

No cross-loader world-conversion guarantee is made for this backport. Fabric and NeoForge store placed-block provenance in different loader-owned chunk envelopes; players should keep a backup and use the same loader for an existing world unless a future release documents a target-native migration test.

## Planned artifacts

- Fabric: `build/libs/smart-resource-multiplier-1.3.1+mc1.20.1.jar`
- NeoForge: `neoforge/build/libs/smart-resource-multiplier-neoforge-1.3.1+mc1.20.1.jar`

The final release workflow publishes only those two JARs from tag `v1.3.1+mc1.20.1`; it does not merge this branch into `main` or mark the backport as the latest GitHub release.
