# Architecture

## Design goal

Resource Multiplier changes only final loot at three explicit boundaries: qualifying block loot, standard living-entity death-table loot, and supported certified entity-shearing helper output. The implementation avoids world scans, per-tick polling, broad item-entity interception, and a third-party configuration library.

## Runtime path

1. A supported source starts a short-lived session for one exact server level, block position, and original block state. Real players, explosions, and automation are distinct sources; Fabric fake players use the automation policy.
2. The rule resolver determines whether that block is eligible and calculates one effective multiplier.
3. Minecraft builds the normal loot list from its loot table, tool, enchantments, block entity, actor, and other context.
4. A return injection on `BlockStateBase.getDrops` must match and claim the session's exact target. It groups identical item-and-component results, multiplies their counts, and splits only at the item's legal stack size.
5. The context remains active through `spawnAfterBreak`. XP multiplication is independent and requires the same level and block position.
6. The source-specific session is cleared. Nested automation for a different block gets its own stacked session, so it cannot consume the outer target.

The loot list is multiplied rather than reconstructed. This avoids reimplementing Fortune, Silk Touch, data-pack conditions, modded loot functions, or component data.

## Shearing runtime path

1. A real server player interacting with a living `Shearable`, or the exact vanilla dispenser entity-shear invocation, opens a nested identity-bound scope.
2. The resolver checks the master/source gate and live entity-type tags. Known/tagged special entities win over standard certification; unknown entities fail closed at `1x`.
3. The entity calls `LivingEntity.dropFromShearingLootTable` normally. Its final consumer is replaced with a bounded collector only for the exact active target; the loot table runs once.
4. Each helper invocation retains its own downstream consumer. The real shear method finishes its state/tool/event work once before output commits.
5. The complete action is preflighted cumulatively against 1,024 items and 256 source entries or materialized legal stacks. It emits all legal multiplied batches or all original `1x` batches, never a partial mix.
6. Exceptional actions attempt original-output rollback without masking the original exception, then clear the thread-local scope in `finally`.

Beehives, leash removal, block shearing, direct equipment ejection, custom machine calls outside the vanilla dispenser path, and global spawn paths never enter this pipeline.

## Main packages

| Package | Responsibility |
| --- | --- |
| `config` | JSON loading, sanitization, atomic saves, defaults, and presets |
| `core` | Rule resolution, drop context, categories, tags, and optional statistics |
| `provenance` | Persistent per-chunk placement markers and placement transactions |
| `command` | Server-authoritative configuration commands |
| `network` | Bounded, request-correlated server snapshot transport for the client screen |
| `mixin` | Narrow hooks into loot calculation, placement, falling blocks, pistons, and XP |
| `client` | Lightweight local-default and server-authoritative configuration screen |

## Configuration hierarchy

The rule policy is separated from mixin hooks and has a dependency-light core assertion suite. Minecraft-facing behavior is additionally tested with JUnit against mapped classes and with dedicated-server and client GameTests. After safety gates, the hierarchy is:

```text
optional player override > block > category > dimension > global
```

The block through global chain determines the base value. A player override is applied last only when the server enables it, so it has the highest effective priority. The value is capped by both `maxPlayerMultiplier` and `maximumMultiplier`.

## Provenance storage

The attachment key is:

```text
smart_resource_drops:placed_blocks
```

Each affected chunk stores a compact set of integers. Each integer encodes local X, local Z, and Y. The chunk is marked unsaved only after its mutable set changes. Empty attachments are removed.

Storage cost is proportional to recorded player placements, not world size. Natural terrain is represented by the absence of a marker.

`BlockItem.place` opens a bounded nested transaction. Successful world writes collect candidate positions and the transaction commits them only if placement succeeds, which covers multi-block items such as doors without scanning nearby blocks. Custom item code that bypasses `BlockItem.place` needs an explicit compatibility integration.

Successful same-position writes use a transition policy. Resource-identity transformations retain the marker, known generated growth clears it, and unrelated replacement removes it. Remove-before-drop integrations get a two-tick, 4,096-entry-per-level compatibility cache; an overflow temporarily fails closed rather than treating unknown provenance as natural.

## Thread model

Minecraft block changes run on the server thread. Very short `ThreadLocal` stacks correlate nested loot and placement calls without global mutable context. MixinExtras method wrappers close those scopes in `finally` blocks; level changes use wrapper-local state, and falling-block capture restores provenance conservatively if creation throws. Mods that replace the wrapped vanilla methods still require compatibility testing.

## Server authority

The gameplay config is read and applied by the logical server. With no active world, the client can atomically edit the global local file as defaults for future integrated servers. With a play connection, the screen requests a sanitized live snapshot and never falls back to that file; it opens read-only for non-operators. Operator Apply sends one bounded dirty-field delta which the server permission-checks, validates, atomically persists, and acknowledges. The client never sends loot decisions, per-player UUID rules, or its local configuration file to a connected server.

Snapshot requests and patch acknowledgements carry an ID and the client binds a pending response to its current connection. Stale replies are ignored. The server rate-limits snapshot requests and keeps only the newest pending request per player, caches serialized snapshots until a config mutation, strips per-player UUID values, caps snapshots at 1 MiB, and caps patches at 256 KiB/2,048 edits. During a patch cooldown, only the newest already-authorized update is queued; permission is checked again before application and unauthorized payloads are never retained. The small client queue is bounded, removes due work before callbacks for re-entrant safety, and clears on disconnect.

## Dependency policy

Runtime requirements are only:

- Fabric Loader
- Fabric API
- Minecraft

Gson is supplied by Minecraft. No Cloth Config, Kotlin runtime, or utility library is required. Mod Menu is a compile-only optional client integration and is not present on the dedicated-server runtime classpath.
