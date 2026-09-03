# Public release checklist — Minecraft 1.20.1

Version `1.3.0+mc1.20.1` is a dual-loader backport. Minecraft 26.2 must remain the newest/default GitHub release.

## Identity and compatibility

- [x] Both loaders retain mod ID, namespace, config path, schema 3, commands, defaults, and GUI behavior.
- [x] General says **Multiply Block XP**; Mob XP remains clearly under Entity Drops.
- [x] Fabric and NeoForge use the same bounded fragmented-config protocol and server-authority policy.
- [x] Resource tags use 1.20.1 plural paths and contain no post-1.20.1 entity/block IDs.
- [x] Fabric and NeoForge JAR filenames are unambiguous and cannot be installed together accidentally.
- [x] No unverified cross-loader world-conversion or universal third-party compatibility claim is made.

## Toolchains and metadata

- [x] Java 17 / class major 61 is required for both targets.
- [x] Fabric pins Minecraft 1.20.1, Loader 0.19.5, Fabric API 0.92.12+1.20.1, and optional Mod Menu 7.2.2.
- [x] NeoForge pins `net.neoforged:forge:1.20.1-47.1.106` through ModDevGradle legacy plugin 2.0.146.
- [x] NeoForge uses legacy `META-INF/mods.toml`, SRG reobfuscation, refmap generation, manifest `MixinConfigs`, and Java-17 mixin compatibility.
- [x] NeoForge bundles exactly MixinExtras Forge 0.5.4 through JarJar; arbitrary or additional nested archives are rejected.

## Automated gates

- [ ] Static package, release-policy, Mod Menu, tooltip, edge-case, polish, and core-rule validators all pass.
- [ ] Fabric clean JUnit, dedicated GameTest, build, 320x180 physical GUI, and separate-process multiplayer/fragmented-wire gates pass.
- [ ] NeoForge clean JUnit, generated GameTest, build/JAR audit, 320x180 physical GUI, multiplayer, optional-installation, and hostile-wire gates pass.
- [ ] The final NeoForge JAR passes the checksum-pinned official Forge 47 installer/server gate under the real `forgeserver` target, including status, validation, config creation, and clean shutdown with no mixin/refmap/JarJar/linkage/client-class errors.
- [ ] Both physical Java 17 client GUI gates pass, and strict final-JAR audits verify each loader's exact metadata, entrypoints, mixins, refmap, classes, and resources. No named userdev launch is presented as production-namespace evidence.
- [ ] Near-limit valid C2S and S2C config transfers work across multiple fragments; malformed/oversized/decompression-abuse traffic fails closed without changing config or harming server health.
- [ ] Test, GameTest, probe, dev metadata, and cross-loader implementation classes are absent from both playable JARs.

## Manual checks

- [ ] Test block loot, Block XP, entity death loot, Mob XP, player/tamed-wolf/non-player attribution, and supported shearing on both final JARs.
- [ ] Test placed/natural blocks, pistons, falling blocks, chunk unload, and a full restart on both loaders.
- [ ] Inspect all GUI screens, tooltips, focus, dirty/apply/reset state, and read-only/operator behavior at representative scales.
- [ ] Verify client-only, server-only, and both-sides installation behavior where supported.
- [ ] Record exact versions, date, results, final file sizes, and SHA-256 hashes.

## Publication

- [x] Release workflow requires exact tag `v1.3.0+mc1.20.1` on `origin/backport/1.20.1` and exact matching loader versions.
- [x] Workflow publishes exactly the Fabric and NeoForge JARs with `make_latest: false`.
- [x] Casual-player release notes exist at `docs/releases/1.3.0+mc1.20.1.md`.
- [ ] Every gate above is complete on the exact final commit.
- [ ] Final commit changes `release_ready=false` to `release_ready=true` only after verification.
- [ ] Push final commit and tag; confirm Minecraft 26.2 remains marked **Latest**.
- [ ] Upload each JAR separately to CurseForge with its correct loader and Minecraft 1.20.1 classification.
