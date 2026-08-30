# Edge-case behavior

Resource Multiplier multiplies the loot result produced by Minecraft, not the source block item itself. This keeps Fortune, Silk Touch, modded loot tables and explosion decay authoritative.

## Provenance rules

- A successful BlockItem placement records every position created in that placement transaction.
- Canceled or failed placements do not record positions.
- Property-only state changes retain provenance except for known generated growth. Explicit same-resource block transformations include soil changes, stripped logs/wood, copper weathering/waxing, concrete powder to matching concrete, coral variants, and cauldron variants.
- Unrelated replacement clears provenance. Crop, nether-wart, cocoa, berry-bush, cave-vine, and kelp growth is treated as generated so provenance does not spread permanently.
- Piston and falling-block transfers retain provenance through their existing transfer hooks.
- A bounded two-tick removal cache covers machines that clear a marked block immediately before requesting loot. It holds at most 4,096 entries per level, clears on clock rollback, and temporarily fails closed after overflow.
- Naturally generated, grown or regenerated positions remain natural unless a configured source policy says otherwise.
- Commands and world-editing tools can create blocks without a placement transaction. Server operators should treat those as administrative world generation or explicitly blacklist them.

## Loot safety

- `0x` must return no loot from the multiplied path.
- `1x` must preserve vanilla behavior without bonus entities.
- Values above `1x` duplicate the already-calculated result.
- Identical item-and-component results are consolidated, then output is split at each item's legal maximum stack size.
- Block entities and data-bearing containers remain protected by default.
- Creative and spectator behavior must remain vanilla unless a server deliberately changes it.
- Each drop session can be claimed only by its exact level, position, and original state. Nested loot for another block cannot borrow the outer multiplier; XP must also match the exact level and position.
- Fabric fake players are automation, not ordinary players, and therefore require `automatedMining`.
- Automation that bypasses vanilla `Block.dropResources` and creates items itself is outside the safe multiplication boundary.
- Entity standard-loot ownership is claimed only when the first real stack is emitted, so an empty preliminary table cannot consume the death's exactly-once guard. Nested duplicate wrappers multiply one time. Mod-created entity output is cumulatively bounded to 262,144 items and 4,096 stacks per death; once exceeded, later standard-table callbacks remain vanilla.
- Mob XP uses a one-shot identity token for the exact `ExperienceOrb.award` call. A nested unrelated award cannot inherit the mob rule, and an amplified result above 634,112 remains at its original amount.
- Entity shearing is eligible only for an identity-matched real-player or exact vanilla-dispenser scope, current standard-resource tag membership, absence from special safety, and final output through `dropFromShearingLootTable`. An override alone cannot certify an unknown entity.
- A `0x` eligible shear still performs the state transition and tool path once while suppressing standard helper loot. Special and unknown shearables stay vanilla `1x` regardless of configured defaults.
- Multiple helper calls share one cumulative 1,024-item/256-stack-group preflight. Overflow emits the complete original action output through each original consumer; an exception after collection attempts original output once, clears context, and rethrows the original failure.
- Mooshroom, Snow Golem, Bogged, Copper Golem, and Sulfur Cube are hard special cases. Direct equipment ejection, beehives, leash removal, and block shearing are outside the entity-shearing scope.

## Multiplayer

- The server owns gameplay configuration.
- With no active connection, the title-screen editor changes only local defaults for future integrated worlds; it cannot modify a remote server.
- Client GUI writes are one bounded dirty-field delta, permission-checked and atomically persisted by the server.
- Non-operators must not be able to mutate server settings.
- Configuration values are clamped and identifiers are validated on the server.
- Snapshot replies carry request IDs and connection identity. Late replies from a closed screen or previous server are ignored.
- Snapshot requests have a 40-tick per-player cooldown, keep only the newest pending request, and have a 1 MiB JSON limit. Rejected or unauthorized mutations receive compact explicit results rather than a reflected full snapshot.
- During a patch cooldown, only the newest update received from an authorized player is queued. Permission is checked again before application; unauthorized payloads are never retained for later promotion.
- Patch acknowledgements distinguish applied, rejected, and unauthorized updates; disconnect clears pending client request/screen state and disconnected server entries are discarded.

## Category overlaps

Blocks can match more than one category tag. Category resolution follows the documented category enum order and uses the first matching category that has an override. Exact block overrides still win and should be used when an overlap needs a specific answer.

## Compatibility boundaries

- A custom item that bypasses `BlockItem.place` does not automatically enter the placement transaction.
- Commands, structures, and world-edit tools are treated as generated unless an integration marks their output.
- Placement, loot, explosion, level-change, and falling-block scopes use exception-safe wrappers. A third-party mod that replaces a wrapped method still needs targeted compatibility testing.
- Hand-edited rule data is syntax-validated and limited independently to 2,048 block entries, 512 death-entity entries, and 256 shearing entries, keeping sanitized snapshots below the network ceiling.
