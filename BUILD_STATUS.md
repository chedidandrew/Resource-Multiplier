# Build status

## Resource Multiplier 1.2.1

Resource Multiplier `1.2.1` is the current stable metadata patch for Minecraft Java Edition 26.2 and Fabric. It updates the optional Mod Menu Website destination to the official CurseForge project, preserves GitHub Issues and source links, and adds optional support links. Gameplay behavior is unchanged from `1.2.0`.

- Version: `1.2.1`
- Publication latch in the tagged release source: `release_ready=true`
- Stable JAR: `resource-multiplier-1.2.1.jar`
- Website: `https://www.curseforge.com/minecraft/mc-mods/resource-multiplier`
- Issues: `https://github.com/chedidandrew/Resource-Multiplier/issues`
- Sources: `https://github.com/chedidandrew/Resource-Multiplier`
- Donation links: Ko-fi, PayPal, and Cash App
- Mod ID and datapack namespace remain `smart_resource_drops`
- Config path remains `config/smart_resource_drops.json`
- Commands remain `/smartdrops` and `/smartdropsgui`

## Verified release identity

- Tested release commit: `34cc5a64c6d97c8db332ce45260c7615a8f9ab97`
- Annotated tag: `v1.2.1`
- Clean-checkout Build and verify run: `33419561475`
- Guarded release workflow run: `33419972111`
- Official JAR size: `601971` bytes
- ZIP entries: `311`
- Official JAR SHA-256: `6E90578892E1F9AA2BF22B8FE4BE1B7831E8BE87F767D14BDB2C376F0443CA32`
- GitHub Release: `https://github.com/chedidandrew/Resource-Multiplier/releases/tag/v1.2.1`

The guarded release workflow rebuilt the exact tagged source, repeated the full Java 25 validation and gameplay test chain, created the deterministic release bundle, and published the official checksums and artifacts.

The stable JAR must be rebuilt from source. Renaming `resource-multiplier-1.2.0.jar` would leave its embedded version and checksums unchanged.

Previous stable release evidence is preserved at [`docs/archive/BUILD_STATUS-1.2.0.md`](docs/archive/BUILD_STATUS-1.2.0.md).
