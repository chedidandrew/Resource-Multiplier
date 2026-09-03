# Smart Resource Multiplier — NeoForge 1.20.1

This module builds the legacy NeoForge/Forge 47 target while consuming the repository's canonical gameplay, configuration, network-policy, and GUI sources. Minecraft 26.2 remains the newest/default release on `main`.

## Toolchain

- Minecraft 1.20.1
- Java 17
- `net.neoforged:forge:1.20.1-47.1.106`
- ModDevGradle legacy plugin 2.0.146
- MixinExtras Forge 0.5.4 bundled through JarJar

## Build and unit tests

From the repository root:

```powershell
.\gradlew.bat -p neoforge --no-daemon clean test build
py -3 tools/validate_neoforge_jar.py
```

The playable output is `neoforge/build/libs/smart-resource-multiplier-neoforge-1.3.0+mc1.20.1.jar`. The final file is SRG-reobfuscated; `build/devlibs` is never a release source.

## Target-native runtime gates

Dedicated GameTests:

```powershell
.\gradlew.bat -p neoforge --no-daemon runGameTestServer
```

Physical 320x180 Entity Categories/GUI smoke:

```powershell
.\gradlew.bat -p neoforge --no-daemon runClientCategoryTest
```

Linux CI runs the coordinated separate-process gates:

```bash
bash tools/run_neoforge_multiplayer_smoke.sh
bash tools/run_neoforge_optional_channel_smoke.sh
bash tools/run_neoforge_oversized_wire_smoke.sh
```

These cover real client/server authority, multi-fragment near-limit configuration traffic, a second request after the large transfer, reset/reconnect cleanup, optional client-only/server-only installation, and malformed/decompression-abuse rejection. Test source sets and probe metadata must not appear in the playable JAR.

## Legacy loader details

This target uses `META-INF/mods.toml`, `net.minecraftforge.*` APIs, `SimpleChannel`/`FriendlyByteBuf`, persistent chunk capabilities, `RegisterGameTestsEvent`, plural 1.20.1 resource-tag folders, Java-17 mixin compatibility, SRG remapping, and a generated refmap. It must not be modernized by copying 1.21-era NeoForge APIs into this branch.

Fabric and NeoForge retain the same logical mod/config/provenance identities, but their chunk persistence envelopes differ. No tested cross-loader world-conversion guarantee is included in this release candidate.

See [the port contract](../docs/NEOFORGE_PORT.md) and [testing guide](../docs/TESTING.md) for the complete release requirements.
