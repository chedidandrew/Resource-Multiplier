# Resource Multiplier compatibility notes

Resource Multiplier 1.2.0 passes its automated synthetic-testmod, dedicated/client GameTest, clean-build, package-validator, and playable-JAR gates. All named third-party manual cases remain pending. This evidence documents the generic boundary only; it is not a claim that every mod, datapack, or server stack is compatible.

## Resource Multiplier 1.2.0 supported shearing boundary

The 1.2.0 candidate recognizes two sources only: a real server player inside `Player.interactOn`, and the exact vanilla dispenser entity call inside `ShearsDispenseItemBehavior.tryShearEntity`. The outer player scope deliberately does not require `Items.SHEARS`, so a compatible custom tool can work if it follows the standard entity interaction and final shearing-helper path. Fabric fake players, direct `Shearable.shear` calls, custom machine output, and inference from `SoundSource.BLOCKS` remain unsupported vanilla `1x`.

Eligible output must pass through `LivingEntity.dropFromShearingLootTable`. That helper runs once and its final stacks are buffered; Resource Multiplier never reruns the table or repeats the state transition. Per-call consumers are retained, preserving sheep velocity, entity-specific positions, and compatible mod placement behavior. Direct `spawnAtLocation`, `ItemEntity`, inventory, and equipment ejection are not intercepted.

Production certification includes sheep only. Mooshroom, Snow Golem, Bogged, Copper Golem, and Sulfur Cube are special and fixed at vanilla `1x`; the hardcoded audited safety set prevents a datapack `replace` or conflicting standard tag from bypassing that rule. Unknown modded shearables fail closed until a pack deliberately adds the type to `#smart_resource_drops:shearing/standard_resources`. Certification means only that standard-helper output is eligible—it does not promise that a modded entity actually calls the helper.

Vanilla dispenser beehive honeycomb and leash removal occur outside the wrapped entity call. Leaves, vines, cobwebs, and other blocks remain governed only by block-drop rules. A custom machine that delegates to the vanilla dispenser behavior may work; a machine that bypasses it remains `1x` until a concrete compatibility integration is designed.

### Certifying a compatible modded shearable

First verify that the modded entity implements the normal shearing interaction and sends its final stacks through `LivingEntity.dropFromShearingLootTable`. Certification cannot adapt an entity that spawns items directly, inserts them into an inventory, or bypasses the supported player/vanilla-dispenser source scopes.

In a datapack, create `data/smart_resource_drops/tags/entity_type/shearing/standard_resources.json` with the entity types that have been audited. For example:

```json
{
  "replace": false,
  "values": [
    "wildlife:highland_sheep"
  ]
}
```

Enable the pack and run `/reload`. Then run `/smartdrops validate verbose` and target the entity with `/smartdrops inspect entity verbose`; inspection must report the current standard-resource classification and the expected manual/dispenser resolution before an operator enables or raises a multiplier. Removing the entry and reloading must return the type to unknown vanilla `1x`. Membership in the special tag or audited known-special set always wins over this file, and an exact configuration override alone never certifies a type.

## Expected compatibility

The multiplier operates on Minecraft's calculated loot output so Fortune, Silk Touch, custom loot tables and item components remain authoritative. Server configuration controls multiplayer behavior.

For the 1.1 entity path, compatibility means final **standard death-table** output only. Minecraft loot conditions, Looting, cooking, components, datapack tables, and Fabric final-drop modification run before multiplication. The mod does not reroll the table or intercept broad `spawnAtLocation`/`ItemEntity` creation.

## Entity death-loot boundary

- Supported: non-player `LivingEntity` standard death-table stacks emitted by Minecraft's death-specific loot consumer.
- Always outside the multiplier: players, armor stands, equipment, held/picked-up items, entity inventories/cargo, custom death output, direct item spawns, gifts, shearing, fishing, breeding, trading, commands, advancement/score/stat rewards, and unrelated XP.
- `doMobLoot=false`, an empty table, a baby/no-drop condition, cancellation, and another mod's final zero-result remain authoritative. Resource Multiplier does not recreate output.
- Ravager's Minecraft 26.2 standard table contains a saddle and Evoker's contains a Totem of Undying. Both items are protected at final-output time and remain single even when boss item multiplication is deliberately enabled. Datapacks may protect additional progression output through `#smart_resource_drops:protected_entity_loot`; the shipped tag uses `replace: false`.
- Wither/Dragon and other special drops must be tested individually. Output produced through subclass custom-death code remains outside the standard-table hook by design.
- A mod that replaces `dropAllDeathLoot`, bypasses the supported death-table call, consumes loot into an inventory, or creates items later receives vanilla behavior unless it offers a targeted compatibility contract. This fail-closed result is preferred to multiplying ambiguous inventory/equipment output.

## Modded entity classification

Mods and datapacks may add entity types to `#smart_resource_drops:categories/bosses`, `villagers_npcs`, `golems`, `neutral`, `passive`, `hostile`, `aquatic`, `ambient`, or `miscellaneous`. Every shipped file uses `replace: false`. Explicit project-tag evidence wins over class/`MobCategory` fallbacks in the documented category order; every match and source remains visible in verbose entity inspection. A modded `Animal` and `Monster` receive Passive and Hostile fallbacks respectively, while an unknown `LivingEntity` safely becomes Miscellaneous at `1x` by default.

Category tags are server datapack state and reload with server resources. A real `/reload` test that changes a custom category assignment is still required before release. Servers should prefer explicit project tags for entities whose inheritance or `MobCategory` does not express gameplay intent, especially modded bosses or multi-role entities.

The built-in development testmod has dedicated untagged `Animal`, `Monster`, `NeutralMob`, and `WaterAnimal` probes so class fallback is not accidentally masked by project tags. It also covers project-tag override, multi-category priority, unknown fallback, boss safety, final Fabric loot modification, mapped Looting, nested callbacks, real mob pickup, and equipment/inventory exclusion. The final serialized dedicated run passes all 22 required GameTests, but none of this proves compatibility with every third-party mod. Release evidence must name the exact mod and version used for wildlife, hostile, boss, and equipment/inventory cases.

## Configuration diagnostics

Operators and the server console can run `/smartdrops validate` or `/smartdrops validate verbose` before and after changing a mod/datapack set. Validation uses the current immutable configuration plus the server's live block, entity-type, dimension, and tag registries. It checks only configured references and combinations; it does not scan worlds, chunks, loaded entities, loot tables, or datapack contents and does not reload resources. Compact output shows at most 15 issues and verbose output at most 100.

An unknown configured ID, tag, or dimension is reported but retained. That is intentional compatibility behavior: a temporarily absent mod/datapack must not erase an administrator's rule before the content returns. Load-time diagnostics are bounded; UUID player overrides are counted but never sampled or printed, and backup information is limited to basenames. Validation is advisory and read-only—it never edits, saves, normalizes, or increments the configuration revision.

## Kill attribution and automation

`PLAYER_KILLS_ONLY` follows vanilla player-credit state, including player-owned projectiles and later environmental death while credit remains live. `PLAYER_OR_TAMED_ENTITY` accepts only an already resolved real-player owner; offline/unresolved owners do not trigger blocking profile/network lookup. `ALL_STANDARD_DEATH_LOOT` deliberately admits environmental standard loot. Fabric fake players are never reclassified as real players.

Mob XP is scoped to the active death's `ExperienceOrb.award` call. Nearby or later XP remains unchanged. Boss XP has a separate gate. Mods that replace the XP path or award XP outside it remain vanilla.

## Special integrations

- Piston and falling-block provenance must be transferred rather than recreated.
- Remove-before-drop machines are covered for a short bounded window.
- Fabric fake players are classified as automation and follow `automatedMining`, not ordinary player-mining or personal-override policy.
- Claim or spawn-protection cancellations must not consume provenance.
- Blocks created directly with commands, structure tools or world-editing APIs may be considered world-generated unless the integration explicitly marks them artificial.
- Custom block items that bypass `BlockItem.place` require an explicit provenance integration.
- Automation must reach a vanilla `Block.dropResources` path. Directly manufactured item output is intentionally untouched.
- Nested loot is target-guarded by level, position and state, and correlation scopes close in exception-safe wrappers. Mods that replace the wrapped vanilla methods still need targeted compatibility testing.

## Block output budget and failure behavior

Resource Multiplier preflights each block's complete final loot list before applying a multiplier above `1x`. If the estimate exceeds 262,144 items or 4,096 legal stacks, or arithmetic saturates because another mod supplied pathological counts, the complete original list is returned untouched at vanilla `1x`. The mod never partially multiplies, truncates, or reconstructs that list. This protects player, explosion, and supported automation paths at the same boundary while preserving another mod's already-produced output.

With statistics enabled, the fallback counts the evaluation and original vanilla items, increments `blockBudgetFallbacks`, and records no multiplied block or bonus items. A bounded warning identifies the block, dimension, position, multiplier, estimates, limits, and fallback; repeated block-ID/reason pairs are suppressed for five minutes in a cache capped at 256 keys. A mod that manufactures output outside the supported final block-loot boundary remains untouched rather than being forced through this budget.

## Compatibility evidence format

Every manual compatibility result must record the mod/project name, exact tested version, calendar test date, exact Resource Multiplier version/artifact, result, and known limitation. Do not write “compatible with modded mobs” or another blanket claim from a synthetic fixture or one project. At minimum, release evidence needs separate rows for biome/wildlife or passive entities, hostile entities, bosses, equipment/inventory behavior, custom shearables, automated miners, and custom placement/drop integrations.

| Category | Mod/project | Exact version | Test date | Resource Multiplier version | Result | Known limitation |
| --- | --- | --- | --- | --- | --- | --- |
| Biome/wildlife or passive | Pending selection | Not tested | Not tested | 1.2.0 | Pending | Manual gate not performed |
| Hostile entity | Pending selection | Not tested | Not tested | 1.2.0 | Pending | Manual gate not performed |
| Boss | Pending selection | Not tested | Not tested | 1.2.0 | Pending | Manual gate not performed |
| Equipment/inventory | Pending selection | Not tested | Not tested | 1.2.0 | Pending | Manual gate not performed |
| Custom shearable | Pending selection | Not tested | Not tested | 1.2.0 | Pending | Manual gate not performed |
| Automated miner | Pending selection | Not tested | Not tested | 1.2.0 | Pending | Manual gate not performed |
| Custom placement/drop | Pending selection | Not tested | Not tested | 1.2.0 | Pending | Manual gate not performed |

## Integration API stance

Neither version 1.1.x nor the current 1.2.0 shearing candidate exposes a supported public Java API. Other mods and packs should use documented configuration and commands, project-owned category tags, `#smart_resource_drops:protected_entity_loot`, and the audited `#smart_resource_drops:shearing/standard_resources` certification surface rather than bind to public-looking implementation classes. A formal API is deferred until a concrete reproducible third-party case cannot be solved safely through those surfaces and supplies lifecycle, failure, versioning, documentation, and cross-mod test requirements.

## Safe defaults

Block entities and data-bearing containers are excluded by default. Automation multiplication and XP multiplication are separate opt-ins.

## Mod Menu

Resource Multiplier integrates with Mod Menu through an optional client entrypoint. Mod Menu is not declared as a required dependency and is not needed on dedicated servers.

With Mod Menu installed, the Mods screen exposes a `Configure` button for Resource Multiplier. With no active world or integrated server, it opens the complete editor against the global local file; those values are defaults for future singleplayer worlds and never modify a remote server. With a play connection, the same route requests the authoritative server snapshot and never falls back to local values.

If a connected server does not support Resource Multiplier config sync, the loading screen reports that state and allows the player to return safely. Regular players receive the full connected interface read-only; operators and the integrated-server owner can apply validated changes.

The final hardening pass reconfirms compile-only dependency and dedicated-server classpath isolation: standalone server startup without Mod Menu reaches `Done (0.313s)`, executes validation, and stops cleanly; the client GameTest passes in 38 seconds across the shared title/local, cached connected, integrated-server, dedicated non-operator, and dedicated operator routes. A historical Mod Menu 20.0.0 run visibly completed the title-screen Mods-list Configure route and every major child dirty-state view, while the current standalone Mod Menu client run is startup smoke only. Connected Mod Menu navigation and a separately installed multiplayer session remain pending.
