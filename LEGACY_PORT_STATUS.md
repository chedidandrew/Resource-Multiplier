# Minecraft 1.7.10 Forge release

This branch contains the tested Smart Resource Multiplier 1.3.2 release for Minecraft 1.7.10. The automated test suite and real Forge client playtest are complete, and `release_ready=true` records that publication approval.

The 1.7.10 edition uses a dedicated Java 8 implementation because modern Minecraft APIs cannot run on this version. It preserves schema-three JSON settings, final block/entity drop multipliers, placement protection, categories and overrides, block and mob XP controls, supported manual shearing, operator commands, a server-authoritative menu, the current icon, and support links where the old Forge runtime exposes equivalent hooks.

It targets the recommended Forge 10.13.4.1614 build. Automated dispenser shearing is unavailable in vanilla Minecraft 1.7.10. Sheep are recognized as standard-resource shearables; compatible modded `IShearable` entities require an explicit `shearingEntityMultipliers` entry because this Minecraft version has no data-pack certification tags. Minecraft 26.2 remains the newest release and the default `main` branch.
