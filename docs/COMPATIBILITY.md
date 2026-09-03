# Smart Resource Multiplier compatibility notes

Smart Resource Multiplier `1.3.1+mc1.21.1` targets Minecraft 1.21.1 on Fabric and NeoForge. Both builds share rule resolvers, safety budgets, schema 3 configuration, commands, and GUI; loader-specific adapters handle lifecycle, networking, fake-player detection, and placed-block storage. Minecraft 26.2 remains the newest/default release on `main`.

Compatibility evidence is version- and loader-specific. This document does not claim compatibility with every mod, datapack, server stack, or newer Minecraft world.

## Shearing boundary on Minecraft 1.21.1

This backport multiplies vanilla Sheep output only. It recognizes a real server player inside `Player.interactOn` and the exact vanilla dispenser entity call inside `ShearsDispenseItemBehavior.tryShearLivingEntity`. Loader-recognized fake players, direct `Shearable.shear` calls, custom machines, and inference from `SoundSource.BLOCKS` remain unsupported vanilla `1x`.

Minecraft 1.21.1 has no generic final-output `LivingEntity.dropFromShearingLootTable` boundary. The implementation therefore wraps the final `Sheep.spawnAtLocation` calls and fails closed for every other entity type. Mooshroom, Snow Golem, and Bogged are special transformations and remain `1x`. Copper Golem does not exist on this target.

The `#smart_resource_drops:shearing/standard_resources` tag and existing schema 3 override keys are retained for configuration compatibility, but adding a custom type to that tag does not make it eligible on Minecraft 1.21.1. Validation reports such preserved overrides as inactive, and the GUI does not present an unsupported custom type as multiplied. This is intentionally stricter than later Minecraft targets.

Beehive honeycomb, leash removal, leaves, vines, cobwebs, and other blocks occur outside the wrapped Sheep output and remain governed by their normal block or vanilla rules.

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

Release evidence must come from target-native Minecraft 1.21.1 runs. The Fabric gates use a physical GUI smoke plus a separate real client/server authority test; NeoForge uses its loader-specific physical client and multiplayer gates. Historical Minecraft 26.2 or 1.21.11 results are not proof for this backport.

## Integration API stance

This version does not expose a supported public Java API. Packs and mods should use documented configuration, commands, category tags, and `#smart_resource_drops:protected_entity_loot`. The retained shearing tag is not a custom-shearable certification surface on Minecraft 1.21.1.
