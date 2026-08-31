# Smart Resource Multiplier command reference

This reference applies to **Smart Resource Multiplier 1.2.2**. The public name changed, but the command entry points remain legacy-compatible: server commands use `/smartdrops`, and the client configuration screen uses `/smartdropsgui`.

Administrative configuration mutations are server-authoritative. They are validated, persisted, and published as one configuration update; if persistence fails, the live configuration is not changed. For the JSON field reference and defaults, see [CONFIGURATION.md](CONFIGURATION.md).

## Entry points and permissions

| Entry point | Who can use it | Purpose |
| --- | --- | --- |
| `/smartdrops` | Players and console | Show a compact status line |
| `/smartdropsgui` | Client players with Smart Resource Multiplier installed | Open the configuration screen |
| `/smartdrops gui` | Players and console | Print a reminder to use `/smartdropsgui`; it does not open a screen |
| `/smartdrops status` | Players and console | Show the same compact status as `/smartdrops` |
| `/smartdrops shearing [status]` | Players and console | Show the shearing-specific status |
| `/smartdrops inspect ...` | Players | Inspect the block or non-player living entity currently targeted |
| `/smartdrops validate [verbose]` | Game-master permission or server console | Audit configured references against live registries and tags |
| `/smartdrops personal ...` | Players | Set or clear the invoking player's block multiplier override |
| `/smartdrops admin ...` | Game-master permission or server console | Change authoritative configuration, apply presets, reload/reset, or manage statistics |

`/smartdropsgui` is a client-side command with no arguments or aliases. It opens the same screen route used by Mod Menu. When that route is opened without a play connection--normally through Mod Menu on the title screen--it edits the local configuration used as defaults for future local or singleplayer servers. While connected, it requests the server's authoritative configuration. Game masters may apply validated changes; the owner of an integrated singleplayer server is also authorized even when not currently listed as an operator. Other non-operators receive the complete interface read-only. A dedicated server does not need the client command or Mod Menu.

In this document, **administrator** means a command source with Minecraft's game-master command permission. The server console satisfies that requirement. Commands that need a looked-at target or a player UUID still require an actual player even when the console is otherwise authorized.

## Read-only status, inspection, and validation

### General status

```text
/smartdrops
/smartdrops status
```

Both forms report the master switch, global multiplier, placement protection, source and filter modes, block XP, explosions, supported block automation, entity-death settings, and the manual/automated/default shearing state. They do not modify configuration.

```text
/smartdrops shearing
/smartdrops shearing status
```

Both shearing forms report the master switch, manual and vanilla-dispenser gates, configured or inherited default, exact-override count, fixed `1x` treatment for unknown/special types, and the always-on 1,024-item/256-source-or-materialized-stack output budget.

### Block inspection

Look at a block within normal interaction range and run:

```text
/smartdrops inspect
/smartdrops inspect verbose
```

Inspection is server-authoritative and player-only. The server performs the raycast and evaluates the same immutable rule trace used for a player block drop. Compact output summarizes the decision; verbose output includes category matches, filters, provenance, block-entity protection, source gates, individual/category/dimension/global candidates, the personal override where relevant, and the effective result.

Inspection does not break or change the block, evaluate its loot table, spawn loot or XP, change configuration or statistics, or consume placement/recent-removal provenance. The target must remain loaded and unchanged while the trace is produced.

### Entity inspection

Look at a non-player living entity within normal entity-interaction range and run:

```text
/smartdrops inspect entity
/smartdrops inspect entity verbose
```

The command reports the hypothetical entity death-loot and mob-XP multiplier decisions under direct-player attribution. When the target implements `Shearable`, has a standard/special shearing tag, or has a configured exact override, it also reports separate manual-player and vanilla-dispenser shearing decisions. It is read-only: it does not damage, move, kill, or shear the entity; call `readyForShearing`; evaluate loot; damage a tool; alter combat credit; or expose inventory, equipment, NBT, or private player data. Players, spectators' targets, removed entities, and targets that move out of range are rejected.

### Configuration validation

```text
/smartdrops validate
/smartdrops validate verbose
```

Validation requires administrator permission even though it is read-only. It compares the current immutable configuration with the server's live block, entity-type, dimension, and tag registries. It checks configured references and bounded rule domains only; it does not scan worlds, chunks, loaded entities, inventories, loot tables, or datapack files, and it does not trigger `/reload`.

Unknown IDs, tags, and dimensions are reported but preserved so temporarily missing mods or datapacks do not erase administrator intent. Compact output shows at most 15 issues and verbose output at most 100. Player UUID override values are counted for capacity diagnostics but are never listed. Neither form saves, normalizes, or changes the configuration revision.

## Personal block override

```text
/smartdrops personal <0..64>
/smartdrops personal inherit
```

These commands are player-only. A non-administrator can set a value only when the server has enabled personal overrides; an administrator player may set one while the feature is disabled for ordinary players. The requested value is capped by `maxPlayerMultiplier` and the server's overall maximum before storage.

The personal value is the highest-priority numeric rule for otherwise eligible **block** drops. It does not bypass the master switch, source/filter checks, placement protection, or block-entity safety, and it does not configure entity-death or shearing output. `inherit` removes the player's stored UUID rule and returns block resolution to the server hierarchy.

Turning personal overrides off makes stored values inactive but does not erase them. `inherit` remains available to remove the invoking player's value even while new ordinary-player overrides are disabled.

Administrators control access and the cap with:

```text
/smartdrops admin player-overrides <on|off>
/smartdrops admin player-maximum <1..64>
```

## Administrative toggles and scalar settings

Every command in this section requires administrator permission.

### Toggles

Each toggle accepts exactly `on` or `off`:

| Command | Effect |
| --- | --- |
| `/smartdrops admin enabled <on|off>` | Master switch for Smart Resource Multiplier gameplay |
| `/smartdrops admin protection <on|off>` | Smart placement-provenance protection for block rules |
| `/smartdrops admin xp <on|off>` | Independent block-experience multiplication gate |
| `/smartdrops admin player-mining <on|off>` | Allow ordinary player block mining to use configured block multipliers |
| `/smartdrops admin explosions <on|off>` | Allow qualifying explosion-created block loot to multiply |
| `/smartdrops admin automation <on|off>` | Allow supported non-player `Block.dropResources` paths; this is not the dispenser-shearing gate |
| `/smartdrops admin blockentities <on|off>` | Protect blocks carrying block entities; `on` means protected unless allowlisted |
| `/smartdrops admin piston-safe <on|off>` | Conservatively protect piston destinations |
| `/smartdrops admin player-overrides <on|off>` | Permit ordinary players to set personal block overrides |
| `/smartdrops admin stats <on|off>` | Enable or disable collection of in-memory block statistics |

Turning statistics off does not clear counters already collected. Use `/smartdrops admin statistics reset` when the accumulated snapshot should be cleared.

### Numeric settings

```text
/smartdrops admin global <0..64>
/smartdrops admin maximum <1..64>
/smartdrops admin xp-multiplier <1..64>
/smartdrops admin player-maximum <1..64>
```

- `global` is the final block-rule fallback. It is also used by the entity-death and shearing defaults when those domains are configured to inherit Global.
- `maximum` is the authoritative cap for configured multipliers. Lowering it clamps dependent scalar and rule values during sanitization.
- `xp-multiplier` controls block XP only and is applied only while `/smartdrops admin xp on`.
- `player-maximum` caps stored personal block overrides and cannot exceed the overall maximum after sanitization.

The command parser accepts the absolute ranges shown above. The stored result may be lower when the current overall maximum or personal cap applies; the success message reports the stored value.

### Block source mode

```text
/smartdrops admin source natural-only
/smartdrops admin source all
/smartdrops admin source player-placed-only
```

| Source mode | Natural block | Known player-placed block |
| --- | --- | --- |
| `natural-only` | Eligible | Forced to vanilla quantity while placement protection is on; eligible when it is off |
| `all` | Eligible | Eligible |
| `player-placed-only` | Ineligible | Eligible, regardless of the placement-protection toggle |

Source mode does not bypass the master switch, filter policy, block-entity safety, or the independent player/explosion/automation source gates. Placement provenance continues to be recorded while protection is off. See [ANTI_DUPE.md](ANTI_DUPE.md) for piston, falling-block, transformation, and persistence behavior.

There are no `/smartdrops admin entity ...` mutation branches in 1.2.0. Entity-death settings are edited through the Entity Drops configuration screens or `config/smart_resource_drops.json`; `/smartdrops inspect entity` and `/smartdrops validate` remain the command-line diagnostics for that domain.

## Multiplier rules, hierarchy, and `inherit`

```text
/smartdrops admin block <namespace:block_id> <0..64|inherit>
/smartdrops admin category <category> <0..64|inherit>
/smartdrops admin dimension <namespace:dimension_id> <0..64|inherit>
```

After all eligibility and safety gates pass, block multipliers resolve in this order:

```text
personal override > block > category > dimension > global
```

The first configured rule wins. The optional personal override is applied only when enabled for the player; all values are bounded by the configured maxima.

- `0x` suppresses qualifying normal item loot from the multiplied block path.
- `1x` preserves Minecraft's calculated quantity.
- Values above `1x` multiply the already-calculated final loot and split it into legal stack sizes only within the 262,144-item/4,096-legal-stack block budget; an over-budget result falls back wholly to the original vanilla `1x` output.
- `inherit` removes the specified map entry. It does not store a special sentinel value.
- A block without an exact rule continues to category, then dimension, then global.
- A category without a rule continues to dimension and global; a dimension without a rule continues to global.

Valid canonical category keys are:

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

Category input is a single command word and is case-normalized; hyphens normalize to underscores. Use the canonical underscore keys above for clarity. Block and dimension arguments must be syntactically valid namespaced identifiers. Unknown but valid block or dimension IDs can be retained for a temporarily absent mod; use `/smartdrops validate` to audit them.

Block maps, filter lists, the block-entity allowlist, and personal UUID rules share a bounded 2,048-entry block domain. Rule keys are limited to 256 characters. An update that cannot fit is rejected rather than silently displacing another rule.

## Filter lists and block-entity allowlist

Select the active block filter policy with:

```text
/smartdrops admin filter-mode blacklist
/smartdrops admin filter-mode whitelist
```

In blacklist mode, otherwise eligible blocks are allowed unless an exact block ID or one of the block's tags is listed. In whitelist mode, a block must match an exact ID or tag in the corresponding whitelist.

Manage exact block IDs:

```text
/smartdrops admin blacklist <add|remove> <namespace:block_id>
/smartdrops admin whitelist <add|remove> <namespace:block_id>
```

Manage block-tag IDs:

```text
/smartdrops admin tag-blacklist <add|remove> <namespace:tag_id>
/smartdrops admin tag-whitelist <add|remove> <namespace:tag_id>
```

Tag commands accept the ID with or without one leading `#`; the stored form omits it. Exact and tag entries are lowercased and syntax-validated, but they do not have to exist in the current registry. This preserves rules across temporary mod or datapack removal.

Adding an entry already present and removing an entry already absent are harmless successful no-ops. A full rule domain rejects an addition instead of evicting another configured entry.

Block entities are protected independently of the filter mode. To deliberately permit one exact block type while block-entity protection is enabled:

```text
/smartdrops admin blockentity-allowlist add <namespace:block_id>
/smartdrops admin blockentity-allowlist remove <namespace:block_id>
```

Allowlisting a container or another data-bearing block accepts responsibility for whatever its loot table emits, including component-bearing contents. Keeping the allowlist empty is the safest policy.

## Shearing commands

Read-only status is available to players and the console:

```text
/smartdrops shearing
/smartdrops shearing status
```

Administrator mutations are:

```text
/smartdrops admin shearing manual <on|off>
/smartdrops admin shearing automated <on|off>
/smartdrops admin shearing multiplier <inherit|0..64>
/smartdrops admin shearing entity <namespace:entity_id> <inherit|0..64>
```

- `manual` controls supported real-player entity interactions.
- `automated` controls only the exact vanilla dispenser entity-shearing branch. It is independent of `/smartdrops admin automation`, which controls supported block-drop automation.
- A numeric default disables Global inheritance and stores that default. `inherit` restores Global inheritance.
- An exact entity rule has priority over the shearing default, but cannot certify an entity as safe.

Shearing resolution is fail-closed:

```text
known/tagged special safety > unknown/uncertified safety > master disabled > selected source disabled > exact entity > shearing default > global
```

Adding an exact rule requires the entity type to exist in the live registry, be present in `#smart_resource_drops:shearing/standard_resources`, and not be a known or tagged special transformation. Unknown and special types remain fixed at vanilla `1x`. Removing a rule with `inherit` is allowed even when the entity is now missing, uncertified, or special; a currently certified standard type then returns to the default shearing rule.

For eligible standard-helper output within the whole-action safety budget, `0x` suppresses the helper-produced items while the real shearing state transition, event/sound path, and tool behavior still happen once. An action exceeding the source-entry/materialized-legal-stack preflight falls back wholly to its original vanilla `1x` output even when `0x` is configured. Smart Resource Multiplier never reruns the shearing loot table or repeats the shear action. See [COMPATIBILITY.md](COMPATIBILITY.md#certifying-a-compatible-modded-shearable) before certifying a modded entity.

## Presets

```text
/smartdrops admin preset vanilla-plus
/smartdrops admin preset faster-survival
/smartdrops admin preset fast-progression
```

Command presets apply immediately; unlike the GUI preset route, they do not open a preview.

| Preset | Applied block multiplier rules |
| --- | --- |
| `vanilla-plus` | Global `1x`, Ores `2x`, Logs `2x` |
| `faster-survival` | Global `2x`, Logs `3x`, Ores `2x`, Stone `2x`, Crops `2x` |
| `fast-progression` | Global `4x`, no category overrides |

Applying a preset clears the block, category, and dimension multiplier maps before adding the values above. It does not clear filters, source toggles, block-entity safety, player permissions, entity-death settings, or shearing settings. Values remain subject to the current overall maximum.

## Maintenance and statistics

### Reload configuration

```text
/smartdrops admin reload
```

Reloads `config/smart_resource_drops.json` through the safe configuration loader. A recoverably malformed file is backed up, replaced with persisted safe defaults, and may complete successfully with diagnostics. A read error makes the command fail and leaves the existing live configuration unchanged. An unsupported future-schema file is left untouched, publishes safe in-memory state, and also reports failure. Failed reloads direct the administrator to the server log. This is not a datapack resource reload; use Minecraft's `/reload` separately after changing datapack tags.

### Reset configuration

```text
/smartdrops admin reset
```

Resets configuration to the current safe defaults, persists it, and invalidates connected editor state so stale drafts cannot overwrite the reset. This does **not** erase chunk placement provenance, world data, entity state, tools, or accumulated statistics. Use the separate statistics reset when desired.

The command executes immediately and has no confirmation prompt. The GUI's Reset flow has a separate confirmation screen.

### Statistics

```text
/smartdrops admin stats <on|off>
/smartdrops admin statistics show
/smartdrops admin statistics reset
```

`stats` controls future collection. `statistics show` reports evaluated and multiplied blocks, original and bonus item totals, suppressed items, bonus block XP, and whole-block output-budget fallbacks. `statistics reset` clears the in-memory counters but does not change whether collection is enabled.

## Examples

Open the client configuration screen and check the server summary:

```text
/smartdropsgui
/smartdrops status
```

Use a `3x` global block multiplier, then give diamond ores a `4x` exact rule:

```text
/smartdrops admin global 3
/smartdrops admin block minecraft:diamond_ore 4
/smartdrops admin block minecraft:deepslate_diamond_ore 4
```

Remove the exact diamond-ore rule so it inherits category, dimension, or global behavior again:

```text
/smartdrops admin block minecraft:diamond_ore inherit
```

Switch to a tag-based whitelist:

```text
/smartdrops admin filter-mode whitelist
/smartdrops admin tag-whitelist add #c:ores
```

Allow ordinary players to request a personal block multiplier up to `4x`:

```text
/smartdrops admin player-maximum 4
/smartdrops admin player-overrides on
/smartdrops personal 3
```

Enable certified manual shearing with an inherited Global default, while leaving dispenser shearing off:

```text
/smartdrops admin shearing manual on
/smartdrops admin shearing automated off
/smartdrops admin shearing multiplier inherit
/smartdrops shearing status
```

After changing mods, datapacks, or hand-edited configuration, reload the appropriate resource and run a verbose audit:

```text
/reload
/smartdrops admin reload
/smartdrops validate verbose
```
