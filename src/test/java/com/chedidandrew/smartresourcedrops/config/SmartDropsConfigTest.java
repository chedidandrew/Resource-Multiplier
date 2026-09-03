package com.chedidandrew.smartresourcedrops.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SmartDropsConfigTest {
    @Test
    void freshConfigPersistsStableSchemaAndExactKeySet(@TempDir Path directory) throws IOException {
        Path path = directory.resolve("smart_resource_drops.json");

        assertTrue(ConfigManager.load(path));

        JsonObject persisted = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        assertEquals(SmartDropsConfig.CURRENT_SCHEMA, persisted.get("schemaVersion").getAsInt());
        assertEquals(Set.of(
                "schemaVersion",
                "enabled",
                "globalMultiplier",
                "maximumMultiplier",
                "sourceMode",
                "filterMode",
                "smartPlacementProtection",
                "protectBlockEntities",
                "playerMining",
                "explosions",
                "automatedMining",
                "multiplyExperience",
                "experienceMultiplier",
                "conservativePistonProtection",
                "allowPlayerOverrides",
                "maxPlayerMultiplier",
                "statisticsEnabled",
                "entityDropsEnabled",
                "inheritDefaultEntityMultiplier",
                "defaultEntityMultiplier",
                "entityKillRequirement",
                "entityFilterMode",
                "bossDropsEnabled",
                "multiplyMobExperience",
                "mobExperienceMultiplier",
                "multiplyBossExperience",
                "manualShearingDropsEnabled",
                "automatedShearingDropsEnabled",
                "inheritDefaultShearingMultiplier",
                "defaultShearingMultiplier",
                "dimensionMultipliers",
                "categoryMultipliers",
                "blockMultipliers",
                "blacklist",
                "whitelist",
                "tagBlacklist",
                "tagWhitelist",
                "blockEntityAllowlist",
                "playerMultipliers",
                "entityCategoryMultipliers",
                "entityMultipliers",
                "entityBlacklist",
                "entityWhitelist",
                "entityTagBlacklist",
                "entityTagWhitelist",
                "shearingEntityMultipliers"), persisted.keySet());
    }

    @Test
    void freshAndExistingFileFallbackDefaultsHaveDistinctShearingSafety() {
        SmartDropsConfig fresh = SmartDropsConfig.defaults();
        SmartDropsConfig existingFileFallback = SmartDropsConfig.safeExistingFileDefaults();

        assertTrue(fresh.manualShearingDropsEnabled);
        assertFalse(fresh.automatedShearingDropsEnabled);
        assertTrue(fresh.inheritDefaultShearingMultiplier);
        assertEquals(2, fresh.defaultShearingMultiplier);
        assertTrue(fresh.shearingEntityMultipliers.isEmpty());

        assertFalse(existingFileFallback.manualShearingDropsEnabled);
        assertFalse(existingFileFallback.automatedShearingDropsEnabled);
        assertTrue(existingFileFallback.inheritDefaultShearingMultiplier);
        assertEquals(2, existingFileFallback.defaultShearingMultiplier);
        assertTrue(existingFileFallback.shearingEntityMultipliers.isEmpty());
    }

    @Test
    void normalizesOptionalTagMarkersFromJson() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        config.tagBlacklist = new LinkedHashSet<>();
        config.tagBlacklist.add(" #C:ORES ");
        config.tagBlacklist.add("##minecraft:mineable/pickaxe");
        config.tagBlacklist.add("minecraft:logs");
        config.tagWhitelist = new LinkedHashSet<>();
        config.tagWhitelist.add("#");

        config.sanitize();

        assertEquals(
                new LinkedHashSet<>(List.of("c:ores", "minecraft:mineable/pickaxe", "minecraft:logs")),
                config.tagBlacklist);
        assertTrue(config.tagWhitelist.isEmpty());
    }

    @Test
    void missingBlacklistReceivesSafetyDefaultsButExplicitEmptyListIsHonored() {
        ConfigManager.ParsedConfig missing = ConfigManager.parseStoredConfig("{}");
        ConfigManager.ParsedConfig explicitEmpty = ConfigManager.parseStoredConfig("{\"blacklist\":[]}");

        assertTrue(missing.config().blacklist.contains("minecraft:bedrock"));
        assertTrue(missing.config().blacklist.contains("minecraft:command_block"));
        assertTrue(explicitEmpty.config().blacklist.isEmpty());
    }

    @Test
    void futureSchemaIsDetectedWithoutDowngradingItsPayload() {
        ConfigManager.ParsedConfig parsed = ConfigManager.parseStoredConfig(
                "{\"schemaVersion\":" + (SmartDropsConfig.CURRENT_SCHEMA + 1) + ",\"futureField\":true}");

        assertTrue(parsed.isFutureSchema());
        assertEquals(SmartDropsConfig.CURRENT_SCHEMA + 1, parsed.schemaVersion());
        assertNull(parsed.config());
    }

    @Test
    void malformedRootIsRejected() {
        assertThrows(JsonParseException.class, () -> ConfigManager.parseStoredConfig("[]"));
        assertThrows(JsonParseException.class, () -> ConfigManager.parseStoredConfig("{\"schemaVersion\":\"1\"}"));
        assertThrows(JsonParseException.class, () -> ConfigManager.parseStoredConfig("{\"schemaVersion\":1.5}"));
    }

    @Test
    void semanticEqualitySeparatesNoOpAndRealUpdates() {
        SmartDropsConfig original = SmartDropsConfig.defaults();
        SmartDropsConfig unchanged = SmartDropsConfig.defaults();
        SmartDropsConfig changed = SmartDropsConfig.defaults();
        changed.enabled = false;

        assertTrue(ConfigManager.configurationsEqual(original, unchanged));
        assertFalse(ConfigManager.configurationsEqual(original, changed));
    }

    @Test
    void sanitizationValidatesKeysAndBoundsTotalRuleCollections() {
        SmartDropsConfig config = new SmartDropsConfig();
        config.blacklist.clear();
        config.blacklist.add("not an identifier");
        config.blacklist.add("minecraft:bedrock");
        config.categoryMultipliers.put("not_a_category", 2);
        for (int index = 0; index < SmartDropsConfig.MAX_TOTAL_RULE_ENTRIES + 100; index++) {
            config.blockMultipliers.put("example:block_" + index, 2);
        }

        config.sanitize();

        assertFalse(config.blacklist.contains("not an identifier"));
        assertTrue(config.blacklist.contains("minecraft:bedrock"));
        assertFalse(config.categoryMultipliers.containsKey("not_a_category"));
        assertTrue(config.ruleEntryCount() <= SmartDropsConfig.MAX_TOTAL_RULE_ENTRIES);
        assertTrue(config.blockRuleEntryCount() <= SmartDropsConfig.MAX_BLOCK_RULE_ENTRIES);
        assertTrue(config.entityRuleEntryCount() <= SmartDropsConfig.MAX_ENTITY_RULE_ENTRIES);
    }

    @Test
    void entityDefaultsAreDisabledBossSafeAndConservative() {
        SmartDropsConfig defaults = SmartDropsConfig.defaults();

        assertFalse(defaults.entityDropsEnabled);
        assertTrue(defaults.inheritDefaultEntityMultiplier);
        assertEquals(2, defaults.defaultEntityMultiplier);
        assertEquals(SmartDropsConfig.EntityKillRequirement.PLAYER_KILLS_ONLY,
                defaults.entityKillRequirement);
        assertFalse(defaults.bossDropsEnabled);
        assertFalse(defaults.multiplyMobExperience);
        assertFalse(defaults.multiplyBossExperience);
        assertEquals(1, defaults.entityCategoryMultipliers.get("golems"));
        assertEquals(1, defaults.entityCategoryMultipliers.get("villagers_npcs"));
        assertEquals(1, defaults.entityCategoryMultipliers.get("bosses"));
        assertEquals(1, defaults.entityCategoryMultipliers.get("miscellaneous"));
    }

    @Test
    void schemaOneMigrationPreservesBlockRulesAndForcesSafeNewDomainDefaults() throws IOException {
        ConfigManager.ParsedConfig parsed = ConfigManager.parseStoredConfig(fixture("schema-1.json"));

        assertTrue(parsed.migrationRequired());
        assertEquals(1, parsed.schemaVersion());
        assertEquals(SmartDropsConfig.CURRENT_SCHEMA, parsed.config().schemaVersion);
        assertFalse(parsed.config().enabled);
        assertEquals(7, parsed.config().globalMultiplier);
        assertEquals(32, parsed.config().maximumMultiplier);
        assertEquals(SmartDropsConfig.SourceMode.ALL, parsed.config().sourceMode);
        assertEquals(SmartDropsConfig.FilterMode.WHITELIST, parsed.config().filterMode);
        assertFalse(parsed.config().smartPlacementProtection);
        assertFalse(parsed.config().explosions);
        assertTrue(parsed.config().automatedMining);
        assertTrue(parsed.config().multiplyExperience);
        assertEquals(5, parsed.config().experienceMultiplier);
        assertEquals(9, parsed.config().blockMultipliers.get("minecraft:diamond_ore"));
        assertEquals(6, parsed.config().categoryMultipliers.get("ores"));
        assertEquals(3, parsed.config().dimensionMultipliers.get("minecraft:the_nether"));
        assertTrue(parsed.config().whitelist.contains("minecraft:diamond_ore"));
        assertTrue(parsed.config().tagWhitelist.contains("minecraft:mineable/pickaxe"));
        assertTrue(parsed.config().blockEntityAllowlist.contains("minecraft:chest"));
        assertEquals(4, parsed.config().playerMultipliers
                .get("00000000-0000-0000-0000-000000000001"));
        assertFalse(parsed.config().entityDropsEnabled);
        assertFalse(parsed.config().bossDropsEnabled);
        assertEquals(SmartDropsConfig.EntityKillRequirement.PLAYER_KILLS_ONLY,
                parsed.config().entityKillRequirement);
        assertTrue(parsed.config().entityMultipliers.isEmpty());
        assertEquals(SmartDropsConfig.defaults().entityCategoryMultipliers,
                parsed.config().entityCategoryMultipliers);
        assertFalse(parsed.config().manualShearingDropsEnabled);
        assertFalse(parsed.config().automatedShearingDropsEnabled);
        assertTrue(parsed.config().inheritDefaultShearingMultiplier);
        assertEquals(2, parsed.config().defaultShearingMultiplier);
        assertTrue(parsed.config().shearingEntityMultipliers.isEmpty());
    }

    @Test
    void schemaTwoMigrationPreservesEveryEntitySettingAndDisablesShearing() throws IOException {
        ConfigManager.ParsedConfig parsed = ConfigManager.parseStoredConfig(fixture("schema-2.json"));
        SmartDropsConfig migrated = parsed.config();

        assertTrue(parsed.migrationRequired());
        assertEquals(2, parsed.schemaVersion());
        assertEquals(3, migrated.schemaVersion);
        assertFalse(migrated.enabled);
        assertEquals(11, migrated.globalMultiplier);
        assertEquals(SmartDropsConfig.SourceMode.PLAYER_PLACED_ONLY, migrated.sourceMode);
        assertEquals(SmartDropsConfig.FilterMode.WHITELIST, migrated.filterMode);
        assertFalse(migrated.smartPlacementProtection);
        assertFalse(migrated.protectBlockEntities);
        assertFalse(migrated.playerMining);
        assertFalse(migrated.explosions);
        assertTrue(migrated.automatedMining);
        assertEquals(6, migrated.blockMultipliers.get("minecraft:ancient_debris"));
        assertEquals(5, migrated.categoryMultipliers.get("logs"));
        assertEquals(4, migrated.dimensionMultipliers.get("minecraft:the_end"));
        assertEquals(7, migrated.playerMultipliers
                .get("00000000-0000-0000-0000-000000000002"));

        assertTrue(migrated.entityDropsEnabled);
        assertFalse(migrated.inheritDefaultEntityMultiplier);
        assertEquals(13, migrated.defaultEntityMultiplier);
        assertEquals(SmartDropsConfig.EntityKillRequirement.ALL_STANDARD_DEATH_LOOT,
                migrated.entityKillRequirement);
        assertEquals(SmartDropsConfig.FilterMode.WHITELIST, migrated.entityFilterMode);
        assertTrue(migrated.bossDropsEnabled);
        assertTrue(migrated.multiplyMobExperience);
        assertEquals(14, migrated.mobExperienceMultiplier);
        assertTrue(migrated.multiplyBossExperience);
        assertEquals(Map.of(
                "passive", 3,
                "neutral", 4,
                "hostile", 5,
                "aquatic", 6,
                "ambient", 7,
                "golems", 8,
                "villagers_npcs", 9,
                "bosses", 10,
                "miscellaneous", 11), migrated.entityCategoryMultipliers);
        assertEquals(Map.of("minecraft:cow", 12, "minecraft:zombie", 13), migrated.entityMultipliers);
        assertEquals(new LinkedHashSet<>(List.of("minecraft:creeper")), migrated.entityBlacklist);
        assertEquals(new LinkedHashSet<>(List.of("minecraft:cow")), migrated.entityWhitelist);
        assertEquals(new LinkedHashSet<>(List.of("minecraft:raiders")), migrated.entityTagBlacklist);
        assertEquals(new LinkedHashSet<>(List.of("minecraft:undead")), migrated.entityTagWhitelist);

        assertFalse(migrated.manualShearingDropsEnabled);
        assertFalse(migrated.automatedShearingDropsEnabled);
        assertTrue(migrated.inheritDefaultShearingMultiplier);
        assertEquals(2, migrated.defaultShearingMultiplier);
        assertTrue(migrated.shearingEntityMultipliers.isEmpty());
    }

    @Test
    void schemaTwoMigrationIgnoresCoincidentallyNamedMalformedShearingFields() {
        ConfigManager.ParsedConfig parsed = ConfigManager.parseStoredConfig("""
                {
                  "schemaVersion": 2,
                  "blacklist": [],
                  "entityDropsEnabled": true,
                  "defaultEntityMultiplier": 7,
                  "entityMultipliers": {"minecraft:cow": 5},
                  "manualShearingDropsEnabled": {"not": "a boolean"},
                  "automatedShearingDropsEnabled": ["not", "a", "boolean"],
                  "inheritDefaultShearingMultiplier": 42,
                  "defaultShearingMultiplier": [64],
                  "shearingEntityMultipliers": "not a map"
                }
                """);

        assertTrue(parsed.config().entityDropsEnabled);
        assertEquals(7, parsed.config().defaultEntityMultiplier);
        assertEquals(5, parsed.config().entityMultipliers.get("minecraft:cow"));
        assertFalse(parsed.config().manualShearingDropsEnabled);
        assertFalse(parsed.config().automatedShearingDropsEnabled);
        assertTrue(parsed.config().inheritDefaultShearingMultiplier);
        assertTrue(parsed.config().shearingEntityMultipliers.isEmpty());
    }

    @Test
    void schemaThreeLoadsShearingSettingsNormally() {
        ConfigManager.ParsedConfig parsed = ConfigManager.parseStoredConfig("""
                {
                  "schemaVersion": 3,
                  "blacklist": [],
                  "manualShearingDropsEnabled": false,
                  "automatedShearingDropsEnabled": true,
                  "inheritDefaultShearingMultiplier": false,
                  "defaultShearingMultiplier": 5,
                  "shearingEntityMultipliers": {"minecraft:sheep": 8}
                }
                """);

        assertFalse(parsed.migrationRequired());
        assertFalse(parsed.config().manualShearingDropsEnabled);
        assertTrue(parsed.config().automatedShearingDropsEnabled);
        assertFalse(parsed.config().inheritDefaultShearingMultiplier);
        assertEquals(5, parsed.config().defaultShearingMultiplier);
        assertEquals(8, parsed.config().shearingEntityMultipliers.get("minecraft:sheep"));
    }

    @Test
    void schemaOneMigrationIgnoresMalformedCoincidentalEntityFields() {
        ConfigManager.ParsedConfig parsed = ConfigManager.parseStoredConfig("""
                {
                  "schemaVersion": 1,
                  "globalMultiplier": 7,
                  "blockMultipliers": {"minecraft:diamond_ore": 5},
                  "blacklist": [],
                  "entityDropsEnabled": {"not": "a boolean"},
                  "entityKillRequirement": 42,
                  "entityCategoryMultipliers": ["not", "a", "map"],
                  "entityMultipliers": "not a map",
                  "entityTagWhitelist": {"not": "a set"}
                }
                """);

        assertTrue(parsed.migrationRequired());
        assertEquals(7, parsed.config().globalMultiplier);
        assertEquals(5, parsed.config().blockMultipliers.get("minecraft:diamond_ore"));
        assertFalse(parsed.config().entityDropsEnabled);
        assertEquals(SmartDropsConfig.EntityKillRequirement.PLAYER_KILLS_ONLY,
                parsed.config().entityKillRequirement);
        assertTrue(parsed.config().entityMultipliers.isEmpty());
        assertTrue(parsed.config().entityTagWhitelist.isEmpty());
    }

    @Test
    void entitySanitizationUsesIndependentBudgetAndRejectsPlayersAndInvalidTags() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        config.entityMultipliers.put("minecraft:player", 64);
        config.entityMultipliers.put("minecraft:cow", 1000);
        config.entityTagBlacklist.add("#minecraft:undead");
        config.entityTagBlacklist.add("#");
        for (int index = 0; index < SmartDropsConfig.MAX_ENTITY_RULE_ENTRIES + 100; index++) {
            config.entityWhitelist.add("example:mob_" + index);
        }

        config.sanitize();

        assertFalse(config.entityMultipliers.containsKey("minecraft:player"));
        assertFalse(config.entityTagBlacklist.contains(""));
        assertTrue(config.entityRuleEntryCount() <= SmartDropsConfig.MAX_ENTITY_RULE_ENTRIES);
    }

    @Test
    void shearingSanitizationUsesAnIndependentBoundedDomain() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        config.blockMultipliers.put("minecraft:stone", 3);
        config.entityMultipliers.put("minecraft:cow", 4);
        config.shearingEntityMultipliers = new LinkedHashMap<>();
        config.shearingEntityMultipliers.put("minecraft:player", 64);
        config.shearingEntityMultipliers.put("not an identifier", 64);
        config.shearingEntityMultipliers.put("example:certified", 1000);
        for (int index = 0; index < SmartDropsConfig.MAX_SHEARING_RULE_ENTRIES + 100; index++) {
            config.shearingEntityMultipliers.put("example:shearable_" + index, 2);
        }

        config.sanitize();

        assertEquals(SmartDropsConfig.MAX_SHEARING_RULE_ENTRIES, config.shearingRuleEntryCount());
        assertFalse(config.shearingEntityMultipliers.containsKey("minecraft:player"));
        assertFalse(config.shearingEntityMultipliers.containsKey("not an identifier"));
        assertEquals(SmartDropsConfig.ABSOLUTE_MAX_MULTIPLIER,
                config.shearingEntityMultipliers.get("example:certified"));
        assertEquals(3, config.blockMultipliers.get("minecraft:stone"));
        assertEquals(4, config.entityMultipliers.get("minecraft:cow"));
    }

    @Test
    void storedConfigParsingRecordsBoundedNonSensitiveSanitizationDiagnostics() {
        String privatePlayerValue = "not-a-valid-private-player-id";
        ConfigManager.ParsedConfig parsed = ConfigManager.parseStoredConfig("""
                {
                  "schemaVersion": 2,
                  "maximumMultiplier": 999,
                  "globalMultiplier": -5,
                  "blacklist": ["not an identifier"],
                  "categoryMultipliers": {"not_a_category": 2},
                  "playerMultipliers": {"%s": 3},
                  "entityMultipliers": {"minecraft:player": 4},
                  "entityTagBlacklist": ["#"]
                }
                """.formatted(privatePlayerValue));

        ConfigLoadDiagnostics diagnostics = parsed.diagnostics();
        assertTrue(diagnostics.invalidIdentifiersRemoved() >= 3);
        assertTrue(diagnostics.invalidCategoryNamesRemoved() >= 1);
        assertEquals(1, diagnostics.invalidPlayerOverridesRemoved());
        assertTrue(diagnostics.valuesAdjusted() >= 2);
        assertTrue(diagnostics.samples().size() <= 16);
        assertFalse(diagnostics.samples().stream().anyMatch(sample ->
                sample.value().contains(privatePlayerValue)));
    }

    @Test
    void schemaOneFileIsBackedUpThenPersistedAsSafeSchemaThree(@TempDir Path directory)
            throws IOException {
        Path path = directory.resolve("smart_resource_drops.json");
        String legacy = """
                {
                  "schemaVersion": 1,
                  "globalMultiplier": 6,
                  "blockMultipliers": {"minecraft:diamond_ore": 4},
                  "blacklist": [],
                  "entityDropsEnabled": true
                }
                """;
        Files.writeString(path, legacy);

        assertTrue(ConfigManager.load(path));

        ConfigManager.ParsedConfig persisted = ConfigManager.parseStoredConfig(Files.readString(path));
        assertEquals(SmartDropsConfig.CURRENT_SCHEMA, persisted.schemaVersion());
        assertFalse(persisted.migrationRequired());
        assertEquals(6, persisted.config().globalMultiplier);
        assertEquals(4, persisted.config().blockMultipliers.get("minecraft:diamond_ore"));
        assertFalse(persisted.config().entityDropsEnabled);
        try (var files = Files.list(directory)) {
            Path backup = files
                    .filter(candidate -> candidate.getFileName().toString()
                            .startsWith("smart_resource_drops.schema-1-"))
                    .findFirst()
                    .orElseThrow();
            assertEquals(legacy, Files.readString(backup));
        }
    }

    @Test
    void confirmedMissingFileCreatesFreshShearingDefaults(@TempDir Path directory) throws IOException {
        Path path = directory.resolve("smart_resource_drops.json");

        assertTrue(ConfigManager.load(path));

        SmartDropsConfig active = ConfigManager.get();
        assertTrue(active.manualShearingDropsEnabled);
        assertFalse(active.automatedShearingDropsEnabled);
        assertTrue(active.inheritDefaultShearingMultiplier);
        assertTrue(active.shearingEntityMultipliers.isEmpty());
        SmartDropsConfig persisted = ConfigManager.parseStoredConfig(Files.readString(path)).config();
        assertTrue(persisted.manualShearingDropsEnabled);
        assertFalse(persisted.automatedShearingDropsEnabled);
    }

    @Test
    void schemaTwoFileIsBackedUpAndPersistsAllEntitySettings(@TempDir Path directory) throws IOException {
        Path path = directory.resolve("smart_resource_drops.json");
        String legacy = fixture("schema-2.json");
        Files.writeString(path, legacy);

        assertTrue(ConfigManager.load(path));

        SmartDropsConfig persisted = ConfigManager.parseStoredConfig(Files.readString(path)).config();
        assertEquals(3, persisted.schemaVersion);
        assertTrue(persisted.entityDropsEnabled);
        assertFalse(persisted.inheritDefaultEntityMultiplier);
        assertEquals(13, persisted.defaultEntityMultiplier);
        assertEquals(Map.of("minecraft:cow", 12, "minecraft:zombie", 13), persisted.entityMultipliers);
        assertEquals(14, persisted.mobExperienceMultiplier);
        assertFalse(persisted.manualShearingDropsEnabled);
        assertFalse(persisted.automatedShearingDropsEnabled);
        assertTrue(persisted.shearingEntityMultipliers.isEmpty());
        try (var files = Files.list(directory)) {
            Path backup = files
                    .filter(candidate -> candidate.getFileName().toString()
                            .startsWith("smart_resource_drops.schema-2-"))
                    .findFirst()
                    .orElseThrow();
            assertEquals(legacy, Files.readString(backup));
        }
    }

    @Test
    void migrationPersistenceFailureDoesNotPublishCandidateOrAdvanceRevision(@TempDir Path directory)
            throws IOException {
        Path activePath = directory.resolve("active.json");
        assertTrue(ConfigManager.load(activePath));
        assertTrue(ConfigManager.update(config -> config.globalMultiplier = 9, activePath));
        SmartDropsConfig activeBefore = ConfigManager.snapshot();
        long revisionBefore = ConfigManager.revision();

        Path migrationPath = directory.resolve("migration.json");
        String legacy = """
                {
                  "schemaVersion": 1,
                  "globalMultiplier": 3,
                  "blockMultipliers": {"minecraft:stone": 5},
                  "blacklist": []
                }
        """;
        Files.writeString(migrationPath, legacy);
        ConfigManager.setConfigWriterForTests((target, content) -> {
            throw new IOException("simulated persistence failure");
        });
        try {
            assertFalse(ConfigManager.load(migrationPath));
        } finally {
            ConfigManager.resetConfigWriterForTests();
        }

        assertTrue(ConfigManager.configurationsEqual(activeBefore, ConfigManager.get()));
        assertEquals(revisionBefore, ConfigManager.revision());
        assertEquals(legacy, Files.readString(migrationPath));

        // Clear the intentional write-suppression state for later tests in this JVM.
        assertTrue(ConfigManager.load(directory.resolve("restored-after-failure.json")));
    }

    @Test
    void futureMalformedAndOversizedFilesArePreservedBeforeReplacement(@TempDir Path directory) throws IOException {
        Path path = directory.resolve("smart_resource_drops.json");
        String future = "{\"schemaVersion\":" + (SmartDropsConfig.CURRENT_SCHEMA + 1)
                + ",\"futureField\":true}";
        Files.writeString(path, future);

        assertFalse(ConfigManager.load(path));

        assertEquals(future, Files.readString(path));
        assertTrue(ConfigManager.get().blacklist.contains("minecraft:bedrock"));
        assertFalse(ConfigManager.get().manualShearingDropsEnabled);
        assertFalse(ConfigManager.get().automatedShearingDropsEnabled);
        assertTrue(ConfigManager.validationSnapshot().writesSuppressed());
        assertTrue(ConfigManager.validationSnapshot().loadDiagnostics().unsupportedSchemaRejected());

        Files.writeString(path, "{");
        assertTrue(ConfigManager.load(path));

        assertTrue(ConfigManager.parseStoredConfig(Files.readString(path)).config().blacklist
                .contains("minecraft:bedrock"));
        SmartDropsConfig recovered = ConfigManager.parseStoredConfig(Files.readString(path)).config();
        assertFalse(recovered.manualShearingDropsEnabled);
        assertFalse(recovered.automatedShearingDropsEnabled);
        assertTrue(recovered.inheritDefaultShearingMultiplier);
        assertTrue(recovered.shearingEntityMultipliers.isEmpty());
        ConfigLoadDiagnostics malformedDiagnostics = ConfigManager.validationSnapshot().loadDiagnostics();
        assertTrue(malformedDiagnostics.malformedFileRecovered());
        assertEquals(1, malformedDiagnostics.backupFileNames().size());
        assertFalse(malformedDiagnostics.backupFileNames().getFirst().contains(directory.toString()));
        try (var files = Files.list(directory)) {
            Path backup = files
                    .filter(candidate -> candidate.getFileName().toString().startsWith("smart_resource_drops.broken-"))
                    .findFirst()
                    .orElseThrow();
            assertEquals("{", Files.readString(backup));
        }

        StringBuilder oversized = new StringBuilder("{\"blacklist\":[");
        for (int index = 0; index <= SmartDropsConfig.MAX_TOTAL_RULE_ENTRIES; index++) {
            if (index > 0) {
                oversized.append(',');
            }
            oversized.append("\"example:block_").append(index).append("\"");
        }
        oversized.append("]}");
        Files.writeString(path, oversized);

        assertTrue(ConfigManager.load(path));

        assertTrue(ConfigManager.parseStoredConfig(Files.readString(path)).config().ruleEntryCount()
                <= SmartDropsConfig.MAX_TOTAL_RULE_ENTRIES);
        assertTrue(ConfigManager.validationSnapshot().loadDiagnostics().blockEntriesOverBudget() > 0);
        try (var files = Files.list(directory)) {
            Path backup = files
                    .filter(candidate -> candidate.getFileName().toString()
                            .startsWith("smart_resource_drops.oversized-"))
                    .findFirst()
                    .orElseThrow();
            assertEquals(oversized.toString(), Files.readString(backup));
        }
    }

    @Test
    void oversizedShearingDomainIsBackedUpAndTruncatedIndependently(@TempDir Path directory)
            throws IOException {
        Path path = directory.resolve("smart_resource_drops.json");
        StringBuilder oversized = new StringBuilder("{\"schemaVersion\":3,\"blacklist\":[],"
                + "\"blockMultipliers\":{\"minecraft:stone\":3},"
                + "\"entityMultipliers\":{\"minecraft:cow\":4},"
                + "\"shearingEntityMultipliers\":{");
        for (int index = 0; index <= SmartDropsConfig.MAX_SHEARING_RULE_ENTRIES; index++) {
            if (index > 0) {
                oversized.append(',');
            }
            oversized.append("\"example:shearable_").append(index).append("\":2");
        }
        oversized.append("}}");
        Files.writeString(path, oversized);

        assertTrue(ConfigManager.load(path));

        assertEquals(SmartDropsConfig.MAX_SHEARING_RULE_ENTRIES,
                ConfigManager.get().shearingEntityMultipliers.size());
        assertEquals(3, ConfigManager.get().blockMultipliers.get("minecraft:stone"));
        assertEquals(4, ConfigManager.get().entityMultipliers.get("minecraft:cow"));
        assertEquals(1, ConfigManager.validationSnapshot().loadDiagnostics().shearingEntriesOverBudget());
        try (var files = Files.list(directory)) {
            Path backup = files
                    .filter(candidate -> candidate.getFileName().toString()
                            .startsWith("smart_resource_drops.oversized-"))
                    .findFirst()
                    .orElseThrow();
            assertEquals(oversized.toString(), Files.readString(backup));
        }
    }

    private static String fixture(final String name) throws IOException {
        final String resource = "/config/migration/" + name;
        try (InputStream input = SmartDropsConfigTest.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IOException("Missing test fixture " + resource);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
