# Contributing

## Scope charter

Resource Multiplier multiplies final loot produced by qualifying block breaks, living-entity deaths, and supported entity shearing actions. It does not change resource generation, harvesting speed, crafting, processing, transportation, storage, spawning, combat, or world progression.

Changes are in scope when they strengthen server-authoritative final-loot multiplication, placement/death provenance or attribution, the supported player/vanilla-dispenser shearing scope, loot-boundary compatibility, bounded configuration and diagnostics, performance safety, or datapack/mod interoperability at the block-loot, standard entity death-table, and standard shearing-helper boundaries.

Do not add vein mining, tree felling, automatic smelting, magnets, inventory sorting/movement, tool or enchantment rebalancing, or per-biome/tool/enchantment/weather/time rule layers here. Chest/structure loot, fishing, bartering, trading, milking, brushing, egg laying, gifts, breeding output, spawning, crafting/processing, player or armor-stand deaths, equipment, held or picked-up items, entity inventories, direct equipment ejection, beehive/block shearing through the entity subsystem, unsupported custom-machine shearing, and other custom/direct item creation remain excluded. A compatibility change must start from a concrete reproducible case and fail closed outside the supported boundary.

Version 1.1.x has no supported public Java API. Public implementation types are not compatibility promises. Use the documented configuration, commands, project-owned datapack tags, and protected-output tag; a Java API remains deferred until a real third-party case demonstrates that those surfaces cannot solve the integration safely.

## Security and privacy

Read `SECURITY.md` before reporting a duplication exploit, permission bypass, malformed-packet issue, denial of service, or other security-sensitive defect. Do not place exploit details, secrets, player UUIDs, server addresses, personal paths, or unrelated logs in a public issue or pull request. Use the issue forms for ordinary bugs and exact mod-compatibility cases, and redact diagnostic output before posting it.

Use the [GitHub issue chooser](https://github.com/chedidandrew/Resource-Multiplier/issues/new/choose): `.github/ISSUE_TEMPLATE/bug_report.yml` covers ordinary defects and `.github/ISSUE_TEMPLATE/mod_compatibility.yml` covers a reproducible conflict with an exact mod version. Use `.github/PULL_REQUEST_TEMPLATE.md` for changes. Blank issues remain available when neither form fits. The templates guide reporting; they do not replace the sensitive-reporting restrictions in `SECURITY.md`.

## Change checklist

Before submitting a change:

1. Update `CHANGELOG.md` and any affected documentation.
2. Run every source and release-packaging validator listed in `docs/TESTING.md`.
3. Run `tools/run_core_tests.ps1` on Windows or `bash tools/run_core_tests.sh` on POSIX.
4. Run `./gradlew --no-daemon clean test runGameTest build` with Java 25 (`gradlew.bat` on Windows).
5. Test affected natural, player-placed, piston-moved, falling, transformed, automation, block-entity, entity death-loot/XP, attribution, multiplayer-authority and GUI cases in game.
6. Include a mapped-class JUnit or dedicated-server GameTest regression for every bug fix when practical; do not present static source checks as runtime proof.
7. Review `-Xlint:deprecation` output and remove project-source deprecations with supported Minecraft/Fabric APIs. Do not silence a package or turn unrelated dependency warnings into a blanket failure policy.
8. For validation or output-budget work, test compact/verbose bounds, unknown-reference preservation, integer overflow, whole-list `1x` fallback, statistics, and warning throttling. Record final counts and artifact identity only from the serialized release run.
9. Keep the playable JAR free of nested/shaded dependencies, fixtures/tests, source files, configuration, logs, worlds, caches, and release bundles. Keep required public templates, documentation, wrapper files, tags, and key sources in the deterministic source archive.

Avoid new runtime dependencies unless the benefit clearly outweighs the added footprint.
