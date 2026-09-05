# Build status

## Smart Resource Multiplier 1.3.2 for Minecraft 1.21.5

This branch is the tested, release-ready source for separate Fabric and NeoForge JARs targeting Minecraft 1.21.5. Minecraft 26.2 remains the newest/default line on `main`.

- Branch: `backport/1.21.5`
- Exact release tag: `v1.3.2+mc1.21.5`
- Version: `1.3.2+mc1.21.5`
- Publication latch: `release_ready=true`
- Java: `21`
- Fabric: Loader `0.19.5`, Fabric API `0.128.2+1.21.5`, optional Mod Menu `14.0.2`
- NeoForge: `21.5.98`, ModDevGradle `2.0.146`

## Release artifacts

- Fabric: `smart-resource-multiplier-1.3.2+mc1.21.5.jar`
  - SHA-256: `81DA2403F572833740FF5112C24702B3DCDB2ED3580758CFF3BB88FC984E2853`
- NeoForge: `smart-resource-multiplier-neoforge-1.3.2+mc1.21.5.jar`
  - SHA-256: `BBC51138D4C5B6303DE8822CAC36CA56F42B1F082B691761E842C130EF9BFE46`

## Verified evidence

- Both loaders pass production compilation, unit tests, and all 64 gameplay GameTests.
- Client GUI smokes verify centered multiplier values and deduplicated structured tooltips and narration.
- Both loaders pass three-start placement-persistence lifecycles.
- The unchanged Fabric JAR passes an exact packaged dedicated-server boot.
- The unchanged NeoForge JAR passes exact packaged server and physical-client probes.
- Artifact audits confirm correct metadata, Java 21 bytecode, and loader isolation.
- Manual in-game acceptance was completed on both loaders.

The guarded release workflow will publish only when the exact tag points to the remote tip of this branch. It sets `make_latest: false`, preserving Minecraft 26.2 as GitHub's **Latest** release.
