package com.chedidandrew.smartresourcedrops.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ConfigPatchTest {
    @Test
    void appliesPresetThenDirtyDeltaAndPreservesUnrelatedServerState(@TempDir Path directory) throws IOException {
        Path path = directory.resolve("smart_resource_drops.json");
        Files.writeString(path, """
                {
                  "statisticsEnabled": true,
                  "automatedMining": true,
                  "blockMultipliers": {"minecraft:stone": 7},
                  "playerMultipliers": {"00000000-0000-0000-0000-000000000001": 4},
                  "blacklist": []
                }
                """);
        ConfigManager.load(path);

        ConfigPatch patch = new ConfigPatch();
        patch.preset = SmartDropsConfig.Preset.VANILLA_PLUS;
        patch.globalMultiplier = 3;
        patch.blockMultipliers.put("minecraft:diamond_ore", 5);
        patch.inheritedBlocks.add("minecraft:stone");
        patch.blockFilters.put("minecraft:diamond_ore", ConfigPatch.FilterEntryState.WHITELIST);

        String json = ConfigManager.encodeClientPatch(patch);
        assertTrue(ConfigManager.applyClientPatch(json, path));

        SmartDropsConfig result = ConfigManager.get();
        assertEquals(3, result.globalMultiplier);
        assertEquals(2, result.categoryMultipliers.get("ores"));
        assertEquals(2, result.categoryMultipliers.get("logs"));
        assertEquals(5, result.blockMultipliers.get("minecraft:diamond_ore"));
        assertFalse(result.blockMultipliers.containsKey("minecraft:stone"));
        assertTrue(result.whitelist.contains("minecraft:diamond_ore"));
        assertFalse(result.blacklist.contains("minecraft:diamond_ore"));
        assertTrue(result.statisticsEnabled);
        assertTrue(result.automatedMining);
        assertEquals(4, result.playerMultipliers.get("00000000-0000-0000-0000-000000000001"));

        String persisted = Files.readString(path);
        assertTrue(ConfigManager.applyClientPatch(json, path));
        assertEquals(persisted, Files.readString(path), "idempotent retry rewrote or changed the config");
    }

    @Test
    void rejectsInvalidKeysAndWriteFailureWithoutMutatingActiveConfig(@TempDir Path directory) throws IOException {
        Path path = directory.resolve("smart_resource_drops.json");
        ConfigManager.load(path);
        int before = ConfigManager.get().globalMultiplier;

        ConfigPatch invalid = new ConfigPatch();
        invalid.globalMultiplier = 8;
        invalid.blockMultipliers.put("not an identifier", 4);
        assertFalse(ConfigManager.applyClientPatch(ConfigManager.encodeClientPatch(invalid), path));
        assertEquals(before, ConfigManager.get().globalMultiplier);

        ConfigPatch valid = new ConfigPatch();
        valid.globalMultiplier = 8;
        Path unwritableTarget = directory.resolve("existing-directory");
        Files.createDirectory(unwritableTarget);
        assertFalse(ConfigManager.applyClientPatch(ConfigManager.encodeClientPatch(valid), unwritableTarget));
        assertEquals(before, ConfigManager.get().globalMultiplier);
    }

    @Test
    void commandUpdatePublishesOnlyAfterPersistenceSucceeds(@TempDir Path directory) throws IOException {
        Path path = directory.resolve("smart_resource_drops.json");
        ConfigManager.load(path);

        assertTrue(ConfigManager.update(config -> config.globalMultiplier = 6, path));
        assertEquals(6, ConfigManager.get().globalMultiplier);
        assertEquals(6, ConfigManager.parseStoredConfig(Files.readString(path)).config().globalMultiplier);

        Path unwritableTarget = directory.resolve("existing-directory");
        Files.createDirectory(unwritableTarget);
        assertFalse(ConfigManager.update(config -> config.globalMultiplier = 9, unwritableTarget));
        assertEquals(6, ConfigManager.get().globalMultiplier);
    }

    @Test
    void commandUpdateHonorsSuppressedWrites(@TempDir Path directory) throws IOException {
        Path path = directory.resolve("smart_resource_drops.json");
        String future = "{\"schemaVersion\":" + (SmartDropsConfig.CURRENT_SCHEMA + 1) + "}";
        Files.writeString(path, future);
        ConfigManager.load(path);

        int before = ConfigManager.get().globalMultiplier;
        assertFalse(ConfigManager.update(config -> config.globalMultiplier = before + 1, path));
        assertEquals(before, ConfigManager.get().globalMultiplier);
        ConfigPatch shearingPatch = new ConfigPatch();
        shearingPatch.manualShearingDropsEnabled = true;
        assertFalse(ConfigManager.applyClientPatch(ConfigManager.encodeClientPatch(shearingPatch), path));
        assertFalse(ConfigManager.get().manualShearingDropsEnabled);
        assertEquals(future, Files.readString(path));

        ConfigManager.load(directory.resolve("restored-config.json"));
    }

    @Test
    void commandUpdateRejectsRuleOverflowWithoutDisplacingExistingRules(@TempDir Path directory) throws IOException {
        Path path = directory.resolve("smart_resource_drops.json");
        StringBuilder json = new StringBuilder("{\"blacklist\":[],\"blockMultipliers\":{");
        for (int index = 0; index < SmartDropsConfig.MAX_BLOCK_RULE_ENTRIES; index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append("\"example:block_").append(index).append("\":2");
        }
        json.append("}}");
        Files.writeString(path, json);
        assertTrue(ConfigManager.load(path));

        String finalBlock = "example:block_" + (SmartDropsConfig.MAX_BLOCK_RULE_ENTRIES - 1);
        assertTrue(ConfigManager.get().blockMultipliers.containsKey(finalBlock));
        assertFalse(ConfigManager.update(config -> config.blacklist.add("minecraft:stone"), path));
        assertFalse(ConfigManager.get().blacklist.contains("minecraft:stone"));
        assertEquals(SmartDropsConfig.MAX_BLOCK_RULE_ENTRIES, ConfigManager.get().blockMultipliers.size());
        assertTrue(ConfigManager.get().blockMultipliers.containsKey(finalBlock));
        assertTrue(ConfigManager.parseStoredConfig(Files.readString(path)).config()
                .blockMultipliers.containsKey(finalBlock));
    }

    @Test
    void blockEntityAndShearingRulesUseIndependentBudgets(@TempDir Path directory) throws IOException {
        Path path = directory.resolve("smart_resource_drops.json");
        StringBuilder json = new StringBuilder("{\"schemaVersion\":2,\"blacklist\":[],"
                + "\"entityCategoryMultipliers\":{},\"blockMultipliers\":{");
        for (int index = 0; index < SmartDropsConfig.MAX_BLOCK_RULE_ENTRIES; index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append("\"example:block_").append(index).append("\":2");
        }
        json.append("}}");
        Files.writeString(path, json);
        assertTrue(ConfigManager.load(path));

        assertTrue(ConfigManager.update(
                config -> {
                    config.entityMultipliers.put("minecraft:cow", 3);
                    config.shearingEntityMultipliers.put("minecraft:sheep", 4);
                },
                path));
        assertEquals(SmartDropsConfig.MAX_BLOCK_RULE_ENTRIES,
                ConfigManager.get().blockMultipliers.size());
        assertEquals(3, ConfigManager.get().entityMultipliers.get("minecraft:cow"));
        assertEquals(4, ConfigManager.get().shearingEntityMultipliers.get("minecraft:sheep"));
    }

    @Test
    void enforcesBoundedCollectionEdits() {
        ConfigPatch patch = new ConfigPatch();
        for (int index = 0; index <= ConfigPatch.MAX_COLLECTION_EDITS; index++) {
            patch.blockMultipliers.put("example:block_" + index, 2);
        }

        assertFalse(patch.hasValidShape());
        assertThrows(IllegalArgumentException.class, () -> ConfigManager.encodeClientPatch(patch));
    }

    @Test
    void rejectsOutOfRangeNetworkValuesInsteadOfClamping(@TempDir Path directory) throws IOException {
        Path path = directory.resolve("smart_resource_drops.json");
        ConfigManager.load(path);
        int before = ConfigManager.get().globalMultiplier;

        ConfigPatch invalidGlobal = new ConfigPatch();
        invalidGlobal.globalMultiplier = SmartDropsConfig.ABSOLUTE_MAX_MULTIPLIER + 1;
        assertFalse(ConfigManager.applyClientPatch(ConfigManager.encodeClientPatch(invalidGlobal), path));
        assertEquals(before, ConfigManager.get().globalMultiplier);

        ConfigPatch invalidExperience = new ConfigPatch();
        invalidExperience.experienceMultiplier = 0;
        assertFalse(ConfigManager.applyClientPatch(ConfigManager.encodeClientPatch(invalidExperience), path));

        ConfigPatch invalidRule = new ConfigPatch();
        invalidRule.blockMultipliers.put("minecraft:stone", -1);
        assertFalse(ConfigManager.applyClientPatch(ConfigManager.encodeClientPatch(invalidRule), path));

        ConfigPatch invalidShearingDefault = new ConfigPatch();
        invalidShearingDefault.defaultShearingMultiplier = -1;
        assertFalse(ConfigManager.applyClientPatch(
                ConfigManager.encodeClientPatch(invalidShearingDefault), path));

        ConfigPatch invalidShearingRule = new ConfigPatch();
        invalidShearingRule.shearingEntityMultipliers.put(
                "minecraft:sheep",
                SmartDropsConfig.ABSOLUTE_MAX_MULTIPLIER + 1);
        assertFalse(ConfigManager.applyClientPatch(
                ConfigManager.encodeClientPatch(invalidShearingRule), path));
    }

    @Test
    void appliesEntityDeltaAtomicallyAndPreservesBlockAndPlayerRules(@TempDir Path directory)
            throws IOException {
        Path path = directory.resolve("smart_resource_drops.json");
        Files.writeString(path, """
                {
                  "schemaVersion": 2,
                  "blockMultipliers": {"minecraft:stone": 3},
                  "playerMultipliers": {"00000000-0000-0000-0000-000000000001": 4},
                  "blacklist": []
                }
                """);
        assertTrue(ConfigManager.load(path));

        ConfigPatch patch = new ConfigPatch();
        patch.entityDropsEnabled = true;
        patch.inheritDefaultEntityMultiplier = false;
        patch.defaultEntityMultiplier = 3;
        patch.entityKillRequirement = SmartDropsConfig.EntityKillRequirement.PLAYER_OR_TAMED_ENTITY;
        patch.entityCategoryMultipliers.put("hostile", 4);
        patch.entityMultipliers.put("minecraft:cow", 5);
        patch.entityFilters.put("minecraft:zombie", ConfigPatch.FilterEntryState.BLACKLIST);
        patch.entityTagFilters.put("#minecraft:undead", ConfigPatch.FilterEntryState.WHITELIST);

        assertTrue(ConfigManager.applyClientPatch(ConfigManager.encodeClientPatch(patch), path));

        SmartDropsConfig result = ConfigManager.get();
        assertTrue(result.entityDropsEnabled);
        assertFalse(result.inheritDefaultEntityMultiplier);
        assertEquals(3, result.defaultEntityMultiplier);
        assertEquals(SmartDropsConfig.EntityKillRequirement.PLAYER_OR_TAMED_ENTITY,
                result.entityKillRequirement);
        assertEquals(4, result.entityCategoryMultipliers.get("hostile"));
        assertEquals(5, result.entityMultipliers.get("minecraft:cow"));
        assertTrue(result.entityBlacklist.contains("minecraft:zombie"));
        assertTrue(result.entityTagWhitelist.contains("minecraft:undead"));
        assertEquals(3, result.blockMultipliers.get("minecraft:stone"));
        assertEquals(4, result.playerMultipliers.get("00000000-0000-0000-0000-000000000001"));
    }

    @Test
    void rejectsInvalidEntityKeysPlayerTargetsAndMalformedScalarTypes(@TempDir Path directory) {
        Path path = directory.resolve("smart_resource_drops.json");
        assertTrue(ConfigManager.load(path));

        ConfigPatch player = new ConfigPatch();
        player.entityMultipliers.put("minecraft:player", 2);
        assertFalse(ConfigManager.applyClientPatch(ConfigManager.encodeClientPatch(player), path));

        ConfigPatch invalidCategory = new ConfigPatch();
        invalidCategory.entityCategoryMultipliers.put("not_a_category", 2);
        assertFalse(ConfigManager.applyClientPatch(ConfigManager.encodeClientPatch(invalidCategory), path));

        assertFalse(ConfigManager.applyClientPatch("{\"entityDropsEnabled\":1}", path));
        assertFalse(ConfigManager.applyClientPatch("{\"defaultEntityMultiplier\":2.5}", path));
        assertFalse(ConfigManager.applyClientPatch(
                "{\"entityKillRequirement\":\"NOT_A_MODE\"}", path));

        ConfigPatch shearingPlayer = new ConfigPatch();
        shearingPlayer.shearingEntityMultipliers.put("minecraft:player", 2);
        assertFalse(ConfigManager.applyClientPatch(ConfigManager.encodeClientPatch(shearingPlayer), path));

        assertFalse(ConfigManager.applyClientPatch("{\"manualShearingDropsEnabled\":1}", path));
        assertFalse(ConfigManager.applyClientPatch("{\"defaultShearingMultiplier\":2.5}", path));
    }

    @Test
    void appliesShearingDeltaAtomicallyAndIdempotently(@TempDir Path directory) throws IOException {
        Path path = directory.resolve("smart_resource_drops.json");
        Files.writeString(path, """
                {
                  "schemaVersion": 3,
                  "blacklist": [],
                  "blockMultipliers": {"minecraft:stone": 3},
                  "entityMultipliers": {"minecraft:cow": 4},
                  "playerMultipliers": {"00000000-0000-0000-0000-000000000001": 4},
                  "shearingEntityMultipliers": {"example:old_shearable": 2}
                }
                """);
        assertTrue(ConfigManager.load(path));

        ConfigPatch patch = new ConfigPatch();
        patch.manualShearingDropsEnabled = false;
        patch.automatedShearingDropsEnabled = true;
        patch.inheritDefaultShearingMultiplier = false;
        patch.defaultShearingMultiplier = 5;
        patch.inheritedShearingEntities.add("example:old_shearable");
        patch.shearingEntityMultipliers.put("minecraft:sheep", 6);
        String json = ConfigManager.encodeClientPatch(patch);

        assertTrue(ConfigManager.applyClientPatch(json, path));

        SmartDropsConfig result = ConfigManager.get();
        assertFalse(result.manualShearingDropsEnabled);
        assertTrue(result.automatedShearingDropsEnabled);
        assertFalse(result.inheritDefaultShearingMultiplier);
        assertEquals(5, result.defaultShearingMultiplier);
        assertFalse(result.shearingEntityMultipliers.containsKey("example:old_shearable"));
        assertEquals(6, result.shearingEntityMultipliers.get("minecraft:sheep"));
        assertEquals(3, result.blockMultipliers.get("minecraft:stone"));
        assertEquals(4, result.entityMultipliers.get("minecraft:cow"));
        assertEquals(4, result.playerMultipliers.get("00000000-0000-0000-0000-000000000001"));

        String persisted = Files.readString(path);
        assertTrue(ConfigManager.applyClientPatch(json, path));
        assertEquals(persisted, Files.readString(path));
    }

    @Test
    void rejectsShearingDomainOverflowWithoutApplyingAnyScalarOrRule(@TempDir Path directory)
            throws IOException {
        Path path = directory.resolve("smart_resource_drops.json");
        assertTrue(ConfigManager.load(path));
        assertTrue(ConfigManager.update(config -> {
            for (int index = 0; index < SmartDropsConfig.MAX_SHEARING_RULE_ENTRIES; index++) {
                config.shearingEntityMultipliers.put("example:shearable_" + index, 2);
            }
        }, path));
        SmartDropsConfig before = ConfigManager.snapshot();
        String persistedBefore = Files.readString(path);
        long revisionBefore = ConfigManager.revision();

        ConfigPatch overflow = new ConfigPatch();
        overflow.manualShearingDropsEnabled = false;
        overflow.shearingEntityMultipliers.put("example:one_too_many", 3);

        assertFalse(ConfigManager.applyClientPatch(ConfigManager.encodeClientPatch(overflow), path));
        assertTrue(ConfigManager.configurationsEqual(before, ConfigManager.get()));
        assertEquals(persistedBefore, Files.readString(path));
        assertEquals(revisionBefore, ConfigManager.revision());
    }

    @Test
    void boundsShearingPatchCollectionsWithoutPreventingFullReplacement() {
        ConfigPatch tooManyValues = new ConfigPatch();
        for (int index = 0; index <= SmartDropsConfig.MAX_SHEARING_RULE_ENTRIES; index++) {
            tooManyValues.shearingEntityMultipliers.put("example:shearable_" + index, 2);
        }
        assertFalse(tooManyValues.hasValidShape());
        assertThrows(IllegalArgumentException.class, () -> ConfigManager.encodeClientPatch(tooManyValues));

        ConfigPatch replacement = new ConfigPatch();
        for (int index = 0; index < SmartDropsConfig.MAX_SHEARING_RULE_ENTRIES; index++) {
            replacement.inheritedShearingEntities.add("example:old_" + index);
            replacement.shearingEntityMultipliers.put("example:new_" + index, 2);
        }
        assertTrue(replacement.hasValidShape());
    }

    @Test
    void clientSnapshotContainsDetachedShearingStateAndStillRedactsPlayers(@TempDir Path directory) {
        Path path = directory.resolve("smart_resource_drops.json");
        assertTrue(ConfigManager.load(path));
        assertTrue(ConfigManager.update(config -> {
            config.manualShearingDropsEnabled = false;
            config.automatedShearingDropsEnabled = true;
            config.shearingEntityMultipliers.put("minecraft:sheep", 7);
            config.playerMultipliers.put("00000000-0000-0000-0000-000000000001", 4);
        }, path));

        SmartDropsConfig snapshot = ConfigManager.snapshotForClient();

        assertFalse(snapshot.manualShearingDropsEnabled);
        assertTrue(snapshot.automatedShearingDropsEnabled);
        assertEquals(7, snapshot.shearingEntityMultipliers.get("minecraft:sheep"));
        assertTrue(snapshot.playerMultipliers.isEmpty());
        snapshot.shearingEntityMultipliers.clear();
        assertEquals(7, ConfigManager.get().shearingEntityMultipliers.get("minecraft:sheep"));
    }

    @Test
    void localGuiPatchUsesAtomicConfigPathAndPreservesServerOnlyState(@TempDir Path directory) throws IOException {
        Path path = directory.resolve("smart_resource_drops.json");
        Files.writeString(path, """
                {
                  "globalMultiplier": 2,
                  "playerMultipliers": {"00000000-0000-0000-0000-000000000001": 4},
                  "blacklist": []
                }
                """);
        ConfigManager.load(path);

        ConfigPatch patch = new ConfigPatch();
        patch.globalMultiplier = 5;
        assertTrue(ConfigManager.applyLocalPatch(patch, path));

        assertEquals(5, ConfigManager.get().globalMultiplier);
        assertEquals(4, ConfigManager.get().playerMultipliers
                .get("00000000-0000-0000-0000-000000000001"));
        assertEquals(5, ConfigManager.parseStoredConfig(Files.readString(path)).config().globalMultiplier);
    }

    @Test
    void localGuiPatchRejectsAStaleRevision(@TempDir Path directory) {
        Path path = directory.resolve("smart_resource_drops.json");
        assertTrue(ConfigManager.load(path));
        long stagedRevision = ConfigManager.revision();
        assertTrue(ConfigManager.update(config -> config.globalMultiplier = 6, path));

        ConfigPatch patch = new ConfigPatch();
        patch.globalMultiplier = 9;
        assertFalse(ConfigManager.applyLocalPatch(patch, stagedRevision, path));
        assertEquals(6, ConfigManager.get().globalMultiplier);
    }

    @Test
    void saveSanitizesACopyAndPublishesOnlyAfterPersistence(@TempDir Path directory) throws IOException {
        Path path = directory.resolve("smart_resource_drops.json");
        assertTrue(ConfigManager.load(path));
        ConfigManager.get().globalMultiplier = Integer.MAX_VALUE;
        long revisionBeforeFailure = ConfigManager.revision();

        Path invalidTarget = directory.resolve("existing-directory");
        Files.createDirectory(invalidTarget);
        assertFalse(ConfigManager.save(invalidTarget));
        assertEquals(Integer.MAX_VALUE, ConfigManager.get().globalMultiplier,
                "failed save sanitized the live configuration");
        assertEquals(revisionBeforeFailure, ConfigManager.revision());

        assertTrue(ConfigManager.save(path));
        assertEquals(SmartDropsConfig.ABSOLUTE_MAX_MULTIPLIER,
                ConfigManager.get().globalMultiplier);
        assertEquals(revisionBeforeFailure + 1, ConfigManager.revision());
        assertEquals(SmartDropsConfig.ABSOLUTE_MAX_MULTIPLIER,
                ConfigManager.parseStoredConfig(Files.readString(path)).config().globalMultiplier);
    }

    @Test
    void successfulPublicationsReportUpdateAndResetKinds(@TempDir Path directory) {
        Path path = directory.resolve("smart_resource_drops.json");
        assertTrue(ConfigManager.load(path));
        java.util.List<ConfigManager.PublicationKind> kinds = new java.util.ArrayList<>();
        ConfigManager.setPublicationListener((revision, kind) -> kinds.add(kind));
        try {
            assertTrue(ConfigManager.update(config -> config.globalMultiplier = 7, path));
            assertTrue(ConfigManager.reset(path));
            assertEquals(java.util.List.of(
                    ConfigManager.PublicationKind.UPDATE,
                    ConfigManager.PublicationKind.RESET), kinds);
        } finally {
            ConfigManager.setPublicationListener(null);
        }
    }

    @Test
    void authoritativeSnapshotDecodeReportsFailureInsteadOfEditableDefaults() {
        assertTrue(ConfigManager.tryParseSnapshotJson("{not-json").isEmpty());
        assertTrue(ConfigManager.tryParseSnapshotJson("").isEmpty());
        assertTrue(ConfigManager.tryParseSnapshotJson(
                "{\"schemaVersion\":" + (SmartDropsConfig.CURRENT_SCHEMA + 1) + "}").isEmpty());
        assertTrue(ConfigManager.tryParseSnapshotJson("{\"globalMultiplier\":3}").isPresent());
        assertFalse(ConfigManager.parseSnapshotJson("{not-json").manualShearingDropsEnabled);
        assertFalse(ConfigManager.parseSnapshotJson(
                "{\"schemaVersion\":" + (SmartDropsConfig.CURRENT_SCHEMA + 1) + "}")
                .manualShearingDropsEnabled);
    }
}
