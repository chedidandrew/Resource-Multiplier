# Build status

## Smart Resource Multiplier 1.3.2 — Minecraft 1.21.6-1.21.8 lane

This branch prepares three unpublished Java 21 artifacts:

- Fabric `1.3.2+mc1.21.6-1.21.8`, built against Minecraft 1.21.6,
  Fabric Loader 0.19.5, and Fabric API 0.128.2+1.21.6.
- NeoForge `1.3.2+mc1.21.6`, built against NeoForge 21.6.20-beta.
- NeoForge `1.3.2+mc1.21.7-1.21.8`, built against Minecraft 1.21.7 and
  NeoForge 21.7.25-beta.

Fabric uses one unchanged JAR on Minecraft 1.21.6, 1.21.7, and 1.21.8.
NeoForge keeps 1.21.6 separate and uses one unchanged JAR on 1.21.7 and
1.21.8. Mod/config identity remains `smart_resource_drops`,
`config/smart_resource_drops.json`, schema 3; gameplay, commands, GUI,
authority, persistence policy, and safety budgets remain aligned across the
two loaders.

## Verification

- Production compilation and JUnit pass on every declared loader/version
  target (164 Fabric tests and 165 NeoForge tests).
- Fabric passes all 65 required GameTests and its physical client GUI smoke on
  1.21.6, 1.21.7, and 1.21.8.
- NeoForge passes all 64 required GameTests and its physical client GUI smoke
  on 1.21.6, 1.21.7, and 1.21.8.
- Every candidate passes isolated exact-JAR server and client probes on every
  Minecraft version declared by its metadata. Shared-lane candidates retain
  identical SHA-256 bytes across all target launches.
- Candidate hashes and the detailed gate matrix are recorded under `compat/`.

## Publication state

- Branch: `backport/1.21.6-1.21.8-compat`
- Publication latch: `release_ready=false`
- No commit, tag, push, or release was created by this compatibility pass.
- Minecraft 26.2 remains the newest/default release line on `main`.
