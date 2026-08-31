# Build status

## Resource Multiplier 1.2.0

Resource Multiplier `1.2.0` is the stable Fabric release for Minecraft Java Edition 26.2. The maintainer confirmed that the manual `1.2.0-rc.1` release matrix passed on 2026-08-30.

- Version: `1.2.0`
- Publication latch: `release_ready=true`
- Expected JAR: `build/libs/resource-multiplier-1.2.0.jar`
- Mod ID and datapack namespace remain `smart_resource_drops`
- Config path remains `config/smart_resource_drops.json`
- Commands remain `/smartdrops` and `/smartdropsgui`

The stable JAR is rebuilt from source. Renaming the release-candidate JAR is invalid because its embedded Fabric metadata, manifest, package records, and checksums would still identify `1.2.0-rc.1`.

Finalization build: `601807` bytes, `311` ZIP entries, SHA-256 `A34BC93E9AD10BDDA1E74B7ADBF397D1EC4D96BEA52F206DDEF5A7A5074BAA6A`.

Third-party compatibility remains case-specific rather than a blanket guarantee.

## Archived release-candidate evidence
## Resource Multiplier 1.2.0-rc.1

Resource Multiplier `1.2.0-rc.1` remains an unpublished release candidate for Minecraft Java Edition 26.2. Publication is intentionally locked with `release_ready=false`. No release tag or GitHub Release has been created.

Compatibility-critical identifiers remain unchanged:

- Fabric mod ID: `smart_resource_drops`
- Java package: `com.chedidandrew.smartresourcedrops`
- Config path: `config/smart_resource_drops.json`
- Datapack namespace: `smart_resource_drops`
- Commands: `/smartdrops` and `/smartdropsgui`
- Saved-world provenance, network identifiers, schema, and migrations

## Configuration clarity pass - local validation complete

The current working tree contains a deliberately narrow client-presentation correction. The root navigation uses **Block Categories** and **Block Filters** while the child titles remain **Categories** and **Filters**. Dimensions, Advanced, and Entity Drops receive scoped navigation help; the two root XP descriptions explicitly refer to eligible block breaks; and the eight Advanced boolean settings use setting-specific explanations rather than repeating their label and ON/OFF state. Existing detailed tooltips are intentionally preserved.

Resource Multiplier remains `1.2.0-rc.1` with `release_ready=false`. This pass changes no gameplay, default value, configuration field or JSON key, schema or migration, authority rule, persistence behavior, network payload, command, Java package, mod ID, datapack namespace, or saved-world provenance identifier.

Focused label, tooltip, layout, and authority regressions pass. All six repository validators, all 90 dependency-free core assertions, 158 mapped JUnit tests in 24 suites, all 66 dedicated GameTests, the Mod Menu-enabled real client GameTest, the cache-free Java 25 Loom build, standalone server status/validation, and fresh-empty-directory release packaging complete successfully. The client suite covers 320x180, 426x240, 640x360, and 1280x720 tooltip bounds plus local-default, integrated-server, dedicated-operator, and dedicated-non-operator authority. It produced 19 current screenshots; only `docs/images/general-config.webp` changed, while the Block Overrides and Shearing captures remain byte-for-byte unchanged.

The current local playable JAR is `build/libs/resource-multiplier-1.2.0-rc.1.jar`: 601,761 bytes, 311 ZIP entries, SHA-256 `27F73D54F8CD6EFE17F68DFB3C76D11B67BEAB3AB6DF4725F0D398E4D0E5F9C7`. Fresh packaging validates 257 source entries and creates the source ZIP, byte-identical playable JAR, checksum manifest, and five-entry release bundle. A standalone server loads 43 mods without Mod Menu, reaches `Done (0.280s)`, returns general and shearing status, reports `Status: Valid` in compact and verbose validation, confirms no configuration or world data changed, and stops cleanly with `BUILD SUCCESSFUL` in 42 seconds.

The exact production clarity commit is `cb6d17d9a26283ddc98c6e3f7d8fa46ebb341380`. GitHub Actions [run `33354997205`](https://github.com/chedidandrew/Resource-Multiplier/actions/runs/33354997205) completed successfully for that clean checkout. Package policy checks, Java and dedicated-server tests, the Loom build, client GUI/authority tests, and playable-JAR upload all passed. Artifact `ResourceMultiplier-cb6d17d9a26283ddc98c6e3f7d8fa46ebb341380` has artifact ID `9744863404`, size 547,460 bytes, and GitHub ZIP digest `F385B0E9B436AC1B1CCA67BE505BB591436E1E98A928C6B8E199A297D6697E13`.

## Prior verified production-code checkpoint

The latest production-code correction is commit:

```text
325cc6a6fcafe5810000f3c377e9e96ea5bd68c9
```

That commit includes the structured-tooltip composition fix. A clipped row now exposes its complete unabridged row fields before any supplemental hover details, and the combined text still passes through Minecraft's standard 170-pixel tooltip splitter.

GitHub Actions workflow run `33351410406` completed successfully for that exact clean checkout. All configured steps passed:

- package and metadata validation;
- deterministic source and release-packaging validation;
- Mod Menu, tooltip-composition, edge-case, and polish regression checks;
- all 90 dependency-free core assertions;
- mapped JUnit and the Java 25 Loom build;
- all 66 required dedicated-server GameTests;
- client GUI and authority GameTests under Xvfb;
- playable-JAR upload.

The successful clean-checkout run validated 118 production Java sources and 254 Git-tracked source-package entries. The build completed without a project-source compilation failure. Headless runner warnings about audio, narration, authentication, graphics, JOML, operating-system metrics, Node runtimes, and action deprecations are environment or upstream dependency warnings and are not presented as gameplay test coverage.

## Artifact identities

### Prior clean GitHub Actions artifact

Artifact name:

```text
ResourceMultiplier-325cc6a6fcafe5810000f3c377e9e96ea5bd68c9
```

Playable JAR:

```text
resource-multiplier-1.2.0-rc.1.jar
```

Verified identity:

- Size: `601,231` bytes
- ZIP entries: `311`
- Java class major: `69` (Java 25)
- SHA-256: `B974CABC64698679A81B7E39915CD3092815BDF91406C6E5B9E2DBEBF0A403C9`
- Public name: `Resource Multiplier`
- Version: `1.2.0-rc.1`
- Mod ID: `smart_resource_drops`
- Nested JARs: none
- GameTest or testmod content: none
- Runtime world, cache, log, or user data: none

### Earlier exact-candidate artifacts

The local Windows verification build from the presentation pass was:

- Size: `601,044` bytes
- SHA-256: `5CA797D6BC4BBAB6F223361D9A99A118850F40B0273426D3B1ADF8AAB5CDCB31`

The clean GitHub Actions build for presentation commit `3925ef1cf928763b4d5ff98b9ba9962b3133a8d3` was:

- Size: `601,094` bytes
- SHA-256: `F565C5A8CCFE0FD0519900D1C46CB7FF1EFA2097CF2FD6800D821D5AE9BA98B5`

These builds are valid evidence for their named source states, but they are not byte-for-byte identical. They are recorded separately instead of presenting one checksum as universal. The authoritative public-release checksum will be generated by the guarded release workflow for the final tagged `1.2.0` commit.

## Test-only metadata cleanup

The development-only `smart_resource_drops_gametest` module now expands the project version during `processGametestResources`. Test logs therefore identify both the production mod and its GameTest companion as `1.2.0-rc.1` instead of retaining the stale hard-coded `1.0.0` label. This does not change the playable JAR or runtime gameplay.

## Remaining release gates

The clarity production commit `cb6d17d9a26283ddc98c6e3f7d8fa46ebb341380` and prior `325cc6a6fcafe5810000f3c377e9e96ea5bd68c9` checkpoint both passed their exact clean-checkout automated and artifact gates. The remaining release gates require hands-on or named third-party evidence:

- real block, entity death-loot, XP, explosion, provenance, and output-budget gameplay;
- real player and dispenser shearing at disabled, `0x`, `1x`, normal multiplied, and maximum values;
- Mooshroom, Snow Golem, Bogged, Copper Golem, and Sulfur Cube special-safety behavior;
- GUI appearance, keyboard and mouse navigation, Apply, Discard, Reset, search, inheritance, and read-only behavior at common resolutions and GUI scales;
- separately installed operator and non-operator multiplayer, including a client without Mod Menu;
- existing-world restart, configuration migration, and provenance persistence;
- live datapack `/reload` behavior;
- dense farm and high-output observation;
- exact named and versioned third-party tests for biome or passive-animal content, hostile mobs, bosses, inventory-bearing mobs, custom shearables, automated miners, and custom block-placement systems.

Until every required gate closes, keep `release_ready=false`, do not create a `v1.2.0` tag, and do not describe this candidate as a stable public release.

## Repository settings still pending

The source-controlled presentation and release safeguards are in place. These GitHub repository settings still require a repository-settings update:

- Description: `Configurable multipliers for block drops, mob loot, and supported shearing, with persistent anti-duplication protection.`
- Topics: `minecraft`, `minecraft-mod`, `fabric`, `fabricmc`, `java`, `loot`, `server-side`, `anti-dupe`, `resource-multiplier`
- Homepage: leave empty until a real Modrinth or project page exists
- Wiki: disable if `docs/` remains the canonical documentation
- Projects: disable if unused
- Delete merged branches: enable when pull-request development begins

Historical build evidence remains preserved in [the archived pre-sync build status](docs/archive/BUILD_STATUS-3925ef1-pre-ci-sync.md). The exact clean-checkout verification for the tooltip-fixed candidate is recorded in [the 1.2.0-rc.1 CI verification note](docs/verification/1.2.0-rc.1-ci-325cc6a.md).
