# Resource Multiplier

Resource Multiplier is a focused Fabric utility mod that speeds up gathering without turning placed blocks, equipment, special entity transformations, or entity inventories into accidental duplication paths.

The current unreleased candidate adopts the new public name **Resource Multiplier**. Compatibility identifiers do not change: the Fabric mod ID remains `smart_resource_drops`, the configuration remains `config/smart_resource_drops.json`, saved-world provenance remains valid, commands remain under `/smartdrops` and `/smartdropsgui`, and datapack IDs remain in the `smart_resource_drops` namespace. Existing configurations, worlds, commands, and datapacks need no rename migration; the former public name is recorded only in the historical changelog.

The stable block path multiplies Minecraft's calculated block loot after enchantments and loot tables are applied. Version 1.1.0 adds a separate, default-off entity path that multiplies only final standard `LivingEntity` death-table loot. The current unreleased 1.2 work adds a third narrowly scoped path for certified standard entity shearing output. Minecraft evaluates the shearing loot once; Resource Multiplier multiplies only final helper-produced stacks, while the state transition, sound/event path, and tool damage happen once. Unknown modded shearables and special transformations remain fixed at vanilla `1x`.

## Version support

| Component | Version |
| --- | --- |
| Minecraft Java Edition | 26.2 |
| Fabric Loader | 0.19.3 or newer compatible release |
| Fabric API | 0.158.0+26.2 |
| Java | 25 or newer |
| Mod version | 1.1.0 metadata; 1.2.0 candidate passed automated gates but remains unreleased pending manual gates |

## Default behavior

- All eligible natural blocks use a global `2x` multiplier.
- Smart placement protection is enabled.
- A player-placed block returns normal vanilla loot when broken again.
- Piston-moved blocks are conservatively protected by default.
- Falling blocks carry their player-placed provenance to the landing position.
- Block entities are excluded unless explicitly allowlisted.
- Explosions are enabled.
- Automated mining integrations are disabled.
- Experience multiplication is disabled.
- Filtering uses blacklist mode.
- Dragon Egg and other unique/technical blocks are in the removable default safety blacklist.
- Multipliers are limited to `64x`.
- Entity death-loot multiplication is disabled by default and requires vanilla player-kill credit by default.
- Boss item drops, mob XP multiplication, and boss XP multiplication are independent default-off gates.
- Villagers/NPCs, golems, bosses, and unknown/unclassified entities retain safe `1x` category defaults.
- On a truly fresh configuration, manual entity shearing is enabled and inherits the global multiplier; automated dispenser shearing is disabled. Dispensers can run unattended and continuously feed farms, so automated shearing is a deliberate operator opt-in rather than a fresh-install surprise.
- Schema-1/schema-2 migrations and malformed-config recovery keep both shearing sources disabled so existing farms do not change without consent.
- Only `#smart_resource_drops:shearing/standard_resources` entities can multiply. The production tag certifies sheep; known/special transformations remain fixed vanilla `1x` even if another data pack creates a tag conflict.
- Shearing has an always-on whole-action budget of 1,024 items and 256 source/materialized stack groups. An over-budget action emits the complete original vanilla output instead of a partial multiplier.

To certify a compatible modded shearable, add its entity type to `data/smart_resource_drops/tags/entity_type/shearing/standard_resources.json` in a datapack with `"replace": false`. Certification is safe only when the entity uses Minecraft's standard `LivingEntity.dropFromShearingLootTable` helper; it cannot make a direct-output implementation compatible or override special safety. See [Compatibility notes](docs/COMPATIBILITY.md#certifying-a-compatible-modded-shearable) for the exact JSON and `/reload` verification procedure.

## Install

1. Install Minecraft Java Edition 26.2.
2. Install Fabric Loader 0.19.3 or a compatible newer release.
3. Install Fabric API for Minecraft 26.2.
4. Place the Resource Multiplier JAR in the `mods` folder.
5. Start the game or server once to create `config/smart_resource_drops.json`.

The server owns all gameplay rules in multiplayer. Clients cannot raise their own multiplier unless the server enables personal overrides. The mod may run server-side by itself for gameplay; install it on the client too when the in-game configuration screen is wanted.

## Basic use

Open the lightweight client screen with:

```text
/smartdropsgui
```

While connected, the screen requests the server's live configuration. It exposes block controls plus Entity Drops and independent Shearing Drops areas. Shearing provides manual/automated source toggles, an inheritable default, and search-first exact overrides only for certified standard resources. Empty search shows configured overrides rather than dumping the entity registry. All child screens share one draft and show a subtle unsaved-changes indicator when that draft is dirty; **Apply Changes** remains the single commit point on General. Non-operators receive the complete read-only view. Operator Apply sends one bounded delta containing only edited fields; the server permission-checks and validates it, saves once atomically, and returns a full authoritative snapshot on success or a compact explicit failure result.

Check the active server rules with:

```text
/smartdrops status
/smartdrops shearing status
```

Audit the current configured rule references as an operator or from the server console:

```text
/smartdrops validate
/smartdrops validate verbose
```

Validation is server-authoritative, read-only, and limited to the current configuration snapshot plus live block, entity-type, dimension, and tag registries. It checks only entries the administrator configured; it does not scan worlds, chunks, loaded entities, loot tables, or datapacks and does not trigger a resource reload. Unknown IDs and tags are reported but preserved so temporarily missing mods or datapacks do not destroy administrator intent. Compact output is capped at 15 issues and verbose output at 100. Load-time diagnostics retain only bounded samples and backup basenames; player UUID overrides are counted but never printed or sampled.

Shearing administration uses the existing permission and atomic-save path:

```text
/smartdrops admin shearing manual <on|off>
/smartdrops admin shearing automated <on|off>
/smartdrops admin shearing multiplier <inherit|0-64>
/smartdrops admin shearing entity <namespace:id> <inherit|0-64>
```

Adding an exact override requires a current registry entry in the standard-resource tag and not in special safety. `inherit` removal remains available for stale, unknown, or now-unsafe IDs. `/smartdrops inspect entity [verbose]` reports both death-loot and read-only shearing resolution without calling `readyForShearing`, evaluating loot, damaging a tool, or changing entity state.

Set the global multiplier as an operator:

```text
/smartdrops admin global 3
```

Set diamond ore to `4x` while everything else inherits:

```text
/smartdrops admin block minecraft:diamond_ore 4
/smartdrops admin block minecraft:deepslate_diamond_ore 4
```

Return a block to inherited behavior:

```text
/smartdrops admin block minecraft:diamond_ore inherit
```

### Diagnostic command

Look at a block within your normal interaction range and run:

```text
/smartdrops inspect
```

For the complete rule chain, including all matched categories, filters, provenance, block-entity protection, overrides, and source toggles, run:

```text
/smartdrops inspect verbose
```

Inspection is server-authoritative and read-only. The server performs the block raycast and uses the same rule-resolution trace as normal drops without breaking the block, evaluating loot, changing configuration or statistics, or consuming placement and recent-removal provenance. This makes the output suitable for mod compatibility reports: namespaced modded IDs are preserved, vanilla `minecraft:` and common `c:` tags participate in category resolution, and untagged blocks use the normal Miscellaneous fallback. The command works in singleplayer and multiplayer, including dedicated servers without Mod Menu; a player target is required when it is run from the console.

Look at a non-player living entity and use the parallel diagnostic:

```text
/smartdrops inspect entity
/smartdrops inspect entity verbose
```

Entity inspection performs its own server-side entity raycast and evaluates the same immutable decision used by death loot and mob XP. It reports all matched categories and sources, deterministic category selection, boss status, exact/tag filtering, hypothetical invoking-player eligibility, override/default/global resolution, and the separate item/XP gates. It does not damage or move the entity, mutate combat credit, generate loot/XP, reveal inventories/equipment/NBT, or change configuration.

### Mod Menu integration

Mod Menu is optional. When Mod Menu 20.0.0 or a compatible newer 26.2 release is installed on the client, Resource Multiplier exposes a native `Configure` button from the Mods screen. It and `/smartdropsgui` use the same screen implementation.

From the title screen, Configure opens the full editor for the local `config/smart_resource_drops.json`; those atomically saved values are defaults for future local/singleplayer servers. Once connected, the screen never falls back to that local file: the integrated or remote server remains authoritative. Regular multiplayer players see the same values read-only, while operators can apply validated server changes. Dedicated servers do not need Mod Menu.

## Rule priority

The base multiplier is resolved from highest to lowest specificity:

1. Individual block override
2. Category override
3. Dimension override
4. Global multiplier

An optional per-player override is applied after that hierarchy and is capped by the server.

Before that hierarchy is used, the mod evaluates source toggles, blacklist or whitelist rules, block-entity safety, and placed-block provenance.

Entity rules are independent. Exact entity override wins over entity category override, which wins over the default entity multiplier when that value is not set to inherit, which then falls back to the global multiplier. Permanent player/armor-stand exclusions, entity filters, vanilla kill attribution, and the boss gate are evaluated before the item multiplier. Mob XP uses separate default-off gates and never inherits block XP settings.

## Anti-dupe design

The mod records only qualifying player placement positions, not every natural block in the world. The data is stored persistently on each affected chunk. Untouched chunks carry no placement set.

When a recorded block is broken in the default `NATURAL_ONLY` mode, its multiplier is forced to `1x`, then its placement record is removed after a successful break. Moving the block with a piston transfers or conservatively creates protection at the destination. Sand, gravel, concrete powder, anvils, and other falling blocks carry the protection marker while they fall.

With Smart Placement Protection disabled, `NATURAL_ONLY` treats both natural and placed blocks as eligible. `PLAYER_PLACED_ONLY` is different by design: natural blocks remain excluded whether protection is on or off, while known player-placed blocks remain eligible. `ALL` admits both. Placement history continues to be tracked independently of the toggle.

Same-resource transformations retain protection: examples include stripping a log, tilling or pathing dirt, copper weathering or waxing, and concrete powder becoming concrete. A sapling growing into a tree does not mark the generated logs as placed. Crop and similar growth, amethyst growth, cobblestone generators, snow formation, and other newly generated resources remain eligible unless filtered by configuration.

See [docs/ANTI_DUPE.md](docs/ANTI_DUPE.md) for the full behavior model.

## Block output safety

Before a block multiplier greater than `1x` allocates or emits multiplied stacks, Resource Multiplier preflights the block's complete final loot list. The multiplied result must fit both hard bounds: at most 262,144 items and at most 4,096 legal stacks. Arithmetic is overflow-safe. If either estimate exceeds its bound, the entire original list is returned untouched at vanilla `1x`; there is no partial multiplication, partial truncation, or partially rebuilt list. This applies uniformly to qualifying player mining, explosions, and supported automation.

When statistics are enabled, a budget fallback records the block as evaluated, counts only the original vanilla items, and increments `blockBudgetFallbacks`; it does not count the block as multiplied or invent bonus items. The server warning includes the block, dimension, position, multiplier, estimates, limits, and the `1x` fallback, but is rate-limited by block ID and reason to a bounded 256-key cache with a five-minute interval. `/smartdrops inspect verbose` reports the limits, while remaining read-only and never evaluating the target's loot table.

## Configuration

The generated file is:

```text
config/smart_resource_drops.json
```

A clean reference copy is included at [config-examples/default.json](config-examples/default.json).

Important values:

```json
{
  "globalMultiplier": 2,
  "maximumMultiplier": 64,
  "smartPlacementProtection": true,
  "sourceMode": "NATURAL_ONLY",
  "filterMode": "BLACKLIST",
  "multiplyExperience": false,
  "experienceMultiplier": 2,
  "explosions": true,
  "automatedMining": false,
  "protectBlockEntities": true,
  "conservativePistonProtection": true,
  "entityDropsEnabled": false,
  "defaultEntityMultiplier": 2,
  "entityKillRequirement": "PLAYER_KILLS_ONLY",
  "bossDropsEnabled": false,
  "multiplyMobExperience": false,
  "mobExperienceMultiplier": 2,
  "multiplyBossExperience": false
}
```

Complete field documentation is in [docs/CONFIGURATION.md](docs/CONFIGURATION.md).

## Categories

Built-in data tags group blocks into these categories:

- Ores
- Raw resource blocks
- Logs
- Stone
- Soil
- Nether
- End
- Crops
- Plants
- Leaves
- Building blocks
- Miscellaneous

Modded blocks automatically inherit a category when they participate in the vanilla `minecraft:` or common `c:` block tags used by that category. Untagged blocks use Miscellaneous. A block can match multiple categories; the first configured match in the listed order wins. Individual block overrides always win and can resolve an overlap explicitly.

Entity categories are Bosses, Villagers/NPCs, Golems, Neutral, Passive, Hostile, Aquatic, Ambient, and Miscellaneous/Unclassified. Project-owned tags live at `#smart_resource_drops:categories/<category>` and are datapack-extensible with `replace: false`. Explicit project tags take precedence over class and `MobCategory` fallbacks; unknown modded living entities use Miscellaneous at safe `1x`. Bosses are detected through the boss tag plus a conservative vanilla fallback set. A final standard-loot saddle or Totem of Undying remains protected even when boss multiplication is deliberately enabled.

## Presets

```text
/smartdrops admin preset vanilla-plus
/smartdrops admin preset faster-survival
/smartdrops admin preset fast-progression
```

- `vanilla-plus`: ores and logs at `2x`; most other categories at `1x`.
- `faster-survival`: ores, stone, soil, Nether, End, crops, and plants at `2x`; logs at `3x`.
- `fast-progression`: all safe eligible blocks at `4x`.

## Commands

All administrative commands start with `/smartdrops admin` and require game-master permission.

```text
/smartdrops status
/smartdrops shearing status
/smartdrops validate
/smartdrops validate verbose
/smartdrops inspect
/smartdrops inspect verbose
/smartdrops inspect entity
/smartdrops inspect entity verbose
/smartdrops gui
/smartdrops personal <0..64|inherit>

/smartdrops admin enabled <on|off>
/smartdrops admin global <0..64>
/smartdrops admin maximum <1..64>
/smartdrops admin player-maximum <1..64>
/smartdrops admin protection <on|off>
/smartdrops admin source <natural-only|all|player-placed-only>
/smartdrops admin xp <on|off>
/smartdrops admin player-mining <on|off>
/smartdrops admin xp-multiplier <1..64>
/smartdrops admin explosions <on|off>
/smartdrops admin automation <on|off>
/smartdrops admin blockentities <on|off>
/smartdrops admin piston-safe <on|off>
/smartdrops admin player-overrides <on|off>
/smartdrops admin stats <on|off>
/smartdrops admin filter-mode <blacklist|whitelist>
/smartdrops admin block <block_id> <0..64|inherit>
/smartdrops admin category <category> <0..64|inherit>
/smartdrops admin dimension <dimension_id> <0..64|inherit>
/smartdrops admin blacklist <add|remove> <block_id>
/smartdrops admin whitelist <add|remove> <block_id>
/smartdrops admin tag-blacklist <add|remove> <tag_id>
/smartdrops admin tag-whitelist <add|remove> <tag_id>
/smartdrops admin blockentity-allowlist <add|remove> <block_id>
/smartdrops admin preset <vanilla-plus|faster-survival|fast-progression>
/smartdrops admin shearing manual <on|off>
/smartdrops admin shearing automated <on|off>
/smartdrops admin shearing multiplier <inherit|0-64>
/smartdrops admin shearing entity <namespace:id> <inherit|0-64>
/smartdrops admin reload
/smartdrops admin reset
/smartdrops admin statistics <show|reset>
```

## Build from source

The project includes a checksum-verifying Gradle 9.5.1 bootstrap for macOS, Linux, and Windows. It does not select an arbitrary system Gradle.

```bash
./gradlew clean build
```

Windows:

```bat
gradlew.bat clean build
```

The playable JAR is created at `build/libs/resource-multiplier-1.1.0.jar`. Java 25 is required. The build pins stable Fabric Loom 1.17.20 and uses Mod Menu only as a compile-time client integration.

For the complete release check on Windows PowerShell:

```powershell
python tools/validate_package.py
python scripts/test_release_packaging.py
python scripts/test_modmenu_integration.py
python scripts/edge_case_source_audit.py
python scripts/polish_regression_tests.py
.\tools\run_core_tests.ps1
.\gradlew.bat --no-daemon clean test runGameTest build
.\gradlew.bat --no-daemon runClientGameTest
```

The POSIX equivalent uses `bash tools/run_core_tests.sh` and `./gradlew`; CI runs the client GameTest under Xvfb. Project sources compile with `-Xlint:deprecation` so obsolete Minecraft/Fabric calls remain visible, without promoting third-party dependency warnings to fatal errors. The current cleanup uses `BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(...)` for entity holders and the supported `makeMockServerPlayer(GameType)` GameTest API through shared helpers instead of broad warning suppressions.

The release gate includes every source/package validator, the complete core and mapped-Minecraft JUnit suites, all required dedicated-server Fabric GameTests, the real client GUI/authority GameTest, and the Loom build. Packaging checks reject nested JARs; common shaded dependency, test, and fixture namespaces; undeclared or missing entrypoint/mixin/tag assets; and root-level source, config, logs, worlds, caches, or packaged-release leaks from the playable JAR. The deterministic source bundle must retain the issue forms, pull-request template, scope/security documentation, wrapper, protected-output tag, and key source assets. Exact final counts and artifact identity are recorded only after the final build in [BUILD_STATUS.md](BUILD_STATUS.md).

## Scope

### Project charter

Resource Multiplier multiplies final loot produced by qualifying block breaks, living-entity deaths, and supported entity shearing actions. It does not change resource generation, harvesting speed, crafting, processing, transportation, storage, spawning, combat, or world progression.

In scope are server-authoritative multiplication of final loot, placement/death provenance and attribution safety, supported player/vanilla-dispenser shearing scopes, loot-boundary compatibility, bounded configuration and diagnostics, performance safeguards, and datapack/mod interoperability at the block-loot, standard entity death-table, and standard shearing-helper boundaries.

Explicitly excluded are vein mining, tree felling, automatic smelting, magnets, inventory sorting or movement, tool/enchantment rebalancing, and new per-biome, per-tool, per-enchantment, weather, or time-of-day rule layers. Chest/structure loot, fishing, bartering, trading, milking, brushing, egg laying, gifts, breeding output, spawning, crafting/processing, player or armor-stand deaths, equipment, held or picked-up items, entity inventories, direct equipment ejection, custom/direct item creation, beehive shearing, and unsupported custom-machine shearing paths remain outside the multiplier boundary.

The 1.0.x releases remain block-resource-only and 1.1.0 remains the entity-death release baseline. The current tree contains unreleased shearing work and has passed its serial automated suite plus server/client startup checks, but deliberately retains 1.1.0 metadata until the required hands-on gameplay, UI, multiplayer, reload, and third-party matrix is complete. No 1.2.0 release is claimed; see [docs/ROADMAP.md](docs/ROADMAP.md).

Ordinary defects should use the structured bug form, exact third-party conflicts should use the mod-compatibility form, and proposed changes should use the pull-request template. Blank issues remain enabled for cases the forms do not fit. Follow [SECURITY.md](SECURITY.md) for sensitive reports; this snapshot does not claim an unavailable private intake channel.

## Project documents

- [Architecture](docs/ARCHITECTURE.md)
- [Anti-dupe behavior](docs/ANTI_DUPE.md)
- [Configuration reference](docs/CONFIGURATION.md)
- [Compatibility notes](docs/COMPATIBILITY.md)
- [Performance design](docs/PERFORMANCE.md)
- [Testing and verification](docs/TESTING.md)
- [Public release checklist](docs/PUBLIC_RELEASE_CHECKLIST.md)
- [Roadmap](docs/ROADMAP.md)
- [GitHub upload guide](docs/GITHUB_UPLOAD.md)
- [Implementation log](docs/IMPLEMENTATION_LOG.md)
- [Build status](BUILD_STATUS.md)
- [Changelog](CHANGELOG.md)
- [Contributing](CONTRIBUTING.md)
- [Security policy](SECURITY.md)

## License

MIT. See [LICENSE](LICENSE).

## Verification and release gate

Version 1.0.3 and the pre-hardening 1.1 checkpoint remain preserved in [CHANGELOG.md](CHANGELOG.md) as history. The final 1.1.0 automated pass records 90 core assertions, 125 mapped JUnit tests in 21 suites, 22 dedicated GameTests, the real client GameTest, standalone server and client startup checks, all five validators, and a 272-entry playable-JAR audit. These counts are runner evidence, not a one-test-per-requirement claim. The exact artifact identity and the manual gates that remain open are recorded in [docs/TESTING.md](docs/TESTING.md), [BUILD_STATUS.md](BUILD_STATUS.md), and [docs/PUBLIC_RELEASE_CHECKLIST.md](docs/PUBLIC_RELEASE_CHECKLIST.md).
