# Build status

## Unreleased shearing candidate status — 2026-08-30

The tree now contains schema-3 safe entity-shearing work while deliberately retaining `1.1.0` metadata. This directory still has no `.git` metadata, so no baseline commit, branch, tag, or remote can be reported and no push/release has been performed.

The candidate's public name is now **Resource Multiplier**. The rename does not change the `smart_resource_drops` mod ID, configuration path, saved-world provenance, commands, or datapack namespace. Historical artifact filenames remain exact where they identify evidence; the former public phrase itself is retained only in `CHANGELOG.md`.

Before the shearing edits, the real Java 25 baseline was rerun: `clean test`, all 22 dedicated GameTests, `runClientGameTest`, and `clean build --warning-mode all` succeeded. The completed candidate then passed one serialized verification chain:

- `compileGametestJava --rerun-tasks` succeeded against the mapped Minecraft 26.2 APIs.
- The final `test` task passed 158 JUnit tests in 24 suites with zero failures, errors, or skips.
- The dedicated runner passed all 66 required GameTests: the existing 22 plus 44 focused shearing tests. Coverage includes real sheep/manual/dispenser paths, all five audited special vanilla implementations, direct-output isolation, blocks/beehive/leash non-regressions, budgets, consumer preservation, commands, inspection, mixin loading, and same-target nested-source isolation.
- `runClientGameTest -Pinclude_modmenu_runtime=true --rerun-tasks` completed with `BUILD SUCCESSFUL` in 40 seconds after exercising title-screen, integrated-server, dedicated-server editor/authority, configuration-title, and reset-confirmation paths with Mod Menu 20.0.0 present.
- The final `clean build --rerun-tasks --warning-mode all` completed with `BUILD SUCCESSFUL` in 30 seconds and reran the 158 JUnit tests and all 66 dedicated GameTests.
- All six static/local validators passed: package/source validation (118 Java sources), deterministic release-packaging tests (248 source entries), Mod Menu integration, edge-case audit, polish regressions, and all 90 dependency-free core assertions.
- Standalone `runServer` loaded 43 mods without Mod Menu, initialized Resource Multiplier, reached `Done (0.287s)`, returned the renamed `/smartdrops` status and `Resource Multiplier Validation`, rejected console-only block inspection with the expected legacy-compatible `/smartdrops inspect` guidance, accepted `stop`, and completed with `BUILD SUCCESSFUL` in 54 seconds.
- Standalone `runClient -Pinclude_modmenu_runtime=true` loaded Mod Menu 20.0.0 and completed resource, audio, and texture-atlas initialization. Direct captures verified the Mod Menu list/detail entry, configuration title, and `Reset Resource Multiplier?` confirmation; the destructive reset was not confirmed and the client was intentionally interrupted after verification.
- Full release-package generation succeeded in a temporary verification directory with `resource-multiplier-1.1.0.jar`, `ResourceMultiplier-1.1.0-source.zip`, `ResourceMultiplier-1.1.0-SHA256SUMS.txt`, and `ResourceMultiplier-1.1.0-release-bundle.zip`.

The inspected candidate artifact is `C:\Users\Andrew\Documents\GitHub\SmartResourceDrops\build\libs\resource-multiplier-1.1.0.jar`: 559,870 bytes, 311 ZIP entries, SHA-256 `C88AF403D2BD245E946AD3114156537CDEF2026393A73AD1246C8F6F78E77A78`, embedded mod ID `smart_resource_drops`, public name `Resource Multiplier`, version 1.1.0, Java class major 69, `LICENSE_resource-multiplier`, the stable `data/smart_resource_drops` namespace, and zero GameTest classes or nested JARs. The legacy-name same-version JAR under `dist\` is an older historical 1.1.0 artifact with no shearing classes and must not be used as this candidate.

Open release gates:

- hands-on manual sheep/special/dispenser/block/beehive/leash/UI/permission/reload matrix;
- real third-party safe/unknown/direct-output/custom-tool/custom-machine fixtures and an in-running-game datapack `/reload` cycle;
- version bump to `1.2.0` and release packaging only after those manual gates pass.

Until those gates close, this remains an automated 1.1.0 shearing candidate rather than a publishable 1.2.0 release. The 1.1.0 artifact evidence below is historical and must not be described as proof of the new shearing feature.

## 1.1.0 final hardening validation under the former public name - 2026-08-30

This directory is not a Git checkout: it has no `.git` metadata, branch, commit, tags, or remote. The baseline commit and public-release status therefore cannot be verified locally. Version 1.1.0 is retained, Fabric contact metadata remains `{}`, and no release or push was performed.

Verified target: Minecraft Java 26.2, Fabric Loader 0.19.3, Fabric API 0.158.0+26.2, Java 25, Fabric Loom 1.17.20, and optional client-only Mod Menu 20.0.0.

### Baseline before the final hardening changes

- `clean test` passed 92 tests in 16 suites with zero failures, errors, or skips.
- The dedicated runner passed all 20 required GameTests, and the client GameTest passed.
- All five repository validators and all 90 dependency-free core assertions passed.
- The first baseline `clean build --warning-mode all` exposed an intermittent XP GameTest failure: merged equal-value XP orbs made a value-only count observe 4 instead of the expected 5. The final fixture uses distinct Minecraft orb denominations and the complete post-fix sequence below passes.
- The baseline JAR was 426,260 bytes with 242 entries and SHA-256 `6AC0002F1615AB2FFF6248DFABE356A50EC65825D59190A57B9AE219DD3A1866`.

### Final automated and runtime evidence

- All five static/package/UI validators pass. Package validation counts 103 production Java sources; deterministic source validation covers 224 entries; the Mod Menu, edge-case, and polish audits pass.
- The Windows dependency-free core runner passes all 90 assertions.
- Mapped JUnit reports 125 tests in 21 suites with zero failures, errors, or skips.
- The dedicated Minecraft runner passes all 22 required GameTests, including live validation-command invariants and player/explosion/automation block-budget fallback/recovery coverage.
- `runClientGameTest` completes with `BUILD SUCCESSFUL` in 38 seconds.
- The authoritative Java 25 `gradlew.bat --no-daemon --no-build-cache clean build --warning-mode all` completes with `BUILD SUCCESSFUL` in 32 seconds and includes the 125-test JUnit and 22-test dedicated results. Project sources emit no deprecation warning; the remaining JOML/OSHI/Windows host warnings originate outside the project.
- Standalone `runServer --warning-mode all` loads 43 mods without Mod Menu or a client-only classloading failure, reaches `Done (0.313s)`, executes both `/smartdrops validate` forms from the console with status `Valid`, reports no missing tag warnings or data changes, accepts `stop`, and completes with `BUILD SUCCESSFUL` in 45 seconds.
- Standalone `runClient -Pinclude_modmenu_runtime=true` loads the then-current 1.1.0 display name and Mod Menu 20.0.0 and completes resource, audio, and texture-atlas initialization before an intentional interrupt. This is historical startup evidence only, not an interactive GUI or in-world manual pass.

The inspected playable artifact is `C:\Users\Andrew\Documents\GitHub\SmartResourceDrops\build\libs\smart-resource-drops-1.1.0.jar`:

- 483,638 bytes and 272 ZIP entries
- SHA-256 `E3A6A38ADB3412F081ED546089C0E61C932AC8E0D507AB5B33F9A94A5DF66EBA`
- embedded mod version 1.1.0, Java class major 69 (Java 25), and contact metadata `{}`
- all required validation/output-budget classes present, with zero forbidden testmod/GameTest/fixture/development-loot/bundled-dependency/nested-JAR/runtime-data leaks

The checksum-pinned standard Gradle 9.5.1 wrapper is present and boots successfully. Deterministic source packaging validates 224 entries; the generated `SmartResourceDrops-1.1.0-SHA256SUMS.txt` is the authoritative non-self-referential checksum evidence for the playable JAR and source archive.

Still pending are the standalone interactive client/in-world GUI and command matrix, player-driven vanilla block/entity matrix, manual unknown/conflicting/near-limit validation configuration, hands-on pathological block-loot fallback observation, custom-tag `/reload`, running-game restart/migration, dense-farm observation, separately installed operator/non-operator multiplayer, and named/versioned third-party wildlife/hostile/boss/inventory/miner/custom-placement cases. Automated fixtures and startup smoke do not satisfy those gates or justify a general compatibility claim.

The 1.1.0 checkpoint immediately below is preserved historical evidence and is not the current artifact identity.

## 1.1.0 automated release-candidate validation under the former public name - historical pre-hardening checkpoint

The working tree contains the default-off entity death-loot implementation, schema 2 configuration/editor/networking, server-side entity inspection, common entity category tags, the narrow death-loot/XP mixin boundary, deterministic development fixtures, and playable-JAR fixture-leak checks.

The follow-up safety and verification hardening is included in the measurements below. It adds bounded amplified item/XP output, lazy exactly-once claims, a one-shot mob-XP token, persisted attribution origin, stricter player-credit validation, an extensible protected-loot tag, transactional config publication, server-authoritative connected catalogs, stale-editor invalidation, compact mutation failures, real class-fallback/pickup/Looting and independent XP coverage, broader entity-editor transaction checks, and stricter archive validation. “20 required GameTests” remains a runner count, not evidence that every one of the 54 requested behaviors is a separate passing test.

Completed evidence:

- All five static/package/UI validators pass. Package validation counts 90 production Java sources, deterministic source packaging validates 198 entries, the Mod Menu integration audit passes, the edge-case audit passes, and the polish regression suite passes.
- The Windows dependency-free core runner passes all 90 assertions.
- The exact Java 25 `gradlew.bat --no-daemon --no-build-cache clean build` completes with `BUILD SUCCESSFUL` in 35 seconds. JUnit XML reports 16 suites, 92 tests, zero failures, zero errors, and zero skipped tests; the dedicated-server runner reports all 20 required GameTests passed.
- `runClientGameTest` completes with `BUILD SUCCESSFUL` in 39 seconds, including the complete entity draft/copy/equality paths, child staging, namespaced search, reset, and local/integrated/dedicated permission paths.
- Standalone `runClient` loads the 1.1.0 artifact and completes resource, audio, and texture-atlas initialization before an intentional interrupt. This is a startup smoke, not interactive gameplay.
- Standalone `runServer` without Mod Menu loads the 1.1.0 artifact, reaches `Done (0.436s)`, accepts `stop`, saves all dimensions, and finishes with `BUILD SUCCESSFUL` in 32 seconds without a client-only classloading failure.

The inspected playable artifact is `C:\Users\Andrew\Documents\GitHub\SmartResourceDrops\build\libs\smart-resource-drops-1.1.0.jar`:

- 426,260 bytes and 242 ZIP entries
- SHA-256 `6AC0002F1615AB2FFF6248DFABE356A50EC65825D59190A57B9AE219DD3A1866`
- embedded mod version 1.1.0, Java class major 69 (Java 25), and contact metadata `{}`
- no GameTest/testmod/fixture classes or data, development loot tables, bundled Fabric/Minecraft/Mod Menu classes, nested dependency JARs, or runtime configuration/world data

Still pending: the complete player-driven vanilla entity matrix, custom category-tag `/reload`, running-game restart/migration, dense-farm observation, separately installed multiplayer authority checks, and named/versioned third-party wildlife/hostile/boss/equipment-or-inventory compatibility matrix. Synthetic fixtures prove the supported integration boundary but do not justify a general mod-compatibility or publication-readiness claim.

The 1.0.3 paths, timings, counts, and hashes below are preserved historical baselines only.

## 1.0.3 public-release cleanup under the former public name

Target environment:

- Minecraft Java Edition 26.2
- Fabric Loader 0.19.3+
- Fabric API 0.158.0+26.2 or a compatible newer 26.2 build
- Java 25
- Fabric Loom 1.17.20
- Gradle 9.5.1 from the checksum-pinned wrapper bootstrap
- Mod Menu 20.0.0 as an optional client integration

## Block inspection feature - validated 2026-08-30

`/smartdrops inspect` and `/smartdrops inspect verbose` are implemented as server-authoritative, read-only diagnostics. They use a server-player raycast and the same immutable rule trace as gameplay, including modded namespaced IDs and dimensions, vanilla `minecraft:`/common `c:` category tags, Miscellaneous fallback, exact/tag filters, source and placement-provenance gates, block-entity protection/allowlisting, override precedence, configured multiplier, effective result, and reason. Inspection uses non-consuming provenance/cache peeks and neither evaluates loot nor changes world, configuration, revision, statistics, drops, XP, or provenance.

Inspection-specific automated coverage spans dependency-free core assertions, mapped JUnit trace/parser/formatter/privacy tests, a recent-removal peek regression, and dedicated-server GameTests. The GameTests repeatedly inspect real stone and a tracked, populated chest while checking stable traces, gameplay-decision parity, and configuration/statistics/world/block-entity/provenance invariants; they also execute the real server command dispatcher for a looked-at block, sky/no-target guidance, and console rejection. The existing server-only classloading audit remains active.

Final measured inspection results:

- All five Python validators passed; package validation counted 64 production Java sources and release packaging validated a deterministic 132-entry source archive.
- The Windows-native dependency-free core harness passed all 90 assertions.
- JUnit XML reports 13 suites, 63 tests, zero failures, zero errors, and zero skipped tests. The focused `RuleResolutionTraceTest` passed 12/12 and `SmartDropsCommandsTest` passed 6/6.
- The dedicated-server runner passed exactly 8/8 required GameTests.
- The final clean build completed with `BUILD SUCCESSFUL` in 17 seconds, and the real Minecraft 26.2 client GameTest completed with `BUILD SUCCESSFUL` in 34 seconds.
- Standalone `runServer` without Mod Menu reached `Done`, stopped cleanly, showed no client-only classloading failure, and completed with `BUILD SUCCESSFUL` in 33 seconds.

The final inspection-feature playable artifact is `C:\Users\Andrew\Documents\GitHub\SmartResourceDrops\build\libs\smart-resource-drops-1.0.3.jar`:

- 291,391 bytes and 173 ZIP entries
- SHA-256 `1F4FBF54C13C544909BA61F0800FFB76C073E2ED04EB0F95FF10B33849565A1B`
- JUnit/GameTest classes remain excluded from the playable JAR

A standalone `runClient` startup smoke initialized the mod and completed client resource, audio, and texture-atlas loading before the task was intentionally interrupted. No in-world interaction was claimed: the player-driven natural/placed mining check after repeated inspection and the real third-party modded block, block-entity, and dimension compatibility matrix remain manual release gates.

The verified sections below are the pre-inspection 1.0.3 public-release baseline. Their 2026-08-30 counts, timings, and artifact identity are intentionally preserved for history and are not the inspection-feature measurements above.

## Verified locally on 2026-08-30

- All five Python validators passed: package/source structure, deterministic release packaging, Mod Menu integration, edge-case contracts, and polish regressions.
- Package validation counted 62 production Java sources. The Windows-native dependency-free core suite passed all 62 assertions.
- Gradle JUnit passed all 47 tests in 12 suites against the mapped Minecraft classes.
- The dedicated-server GameTest runner passed all six required tests; Resource Multiplier contributes five methods covering server-only/mixin boundaries, provenance transitions, real door placement, the real stone `dropResources` path, and reset without provenance loss.
- The real Minecraft 26.2 client GameTest completed with `BUILD SUCCESSFUL` in 32 seconds. It covers the shared child-screen dirty indicator, root-only Apply/Discard actions, Reset Cancel/confirm behavior, compact 320x180 layout, hierarchy/search behavior, and local/integrated/dedicated authority paths.
- A real Mod Menu 20.0.0 `runClient` completed with `BUILD SUCCESSFUL` in 14 minutes 18 seconds. The historical title-screen Mods > former-name Configure route, all major child dirty states, successful Apply clearing, and the red Reset Everything confirmation were manually exercised before the rename.
- A standalone `runServer` without Mod Menu initialized the mod in the server environment, reached `Done (0.256s)`, accepted `stop`, and completed with `BUILD SUCCESSFUL` in 37 seconds.
- The final deterministic source archive was independently inspected: 129 entries under one versioned top-level directory, all required Gradle bootstrap files present, executable mode `755` on `gradlew`, and zero forbidden generated/runtime entries.

The POSIX core launcher was not executed because this Windows host has no installed WSL distribution. `tools/run_core_tests.ps1` compiled the same dependency-free sources and executed the same assertions natively.

## Rule, configuration, and editor result

- `PLAYER_PLACED_ONLY` now excludes natural blocks with placement protection either on or off. `NATURAL_ONLY` excludes placed blocks only while protection is on; with protection off it treats both provenance states as eligible. `ALL` admits both. Independent source toggles and safety/filter rules still apply.
- `minecraft:dragon_egg` is included in the authoritative default blacklist and shipped example. It is not hard-coded in `RuleEngine`; an administrator may remove it and use normal rule resolution.
- Every child screen displays subtle shared-session dirty state near Back. Apply Changes remains available only on the General/root screen, which still switches Done to Discard Changes while dirty. Apply remains one bounded `ConfigPatch`; reset remains a distinct permission-checked, revision-checked atomic operation.
- `fabric.mod.json` contact metadata remains `{}` because this directory has no `.git` metadata or verifiable project-owned URL. `docs/PUBLIC_RELEASE_CHECKLIST.md` records the exact homepage/sources/issues TODO.
- Mob/entity drops remain deliberately absent from 1.0.x. `docs/ROADMAP.md` contains only a conservative, optional 1.1 design note.

## Final clean build and artifact

The final exact `gradlew.bat --no-daemon clean build` completed with `BUILD SUCCESSFUL` in 15 seconds. It compiled every Java 25 source set, restored the input-identical JUnit result from Gradle's verified cache, and launched the dedicated-server GameTest runner, which passed all six required tests. The immediately preceding clean build in the same final pass executed the 47-test JUnit task directly and also completed successfully; the XML result contains 12 suites, 47 tests, zero failures, zero errors, and zero skipped tests.

The inspected playable artifact is `C:\Users\Andrew\Documents\GitHub\SmartResourceDrops\build\libs\smart-resource-drops-1.0.3.jar`:

- 272,738 bytes and 169 ZIP entries
- SHA-256 `41A0604C6D907E1619F5A468487FB0984C3A7863C56F3333D0F9F31CE7697CCC`
- embedded mod version 1.0.3 and contact metadata `{}`
- Java class major 69 (Java 25)
- no JUnit/GameTest classes, bundled Mod Menu or Fabric API classes, or nested dependency JARs

## Remaining release gates and known risks

These are not inferred from compilation, automated clients, or GameTests:

- Perform actual player-driven natural and placed mining at `0x`, `1x`, `2x`, `4x`, and `64x`, including Fortune, Silk Touch, XP on/off, item components, and vanilla gamerules.
- Restart the JVM/world and reload affected chunks to verify persisted provenance. Exercise piston, sticky-piston, and falling-block movement across chunk boundaries and save/restart boundaries.
- Test TNT/explosion decay, a Fabric fake player, representative third-party automated miners, populated vanilla/modded block entities, and a modded block/dimension fixture.
- A third-party machine that removes a marked block but delays its eventual supported `Block.dropResources` call beyond the bounded two-tick recent-removal window can be classified as natural. A mod that manufactures items without a supported drop path remains outside the multiplication boundary.
- Repeat Mod Menu Configure against a separately installed multiplayer server as both non-operator and operator, test missing-server-mod and retry/failure paths, remove Mod Menu and verify the command route, and complete the full GUI-scale/keyboard/mouse/tooltip matrix.
- This working directory has no `.git` metadata. Restore or initialize source control, run CI on the exact release commit, verify the canonical GitHub URL, populate contact metadata only from that verified URL, and compare the published checksum with the uploaded artifact.

The exhaustive publication gate is [docs/PUBLIC_RELEASE_CHECKLIST.md](docs/PUBLIC_RELEASE_CHECKLIST.md).
