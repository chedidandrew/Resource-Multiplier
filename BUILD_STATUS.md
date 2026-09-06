# Build status

## Minecraft 26.1–26.1.2 backport

Smart Resource Multiplier `1.3.2` is release-ready for Fabric and NeoForge. Minecraft 26.2 remains the newest release and the default `main` branch.

- Branch: `backport/26.1-26.1.2`
- Tag: `v1.3.2+mc26.1-26.1.2`
- Publication latch: `release_ready=true`
- Runtime: Java 25
- Fabric: one JAR for Minecraft 26.1, 26.1.1, and 26.1.2
- NeoForge: separate JARs for Minecraft 26.1, 26.1.1, and 26.1.2

The Fabric suite passed 161 tests and the NeoForge suite passed 167 tests during release preparation. All six Minecraft/loader combinations then passed real client playtests. The published assets are the same verified JARs uploaded to CurseForge.

Minecraft-version downgrades and cross-loader world conversion are not guaranteed. Back up important worlds and install exactly one loader build.
