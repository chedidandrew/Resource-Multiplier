# Smart Resource Multiplier - NeoForge 1.21.1

This directory is the NeoForge target for Minecraft 1.21.1. It consumes the canonical gameplay, schema 3 configuration, command, network-policy, and GUI sources while keeping NeoForge lifecycle, transport, fake-player, shearing, and placed-block-storage adapters local to this module. Minecraft 26.2 remains the newest/default release on `main`.

## Toolchain

- Minecraft 1.21.1
- NeoForge 21.1.249
- ModDevGradle 2.0.146
- FML 4 / `javafml`
- Java 21

## Build and tests

From the repository root on Windows:

```powershell
.\gradlew.bat -p neoforge --no-daemon clean test build runGameTestServer
```

The GameTest gate is valid only when the registrar discovers exactly 64 target tests and the server reports all 64 completed successfully.

Run the test-only physical-client category check with:

```powershell
.\gradlew.bat -p neoforge --no-daemon runClientCategoryTest
```

Run the separate real client/server authority gate on Linux with:

```bash
bash tools/run_neoforge_multiplayer_smoke.sh
```

The optional-channel and oversized-wire gates are:

```bash
bash tools/run_neoforge_optional_channel_smoke.sh
bash tools/run_neoforge_oversized_wire_smoke.sh
```

Test entrypoints, probes, fixtures, and structures are run-only and must never appear in the playable JAR.

## Playable artifact

```text
neoforge/build/libs/smart-resource-multiplier-neoforge-1.3.0+mc1.21.1.jar
```

Validate it from the repository root with:

```powershell
py -3 tools/validate_neoforge_jar.py
```

The validator checks `javafml`/FML 4 metadata, exact Minecraft and NeoForge dependencies, all-class Java 21 bytecode, required mixins, icon bytes, and loader/test isolation.

For final release, the candidate JAR must also start in clean packaged-JAR-only NeoForge 21.1.249 server and client profiles. Source-set ModDevGradle runs are development evidence, not substitutes for that final installation test.

## Compatibility boundary

Both loaders preserve mod ID `smart_resource_drops`, `config/smart_resource_drops.json`, schema 3, commands, and network identifiers. On Minecraft 1.21.1 shearing multiplication deliberately supports vanilla Sheep only. Minecraft world downgrades and cross-loader placed-block-data migration are unsupported; back up worlds before changing versions or loaders.

`release_ready` remains `false` until every release checklist gate passes.
