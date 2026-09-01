# Smart Resource Multiplier configuration reference

Smart Resource Multiplier intentionally continues to use the compatibility-stable gameplay configuration file `config/smart_resource_drops.json`. On a dedicated or connected server it is authoritative; the client installation's title-screen copy supplies defaults for future local/singleplayer servers. It is generated on first launch and rewritten in a normalized form after a real change. No-op updates do not write the file.

Values outside safe ranges are clamped during load. A malformed file is replaced only after the original is successfully moved to a timestamped `.broken-*` backup. If the file cannot be read or backed up, writes are suppressed so the original is not destroyed. A file with a schema newer than this mod supports is left untouched; the server logs the incompatibility and runs safe in-memory defaults without downgrading the file.

If `blacklist` is absent or `null`, the safety blacklist is installed for compatibility with older/minimal files. An explicit `"blacklist": []` is an administrator choice and remains empty.

## Schema 3 shearing settings

Smart Resource Multiplier 1.2.3 uses schema 3. The icon and build-toolchain refresh changes no configuration field, default, migration rule, or authority behavior. These settings form an independent 256-entry domain and do not consume the block or death-entity rule budgets.

| Field | Fresh/reset default | Meaning |
| --- | ---: | --- |
| `manualShearingDropsEnabled` | `true` | Allow supported real-player entity shearing to use the shearing multiplier |
| `automatedShearingDropsEnabled` | `false` | Allow only the vanilla dispenser entity-shearing call to use it |
| `inheritDefaultShearingMultiplier` | `true` | Inherit `globalMultiplier` when no exact certified-entity override exists |
| `defaultShearingMultiplier` | `2` | Stored `0..maximumMultiplier` value used when inheritance is off |
| `shearingEntityMultipliers` | `{}` | Exact certified entity-type rules, at most 256 entries |

A truly missing file receives the fresh defaults above. Automated dispenser shearing is intentionally OFF because it can run unattended and continuously amplify farms; an operator must opt in after reviewing the multiplier and output budget. Schema-1 and schema-2 files migrate with both shearing toggles OFF, inheritance ON, and no shearing overrides. In particular, schema 2→3 preserves every valid entity death-loot field; it does not apply the old broad “pre-entity” reset. Malformed existing-file recovery and unsupported future-schema in-memory fallback also keep both shearing sources OFF. An explicit confirmed Reset restores the current fresh defaults, including manual shearing ON, but does not change placement provenance, world data, entity state, or tools.

Rule priority is: hard special safety → unknown/uncertified safety → exact shearing entity override → shearing default → global. An exact override cannot certify an entity. Certification comes from `#smart_resource_drops:shearing/standard_resources`; `#smart_resource_drops:shearing/special` and the audited known-vanilla-special set always win. Unknown and special entities remain vanilla `1x`, including when the inherited default is `0x` or greater than `1x`.

Operator commands use one atomic configuration update:

```text
/smartdrops admin shearing manual <on|off>
/smartdrops admin shearing automated <on|off>
/smartdrops admin shearing multiplier <inherit|0-64>
/smartdrops admin shearing entity <namespace:id> <inherit|0-64>
```

Adding a rule requires the entity to exist in the live registry, be certified standard, and not be special. Removing an existing rule with `inherit` remains possible after a mod or datapack disappears. `/smartdrops shearing status`, `/smartdrops inspect entity [verbose]`, and `/smartdrops validate [verbose]` are read-only.

## Core fields

| Field | Default | Meaning |
| --- | ---: | --- |
| `enabled` | `true` | Master switch |
| `globalMultiplier` | `2` | Fallback item multiplier |
| `maximumMultiplier` | `64` | Hard cap for all configured values |
| `smartPlacementProtection` | `true` | Enable placed-block provenance gating |
| `sourceMode` | `NATURAL_ONLY` | Natural, all, or player-placed-only eligibility; see the matrix below |
| `filterMode` | `BLACKLIST` | Interpret filter sets as blacklist or whitelist |
| `playerMining` | `true` | Allow ordinary player mining to use configured multipliers |
| `multiplyExperience` | `false` | Independently multiply block XP |
| `experienceMultiplier` | `2` | XP multiplier when enabled |
| `explosions` | `true` | Allow explosion-created block loot to multiply |
| `automatedMining` | `false` | Allow supported non-player `Block.dropResources` paths |
| `protectBlockEntities` | `true` | Exclude blocks carrying block entities |
| `conservativePistonProtection` | `true` | Treat all piston destinations as protected |
| `statisticsEnabled` | `false` | Count evaluated and bonus drops in memory |

## Entity death-loot fields (schema 2)

Entity rules are independent from block source/provenance rules. Upgrading a 1.0.x file preserves every block setting and initializes the entity feature safely OFF.

| Field | Default | Meaning |
| --- | ---: | --- |
| `entityDropsEnabled` | `false` | Multiply qualifying final standard entity death-table loot |
| `inheritDefaultEntityMultiplier` | `true` | Use `globalMultiplier` when no category/entity override exists |
| `defaultEntityMultiplier` | `2` | Entity-domain fallback when inheritance is disabled |
| `entityKillRequirement` | `PLAYER_KILLS_ONLY` | Required vanilla kill attribution |
| `entityFilterMode` | `BLACKLIST` | Interpret independent entity exact/tag filter sets |
| `bossDropsEnabled` | `false` | Permit boss standard-table item multiplication; protected saddle/totem output remains single |
| `multiplyMobExperience` | `false` | Independently multiply qualifying death XP |
| `mobExperienceMultiplier` | `2` | XP factor; does not use the item multiplier |
| `multiplyBossExperience` | `false` | Additional opt-in required before boss XP can multiply |

Kill requirement values are:

- `PLAYER_KILLS_ONLY`: direct real-player damage, a player-owned projectile, or later environmental death while vanilla player credit remains valid.
- `PLAYER_OR_TAMED_ENTITY`: the above plus a tamed attacker whose already resolved root owner is a real player. An offline/unresolved owner stays unqualified without a blocking lookup.
- `ALL_STANDARD_DEATH_LOOT`: any otherwise eligible standard death-table output, including environmental deaths.

Entity rule maps and sets are separate from block rules:

```json
"entityCategoryMultipliers": {
  "golems": 1,
  "villagers_npcs": 1,
  "bosses": 1,
  "miscellaneous": 1
},
"entityMultipliers": {
  "minecraft:cow": 2,
  "example_mod:rare_beast": 1
},
"entityBlacklist": [],
"entityWhitelist": [],
"entityTagBlacklist": [],
"entityTagWhitelist": []
```

Entity values accept `0` through `maximumMultiplier`. Resolution priority is exact entity override, selected category override, default entity multiplier when inheritance is disabled, then global multiplier. Filters and kill/boss gates are evaluated before the number is applied. Entity filters accept entity-type tags such as `minecraft:undead` or `smart_resource_drops:categories/hostile`; optional leading `#` is normalized.

Valid entity category keys, in deterministic selection order, are:

```text
bosses
villagers_npcs
golems
neutral
passive
hostile
aquatic
ambient
miscellaneous
```

Every category is a datapack-extensible entity-type tag at `#smart_resource_drops:categories/<key>` with `replace: false`. Boss evidence wins. Otherwise the first explicit project-tag match in the order above wins, then class/`MobCategory` evidence. All matches remain visible in verbose entity inspection. A completely unknown modded living entity uses Miscellaneous; the default `1x` entry is the safe fallback.

Additional progression-sensitive standard-table items may be excluded from multiplication through the item tag `#smart_resource_drops:protected_entity_loot`. The shipped `replace: false` tag contains saddles and Totems of Undying; datapacks may add modded items without replacing those defaults.

Block rules are capped at 2,048 entries and entity rules have a separate 512-entry cap. A patch cannot use one domain to displace entries in the other. Entity snapshots and patches include only bounded validated IDs, tags, categories, and scalars; player UUID rules remain server-only.

`0x` suppresses only qualifying standard death-table stacks. It does not delete equipment, carried/inventory items, custom/direct output, saddles, or Totems of Undying. `1x` preserves vanilla quantity. Mob XP is never changed merely because entity item multiplication is enabled.

## Rule maps

Map values accept integers from `0` through `maximumMultiplier`.

```json
"blockMultipliers": {
  "minecraft:diamond_ore": 4,
  "minecraft:ancient_debris": 1
},
"categoryMultipliers": {
  "ores": 3,
  "logs": 2
},
"dimensionMultipliers": {
  "minecraft:the_nether": 2,
  "minecraft:the_end": 3
}
```

Removing a key restores inheritance. A `0` value suppresses the block's normal item loot. A `1` value keeps vanilla quantity.

Valid category keys:

```text
ores
raw_resource_blocks
logs
stone
soil
nether
end
crops
plants
leaves
building_blocks
miscellaneous
```

A block can participate in more than one category tag. The first configured matching category in the order shown above wins. An individual block override has higher priority and is the clearest way to resolve an overlap for one block.

## Presets

Applying a preset clears block, category, and dimension multiplier maps, then applies:

- `vanilla-plus`: global `1x`, ores `2x`, logs `2x`
- `faster-survival`: global `2x`, logs `3x`, with ores, stone, and crops explicitly at `2x`
- `fast-progression`: global `4x`

Presets do not clear filters, block-entity safety, source toggles, or player permissions. The GUI previews the preset's global and override-map changes before Apply; `custom` cancels that staged preset and restores those fields from the live snapshot.

## Filters

Blacklist mode multiplies every otherwise eligible block except exact IDs and tags in the blacklist.

Whitelist mode multiplies nothing unless an exact ID or matching tag is present in the whitelist.

The authoritative default blacklist includes technical/special blocks and `minecraft:dragon_egg`. Dragon Egg is not hard-coded in the rule engine: an administrator who deliberately removes that ordinary blacklist entry opts back into normal rule resolution for it.

```json
"blacklist": [
  "minecraft:bedrock",
  "minecraft:spawner"
],
"whitelist": [],
"tagBlacklist": [],
"tagWhitelist": []
```

JSON tag IDs may be written with or without leading `#` characters and are normalized internally without them. Tag-list commands accept an optional single `#`. Block and tag commands validate complete namespaced identifiers such as `minecraft:diamond_ore`, `c:ores`, and `some_mod:mineable_resource`.

## Block entities

```json
"protectBlockEntities": true,
"blockEntityAllowlist": []
```

Keeping the allowlist empty is the safest choice. An allowlisted container can multiply whatever its loot table emits, including component-bearing items.

## Player overrides

```json
"allowPlayerOverrides": false,
"maxPlayerMultiplier": 4,
"playerMultipliers": {}
```

Keys in `playerMultipliers` are UUID strings. A player's effective value is clamped to the lower of `maxPlayerMultiplier` and `maximumMultiplier`.

Set the server cap in-game with:

```text
/smartdrops admin player-maximum <1..64>
```

## Source modes and placement protection

`sourceMode` and `smartPlacementProtection` have deliberately distinct responsibilities:

| Source mode | Natural block | Player-placed, protection ON | Player-placed, protection OFF |
| --- | ---: | ---: | ---: |
| `NATURAL_ONLY` | eligible | excluded | eligible |
| `ALL` | eligible | eligible | eligible |
| `PLAYER_PLACED_ONLY` | excluded | eligible | eligible |

Disabling Smart Placement Protection relaxes only the placed-block exclusion used by `NATURAL_ONLY`. It never turns a natural block into an eligible `PLAYER_PLACED_ONLY` source. Provenance continues to be recorded and supplied to rule evaluation while protection is off, so either setting can be changed later without losing world history.

## Source toggles

```json
"explosions": true,
"automatedMining": false
```

Automation compatibility is intentionally opt-in. A machine must use a vanilla `Block.dropResources` path to be detected. A mod that bypasses Minecraft loot calculation and creates items directly cannot be multiplied safely by this mod.


## Client screen and server sync

`/smartdropsgui` and Mod Menu's optional Configure button share one route. When that route opens with no play connection or integrated server—normally through Mod Menu on the title screen—it edits the global local file as defaults for future singleplayer worlds. Once a play connection or integrated server exists, the route is exclusively server-authoritative: it requests a sanitized live snapshot and never falls back to the client file. Regular players receive a read-only view. Operators and the integrated-server owner can stage and apply:

- Global and XP multipliers
- Placement protection, source mode, and filter mode
- Per-block overrides searchable by namespaced ID or translated block name
- Category overrides
- Dimension overrides for the three vanilla dimensions, the current dimension, and existing configured dimensions
- Exact block blacklist and whitelist membership
- Operational server toggles for the mod, player mining, explosions, automation, block-entity protection, conservative piston protection, personal overrides, and statistics
- Entity Drops General, Categories, Entity Overrides, Filters, and Advanced screens, including the feature/default inheritance, kill policy, boss/XP gates, searchable registry catalog, exact/tag filters, and inherited/effective values
- Shearing Drops source toggles, default inheritance/multiplier, and search-first exact overrides for certified standard resources; the route remains available while Entity Drops is OFF and uses the same root-owned Apply, Discard, and confirmed Reset transaction
- The Vanilla+, Faster Survival, and Fast Progression presets

Block tag filters, the block-entity allowlist, maximum caps, and individual player UUID entries remain command/JSON settings. Entity exact and tag filters are editable in the Entity Drops screens. Player UUID values are never included in the client snapshot.

Snapshots carry request IDs and are tied to the active connection, so a late response from a closed screen or previous server is ignored. The server rate-limits snapshot requests to one immediate response per 40 ticks, keeps only the newest request during the cooldown, caches the sanitized JSON until configuration changes, and refuses snapshots larger than 1 MiB. Apply sends one delta capped at 256 KiB and containing only explicitly edited fields. The server validates shape and identifiers, checks permission when received and again when applied, and performs one atomic replacement. Success returns the complete authoritative snapshot; rejection or lost permission returns a compact result, with a fresh snapshot requested only when permission state must be refreshed. During the patch cooldown, only the newest already-authorized patch is queued until eligible; unauthorized payloads are never retained for a later permission change. Per-player UUID rules never enter the patch. Closing after Apply may suppress the reply screen but does not cancel an already-sent update; disconnect clears pending client work and pending server entries are discarded for disconnected players.

Block multiplier/filter/allowlist/player collections are limited to 2,048 validated entries; entity category/multiplier/filter collections have an independent 512-entry limit. Keys are no longer than 256 ASCII identifier characters. Before oversized hand-edited collections are sanitized to either bound, the complete original file is preserved as a timestamped `smart_resource_drops.oversized-*.json` backup. If the backup cannot be written, the active file is not replaced.

The final post-hardening real client GameTest passes in 38 seconds. It covers the title/local hierarchy, shared dirty-state navigation, root Apply/Discard, reset confirmation, integrated-server Apply/reset, dedicated non-operator/operator presentation, every entity field in snapshot copy/equality, and Entity Categories/overrides/filters through the shared session. A historical Mod Menu 20.0.0 run visibly completed the title-screen Mods-list Configure route, every major child dirty-state view, root Apply, and Reset Everything; the current hardening pass reran Mod Menu startup through full resource/audio/atlas initialization only. Connected Mod Menu navigation, failure/retry presentation, and a separately installed multiplayer session remain manual release checks.

## Read-only configuration validation

Operators and the server console can audit the currently loaded configuration with:

```text
/smartdrops validate
/smartdrops validate verbose
```

Validation is server-authoritative and read-only. It compares the current immutable configuration snapshot with the server's live block, entity-type, dimension, and tag registries, checking only IDs, tags, dimensions, filters, overrides, allowlists, rule budgets, risky combinations, and retained load diagnostics that are already configured. It does not scan worlds, chunks, loaded entities, inventories, loot tables, or datapack files; it does not execute loot, trigger `/reload`, save/normalize the file, change the revision, or remove entries.

Unknown configured IDs, tags, and dimensions are warnings and remain in the configuration. This allows temporarily absent mods or datapacks to return without losing administrator intent. Compact output contains at most 15 issues; verbose output contains at most 100. Load-time diagnostics retain bounded samples and backup basenames only. Player UUID overrides are counted for capacity diagnostics but never sampled, enumerated, or rendered.

The 2,048-entry block and 512-entry entity rule limits above are configuration-input budgets. They are distinct from the non-configurable runtime output limits: a block result above 262,144 multiplied items or 4,096 legal stacks falls back as one complete untouched original list at `1x`. Arithmetic is overflow-safe, statistics record the dedicated fallback without bonus/multiplied credit, and the server warning is limited to 256 block-ID/reason keys with five-minute suppression. `/smartdrops inspect verbose` reports these limits but remains read-only and never evaluates loot.

## Reloading

After manual edits:

```text
/smartdrops admin reload
```

Real command changes are saved immediately using a temporary file and atomic replacement when the filesystem supports it. The live config is published only after persistence succeeds; a failed write leaves it unchanged and the command reports failure. If a future-schema or unreadable file suppressed writes, resolve that file deliberately or use the explicit reset command before expecting saves.
