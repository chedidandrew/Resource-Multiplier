# Smart Resource Multiplier - NeoForge 1.21.11

This directory is the NeoForge loader target for Minecraft 1.21.11. It consumes the repository's canonical gameplay, configuration, networking-policy, and GUI sources while keeping NeoForge entrypoints and adapters local to this module. Minecraft 26.2 remains the newest/default release on `main`.

## Toolchain

- Minecraft 1.21.11
- NeoForge 21.11.45
- ModDevGradle 2.0.146
- Java 21

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

The run is successful only when it reports all 64 required tests passed.

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

This smoke check negotiates all six config channels and invokes the production `/smartdropsgui` command. It proves a fresh non-operator receives a read-only root screen, promotes that same player, navigates and edits the connected entity override/filter and shearing child screens, applies their shared patch from General, receives the exact server-authoritative values, fills block rules to maximum capacity, rejects an oversized edit locally before transport, and completes the real Reset Everything confirmation. It then disconnects with a request outstanding, proves cached authority state was cleared, reconnects over a new connection with all six channels, and receives a fresh editable reset snapshot.

Run the isolated client-only/server-only installation matrix on Linux with:

```bash
bash tools/run_neoforge_optional_channel_smoke.sh
```

This launches two physical client/server pairs with the production mod installed on only one side at a time. It requires both joins and disconnects to complete cleanly, checks each channel direction whose destination lacks the mod is unavailable, and confirms the client-only connected config route fails closed instead of exposing editable local defaults.

Run the isolated malicious oversized-wire gate on Linux with:

```bash
bash tools/run_neoforge_oversized_wire_smoke.sh
```

This bypasses the typed client encoder and sends a 1,048,577-character patch over the negotiated payload ID. The gate requires rejection at the 1,048,576-character decoder limit, unchanged server configuration and revision, 40 subsequent healthy ticks, a responsive `/smartdrops status` command, clean client/server exits, and independent pass markers.

Run the two-JVM placement-provenance migration gate with:

```powershell
.\gradlew.bat -p neoforge --no-daemon runMigrationRestartServerTest
```

That one command seeds a minimal Anvil region with the hash-locked Fabric-authored 1.21.11 chunk, launches an import server that loads it through a real `ServerLevel`, checks the gameplay provenance API, and performs a native server save. It then launches a fresh server JVM and requires the saved chunk to contain native NeoForge data without the legacy Fabric envelope and to pass the gameplay lookup again. Independent marker checks make either phase fail closed. This proves the captured Minecraft 1.21.11 chunk, not a complete-world, older-version, custom-dimension, or modded-registry migration matrix.

The playable artifact is:

```text
neoforge/build/libs/smart-resource-multiplier-neoforge-1.3.0+mc1.21.11.jar
```

Validate the rebuilt playable JAR from the repository root with:

```powershell
py -3 tools/validate_neoforge_jar.py
```

The validator rejects Fabric classes/metadata, test fixtures, nested dependencies, missing mixins, stale loader metadata, the wrong icon, and non-Java-21 bytecode.

## Validation state

The module clean-builds, passes 164 JUnit tests and all 64 dedicated-server GameTests (including NeoForge-native loader and mixin audits), starts a dedicated server through `Done`, passes a physical-client Entity Categories screen check, passes the connected GUI/reconnect authority check, passes the client-only/server-only optional-channel matrix, and rejects malicious oversized wire input without mutating server state. Migration now passes both the focused Anvil close/reopen regression and the two-dedicated-JVM `ServerLevel` import, native save, restart, disk-envelope, and gameplay-lookup gate.

Version `1.3.0+mc1.21.11` is the stable NeoForge counterpart to the Fabric 1.21.11 build for the documented mod behavior. See `docs/NEOFORGE_PORT.md` for the full checklist and the one-way world-migration warning.
