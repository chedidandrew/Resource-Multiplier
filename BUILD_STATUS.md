# Build status

## Smart Resource Multiplier 1.3.2+mc1.21.4 candidate lane

This worktree contains separate Fabric and NeoForge candidates for exactly
Minecraft 1.21.4. The lane is tested locally but remains unpublished.

- Branch: `backport/1.21.4-compat`
- Version: `1.3.2+mc1.21.4`
- Java: `21`
- Fabric: Loader `0.19.5`, Fabric API `0.119.4+1.21.4`, optional Mod Menu `13.0.4`
- NeoForge: `21.4.157`, ModDevGradle `2.0.146`
- Publication latch: `release_ready=false`
- Publication state: no tag, push, GitHub release, or other publication is authorized

## Candidate artifacts

- Fabric: `compat/candidates/smart-resource-multiplier-fabric-1.3.2+mc1.21.4.jar`
  - SHA-256: `4B84930EAFF19A7100F3AB9C9718AAB2C7D5A2A83A34CF177EAF945706088129`
- NeoForge: `compat/candidates/smart-resource-multiplier-neoforge-1.3.2+mc1.21.4.jar`
  - SHA-256: `1442F542FD3A68FB86F292AF629CF9B86A27B426E7F45A0713B5F6CF00C9E1F4`

Both local files match `compat/candidates/SHA256SUMS.txt` and the retained
runtime evidence. Candidate JARs are intentionally ignored by Git.

## Verified evidence

- Production compilation and unit tests pass for both loaders.
- Fabric and NeoForge each pass all 64 required gameplay GameTests.
- Both client GUI/category smokes, multiplayer authority/reconnect tests, and
  three-start placement-persistence lifecycles pass.
- NeoForge passes the client-only and server-only optional-installation matrix
  and rejects an oversized network payload without changing server state.
- The unchanged Fabric candidate boots on a dedicated server with its hash
  preserved. The unchanged NeoForge candidate passes both dedicated-server and
  physical-client packaged-JAR probes.
- Package and loader-isolation audits pass. Fabric's exact packaged-JAR harness
  does not include a physical-client probe; its client coverage comes from the
  production-source GUI smoke.

The detailed matrix is in `compat/COMPATIBILITY_MATRIX.md`. Compact replay
evidence is in `compat/evidence/1.21.4-runtime-gates.json`, and the exact Fabric
server result is retained below `compat/fabric-exact-smoke/runtime/`.

This is a local candidate-ready state awaiting manual acceptance. Keep
`release_ready=false`; do not push or publish it without a separate release
decision.
