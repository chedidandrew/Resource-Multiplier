# Build status

## Smart Resource Multiplier 1.3.2 for Minecraft 1.21.2–1.21.3

This branch is the tested, release-ready source for separate Fabric and NeoForge JARs whose unchanged bytes support Minecraft 1.21.2 and 1.21.3. Minecraft 26.2 remains the newest/default line on `main`.

- Branch: `backport/1.21.2-1.21.3`
- Exact release tag: `v1.3.2+mc1.21.2-1.21.3`
- Version: `1.3.2+mc1.21.2-1.21.3`
- Fabric Minecraft range: `>=1.21.2 <1.21.4`
- NeoForge Minecraft range: `[1.21.2,1.21.4)`
- Publication latch: `release_ready=true`
- Java: `21`
- Mod/config identity: `smart_resource_drops`, `config/smart_resource_drops.json`, schema 3

## Release artifacts

- Fabric: `smart-resource-multiplier-1.3.2+mc1.21.2-1.21.3.jar`
  - SHA-256: `3D69559273E30DAFF9B65FD1335EF0F499A52125743CC103DA46CC23AE2AC3CA`
- NeoForge: `smart-resource-multiplier-neoforge-1.3.2+mc1.21.2-1.21.3.jar`
  - SHA-256: `468D86611B61FA1DE310A80A594C0B700F941E0FB49531522E68FCF5E15E6EA1`

## Verified evidence

- Both loaders pass their complete unit suites and all 64 project GameTests on the 1.21.2 baseline.
- The same unchanged JAR bytes pass target-native runtime and GUI gates on Minecraft 1.21.2 and 1.21.3.
- Client GUI smokes verify centered multiplier values and non-duplicated structured-row hover and narration text.
- Fabric exact-JAR dedicated-server boots pass on both versions; NeoForge exact packaged-JAR server and physical-client probes pass on both versions.
- Artifact audits confirm Java 21 bytecode, correct loader metadata, and no test, source, nested-JAR, Minecraft, or opposite-loader contamination.
- Manual in-game acceptance was completed on both versions and loaders.

The guarded release workflow will publish only when the exact tag points to the remote tip of this branch. It sets `make_latest: false`, preserving Minecraft 26.2 as GitHub's **Latest** release.
