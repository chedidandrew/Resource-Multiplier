# Smart Resource Multiplier - NeoForge 26.2

This directory is the NeoForge loader target for Minecraft 26.2.

## Port contract

The NeoForge build must preserve the public gameplay contract of the Fabric build:

- mod ID and data namespace: `smart_resource_drops`
- config file: `config/smart_resource_drops.json`
- `/smartdrops` and `/smartdropsgui`
- block, category, dimension, player, source, entity, XP, and shearing multipliers
- smart player-placement protection, including piston and falling-block provenance
- server-authoritative GUI editing and revision checks
- the same bounded output and rate-limit safety rules
- the same resource tags and language assets

Loader-specific code belongs under this directory. Gameplay policy stays in the existing loader-neutral `core`, `config`, payload, and GUI classes wherever Minecraft APIs permit it.

## Toolchain

- Minecraft 26.2
- NeoForge 26.2.0.72
- ModDevGradle 2.0.144
- Java 25

The versions above match the official NeoForge 26.2 MDK baseline current when this port was started.

## Build

From this directory, use the repository Gradle wrapper with `-p neoforge` once the port branch has completed its compile gate:

```text
../gradlew -p neoforge build
```

Do not publish a NeoForge artifact until the parity checklist in `docs/NEOFORGE_PORT.md` is green. The existing Fabric release remains the production artifact while this port is validated.
