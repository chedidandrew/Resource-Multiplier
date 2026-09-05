# Minecraft 1.21.6-1.21.8 compatibility lane

This release lane is fully tested and authorized for publication. Fabric and NeoForge are
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
| JUnit suite | 167 pass | 167 pass | 167 pass | 168 pass | 168 pass | 168 pass |
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
  `8A610C0B6AD8F50470E00378078C7198A0AA3E22A9474CF37532E9383D6AA9C9`
- NeoForge 1.21.6:
  `9DD8081477D89AB8BB277EF7CDEAB76064C230716E4A3C3AD5A4726829073838`
- NeoForge 1.21.7-1.21.8:
  `B04A087F9A6D11D520F1D98B4BA2AC09BE70F4611595E6384BEBC9A0C7FCB7CA`

The JAR files are retained locally in `compat/candidates/` but intentionally
ignored by Git. Release binaries belong on the release, not in source control.

`release_ready=true` authorizes only the guarded
`v1.3.2+mc1.21.6-1.21.8` release workflow. It verifies that the tag points to
the exact `backport/1.21.6-1.21.8` tip and keeps Minecraft 26.2 marked Latest.
