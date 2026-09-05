# NeoForge 1.21.5 backport

## Baseline and layout

The NeoForge build targets Minecraft `1.21.5`, NeoForge `21.5.98`, ModDevGradle `2.0.146`, and Java 21. It consumes the canonical common and client source trees while excluding Fabric adapters, then adds NeoForge entrypoints, networking, placement storage, mixins, metadata, and target-native tests under `neoforge/`.

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
- NeoForge-specific placement and shearing mixins use exact 1.21.5 method descriptors with required injection counts.
- The older RegisterGameTestsEvent API receives an exact 64-test suite through a loader-only generator and binary `data/smart_resource_drops_gametest/structure/wide.nbt` fixture.

Standard living-entity shearing uses the safe final-output boundary available on this target. Special transformations and unsupported shearing paths stay `1x`.

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

The established filenames are `smart-resource-multiplier-1.3.2+mc1.21.5.jar` for Fabric and `smart-resource-multiplier-neoforge-1.3.2+mc1.21.5.jar` for NeoForge. Install only the file matching the chosen loader.
