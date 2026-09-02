# NeoForge 26.2 Port

## Status

Implementation is now buildable on both loaders. On 2026-09-01, the NeoForge module passed a clean compile/build, 164 JUnit tests (158 shared tests plus 6 NeoForge-focused tests), and all 65 dedicated-server GameTests. Those server tests include the complete shared entity-death, mob-XP, placement/provenance, block-loot-budget, and shearing/dispenser suites plus NeoForge-native server-loader and mixin audits. A dedicated NeoForge server reached `Done`. A physical NeoForge client reached the title screen, opened the production Entity Categories screen, rendered all nine rows and Back, and verified tag-dependent Enderman and Iron Golem classifications without mod or mixin crashes. A separate-process physical client/dedicated server smoke also passed six-channel negotiation, non-operator read-only authority, operator promotion, server-authoritative global and maximum-capacity block edits/snapshots, local oversized-edit rejection, and reset. Fabric passed a clean build, all 158 JUnit tests, and all 66 dedicated-server GameTests after the adapter refactor.

This remains a validation build rather than a public parity release. The remaining network gates include the optional-channel installation matrix, reconnect behavior, entity/filter/shearing GUI edits, and malicious oversized wire input. The captured Fabric chunk now passes both a focused native Anvil close/reopen regression and a two-dedicated-JVM `ServerLevel` import, real server save, restart, disk-envelope, and gameplay-lookup gate. Broader whole-world migration coverage remains distinct from that one-chunk proof.

## Public repository layout

The staged multi-loader layout keeps one gameplay implementation while making loader boundaries explicit:

```text
/
├── src/main/java/       # canonical gameplay, config, payload, and shared server code
├── src/client/java/     # canonical GUI/client code
│   └── .../platform/    # thin Fabric client adapters
├── neoforge/            # NeoForge build, metadata, adapters, mixins, and focused tests
├── docs/                # shared behavior, compatibility, testing, and release history
├── config-examples/     # shared config schema/examples
└── tools/               # release and validation tooling
```

The NeoForge build consumes the two canonical source trees while excluding `platform/fabric`, then adds its own entrypoints, networking, persistent storage, client hooks, and mixins. A future cleanup may promote the trees to top-level `common/`, `fabric/`, and `neoforge/` modules, but that cosmetic move is deferred until behavior parity is proven.

## Compatibility contract

The NeoForge port keeps these identifiers stable:

- `smart_resource_drops` mod ID and datapack/network namespace
- `config/smart_resource_drops.json`
- schema 3 configuration and migration behavior
- `smart_resource_drops:placed_blocks` provenance identity
- `/smartdrops` and `/smartdropsgui`
- `com.chedidandrew.smartresourcedrops` Java namespace

The implementation is designed to preserve multiplier precedence, source modes, smart placement protection, block-entity protection, piston/falling-block propagation, block/entity/shearing output budgets, XP rules, permissions, GUI revision handling, config atomicity, and malformed/oversized config recovery. The unchecked release gates below still separate that design contract from proven runtime parity.

## Implemented loader adapter map

- Fabric and NeoForge entrypoints install their config path, command, lifecycle/tick, fake-player, networking, and placement-storage services before common initialization.
- Fabric and NeoForge networking preserve the existing payload IDs/codecs. NeoForge channels are optional so server-only and client-optional installations can connect.
- Physical-client entrypoints keep GUI and client networking classes off dedicated servers.
- `ClientNetworkBridge` and `ClientModResources` keep client state and installed-mod tag discovery loader-neutral.
- Fabric uses its persistent chunk attachment; NeoForge uses a nonsynced data attachment with the same logical identity.
- NeoForge-specific mixins cover placement rollback boundaries, successful post-loot break cleanup, NeoForge `IShearable` dispenser behavior, and legacy Fabric chunk-data capture.
- Mod Menu remains Fabric-only. NeoForge exposes the same editor through its native config-screen factory.

These adapters are compile- and startup-validated, and all 65 dedicated-server GameTests pass on NeoForge, including the NeoForge-native loader and mixin audits. That is not yet a claim of complete behavior parity.

## Existing-world migration

Fabric and NeoForge use different chunk-NBT envelopes for attachments. The NeoForge port therefore includes a lazy, one-way importer for Fabric's `smart_resource_drops:placed_blocks` list:

1. Chunk parsing decodes valid Fabric provenance without touching world state on the background thread.
2. NeoForge's main-thread chunk-load event imports it only when native NeoForge provenance is absent.
3. The chunk is marked unsaved so its next save uses NeoForge's native attachment format.
4. Malformed legacy data is ignored instead of crashing the chunk load.

The decoder and runtime mixin carrier have focused tests. A captured Fabric-authored chunk artifact verifies legacy import, native NeoForge attachment serialization, Anvil-region write/close/reopen, native deserialization, and native-data precedence after reopen. The two-JVM server gate then embeds those exact hash-locked bytes in a minimal Anvil region: the first dedicated server loads the chunk through `ServerLevel`, confirms the gameplay placement lookup, and performs a real server save; the second dedicated-server JVM confirms the legacy envelope was removed, native disk data survived, and the gameplay lookup still succeeds. This proves one authentic Minecraft 26.2 Fabric chunk, not complete-world, older-version, custom-dimension, or modded-registry migration. Always back up a world before changing loaders. Migration is intentionally one-way; repeatedly switching the same world between Fabric and NeoForge is unsupported because a NeoForge save does not preserve Fabric's attachment envelope.

## NeoForge baseline

- Minecraft `26.2`
- NeoForge `26.2.0.72`
- ModDevGradle `2.0.144`
- Java `25`

## Release gates

- [x] NeoForge clean compile/build
- [x] Shared JUnit suite plus focused NeoForge migration/category tests (164 total)
- [x] Dedicated server reaches `Done` with no client-classloading or mixin crash
- [x] Physical client opens Entity Categories with all nine rows, Back, and tagged Enderman/Golem classifications
- [x] NeoForge dedicated-server harness: all 65 entity/XP, placement/provenance, shearing/dispenser, block-output-budget, and native loader/mixin-audit GameTests
- [x] Shared entity fixtures have loader-specific Fabric and NeoForge registration/final-loot adapters
- [x] Fabric clean build, JUnit suite, and all 66 dedicated-server GameTests after the adapter refactor
- [x] Fabric provenance decoder and NeoForge parse-mixin carrier tests
- [x] Captured Fabric-authored chunk import, NeoForge native attachment save, and Anvil-region close/reopen persistence test
- [x] Standalone NeoForge JAR validator rejects loader crossover, test fixtures, nested dependencies, missing mixins, and metadata/icon drift
- [x] Captured Fabric chunk loaded through `ServerLevel`, saved by a real server, restarted in a second JVM, and verified through native disk data plus gameplay-level provenance lookup
- [x] Separate-process six-channel multiplayer smoke: non-op read-only, operator promotion, authoritative global and maximum-capacity block patches/snapshots, local oversized-edit rejection, and reset
- [ ] Connected NeoForge `/smartdropsgui` command plus entity overrides/filters, shearing, and child-screen Apply/Reset checks
- [x] NeoForge-native replacements for both Fabric-loader-only mixin audits
- [ ] Optional-channel client-only/server-only compatibility matrix and disconnect/reconnect behavior
- [ ] Server-side rejection of malicious oversized wire input
- [ ] Separate Fabric and NeoForge release documentation and platform metadata

## Packaging policy

The established Fabric artifact remains `smart-resource-multiplier-<version>.jar`. NeoForge must remain unambiguous as `smart-resource-multiplier-neoforge-<version>.jar`. Never publish the two loader builds under the same filename or mark the NeoForge artifact as Fabric-compatible.
