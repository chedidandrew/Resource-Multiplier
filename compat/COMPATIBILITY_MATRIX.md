# Minecraft 1.21.2-1.21.10 compatibility matrix

Fabric and NeoForge remain
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

## Verified Fabric lane: Minecraft 1.21.9-1.21.10

One unchanged Fabric artifact is compiled against Minecraft 1.21.9 and declares
the half-open range `>=1.21.9 <1.21.11`. The same candidate bytes were exercised
with Fabric API `0.134.1+1.21.9` on Minecraft 1.21.9 and Fabric API
`0.138.4+1.21.10` on Minecraft 1.21.10.

| Gate | Fabric 1.21.9 | Fabric 1.21.10 |
| --- | --- | --- |
| Production and client-test compile | Pass | Pass |
| Shared JUnit suite | 164/164 | 164/164 |
| Runtime GameTests (64 project + 1 Fabric API runner) | 65/65 | 65/65 |
| Automated client GUI/authority smoke, including value centering and tooltip deduplication | Pass | Pass |
| Three-restart placement-persistence smoke | Pass | Pass |
| Separate-process multiplayer authority/reconnect smoke | Pass | Pass |
| Exact packaged-JAR dedicated-server boot | Pass | Pass |
| Candidate SHA-256 preserved before/after exact boot | Pass | Pass |

Candidate:

- File: `smart-resource-multiplier-fabric-1.3.2+mc1.21.9-1.21.10.jar`
- Size: `968433` bytes
- SHA-256: `6B27365B16EBA57D12C681154468ACE21A3B217A81C6AD2CA5B9DE30298C8106`
- Metadata: mod version `1.3.2+mc1.21.9-1.21.10`, Fabric Loader
  `>=0.19.5`, Fabric API `>=0.134.1+1.21.9`, Java `>=21`

The artifact audit checked all 272 class files at Java class-file major 65,
verified the metadata and audited mixin/tag set, and found no GameTest, client
smoke, fixture, source, nested-JAR, Fabric Loader, or Minecraft classes bundled
inside the candidate. Hands-on client acceptance also passed on both declared
Minecraft versions. Publication is deliberately delegated to the exact 1.21.10
NeoForge child branch so all three release assets share one guarded tag.

## Verified NeoForge lane: Minecraft 1.21.9

NeoForge crosses an API boundary before Minecraft 1.21.10, so this artifact is
intentionally exact to Minecraft 1.21.9 and NeoForge 21.9.16-beta. It retains
the 1.21.9 three-argument `ServerPlayerGameMode#removeBlock` hook and declares
exact metadata ranges rather than claiming compatibility with 1.21.10.

| Gate | NeoForge 1.21.9 |
| --- | --- |
| Clean production build and shared JUnit suite | Pass (170/170) |
| Runtime GameTests | Pass (64/64) |
| Automated physical-client GUI/category smoke, including value centering, tooltip deduplication, and Copper Golem classification | Pass |
| Three-restart native placement-persistence smoke | Pass |
| Fabric provenance import and NeoForge-native restart smoke | Pass |
| Separate-process multiplayer authority/reconnect smoke | Pass |
| Optional-channel client-only and server-only matrix | Pass |
| Oversized-wire rejection and post-rejection server health | Pass |
| Exact preserved-JAR dedicated-server probe | Pass |
| Exact preserved-JAR physical-client navigation/category probe | Pass |
| Candidate SHA-256 preserved in both packaged-profile copies | Pass |

Candidate:

- File: `smart-resource-multiplier-neoforge-1.3.2+mc1.21.9.jar`
- Size: `965324` bytes
- SHA-256: `614D57CC846F1C34B8240938F9667D05D2B88D8A5031B9609DFAC3234879634C`
- Metadata: mod version `1.3.2+mc1.21.9`, JavaFML `[4,)`, NeoForge
  `[21.9,21.10)`, Minecraft `[1.21.9]`, Java 21 bytecode

The artifact audit checked all 277 class files and 307 total file entries,
verified every class as Java class-file major 65, and found no Fabric platform,
development-test, nested-JAR, source, or GameTest contamination. The Fabric
provenance importer gives same-version Fabric-to-NeoForge world moves a tested
one-way migration path while native NeoForge attachment data remains
authoritative. Hands-on client acceptance passed. The parent branch remains
locked because the combined release tag must live on the exact 1.21.10 NeoForge
child tip.

## Verified NeoForge lane: Minecraft 1.21.10

Minecraft 1.21.10 adds an `ItemStack` argument to
`ServerPlayerGameMode#removeBlock`, so NeoForge uses a separately compiled exact
artifact with the four-argument hook and strict 1.21.10 metadata. The shared
gameplay, configuration, GUI, networking policy, safety budgets, and provenance
migration remain aligned with the other loader lanes.

| Gate | NeoForge 1.21.10 |
| --- | --- |
| Clean production build and shared JUnit suite | Pass (170/170) |
| Runtime GameTests | Pass (64/64) |
| Automated physical-client GUI/category smoke, including Copper Golem classification | Pass |
| Three-restart native placement-persistence smoke | Pass |
| Fabric provenance import and NeoForge-native restart smoke | Pass |
| Separate-process multiplayer authority/reconnect smoke | Pass |
| Client-only and server-only installation matrix | Pass |
| Oversized-wire rejection and post-rejection server health | Pass |
| Exact preserved-JAR dedicated-server probe | Pass |
| Exact preserved-JAR physical-client navigation/category probe | Pass |
| Candidate SHA-256 preserved in build, candidate, server, and client copies | Pass |

Candidate:

- File: `smart-resource-multiplier-neoforge-1.3.2+mc1.21.10.jar`
- Size: `965351` bytes
- SHA-256: `AE51D8DFFC384A91521BECA059163BF3A94FAB7B233C7C62246D3D2C1E886FC2`
- Metadata: mod version `1.3.2+mc1.21.10`, JavaFML `[4,)`, NeoForge
  `[21.10,21.11)`, Minecraft `[1.21.10]`, Java 21 bytecode

The artifact audit checked all 277 class files and 307 file entries, verified
every class as Java class-file major 65, and found no Fabric platform,
development-test, nested-JAR, source, or GameTest contamination. Publication
remains locked with `release_ready=false`.

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
