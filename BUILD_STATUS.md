# Build status

## Smart Resource Multiplier 1.3.2 for Minecraft 1.21.4

This branch is the tested, release-ready source for separate Fabric and NeoForge JARs targeting Minecraft 1.21.4. Minecraft 26.2 remains the newest/default line on `main`.

- Branch: `backport/1.21.4`
- Exact release tag: `v1.3.2+mc1.21.4`
- Version: `1.3.2+mc1.21.4`
- Publication latch: `release_ready=true`
- Java: `21`
- Fabric: Loader `0.19.5`, Fabric API `0.119.4+1.21.4`, optional Mod Menu `13.0.4`
- NeoForge: `21.4.157`, ModDevGradle `2.0.146`

## Release artifacts

- Fabric: `smart-resource-multiplier-1.3.2+mc1.21.4.jar`
  - SHA-256: `8A470D9BE6EDD6D1E8DF966F5162A6052C950F660FAFFD9698EA12D0F59528A9`
- NeoForge: `smart-resource-multiplier-neoforge-1.3.2+mc1.21.4.jar`
  - SHA-256: `7B792370FF42F066DB024A06B1A00408C55192216EE7059A8285B2341EC20032`

## Verified evidence

- Both loaders pass production compilation, unit tests, and all 64 gameplay GameTests.
- Client GUI/category smokes verify centered multiplier values and deduplicated structured tooltips and narration.
- Both loaders pass multiplayer authority/reconnect and three-start placement-persistence gates.
- NeoForge passes optional-installation, oversized-wire, and exact packaged server/client probes.
- The unchanged Fabric JAR passes an exact packaged dedicated-server boot; its physical-client coverage is the production-source GUI smoke.
- Artifact audits confirm correct metadata, Java 21 bytecode, and loader isolation.
- Manual in-game acceptance was completed on both loaders.

The guarded release workflow will publish only when the exact tag points to the remote tip of this branch. It sets `make_latest: false`, preserving Minecraft 26.2 as GitHub's **Latest** release.
