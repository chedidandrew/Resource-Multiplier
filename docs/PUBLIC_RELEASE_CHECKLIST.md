# Smart Resource Multiplier public release checklist

## Smart Resource Multiplier 1.3.2+mc1.21.6-1.21.8

This lane publishes three native artifacts: one Fabric JAR for Minecraft
1.21.6-1.21.8, one NeoForge JAR for 1.21.6, and one NeoForge JAR for
1.21.7-1.21.8. All preserve schema 3, commands, GUI, safety budgets, and the
`smart_resource_drops` compatibility identity. Minecraft 26.2 remains the
newest/default release on `main`.

## Identity and compatibility

- [x] Fabric metadata declares Minecraft `>=1.21.6 <1.21.9` and Java 21.
- [x] NeoForge metadata is split at the tested 21.6/21.7 dependency boundary.
- [x] Every artifact preserves the mod ID, config path, schema, namespaces, and
  `/smartdrops` and `/smartdropsgui` commands.
- [x] No Minecraft world-downgrade or cross-loader placement-data migration
  claim is made.

## Runtime gates

- [x] Fabric and both NeoForge profiles compile and pass their mapped JUnit and
  dedicated-server GameTest suites.
- [x] Physical client GUI checks pass on Minecraft 1.21.6, 1.21.7, and 1.21.8
  for both loaders.
- [x] Every exact candidate passes isolated packaged-JAR server and client
  probes on every version declared in its metadata.
- [x] The user manually accepted the NeoForge 1.21.6, 1.21.7, and 1.21.8
  interfaces after the tooltip-deduplication fix.
- [x] Regression tests ensure structured row tooltips and narration do not
  duplicate visible text.

## Packaging and distribution

- [x] `release_ready=true` appears only in this fully tested release commit.
- [x] Tag `v1.3.2+mc1.21.6-1.21.8` must equal the exact
  `backport/1.21.6-1.21.8` tip.
- [x] The workflow rebuilds the NeoForge 1.21.6 variant with quoted explicit
  Gradle properties rather than editing tracked property files.
- [x] The publisher exposes exactly these three playable assets:
  - `smart-resource-multiplier-1.3.2+mc1.21.6-1.21.8.jar`
  - `smart-resource-multiplier-neoforge-1.3.2+mc1.21.6.jar`
  - `smart-resource-multiplier-neoforge-1.3.2+mc1.21.7-1.21.8.jar`
- [x] `make_latest: false` preserves Minecraft 26.2 tag `v1.3.0` as GitHub's
  **Latest** release.
