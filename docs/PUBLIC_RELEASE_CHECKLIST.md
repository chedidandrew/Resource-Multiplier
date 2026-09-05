# Smart Resource Multiplier public release checklist

## Smart Resource Multiplier 1.3.2+mc1.21.9-1.21.10

This release provides one shared Fabric JAR for Minecraft 1.21.9-1.21.10 and exact NeoForge JARs for Minecraft 1.21.9 and 1.21.10. All three preserve gameplay, schema 3 configuration, commands, GUI, safety budgets, and the `smart_resource_drops` compatibility identity. Minecraft 26.2 remains the newest/default release on `main`.

## Identity and compatibility

- [x] All metadata targets Java 21 and only the Minecraft/loader versions named in each JAR.
- [x] All builds preserve mod ID, datapack/network namespace, configuration path, schema, Java packages, and commands.
- [x] Block XP and Mob XP remain clearly separate controls.
- [x] Multiplier values are centered and structured row tooltips do not repeat visible descriptions.
- [x] No newer-world downgrade claim is made; players are told to back up worlds before changing versions or loaders.

## Fabric 1.21.9-1.21.10 gates

- [x] Package, metadata, deterministic-source, Mod Menu, tooltip, copy, and policy validators pass.
- [x] Mapped JUnit and dedicated-server GameTests pass on both target runtimes.
- [x] Target-native GUI, authority, persistence, multiplayer, and exact packaged-JAR gates pass.
- [x] Hands-on client acceptance passes on Minecraft 1.21.9 and 1.21.10 with unchanged JAR bytes.

## NeoForge 1.21.9 and 1.21.10 gates

- [x] Each exact build passes mapped JUnit, dedicated-server GameTests, native persistence, and Fabric-provenance migration.
- [x] Each exact build passes physical-client category/UI, clean packaged server/client, multiplayer, optional-channel, and oversized-wire gates.
- [x] Each exact JAR passes metadata, bytecode, loader-isolation, mixin, icon, and artifact-contamination validation.
- [x] Hands-on client acceptance passes on each exact Minecraft version.

## Packaging and distribution

- [x] The parent `backport/1.21.9-1.21.10` tip remains publication-locked and must be an ancestor of the exact child.
- [x] The exact `backport/1.21.10-neoforge` tip is the only authorized location for tag `v1.3.2+mc1.21.9-1.21.10`.
- [x] The guarded workflow rebuilds and publishes exactly these three assets:
  - `smart-resource-multiplier-1.3.2+mc1.21.9-1.21.10.jar`
  - `smart-resource-multiplier-neoforge-1.3.2+mc1.21.9.jar`
  - `smart-resource-multiplier-neoforge-1.3.2+mc1.21.10.jar`
- [x] CurseForge guidance requires separate uploads with the correct Minecraft version and loader selected.
- [x] `make_latest: false` preserves Minecraft 26.2 tag `v1.3.0` as GitHub's **Latest** release.

The workflow records the authoritative release-build hashes after every gate passes.
