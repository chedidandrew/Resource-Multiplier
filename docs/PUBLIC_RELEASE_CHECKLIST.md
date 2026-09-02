# Smart Resource Multiplier public release checklist

## Smart Resource Multiplier 1.3.0+mc1.21.11

Version `1.3.0+mc1.21.11` is the maintained Minecraft 1.21.11 backport for Fabric and NeoForge. The two JARs share gameplay, schema 3 configuration, commands, GUI, safety budgets, and the `smart_resource_drops` compatibility identity while using loader-specific lifecycle, networking, and placed-block-storage adapters. Minecraft 26.2 remains the newest/default release on `main`.

## Identity and compatibility

- [x] Fabric and NeoForge expose the public name **Smart Resource Multiplier** and version `1.3.0+mc1.21.11`.
- [x] Both builds preserve mod ID and datapack/network namespace `smart_resource_drops`.
- [x] Both builds preserve `config/smart_resource_drops.json`, schema 3, Java packages, `/smartdrops`, and `/smartdropsgui`.
- [x] The General screen says **Multiply Block XP** and **Block XP Multiplier** on both loaders; Mob XP remains separately configured under Entity Drops.
- [x] The production icon is the reviewed `512x512` PNG documented in [`BRANDING.md`](BRANDING.md), SHA-256 `db216ccd6058404de18f797ebb5be87a313899a27c3f1971fdf086b8637dc190`.
- [x] Fabric-to-NeoForge placed-block provenance migration is validated and explicitly documented as one-way. Players are told to back up worlds and not switch repeatedly between loaders.
- [x] Universal mod compatibility and broader whole-world/older-version/custom-dimension migration are not claimed.

## Fabric gates

- [x] Package metadata, deterministic-source, Mod Menu, tooltip, copy, and policy validators pass.
- [x] Core assertions and all 158 mapped JUnit tests pass.
- [x] All 65 Fabric dedicated-server GameTests pass.
- [x] The real Fabric client GUI/authority GameTest passes with the updated block-XP wording.
- [x] The Java 21 Fabric Loom build completes and produces a test-free, dependency-clean playable JAR.

## NeoForge gates

- [x] Clean Java 21 build, 164 JUnit tests, and all 64 NeoForge dedicated-server GameTests pass.
- [x] A dedicated server reaches `Done` without client-classloading or mixin failure.
- [x] A physical client opens Entity Categories and verifies all nine rows plus tag-based classifications.
- [x] A separate physical client/server pair exercises the real `/smartdropsgui` route, non-operator read-only state, operator promotion, entity overrides/filters, shearing, root Apply, confirmed Reset, and server-authoritative results.
- [x] Disconnect cleanup and reconnect use a new connection identity, renegotiate all six channels, and receive a fresh authoritative snapshot.
- [x] Client-only and server-only installations join and disconnect cleanly; unavailable destinations stay unavailable and the connected GUI fails closed.
- [x] A malicious 1,048,577-character wire payload is rejected at the 1,048,576-character limit without changing configuration or revision; the server remains responsive.
- [x] A captured Fabric-authored chunk imports into native NeoForge provenance, saves through a real server, restarts in a second JVM, and remains visible to gameplay lookup.
- [x] The NeoForge JAR validator rejects Fabric crossover, test/probe fixtures, nested dependencies, metadata drift, missing mixins, incorrect icon bytes, and non-Java-21 bytecode.

## Packaging and distribution

- [x] Fabric and NeoForge `mod_version` values match exactly at `1.3.0+mc1.21.11`; the guarded workflow accepts `release_ready=true` only in the final tested release commit.
- [x] Deterministic packaging requires both freshly rebuilt, separately validated JARs.
- [x] Fabric filename: `smart-resource-multiplier-1.3.0+mc1.21.11.jar`.
- [x] NeoForge filename: `smart-resource-multiplier-neoforge-1.3.0+mc1.21.11.jar`.
- [x] The checksum manifest covers both JARs and the source archive.
- [x] CurseForge instructions require two separate file uploads with the correct loader selected and warn never to install both JARs together.
- [x] Backport source remains on `backport/1.21.11`; tag `v1.3.0+mc1.21.11` invokes the guarded publisher with `make_latest: false`, preserving Minecraft 26.2 tag `v1.3.0` as GitHub's **Latest** release.

Exact final file sizes, ZIP-entry counts, and SHA-256 values are recorded in [`BUILD_STATUS.md`](../BUILD_STATUS.md) after the serialized release build.
