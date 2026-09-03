# Testing Minecraft 1.20.1

This branch targets Minecraft Java Edition 1.20.1 with Java 17. Launch Gradle with Java 21 because the pinned Fabric Loom plugin requires it; Gradle then selects the declared Java 17 toolchain for compilation and Minecraft test processes. A successful compile alone is not release evidence: both loader JARs must pass their target-native server, client, multiplayer, protocol, and packaging gates from the exact candidate commit.

## Static and framework-independent checks

From the repository root:

```powershell
py -3 tools/validate_package.py
py -3 scripts/test_release_packaging.py
py -3 scripts/test_modmenu_integration.py
py -3 scripts/test_structured_tooltip_composition.py
py -3 scripts/edge_case_source_audit.py
py -3 scripts/polish_regression_tests.py
.\tools\run_core_tests.ps1
```

On Linux/macOS, use `python3` and `bash tools/run_core_tests.sh`.

These checks cover source/package structure, exact dependency and metadata policy, bounded rule behavior, GUI composition contracts, release-workflow guards, and loader isolation. They do not replace a Minecraft launch.

## Fabric gates

```powershell
.\gradlew.bat --no-daemon clean test runGameTest build
.\gradlew.bat --no-daemon runClientSmoke
```

On Linux CI, the physical 320x180 client runs under Xvfb and the actual separate-process authority/protocol gate is:

```bash
xvfb-run -a ./gradlew --no-daemon runClientSmoke
bash tools/run_fabric_multiplayer_smoke.sh
```

Required Fabric evidence:

- All JUnit tests pass, including strict UTF-8 fragmentation, malformed-frame rejection, authorization-before-allocation, global transfer bounds, deferred-work replacement, and cleanup.
- Every registered dedicated-server GameTest passes on Minecraft 1.20.1.
- The physical client opens General and every child screen at 320x180, shows **Multiply Block XP**, renders a non-empty Entity Categories screen with expected legacy classifications, and verifies dirty/apply/reset/read-only navigation.
- A real client/server connection proves non-operator read-only behavior, operator apply/reset, a legitimate multi-fragment near-limit patch and snapshot, a second small request, disconnect cleanup, and reconnect.
- The final Fabric JAR contains Fabric metadata only, Java 17 bytecode, the approved icon and license, and no test harnesses.

## NeoForge 47 gates

The 1.20.1 target uses the legacy NeoForge artifact and `mods.toml`, not modern 1.21-era NeoForge APIs or metadata.

```powershell
.\gradlew.bat -p neoforge --no-daemon clean test build runGameTestServer
py -3 tools/validate_neoforge_jar.py
.\gradlew.bat -p neoforge --no-daemon runClientCategoryTest
```

Linux CI also runs:

```bash
bash tools/run_neoforge_multiplayer_smoke.sh
bash tools/run_neoforge_optional_channel_smoke.sh
bash tools/run_neoforge_oversized_wire_smoke.sh
bash tools/run_neoforge_production_server_smoke.sh
```

Required NeoForge evidence:

- All shared and loader-specific JUnit tests pass under Java 17.
- Every generated test uses the target-native 32x8x32 structure and passes under Forge 47's `gameTestServer` launch.
- A physical client renders all nine Entity Categories rows and expected tag/fallback classifications.
- Separate client/server processes prove the same GUI authority and fragmented-config behavior as Fabric.
- Client-only and server-only installations join and disconnect cleanly; config messages fail closed when the other side lacks the channel.
- Malformed, duplicate, mixed, incomplete, direction-spoofed, or decompression-abuse frames cannot mutate configuration or make the server unhealthy.
- `tools/validate_neoforge_jar.py` accepts exactly one pinned MixinExtras Forge JarJar dependency and rejects arbitrary nested archives, Fabric leakage, test code, missing refmaps/mixin manifests, wrong metadata, and non-Java-17 bytecode.
- The final reobfuscated JAR, not a userdev/devlibs artifact, starts under Java 17 on a fresh server installed by the checksum-pinned official Forge 47.1.106 installer. The gate requires the real `forgeserver` target, exact byte-for-byte candidate copy, successful status/validation commands, clean shutdown, canonical config creation, and no mixin/refmap/JarJar/linkage or missing-pack errors.
- Client GUI evidence comes from the physical Java 17 client source-set launch plus strict final-JAR metadata/class/resource/refmap validation. This branch does not mislabel a named userdev client launch as a production-namespace final-JAR test.

## Manual gameplay matrix

Before publication, test both final JARs in clean instances and record loader, exact versions, date, result, and logs:

1. Natural and placed blocks at `0x`, `1x`, `2x`, and `64x`, including Fortune, Silk Touch, block XP, pistons, falling blocks, chunk unload, and restart.
2. Passive, neutral, hostile, aquatic, boss, inventory/equipment, mob XP, direct player kill, tamed-wolf kill, and non-player/fake-player denial cases.
3. Player and dispenser sheep shearing, protected special transformations, and one explicitly certified datapack shearable.
4. `/smartdropsgui`, `/smartdrops status`, `/smartdrops validate`, non-op read-only access, operator changes, Apply, Reset, disconnect, and reconnect.
5. Client-only, server-only, and both-sides installations for each loader where supported.

Do not claim compatibility with an unnamed third-party mod from the vanilla/synthetic fixtures. Do not claim Fabric-to-NeoForge world conversion on this branch: no target-native cross-loader migration fixture is part of the release gate.

## Publication rule

Keep `release_ready=false` while any gate is pending or failing. Set it to true only on the exact fully verified commit, then create tag `v1.3.1+mc1.20.1`. The release workflow re-runs the complete suite, publishes exactly one Fabric JAR and one NeoForge JAR, and uses `make_latest: false` so the Minecraft 26.2 release remains first.
