# Implementation log

## 2026-08-30, structured tooltip wrapping correction

Structured configuration rows now pass hover text through Minecraft's standard `Tooltip.splitTooltip` path before rendering. This consumes embedded line breaks and constrains each rendered line to the vanilla 170-pixel tooltip width, fixing both the screen-wide hover text and visible `LF` control glyphs. Entity-category hover text was also shortened to supplemental inheritance and estimate guidance because the category, configured value, and active safety state are already rendered in the row. A real-client regression uses the mapped runtime font to require every category tooltip line to stay within 170 pixels and to reject surviving LF or CR code points.

## 2026-08-30, canonical public repository and release metadata

The canonical project is now the public [chedidandrew/Resource-Multiplier](https://github.com/chedidandrew/Resource-Multiplier) repository with GitHub Issues enabled. Fabric metadata publishes the exact homepage, source, and issue-tracker URLs consumed by Mod Menu, while package, Mod Menu, and release-JAR validators prevent those links, the production icon path, or the SPDX `MIT` declaration from drifting. GitHub recognizes the repository license as MIT; the bundled license contains the permission grant, retained-notice condition, and warranty disclaimer. MIT keeps this project openly reusable but does not impose a source-publication requirement on downstream forks.

The public-source audit found no credential/token/private-key signatures, tracked runtime/build/cache/world files, symlinks, or suspicious oversized binaries. Historical local artifact paths were converted to repository-relative paths. Source packaging now consumes exactly the Git-tracked manifest, rejects stray non-ignored files plus common secret/runtime paths, uses stable cross-platform modes, and refuses a non-empty destination. The playable-JAR gate requires every production top-level class and exact entrypoint, mixin, runtime-dependency, icon, contact, and byte-identical embedded-license contracts. GitHub Actions keep least-privilege job permissions, disable persisted checkout credentials, impose timeouts, pin every third-party action to an immutable commit, and refuse tagged publication unless the explicit release latch is enabled on a commit contained in `origin/main`. These publication checks do not close the remaining hands-on gameplay, multiplayer, datapack-reload, or third-party compatibility gates for the unreleased shearing candidate.

## 2026-08-30, Resource Multiplier public-name transition

The public-facing mod name changed to **Resource Multiplier** across Fabric and Mod Menu metadata, client configuration and reset screens, commands and diagnostics, documentation, support templates, build artifacts, and release packaging. This is deliberately a presentation-only change: the `smart_resource_drops` mod ID and datapack/network namespace, `com.chedidandrew.smartresourcedrops` Java package, `config/smart_resource_drops.json` schema and keys, saved-world provenance identifiers, and `/smartdrops` and `/smartdropsgui` commands remain unchanged. Existing configurations, worlds, provenance data, commands, and datapacks therefore require no migration. The final rename pass passed 158 JUnit tests, all 66 dedicated GameTests, the real client GameTest with Mod Menu present, all six static/core validators, standalone server/client checks, direct Mod Menu/configuration/reset captures, final JAR inspection, and deterministic source/release-package generation.

## 2026-08-30, schema-3 safe entity shearing candidate

The candidate adds one narrowly scoped resource path without broadening into other interactions. `Player.interactOn` creates a server-side frame only for a living `Shearable`; only a real-player frame may capture output, while fake/unsupported, disabled, and fixed-`1x` frames act as non-capturing barriers so a nested action cannot inherit an outer source. The exact `Shearable.shear` invocation inside the vanilla dispenser entity path creates the automated frame. `LivingEntity.dropFromShearingLootTable` is wrapped only while an identity-matched capturing frame is active. Its final stacks are copied into consumer-preserving batches, the real shear implementation finishes once, then the complete action emits multiplied legal stacks or complete original `1x` output. Loot tables are never rerun, the entity state transition is never repeated, and global spawn/item/inventory/equipment paths are not intercepted.

The nested context is cleared in `finally`. Multiple helper calls share a cumulative 1,024-item/256-stack-group preflight with saturated arithmetic. Budget overflow never partially multiplies. If the action throws after collection, the closest vanilla behavior is attempted by emitting the originals once, scoped references are cleared, and the original action exception remains authoritative. Warnings use a bounded 256-key five-minute limiter.

Live entity tags certify standard resources and identify specials. Sheep ships as standard; Bogged, Copper Golem, Mooshroom, Snow Golem, and Sulfur Cube ship as special and are also hard-gated by audited IDs. Special wins over standard conflicts, and unknown exact overrides cannot bypass certification. Current holder tags are read at resolution time for reload safety.

Schema migration was split by source version. Schema 1 preserves valid block settings and applies the established safe entity initialization; schema 2 preserves every entity field exactly; both initialize shearing OFF and empty. Fresh/reset uses manual ON, automation OFF, inherited global default, and no overrides. Malformed/future-schema safe state keeps both sources OFF. The new patch/session/network path remains one bounded revision-checked atomic Apply.

Commands, read-only inspection, live validation, focused GUI screens, package manifests, and documentation were extended. The serial Java 25 chain now passes 158 JUnit tests, all 66 dedicated GameTests (44 shearing-focused), the real client GameTest, the clean Loom build, six static/core validators, and standalone server/client startup smokes. Version metadata remains 1.1.0 and this entry makes no release claim until the hands-on gameplay, multiplayer, datapack-reload, and third-party gates in `docs/TESTING.md` and `docs/PUBLIC_RELEASE_CHECKLIST.md` are complete.

## 2026-08-30, final 1.1 diagnostics, output-budget, and publication hardening

### Scope retained

Under the former public name, the 1.1 charter stated that the mod multiplies final loot produced by qualifying block breaks and living-entity deaths. It does not change resource generation, harvesting speed, crafting, processing, transportation, storage, spawning, combat, or world progression. The hardening work stayed inside that charter: it added read-only diagnostics, bounded output, tests, release validation, and reporting support without adding vein mining, tree felling, smelting, per-biome/tool/enchantment/weather/time layers, chest or structure loot, fishing, bartering, trading, shearing, milking, spawning, processing, inventory movement, or another progression system.

### Read-only live configuration validation

`/smartdrops validate` and `/smartdrops validate verbose` are operator-only commands that also work from the server console. The command takes the current immutable configuration/load-diagnostic snapshot and a view of the server's live block, entity-type, dimension, and tag registries. It checks only configured block/entity IDs, tags, dimensions, filters, rule budgets, block-entity safety entries, risky combinations, and retained load warnings. It does not enumerate worlds or chunks, scan loaded entities, execute loot tables, reload resources, save configuration, change revisions, or normalize/delete entries.

Unknown configured IDs, tags, and dimensions are warnings rather than destructive cleanup. This preserves rules for a temporarily absent mod or datapack until that content returns. Compact output renders at most 15 issues and verbose output at most 100, while the internal report remains bounded. Load diagnostics retain only bounded value samples and backup basenames; UUID-based player overrides are counted but never sampled, enumerated, or printed.

### Complete block-loot preflight

The block path now preflights the complete final loot list before allocating multiplied output. Multiplication above `1x` must fit both shared hard limits: 262,144 items and 4,096 legal stacks. Planning uses saturated, overflow-safe arithmetic. If either estimate exceeds its limit, the block path returns the exact original list object and contents at vanilla `1x`; it does not partially multiply, truncate, split, or rebuild the list. The invariant applies to player, explosion, and supported automation drop sessions. `0x` still returns an empty mutable result and `1x` remains the unchanged fast path.

A fallback updates statistics as an evaluated block with the original vanilla-item count and one `blockBudgetFallbacks` increment. It does not increment multiplied-block or bonus-item counts. The warning records the block ID, dimension, position, multiplier, estimates, limits, and explicit vanilla `1x` fallback. To avoid farm/log amplification, warnings are keyed by block ID plus reason in a 256-key bounded cache with a five-minute interval. Verbose block inspection advertises the drop-time limits but never evaluates loot.

### Deprecations, templates, and package boundaries

All project Java compilation enables `-Xlint:deprecation` without `-Werror`. Entity holder lookup was moved from the removed/deprecated convenience method to `BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(...)`; GameTests use the supported `makeMockServerPlayer(GameType)` route through shared player helpers instead of repeated deprecated calls or broad suppression. The intermittent XP fixture isolates the orbs belonging to each scenario so a prior award cannot satisfy a later assertion.

Structured bug and exact mod-compatibility issue forms, blank-issue configuration, and the uppercase `.github/PULL_REQUEST_TEMPLATE.md` now provide public reporting routes. `SECURITY.md` remains explicit that this snapshot has no verified private vulnerability channel. Packaging validation rejects nested JARs, common shaded dependency/test/fixture namespaces, missing declared entrypoint/mixin/tag assets, and root source/config/log/world/cache/release-bundle leaks from the playable JAR. Deterministic source validation requires the templates, scope/security documents, protected item tag, wrapper, and key sources while continuing to include development tests and fixtures only in the source archive.

### Current verification state

The final post-hardening automated sequence passes all five validators (103 production Java sources and a deterministic 224-entry source set), all 90 dependency-free core assertions, 125 mapped JUnit tests in 21 suites, all 22 required dedicated GameTests, the 38-second real client GameTest, and the 32-second no-build-cache Java 25 clean build. A standalone server without Mod Menu reaches `Done (0.313s)`, runs compact and verbose validation successfully from the console, stops cleanly, and completes in 45 seconds. A Mod Menu 20.0.0 client completes startup resource/audio/atlas initialization before intentional interruption; it is not counted as interactive GUI evidence.

The final playable JAR is 483,638 bytes with 272 entries and SHA-256 `E3A6A38ADB3412F081ED546089C0E61C932AC8E0D507AB5B33F9A94A5DF66EBA`; required hardening classes are present and the release audit finds no fixture, testmod, development-loot, nested/bundled dependency, or runtime-data leak. The working directory has no Git metadata, so a commit, tag, remote, or prior public-release state cannot be recorded and version 1.1.0 remains unchanged. Exact command and artifact evidence is centralized in `BUILD_STATUS.md`.

A standalone interactive client/in-world pass, manual validation scenarios, hands-on pathological block-budget fallback/warning observation, the 46-case vanilla matrix, custom-tag `/reload`, running-game migration/restart, dense-farm observation, separately installed multiplayer, and named/versioned third-party compatibility remain manual release gates.

## 2026-08-30, 1.1 entity death-loot implementation and pre-hardening automated checkpoint

The measurements in this section are a preserved pre-final checkpoint. They predate the validation-command, complete-list block-budget, deprecation, publication-template, and latest packaging changes described above and are not the current final release evidence.

### Post-baseline verification hardening

The first 20/20 dedicated-runner result was a count of runner cases, not a one-test-per-requirement ledger for the 54 requested automated behaviors. The follow-up fixture pass removes project tags from the four class-fallback probes, drives real Passive/category-priority deaths, uses Minecraft's mob pickup path, uses a deterministic mapped Looting function plus `killed_by_player`, checks component-rich `1x`, two production wrappers over the same list, runtime player exclusion, and independent item/XP gate combinations. The client GameTest now includes every entity field in its snapshot equality/copy helpers and exercises Entity Categories, Overrides, Filters, Apply, and Discard on the shared draft. Packaging additionally rejects nested JARs, common shaded/test namespaces, missing declared entrypoint/mixin/tag assets, and root source/runtime leaks.

These changes passed a fresh serialized validation after integration. A live custom-tag `/reload`, process restart/migration, dense-farm observation, the 46 manual cases, separately installed multiplayer, and named third-party mods remain outside this automated pass.

### Boundary chosen from Minecraft 26.2

`LivingEntity.dropAllDeathLoot(ServerLevel, DamageSource)` is the authoritative death sequence. Minecraft calls standard `dropFromLootTable` first, then custom death loot, equipment, and experience. The mod begins an exception-safe context around that one death method and wraps only the death-specific four-argument-to-five-argument loot-consumer call. The captured list has already passed loot conditions, Looting, cooking/furnace functions, components, datapack tables, and Fabric final-drop modifiers. Multiplication never rerolls the table.

That narrow boundary is why ordinary standard death-table output can multiply while equipment, held/picked-up items, entity inventory/cargo, subclass custom death output, and direct `ItemEntity` creation remain vanilla. Player and armor-stand targets are permanently excluded. The generic gift, interaction, shearing, fishing, breeding, command, and inventory paths never enter the death context.

The multiplier copies final stacks, preserves components, consolidates only identical item/component combinations, and splits at legal maximum stack size. `0x`, `1x`, and `64x` remain distinct. Saddles and Totems of Undying are protected final outputs: this resolves the Minecraft 26.2 cases where Ravager's standard loot table is a saddle and Evoker's standard table contains a totem, which a generic final-list hook cannot distinguish as equipment or progression output.

### Exactly-once, attribution, and XP

Each death context is bound to the victim, level, and damage source. A standard-loot claim is consumed once per death session; a duplicate supported hook path passes through at vanilla quantity instead of applying a second multiplier. Contexts form a stack, so a loot callback that kills another entity receives an independent inner context and returns to the outer one. Every scope closes in `finally`, including a thrown loot callback.

Kill attribution reuses vanilla state. Direct real-player damage, player-owned projectiles, and later environmental deaths with live vanilla player credit can qualify in `PLAYER_KILLS_ONLY`. `PLAYER_OR_TAMED_ENTITY` additionally accepts a resolved real-player owner of a tamed attacker. Missing/offline owner references stay unqualified without a profile or network lookup. `ALL_STANDARD_DEATH_LOOT` admits otherwise-unattributed standard death loot. Fabric fake players are not treated as real players.

Mob XP is independent from both block XP and item multiplication. Only the `ExperienceOrb.award` call inside the active victim's `dropExperience` step can receive `mobExperienceMultiplier`; unrelated awards, advancement/score/stat rewards, and nearby orbs are outside the context. Boss XP requires both mob-XP enablement and the separate boss-XP opt-in.

The first attribution audit found that an untamed-wolf hit cleared the mod's remembered direct-player origin even though Minecraft retains the existing player-credit window for that hit. The tracker had treated every wolf as a possible attribution replacement. It now changes origin only for a real player or a tame wolf with a safely resolved owner. A dedicated-server regression establishes player credit, applies untamed-wolf damage after clearing only the fixture's immediate invulnerability frames, finishes with environmental damage, and requires the player-only multiplier to remain active.

The hardening audit made the standard-loot claim lazy and owner-token based: empty preliminary invocations do not claim the session, while nested or duplicate wrappers around the same consumer multiply exactly once. Mod-created amplification is cumulatively bounded per death to 262,144 items and 4,096 emitted stacks; after either bound is reached, later callbacks pass through unchanged with one diagnostic. Mob XP now uses an identity-matched one-shot award token instead of a broad depth flag, and multiplication above 634,112 keeps the original XP award. Direct/tamed attribution origin is saved with the living entity, malformed or legacy values fail closed, and an immediate player must agree with vanilla's remembered UUID/origin. Saddles and totems remain hard protected while `#smart_resource_drops:protected_entity_loot` lets datapacks protect additional outputs.

The configuration/editor audit changed `save` to sanitize and atomically write a copy before publication, strips every schema-2 entity field before schema-1 decoding, revision-guards local Apply, and broadcasts typed UPDATE/RESET invalidations after every publication. Clean connected editors reload; dirty drafts remain visible but stale and cannot silently overwrite the new revision. Rejected or unauthorized mutations use a compact result payload. Connected entity catalogs use live server tag bindings only, while metadata-only `MobCategory` previews are labeled estimated and direct players to server inspection.

### Classification, configuration, and inspection

The ordered categories are Bosses, Villagers/NPCs, Golems, Neutral, Passive, Hostile, Aquatic, Ambient, and Miscellaneous. Classification records every match and its source. Boss evidence always wins; otherwise explicit `#smart_resource_drops:categories/*` tag evidence wins in category order, followed by vanilla class and `MobCategory` evidence. Unknown modded living entities fall back to Miscellaneous. All nine production category files use `replace: false`, and their explicit vanilla IDs were checked against the mapped Minecraft 26.2 entity registry.

Schema 2 adds a default-off entity feature, inherited/default/category/exact multiplier hierarchy, independent exact/tag filters, player/tamed/all-deaths policy, boss item gate, and separate mob/boss XP gates. The entity domain has its own 512-entry budget rather than consuming the 2,048 block-rule budget. Loading a 1.0.x file preserves its block values and initializes all entity behavior safely OFF. Patch, snapshot, permission, revision, atomic persistence, and Reset All reuse the existing authoritative transaction; reset does not touch chunk placement provenance.

The shared editor now has Entity Drops General, Categories, Entity Overrides, Filters, and Advanced routes. They use the root-owned draft and Apply/Discard transaction, remain complete/read-only for non-operators, and continue allowing kill/filter editing when item drops are off but mob XP is on. `/smartdrops inspect entity` and its verbose form perform a server raycast and render an immutable hypothetical invoking-player trace. Repeated inspection does not mutate health, position, combat credit, tags, configuration/revision, items, XP, equipment, or inventory, and the formatter never exposes NBT or inventory contents.

### Development fixtures and current verification

The GameTest mod registers test-only passive, hostile, neutral, aquatic, tag-only/multi-category, unclassified, boss, equipment, carried-item, inventory, component-rich Fabric final-drop, direct-output, nested-death, exception, duplicate-hook, cooked, empty, Looting-final, and unstackable fixtures. Its resource pack extends the production category tags and supplies deterministic entity loot tables. The focused server suite maps its assertion names to disabled/`0x`/`1x`/`64x`, final-result, exclusion, attribution, boss/XP, category/filter/precedence, context, inspection, gamerule, empty/baby, and legal-stack requirements.

Packaging validation rejects fixture/testmod/GameTest paths, development loot tables, nested or shaded dependencies, missing declared entrypoint/mixin/tag resources, source leaks, and runtime/world data from the playable JAR. It intentionally retains tests and fixtures in the complete source archive. Targeted package/static validation passes with 90 production Java sources and a 198-entry source archive.

Final automated validation passes: all five static/package/UI validators, 90 core assertions, 92/92 mapped JUnit tests in 16 suites, all 20 required dedicated GameTests, the 39-second real client GameTest, a standalone client startup smoke, and a 32-second clean standalone server startup/stop. The cache-free exact clean build succeeds in 35 seconds. The inspected 1.1.0 playable JAR has 242 entries, is 426,260 bytes, uses Java class major 69, and contains no fixture/testmod/development-loot/bundled-dependency/runtime-data leaks; its SHA-256 is `6AC0002F1615AB2FFF6248DFABE356A50EC65825D59190A57B9AE219DD3A1866`.

`/reload` behavior, the complete player-driven vanilla matrix, running-game restart/migration, dense-farm observation, separately installed multiplayer checks, and named/versioned third-party mob compatibility remain pending. No publication-readiness or generic compatibility claim is made from automated fixtures. The historical block checkpoints below are preserved unchanged.

## 2026-08-30, public release cleanup

### Rule and default semantics

- `PLAYER_PLACED_ONLY` previously excluded natural blocks only inside the `smartPlacementProtection` branch. Turning protection off therefore made the mode admit natural blocks as well as placed blocks. The source-mode decision is now independent where it must be: `PLAYER_PLACED_ONLY` always rejects natural blocks, with protection either on or off; `NATURAL_ONLY` rejects placed blocks only while protection is on and admits natural plus placed blocks when protection is off; `ALL` admits natural plus placed blocks regardless of the protection toggle. The independent player-mining, explosion, and automation source toggles still apply before these provenance rules.
- The authoritative safety blacklist now includes `minecraft:dragon_egg`. This remains an ordinary, removable configuration entry rather than a special case in `RuleEngine`; removing it restores normal filter and multiplier resolution. The shipped default example is checked against the authoritative blacklist entries and ordering so the two cannot silently drift.

### Server-authoritative block inspection

- `/smartdrops inspect` and `/smartdrops inspect verbose` are common/server commands available to normal players in integrated or dedicated play, independently of Mod Menu. The invoking server player supplies the eye position and current block-interaction range for the server-side block raycast; the client never supplies a registry ID. Sky/no-target and non-player console sources receive bounded, explicit failures. A coordinate form was deliberately left out of this lightweight first version.
- `RuleEngine.trace` now produces an immutable `RuleResolutionTrace`, and ordinary gameplay resolution delegates to that same trace before taking its applied multiplier. This keeps exact/tag filters, block-entity protection, source and provenance gates, player/block/category/dimension/global precedence, configured multiplier, applied result, and human-readable reason in one authoritative algorithm instead of reimplementing the rules in command formatting.
- `MultiplierResolver.inspect` constructs the same targeted `RuleInput` as player mining but switches provenance lookup to `PlacementTracker.peekPlaced`. That in turn uses `RecentRemovalCache.peekWasRecentlyRemoved`, which neither removes an entry nor advances/purges the bounded cache. Formatting consumes only the immutable trace and the already loaded target state; it does not evaluate loot tables, open a drop session, spawn loot or XP, mutate a block or block entity, save configuration, change the configuration revision, update statistics, or add/remove provenance.
- The trace retains all category matches in normal gameplay order and the category that actually supplied the active override. It reads only the target block's associated tags, so vanilla `minecraft:` tags and common `c:` tags classify modded blocks without scanning registries or chunks; a block with no category match reports Miscellaneous and continues through dimension/global resolution. Namespaced mod IDs and modded dimension IDs remain intact.
- Compact output is intentionally chat-sized. Verbose output adds bounded block-state text, exact and tag filter matches, provenance eligibility, each candidate override, all source toggles, and separate `has block entity` / `protection enabled` / `allowlisted` / `result` fields. It never reads or renders block-entity NBT, inventory contents, owner UUIDs, locks, or custom names, and it exposes only the invoking player's relevant override.
- Inspection-specific regression coverage is split between dependency-free core assertions and mapped JUnit tests for trace behavior, parsing, formatting bounds/privacy, and a non-consuming recent-removal peek, plus dedicated-server GameTests. The GameTests repeatedly inspect real stone and a tracked, populated chest while checking stable traces, gameplay-decision parity, and configuration/statistics/world/block-entity/provenance invariants; they also execute the real command dispatcher for a server-player raycast, compact output, verbose sky/no-target guidance, and console rejection.
- Final validation passed all five Python validators with 64 production Java sources and a deterministic 132-entry source archive, all 90 core assertions, all 63 JUnit tests in 13 suites (including 12/12 focused trace tests and 6/6 command tests), all 8/8 required dedicated-server GameTests, the 17-second clean build, the 34-second real client GameTest, and a 33-second standalone server run that reached `Done` without Mod Menu or a client-only classloading failure and stopped cleanly. The final JAR is `build/libs/smart-resource-drops-1.0.3.jar`, 291,391 bytes and 173 entries, SHA-256 `1F4FBF54C13C544909BA61F0800FFB76C073E2ED04EB0F95FF10B33849565A1B`.
- A standalone `runClient` startup smoke initialized the mod and completed client resource, audio, and texture-atlas loading before the task was intentionally interrupted. The in-world inspection/mining checklist was not performed. Real third-party modded blocks, block entities, and dimensions also remain a manual compatibility matrix; synthetic namespaced rule inputs and vanilla/common-tag fixtures do not replace those checks. The completed evidence below this section predates block inspection and remains preserved as historical baseline data.

### Shared editor state and root-owned actions

- Every category, block, dimension, filter, advanced, and preset child continues to share the root `ConfigEditorSession`. A child-level **Unsaved Changes** indicator now makes the shared dirty state visible without duplicating mutation controls.
- Apply, Done/**Discard Changes**, and **Reset All Settings** remain root-only actions. Back or Escape from a child preserves the shared draft and returns to the root flow; Cancel or Escape from reset confirmation preserves that same staged session. Successful Apply or reset still reconstructs the editor from the authoritative snapshot so stale child models and cached override indicators are not retained.

### Public packaging and deferred scope

- Public packaging was hardened around a deterministic, validated source archive that excludes generated build output, runtime worlds/configuration, caches, backups, and recursively packaged releases while retaining the required Gradle wrapper, source, tests, workflows, and documentation. JAR freshness is checked only against actual Gradle/source/test inputs so recording the built artifact in documentation cannot create a finalization loop. Release checks also reject unsafe/incomplete source archives, stale Loom artifacts, and default-example drift, and the tagged release workflow publishes the validated bundle it documents.
- `fabric.mod.json` intentionally retains empty contact metadata. No homepage, source, or issue-tracker URL has been verified for this project, so adding real contact links remains a release-owner TODO rather than publishing an invented destination.
- Mob and general entity drops remain outside the 1.0.x implementation. They are documented only as a candidate 1.1 design in `docs/ROADMAP.md`; this cleanup adds no partial entity-drop mixin, configuration field, payload, migration, or promise of delivery.

### End-to-end audit and completed evidence

- The complete provenance path was re-audited from placement capture through persistent chunk attachments, the short remove-before-drop cache, `MultiplierResolver`, `RuleInput.playerPlaced`, and player/explosion/automation drop sessions. Provenance lookup remains unconditional even when smart placement protection is off, so `PLAYER_PLACED_ONLY` can still identify placed blocks. `ALL` changes only provenance eligibility and does not bypass filters, protected block entities, or the independent drop-source toggles.
- The configuration network/reset path was re-audited for permission rechecks, connection identity, expected revisions, stale mutation rejection, reset cooldown, pending-patch invalidation, authoritative refresh of other open editors, atomic persist-before-publish behavior, and preservation of provenance/statistics history. Rejected reset packets now rate-limit their potentially large authoritative snapshot replies, while a corrected request is not suppressed by the destructive-write cooldown. Successful command/console resets now use the same queued-patch clearing and editor invalidation completion hook as GUI resets. No new dependency or alternate configuration format was introduced.
- One compatibility risk remains explicit: a third-party automation integration that removes a marked block and delays its eventual `Block.dropResources` call beyond the bounded two-tick recent-removal window can be classified as natural. The hooked vanilla and synchronous automation paths resolve within that window; integrations that manufacture items without a supported drop path remain out of scope.
- Five Python validators passed. The Windows-native dependency-free core suite passed all 62 assertions.
- `runClientGameTest` completed with `BUILD SUCCESSFUL` in 32 seconds after the dirty-state test was strengthened to use the real category control, navigate specifically to Block Overrides, and verify Discard did not persist the abandoned category edit.
- A manual Mod Menu-enabled `runClient` completed with `BUILD SUCCESSFUL` in 14 minutes 18 seconds.
- A fresh standalone dedicated `runServer` completed with `BUILD SUCCESSFUL` in 37 seconds after reaching `Done (0.256s)` and accepting `stop`.
- The final exact clean build completed with `BUILD SUCCESSFUL` in 15 seconds and all six required GameTests passed. Measured JUnit XML reported 12 suites and 47/47 tests with no failures, errors, or skips. The release-candidate artifact identity is recorded once in `BUILD_STATUS.md`.
- The final deterministic source ZIP and five-file release bundle were generated after the clean build. Independent inspection found 129 source entries, no path outside the versioned top-level directory, no generated/runtime entry, all required Gradle bootstrap files, and executable mode `755` on `gradlew`.

## 2026-08-29, Reset All Settings

### Confirmation and staged-state safety

- The General/root screen now gives **Reset All Settings** its own full-width section, visually separated from Apply and Done. Selecting it opens the vanilla confirmation screen; the destructive **Reset Everything** choice is rendered red.
- Merely opening the confirmation cannot mutate configuration. Cancel and Escape invoke the same negative callback and restore the exact existing root screen and `ConfigEditorSession`, including every staged edit, dirty flag, search query, and navigation state.
- Only confirmation begins a reset. The replacement value is exactly `SmartDropsConfig.defaults()`, so every serialized field is restored from the configuration model's single source of truth rather than a partial or duplicated GUI defaults table.

### Atomic and authoritative reset transaction

- A local-defaults reset performs one atomic persist-before-publish transaction. On success the configuration and revision advance together and a fresh clean editor session is opened immediately; a write failure leaves the prior configuration and revision intact.
- Integrated and dedicated multiplayer use the dedicated reset payload instead of encoding reset as an ordinary patch. The server validates edit permission and the client's expected revision at the mutation boundary, then performs one logical reset/write/revision increment and returns the authoritative snapshot. Unauthorized and stale requests do not mutate state.
- Confirmation invalidates the staged draft plus every pending client command/request generation and server patch queue. Packets from the old generation or revision cannot replay changes over the defaults. A successful authoritative reset also invalidates other open editors so they cannot continue from an obsolete baseline.
- A short reset cooldown bounds repeated reset requests and prevents disk-write spam without weakening permission or revision validation.

### Data intentionally outside configuration

- Placed-block provenance attachments are not erased. Resetting configuration must never make a player-placed block appear natural after its chunk data is saved or reloaded.
- Accumulated `SmartDropsStats` session history is preserved. The serialized `statisticsEnabled` option itself is a configuration field and therefore returns to its model default, but the already accumulated counts/history remain available.

### Verification

- The dedicated JUnit reset suite passed, covering exhaustive model defaults, persistence, atomic write failure, exactly one successful revision advance, stale reset/patch rejection, and statistics preservation.
- All 6/6 server GameTests passed, including the real provenance marker surviving a configuration reset.
- The full real client GameTest passed in 34 seconds. It exercises local confirmation/non-mutation/Cancel/Escape/confirm behavior, integrated-server authority, dedicated operator success, dedicated non-operator rejection, and stale/pending queue invalidation.
- Static package, Mod Menu, and reset-contract audits passed, including checks for the dedicated payload, server permission and revision gates, queue invalidation, confirmation copy/tooltip, and the absence of provenance/statistics reset calls.
- Package validation counted 62 production Java sources, all 47 JUnit tests and 42 dependency-free core assertions passed, and the clean `test runGameTest build` pipeline completed successfully.
- Reset checkpoint artifact: `build/libs/smart-resource-drops-1.0.3.jar`, 269,733 bytes, SHA-256 `4e0614b0735f87383968278d9eacec9185ac01e40fb8fa8ae39779fb0924797d`.
- Manual-only limitations remain: a true JVM/world restart followed by provenance chunk reload, interoperability between separately installed multiplayer client/server instances, and human visual/focus inspection at multiple GUI scales. No separate manual `runClient` result is recorded for this feature.

## 2026-08-29, hierarchical configuration GUI redesign

### Why the flat editor was replaced

- The earlier ready screen placed global controls, block/category/dimension rules, filters, operational settings, and presets behind one view selector and page cursor. A normal registry could expose well over a thousand blocks, so finding one override required paging through large full-width button rebuilds and keeping several unrelated concepts in mind at once.
- The redesign makes the common path small and gives each rule type its own searchable context. Manual page controls and the monolithic view state were removed rather than restyled.

### Screen hierarchy

```text
General
├── Categories
│   └── Category editor (configured / inherited / effective)
├── Block Overrides
│   └── Block editor (ID / category / inherited source / effective)
├── Dimensions
│   └── Dimension editor (configured / inherited / effective)
├── Filters
│   └── Blacklist/whitelist entries and mode explanation
└── Advanced
    └── Existing operational settings and preset previews
```

- General contains only the global `[-] value [+]` multiplier, smart placement protection, multiplier source, XP toggle/multiplier, authority/status text, navigation, dirty-gated Apply, and Done.
- Category and dimension screens use focused list/detail flows. Dimension rows are derived at runtime from the vanilla dimensions plus configured, current, and connected dimensions, so modded dimensions do not require hard-coded UI entries.
- Block Overrides is deliberately search-first. With an empty query it shows only configured overrides and explanatory text; it never expands the complete registry. A query matches translated block names and namespaced IDs. At most 200 broad-query matches are allocated/displayed, with a refine-search message when more exist.
- Filters keeps blacklist/whitelist behavior and its explanation together. Exact block entries can be staged through the existing patch representation; configured tag entries remain visible and read-only because changing that wire/config representation was outside this GUI-only refactor.

### Shared draft, rendering, and authority

- `ConfigEditorSession` owns the deep-copied baseline and working draft, effective-value resolution, dirty calculation, and construction of the single `ConfigPatch`. Every child receives the same root/session, so Back/Escape navigation does not request a fresh snapshot or lose staged edits.
- Apply is inactive for a clean or read-only session. A connected Apply still passes exactly one bounded patch through the existing loading/acknowledgement screen; acknowledgement reconstructs the editor from the authoritative snapshot. Local defaults still call the atomic configuration transaction.
- Catalogue construction is lazy: opening General alone does not scan the block registry or build block/tag metadata. The first catalogue-backed screen builds it once for the session. Block and filter searches use bounded 32-entry normalized-query caches, and filter matching applies its 200-row cap before allocating rendered row models.
- `StructuredConfigList` uses lightweight `ObjectSelectionList` entries instead of one `Button` per registry item, leaving Minecraft's list widget to render only visible rows and handle scrolling/keyboard selection. List/search state is retained when a focused category or block editor returns to its parent.
- Non-operators can inspect every screen and effective value but cannot stage changes. No packet, permission, rate-limit, snapshot, patch, server-validation, configuration-schema, or on-disk format behavior changed as part of the redesign.

### Client category resolution and interaction polish

- Runtime tag bindings are not populated in the title-screen/local-default path, which initially made valid category lists appear empty even though the installed category JSON was correct. `ClientCategoryTagIndex` now resolves the installed block-tag JSON graph client-side and combines that result with live runtime bindings when available. The client runtime test resolves 44 vanilla blocks for Logs/Wood.
- Category membership remains data-driven rather than hard-coded, including nested tag references. This preserves the installed data-pack definition while making local/default editing useful before a world connection exists.
- Search queries and list scroll positions survive opening a focused editor and pressing Back. `0x` remains a concrete override distinct from **Inherit**, including in focused category and block editors.
- Compact-height layout spacing was corrected after screenshot inspection. When the shared draft is dirty, the root exit button now reads **Discard Changes** so leaving without Apply is explicit.

### Final verification

Verified against the completed redesign on 2026-08-29:

- `validate_package` passed and counted 59 Java sources.
- The edge-case source audit, polish regression tests, and hierarchical Mod Menu static validator passed.
- The Windows-native core runner passed all 42 assertions.
- `gradlew --no-daemon clean test runGameTest build` completed with `BUILD SUCCESSFUL`; all five required server GameTests passed.
- `gradlew --no-daemon runClientGameTest` completed with `BUILD SUCCESSFUL` in a real Minecraft 26.2 client.
- The final client suite exercised General protection/source/XP round trips; a populated 44-block Logs category; diamond display-name search; exact `minecraft:diamond_ore` ID search; acacia search; the broad-query 200-row cap; focused category and block editors; concrete `0x` versus inheritance; retained query/scroll state after Back; exact block-filter add/remove/revert; non-mutating preset preview; bounded widget/row construction; and local, integrated, dedicated non-operator/operator, Apply, acknowledgement, cooldown, and disconnect authority behavior.
- An earlier real Mod Menu-enabled `runClient` launch succeeded and its screenshots were inspected. The latest client GameTest screenshots additionally show the 44-block Logs category and diamond search results.
- GUI-redesign checkpoint artifact: `build/libs/smart-resource-drops-1.0.3.jar`, 256,580 bytes, SHA-256 `7ea0dd2dc48143756ceb6e2535f03941a7e2bb8d37102bd02556057337716f49`.

The following remain genuine manual/fixture limitations, not failed automated gates:

- [ ] Exercise an actual third-party modded block and modded dimension fixture; the dynamic paths are covered structurally, but no third-party content mod is installed in the automated runtime.
- [ ] Add GUI tag-filter editing only if the protocol is deliberately extended. Configured tag filters are currently visible/read-only because the existing `ConfigPatch` has no tag edit fields.
- [ ] Exhaustively human-click the complete separately installed multiplayer and GUI-scale/window-size matrix. Automated compact-layout and embedded dedicated-server coverage does not replace that compatibility pass.

The sections below preserve the earlier 1.0.3 diagnosis, synchronization work, and verification history.

## 2026-08-29, version 1.0.3 Mod Menu configuration release blocker

### Diagnosis

- Reproduced the title-screen failure in a real Minecraft 26.2 client with Mod Menu 20.0.0. Mod Menu correctly invoked the integration, but that factory always opened the connected-server loading bridge. With no play connection, no request could be sent and the full settings screen was never constructed.
- Minecraft 26.2's GUI extractor rejects text whose alpha byte is zero. The custom screens used six-digit RGB literals such as `0xFFFFFF`, so the title, status, reason, and all custom labels were transparent while native Retry/Back button labels remained visible. Both screens also reported themselves as in-game UI at the title screen, producing the black background.
- Networking payload registration, IDs, codecs, and directions were correct. Additional lifecycle review found stale screen references, implicit phases, invalid snapshots becoming editable defaults, no immediate disconnect error, retry/resize state loss, an integrated-owner permission mismatch, silent patch drops during cooldown, and a delayed command-screen race.

### GUI and authority changes

- Mod Menu and `/smartdropsgui` now share one route. With no world or integrated server, it constructs the complete editor from the global local config and saves one validated patch atomically. That file supplies future local/singleplayer defaults.
- With any active play connection, the route is immutable server-authoritative. It shows explicit loading/error screens until a valid snapshot arrives; it never falls back to local settings. Non-operators see the complete read-only interface, operators can Apply, and the integrated owner is editable even without cheats.
- All custom colors now contain opaque alpha, title/world backgrounds are selected dynamically, long text is bounded, compact layouts are calculated rather than independently clamped, mouse-wheel pagination works, and block rows show namespaced IDs plus inherited/effective multipliers.
- Connected snapshots are cached only for the active connection and invalidated when the connected settings screen is removed, so a later reopen refreshes changed server settings and op/de-op state.

### Request and mutation lifecycle

- Requests use explicit `LOADING`, `READY`, `ERROR`, and closed lifecycle state with monotonically changing IDs and connection identity. Response handling crosses `Minecraft.execute`, validates JSON, verifies the exact current loading screen, and ignores stale, superseded, removed, or disconnected requests.
- Retry creates a new generation, resize preserves button/error state, disconnect clears queued work and transitions the visible loading screen immediately, and invalid/future snapshots remain errors rather than editable defaults.
- Server Apply rechecks permission and rejects out-of-range values and invalid shapes before one atomic transaction. Cooldown-limited authorized patches keep only the newest request for delayed processing, while unauthorized payloads are never retained for a later permission promotion. Client queued work is removed before callbacks execute, preventing re-entrant cancellation from corrupting iteration.
- `/smartdropsgui` opens after the chat-close cycle, but its delayed callback is bound to the originating connection and screen flow so it cannot hijack an unrelated GUI.

### Regression and runtime verification

- Added pure JUnit coverage for request generations, Retry/stale responses, close/disconnect, title versus connected authority, cached READY decisions, common and narrow responsive layouts, bounds/invalid snapshots, delayed command guards, and re-entrant queue draining. The mapped suite now contains 42 passing tests.
- Added a Fabric client GameTest which launches a real client, screenshots the title/local and integrated-server screens, reconstructs a populated screen from the connection cache, applies and restores a real integrated-server setting, launches and joins a dedicated server, verifies the full non-operator screen is read-only, verifies unauthorized cooldown traffic cannot be pre-staged before promotion, reopens after `/op`, and proves an authorized cooldown-queued patch is discarded on disconnect.
- A Mod Menu-enabled `runClient` launch displayed the complete title-screen settings UI. `runGameTest` still passes all five required dedicated-server tests with client/Mod Menu classes absent. A normal `runServer` launch without Mod Menu reached `Done`, saved all three dimensions, and stopped cleanly.

## 2026-08-29, version 1.0.3 release hardening

### Repository and build baseline

- Audited the project before changing behavior. The supplied directory had no `.git` metadata, so a commit baseline and source-control history could not be verified. A recoverable source backup was made outside the project before release-hardening changes.
- The unchanged Windows bootstrap failed because its nested PowerShell checksum command could not resolve `Get-FileHash`. Running the pinned Gradle distribution directly on Java 25 proved that the baseline Loom project compiled, but the Gradle test task reported `NO-SOURCE`; the existing 42-assertion core runner was not a Minecraft integration test.
- Pinned stable Fabric Loom 1.17.20, added JUnit 5.14.2 and a dedicated GameTest source set, declared Fabric API as a real minimum version, and kept Mod Menu compile-only. The server GameTest classpath intentionally excludes client and Mod Menu runtime classes.
- Removed wrapper fallback to arbitrary `gradle` executables. The Windows bootstrap now hashes with the .NET SHA-256 implementation and successfully reports Gradle 9.5.1 on Java 25.0.3.
- Updated build and release workflows to run validators, core assertions, JUnit, dedicated-server GameTests, and the Loom build. Release tags are checked against `mod_version`, and packaging rejects a stale or incorrectly versioned JAR.

### Exact drop sessions and stack output

- Problem: a broad player-break context and a single consumable loot session could be claimed by a nested loot calculation for a different block. Fake players were also treated as ordinary players, and nearby XP could borrow the active block decision.
- Cause: the context tracked source and rule decision without requiring the loot call to match the originating level, position, and state.
- Change: each session now carries an exact target guard. Item loot must match level, position, and state and may claim the target only once; XP must match level and position. A nested automation call for a different target receives its own stacked session. Player context begins at the exact full `Block.dropResources` overload, and Fabric fake players follow the automation toggle.
- Regression coverage: `DropTargetGuardTest` verifies mismatched and repeated claims plus XP position matching. A dedicated-server GameTest drives the real stone drop path and verifies natural `2x`, marked `1x`, `0x`, and one-entity `64x` output; Fortune, XP, explosions, and representative automation remain part of the manual matrix.
- Problem: high multipliers could retain fragmented partial loot entries and needed stronger component/max-stack guarantees.
- Change: identical item-and-component stacks are grouped, multiplied with checked `long` arithmetic, copied with components intact, and split at each stack's actual maximum.
- Regression coverage: `StackConsolidatorTest` uses real Minecraft `ItemStack` and component types, including custom data, a name, and a custom maximum stack size.

### Placement provenance and transformation policy

- Problem: overlapping placement and world-write hooks could record unsuccessful or unrelated writes, while broad replacement cleanup could either launder a transformed placed resource or contaminate genuinely generated growth.
- Cause: placement intent and successful state transition were observed in separate legacy hooks without one nested transaction or explicit transformation classification.
- Change: `BlockItem.place` now opens a bounded nested transaction. Successful `Level.setBlock` changes become candidates, and only a successful placement commits every candidate position. The obsolete placement context and legacy set-block mixins were removed.
- Change: one transition policy preserves the marker for same-resource state changes, soil-family changes, stripped logs or wood, copper weathering/waxing, concrete powder to concrete, coral death/revival, and cauldron variants. Unrelated replacements remove the marker; crop and similar same-block growth is classified as newly generated.
- Regression coverage: JUnit checks the transition table and unrelated replacement. Dedicated-server GameTests exercise real level writes and a real door placement that marks both halves.
- Change: the recent-removal compatibility window now uses an incrementally expired insertion-ordered map, caps each level at 4,096 positions, clears stale entries on game-time rollback, and conservatively treats all positions as protected for two ticks after overflow.
- Regression coverage: JUnit verifies expiry, overflow, and rollback behavior.

### Commands and configuration durability

- Problem: terminal list arguments used word parsing, so namespaced identifiers containing `:` could fail. JSON tag entries with `#` were not normalized consistently.
- Change: list commands consume and validate the full terminal identifier, normalize namespace/path case, and accept optional `#` for tag lists. JSON tag filters strip any leading marker during sanitization.
- Regression coverage: three Brigadier/Minecraft command tests and a JSON tag-normalization test.
- Problem: load-time normalization could overwrite a future schema or a malformed/unreadable file, and a missing blacklist could not be distinguished from an administrator's explicit empty list.
- Change: future-schema files are left byte-for-byte untouched while safe in-memory defaults run with writes suppressed. A malformed file is replaced only after a timestamped backup succeeds; backup or read failure suppresses writes. Missing blacklists install safety defaults, explicit empty arrays remain empty, no-op updates skip disk writes, and normal writes use atomic replacement when supported.
- Regression coverage: seven base-config tests exercise these distinctions, identifier/rule bounds, and failure-safe paths with temporary files.
- Problem: command updates published a candidate to live memory before attempting the file write, while write failures were only logged. An operator could receive a success message for an in-memory-only change that would disappear on restart.
- Cause: the command mutation API returned no result and reused a save routine that absorbed persistence failures.
- Change: command and personal-rule mutations now reject a raw candidate over the rule cap, write the sanitized candidate first, publish it only after success, and propagate a failure result to every command caller. This prevents a newly added high-priority collection entry from silently displacing a lower-priority rule at the 2,048-entry boundary. Explicit reset is also persist-before-publish, and reload reports protected/read/persistence failures instead of unconditional success.
- Regression coverage: three additional temporary-file tests prove successful persistence, failed-write rollback, suppression while a future-schema file is protected, and full-cap rejection without rule displacement.

### Client synchronization and optional integration

- Problem: late snapshots and reconnects could apply to the wrong screen, command-per-edit Apply caused many server-thread disk writes and partial updates, and repeated snapshot serialization/request traffic was unnecessary.
- Change: requests carry monotonically changing IDs and are bound to the active connection. Closing or disconnecting cancels pending opens and clears the bounded request queue. Apply sends one 256 KiB-capped delta containing only dirty fields; the server permission-checks it, validates at most 2,048 total rule edits/entries, writes once atomically, and acknowledges applied, rejected, or unauthorized with the authoritative snapshot. Server snapshot requests have a ten-tick per-player cooldown, snapshots are cached until config mutation, UUID overrides are removed, and snapshot payloads are capped at 1 MiB.
- Change: patch processing is rate-limited for authorized and unauthorized senders. The newest authorized patch during cooldown is processed when eligible instead of being silently lost; unauthorized payloads are never retained. The lightweight screen now includes dimension overrides, operational server toggles, and previewed/staged presets. Block and filter search matches translated names as well as namespaced IDs. Selecting an already-active Custom state preserves ordinary dirty edits, and raw multiplier/search text survives screen rebuilds such as fullscreen or resolution changes.
- Regression coverage: six configuration-transaction tests cover preset-plus-delta order, unrelated setting and UUID preservation, filter/map edits, idempotence, validation bounds, failed persistence without active-state mutation, and command transactions. Static GUI/network contracts guard acknowledgement throttling, Custom no-op behavior, and rebuild-safe text state.
- Mod Menu remains an optional client entrypoint opening the shared local/server-authority route. Dedicated GameTests confirm its classes do not leak onto the server runtime; the later client verification above covers the launched UI and authority states.

### Verification completed

- Package, edge-case, polish, and Mod Menu static validators passed.
- The Windows-native core runner passed all 42 assertions.
- All 42 JUnit tests passed against mapped Minecraft classes.
- The dedicated Fabric server runner reported exactly five required GameTests passing: four mod methods covering Mixin/server isolation, transformations, door placement, and the real stone loot pipeline, plus the Fabric runner's required count.
- Main, client, test, and GameTest sources compiled on Java 25.
- The complete interactive gameplay matrix remains documented as a release gate rather than reported as tested; the configuration client/server matrix is covered as described above.

## 2026-08-29, version 1.0.2

- Added optional Mod Menu 20.0.0 integration for Minecraft 26.2.
- Registered a dedicated `modmenu` entrypoint that returns the existing server-authoritative configuration flow.
- Added a lightweight loading bridge with no-world, unavailable-sync, timeout, retry, and cancel states.
- Added cancellation protection so late server snapshots cannot reopen a closed settings screen.
- Flushed coalesced GUI command updates before requesting a refreshed server snapshot.
- Added Mod Menu translation metadata and regression checks.
- Updated the plants category data tag for Minecraft 26.2 by removing the obsolete `#minecraft:tall_flowers` reference; the valid `#minecraft:flowers` tag already contains the tall flower blocks.
- Added package validation to prevent the removed `#minecraft:tall_flowers` reference from being reintroduced.

## 2026-08-29, version 1.0.1

### Added

- Fabric project targeting Minecraft Java 26.2 and Java 25
- Global item multiplier from `0x` through `64x`
- Rule hierarchy for block, category, dimension, and global values
- Optional capped per-player override
- Blacklist and whitelist modes with exact block and tag filters
- Default `2x` natural-block behavior
- Independent block XP multiplier, disabled by default
- Player mining, explosion, and automation source toggles
- Loot-list multiplication at Minecraft's calculated drop boundary
- Stack-count consolidation to reduce spawned item entities at high multipliers
- Persistent per-chunk placement provenance
- Multi-block placement capture through the item placement transaction
- Piston provenance transfer with conservative protection
- Piston provenance persistence across saves
- Falling-block provenance transfer and persistence
- Block-entity safety with explicit allowlist
- Searchable command-backed client configuration screen with block, category, and filter-list views
- Sanitized live server configuration snapshots with operator-only editing
- Server commands and three presets
- Optional in-memory statistics
- English language strings and mod icon
- GitHub Actions build and release workflows
- Dependency-free core rule assertion runner (not a Minecraft runtime test)
- Offline source contract compile
- Responsive configuration layout for standard narrow GUI widths
- Canonical identifier normalization for commands with omitted namespaces
- Interface-backed piston provenance carrier to avoid invalid mixin self-casts
- Deterministic, checksum-labeled offline candidate JAR builder for sandbox smoke testing
- Deterministic source and release-bundle packaging helper
- Metadata and package validator
- Architecture, anti-dupe, configuration, testing, and GitHub documentation

### Kept out of scope

- Vein mining
- Automatic smelting
- Tree felling
- Magnet pickup
- Inventory sorting
- Tool durability changes
- Custom progression systems

These exclusions preserve the mod's lightweight identity.
