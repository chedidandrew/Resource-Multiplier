# Changelog

All notable changes are documented here.

## Unreleased — 1.2.0 candidate

### Changed

- Fixed structured configuration tooltips so Minecraft performs its standard 170-pixel wrapping and consumes explicit line breaks instead of displaying `LF` control glyphs. Entity-category hover copy is now concise and leaves details already visible in the row out of the tooltip.
- Changed the public name from **Smart Resource Drops** to **Resource Multiplier**. This is a branding-only compatibility change: the mod ID remains `smart_resource_drops`; `config/smart_resource_drops.json`, saved-world provenance, `/smartdrops` and `/smartdropsgui`, and `smart_resource_drops:*` datapack IDs remain compatible. Existing configurations, worlds, commands, and datapacks require no rename migration.
- Published the canonical project homepage/source and Issues URLs in Fabric metadata so Mod Menu exposes working **Website** and **Issues** actions. Release validation now locks those URLs, the production icon path, and the SPDX `MIT` declaration.
- Hardened the public GitHub workflows by pinning every third-party action to a verified immutable commit, disabling persisted checkout credentials, and bounding job runtimes.
- Hardened public packaging so source archives contain exactly Git-tracked files, reject untracked or secret/runtime paths, normalize cross-platform permissions, require an empty output directory, and fail closed unless every production class plus the exact entrypoint, mixin, dependency, icon, contact, and embedded-license contract is present. Tagged publication is additionally locked behind an explicit `release_ready` latch and main-branch ancestry check.

### Added

- Added narrowly scoped, server-authoritative multiplication for certified standard entity shearing output. Manual player and vanilla dispenser sources have independent gates; manual defaults ON only for a truly fresh/reset configuration, while automation defaults OFF.
- Added `#smart_resource_drops:shearing/standard_resources` with sheep as the production certification and `#smart_resource_drops:shearing/special` for Bogged, Copper Golem, Mooshroom, Snow Golem, and Sulfur Cube. Known vanilla specials are also hard-gated in code, so data-pack replacement or a conflicting safe tag cannot make their transformation/equipment output multiply.
- Added an identity-matched, nested `ShearingActionContext` around `Player.interactOn` and only the entity branch of `ShearsDispenseItemBehavior.tryShearEntity`. The final `LivingEntity.dropFromShearingLootTable` consumer is buffered once per helper call; loot is never rerolled and `Shearable.shear` is never repeated.
- Added an always-on whole-action shearing preflight capped at 1,024 multiplied items and 256 collected/materialized stack groups. Overflow or pathological output falls back completely to original `1x` batches through their original consumers, with saturating arithmetic and bounded five-minute warning keys.
- Added schema-3 shearing fields, a separate 256-entry exact-rule domain, atomic patch/removal support, a shared-draft Shearing Drops GUI, operator commands, read-only entity-inspection output, and shearing-specific validation codes.
- Added focused resolver/buffer/budget tests and schema-1/schema-2 serialized migration fixtures. The dedicated mapped shearing GameTest matrix is part of this candidate hardening pass.

### Migration and safety

- Schema 1→3 retains valid block settings, applies the established safe entity migration, and initializes both shearing sources OFF. Schema 2→3 preserves every valid entity death-loot field exactly and initializes shearing OFF with no overrides.
- Malformed existing files and future-schema safe fallback never activate shearing. Explicit Reset restores current fresh defaults—manual ON, automation OFF, inherited default, no exact overrides—without altering placement provenance or world data.
- Unknown/uncertified modded shearables and unsupported custom-machine calls remain vanilla `1x`. Modpack authors may certify a compatible standard-helper entity through the project tag; exact config overrides alone cannot bypass certification.
- Beehive honeycomb, dispenser leash removal, block shearing, direct equipment ejection, and global item-spawn paths are not intercepted.

### Verification status

- The serialized Java 25 automated chain passes: 158 JUnit tests in 24 suites, all 66 dedicated GameTests (44 focused on shearing), the 40-second real client GameTest with Mod Menu present, the 30-second clean Loom build, all six package/static/core validators, standalone server/client startup smokes, and direct Mod Menu/configuration/reset-title captures. The nested runtime installs non-capturing barrier frames for disabled, fixed-1x, and untrusted re-entrant actions, preventing a same-target inner source from inheriting an eligible outer multiplier.
- The canonical-repository safety pass reran the cache-free clean build in 26 seconds with 158/158 JUnit tests and 66/66 GameTests, passed the 37-second real client GameTest plus all package/Mod Menu/release/core validators, produced and validated the tracked-only deterministic source/release bundle, and inspected the final 311-entry JAR for exact MIT/contact/icon/entrypoint/mixin/dependency metadata, every production top-level class, and embedded-license identity.
- This work intentionally retains `1.1.0` metadata. The hands-on client/gameplay/multiplayer matrix, a real in-running-game datapack reload cycle, and representative third-party custom shearables/tools/machines remain required before changing the version to `1.2.0` or publishing an artifact. No release claim is made by this entry.

## 1.1.0 - 2026-08-30

### Added

- Added an independent, server-authoritative entity death-loot domain. It is disabled by default, defaults to vanilla player-kill attribution, and does not change any 1.0.x block rules or placement provenance.
- Multiplies only final standard non-player `LivingEntity` death-table stacks. The hook is scoped to `LivingEntity.dropAllDeathLoot`, wraps only the death-specific loot consumer, and leaves equipment, held/picked-up items, entity inventories, custom/direct item spawns, shearing, gifts, fishing, player deaths, and other non-death resources untouched.
- Added exception-safe death contexts bound to entity identity, level, and damage source. A per-session claim prevents two supported hook paths from multiplying the same death twice; nested deaths push independent scopes and exceptional loot generation closes the outer scope in `finally`.
- Added vanilla-compatible kill modes for player kills, player-or-tamed-entity kills, and all standard death loot. Projectile and later environmental deaths use vanilla player-credit state; unresolved/offline tame owners never trigger a blocking profile lookup.
- Added separate default-off mob XP and boss XP controls. Only the `ExperienceOrb.award` call made by the active qualifying death context can multiply; unrelated nearby XP remains vanilla.
- Added Bosses, Villagers/NPCs, Golems, Neutral, Passive, Hostile, Aquatic, Ambient, and Miscellaneous/Unclassified entity categories. Datapacks and mods can extend `#smart_resource_drops:categories/<category>` with `replace: false`; explicit project tags win over class and `MobCategory` fallbacks, and unknown modded living entities fall back to safe Miscellaneous `1x`.
- Added conservative boss safety for Ender Dragon, Wither, Warden, Elder Guardian, Ravager, and Evoker. Boss item and XP multiplication are separately gated. Ravager saddles and Evoker Totems of Undying are protected final outputs and remain single even after boss item multiplication is deliberately enabled.
- Added schema 2 entity settings, independent 512-entry entity-rule budget, atomic migration from 1.0.x with entity drops OFF, full `ConfigPatch`/snapshot/reset coverage, and a shared-session Entity Drops GUI with General, Categories, Entity Overrides, Filters, and Advanced views.
- Added `/smartdrops inspect entity` and `/smartdrops inspect entity verbose`. The server raycasts the looked-at non-player living entity and renders the same immutable classification/filter/attribution/item/XP trace used by gameplay without damaging the target, changing combat credit, generating output, exposing inventory/NBT, or mutating configuration.
- Added a development-only GameTest mod with deterministic passive, hostile, neutral, aquatic, tag-only, unclassified, boss, equipment, carried-item, inventory, component-rich Fabric loot-modifier, direct-output, nested-death, exceptional, duplicate-hook, cooked, empty, Looting-final, and unstackable fixtures. Release packaging now rejects these fixtures, development loot tables, bundled dependencies, and runtime/world data from the playable JAR.
- Added operator/console-only `/smartdrops validate` and `/smartdrops validate verbose`. Validation is server-authoritative and read-only, uses the current immutable configuration plus live block, entity-type, dimension, and tag registries, and checks configured entries only rather than scanning worlds, loaded entities, loot tables, or datapacks. Unknown configured IDs/tags are reported but preserved. Chat output is bounded to 15 issues in compact mode and 100 in verbose mode; bounded load diagnostics count UUID overrides without printing or sampling them.
- Added a block-loot output preflight with hard limits of 262,144 multiplied items and 4,096 legal emitted stacks. A block result that exceeds either limit, including through saturated overflow-safe arithmetic, returns the complete original final list untouched at `1x`; it is never partially multiplied or truncated. Budget fallbacks have a dedicated statistic and bounded five-minute/256-key server warning limiter.
- Added structured GitHub bug and exact mod-compatibility issue forms, a blank-issue policy, and a pull-request template. Source/package validation requires those publication assets, the scope/security documents, the protected-output tag, and key declared source/mixin/entrypoint resources.

### Fixed during validation

- Made the standard-loot claim lazy, so an empty preliminary invocation cannot consume multiplication, and added a production two-wrapper regression proving the same emitted list is multiplied exactly once. Amplified deaths are now cumulatively bounded to 262,144 created items and 4,096 emitted stacks; later output stays vanilla after the bound is reached.
- Replaced the broad nested XP suppression depth with an identity-matched one-shot award token. Mod-created mob XP amplification above 634,112 falls back to the original award, while block and unrelated nested XP still use their own rules.
- Persisted direct/tamed kill-origin metadata with living entities and require immediate-player attribution to agree with vanilla's remembered player UUID and origin. Legacy, unresolved, offline-owner, and mismatched states fail closed.
- Added `#smart_resource_drops:protected_entity_loot` as a datapack extension point while retaining hard saddle/totem protection. Exact inner loot/XP mixin injections are optional (`require = 0`) so another mod replacing that call disables the narrow feature path instead of crashing startup; the required outer death scope still audits broad mapping drift.
- Made `ConfigManager.save` transactional, stripped every schema-2 entity field before decoding schema-1 files, revision-guarded local Apply, and broadcast typed update/reset invalidations. Clean editors refresh, dirty drafts are preserved and marked stale, and unauthorized/stale mutations receive compact explicit results instead of reflecting a full configuration snapshot.
- Connected entity catalogs now use only live server tag bindings. Registry-metadata-only category previews are visibly marked estimated and direct users to `/smartdrops inspect entity` for runtime class-authoritative results.
- Preserved vanilla player-kill credit when an already player-credited victim is later damaged by an untamed wolf. The first origin tracker incorrectly cleared `DIRECT_PLAYER` for every wolf hit, while Minecraft only replaces the responsible-player state for a tame wolf. Tracking now changes only for real-player or tame-wolf attribution, and a dedicated-server regression performs player damage, untamed-wolf damage, then environmental death in `PLAYER_KILLS_ONLY` mode.
- Isolated the wide entity GameTest fixtures with explicit structure padding. Their first concurrent run allowed long relative fixture positions to enter a neighboring test's raycast area; the corrected run passes all required tests without cross-test targeting.
- Updated the real client permission test to treat Entity Drops as read-only navigation, then added real-client coverage for the Entity and Mob Drops screen, namespaced cow lookup, and rejected non-operator mutation.
- Removed project-source deprecation use in the entity holder and GameTest player paths. Java compilation now enables `-Xlint:deprecation`; runtime entity classification uses the supported registry holder wrapper, tests centralize the supported `makeMockServerPlayer(GameType)` path, and no broad suppression or dependency-warning `-Werror` policy was introduced.
- Hardened playable-JAR validation against any nested JAR, common shaded dependency/test/fixture namespaces, missing required entrypoint/mixin/tag assets, and root-level source, configuration, log, world, cache, or release-bundle leaks. Deterministic source validation separately requires the complete public-support and source asset set, including the restored standard checksum-pinned Gradle 9.5.1 wrapper JAR/scripts.

### Verification status

**Final automated hardening evidence passes.** All five validators accept 103 production Java sources and the deterministic 224-entry source set; the dependency-free core runner passes 90 assertions; mapped JUnit passes 125 tests in 21 suites; all 22 required dedicated GameTests pass; the real client GameTest passes in 38 seconds; and the no-build-cache Java 25 clean build passes in 32 seconds. A standalone server without Mod Menu reaches `Done (0.313s)`, runs both validation forms successfully from the console, stops cleanly, and passes in 45 seconds. A Mod Menu 20.0.0 client reaches full resource/audio/atlas initialization before intentional interruption; that is a startup smoke, not a manual GUI result.

- **Final playable artifact:** `build/libs/smart-resource-drops-1.1.0.jar`, 483,638 bytes, 272 entries, Java class major 69, SHA-256 `E3A6A38ADB3412F081ED546089C0E61C932AC8E0D507AB5B33F9A94A5DF66EBA`. Required hardening classes are present and no fixture/testmod/development-loot/bundled-dependency/nested-JAR/runtime-data leak was found.
- The source tree is not a Git checkout, so no commit, tag, remote, or prior public 1.1.0 release can be verified. Version 1.1.0 and empty contact metadata are retained; no release was created.

- Coverage accounting is behavioral rather than method-count based: the pre-final 20/20 dedicated-runner checkpoint exercised many requested behaviors inside shared methods, but did not by itself prove all 54. That checkpoint added untagged class-fallback deaths, real mob pickup, mapped Looting/killed-by-player output, component-rich `1x`, same-list/two-wrapper and player-exclusion contexts, category-priority death, independent item/XP combinations, complete entity GUI draft copy/equality/apply/discard paths, and stronger playable-JAR leak checks.

- **Historical pre-final checkpoint:** all static/package/UI validators passed with 90 production Java sources, a deterministic 198-entry source archive, generic nested/shaded dependency and required-resource rejection, Mod Menu integration, edge-case contracts, polish regressions, and 90 dependency-free core assertions.
- **Historical pre-final checkpoint:** the cache-free exact clean build succeeded in 35 seconds. JUnit XML reported 16 suites and 92/92 tests; the dedicated runner passed all 20 required GameTests; the real client GameTest succeeded in 39 seconds; and the standalone server reached `Done (0.436s)`, stopped cleanly, and succeeded in 32 seconds. A standalone client startup smoke reached full resource/audio/texture initialization before intentional interruption.
- **Historical pre-final artifact only:** `build/libs/smart-resource-drops-1.1.0.jar`, 426,260 bytes, 242 entries, Java class major 69, SHA-256 `6AC0002F1615AB2FFF6248DFABE356A50EC65825D59190A57B9AE219DD3A1866`. This is preserved for traceability and is not the current final release artifact.
- The standalone interactive client/in-world flow, the 46-case player-driven vanilla matrix, hands-on block-budget fallback/warning observation, manual unknown/conflicting/near-limit validation scenarios, datapack `/reload`, running-game restart/migration, dense-farm observation, separately installed multiplayer checks, and named/versioned third-party biome/hostile/boss/inventory matrix remain **pending**. Automated fixtures are not a generic mod-compatibility claim.

## 1.0.3 - 2026-08-29

### Public release cleanup (2026-08-30)

- Fixed `PLAYER_PLACED_ONLY` when Smart Placement Protection is disabled. Natural blocks are now always excluded in that source mode; known player-placed blocks remain eligible. `NATURAL_ONLY` still treats placed blocks as eligible when protection is disabled, and `ALL` admits both sources.
- Added `minecraft:dragon_egg` to the removable default safety blacklist and the default example configuration. It is not hard-coded: deleting the entry returns the Dragon Egg to ordinary rule resolution.
- Added a subtle shared-session **Unsaved changes** footer indicator to every child/focused configuration screen. It is hidden for clean drafts, survives navigation, clears after Apply/Discard/reset, and stacks safely above Back at compact sizes.
- Kept **Apply Changes** exclusively on General. Child edits continue to build one shared staged draft, root Apply sends one bounded `ConfigPatch`, and dirty root **Done** continues to become **Discard Changes**.
- Corrected the three source/protection tooltips and documented the complete source-mode matrix.
- Hardened public-source packaging: generated/runtime/cache paths are ignored and rejected, required wrapper files are preserved, the source manifest/ZIP is deterministic, symlinks and unsafe names are rejected, CI exercises the packaging contract, and tagged releases publish the validated deterministic bundle rather than promising an artifact the workflow never created.
- Left Fabric contact metadata empty because this source tree has no verified project-owned GitHub URL. The public release checklist now identifies the exact `homepage`, `sources`, and `issues` fields to populate after the canonical URL is established.
- Added a documentation-only [1.1 mob/entity-drop design](docs/ROADMAP.md). Version 1.0.x remains block-resource-only and contains no mob loot mixin, config key, payload, or XP setting.
- Added regression coverage for the complete source-mode/protection matrix, removable Dragon Egg behavior, child dirty-state navigation, Apply/Discard/reset behavior, compact layout, and clean source packaging. Current measured totals and final artifact identity are maintained in `BUILD_STATUS.md` rather than duplicated here.
- Verified the real client GameTest in Minecraft 26.2, a standalone Mod Menu 20.0.0 Configure flow across every major child screen, and a standalone dedicated server without Mod Menu. Remaining release gates are the separately installed multiplayer/gameplay compatibility matrix and true restart/chunk-reload provenance checks.
- Rate-limited full-snapshot replies to unauthorized, stale, or cooldown-rejected reset packets. Successful command/console resets now clear queued GUI patches and immediately invalidate all open editors, matching the network reset path.

### Block inspection diagnostic (validated 2026-08-30)

- Added `/smartdrops inspect` and `/smartdrops inspect verbose` for normal players. The server performs the looked-at-block raycast using the player's current interaction range, preserves the exact namespaced ID for vanilla or modded blocks, and returns a clear message for sky/no-target or console invocation rather than trusting a client-supplied block ID.
- Inspection reuses the gameplay resolver's immutable `RuleResolutionTrace`. The compact view summarizes classification, provenance, filtering, the selected rule, and the effective player-mining result; verbose output exposes the complete player/block/category/dimension/global chain, every matched category in gameplay order, source toggles, and separate block-entity protection/allowlist booleans without reading NBT, inventories, owner data, or other sensitive contents.
- The diagnostic path is read-only: it does not break blocks, evaluate loot, spawn drops or XP, alter the world or configuration, increment statistics, or add/remove provenance. `PlacementTracker.peekPlaced` and the recent-removal cache's non-consuming peek preserve the result of a later real break even after repeated inspection.
- Category diagnostics use the same bounded tags already associated with the targeted block, including vanilla and common `c:` tags. Properly tagged modded blocks therefore inherit the ordinary categories and resolution order, while an untagged modded block reports the Miscellaneous fallback and still receives the applicable dimension or global rule.
- Inspection-specific JUnit coverage exercises command parsing and bounded/privacy-safe formatting, the trace precedence/filter/source/provenance/block-entity matrix, `0x`/`1x`/`64x`, multiple categories, and non-consuming recent-removal lookup. Dedicated-server GameTests repeatedly inspect real stone and a tracked, populated chest, check stable traces and gameplay-decision parity, preserve configuration revision, statistics, block state, block-entity identity/contents, and persistent provenance, and execute the real command dispatcher for looked-at stone, sky/no-target guidance, and console rejection.
- Final inspection validation passed: all five Python validators accepted 64 production Java sources and a deterministic 132-entry source archive; the dependency-free core harness passed all 90 assertions; JUnit XML reported 13 suites and 63/63 tests, including 12/12 focused trace tests and 6/6 command tests; all 8/8 required dedicated-server GameTests passed; the clean build succeeded in 17 seconds; the real client GameTest succeeded in 34 seconds; and a standalone server without Mod Menu reached `Done`, stopped cleanly, and completed in 33 seconds without a client-only classloading failure.
- Final playable artifact: `build/libs/smart-resource-drops-1.0.3.jar`, 291,391 bytes, 173 entries, SHA-256 `1F4FBF54C13C544909BA61F0800FFB76C073E2ED04EB0F95FF10B33849565A1B`. A standalone `runClient` startup smoke reached full resource loading with the mod initialized before it was intentionally stopped; the interactive in-world inspection checklist and the third-party modded block/block-entity/dimension compatibility matrix were not performed and remain manual release gates.

### Reset All Settings checkpoint (2026-08-29)

- Added a full-width **Reset All Settings** button in its own separated root-screen section above the normal Apply/Done actions. It opens Minecraft's vanilla confirmation screen, where the destructive **Reset Everything** action is red.
- Opening the confirmation is non-mutating. Cancel and Escape return to the exact same root screen and editor session, preserving every staged value and navigation/search state.
- Confirmation replaces the complete serialized configuration with the exact result of `SmartDropsConfig.defaults()`; no field, collection, override, filter, preset, or advanced option is reset from a second UI-maintained defaults list.
- Local defaults reset through one atomic persist-before-publish transaction. Integrated and dedicated-server resets are authoritative server operations with permission and expected-revision validation before the same single logical reset is committed.
- A successful reset invalidates all staged edits and pending command, request, and patch queues. Stale queued work is rejected, other open editors are invalidated, and a short reset cooldown prevents repeated disk-write spam.
- Placed-block provenance and accumulated session statistics/history are intentionally preserved. They are runtime/world data, not configuration; the serialized setting that enables statistics still returns to its default.
- Final reset verification passed: package validation counted 62 production Java sources; all 47 JUnit tests, 42 dependency-free core assertions, and 6/6 server GameTests passed; the clean release build succeeded; and the full real client GameTest passed in 34 seconds across local, integrated, dedicated operator/non-operator, and stale-queue flows.
- Reset checkpoint artifact: `build/libs/smart-resource-drops-1.0.3.jar`, 269,733 bytes, SHA-256 `4e0614b0735f87383968278d9eacec9185ac01e40fb8fa8ae39779fb0924797d`.
- Remaining manual-only coverage is limited to a true JVM/world restart with provenance chunk reload, interoperability between separately installed multiplayer instances, and human inspection of GUI scale and keyboard/mouse focus behavior. No separate manual `runClient` result is claimed for this feature.

### GUI redesign (2026-08-29)

- Replaced the flat, paginated configuration screen with a task-oriented hierarchy. The previous screen mixed common settings with hundreds of category, block, dimension, filter, and preset rows, which made ordinary changes difficult to find and encouraged large button/page rebuilds.
- The landing screen is now **General**, with the global multiplier, placement protection, multiplier source, and XP controls followed by navigation to **Categories**, **Block Overrides**, **Dimensions**, **Filters**, and **Advanced**. Categories, blocks, and dimensions open focused editors that show configured, inherited, and effective values.
- All screens in the hierarchy share one staged editor session. Child navigation does not request another snapshot or discard drafts, Apply is disabled until the session is dirty, and one Apply still produces one bounded patch. Server acknowledgement remains authoritative; local defaults still use the same atomic patch persistence.
- Block Overrides is search-first: an empty query shows configured block overrides rather than enumerating the registry, while a non-empty query matches translated display names and namespaced IDs. Broad queries are capped at 200 displayed matches and ask the user to refine the search.
- Replaced per-row buttons and manual pagination with lightweight `ObjectSelectionList` entries. The General screen does not build the registry catalogue; block/tag metadata is created lazily only when a catalogue-backed screen is opened. Search memoization now uses bounded 32-entry caches, filter results are capped before row allocation, only visible rows render, and the dimension list is built dynamically from vanilla, configured, current, and connected dimensions.
- Fixed title-screen/local category membership by adding `ClientCategoryTagIndex`, which resolves the installed category tag JSON without requiring a connected world's runtime tag bindings. The real client test now finds all 44 vanilla Logs/Wood blocks in the populated category.
- Search text and scroll position survive returning from a focused editor, compact-height controls no longer collide, a dirty root makes the exit action explicit as **Discard Changes**, and child screens now show the shared draft's subtle **Unsaved changes** state near Back.
- Non-operators retain the complete hierarchy in read-only form. The server-authoritative networking lifecycle, permission checks, patch acknowledgement behavior, and on-disk configuration format are unchanged.
- Final redesign verification passed: package validation counted 59 Java sources; the edge-case, polish, and hierarchical Mod Menu static validators passed; the Windows core runner passed all 42 assertions; `gradlew --no-daemon clean test runGameTest build` completed successfully with all five required server GameTests; and `gradlew --no-daemon runClientGameTest` completed successfully.
- The final client suite covers General protection/source/XP round trips; the populated 44-block Logs category; display-name and ID searches for diamond, exact `minecraft:diamond_ore`, acacia, and a broad-query 200-row cap; focused category and block editors; `0x` versus inheritance; retained search and scroll state; exact block-filter add/remove/revert; non-mutating preset preview; and local, integrated-server, dedicated-server, Apply, acknowledgement, and permission authority paths. The latest screenshots show the 44-block Logs category and the diamond search results; an earlier real Mod Menu-enabled `runClient` also succeeded and its screenshots were visually inspected.
- GUI-redesign checkpoint artifact: `build/libs/smart-resource-drops-1.0.3.jar`, 256,580 bytes, SHA-256 `7ea0dd2dc48143756ceb6e2535f03941a7e2bb8d37102bd02556057337716f49`.

### Added

- JUnit 5 regression tests against the mapped Minecraft classes for namespaced command parsing, configuration preservation, exact loot targeting, component-safe stack consolidation, provenance transitions, and the bounded recent-removal cache.
- Five Resource Multiplier dedicated-server GameTest scenarios that audit required mixins and server-only class loading, exercise real `Level.setBlock` provenance transitions, verify that a real door placement marks both halves, drive the real stone drop path at natural `2x`, placed `1x`, `0x`, and aggregated `64x`, and prove a full configuration reset preserves real provenance data.
- A Windows-native Java 25 core-rule test runner matching the existing POSIX runner.
- Additional lightweight GUI views for dimension overrides, operational server toggles, and staged presets. Block/filter search now matches translated block names as well as namespaced IDs.
- Explicit request-lifecycle, screen-open policy, adaptive-layout, and re-entrant queue-drain tests, plus a Fabric client GameTest covering title-screen local defaults, cached READY construction, the full child hierarchy, shared dirty-state navigation, root Apply/Discard, reset confirmation, integrated-server Apply/reset, dedicated-server read-only/operator states, permission refresh, unauthorized-patch promotion safety, and queued-patch disconnect cleanup.

### Fixed

- Fixed Mod Menu Configure opening as a black screen containing only Retry and Back. The title-screen path had always attempted server sync with no connection, Minecraft 26.2 discarded every custom 24-bit text color as alpha zero, and both screens incorrectly forced the in-world background.
- Title-screen Configure now opens the complete vanilla-like local/default editor and saves through the existing atomic config transaction. Connected singleplayer and multiplayer always request authoritative server values and never fall back to the local file.
- Loading, ready, error, retry, timeout, disconnect, invalid-response, and stale-response handling now use explicit state and connection-bound request IDs. Responses are applied on the client thread only while the originating loading screen is still current; screen references are no longer held globally.
- The integrated-server owner can edit without enabling cheats. Dedicated-server operators receive editable controls and non-operators receive the same complete screen read-only; closing a connected screen invalidates cached values and permission state before the next open.
- `/smartdropsgui` now reaches the same shared route after chat closes without being immediately removed, and its delayed callback refuses to replace another connection or unrelated screen.
- All custom GUI text uses opaque ARGB colors, menu/world backgrounds are selected dynamically, long status text is bounded, inherited block rows show their effective multiplier, mouse-wheel pagination works, and adaptive layout invariants cover common compact GUI sizes.
- Invalid authoritative JSON enters a visible error state instead of becoming editable defaults. Server patches now reject malformed and out-of-range scalar or map multipliers rather than clamping client input.
- Queued client actions are removed before execution so a send-failure callback can safely cancel/re-enter the queue without invalidating its iterator.
- Loot sessions now claim only the exact level, block position, and block state that opened the session. Nested or re-entrant loot calculation for another block can no longer consume the outer block's multiplier or XP context.
- Fabric fake players are classified as automation instead of ordinary players, so `automatedMining` remains the controlling opt-in.
- XP multiplication now requires the same level and position as the qualifying block drop.
- Identical component-bearing loot stacks are consolidated and split only at the item's legal maximum stack size.
- Successful `BlockItem` placement uses a nested transaction and records every changed position only after the placement succeeds. The older broad placement hooks were removed.
- Provenance is preserved across resource-identity transformations such as stripping logs, soil changes, copper weathering or waxing, concrete-powder hydration, coral changes, and cauldron changes. Unrelated replacement clears provenance, while crop and similar growth is treated as generated rather than permanently contaminated.
- The remove-before-drop cache is bounded to 4,096 entries per level, expires incrementally after two ticks, clears on clock rollback, and fails closed for the short window if it overflows.
- Namespaced block and tag list commands now consume the full identifier, validate it, and accept optional leading `#` markers for tags.
- JSON tag filters normalize optional leading `#` markers.
- A missing blacklist installs the safety defaults, while an explicit empty blacklist remains empty.
- Config files with a newer schema are left untouched and run with safe in-memory defaults. Malformed files are replaced only after the original is successfully preserved as a timestamped backup; read failures suppress writes.
- Oversized hand-edited rule collections preserve the complete original in a timestamped backup before bounded sanitization; a backup failure leaves the active file untouched.
- No-op command updates no longer rewrite the config file.
- Client config snapshots use request IDs and connection identity so stale responses cannot open or replace a screen. Snapshot requests are rate-limited and cached server-side, with a 1 MiB payload limit.
- GUI Apply now sends one permission-checked delta containing only explicitly edited fields. The server validates it, performs one atomic config write, and returns an applied/rejected/unauthorized acknowledgement with the authoritative snapshot.
- Patch throttling now covers authorized operators as well as unauthorized senders. A rate-limited authorized Apply keeps only the newest bounded patch and processes it after the cooldown instead of silently timing out; unauthorized payloads are never retained for a later permission change. Repeatedly selecting Custom with no staged preset no longer discards unsaved multiplier edits.
- Admin and personal commands now publish configuration changes only after the atomic file replacement succeeds and report persistence failures instead of leaving misleading in-memory-only settings. Reload also reports protected/read/persistence failures instead of unconditional success. A command that would exceed the 2,048-rule cap is rejected before sanitation, so it cannot displace another rule.
- Global/XP multiplier text and the GUI search query now survive window, resolution, and fullscreen rebuilds.
- Rule collections are syntax-validated and capped at 2,048 total entries. Tag filtering now iterates the block's actual tags instead of every configured tag.
- Placement, loot, explosion, level-change, and falling-block correlation hooks now use exception-safe MixinExtras wrappers so a caught exceptional exit cannot strand thread-local state.

### Build and packaging

- Pinned Fabric Loom to stable `1.17.20`, JUnit to `5.14.2`, and declared the Fabric API minimum in generated metadata instead of using a wildcard dependency.
- Kept Mod Menu compile-only and isolated from the dedicated-server runtime while retaining the optional client entrypoint.
- The Gradle bootstrap now always uses the checksum-pinned Gradle `9.5.1` distribution. The Windows checksum path uses the .NET SHA-256 implementation and was verified with Java 25.
- CI and release workflows now run metadata, Mod Menu, edge-case, polish, and deterministic source-packaging checks, the core rules suite, JUnit tests, dedicated-server GameTests, the client GameTest, and the Loom build. Release tags must match `mod_version`.
- Release packaging rejects stale JARs, verifies the version embedded in `fabric.mod.json`, rejects generated/runtime/cache entries, and preserves only the required Gradle wrapper sources.

### GUI-redesign verification status (historical checkpoint)

- All 42 JUnit tests passed locally.
- The dedicated-server runner reported all five required GameTests passing (four Resource Multiplier methods plus the Fabric runner's required count), including the real loot-pipeline, mixin-audit, and optional Mod Menu isolation checks.
- The final Fabric client GameTest passed in a real Minecraft 26.2 client. In addition to local defaults, cached READY construction, integrated-server round trips, dedicated non-op/operator presentation, permission refresh, unauthorized-patch promotion safety, and queued-patch disconnect cleanup, it verified the hierarchical lists/editors, populated client-side category resolution, search limits and retention, `0x` versus inheritance, and dirty/clean Apply behavior.
- `validate_package` passed with 59 Java sources. The edge-case source audit, polish regression suite, hierarchical Mod Menu static validator, and all 42 Windows core-rule assertions passed. A normal standalone dedicated server without Mod Menu reached `Done` and stopped cleanly.
- A Mod Menu 20.0.0 `runClient` launch displayed the complete title-screen configuration editor and its screenshots were inspected. That GUI-redesign checkpoint JAR was 256,580 bytes with SHA-256 `7ea0dd2dc48143756ceb6e2535f03941a7e2bb8d37102bd02556057337716f49`.
- The automated suite does not substitute for an actual third-party modded block/dimension fixture or the complete separately installed multiplayer and manual GUI-scale matrix. Tag filters are intentionally visible/read-only in the GUI because the existing `ConfigPatch` has no tag-filter edit fields; see `docs/TESTING.md` and `docs/PUBLIC_RELEASE_CHECKLIST.md`.

## 1.0.2 - 2026-08-29

### Added

- Native Mod Menu integration through the optional `modmenu` entrypoint.
- A Mod Menu `Configure` button that opened the then-named Smart Resource Drops server-authoritative settings flow. The current public name is Resource Multiplier.
- Lightweight loading, retry, timeout, and no-world states for config access from Mod Menu.
- Mod Menu name, summary, and description translation metadata.

### Fixed

- Removed the obsolete `#minecraft:tall_flowers` block-tag reference from the plants category for Minecraft 26.2. `#minecraft:flowers` already includes the tall flower blocks in 26.2.
- Late configuration snapshots no longer reopen the settings screen after the player backs out.
- Queued GUI changes are flushed before the client requests a refreshed server snapshot.
- Rebuilding the loading screen after a window resize no longer sends a duplicate configuration request.

### Compatibility

- Mod Menu 20.0.0 for Minecraft 26.2 is an optional integration, not a runtime requirement.
- Dedicated servers do not need Mod Menu installed.

## 1.0.1 - 2026-08-29

### Added

- Configurable `0x` through `64x` block loot multipliers
- Persistent smart placement protection
- Block, category, dimension, and global override hierarchy
- Blacklist and whitelist filters for blocks and tags
- Fortune, Silk Touch, NBT, data-pack, and modded loot-table compatibility through final loot-list multiplication
- Stack-count consolidation to keep high multipliers lighter on item entities
- Piston and falling-block provenance transfer
- Safe block-entity defaults
- Independent XP multiplier
- Explosion and opt-in automation support
- Server-authoritative commands and capped personal overrides
- Searchable client configuration screen with live server snapshots, read-only non-operator access, category editing, and exact filter editing
- Presets, statistics, tests, CI, release workflow, and complete documentation
- Responsive narrow-screen GUI layout and canonical command identifiers
- Interface-backed piston provenance transfer for runtime-safe mixin access
