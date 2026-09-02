# Smart Resource Multiplier - NeoForge 26.2

This directory is the NeoForge loader target for Minecraft 26.2. It consumes the repository's canonical gameplay, configuration, networking-policy, and GUI sources while keeping NeoForge entrypoints and adapters local to this module.

## Toolchain

- Minecraft 26.2
- NeoForge 26.2.0.72
- ModDevGradle 2.0.144
- Java 25

## Build

From the repository root on Windows:

```powershell
.\gradlew.bat -p neoforge clean build
```

From inside this directory on Windows:

```powershell
..\gradlew.bat clean build
```

Unix-like equivalents are `./gradlew -p neoforge clean build` from the root and `../gradlew clean build` here.

Run the portable NeoForge gameplay suite from the repository root with:

```powershell
.\gradlew.bat -p neoforge runGameTestServer
```

The run is successful only when it reports all 65 required tests passed.

Run the test-only physical-client Entity Categories check with:

```powershell
.\gradlew.bat -p neoforge runClientCategoryTest
```

It opens the production screen, verifies all nine rows and tag-dependent classifications, and exits automatically. This test source is not packaged in the playable JAR.

Run the separate-process multiplayer authority check from two Windows PowerShell windows. Start the server first:

```powershell
.\gradlew.bat -p neoforge --no-daemon runMultiplayerServerTest
```

After that server reports `Done`, start the physical client in the second window:

```powershell
.\gradlew.bat -p neoforge --no-daemon runMultiplayerClientTest
```

The server uses an isolated test world on port `25578`; both isolated game directories are recreated for each run. Success requires both Gradle processes to exit cleanly and both logs to report their multiplayer smoke-test pass marker. On Linux, `bash tools/run_neoforge_multiplayer_smoke.sh` compiles once, coordinates both processes under Xvfb, enforces timeouts, and verifies those markers.

This smoke check negotiates all six config channels; proves a fresh non-operator receives a read-only root screen; promotes that same player; applies and receives a server-authoritative global edit and a maximum-capacity block-rule snapshot; rejects an oversized edit locally before transport; and resets the server configuration. It does not yet cover the optional-channel client-only/server-only matrix, reconnect behavior, entity/filter/shearing child screens, or malicious oversized wire input.

Run the two-JVM placement-provenance migration gate with:

```powershell
.\gradlew.bat -p neoforge --no-daemon runMigrationRestartServerTest
```

That one command seeds a minimal Anvil region with the hash-locked Fabric-authored chunk, launches an import server that loads it through a real `ServerLevel`, checks the gameplay provenance API, and performs a native server save. It then launches a fresh server JVM and requires the saved chunk to contain native NeoForge data without the legacy Fabric envelope and to pass the gameplay lookup again. Independent marker checks make either phase fail closed. This proves the captured Minecraft 26.2 chunk, not a complete-world, older-version, custom-dimension, or modded-registry migration matrix.

The playable artifact is:

```text
neoforge/build/libs/smart-resource-multiplier-neoforge-1.3.0-beta.1.jar
```

Validate the rebuilt playable JAR from the repository root with:

```powershell
py -3 tools/validate_neoforge_jar.py
```

The validator rejects Fabric classes/metadata, test fixtures, nested dependencies, missing mixins, stale loader metadata, the wrong icon, and non-Java-25 bytecode.

## Validation state

The module clean-builds, passes 164 JUnit tests and all 65 dedicated-server GameTests (including NeoForge-native loader and mixin audits), starts a dedicated server through `Done`, passes a physical-client Entity Categories screen check, and passes the separate-process multiplayer authority check described above. Migration now passes both the focused Anvil close/reopen regression and the two-dedicated-JVM `ServerLevel` import, native save, restart, disk-envelope, and gameplay-lookup gate.

It is not yet a public parity build. The remaining gates include the other connected GUI domains, optional-channel and reconnect cases, malicious oversized wire input, and broader whole-world migration coverage beyond the single captured 26.2 chunk. See `docs/NEOFORGE_PORT.md` for the full checklist and the one-way world-migration warning.
