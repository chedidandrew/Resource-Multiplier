# Resource Multiplier public release checklist

## Resource Multiplier 1.2.0

Resource Multiplier `1.2.0` is the stable Fabric release for Minecraft Java Edition 26.2. It promotes the manually validated `1.2.0-rc.1` candidate without gameplay changes.

## Automated and artifact gates

- [x] Package and metadata validation passes.
- [x] Deterministic source and release packaging validation passes.
- [x] Mod Menu, tooltip, edge-case, and polish regression checks pass.
- [x] All dependency-free core assertions pass.
- [x] Mapped JUnit passes with zero failures, errors, or skips.
- [x] All 66 required dedicated-server GameTests pass.
- [x] Client GUI and authority GameTests pass in a real Minecraft client runtime.
- [x] The Java 25 Fabric Loom build completes successfully.
- [x] A dedicated server starts without Mod Menu or client-only class loading.
- [x] The playable JAR contains Resource Multiplier `1.2.0`, the stable `smart_resource_drops` compatibility ID, the MIT license, icon, contact metadata, required resources, and no nested JAR or GameTest content.
- [x] The exact release commit `88bb5a79facdc7da120b912d8a67e9f41ecb57e0` passes the clean-checkout Build and verify workflow in run `33359029127`.
- [x] Annotated tag `v1.2.0` points to the exact tested release commit.
- [x] The guarded release workflow passes in run `33359315520`.
- [x] GitHub Release `v1.2.0` is published with the playable JAR, source archive, release bundle, build status, and checksum files.
- [x] Official playable JAR SHA-256 is `A34BC93E9AD10BDDA1E74B7ADBF397D1EC4D96BEA52F206DDEF5A7A5074BAA6A`.

## Maintainer manual gates

On 2026-08-30, the maintainer confirmed that the complete `1.2.0-rc.1` manual gameplay and operations matrix was performed and passed, including:

- [x] Natural and player-placed block output at `0x`, `1x`, normal multiplied values, and `64x`.
- [x] Fortune, Silk Touch, block XP, explosions, supported automation, and block-entity safety.
- [x] Piston, sticky-piston, falling-block, transformation, restart, chunk reload, and provenance persistence behavior.
- [x] Entity death loot, Looting, cooked drops, attribution modes, equipment and inventory exclusions, boss safety, and mob XP.
- [x] Manual and dispenser shearing, special shearable safety, non-entity shearing exclusions, tool durability, state changes, and output budgets.
- [x] Configuration screens, search, inheritance, tooltips, keyboard and mouse navigation, Apply, Discard, Reset, and read-only permissions.
- [x] Separately installed multiplayer, commands, configuration migration, malformed and future-schema handling, live datapack reload, and high-output scenarios.
- [x] Public screenshots and logs reviewed for private data.

## Third-party compatibility policy

Third-party compatibility remains case-specific and is not represented as a blanket guarantee. Compatibility reports should identify:

- Exact other project and version
- Minecraft version and loader
- Affected block or entity registry ID
- Relevant `/smartdrops inspect verbose`, `/smartdrops inspect entity verbose`, or `/smartdrops validate` output
- Reproduction steps and relevant logs

Representative biome, mob, boss, inventory-bearing entity, custom shearable, automated miner, and custom placement integrations may be documented over time without changing the stable release status.

## Compatibility invariants

The stable promotion preserves:

- Fabric mod ID: `smart_resource_drops`
- Java package: `com.chedidandrew.smartresourcedrops`
- Config path: `config/smart_resource_drops.json`
- Datapack namespace: `smart_resource_drops`
- Commands: `/smartdrops` and `/smartdropsgui`
- Schema 3 configuration and existing migration behavior
- Saved-world placement provenance and network identifiers

## Repository presentation settings

These repository settings are presentation improvements rather than release blockers:

- [ ] Set the repository description to: `Configurable multipliers for block drops, mob loot, and supported shearing, with persistent anti-duplication protection.`
- [ ] Add topics: `minecraft`, `minecraft-mod`, `fabric`, `fabricmc`, `java`, `loot`, `server-side`, `anti-dupe`, `resource-multiplier`.
- [ ] Leave the homepage empty until a real Modrinth or other canonical project page exists.
- [ ] Disable Wiki if `docs/` remains canonical.
- [ ] Disable Projects if unused.
- [ ] Enable automatic deletion of merged branches when pull-request development begins.
- [ ] Protect `main` with the Build and verify check when branch protection is desired.

The complete release-candidate checklist and historical evidence remain preserved under `docs/archive/` and `docs/verification/`.
