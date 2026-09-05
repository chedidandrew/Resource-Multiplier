# Smart Resource Multiplier public release checklist

## 1.3.2+mc1.21.2-1.21.3

This checked record covers the separate Fabric and NeoForge releases for Minecraft 1.21.2 and 1.21.3. Minecraft 26.2 remains the newest/default release on `main`.

## Identity and compatibility

- [x] Both loader metadata files declare version `1.3.2+mc1.21.2-1.21.3`, Java 21, and only Minecraft 1.21.2–1.21.3.
- [x] Mod ID, configuration identity, schema 3, commands, datapack namespace, and server authority remain compatible.
- [x] Fabric and NeoForge use separate JARs; no cross-loader placed-block-data migration is promised.
- [x] Block XP is named explicitly, multiplier values are centered, and structured tooltips are not duplicated.

## Verification

- [x] Package, deterministic-source, metadata, loader-isolation, icon, license, and Java-bytecode audits pass.
- [x] Both loaders pass unit tests and exactly 64 required GameTests.
- [x] Target-native GUI and runtime checks pass on both Minecraft versions.
- [x] Exact Fabric server probes and exact NeoForge server/client probes preserve candidate hashes.
- [x] Manual in-game acceptance passes for Fabric and NeoForge on both supported Minecraft versions.

## Publication

- [x] `release_ready=true` is set on the tested source commit.
- [x] The workflow permits only tag `v1.3.2+mc1.21.2-1.21.3` at the tip of `backport/1.21.2-1.21.3`.
- [x] The release contains exactly the Fabric and NeoForge playable JARs named in `BUILD_STATUS.md`.
- [x] `make_latest: false` preserves Minecraft 26.2 as GitHub's **Latest** release.
