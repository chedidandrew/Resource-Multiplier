# Resource Multiplier public release checklist

## 1.2.0-rc.1 release candidate

This checklist applies to the current Resource Multiplier `1.2.0-rc.1` candidate. An unchecked item remains a release blocker. Keep `release_ready=false`, do not create a `v1.2.0` tag, and do not publish a stable release while any required gate remains open.

## Automated and artifact gates

- [x] Package and metadata validation passes for 118 production Java sources.
- [x] Deterministic source and release-packaging validation passes, including secret, runtime, generated-data, nested-JAR, test-fixture, entrypoint, mixin, icon, license, and contact checks.
- [x] Mod Menu integration, structured-tooltip composition, edge-case, and polish regression checks pass.
- [x] All 90 dependency-free core assertions pass.
- [x] Mapped JUnit passes with zero failures, errors, or skips.
- [x] All 66 required dedicated-server GameTests pass.
- [x] Client GUI and authority GameTests pass under a real Minecraft client runtime.
- [x] The Java 25 Fabric Loom build completes successfully.
- [x] A dedicated server starts without Mod Menu or client-only class loading.
- [x] The playable JAR contains the Resource Multiplier branding, `1.2.0-rc.1` version, `smart_resource_drops` compatibility ID, MIT license, icon, contact metadata, required resources, and no nested JAR or GameTest content.
- [x] The normal build workflow handles branches, pull requests, and manual runs only. The guarded release workflow is the sole `v*` tag handler.
- [x] The guarded release workflow checks `release_ready=true`, tag-to-version equality, and main-branch ancestry before publication.
- [x] The exact clean-checkout GitHub Actions workflow passed for presentation commit `3925ef1cf928763b4d5ff98b9ba9962b3133a8d3`.
- [x] The exact clean-checkout GitHub Actions workflow passed for tooltip-fixed production commit `325cc6a6fcafe5810000f3c377e9e96ea5bd68c9` in run `33351410406`.
- [x] The current clean GitHub Actions JAR was independently inspected: 601,231 bytes, 311 entries, Java class major 69, SHA-256 `B974CABC64698679A81B7E39915CD3092815BDF91406C6E5B9E2DBEBF0A403C9`.
- [x] Local Windows and clean GitHub artifact hashes are recorded separately rather than presented as universally reproducible.
- [x] The development-only GameTest module expands the current project version rather than logging a stale hard-coded `1.0.0` version.
- [x] Structured tooltips show complete clipped row fields before supplemental details and still use Minecraft's standard 170-pixel wrapping.

## Manual gameplay and operations gates

- [ ] Verify natural and player-placed block output at `0x`, `1x`, normal multiplied values, and `64x`.
- [ ] Verify Fortune and Silk Touch final loot is multiplied without rerolling or replacing Minecraft loot behavior.
- [ ] Verify block XP disabled and enabled behavior.
- [ ] Verify explosions against natural and player-placed blocks.
- [ ] Verify supported automated mining and confirm unsupported direct-inventory systems fail closed.
- [ ] Verify block-entity safety with populated chests, barrels, shulker boxes, furnaces, hoppers, beehives, decorated pots, and at least one modded block entity.
- [ ] Verify piston and sticky-piston provenance, including cross-chunk movement.
- [ ] Verify sand, gravel, concrete powder, anvils, and other falling-block provenance through landing and restart.
- [ ] Verify same-position transformations retain provenance without contaminating genuinely generated resources.
- [ ] Verify configuration and placement provenance survive complete client or server restart and chunk unload or reload.
- [ ] Trigger a real block output-budget fallback and verify complete vanilla `1x` output, no partial multiplication, accurate statistics, and rate-limited warnings.
- [ ] Verify normal entity death loot at disabled, `0x`, `1x`, normal multiplied values, and `64x`.
- [ ] Verify Looting, cooked meat, player credit, projectiles, tamed kills, environmental deaths, and all configured kill modes.
- [ ] Verify equipment, picked-up items, saddles, entity inventories, cargo, player deaths, and direct custom output are not multiplied.
- [ ] Verify boss item and XP safety, including protected rare outputs.
- [ ] Verify mob XP disabled and enabled without affecting block or unrelated XP.
- [ ] Verify player sheep shearing at disabled, `0x`, `1x`, `2x`, and `64x`, including color, item components, legal stacks, state, sound, game event, tool damage, and regrowth exactly once.
- [ ] Verify vanilla dispenser sheep shearing at disabled and enabled settings.
- [ ] Verify Mooshroom, Snow Golem, Bogged, Copper Golem, and Sulfur Cube remain fixed vanilla `1x`, including conflicting tag and exact-override attempts.
- [ ] Verify leaves, vines, cobwebs, beehive dispenser action, and leash removal remain outside the entity-shearing subsystem.
- [ ] Trigger a real shearing output-budget fallback and verify the complete original `1x` action is emitted without partial multiplication.
- [ ] Verify every configuration screen at 1280 by 720 and common GUI scales.
- [ ] Verify keyboard and mouse navigation, scrolling, search, tooltips, inheritance, shared dirty state, Back, Apply, Discard, Reset All, and Escape behavior.
- [ ] Verify clipped structured rows with supplemental tooltips display both complete row text and supplemental details.
- [ ] Verify local title-screen defaults, integrated singleplayer authority, multiplayer operator editing, and multiplayer non-operator read-only behavior.
- [ ] Verify a separately installed dedicated server with a client that does not have Mod Menu.
- [ ] Exercise `/smartdrops`, `/smartdropsgui`, shearing status, block and entity inspection, validation, and administrator mutations in a real world or server.
- [ ] Verify `/reload` updates block, entity, and shearing tag classification without restart and reports no missing production tags.
- [ ] Load representative 1.0.x and 1.1.0 configs plus existing saved-world provenance and verify lossless migration and restart behavior.
- [ ] Separately test malformed, unreadable, oversized, and future-schema configuration files.
- [ ] Observe dense farms and high-output scenarios for duplicate output, leaked contexts, warning spam, excessive entities or orbs, or unbounded memory and CPU behavior.
- [ ] Inspect all public screenshots and logs for usernames, UUIDs, server addresses, tokens, file-system paths, world names, or other private data.

## Named third-party compatibility gates

Record an exact project name, exact installed version, date, result, and limitation. Synthetic fixtures and generic category claims do not satisfy these rows.

| Category | Mod or project | Exact version | Test date | Resource Multiplier | Result | Limitation |
| --- | --- | --- | --- | --- | --- | --- |
| Biome or passive-animal content | Not selected | Pending | Pending | 1.2.0-rc.1 | Not performed | Pending |
| Hostile mob | Not selected | Pending | Pending | 1.2.0-rc.1 | Not performed | Pending |
| Boss | Not selected | Pending | Pending | 1.2.0-rc.1 | Not performed | Pending |
| Inventory-bearing mob | Not selected | Pending | Pending | 1.2.0-rc.1 | Not performed | Pending |
| Custom shearable | Not selected | Pending | Pending | 1.2.0-rc.1 | Not performed | Pending |
| Automated miner | Not selected | Pending | Pending | 1.2.0-rc.1 | Not performed | Pending |
| Custom block placement | Not selected | Pending | Pending | 1.2.0-rc.1 | Not performed | Pending |

No generalized compatibility statement is permitted until the relevant row contains reproducible evidence.

## Repository presentation settings

These do not block local code testing, but they should be completed before broad public promotion:

- [ ] Set the repository description to: `Configurable multipliers for block drops, mob loot, and supported shearing, with persistent anti-duplication protection.`
- [ ] Add topics: `minecraft`, `minecraft-mod`, `fabric`, `fabricmc`, `java`, `loot`, `server-side`, `anti-dupe`, `resource-multiplier`.
- [ ] Leave the homepage empty until a real Modrinth or project page exists.
- [ ] Disable Wiki if `docs/` remains canonical.
- [ ] Disable Projects if unused.
- [ ] Enable automatic deletion of merged branches when pull-request development begins.
- [ ] Protect `main` with the Build and verify check when branch protection is desired.

## Final promotion gate

- [ ] Complete every required manual gameplay and operations gate against one exact candidate commit and artifact.
- [ ] Complete every required named third-party compatibility row with reproducible evidence.
- [ ] Rebuild from a clean checkout and record the final automated, manual, third-party, JAR, and checksum evidence.
- [ ] Change `mod_version` from `1.2.0-rc.1` to `1.2.0` only after all gates pass.
- [ ] Keep `release_ready=false` during final validation, then set it to `true` only in the exact fully tested release commit.
- [ ] Merge the exact release commit to `main` and wait for its required clean-checkout Build and verify result.
- [ ] Create the `v1.2.0` publication tag only after every gate above is complete.

Until all promotion steps are complete, Resource Multiplier must be described only as an unpublished release candidate.

The prior full checklist and historical evidence are preserved at [docs/archive/PUBLIC_RELEASE_CHECKLIST-3925ef1-pre-ci-sync.md](archive/PUBLIC_RELEASE_CHECKLIST-3925ef1-pre-ci-sync.md).
