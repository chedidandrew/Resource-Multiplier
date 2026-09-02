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

The module clean-builds, passes 163 JUnit tests and all 65 dedicated-server GameTests (including NeoForge-native loader and mixin audits), starts a dedicated server through `Done`, and passes a physical-client Entity Categories screen check. It also has a tested one-way decoder/mixin path for Fabric placement provenance.

It is not yet a public parity build. Connected GUI authority checks, multiplayer and large-payload tests, and a real Fabric-region migration/save/restart test remain open. See `docs/NEOFORGE_PORT.md` for the full checklist and the one-way world-migration warning.
