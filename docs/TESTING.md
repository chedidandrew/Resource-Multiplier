# Smart Resource Multiplier testing and verification

## 1.3.2+mc1.21.4 Fabric and NeoForge release

This branch provides separate Fabric and NeoForge artifacts for Minecraft 1.21.4 using Java 21. The loaders share gameplay, schema 3 configuration, commands, payload policy, and GUI, with loader-specific entrypoints, networking, fake-player handling, and placement storage. Minecraft 26.2 remains the newest/default release line on `main`; this tested commit has `release_ready=true`.

The General-screen labels **Multiply Block XP** and **Block XP Multiplier** now make their block-only scope explicit, while their tooltips point to the independent Mob XP controls under Entity Drops. This presentation-only clarification does not change configuration fields, defaults, or multiplier behavior and is shared by both loader builds.

The required validation sequence is:

```bash
python3 tools/validate_package.py
python3 scripts/test_release_packaging.py
./gradlew --no-daemon clean test runGameTest build
./gradlew --no-daemon runClientSmoke
./gradlew --no-daemon runPersistenceVerifyAbsentServerSmoke
bash tools/run_fabric_multiplayer_smoke.sh
./gradlew -p neoforge --no-daemon clean build runGameTestServer
python3 tools/validate_neoforge_jar.py
./gradlew -p neoforge --no-daemon runPersistenceVerifyAbsentServerTest
xvfb-run -a ./gradlew -p neoforge --no-daemon runClientCategoryTest
./gradlew -p neoforge --no-daemon runPackagedServerTest
xvfb-run -a ./gradlew -p neoforge --no-daemon runPackagedClientTest
bash tools/run_neoforge_multiplayer_smoke.sh
bash tools/run_neoforge_optional_channel_smoke.sh
bash tools/run_neoforge_oversized_wire_smoke.sh
```

The Fabric server gate must discover and complete exactly 64 tests, including target-native fake-player denial and shearing boundaries. `runClientSmoke` verifies wording, centered values, deduplicated tooltips, child navigation, non-empty Entity Categories, target classifications, search/configure behavior, and shared dirty/apply/reset state. The persistence and multiplayer runners prove save/restart behavior and server authority.

NeoForge 21.4 binds the shared tests to the binary `data/smart_resource_drops_gametest/structure/wide.nbt` template and must discover and execute exactly 64 tests. Release evidence also includes attachment persistence, production server and client starts, multiplayer, optional-installation, oversized-wire, and exact packaged-JAR gates.

Configuration files preserve their schema and identity. Minecraft world downgrades and cross-loader placed-block-data migration are unsupported; back up worlds before changing versions or loaders.

## 1.2.3 stable icon and toolchain release

Smart Resource Multiplier `1.2.3` is a branding-and-build stable release with `release_ready=true`. It packages the approved `512x512` production icon and declares a Java 25 Gradle toolchain with automatic detection and download fallback so a Java 21 shell can invoke the build safely. It does not change gameplay, mixin targets, configuration fields or defaults, schema, networking, permissions, persistence, or anti-duplication behavior.

The release commit must pass this clean-checkout sequence on Java 25 before publication:

```bash
python3 tools/validate_package.py
python3 scripts/test_release_packaging.py
python3 scripts/test_modmenu_integration.py
python3 scripts/test_structured_tooltip_composition.py
python3 scripts/edge_case_source_audit.py
python3 scripts/polish_regression_tests.py
bash tools/run_core_tests.sh
./gradlew --no-daemon clean test runGameTest build
xvfb-run -a ./gradlew --no-daemon runClientGameTest
```

The expected playable artifact is `build/libs/smart-resource-multiplier-1.2.3.jar`. Inspection must confirm public name `Smart Resource Multiplier`, version `1.2.3`, stable mod ID `smart_resource_drops`, the approved icon bytes, embedded MIT license, no nested JARs, and no GameTest classes or resources. The preserved release record is [`releases/1.2.3.md`](releases/1.2.3.md); `BUILD_STATUS.md` describes the current stable release rather than this historical artifact.

No new hands-on gameplay matrix is required because the gameplay implementation is unchanged. Runtime client and dedicated-server tests remain required because the visible title, reset prompts, command headers, metadata, and packaged artifact identity must be exercised through real runtime paths.

## Historical verification records

The remaining sections preserve the evidence captured for earlier public names and versions. Artifact names, hashes, versions, and release status in those sections are historical facts and should not be rewritten to match the current brand.


## 1.2.0 automated evidence and open release gates

Resource Multiplier `1.2.0` is an unpublished release candidate with `release_ready=false`. The current automated pass was run on Java 25 against Minecraft 26.2, Fabric Loader 0.19.3, Fabric API 0.158.0+26.2, and optional client-only Mod Menu 20.0.0. These results establish build and regression evidence; they do not replace hands-on gameplay, separately installed multiplayer, datapack-reload, or named third-party compatibility tests.

### Configuration clarity pass - local validation complete

Focused coverage confirms that the root buttons read **Block Categories** and **Block Filters**, their child-screen titles remain **Categories** and **Filters**, and the existing visible-label contracts remain unchanged. It preserves the previously detailed tooltip text; requires the new Dimensions, Advanced, and Entity Drops navigation help; requires both root XP descriptions to identify eligible block breaks; and requires meaningful, setting-specific help on all eight Advanced boolean rows without removing the visible ON/OFF state.

Runtime tooltip regressions prove shared `MultiplierControl` help on the label, value, minus button, and plus button; Minecraft's 170-pixel wrapping; on-screen placement at 320x180, 426x240, 640x360, and 1280x720; complete clipped-row information followed by supplemental details; and explanatory help on read-only controls. Authority coverage includes local defaults, integrated-server authority, and dedicated-server operator and non-operator paths, including non-mutation when read-only Advanced actions are invoked.

The local final chain passes all six repository validators, including `python scripts/test_structured_tooltip_composition.py`, the 90-assertion dependency-free core runner, mapped JUnit, the dedicated GameTest runtime, the real client GameTest with Mod Menu present, the cache-free Java 25 Loom build, standalone server checks, and fresh-empty-directory packaging. The client run recaptured only `docs/images/general-config.webp`; the Block Overrides and Shearing screenshots remain byte-for-byte unchanged because their visible content did not change. Exact clean-checkout GitHub Actions run `33354997205` also passes for production commit `cb6d17d9a26283ddc98c6e3f7d8fa46ebb341380`.

The exact local sequence was:

```powershell
python tools/validate_package.py
python scripts/test_release_packaging.py
python scripts/test_modmenu_integration.py
python scripts/test_structured_tooltip_composition.py
python scripts/edge_case_source_audit.py
python scripts/polish_regression_tests.py
.\tools\run_core_tests.ps1
.\gradlew.bat --no-daemon --no-build-cache --rerun-tasks clean test runGameTest build --warning-mode all
.\gradlew.bat --no-daemon --no-build-cache --rerun-tasks runClientGameTest -Pinclude_modmenu_runtime=true --warning-mode all
.\gradlew.bat --no-daemon --no-build-cache --rerun-tasks runServer --warning-mode all
python tools/package_release.py --output-dir <fresh-empty-directory>
```

Measured automated results:

- Package/metadata/README validation passed with 118 production Java sources. Deterministic release-package regression passed with 257 Git-tracked source entries. Mod Menu integration, structured-tooltip composition, edge-case audit, polish regressions, and all 90 dependency-free core assertions passed.
- The cache-free `clean test runGameTest build` completed with `BUILD SUCCESSFUL` in 31 seconds. JUnit passed 158 tests in 24 suites with zero failures, errors, or skips; the dedicated Minecraft runner passed all 66 required GameTests.
- The real client GameTest completed with `BUILD SUCCESSFUL` in 40 seconds with Mod Menu 20.0.0 present. It exercised title-screen, integrated-server, dedicated-server operator/non-operator authority, reset, navigation, search, exact tooltip narration, wrapping, viewport bounds, and read-only non-mutation, and produced 19 current screenshots. The refreshed general capture is published in the README.
- The standalone dedicated server loaded 43 mods without Mod Menu, initialized Resource Multiplier under the preserved `smart_resource_drops` ID, reached `Done (0.280s)`, returned the expected `/smartdrops` and shearing status, reported `Status: Valid` from compact and verbose validation, confirmed no configuration or world data changed, stopped cleanly, and completed in 42 seconds.
- Release packaging succeeded in a fresh empty directory. The playable JAR is `build/libs/resource-multiplier-1.2.0.jar`: 601,761 bytes, 311 ZIP entries, SHA-256 `27F73D54F8CD6EFE17F68DFB3C76D11B67BEAB3AB6DF4725F0D398E4D0E5F9C7`. It embeds the MIT license, production icon, public name `Resource Multiplier`, version `1.2.0`, and the unchanged mod ID/data namespace, with zero nested JAR or GameTest entries.
- Lightweight Markdown validation checked README length, heading hierarchy, balanced fences, local links/images, image alt text and declared widths. Every README GitHub and badge URL returned HTTP 200.
- Static workflow inspection confirms the ordinary build workflow handles branch pushes, pull requests, and manual dispatch only. The guarded release workflow is the sole `v*` tag handler and exits before build or publication while `release_ready=false`.
- GitHub Actions run `33354997205` completed successfully for the exact production clarity commit `cb6d17d9a26283ddc98c6e3f7d8fa46ebb341380`. Its package-policy, Java/dedicated test/build, client GUI/authority, and artifact-upload steps all passed.

The mapped and dedicated suites cover fresh/reset versus migrated/recovery defaults, exact schema-2 entity preservation, 0/1/2/64x resolution, special/unknown/tag-conflict safety, bounded/atomic patches, component/legal-stack preservation, cumulative source/item/materialized-stack limits, full-event fallback, nested contexts, exception cleanup, and read-only inspection/validation. Dedicated tests distinguish real sheep state/tool/output behavior from resolver-only assertions and retain beehive, leash, block shearing, direct equipment output, and existing block/death-loot regression coverage.

Still open are the hands-on sheep/special/dispenser/block/beehive/leash/UI/reload matrix; a separately installed operator/non-operator multiplayer pass; existing-config and saved-provenance restart checks in a real world; dense high-output observation; and named/versioned third-party cases for biome/passive-animal content, a hostile mob, boss, inventory-carrying mob, custom shearable, automated miner, and custom placement. The clarity candidate's exact clean-checkout GitHub Actions gate has passed, but none of these remaining hands-on gates is inferred from the automated client, server, or synthetic fixtures. See [PUBLIC_RELEASE_CHECKLIST.md](PUBLIC_RELEASE_CHECKLIST.md).

## 1.1.0 final hardening verification under the former public name - 2026-08-30

That historical 1.1.0 tree added the read-only configuration validator, complete block-loot output preflight, deprecation cleanup, publication templates, and expanded package/source hygiene. The following results came from its serialized post-hardening run and final artifact inspection rather than older XML or the checkpoint below.

The intended automated sequence is:

```powershell
python tools/validate_package.py
python scripts/test_release_packaging.py
python scripts/test_modmenu_integration.py
python scripts/edge_case_source_audit.py
python scripts/polish_regression_tests.py
.\tools\run_core_tests.ps1
.\gradlew.bat --no-daemon --warning-mode all clean test runGameTest build
.\gradlew.bat --no-daemon runClientGameTest
```

The POSIX core equivalent is `bash tools/run_core_tests.sh`; it was not run on this Windows host. The Windows runner covers the same dependency-free assertions. After the Gradle build, the actual playable JAR was inspected before deterministic source-package validation. A standalone interactive `runClient` in a world remains a separate manual gate; a startup smoke or client GameTest does not satisfy it.

The new automated coverage proves these contracts:

- `/smartdrops validate` permission and console dispatch, compact/verbose formatting at 15/100 issues, live configured block/entity/dimension/tag lookup, warnings/advice, unknown-reference preservation, bounded load diagnostics, UUID privacy, and read-only config/revision/file behavior.
- Block preflight at 0x/1x/2x/64x and both sides of the 262,144-item/4,096-stack limits, overflow-safe estimates, legal/component-bearing stacks, whole original-list identity on fallback, player/explosion/automation parity, statistics accounting, warning message fields, five-minute suppression, and 256-key limiter eviction.
- Project-source deprecation lint, supported entity-holder/mock-player APIs, isolated XP-orb scenarios, required issue/PR templates, protected-output tag/source assets, and generic rejection of nested/shaded/test/fixture/runtime/source leaks from the playable JAR.

Measured results: all five validators pass with 103 production Java sources and a deterministic 224-entry source set; the Windows core runner passes 90 assertions; JUnit passes 125 tests in 21 suites with zero failures, errors, or skips; all 22 required dedicated GameTests pass; `runClientGameTest` passes in 38 seconds; and the exact no-build-cache Java 25 clean build passes in 32 seconds. Standalone `runServer` without Mod Menu reaches `Done (0.313s)`, executes compact and verbose validation from the console with status `Valid`, stops cleanly, and passes in 45 seconds. Standalone Mod Menu `runClient` reaches complete resource/audio/atlas initialization before intentional interruption and is recorded only as a startup smoke.

The inspected playable JAR has 272 entries, is 483,638 bytes, embeds version 1.1.0 with Java class major 69, and has SHA-256 `E3A6A38ADB3412F081ED546089C0E61C932AC8E0D507AB5B33F9A94A5DF66EBA`. No required hardening class is missing and no forbidden fixture, nested dependency, or runtime artifact is present. The baseline build's intermittent XP expectation was traced to equal-value orb count merging; the final fixture uses distinct legal orb denominations and the complete final sequence passes.

Manual evidence still required includes: run both validation forms from an operator and console against valid, unknown, conflicting, near-limit, and retained-load-diagnostic configurations; confirm the command does not rewrite or remove unknown entries; trigger a pathological block-list fallback and inspect its untouched output, statistic, and throttled warning; run the standalone interactive client/in-world command and GUI checks; perform custom-tag `/reload`, restart/migration, dense-farm, separately installed multiplayer, the 46-case vanilla matrix, and named/versioned third-party compatibility cases. None is marked passed here.

## 1.1.0 entity death-loot verification under the former public name - pre-hardening automated checkpoint

The results below are preserved historical evidence for the measured pre-final tree. They predate the final hardening section above and must not be presented as the final current release result.

The entity feature has three honest test layers:

- Pure/mapped JUnit covers entity rule precedence, exact/tag blacklist and whitelist behavior, whitelist miss, kill modes, permanent player exclusion, boss item/XP separation, configuration schema/migration/reset/patch/revision limits, component-preserving `0x`/`64x` stack output, and protected saddles/totems.
- The development-only Fabric testmod supplies real registered modded entity types, `replace: false` category-tag extensions, deterministic death loot tables, a component-rich `LootTableEvents.MODIFY_DROPS` callback, equipment/carried/inventory/custom output, nested death, thrown callback, duplicate standard-hook, cooked, empty, precomputed Looting-final, and unstackable fixtures.
- Dedicated GameTests drive real `LivingEntity` deaths, the actual mixin boundary, real item entities/experience orbs, vanilla player/projectile/environmental/tamed credit, `doMobLoot`, server command dispatch, server-side entity raycasting, and dedicated classloading. Static inspection is not counted as runtime behavior.

The server methods are named to map visibly to groups of requested cases: feature gate and `0x`/`1x`/`64x`; final Fabric modifier/components; equipment/picked-up/inventory/direct output; boss gate/saddle/totem/XP; direct/projectile/environmental/tamed/all-deaths attribution; mob-loot gamerule/scoped XP; nested and exceptional cleanup; duplicate-hook exactly once; mapped Looting/cooked/empty/baby/unstackable results; modded category priority/fallback/inspection; runtime precedence/whitelist modes; and compact/verbose/miss/console entity inspection. One method often covers several assertions. Therefore the runner's 20 required tests must never be reported as “all 54 behaviors” without a separate behavior ledger.

The hardened pass adds untagged `Animal`/`Monster`/`NeutralMob`/`WaterAnimal` fallback evidence, a real passive 2x death and category-priority death, genuine mob pickup, mapped Looting III and `killed_by_player`, component-rich 1x identity, same-list/two-wrapper and player-exclusion production contexts, independent item/XP gate combinations, complete entity-config snapshot helpers, entity child staging/Apply/Discard/filter paths, and stricter packaging regressions. The serialized Gradle, client GameTest, standalone startup, and artifact checks below were all rerun after those edits.

The duplicate-hook fixture deliberately invokes the standard death-table method twice inside one real `dropAllDeathLoot` session. At `2x` with one item per table call, the required total is `3`: the first call is multiplied to two, while the second passes at vanilla one because the session claim is already consumed. A result of four would prove accidental double application. The nested fixture lethally kills another registered entity from inside the loader's final-drop callback, so the inner call executes a genuine nested `dropAllDeathLoot` scope. The exception fixture throws during final loot generation in a lethal death, after which a fresh death must still resolve normally.

The final serialized automated sequence completed on Java 25:

```powershell
python tools/validate_package.py
python scripts/test_release_packaging.py
python scripts/test_modmenu_integration.py
python scripts/edge_case_source_audit.py
python scripts/polish_regression_tests.py
.\tools\run_core_tests.ps1
.\gradlew.bat --no-daemon clean build
.\gradlew.bat --no-daemon runClientGameTest
.\gradlew.bat --no-daemon runServer
.\gradlew.bat --no-daemon runClient
```

Results: all five validators pass; package validation counts 90 production Java sources; deterministic packaging validates 198 source entries; the core runner passes 90 assertions; the cache-free exact clean build succeeds in 35 seconds; JUnit XML reports 16 suites, 92 tests, and zero failures/errors/skips; and all 20 required dedicated GameTests pass. The real client GameTest succeeds in 39 seconds. Standalone `runServer` reaches `Done (0.436s)`, stops cleanly, and succeeds in 32 seconds without Mod Menu or client-class leakage. Standalone `runClient` loads 1.1.0 and completes resource/audio/texture initialization before intentional interruption; it is a startup smoke, not an in-world manual result.

Direct inspection records `build/libs/smart-resource-drops-1.1.0.jar` at 426,260 bytes and 242 entries, embedded version 1.1.0, Java class major 69, and SHA-256 `6AC0002F1615AB2FFF6248DFABE356A50EC65825D59190A57B9AE219DD3A1866`. It contains no GameTest/testmod/fixture data, development loot tables, bundled dependencies, nested JARs, or runtime configuration/world data.

Manual 1.1 coverage remains pending: cow OFF/ON/Looting/Fire Aspect/fall/player-credit/tamed; zombie equipment; skeleton bow; fox item; allay and donkey/llama inventories; villager/golem/wither skeleton; Wither/Dragon/Warden; slime/magma splitting; mob-caused Creeper, TNT and dispenser attribution; `0x`/`64x`; mob/boss XP gates; `doMobLoot=false`; blacklist/whitelist/tag/exact/category rules; reset/restart/1.0.x migration; non-op/operator editor; `/reload` category-tag refresh; `/smartdrops inspect entity`; dedicated server without Mod Menu; and named/versioned third-party wildlife, hostile, boss, and equipment/inventory mods. No generic mod compatibility claim may be derived from the synthetic testmod.

All 1.0.3 block-inspection and release-cleanup results below are historical and remain valid for their recorded artifacts; they do not validate the unreleased entity code.

## Block inspection validation - 2026-08-30

The inspection-specific automated and server-runtime validation is complete. The older public-release cleanup and 2026-08-29 sections below remain historical baselines; their counts, timings, hashes, and JAR sizes were not rewritten.

### Inspection-specific automated coverage

The dependency-free core and mapped JUnit suites cover the immutable rule trace and command presentation without launching Minecraft gameplay:

- Natural/untracked and player-placed provenance, player mining disabled, every source-mode/protection combination, and player/explosion/automation toggles use the same reasons and effective results as `RuleEngine.resolve`.
- Synthetic resolved-category inputs representing a modded log, a `c:ores` modded ore, and no-category/Miscellaneous fallback, plus multiple matches in gameplay order and player > block > category > dimension > global precedence, retain every candidate while identifying the selected rule. The dedicated-server stone fixture verifies real vanilla tag binding; real `c:ores` and third-party mod tag binding remain in the unperformed manual compatibility matrix below.
- Exact and tag blacklist rejection; exact and tag whitelist allowance; whitelist no-match rejection; protected and allowlisted block entities; and eligible `0x`, `1x`, and `64x` outcomes are represented independently in the trace.
- Command parsing covers both normal-player forms. Formatter coverage uses mapped block state, bounds a long modded registry ID/state, and verifies that block-entity NBT or inventory data is never rendered.
- The recent-removal-cache regression calls the read-only peek repeatedly and proves it does not advance the cache clock, expire, or consume the marker before gameplay resolution.

The inspection-specific dedicated-server GameTest exercises Minecraft behavior that a pure or mapped unit test cannot:

- It repeatedly inspects a real stone block with a Stone category override and a tracked, populated chest. Each target produces a stable equal trace across repeated calls, and the trace decision is compared with normal `MultiplierResolver.resolve` output.
- It verifies the real stone category binding and the chest's block-entity protection plus tracked provenance while preserving configuration revision, statistics snapshot, both block states, block-entity identity, chest contents, and persistent provenance.
- A second inspection GameTest positions and rotates a mock server player, proves the real server raycast selects the intended stone using the player's interaction range, dispatches `/smartdrops inspect`, and checks the component output. It then dispatches verbose inspection while looking into the sky and invokes inspection from the console, verifying clear no-target and player-required failures without changing the block.
- The existing server-only GameTest continues to prove that no Minecraft client or Mod Menu class is required.

The automated compatibility boundary remains explicit: no test installs a real third-party modded block, modded block entity, or modded dimension. Synthetic namespaced rule inputs and the real vanilla tag fixture prove the generic path, but cannot replace the manual third-party compatibility matrix.

### Completed post-change validation

The static, dependency-free, mapped, dedicated-server, client-GameTest, and packaging layers all passed:

```powershell
python tools/validate_package.py
python tools/validate_release_packaging.py
python tools/validate_modmenu_integration.py
python tools/edge_case_source_audit.py
python tools/polish_regression_tests.py
.\tools\run_core_tests.ps1
.\gradlew.bat --no-daemon clean test runGameTest build
.\gradlew.bat --no-daemon runClientGameTest
```

Measured results:

- All five Python validators passed. Package validation counted 64 production Java sources, and deterministic release packaging validated 132 source entries.
- The Windows dependency-free core harness passed all 90 assertions.
- JUnit XML contains 13 suites and 63 tests with zero failures, zero errors, and zero skipped tests. `RuleResolutionTraceTest` passed 12/12 and `SmartDropsCommandsTest` passed 6/6.
- The dedicated-server runner passed exactly 8/8 required GameTests: seven Resource Multiplier methods plus the Fabric runner's required count.
- The final `gradlew.bat --no-daemon clean build` completed with `BUILD SUCCESSFUL` in 17 seconds.
- The real Minecraft 26.2 `runClientGameTest` completed with `BUILD SUCCESSFUL` in 34 seconds.
- A standalone `runClient` startup smoke initialized the mod and completed client resource, audio, and texture-atlas loading before the task was intentionally interrupted; this proves startup only, not the manual in-world checklist.
- Standalone `runServer` without Mod Menu reached `Done`, accepted a clean stop, showed no client-only classloading failure, and completed with `BUILD SUCCESSFUL` in 33 seconds.
- The resulting `build/libs/smart-resource-drops-1.0.3.jar` is 291,391 bytes with 173 ZIP entries and SHA-256 `1F4FBF54C13C544909BA61F0800FFB76C073E2ED04EB0F95FF10B33849565A1B`.

### Remaining manual inspection matrix

The standalone `runClient` smoke did not enter a world or execute the inspection commands. It remains necessary to inspect and then mine natural and placed blocks; inspect a chest and flower; exercise block/category/dimension/player overrides plus blacklist and whitelist paths; and confirm repeated inspection does not affect the later player-driven break. No real third-party modded block, modded block entity, or modded dimension was installed for this validation, so that compatibility matrix also remains unperformed. Do not infer either manual result from the passing client GameTest, startup smoke, or dedicated-server launch.

## Public-release cleanup verification - 2026-08-30

Current measured results:

- All five Python source/package validators passed: package structure, deterministic release packaging, Mod Menu integration, edge cases, and polish regressions.
- Package validation counted 62 production Java sources. The Windows-native dependency-free core suite passed all 62 assertions.
- Gradle JUnit passed all 47 tests in 12 suites against the mapped Minecraft classes.
- The dedicated-server GameTest runner passed all six required tests. Resource Multiplier contributes five methods, including real drop-path and reset/provenance coverage.
- The real Minecraft 26.2 client GameTest completed with `BUILD SUCCESSFUL` in 32 seconds. It now covers the shared dirty indicator, root-only Apply/Discard behavior, reset semantics, and compact 320x180 layout in addition to the existing hierarchy and authority paths.
- A manual Mod Menu 20.0.0 `runClient` completed with `BUILD SUCCESSFUL` in 14 minutes 18 seconds. Starting from the title screen, the historical Mods > former-name entry > Configure route was exercised before the rename; shared dirty state was checked in Categories, Block Overrides, Dimensions, Filters, and Advanced; Apply cleared it; and Reset All Settings displayed its red confirmation and returned a clean default editor after confirmation.
- A standalone dedicated `runServer` without Mod Menu initialized the mod in the server environment, reached `Done (0.256s)`, accepted `stop`, and completed with `BUILD SUCCESSFUL` in 37 seconds.
- The final exact `gradlew.bat --no-daemon clean build` completed with `BUILD SUCCESSFUL` in 15 seconds and again passed all six required server GameTests. The measured JUnit XML contains 12 suites, 47 tests, and no failures, errors, or skips; `BUILD_STATUS.md` records the single authoritative JAR identity.
- The deterministic source-packaging regression produced and validated 129 source entries, retained the required Gradle wrapper files with executable `gradlew`, and rejected generated/runtime paths. The exact final artifact identities belong in `BUILD_STATUS.md` and the release checksum file rather than being duplicated throughout this document.

Behavioral contract covered by this cleanup:

- `PLAYER_PLACED_ONLY` always rejects natural blocks, independent of placement protection. `NATURAL_ONLY` rejects placed blocks only while protection is on; with protection off it admits both natural and placed blocks. `ALL` admits both provenance states. Independent player, explosion, automation, filter, and block-entity gates still apply.
- `minecraft:dragon_egg` is an ordinary entry in the default safety blacklist. Removing that entry allows normal rule resolution; there is no permanent Dragon Egg special case in `RuleEngine`.
- Every child screen reads dirty state from the same root `ConfigEditorSession`. Clean children hide the indicator; a category edit shows it; navigation preserves it; root Discard or successful Apply clears it; reset Cancel preserves it; confirmed reset follows the existing authoritative clean-session workflow.
- Apply Changes, Done/Discard Changes, and Reset All Settings remain centralized on the General/root screen. A connected Apply still sends one bounded `ConfigPatch`, and each authoritative mutation rechecks permission and expected revision before an atomic persist-before-publish update.

The 2026-08-29 sections below are retained as dated checkpoints; their counts and artifact hashes are historical rather than current release-candidate measurements.

## Reset All Settings verification - 2026-08-29 (historical checkpoint)

Current automated results:

- **JUnit reset suite: passed.** Coverage includes resetting every serialized field to the exact `SmartDropsConfig.defaults()` model, reload persistence, failed-write atomicity, one revision increment on success, stale reset/patch rejection, and preservation of accumulated statistics/history.
- **Server GameTests: 6/6 passed.** The suite includes a real placed-block provenance attachment that remains present across `ConfigManager.reset`.
- **Full real client GameTest: passed in 34 seconds.** The run covers the local confirmation opening without mutation, Cancel and Escape returning to the exact staged session, confirmed immediate clean defaults, integrated-server authoritative reset, dedicated-server operator success and non-operator rejection, pending queue invalidation, and stale patch protection.
- **Static audits: passed.** The reset checks require the dedicated reset payload, permission and expected-revision validation, atomic model-default replacement, pending-work invalidation, destructive confirmation copy/tooltip, short cooldown, other-editor invalidation, and preservation of provenance and accumulated statistics.
- **Clean release pipeline: passed.** Package validation counted 62 production Java sources; all 47 JUnit tests and 42 dependency-free core assertions passed; `clean test runGameTest build` completed successfully.
- **Reset-checkpoint artifact:** `build/libs/smart-resource-drops-1.0.3.jar`, 269,733 bytes, SHA-256 `4e0614b0735f87383968278d9eacec9185ac01e40fb8fa8ae39779fb0924797d`.

Behavioral contract verified by those suites:

- The separated root **Reset All Settings** button opens a vanilla confirmation whose **Reset Everything** action is red.
- Cancel and Escape are exact non-mutating round trips: staged values, dirty state, search state, and the editor session survive unchanged.
- Local reset is one atomic persist-before-publish operation. Integrated and dedicated resets are server-authoritative and accept only an authorized request for the current revision.
- Confirmation discards staged state and invalidates all pending command, request, and patch queues; stale work cannot reapply pre-reset values. Other open editors are invalidated, and the short reset cooldown bounds repeated writes.
- Provenance and accumulated statistics/history are deliberately not configuration and survive reset.

Manual-only limitations:

- [ ] Perform a true JVM/world restart, reload the affected chunk from disk, and confirm the placed-block provenance marker still survives.
- [ ] Verify interoperability with client and dedicated server installed and launched as separate multiplayer instances.
- [ ] Inspect visual layout, scaling, keyboard focus, mouse focus, and confirmation emphasis by eye at representative GUI scales.

No separate manual `runClient` result was claimed for this reset checkpoint; the runtime result above was the automated client GameTest.

## Final hierarchical GUI verification - 2026-08-29 (historical checkpoint)

The current editor flow is **General > Categories / Block Overrides / Dimensions / Filters / Advanced**. Category, block, and dimension rows open focused detail editors while retaining the same staged `ConfigEditorSession`. Apply must remain disabled until that shared draft differs from the authoritative baseline.

Final automated and runtime results:

- `validate_package` passed with 59 Java sources.
- `edge_case_source_audit`, `polish_regression_tests`, and the hierarchical Mod Menu static validator passed.
- The Windows-native core runner passed all 42 assertions.
- `gradlew --no-daemon clean test runGameTest build` reported `BUILD SUCCESSFUL`, including all five required dedicated-server GameTests.
- `gradlew --no-daemon runClientGameTest` reported `BUILD SUCCESSFUL` in a real Minecraft 26.2 client.
- A real Mod Menu-enabled `runClient` had already launched successfully and its screenshots were visually inspected. The latest GameTest screenshots show the populated Logs category with 44 vanilla blocks and the diamond search results.
- GUI-redesign checkpoint artifact: `build/libs/smart-resource-drops-1.0.3.jar`, 256,580 bytes, SHA-256 `7ea0dd2dc48143756ceb6e2535f03941a7e2bb8d37102bd02556057337716f49`.

The Block Overrides acceptance rule is intentionally different from the previous paginated screen: an empty query shows existing overrides and help text, never the complete registry; a non-empty query searches translated names and namespaced IDs; no more than 200 broad-query results are allocated/displayed. The category, block, dimension, filter, and advanced lists use lightweight `ObjectSelectionList` rows rather than thousands of child buttons or per-frame registry scans. General leaves the block catalogue unbuilt; catalogue-backed screens initialize it lazily, normalized searches use bounded 32-entry caches, and filter matching caps results before row allocation. Dynamic/modded dimensions come from runtime and configured data.

The title/local category regression is covered by the installed tag-JSON resolver in `ClientCategoryTagIndex`: Logs/Wood resolves to 44 vanilla blocks without relying on a connected world's runtime tag bindings. Search text and scroll survive opening a focused editor and returning with Back, compact-height layout is regression-tested, `0x` is kept distinct from inheritance, and a dirty root labels its exit action **Discard Changes**.

Non-operators must receive the same navigable hierarchy read-only. The redesign does not change the server-authoritative snapshot/acknowledgement lifecycle, rate limiting, permission rules, `ConfigPatch` protocol, or configuration file format.

### Final post-redesign verification checklist

- [x] Run `gradlew --no-daemon clean test runGameTest build`; the build and all five required server GameTests passed.
- [x] Run `gradlew --no-daemon runClientGameTest` against the hierarchical screens; the real client suite passed.
- [x] Verify the bounded General/root structure, navigation to every major child, focused category/block editors, Back state retention, clean/dirty Apply, authoritative acknowledgement, and local/integrated/dedicated authority paths.
- [x] Confirm the empty Block Overrides view shows only overrides; test diamond by display name and exact `minecraft:diamond_ore`, acacia, and the 200-result broad-query cap.
- [x] Confirm populated category resolution and configured/inherited/effective presentation, including concrete `0x` versus Inherit.
- [x] Confirm Filters/Advanced remain bounded and restricted to the existing patch-supported behavior; configured tag filters remain visible/read-only.
- [x] Verify compact-layout regressions and retained search/scroll state through automated client/static coverage.
- [x] Launch the real Mod Menu path and inspect screenshots; record the final packaged artifact identity, size, and hash.
- [ ] Exercise an actual third-party modded block and modded dimension fixture. The automated suite has no third-party content mod installed.
- [ ] Add GUI tag-filter mutation only if `ConfigPatch` is deliberately extended with tag edit fields. Current tag rows are intentionally visible/read-only.
- [ ] Exhaustively human-click a separately installed multiplayer server and the full GUI-scale/window-size matrix. Embedded dedicated-server and compact-layout automation do not replace this manual compatibility pass.

The older verified-state sections below are retained as historical 1.0.3 hardening evidence.

## Complete automated check

Use Java 25. On Windows PowerShell:

```powershell
python tools/validate_package.py
python scripts/test_release_packaging.py
python scripts/test_modmenu_integration.py
python scripts/edge_case_source_audit.py
python scripts/polish_regression_tests.py
.\tools\run_core_tests.ps1
.\gradlew.bat --no-daemon clean test runGameTest build
.\gradlew.bat --no-daemon runClientGameTest
```

On a POSIX host:

```bash
python3 tools/validate_package.py
python3 scripts/test_release_packaging.py
python3 scripts/test_modmenu_integration.py
python3 scripts/edge_case_source_audit.py
python3 scripts/polish_regression_tests.py
bash tools/run_core_tests.sh
./gradlew --no-daemon clean test runGameTest build
xvfb-run -a ./gradlew --no-daemon runClientGameTest
```

The Gradle bootstrap verifies the downloaded Gradle 9.5.1 archive and uses pinned stable Fabric Loom 1.17.20. The release workflow also rejects a tag whose version differs from `mod_version`.

## What each layer proves

The source validators check JSON syntax, package structure, deterministic source-ZIP construction, generated/runtime-path rejection, required Gradle wrapper retention, metadata and dependency policy, removed mixin/tag regressions, bounded-cache and client-queue contracts, and optional Mod Menu wiring. These are static checks; they do not prove that Minecraft launched or that a gameplay scenario worked.

The 62 core-rule assertions compile a small dependency-free rules slice. They cover multiplier precedence, the complete source-mode/protection/provenance matrix, the removable Dragon Egg default, filters, block-entity defaults, `0x`, player caps, coordinate packing, provenance transitions, bounded recent-removal behavior, and range sanitization. They are useful policy tests, not Minecraft runtime tests.

The Gradle `test` task runs 47 JUnit 5 tests against the mapped Minecraft classes:

- Three command tests for namespaced block IDs, optional `#` tag IDs, validation, and canonicalization.
- Seven configuration tests for tag/key normalization, the 2,048-entry rule bound, missing versus explicit-empty blacklist behavior, future schema detection, malformed root rejection, no-op equality, and preservation/recovery of future or malformed files.
- Nine configuration-patch/transaction tests covering preset-plus-dirty-edit order, unrelated server-state and UUID preservation, exact filter/map edits, idempotent retry, invalid keys, scalar/map multiplier bounds, atomic local defaults, invalid authoritative snapshots, write failure without in-memory mutation, command-side persist-before-publish, write suppression, and full-cap rejection without rule displacement.
- Six request-lifecycle tests for valid acceptance, Retry generations, stale responses, close, disconnect, and integer-ID wrap.
- Five screen-open policy tests for title/local, connected loading, cached ready, integrated-server startup, and delayed `/smartdropsgui` connection/screen guards.
- Four adaptive-layout tests covering common logical screens from 320x180 through 640x360, footer/content containment, and navigation containment in panels as narrow as 18 pixels.
- One re-entrant queue-drain test proving due actions are removed before a callback cancels or replaces queue entries.
- Two exact-target tests that prevent nested loot or XP from borrowing another block's session.
- Two provenance-transition tests covering soil, stripped logs, copper, concrete, and unrelated replacements.
- Three recent-removal tests covering two-tick expiry, 4,096-entry fail-closed overflow behavior, and clock rollback.
- Two real `ItemStack` tests covering component preservation, legal maximum stack sizes, and consolidation of identical loot entries.
- Three full-reset transaction tests collectively covering exhaustive authoritative defaults, persistence/reload, accumulated-statistics preservation, failed-write rollback, one revision advance, and stale reset/patch rejection.

The `runGameTest` task launches a real Fabric dedicated-server test runtime. Resource Multiplier contributes five scenarios:

- Confirm the environment is server-only, Mod Menu classes are absent, required mixins audit cleanly, and the piston/falling provenance interfaces are applied.
- Exercise real `Level.setBlock` calls and verify dirt-to-farmland, log stripping, copper weathering, and concrete-powder hydration retain provenance while stone-to-diamond-ore does not.
- Place a door through the real `BlockItem` path and verify both halves receive provenance, then verify a failed door placement leaves no marker.
- Drive the real stone `Block.dropResources` path with a mock survival server player and verify natural stone produces two cobblestone at `2x`, a marked stone produces one, `0x` produces none, and `64x` produces one legal item entity containing 64 cobblestone.
- Mark a block through the real provenance attachment, perform a complete configuration reset, and verify both exact defaults and the pre-reset provenance marker remain intact.

The latest runner reported exactly six required GameTests passing (the five methods above plus the Fabric runner's required count). The GameTests do not currently execute the complete player block-breaking interaction, Fortune/Silk Touch/XP cases, piston movement, a falling entity landing, a world restart, or the client GUI.

The `runClientGameTest` task launches a real Minecraft 26.2 client and covers the configuration authority path end to end:

- Opens the shared route with no world, verifies the bounded General screen is editable and initially clean, round-trips protection/source/XP controls, checks XP-button gating and dirty/revert behavior, navigates every major child, and captures General, Categories, Block Overrides, Dimensions, Filters, and Advanced.
- Verifies the installed client category index populates Logs/Wood with 44 vanilla blocks. It searches by diamond display name and exact `minecraft:diamond_ore`, covers acacia and the broad-query 200-row cap, and opens focused category and block editors to distinguish concrete `0x` from Inherit.
- Opens and returns from focused editors to prove their parent query and scroll state survive Back, while every child retains the same staged `ConfigEditorSession` and bounded widget/list counts.
- Verifies a clean child hides the shared unsaved-changes indicator, a category edit reveals it, and navigation from Categories to Block Overrides retains it. It then confirms root Apply is active, root Discard creates a fresh clean session, and a fresh child no longer shows the indicator.
- Applies an integrated-server edit and confirms the authoritative acknowledgement returns a clean root and clean child. Reset confirmation Cancel preserves the dirty session and indicator, while confirmed reset returns a fresh clean defaults session.
- Resizes a dirty focused child to 320x180, checks that the stacked indicator remains within the viewport without overlapping content or Back, captures the result, and includes that screenshot in local visual inspection.
- Adds, removes, and reverts an exact block filter, then previews a preset and confirms preview navigation does not mutate the shared draft.
- Creates an integrated server, requests its authoritative snapshot, verifies owner editing without cheats, re-enters through the connection-cached READY route, applies a changed multiplier through the payload/atomic server path, verifies the acknowledgement returns a clean root, and restores the value.
- Creates and joins a dedicated server as a non-operator, verifies navigation/search remain available while mutation controls are inactive, and checks that rate-limited unauthorized payloads cannot be retained and applied after promotion.
- Promotes the player, exits through Done to invalidate cached permissions, reopens through the production route, verifies editing is enabled, applies one authorized patch, queues a second during cooldown, disconnects, waits past eligibility, and proves the queued write was discarded.
- Exercises Reset All Settings locally and against integrated/dedicated authority: non-mutating confirmation, exact Cancel/Escape return, immediate clean refresh, read-only rejection, operator success, pending-work invalidation, cooldown, and stale-revision rejection. It also captures and visually checks the vanilla confirmation with its red destructive action.

The client screenshots are inspected during local verification, while CI runs this task under Xvfb. It calls the same `SmartDropsConfigScreens.create` factory used by Mod Menu and `/smartdropsgui`; it does not automate clicks inside Mod Menu itself. The separate 2026-08-30 manual `runClient` did traverse the visible title-screen Mods route and every major child listed above.

## Verified state for the final 1.0.3 hierarchy

On 2026-08-29, package validation passed with 59 Java sources; the edge-case, polish, and hierarchical Mod Menu validators passed; the Windows core runner passed all 42 assertions; `gradlew --no-daemon clean test runGameTest build` completed successfully with all five required dedicated-server GameTests; and `gradlew --no-daemon runClientGameTest` completed successfully on Java 25. The Windows Gradle bootstrap also resolved Gradle 9.5.1 successfully.

The final client screenshots for that hierarchy checkpoint show Logs/Wood populated with 44 vanilla blocks and the diamond search results. A separate real Mod Menu-enabled `runClient` had already succeeded and its screenshots were inspected. That checkpoint artifact was `build/libs/smart-resource-drops-1.0.3.jar`, 256,580 bytes, SHA-256 `7ea0dd2dc48143756ceb6e2535f03941a7e2bb8d37102bd02556057337716f49`.

The POSIX core runner was not executed locally because this Windows host had no WSL distribution; its Windows counterpart uses the same core sources and assertions. A standalone `runServer` without Mod Menu reached `Done`, saved all dimensions, and stopped cleanly. A Mod Menu 20.0.0 `runClient` launch displayed the complete title-screen local/default editor; the user-provided runtime screenshot is the visual acceptance result.

## Manual release matrix

None of the items below should be marked complete solely because the automated suite passed.

### Natural, placed, and transformed blocks

1. Set global `2x`, mine natural stone, and confirm exactly two units of the normal calculated loot.
2. Place one resulting block, break it, and confirm vanilla quantity in `NATURAL_ONLY` mode.
3. Repeat after chunk unload/reload and after a full server restart.
4. Place and break both halves of a door and bed, a tall plant, and a representative modded multi-block item.
5. Strip a placed log; till and path placed dirt; weather and wax placed copper; hydrate placed concrete powder. Confirm each stays protected.
6. Grow a tree, crop, and renewable cobblestone generator output. Confirm newly generated resources do not inherit the original marker.

### Loot and XP

1. Test natural ore and player-placed Silk Touch ore at `0x`, `1x`, `2x`, `4x`, and `64x`.
2. Test Fortune and Silk Touch, including a component-bearing or modded loot result, and compare against Minecraft's rolled loot before multiplication.
3. Confirm legal item stack sizes and reasonable item-entity counts at `64x`.
4. Confirm XP is unchanged while XP multiplication is disabled and uses the configured value only when enabled.
5. Set `doTileDrops=false` and verify vanilla suppression remains authoritative.

### Pistons, falling blocks, and persistence

1. Push and sticky-pull natural and placed blocks, including a move across a chunk border.
2. Break the destination with conservative piston protection both enabled and disabled.
3. Drop placed sand, gravel, and concrete powder; verify provenance at the landed position and after concrete conversion.
4. Save and restart while a piston or falling block is in progress, then verify the completed destination remains protected.

### Explosions, automation, and block entities

1. Destroy natural and placed resources with TNT and verify Minecraft's explosion decay remains authoritative.
2. Exercise a Fabric fake player and at least one real automated miner with automation disabled and enabled.
3. Verify a remove-before-drop machine inside and outside the two-tick compatibility window.
4. Test empty and populated chests, shulker boxes, beehives, decorated pots, and a representative modded block entity with the allowlist empty.
5. Test an intentional allowlist entry and confirm the administrator-visible risk is understood.

### Rules and dimensions

1. Verify block over category over dimension over global precedence, then enable and cap a personal override.
2. Test blacklist and closed-whitelist modes with exact block IDs and tags written both with and without `#`.
3. Verify category behavior for a block that matches more than one category; add an exact block override if deterministic per-block behavior is required.
4. Exercise Overworld, Nether, and End rules and a modded block participating in standard Minecraft or Fabric tags.

### Client, multiplayer, and Mod Menu

1. General, all major child lists, focused category/block editors, representative search and inheritance states, Back retention, dirty indication, Apply/Discard, reset, and dedicated permission states are automated. The full visible title-screen Mod Menu route and all major child dirty states were also checked manually. Still human-click the complete keyboard/focus/tooltip flow and every GUI-scale/window combination.
2. The embedded dedicated-server client test covers non-op and operator presentation. Repeat against a separately installed multiplayer server as a manual compatibility check.
3. Edit rapidly, close before a reply, disconnect with pending work, and reconnect to a different server. Confirm no stale response opens a screen and no accepted edit is silently lost.
4. Title-screen Configure with Mod Menu 20.0.0 was visibly verified through the complete Mods-screen navigation on 2026-08-30. Repeat Configure while connected and against the final packaged JAR in the separately installed multiplayer check.
5. Remove Mod Menu and verify `/smartdropsgui` still works. Start a dedicated server without Mod Menu and join a server without Resource Multiplier to verify both graceful paths.

### Configuration files

1. Load a current config with explicit empty lists and confirm they remain empty.
2. Load malformed JSON and confirm the original appears in a timestamped `.broken-*` backup before safe defaults replace it.
3. Simulate a backup failure or unreadable file and confirm the original is not overwritten.
4. Load a config with a future `schemaVersion`, confirm the file is unchanged, and confirm safe in-memory defaults are used.

Record the Minecraft, Loader, Fabric API, Mod Menu, and Java versions with results. The public release checklist in [PUBLIC_RELEASE_CHECKLIST.md](PUBLIC_RELEASE_CHECKLIST.md) remains the publication gate.
