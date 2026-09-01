# NeoForge 26.2 Port

## Status

Port work started on 2026-09-01 from Fabric 1.2.3. The Fabric implementation remains the behavioral reference and production release until the NeoForge compile, GameTest, client, and dedicated-server gates are green.

## Public repository layout

The repository is moving toward a loader-explicit layout without duplicating gameplay policy:

```text
/
├── src/                 # current Fabric production source while migration is staged
├── neoforge/            # NeoForge 26.2 loader target and metadata
├── docs/                # shared behavior, compatibility, testing, release history
├── config-examples/     # shared config schema/examples
└── tools/               # release and validation tooling
```

After NeoForge parity is proven, the next cleanup pass should promote the layout to `common/`, `fabric/`, and `neoforge/`. That move is deliberately deferred until the loader adapters are proven so the existing public Fabric build is not destabilized by a cosmetic source move.

## Compatibility contract

The NeoForge port must keep these identifiers stable:

- `smart_resource_drops` mod ID and datapack/network namespace
- `config/smart_resource_drops.json`
- schema 3 configuration and migration behavior
- `smart_resource_drops:placed_blocks` saved provenance identity
- `/smartdrops` and `/smartdropsgui`
- `com.chedidandrew.smartresourcedrops` Java namespace

The port must preserve multiplier precedence, source modes, smart placement protection, block-entity protection, piston/falling-block propagation, block/entity/shearing output budgets, XP rules, permissions, GUI revision handling, config atomicity, and malformed/oversized config recovery.

## Loader adapters required

Fabric-specific integration points identified during the audit:

1. Main entrypoint and block-break callback
2. Command registration
3. Payload registration, send/can-send, lifecycle, and server tick callbacks
4. Client payload handlers, disconnect callback, and `/smartdropsgui`
5. Client tick queue
6. Config-directory discovery
7. Installed-mod resource discovery used by title-screen tag indexes
8. Persistent chunk attachment used by placement provenance
9. Fabric fake-player detection in block drop source classification
10. Mod Menu integration, which remains Fabric-only and must not be a NeoForge dependency

Everything else should remain shared where the Minecraft 26.2 API surface is loader-neutral.

## NeoForge baseline

The port workspace is pinned to the official NeoForge 26.2 MDK baseline used at kickoff:

- Minecraft `26.2`
- NeoForge `26.2.0.72`
- ModDevGradle `2.0.144`
- Java `25`

## Release gates

A NeoForge JAR is not public-release ready until all of the following pass:

- NeoForge `compileJava`
- NeoForge `build`
- dedicated-server startup with no client classloading
- client startup and configuration GUI smoke test
- all applicable core JUnit tests
- equivalent NeoForge GameTests for block loot, provenance, entity loot/XP, shearing, piston/falling-block movement, and output budgets
- multiplayer permissions and server-authoritative GUI mutation tests
- Fabric CI still passes unchanged
- both loader JAR names include their loader suffix
- release documentation lists Fabric and NeoForge separately

## Packaging policy

Public artifacts should be named distinctly:

- `smart-resource-multiplier-fabric-<version>.jar`
- `smart-resource-multiplier-neoforge-<version>.jar`

Never publish one loader's JAR under an ambiguous filename.
