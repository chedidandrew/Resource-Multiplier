# Performance design

Resource Multiplier avoids world scans. Provenance is queried only for relevant placement, removal and loot paths.

The shearing subsystem adds no tick, AI, chunk, or entity scan. A scope is created only for a server-side interaction whose target is a living `Shearable`, or for the exact vanilla dispenser entity-shear invocation; a `1x`, disabled, unknown, or special resolution immediately uses the no-op path. Tag membership is read from the current registry holder at resolution time so `/reload` works without a permanent cache.

Qualifying helper output is bounded cumulatively across the action. Planning uses saturating arithmetic and permits at most 1,024 proposed multiplied items plus 256 collected source or materialized legal-stack groups. It delays emission only until the real shear method completes, retains each original consumer, and falls back to the complete original `1x` batches before allocating an oversized result. Warnings are keyed by entity type/source/reason, capped at 256 keys, and suppressed for five minutes per key. No per-entity state or unbounded recent-action history is retained; the nested thread-local stack is cleared in `finally`, including exceptional paths.

- Placement transactions record positions created by the current BlockItem operation.
- The remove-before-drop compatibility cache is limited to two ticks and 4,096 entries per loaded level.
- GUI Apply uses one 256 KiB-capped dirty-field payload and at most one atomic config write instead of one command/write per edit. Client queued work is bounded and removed before callback execution so re-entrant cancellation is safe.
- Server snapshot requests have a 40-tick per-player cooldown and keep only the newest pending request. Sanitized snapshots are serialized once and cached until a real config mutation; payload JSON is capped at 1 MiB, with separate 2,048-entry block, 512-entry death-entity, and 256-entry shearing rule budgets. Rejected mutations use a compact result instead of reflecting the full snapshot. Patch cooldowns likewise keep at most the newest already-authorized update per player and never retain unauthorized payloads.
- `/smartdrops validate` examines only configured references against the current immutable configuration and live block/entity/dimension/tag registries. It performs no world, chunk, entity, inventory, loot-table, or datapack scan and triggers no reload. Compact output is capped at 15 issues and verbose output at 100; retained load diagnostics and samples are separately bounded, and UUID override values are never sampled.
- Tag filters iterate the broken block's bound tags, not every configured tag.
- Multiplied loot is consolidated to each item's legal maximum stack size before entities are spawned.
- A block multiplier above `1x` is planned against the complete final loot list before multiplied stacks are allocated. Overflow-safe estimates must remain at or below 262,144 items and 4,096 legal stacks. If either limit would be exceeded, the full original list is returned untouched at `1x`; no partial result is materialized. `1x` remains an identity-preserving fast path.
- Block budget warnings are keyed by block ID plus reason, limited to 256 keys, and suppressed for five minutes per key. Statistics record one fallback plus only the original evaluation/vanilla item count, avoiding fabricated multiplied/bonus totals.
- One entity death may produce at most 262,144 mod-multiplied items and 4,096 mod-emitted stacks. Mob XP amplification above 634,112 keeps the original award. These bounds do not truncate or rewrite pathological output that another mod already produced; they prevent Resource Multiplier from amplifying it further.
- `1x` should remain a fast no-op path.
- No per-tick block scanning, chunk scanning or inventory scanning is permitted.

Performance regressions should be measured with natural mining, large builds, piston clocks, TNT, automated miners, dense mob farms, a `64x` stress case, and a deliberately pathological block-loot list that exercises the untouched fallback and warning throttle. Those final post-hardening manual observations are pending.
