# Smart Resource Multiplier compatibility notes

Smart Resource Multiplier `1.3.2` targets Minecraft 1.21.6-1.21.8 on Fabric and NeoForge. Both builds share rule resolvers, safety budgets, schema 3 configuration, commands, and GUI; loader-specific adapters handle lifecycle, networking, fake-player detection, and placed-block storage. Minecraft 26.2 remains the newest/default release on `main`.

Compatibility evidence is version- and loader-specific. This document does not claim compatibility with every mod, datapack, server stack, or newer Minecraft world.

## Shearing boundary on Minecraft 1.21.6-1.21.8

This backport recognizes a real server player inside `Player.interactOn` and the exact supported vanilla dispenser entity call. Loader-recognized fake players, unscoped direct shearing calls, custom machines, and inference from sound categories remain unsupported vanilla `1x`.

Minecraft 1.21.6-1.21.8 exposes `LivingEntity.dropFromShearingLootTable`. The implementation wraps only that standard helper's final item consumer while an eligible shearing action is active. Mooshroom, Snow Golem, and Bogged remain protected special transformations at vanilla output.

The `#smart_resource_drops:shearing/standard_resources` tag and schema 3 override keys remain the extension surface. A custom type must both opt into that tag and use the supported standard final-output helper; a tag alone cannot make direct or special output safe to multiply.

Beehive honeycomb, leash removal, leaves, vines, cobwebs, and other blocks occur outside the wrapped entity output and remain governed by their normal block or vanilla rules.

## Entity death-loot boundary

Entity multiplication applies only to non-player `LivingEntity` standard death-table output from Minecraft's death-specific loot consumer. It runs after loot-table calculation, so conditions, Looting, cooking, components, datapack tables, and supported loader-native final-loot modifiers remain authoritative. It does not reroll the table.

Players, armor stands, equipment, held or picked-up items, entity inventories/cargo, custom direct item spawns, gifts, breeding, trading, commands, shearing, fishing, and unrelated XP are outside this boundary. `doMobLoot=false`, empty tables, baby/no-drop conditions, cancellation, and another mod's final zero-result remain authoritative.

Protected progression output remains single even when boss multiplication is enabled. Datapacks may protect additional output through `#smart_resource_drops:protected_entity_loot`.

## Entity classification

Mods and datapacks may add entity types to the project-owned category tags for bosses, villagers/NPCs, golems, neutral, passive, hostile, aquatic, ambient, or miscellaneous. Explicit tag evidence wins over class and `MobCategory` fallbacks in the documented priority order. Unknown living entities safely fall back to Miscellaneous.

Category tags are server datapack state and reload with server resources. Run `/reload`, `/smartdrops validate verbose`, and `/smartdrops inspect entity verbose` before claiming compatibility for a specific mod or datapack pairing.

## Kill attribution and automation

`PLAYER_KILLS_ONLY` follows vanilla player-credit state, including player-owned projectiles and later environmental death while credit remains live. `PLAYER_OR_TAMED_ENTITY` accepts only an already resolved real-player owner. `ALL_STANDARD_DEATH_LOOT` admits environmental standard death-table loot. Loader-recognized fake players are never reclassified as real players.

Mob XP is scoped to the active death's `ExperienceOrb.award` call. Nearby or later XP stays unchanged, and boss XP has a separate gate.

## Placement and automation

- Piston and falling-block provenance is transferred rather than recreated.
- Loader-recognized fake players follow `automatedMining`, not ordinary player-mining or personal-override policy.
- Claim or spawn-protection cancellations must not consume provenance.
- Blocks created by commands, structure tools, or world-editing APIs may look natural unless the integration explicitly marks them artificial.
- Automation must reach the supported vanilla block-drop boundary. Directly manufactured inventory output is untouched.

Fabric and NeoForge use loader-specific persistence formats. Configuration schema compatibility does not prove world-data portability. Minecraft world downgrades and cross-loader placed-block-data migration are unsupported on this backport; always back up a world before changing versions or loaders.

## Output budgets

Before applying a block multiplier above `1x`, the mod preflights the complete final loot list. If the estimate exceeds 262,144 items or 4,096 legal stacks, or arithmetic saturates, it returns the complete original output at vanilla `1x`. It never partially multiplies or truncates the list.

## Configuration diagnostics

`/smartdrops validate` and `/smartdrops validate verbose` inspect configured IDs, dimensions, tags, inactive shearing rules, and conflicting combinations against live registries. Unknown references are reported but retained so temporarily absent content does not erase an administrator's configuration. Validation is advisory and read-only.

## Mod Menu and multiplayer authority

Mod Menu is an optional Fabric client integration and is not required on dedicated servers. With no active connection it opens the local editor. With a connection it requests the authoritative server snapshot and never substitutes local defaults. Non-operators receive the connected editor read-only; operators and the integrated-server owner may apply validated changes.

Release evidence must come from target-native Minecraft 1.21.6-1.21.8 runs. The Fabric gates use a physical GUI smoke plus a separate real client/server authority test; NeoForge uses its loader-specific physical client and multiplayer gates. Historical Minecraft 26.2, 1.21.11, or 1.21.1 results are not proof for this backport.

## Integration API stance

This version does not expose a supported public Java API. Packs and mods should use documented configuration, commands, category tags, and `#smart_resource_drops:protected_entity_loot`. The shearing tag is an opt-in signal, not a guarantee: a custom entity must also use Minecraft's supported standard final-output helper.
