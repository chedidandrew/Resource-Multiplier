# Minecraft 1.21.2-1.21.10 compatibility experiment

This work is experimental and has not been published. Fabric and NeoForge remain
separate artifacts because their entrypoints, networking, placement storage, and
loader hooks are not binary-compatible.

## Verified lane: Minecraft 1.21.2-1.21.3

The production artifacts are compiled against Minecraft 1.21.2 and declare the
half-open range `>=1.21.2 <1.21.4` (Fabric) or `[1.21.2,1.21.4)` (NeoForge).

| Gate | Fabric 1.21.2 | Fabric 1.21.3 | NeoForge 1.21.2 | NeoForge 1.21.3 |
| --- | --- | --- | --- | --- |
| Production compile | Pass | Pass | Pass | Pass |
| Required gameplay GameTests | 64/64 | 64/64 | 64/64 | 64/64 |
| Automated client GUI smoke | Pass | Pass | Pass | Pass |
| Exact packaged-JAR server boot | Pass | Pass | Pass | Pass |
| Exact packaged-JAR client probe | N/A | N/A | Pass | Pass |

Fabric's client smoke uses the production source set compiled for the target
runtime. Its separate exact-JAR harness proves that the unchanged distributable
loads on a real dedicated server and retains the same SHA-256 before and after
the run. NeoForge's packaged probes load the unchanged distributable on both the
client and dedicated server. The shared unit-test suites also pass on the
1.21.2 build baseline.

Candidate hashes:

- Fabric: `C4170F36BBF4E8199755997E63478D4B35888903A77DCCAF42753375E73E3AF3`
- NeoForge: `8A46A4F9997E708CE8A541D8E02122733DCAC587E8FE7FA092F8ACEA42B03848`

The JAR files are retained locally in `compat/candidates/` but intentionally
ignored by Git. Release binaries should be attached to a release, not committed
to source control.

## Why one 1.21.2-1.21.10 JAR is unsafe

The next-version compile probe fails on Minecraft 1.21.4 because that version
removes the `AbstractSelectionList` scrolling methods used by
`StructuredConfigList`: `clampScrollAmount()` and `getScrollAmount()`. Later
versions introduce additional entity attribution, persistence, networking,
input, loot, and NeoForge mixin-descriptor changes. Trimming only the first or
last version cannot remove boundaries located inside the requested range.

Conservative source lanes to implement and verify separately:

| Loader | Candidate unchanged-JAR lanes |
| --- | --- |
| Fabric | 1.21.2-1.21.3; 1.21.4; 1.21.5; 1.21.6-1.21.8; 1.21.9-1.21.10 |
| NeoForge | 1.21.2-1.21.3; 1.21.4; 1.21.5; 1.21.6; 1.21.7-1.21.8; 1.21.9; 1.21.10 |

Each advertised Minecraft version must pass the complete matrix with the exact
same JAR bytes before its metadata range is widened.
