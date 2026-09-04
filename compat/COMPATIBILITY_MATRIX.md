# Minecraft 1.21.6-1.21.8 compatibility lane

This work is fully tested but remains unpublished. Fabric and NeoForge are
separate artifacts because their entrypoints, networking, placement storage,
and loader hooks are not binary-compatible.

Fabric uses one unchanged production JAR for Minecraft 1.21.6 through 1.21.8.
NeoForge uses an exact 1.21.6 JAR plus one unchanged JAR for 1.21.7 through
1.21.8. The split keeps the NeoForge dependency boundary explicit rather than
claiming an untested cross-minor loader range.

## Verified matrix

| Gate | Fabric 1.21.6 | Fabric 1.21.7 | Fabric 1.21.8 | NeoForge 1.21.6 | NeoForge 1.21.7 | NeoForge 1.21.8 |
| --- | --- | --- | --- | --- | --- | --- |
| Production compile | Pass | Pass | Pass | Pass | Pass | Pass |
| JUnit suite | 164 pass | 164 pass | 164 pass | 165 pass | 165 pass | 165 pass |
| Required gameplay GameTests | 65/65 | 65/65 | 65/65 | 64/64 | 64/64 | 64/64 |
| Automated client GUI smoke | Pass | Pass | Pass | Pass | Pass | Pass |
| Exact packaged-JAR server boot | Pass | Pass | Pass | Pass | Pass | Pass |
| Exact packaged-JAR client GUI probe | Pass | Pass | Pass | Pass | Pass | Pass |

Every exact-JAR row used the candidate retained in `compat/candidates/`; the
candidate was not rebuilt between versions in a shared lane. The client probes
also verify the loader-selected physical origin and version before driving the
production configuration screens. The dedicated-server harness preserves and
compares SHA-256 before and after each launch.

## Candidate artifacts

- Fabric 1.21.6-1.21.8:
  `731D313D3E2FAF8EE71AA50353DC7C407DCF481196B3EA9B57D49917F84F2456`
- NeoForge 1.21.6:
  `6C0437BD8C48E0DB10B2DFA67919B0AD7BD8B45CFD83A79E489B5C611E0DA024`
- NeoForge 1.21.7-1.21.8:
  `504503E6B106690E379009ADAA7CFBB1D0488955007977946717D95C858A4073`

The JAR files are retained locally in `compat/candidates/` but intentionally
ignored by Git. Release binaries belong on the release, not in source control.

`release_ready=false` remains set. Publishing requires a separate reviewed
release action.
