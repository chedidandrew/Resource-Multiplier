# NeoForge 1.21.6-1.21.8 backport

## Baseline and layout

The NeoForge build uses two verified profiles: Minecraft `1.21.6` with NeoForge `21.6.20-beta`, and Minecraft `1.21.7-1.21.8` with NeoForge `21.7.25-beta`. Both use ModDevGradle `2.0.146`, FML 4, and Java 21. The build consumes the canonical common and client source trees while excluding Fabric adapters, then adds NeoForge entrypoints, networking, placement storage, mixins, metadata, and target-native tests under `neoforge/`.

Both loaders preserve:

- mod ID and datapack/network namespace `smart_resource_drops`;
- `config/smart_resource_drops.json` and schema 3;
- `/smartdrops` and `/smartdropsgui`;
- multiplier precedence, placement protection, budgets, permissions, and GUI revision behavior.

## Target-specific adapters

- NeoForge lifecycle events install common commands, server ticks, configuration, and storage.
- All six payload types register through target-native NeoForge payload handlers. Client handlers are installed behind physical-client indirection so dedicated servers do not link client classes.
- Placed-block provenance uses a nonsynced NeoForge data attachment with the stable logical identity.
- Native `FakePlayer` detection keeps automated actors excluded from player-only attribution.
- NeoForge-specific placement and shearing mixins use the verified 1.21.6-1.21.8 method descriptors with required injection counts.
- The older RegisterGameTestsEvent API receives an exact 64-test suite through a loader-only generator and binary `data/smart_resource_drops_gametest/structure/wide.nbt` fixture.

Minecraft 1.21.6-1.21.8 exposes the standard living-entity shearing loot-table helper. The implementation wraps its final item consumer inside tightly scoped player or supported automation context. Special transformations remain independently protected and stay at vanilla output.

## World compatibility

Configuration files remain compatible between loaders because they retain the same schema and identity. Fabric and NeoForge store placed-block provenance differently, and this backport does not claim cross-loader placed-block-data migration or Minecraft world downgrade support. Back up a world before changing Minecraft versions or loaders.

## Required release evidence

Before publication, the final source commit must pass:

- the mapped JUnit suite and exactly 64 NeoForge dedicated-server GameTests;
- a native attachment mark/save/unload/restart/lookup/removal persistence test;
- clean dedicated-server and physical-client starts;
- Entity Categories GUI, multiplayer authority/reconnect, optional-channel, and oversized-wire gates;
- a clean packaged-JAR-only server and client test with no development source-set mod;
- final JAR metadata, Java-21 bytecode, loader-isolation, fixture-isolation, icon, and license validation.

The NeoForge filenames are `smart-resource-multiplier-neoforge-1.3.2+mc1.21.6.jar` for Minecraft 1.21.6 and `smart-resource-multiplier-neoforge-1.3.2+mc1.21.7-1.21.8.jar` for Minecraft 1.21.7-1.21.8. Install only the file matching the chosen loader and Minecraft version.
