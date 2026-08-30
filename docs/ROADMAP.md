# Resource Multiplier roadmap

This document describes possible future work. It is not a promise of a release date or a specification for the current build.

## Current 1.2 shearing candidate status

Status: **implemented in source, release gate open**. The candidate adds only supported entity shearing actions: real-player interaction and the exact vanilla dispenser entity branch, with multiplication at `LivingEntity.dropFromShearingLootTable`. Sheep is the shipped standard resource. Unknown types and the audited vanilla special transformations remain `1x`; direct equipment output, beehives, leash removal, block shearing, milking, brushing, egg laying, breeding, gifts, fishing, bartering, trading, chest/structure loot, and unrelated resource systems remain out of scope.

Schema 3, independent source toggles, a 256-rule domain, shared-draft GUI, commands, inspection, validation, 1,024-item/256-stack whole-action safety, and focused tests are present. The serial automated suite, real client GameTest, server/client startup checks, package audit, and candidate artifact inspection pass. The version remains `1.1.0` until the full hands-on vanilla/special/UI/multiplayer/reload matrix and representative third-party compatibility checks pass. Preserve the 1.1 and earlier investigation/history below; mark this plan complete only when those gates are recorded.

## 1.0.x maintenance boundary

The 1.0.x line remains focused on block-resource drops, placed-block provenance, configuration safety, compatibility, and defect fixes. **Mob or general entity drop multiplication will not be implemented in 1.0.x.** No 1.0.x configuration key, network payload, mixin, or data migration should reserve or partially enable the design below.

## Current 1.1 implementation status

Status: **implementation and the final diagnostics/output/publication automated hardening pass are complete**. All five validators, 90 core assertions, 125 mapped JUnit tests in 21 suites, 22 dedicated GameTests, the real client GameTest, standalone server validation, the no-build-cache clean build, and playable-JAR audit pass. Publication-readiness and general mod compatibility remain blocked on the manual and third-party gates below.

The implementation uses a narrower, evidence-backed shape than the original investigation below:

- Entity death loot is a separate server-authoritative domain, disabled by default, with `PLAYER_KILLS_ONLY`, `PLAYER_OR_TAMED_ENTITY`, and `ALL_STANDARD_DEATH_LOOT` policies.
- Only final standard non-player `LivingEntity` death-table output is eligible. Equipment, held/picked-up items, inventories, custom/direct output, players, armor stands, and non-death resources stay vanilla.
- Boss items, ordinary mob XP, and boss XP have separate default-off gates. Ravager saddles and Evoker Totems of Undying are protected even inside the standard table.
- Resolution supports exact entity, selected category, default entity, and global inheritance; independent exact/tag filters; nine datapack-extensible project categories; explicit tag/class/`MobCategory` evidence; and safe Miscellaneous fallback.
- Schema 2, bounded ConfigPatch/snapshot/reset/migration, shared Entity Drops GUI routes, and `/smartdrops inspect entity [verbose]` use the existing server permission/revision/atomic-write model.
- Exception-safe nested death contexts and a per-session standard-loot claim enforce scoped XP and exactly-once item multiplication.
- A development-only Fabric testmod supplies deterministic modded entity, final-loot-modifier, inventory/equipment/custom-output, nested/exception/duplicate-hook, and boundary-result fixtures. Release packaging rejects those fixtures from the playable JAR.
- Operator/console-only `/smartdrops validate [verbose]` performs a bounded, server-authoritative, read-only audit of configured entries against live registries/tags. Unknown references remain preserved; compact/verbose output is capped at 15/100 issues and UUID override values are never exposed.
- Block multiplication now preflights the complete final list against 262,144-item and 4,096-stack limits. Overflow or excess returns the full original list untouched at `1x`, with dedicated statistics and a five-minute/256-key warning limiter.
- Project compilation exposes deprecations without blanket suppression or dependency `-Werror`; public issue/compatibility forms, the PR template, scope/security documents, required assets, nested/shaded leak rejection, and the checksum-pinned standard Gradle 9.5.1 wrapper are package-validation inputs.

The preserved pre-final checkpoint passed 92 mapped JUnit tests in 16 suites and 20 dedicated runner tests; those figures remain history and are not the final result. The final evidence is 103 production Java sources, a deterministic 224-entry source set, 90 core assertions, 125 mapped JUnit tests in 21 suites, 22 dedicated GameTests, a 38-second client GameTest, a 32-second no-build-cache clean build, and a 483,638-byte/272-entry playable JAR with SHA-256 `E3A6A38ADB3412F081ED546089C0E61C932AC8E0D507AB5B33F9A94A5DF66EBA`. Standalone interactive client use, hands-on validation and pathological block-budget scenarios, custom category-tag `/reload`, running-game restart/migration, dense-farm observation, the full player-driven vanilla matrix, separately installed multiplayer checks, and the named/versioned third-party wildlife/hostile/boss/inventory matrix remain open gates. The feature must not be tagged or described as generally mod-compatible until those results are recorded.

## Integration and public API stance

There is no supported public Java API in either version 1.1.x or the current unreleased 1.2 shearing candidate. Publicly visible classes and methods in the implementation are internal details unless a later compatibility document explicitly guarantees them; other mods should not depend on their names or signatures. Documented configuration and commands plus the datapack-extensible category, protected-output, and audited standard-shearing-resource tags are the supported integration surfaces for the current candidate.

A formal Java compatibility API is deferred until at least one concrete, reproducible third-party integration case cannot be solved safely through those existing surfaces. Any proposal must begin with that real case and define ownership, lifecycle, failure behavior, versioning, documentation, and cross-mod tests before an API is promised. Speculative hooks are not roadmap commitments.

## Historical candidate 1.1 design (superseded)

Status: **superseded planning record**. This section is preserved to show the conservative questions considered before implementation; its illustrative field names and deferred-category assumptions are not the current schema. Use [CONFIGURATION.md](CONFIGURATION.md) for the implemented fields and the current status above for release gates.

### Conservative first scope

- The feature would be server-authoritative, disabled by default, and independently configurable from block multipliers.
- Player kills would be required by default. Non-player, environmental, and automated-farm kills would remain vanilla unless a server deliberately enables a broader, separately tested policy.
- Boss drops would remain disabled until each boss or boss tag is explicitly opted in; no broad hostile-mob toggle would silently include them.
- The first supported target would be non-player `LivingEntity` death loot produced through Minecraft's normal loot pipeline. Player drops, item entities, projectiles, vehicles, item frames, inventories, container contents, fishing, block entities, and commands that manufacture items directly would remain outside the feature.
- Vanilla loot tables, Looting, equipment rules, components, damage, and mod-added data must finish first. The mod would multiply only a clearly identified final loot-table stack, copy it without losing components, split it into legal stack sizes, and never reroll a loot table.
- Equipment, saddles, chest/cargo contents, and other carried/container items would not be multiplied unless Minecraft or a cooperating API can identify them unambiguously as ordinary death-table loot. Ambiguous output stays vanilla.
- A death-scoped server token would bind processing to one entity UUID, level, and death event. Each eligible output could be claimed once; nested deaths or mod callbacks would receive separate scoped contexts. Replayed callbacks, unrelated item spawns, and direct inventory insertion must not inherit the multiplier.

### Spawn provenance and farming policy

Minecraft does not provide a universally reliable distinction between every natural, structure, command, spawner, breeding, conversion, and mod-created spawn. Version 1.1 must not infer a "natural" classification from incomplete signals.

The safe initial policy is therefore explicit opt-in by entity ID/tag with a neutral multiplier by default. Any later natural-only or farm-source policy would require persistent, server-side spawn provenance with save/reload, conversion, dimension transfer, passenger, breeding, and modded-spawn tests before it could be enabled.

### Proposed configuration shape

Names are illustrative until schema review:

- `entityDrops.enabled`: master switch, default `false`.
- `entityDrops.multiplier`: independent bounded multiplier, default `1`.
- `entityDrops.playerKillsOnly`: safe farming boundary, default `true`.
- `entityDrops.filterMode` plus entity ID/tag lists: explicit allow/deny policy with unknown IDs preserved across loads.
- Optional passive/hostile group inheritance and per-entity overrides only after the simple policy is proven. Bosses stay explicitly disabled unless opted in; category inference and broad registry enumeration are deferred.
- XP remains a separate, default-off decision. A mob-item multiplier must not silently multiply experience, advancement rewards, score, or statistics.

All edits would use the existing permission, expected-revision, atomic persistence, snapshot, and stale-request protections. Dedicated servers remain authoritative; clients never decide eligibility or quantities.

### Compatibility and failure policy

- Prefer a documented Fabric/Minecraft loot or drop boundary over broad interception of every `spawnAtLocation`/item-entity call.
- If another mod bypasses the supported loot boundary, its direct output remains unchanged unless it offers an explicit compatibility hook.
- Cancellation, keep-inventory-like rules, gamerules, loot-table conditions, and zero-drop outcomes remain authoritative.
- Quantity limits must reuse legal stack splitting and bounded-total safeguards. Overflow fails closed to vanilla output rather than spawning an unbounded number of entities.
- A missing or ambiguous context, duplicate claim, stale configuration revision, or unsupported entity type produces vanilla behavior and a rate-limited diagnostic—not guessed multiplication.

### Required gates before implementation or release

- Unit tests for filtering, bounded arithmetic, stack/component preservation, duplicate claims, and nested death contexts.
- Real server GameTests for natural hostile/passive mobs, zero drops, Looting, equipment separation, conversions, cross-dimension/save-reload behavior where applicable, and mod-style nested callbacks.
- Dedicated multiplayer tests for operator/non-operator configuration authority and stale revisions.
- Compatibility tests showing that canceled deaths, gamerules, commands, inventories, direct mod output, and players are untouched.
- Performance profiling of dense mob farms with the feature both disabled and enabled.
- Documentation and migration review confirming that upgrading from 1.0.x changes no existing block-drop behavior.

The planning record above remains historical. Version 1.1.0 still remains unreleasable until every current manual gate listed earlier in this document and in [PUBLIC_RELEASE_CHECKLIST.md](PUBLIC_RELEASE_CHECKLIST.md) has evidence.
