# NeoForge 1.20.1 backport

## Status

The NeoForge target is a legacy Forge-47-compatible backport of Smart Resource Multiplier `1.3.0` for Minecraft 1.20.1. It shares gameplay, configuration, commands, GUI, rule resolution, and network policy with Fabric while adapting loader-owned lifecycle and persistence APIs. Minecraft 26.2 remains the newest/default release on `main`.

This branch is still a release candidate. Keep `release_ready=false` until all final target-native gates in [TESTING.md](TESTING.md) pass on the exact commit.

## Exact baseline

- Minecraft `1.20.1`
- Java `17`
- NeoForge artifact `net.neoforged:forge:1.20.1-47.1.106`
- ModDevGradle legacy plugin `2.0.146`
- Legacy `META-INF/mods.toml` metadata with Forge loader range `[47,)`
- Mixin `0.8.5` annotation processing, SRG refmap/reobfuscation, and manifest-listed mixin configs
- MixinExtras Forge `0.5.4` bundled as the only permitted JarJar dependency

## Shared compatibility contract

Both loader builds preserve:

- `smart_resource_drops` mod ID, datapack namespace, and network namespace
- `config/smart_resource_drops.json`, schema 3
- `smart_resource_drops:placed_blocks` logical provenance identity
- `/smartdrops` and `/smartdropsgui`
- block, entity-death, mob-XP, and supported shearing behavior
- multiplier precedence, filters, source modes, permissions, output budgets, and atomic configuration updates
- the same bounded fragmented-config protocol and strict malformed-input behavior

The loaders store placed-block data in different loader-owned chunk envelopes. This backport does not claim a tested cross-loader world conversion. Back up worlds and keep an existing world on the same loader unless a later release documents a target-native migration path.

## Loader adapter map

- Bootstrap and events use `net.minecraftforge.*`, `MinecraftForge.EVENT_BUS`, and the mod event bus supplied by `FMLJavaModLoadingContext`.
- Commands, server/client ticks, config-screen registration, and installed-mod discovery use Forge 47 APIs.
- Networking uses one optional `SimpleChannel` protocol with strict play directions and bounded `FriendlyByteBuf` fragment envelopes. Missing channels are accepted so client-only/server-only pairs can connect, while config actions fail closed.
- Placed-block provenance uses a persistent `LevelChunk` capability with `CompoundTag` serialization and marks chunks unsaved after mutation.
- Player shearing adapts Forge's `IForgeShearable` call; vanilla dispenser/sheep paths use target-specific mixins and the shared all-or-nothing output budget.
- Client classes remain on the physical client, and NeoForge exposes the shared GUI with the legacy config-screen extension point.

## Test and packaging contract

The release candidate must pass:

- shared plus NeoForge-specific JUnit tests on Java 17;
- Forge 47 `RegisterGameTestsEvent` discovery using the 32x8x32 Minecraft-1.20.1-authored test structure;
- physical GUI, multiplayer authority, optional-installation, and hostile fragmented-wire runs;
- a fresh Java 17 production `forgeserver` launch from the reobfuscated final JAR using the checksum-pinned official Forge installer;
- a strict JAR audit for `mods.toml`, Java-17 class major 61, both mixin configs, refmap, manifest `MixinConfigs`, approved icon/license, exactly one pinned MixinExtras JarJar, and no Fabric/test/dev leakage.

Build from the repository root:

```powershell
.\gradlew.bat -p neoforge --no-daemon clean test build runGameTestServer
py -3 tools/validate_neoforge_jar.py
```

Linux CI additionally proves the untouched final JAR in a real production namespace:

```bash
bash tools/run_neoforge_production_server_smoke.sh
```

The production-server gate installs Forge `1.20.1-47.1.106` from the verified official installer, copies only the byte-identical final JAR into `mods`, requires launch target `forgeserver`, runs `smartdrops status` and `smartdrops validate`, and stops cleanly. The physical client gate uses the Java 17 development runtime for deterministic GUI interaction; the repository does not present a named userdev client as final-JAR production evidence.

The release artifact is:

```text
neoforge/build/libs/smart-resource-multiplier-neoforge-1.3.1+mc1.20.1.jar
```

Never publish a file from `neoforge/build/devlibs`, never label the NeoForge artifact as Fabric-compatible, and never install both loader JARs in one instance.
