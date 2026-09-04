# Build status

## Smart Resource Multiplier 1.3.2+mc1.21.5 candidate lane

This worktree contains separate Fabric and NeoForge candidates for exactly
Minecraft 1.21.5. The lane is tested locally but remains unpublished.

- Branch: `backport/1.21.5-compat`
- Version: `1.3.2+mc1.21.5`
- Java: `21`
- Fabric: Loader `0.19.5`, Fabric API `0.128.2+1.21.5`, optional Mod Menu `14.0.2`
- NeoForge: `21.5.98`, ModDevGradle `2.0.146`
- Publication latch: `release_ready=false`
- Publication state: no tag, push, GitHub release, or other publication is authorized

## Candidate artifacts

- Fabric: `compat/candidates/smart-resource-multiplier-fabric-1.3.2+mc1.21.5.jar`
  - SHA-256: `1ABD817E395AE13585FC71D4A35E8CB637B4073C4C105ADDF9AB36EA98BF79A4`
- NeoForge: `compat/candidates/smart-resource-multiplier-neoforge-1.3.2+mc1.21.5.jar`
  - SHA-256: `67D9EFE5A3E40D3CB4B13DE59BF8A26A562D7470DF77F0BE423CF3BBC65813C0`

Both local files match `compat/candidates/SHA256SUMS.txt`. Candidate JARs are
intentionally ignored by Git.

## Verified evidence

- Production compilation and unit tests pass for both loaders.
- Fabric and NeoForge each pass all 64 required gameplay GameTests.
- Both automated client GUI smokes and three-start placement-persistence
  lifecycles pass.
- The unchanged Fabric candidate passes its exact packaged-JAR dedicated-server
  boot. The unchanged NeoForge candidate passes exact packaged-JAR server and
  physical-client probes.
- Metadata, Java-bytecode, and loader-contamination audits pass for both JARs.
- Fabric's exact packaged-JAR harness does not include a physical-client probe;
  its client coverage comes from the production-source GUI smoke.

The detailed lane record is in `compat/COMPATIBILITY_MATRIX.md`; candidate
hashes are in `compat/candidates/SHA256SUMS.txt`, and the exact Fabric server
result is retained below `compat/fabric-exact-smoke/runtime/`.

This is a local candidate-ready state awaiting manual acceptance. Keep
`release_ready=false`; do not push or publish it without a separate release
decision.
