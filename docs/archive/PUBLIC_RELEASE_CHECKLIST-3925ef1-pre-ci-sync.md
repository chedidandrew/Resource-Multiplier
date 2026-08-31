# Resource Multiplier public release checklist

## 1.2.0-rc.1 release candidate

This section applies only to the exact `1.2.0-rc.1` candidate. An unchecked item is still pending, and historical evidence below does not satisfy a current gate. Keep `release_ready=false`; do not create a tag or release while any required gate remains open.

### Automated and artifact gates

- [x] Run every repository validator and lightweight Markdown/link check: package/README metadata validates 118 production Java sources; release packaging validates 252 Git-tracked source entries; Mod Menu, edge-case, and polish checks pass; every local README target resolves and every referenced GitHub/badge URL returns HTTP 200.
- [x] Run the full mapped Java 25 suite serially: JUnit passes 158 tests in 24 suites with zero failures/errors/skips, and all 66 required dedicated-server GameTests pass during the 29-second clean build.
- [x] Run the real client GameTest with Mod Menu 20.0.0 present: branding, navigation, reset, dirty-state, authority, search, and mapped tooltip checks pass in 39 seconds, and 19 current screenshots are captured.
- [x] Complete `gradlew.bat --no-daemon --no-build-cache --rerun-tasks clean build --warning-mode all`: `BUILD SUCCESSFUL` in 29 seconds with warnings visible and no shared Gradle race.
- [x] Start standalone `runServer` with 43 mods and no Mod Menu: Resource Multiplier reaches `Done (0.247s)`, `/smartdrops`, shearing status, and `/smartdrops validate` return expected output, no data is changed, and `stop` completes cleanly in 48 seconds without client-only class loading.
- [x] The passing mapped configuration suite covers schema 2→3 preservation with both shearing sources initialized OFF, schema 1, malformed recovery, future schema, fresh install, and Reset semantics.
- [x] Re-audit backward compatibility: the task changes no production source/resource namespace, and validation plus JAR inspection retain `smart_resource_drops`, the Java/config/datapack/network/provenance identifiers, both command roots, schema, and migration behavior.
- [x] Validate the Git-tracked source-package contract, deterministic archive modes, secret/runtime/generated-data deny rules, required documentation and screenshots, and fresh-empty-output requirement; all packaging regressions pass with 252 source entries.
- [x] Inspect `build/libs/resource-multiplier-1.2.0-rc.1.jar`: 601,044 bytes, 311 entries, SHA-256 `5CA797D6BC4BBAB6F223361D9A99A118850F40B0273426D3B1ADF8AAB5CDCB31`, embedded 1.2.0-rc.1/Resource Multiplier/MIT/contact/dependencies/icon/tags, Java class major 69, and zero nested JAR or GameTest entries.
- [x] Build the source archive, playable JAR, checksum manifest, and five-entry release bundle in a fresh empty temporary directory; exact checksums are stored beside those generated artifacts rather than made self-referential in the source tree.
- [x] Verify the ordinary build workflow handles branch pushes, pull requests, and manual runs, while the guarded release workflow is the sole `v*` tag handler and checks `release_ready=true` before build/package/publication; the current false latch keeps publishing locked.
- [ ] Confirm the exact candidate commit passes the required clean-checkout GitHub Actions workflow. Do not create a tag merely to obtain this evidence.

### Manual gameplay and operations gates

- [ ] Verify real sheep player/dispenser output at disabled, `0x`, `1x`, `2x`, and `64x`, including color/components, legal stacks, state/tool/sound/event exactly once, regrowth, and complete vanilla `1x` budget fallback.
- [ ] Verify Mooshroom, Snow Golem, Bogged, Copper Golem, and Sulfur Cube remain fixed vanilla `1x`, including exact-override and conflicting-standard-tag attempts.
- [ ] Verify leaves, vines, cobwebs, beehive dispenser action, dispenser/player leash removal, existing block loot, and entity death loot remain outside the shearing subsystem.
- [ ] Exercise representative block and entity drops in a real world at `0x`, `1x`, ordinary multiplied values, and the configured cap; include Fortune/Looting, Silk Touch, explosions, XP, protected drops, placed-block provenance, restart persistence, and full fallback behavior.
- [ ] Verify all configuration screens at 1280×720 and common GUI scales, including General, block/entity/category/shearing views, search, inheritance, shared dirty state, Back, Apply, Discard, Reset All, keyboard/mouse focus, concise wrapped tooltips, and non-operator read-only authority.
- [ ] On an actual dedicated server, exercise `/smartdrops`, `/smartdropsgui`, shearing status and operator mutations, block/entity inspection, and compact/verbose validation; confirm non-operators cannot write settings.
- [ ] Verify `/reload` updates current block/entity/shearing tag classification without restart, preserves unknown configuration references, and reports no missing production tags.
- [ ] Load representative existing 1.0.x/1.1.0 configs and saved-world provenance data, restart, and confirm the migration is compatible and lossless; separately test malformed, unreadable, and future-schema files.
- [ ] Repeat operator and non-operator Apply/Discard/Reset/stale-patch/reconnect checks against a separately installed multiplayer server, including a client without Mod Menu.
- [ ] Observe a dense farm and high-output block/shearing scenario for duplicates, partial fallback output, leaked contexts, excessive entities/orbs, warning spam, or unbounded memory/CPU behavior.
- [ ] Inspect all public screenshots and logs for usernames, server addresses, tokens, filesystem paths, world names, or other private data before publication.

### Named third-party compatibility gates

Record a real project name and exact installed version. A synthetic fixture, generic category, or "latest" is not evidence. Every row remains pending until its dated result and limitation are filled in.

| Category | Mod/project name | Exact version | Test date | Resource Multiplier version | Result | Limitation |
| --- | --- | --- | --- | --- | --- | --- |
| Biome/passive-animal content | Not selected | Pending | Pending | 1.2.0-rc.1 | Not performed | Pending |
| Hostile mob | Not selected | Pending | Pending | 1.2.0-rc.1 | Not performed | Pending |
| Boss | Not selected | Pending | Pending | 1.2.0-rc.1 | Not performed | Pending |
| Inventory-carrying mob | Not selected | Pending | Pending | 1.2.0-rc.1 | Not performed | Pending |
| Custom shearable | Not selected | Pending | Pending | 1.2.0-rc.1 | Not performed | Pending |
| Automated miner | Not selected | Pending | Pending | 1.2.0-rc.1 | Not performed | Pending |
| Custom placement | Not selected | Pending | Pending | 1.2.0-rc.1 | Not performed | Pending |

No generalized third-party compatibility statement is permitted until the relevant rows contain reproducible evidence.

### Final promotion gate

- [ ] Confirm every automated/artifact, manual gameplay/operations, and named third-party compatibility gate above is complete against one exact commit and candidate artifact.
- [ ] Only then change `mod_version` from `1.2.0-rc.1` to `1.2.0`, rerun the entire validation/build/runtime/artifact sequence, and record the final release artifact identities.
- [ ] Set `release_ready=true` only in that exact fully tested `1.2.0` commit, merge it to `main`, wait for the required clean-checkout workflow, and create the publication tag only after all checks pass.

Until all promotion steps are complete, keep the latch false and describe this build only as an unpublished release candidate.

## 1.1.0 final hardening release evidence under the former public name

The serialized post-hardening automated run and artifact inspection below completed on 2026-08-30. Checked entries are automated/runtime evidence only; they do not satisfy the separate manual or third-party gates.

### Automated and artifact evidence

- [x] Run every static/package/UI validator, the applicable Windows dependency-free core runner, mapped JUnit, all required dedicated GameTests, the real client GameTest, and the exact clean Java 25 build with deprecation warnings visible: validators 103/224, core 90, JUnit 125 in 21 suites, GameTests 22, client 38 seconds, clean build 32 seconds.
- [x] Confirm `/smartdrops validate [verbose]` permission/console behavior, live configured-entry checks, 15/100 output caps, unknown-reference preservation, bounded load diagnostics/UUID privacy, and no configuration/revision/file mutation through mapped tests, GameTests, and standalone console execution.
- [x] Confirm the complete block-list preflight at 262,144 items/4,096 stacks, saturated overflow handling, unchanged-list `1x` fallback, player/explosion/automation parity, statistics accounting, and the five-minute/256-key warning limiter through mapped tests and the dedicated fixture.
- [x] Start a standalone dedicated server without Mod Menu: it reaches `Done (0.313s)`, runs both validation forms, stops cleanly, and passes in 45 seconds without client-only loading. The real client GameTest passes separately in 38 seconds.
- [x] Inspect the final playable JAR: embedded version 1.1.0, class major 69, 483,638 bytes, 272 entries, SHA-256 `E3A6A38ADB3412F081ED546089C0E61C932AC8E0D507AB5B33F9A94A5DF66EBA`, with no forbidden nested/shaded/test/fixture/development/runtime content.
- [x] Confirm every declared entrypoint and mixin exists; required block/entity/protected-item tags are present; and no required runtime resource is missing.
- [x] Verify the standard checksum-pinned Gradle 9.5.1 wrapper JAR/scripts and deterministic 224-entry source set with required wrapper, issue forms/config, uppercase PR template, scope/security/release docs, protected tag, tests/fixtures, and key sources.
- [x] Record the final runtime/build/JAR evidence in `BUILD_STATUS.md`. The generated `SmartResourceDrops-1.1.0-SHA256SUMS.txt` records the exact JAR and source-archive hashes without making the source archive self-referential.

### Manual vanilla and operations gates

- [ ] Use both validation forms as an operator and from console with valid, unknown, conflicting, near-limit, and retained-load-diagnostic configurations; verify unknown entries survive save/reload unchanged and no UUID value is disclosed.
- [ ] In a real world, trigger a pathological block-loot fallback and verify the full vanilla `1x` result, no partial output, fallback statistic, detailed first warning, five-minute repeat suppression, and continued safe behavior under player, explosion, and supported automation paths.
- [ ] Complete a standalone interactive client/in-world command, block/entity inspection, mining/death, GUI, and Mod Menu-absent flow. Startup/resource loading or a client GameTest alone is insufficient.
- [ ] Complete custom-tag `/reload`, live 1.0.x/schema migration and restart, dense farm, separately installed operator/non-operator multiplayer, and all 46 player-driven vanilla cases documented in `TESTING.md`.

### Named third-party compatibility evidence

Record a real project name and exact installed version. A synthetic fixture, generic category, or “latest” is not evidence. Every row remains pending until its dated result and limitation are filled in.

| Category | Mod/project name | Exact version | Test date | Resource Multiplier version | Result | Limitation |
| --- | --- | --- | --- | --- | --- | --- |
| Biome/wildlife or passive entity | Not selected | Pending | Pending | 1.1.0 candidate | Not performed | Pending |
| Hostile entity | Not selected | Pending | Pending | 1.1.0 candidate | Not performed | Pending |
| Boss | Not selected | Pending | Pending | 1.1.0 candidate | Not performed | Pending |
| Equipment/inventory entity | Not selected | Pending | Pending | 1.1.0 candidate | Not performed | Pending |
| Automated miner | Not selected | Pending | Pending | 1.1.0 candidate | Not performed | Pending |
| Custom placement/drop integration | Not selected | Pending | Pending | 1.1.0 candidate | Not performed | Pending |

No generalized third-party compatibility statement is permitted until the relevant rows contain reproducible evidence.

## 1.1.0 entity feature gate under the former public name (historical pre-hardening checkpoint)

The checked results in this section are preserved for the earlier measured tree. They predate the final validation, block-budget, deprecation, template, wrapper, and package/source hardening above and do not satisfy the current final gate.

Completed automated evidence:

- [x] Package/static validation accepts the schema 2 fields, nine `replace: false` production entity category tags, mapped Minecraft 26.2 vanilla IDs, protected saddle/totem policy, testmod metadata, and deterministic fixture matrix.
- [x] Deterministic source packaging passes with 198 entries; the complete source archive intentionally includes tests and fixtures.
- [x] Release packaging has synthetic regressions that reject GameTest/testmod/fixture classes, development entity loot tables, bundled Fabric/Minecraft classes, and runtime/server/world data from a playable JAR.

Still required before publication:

- [x] Re-run the complete serialized validators, JUnit, dedicated GameTests, client GameTest, cache-free clean build, standalone startup checks, and artifact audit after the verification-hardening changes. The strengthened nested-JAR/shaded-dependency/entrypoint/mixin/tag/source-leak regressions pass.

- [x] Run the complete static/core/JUnit/dedicated GameTest/build sequence on Java 25: 90 core assertions, 92/92 JUnit tests in 16 suites, 20/20 required GameTests, and a 35-second cache-free exact clean build.
- [x] Run the real client GameTest and a standalone dedicated server without Mod Menu: client suite succeeds in 39 seconds; server reaches `Done (0.436s)`, stops cleanly, and succeeds in 32 seconds without client-class leakage.
- [ ] Run a standalone interactive client in a world. Startup/resource loading alone is not the manual matrix.
- [x] Inspect `build/libs/smart-resource-drops-1.1.0.jar`: 426,260 bytes, 242 entries, embedded 1.1.0, class major 69, SHA-256 `6AC0002F1615AB2FFF6248DFABE356A50EC65825D59190A57B9AE219DD3A1866`, and zero fixture/testmod/development-loot/bundled-dependency/runtime-data entries.
- [ ] Exercise `/reload` after changing a custom entity category tag and verify classification/inspection changes without stale cache or config mutation.
- [ ] Verify schema 1/1.0.x migration and restart persistence in a running client/server: all block fields preserved, entity feature OFF, safe entity defaults installed.
- [ ] Repeat operator/non-operator Entity Drops editing against a separately installed multiplayer server, including stale patch, unauthorized patch, Apply, Discard, Reset All, reconnect, and Mod Menu absent.
- [ ] Profile/observe a dense mob-farm scenario with the entity feature OFF and ON for duplicate drops, leaked contexts, orb contamination, or unbounded entity output.

Manual vanilla entity matrix:

- [ ] Cow with Entity Drops OFF and ON, Looting, Fire Aspect, fall damage, player-hit-then-fall, and tamed-wolf kill.
- [ ] Zombie equipment, Skeleton bow, Fox carried item, Allay carried inventory, and Donkey/Llama inventory; only normal death-table loot may multiply.
- [ ] Villager, Iron Golem, Wither Skeleton, Wither, Ender Dragon, Warden, Slime split, and Magma Cube split.
- [ ] Creeper killed by another mob, player-ignited TNT, and dispenser projectile under each relevant kill policy.
- [ ] Entity item multipliers `0x`, `1x`, and `64x`; mob XP OFF/ON; boss item/XP gates; and `doMobLoot=false`.
- [ ] Exact blacklist/whitelist, entity-type tag filters, individual override, category/default/global inheritance, reset, restart, and `/smartdrops inspect entity` compact/verbose/no-target behavior.

Third-party compatibility matrix (record exact mod and version; never generalize from one result):

- [ ] At least one biome/wildlife mod and one modded passive animal.
- [ ] At least one modded hostile entity.
- [ ] At least one boss-tagged modded boss with boss item/XP gates OFF and ON.
- [ ] At least one modded entity with equipment or inventory plus one datapack-added category classification.

The checked 1.0.3 items below are preserved historical evidence and do not satisfy these 1.1 gates.

## Automated

Verified locally during the 2026-08-30 public-release cleanup:

- [x] All five package, deterministic source-ZIP, Mod Menu, edge-case, and polish validators pass.
- [x] The source-package regression validates 129 source entries, excludes generated/runtime paths, retains the required Gradle wrapper files, and records executable mode for `gradlew`.
- [x] The Windows core runner passes all 62 assertions on Java 25, including the complete source-mode/protection/provenance matrix and removable default Dragon Egg exclusion.
- [x] Gradle JUnit passes all 47 mapped-Minecraft tests in 12 suites.
- [x] The dedicated-server GameTest run passes all six required tests reported by the runner. Resource Multiplier contributes five methods, including real stone drop-path checks at natural `2x`, placed `1x`, `0x`, and aggregated `64x`, plus reset/provenance coverage.
- [x] The client GameTest completes successfully in a real Minecraft 26.2 client, including clean/dirty child indication, dirty navigation, root Apply/Discard, Apply clearing, Reset Cancel/confirm semantics, and the compact 320x180 indicator layout as well as the existing hierarchy and authority paths.
- [x] Required mixins audit and Mod Menu classes are absent from the dedicated runtime.
- [x] A Mod Menu 20.0.0 manual `runClient` follows the historical title-screen Mods > former-name entry > Configure path before the rename, checks shared dirty state in every major child, confirms Apply clears it, and confirms the red Reset Everything workflow.
- [x] A standalone dedicated server without Mod Menu reaches `Done`, accepts `stop`, and finishes cleanly without loading client-only configuration classes.

Still required on the eventual source-control checkout and release artifact:

- [ ] Run the full command sequence in [TESTING.md](TESTING.md) on a clean checkout.
- [ ] Confirm GitHub Actions succeeds.
- [x] Rebuild and inspect the final `build/libs/smart-resource-drops-1.0.3.jar`; its current 169-entry, 272,738-byte identity and SHA-256 are recorded in `BUILD_STATUS.md`.
- [x] Confirm the final JAR's embedded `fabric.mod.json` reports version 1.0.3, the compiled class major is Java 25's 69, and no JUnit/GameTest, bundled Mod Menu/Fabric API, or nested dependency JAR content is present.
- [ ] Confirm the checksum published beside the release matches the uploaded JAR.
- [x] Generate and independently inspect the final deterministic source ZIP after the final clean build: 129 entries, one versioned top-level directory, required Gradle bootstrap files present, `gradlew` mode `755`, and no forbidden generated/runtime paths.
- [x] Restored Git in the canonical public checkout on 2026-08-30; `main` tracks `origin/main` at `chedidandrew/Resource-Multiplier`.
- [x] Verified the canonical public GitHub URL, replaced the placeholder upload instructions, and populated exact `homepage`, `sources`, and `issues` metadata with regression coverage.

## In-game smoke matrix

The dedicated-server GameTest exercises the underlying stone `dropResources` path, but these player-facing checks remain manual:

- [ ] Mine natural stone at `2x`, then place and mine the result at vanilla quantity.
- [ ] Place and break both halves of a door and bed, a tall plant, and a modded multi-block placement.
- [ ] Push and sticky-pull a placed block across a chunk border, then break it.
- [ ] Drop player-placed sand, gravel, and concrete powder through multiple blocks, then break the landing block.
- [ ] Save/restart during piston and falling movement; test chunk unload/reload and normal placed-block restart persistence.
- [ ] Transform placed copper, strip a placed log, make farmland and a dirt path, then verify provenance remains protected.
- [ ] Break natural ore with Fortune and Silk Touch at `0x`, `1x`, `2x`, `4x`, and `64x`; verify item components, entity aggregation, and XP disabled/enabled.
- [ ] Test TNT with explosion decay and `doTileDrops=false`.
- [ ] Test empty and populated chests, shulker boxes, beehives, decorated pots, and a representative modded block entity.
- [ ] Test canceled breaks from spawn protection or a claim mod and verify provenance is not consumed.
- [ ] Test a Fabric fake player and representative automated miner with automation disabled and enabled.
- [ ] Exercise blacklist/whitelist, tags, block/category/dimension/global priority, all three vanilla dimensions, and a modded standard-tag block.
- [ ] Corrupt the config and verify backup/recovery; separately test a future schema, unreadable file, and explicit empty blacklist.
- [x] From the title screen, use Mod Menu to open the editor and manually check Categories, Block Overrides, Dimensions, Filters, and Advanced with shared dirty state; return to General, Apply, confirm the clean state, and complete the Reset All Settings confirmation.
- [ ] Exercise the remaining failure/retry paths and the complete GUI-scale/window, keyboard, mouse-focus, and tooltip matrix. Automated coverage includes representative search/inheritance state, authority rejection, and compact 320x180 layout, but not the exhaustive human matrix.
- [ ] Repeat non-operator read-only and operator Apply checks against a separately installed multiplayer server. The embedded dedicated-server client test covers these authority states automatically.
- [ ] Type rapidly, close before the snapshot returns, and reconnect to another server; verify no stale screen or silent loss. Server cooldown-queued patch cleanup on disconnect is automated.
- [x] With Mod Menu 20.0.0 installed, verify the historical title-screen Mods > former-name entry > Configure navigation and local-default editor before the rename.
- [ ] Verify Mod Menu Configure while connected, then remove Mod Menu and confirm the command-opened screen still works.
- [ ] Join a server without Resource Multiplier and verify graceful sync-unavailable behavior.
- [x] Start a standalone dedicated server without Mod Menu and confirm normal initialization outside the GameTest harness.

A public release should not be tagged until the automated build and this smoke matrix both pass on the actual target Minecraft version.
