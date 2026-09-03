# Smart Resource Multiplier public release checklist

## Smart Resource Multiplier 1.3.1+mc1.21.1

Version `1.3.1+mc1.21.1` is the Minecraft 1.21.1 backport for Fabric and NeoForge. The two JARs share gameplay, schema 3 configuration, commands, GUI, safety budgets, and the `smart_resource_drops` compatibility identity while using loader-specific lifecycle, networking, and placed-block-storage adapters. Minecraft 26.2 remains the newest/default release on `main`.

The publication latch is authorized after local and manual acceptance. The guarded workflow reruns the named gates on the exact tagged commit before publishing.

## Identity and compatibility

- [x] Fabric and NeoForge metadata target Minecraft 1.21.1, Java 21, and version `1.3.1+mc1.21.1`.
- [x] Both builds preserve mod ID and datapack/network namespace `smart_resource_drops`.
- [x] Both builds preserve `config/smart_resource_drops.json`, schema 3, Java packages, `/smartdrops`, and `/smartdropsgui`.
- [x] Both General screens say **Multiply Block XP** and **Block XP Multiplier**; Mob XP remains separately configured under Entity Drops.
- [x] Minecraft 1.21.1 shearing support fails closed to vanilla Sheep; other special vanilla rewards remain `1x`, and custom tagged shearables are not presented as multiplied.
- [x] No newer-world downgrade or cross-loader world-migration claim is made. Players are told to back up worlds before changing versions or loaders.

## Fabric gates

- [ ] Package, metadata, deterministic-source, Mod Menu, tooltip, copy, and policy validators pass.
- [ ] Mapped JUnit and dedicated-server GameTests pass, including loader-native fake-player denial.
- [ ] The real target-native GUI smoke passes and verifies wording, navigation, non-empty Entity Categories, search/configure, and dirty/apply/reset behavior; its run-only harness is absent from the production JAR.
- [ ] Native Fabric placed-block data survives mark/save/restart, can be looked up and removed, and remains absent after a second save/restart.
- [ ] A separate real Fabric client/server pair verifies non-operator denial, promotion, authoritative Apply/Reset revisions, disconnect cleanup, and reconnect.
- [ ] The Java 21 Fabric Loom build produces one test-free, dependency-clean playable JAR.

## NeoForge gates

- [ ] Clean Java 21 build, mapped JUnit tests, and dedicated-server GameTests pass, including loader-native fake-player denial.
- [ ] A dedicated server reaches `Done` without client-classloading or mixin failure.
- [ ] Native NeoForge placed-block data survives mark/save/restart, can be looked up and removed, and remains absent after a second save/restart.
- [ ] A physical client opens Entity Categories and verifies every 1.21.1 row and target-native classification.
- [ ] Clean server and physical-client runs load production classes exclusively from a byte-identical copy of the final NeoForge JAR, with no production source-set substitution.
- [ ] A separate physical client/server pair verifies non-operator denial, operator promotion, authoritative Apply/Reset revisions, disconnect cleanup, and reconnect.
- [ ] Client-only and server-only installations join and disconnect cleanly and unavailable network destinations fail closed.
- [ ] The oversized-wire gate rejects a payload over the decoder limit without changing configuration or revision and leaves the server responsive.
- [ ] The NeoForge JAR validator rejects Fabric crossover, test/probe fixtures, nested dependencies, metadata drift, missing mixins, incorrect icon bytes, and non-Java-21 bytecode.

## Packaging and distribution

- [x] Fabric and NeoForge `mod_version` values match at `1.3.1+mc1.21.1`; the guarded release workflow revalidates every gate before publication.
- [ ] A serialized clean release build produces exactly these two GitHub release assets and no dev/source JARs:
  - `smart-resource-multiplier-1.3.1+mc1.21.1.jar`
  - `smart-resource-multiplier-neoforge-1.3.1+mc1.21.1.jar`
- [x] CurseForge instructions require two separate uploads with the correct loader selected and warn never to install both JARs together.
- [x] `backport/1.21.1` and exact tag `v1.3.1+mc1.21.1` are guarded so the tag must equal the tested branch tip; `make_latest: false` preserves Minecraft 26.2 tag `v1.3.0` as GitHub's **Latest** release.

Record final commands, counts, file sizes, and SHA-256 values only after the release gate is green.
