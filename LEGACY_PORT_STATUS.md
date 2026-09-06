# Minecraft 1.12.2 Forge release

This branch contains the tested Smart Resource Multiplier 1.3.2 release for Minecraft 1.12.2. The automated test suite and real Forge client playtest are complete, and `release_ready=true` records that publication approval.

Implemented for the Java 8 / Forge 1.12.2 runtime:

- schema-three JSON configuration compatibility;
- final block-drop and block-XP multiplication;
- player-placed block provenance saved with each dimension;
- entity death-loot and mob-XP controls;
- supported manual shearing multiplication;
- server-authoritative `/smartdropsgui` configuration screens;
- `/smartdrops` operator commands;
- block categories, exact overrides, filters, dimensions, presets, icon, and support links.

The runtime is compiled against Forge 14.23.5.2847 and is intended to remain compatible with later 14.23.5 Forge builds. Automated dispenser shearing is unavailable in vanilla Minecraft 1.12.2. Minecraft 26.2 remains the newest release and the default `main` branch.
