<p align="center">
  <img
    src="src/main/resources/assets/smart_resource_drops/icon.png"
    width="180"
    alt="Smart Resource Multiplier icon"
  >
</p>

<h1 align="center">Smart Resource Multiplier</h1>

<p align="center">
  Configurable multipliers for block drops, mob loot, and supported shearing,<br>
  with persistent anti-duplication protection.
</p>

<p align="center">
  <a href="https://github.com/chedidandrew/Resource-Multiplier/actions/workflows/build.yml"><img alt="Build and verify" src="https://github.com/chedidandrew/Resource-Multiplier/actions/workflows/build.yml/badge.svg?branch=main"></a>
  <img alt="Minecraft 26.2" src="https://img.shields.io/badge/Minecraft-26.2-62B47A">
  <img alt="Fabric loader" src="https://img.shields.io/badge/Loader-Fabric-DBD0B4">
  <a href="https://www.curseforge.com/minecraft/mc-mods/resource-multiplier"><img alt="Download on CurseForge" src="https://img.shields.io/badge/Download-CurseForge-F16436?logo=curseforge&amp;logoColor=white"></a>
  <img alt="Java 25" src="https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk&amp;logoColor=white">
  <a href="LICENSE"><img alt="MIT license" src="https://img.shields.io/badge/License-MIT-2EA44F"></a>
  <img alt="Stable release 1.2.2" src="https://img.shields.io/badge/Status-1.2.2-Release-2EA44F">
</p>

> [!IMPORTANT]
> **Current stable release:** Smart Resource Multiplier `1.2.2` for Minecraft Java Edition 26.2 and Fabric. This release completes the public rebrand from Resource Multiplier without changing gameplay or compatibility identifiers.

Smart Resource Multiplier speeds up repetitive gathering by multiplying the final loot Minecraft already calculated. Fortune, Silk Touch, Looting, loot-table changes, item components, NBT, and legal stack sizes are preserved because the mod works after normal loot evaluation instead of replacing it.

Placed-block tracking and conservative safety rules prevent common duplication loops. The server owns gameplay settings in multiplayer, while clients can use the optional configuration interface and Mod Menu integration.

## Highlights

- Global, category, dimension, and individual block multipliers from `0x` through `64x`.
- Persistent anti-duplication protection for player-placed blocks, including piston movement and falling-block provenance.
- Optional, independent multiplication of standard non-player mob death loot.
- Optional manual and vanilla-dispenser multiplication for certified standard shearing resources.
- Separate block, entity, shearing, category, dimension, source, and filter rules.
- Server-authoritative multiplication and configuration for multiplayer worlds.
- A native configuration GUI through `/smartdropsgui`, with optional Mod Menu integration.
- Read-only block/entity inspection and configuration validation commands for troubleshooting.
- Whole-result safety budgets that fall back to the original vanilla output instead of partially multiplying pathological loot.

Smart Resource Multiplier does not add vein mining, tree felling, automatic smelting, magnets, or inventory automation. Its project charter is deliberately limited to safely multiplying final loot at documented Minecraft boundaries.

## Screenshots

<p align="center">
  <img
    src="docs/images/general-config.webp"
    width="760"
    alt="Smart Resource Multiplier general configuration screen"
  >
</p>

<p align="center">
  <img
    src="docs/images/block-overrides.webp"
    width="760"
    alt="Smart Resource Multiplier block override search screen"
  >
</p>

<p align="center">
  <img
    src="docs/images/shearing-config.webp"
    width="760"
    alt="Smart Resource Multiplier shearing configuration screen"
  >
</p>

These are real client captures from the validated `1.2.x` code line. The interface layout is unchanged by the `1.2.2` rebrand.

## Download

Download the current release from [CurseForge](https://www.curseforge.com/minecraft/mc-mods/resource-multiplier). Verified release bundles, source archives, and checksums are also published through [GitHub Releases](https://github.com/chedidandrew/Resource-Multiplier/releases).

The current published Fabric 26.2 artifact is `smart-resource-multiplier-1.2.2.jar`. Remove any older `resource-multiplier-*.jar` before installing it so Fabric does not load two files with the same internal mod ID.

You can also [build the current source](#build-from-source). Do not download JARs from unofficial mirrors.

## Requirements and installation

| Component | Supported version |
| --- | --- |
| Minecraft Java Edition | 26.2 |
| Fabric Loader | 0.19.3 or a compatible newer release |
| Fabric API | 0.158.0+26.2 |
| Java | 25, required by Minecraft 26.2 |
| Mod Menu | Optional; 20.0.0 or a compatible 26.2 release |

1. Install Minecraft Java Edition 26.2.
2. Install Fabric Loader.
3. Install the matching Fabric API.
4. Place the Smart Resource Multiplier JAR in the `mods` folder.
5. Install Smart Resource Multiplier on the server for authoritative multiplayer behavior.
6. Install it on the client when you want the configuration GUI or optional Mod Menu integration.

Starting once creates `config/smart_resource_drops.json`. The public rename did not change the internal mod ID (`smart_resource_drops`), configuration path, Java package, datapack namespace, saved-world provenance, network identifiers, or the legacy-compatible `/smartdrops` commands.

## Configuration at a glance

The default block behavior multiplies eligible natural resources by `2x`. Player-placed blocks remain vanilla `1x`, block entities are protected, automation and XP multiplication are opt-in, and unique or technical resources can remain on the safety blacklist.

Block rules resolve from most specific to broadest:

```text
Block override
> Category
> Dimension
> Global
```

Entity death-loot rules and shearing rules are separate domains with their own enablement, safety, and override paths. Changing one does not silently enable either of the others. Multiplayer edits are permission-checked and committed by the server; non-operators receive the same configuration screens in read-only mode.

Use `/smartdropsgui` to open the editor. Every child screen shares one staged draft, while **Apply Changes** on General performs the authoritative save. Complete field descriptions, defaults, presets, tag extension points, and migration behavior are in the [configuration reference](docs/CONFIGURATION.md).

## Common commands

```text
/smartdropsgui
/smartdrops status
/smartdrops inspect
/smartdrops inspect entity
/smartdrops validate
```

Inspection is read-only and uses the same rule-resolution logic as gameplay without breaking a block, damaging an entity, or generating loot. Validation is also read-only and checks configured IDs and tags against live registries without deleting unknown entries, but `/smartdrops validate` is restricted to game masters or the server console. Administrative changes require game-master permission.

See the [complete command reference](docs/COMMANDS.md) for verbose diagnostics, personal overrides, administrator controls, filters, presets, shearing settings, maintenance, and statistics.

## Safety and compatibility

- Natural blocks may be multiplied; player-placed blocks stay vanilla in the default source mode.
- Placement provenance survives supported piston and falling-block movement across saves.
- Block entities and inventory-bearing outputs are protected by default.
- Boss loot, equipment, inventories, and special transformations use conservative independent gates.
- Unknown modded shearables stay `1x` until a datapack deliberately certifies a compatible standard-helper implementation.
- Output budgets prevent pathological item or stack explosions and return the complete original result on fallback.

Smart Resource Multiplier supports documented vanilla/Fabric boundaries, not every mod automatically. Compatibility reports should name the exact other project and version. See [Compatibility](docs/COMPATIBILITY.md), [Anti-duplication design](docs/ANTI_DUPE.md), [Edge cases](docs/EDGE_CASES.md), and [Performance](docs/PERFORMANCE.md) for the precise behavior and limitations.

## Build from source

Java 25 is required. The included Gradle wrapper verifies its pinned Gradle distribution and produces the playable JAR in `build/libs/`.

macOS or Linux:

```bash
./gradlew clean build
```

Windows CMD:

```bat
gradlew.bat clean build
```

The expected artifact is `build/libs/smart-resource-multiplier-1.2.2.jar`. See [Testing and verification](docs/TESTING.md) for the full validator and GameTest sequence; the landing page intentionally does not duplicate the CI pipeline.

## Documentation

- [Configuration](docs/CONFIGURATION.md) — fields, defaults, hierarchy, presets, migrations, and GUI behavior.
- [Commands](docs/COMMANDS.md) — complete player, diagnostic, and administrator command reference.
- [Anti-duplication design](docs/ANTI_DUPE.md) — persistent provenance and transformation rules.
- [Compatibility](docs/COMPATIBILITY.md) — supported integration boundaries and datapack extension points.
- [Architecture](docs/ARCHITECTURE.md) — server-authoritative runtime design and loot boundaries.
- [Performance](docs/PERFORMANCE.md) — budgets, bounds, caches, and failure behavior.
- [Edge cases](docs/EDGE_CASES.md) — unusual block, entity, and interaction handling.
- [Testing](docs/TESTING.md) — automated evidence and manual test requirements.
- [Public release checklist](docs/PUBLIC_RELEASE_CHECKLIST.md) — automated, manual, and third-party release gates.
- [Build status](BUILD_STATUS.md) — current artifact identity and verification result.
- [Rebrand and upgrade compatibility](docs/REBRAND.md) — preserved identifiers, JAR-name transition, and upgrade steps.
- [Branding](docs/BRANDING.md) - current production icon identity, checksum, and release-history boundary.
- [GitHub publication guide](docs/GITHUB_UPLOAD.md) — contribution, CI, packaging, and release-latch process.
- [Security policy](SECURITY.md) and [Contributing](CONTRIBUTING.md) — reporting and development guidance.

## License and support

Smart Resource Multiplier is open source under the permissive [MIT License](LICENSE). You may use, copy, modify, publish, distribute, sublicense, or sell the software while retaining the copyright and permission notices.

Download releases from [CurseForge](https://www.curseforge.com/minecraft/mc-mods/resource-multiplier). Report ordinary defects through [GitHub Issues](https://github.com/chedidandrew/Resource-Multiplier/issues), use the compatibility form for a reproducible conflict with another mod, and follow [SECURITY.md](SECURITY.md) for sensitive reports. Source code and verified release checksums remain available on [GitHub](https://github.com/chedidandrew/Resource-Multiplier).

Development can be supported through [Ko-fi](https://ko-fi.com/andrewchedid), [PayPal](https://www.paypal.com/paypalme/chedidandrew), or [Cash App](https://cash.app/%24AndrewChedid). Support is optional and never changes access to releases, source code, issue support, or modpack permission.
