# Smart Resource Multiplier rebrand and upgrade compatibility

## Public name

Beginning with source version `1.2.2`, the public project name is **Smart Resource Multiplier**. Versions `1.2.0` and `1.2.1` used **Resource Multiplier**, and earlier development builds used **Smart Resource Drops**.

The new name better represents the project because multiplication is governed by source-aware rules, persistent player-placement provenance, conservative protected-output gates, and bounded whole-result fallback rather than a blind quantity multiplier.

## Compatibility identifiers that do not change

The rebrand deliberately preserves every identifier that existing worlds, configurations, datapacks, servers, and clients use to recognize the mod:

- Fabric mod ID: `smart_resource_drops`
- Configuration file: `config/smart_resource_drops.json`
- Datapack and network namespace: `smart_resource_drops`
- Saved placement-provenance identifier: `smart_resource_drops:placed_blocks`
- Main server command: `/smartdrops`
- Client configuration command: `/smartdropsgui`
- Java package and entrypoint namespace: `com.chedidandrew.smartresourcedrops`

No configuration migration, world conversion, command rename, datapack rename, or server reset is required. The configuration schema remains schema 3.

## JAR and release-package names

The downloadable file name changes with `1.2.2`:

```text
Before: resource-multiplier-1.2.1.jar
After:  smart-resource-multiplier-1.2.2.jar
```

Version `1.3.0` keeps `smart-resource-multiplier-1.3.0.jar` for Fabric and adds the unambiguous `smart-resource-multiplier-neoforge-1.3.0.jar` for NeoForge. Install exactly one loader-matching file.

Release source and bundle names likewise change from the `ResourceMultiplier-*` prefix to `SmartResourceMultiplier-*`.

Before installing `1.2.2` or newer, remove the older `resource-multiplier-*.jar` file from the `mods` folder. Also remove any older Smart Resource Multiplier copy before installing `1.3.0`; never place both Fabric and NeoForge JARs in the same instance because they declare the same stable mod ID.

## Repository and storefront links

The GitHub repository URL remains `chedidandrew/Resource-Multiplier` so existing bookmarks, issue links, badges, and release references continue to work without a URL migration. The CurseForge project slug also remains `resource-multiplier`. The visible project name in source metadata, the in-game mod list, configuration screens, commands, diagnostics, documentation, build artifacts, and future release packages is Smart Resource Multiplier.

## Icon history

The icon was intentionally unchanged in the published `1.2.2` rebrand so binary artwork review remained separate from compatibility-sensitive naming changes. Version `1.2.3` introduced the separately approved `512x512` **SMART RESOURCE MULTIPLIER** diamond-mining artwork documented in [BRANDING.md](BRANDING.md); both `1.3.0` loader builds package those exact approved bytes.
