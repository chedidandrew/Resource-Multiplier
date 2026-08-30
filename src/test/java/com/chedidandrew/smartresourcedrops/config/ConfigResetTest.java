package com.chedidandrew.smartresourcedrops.config;

import com.chedidandrew.smartresourcedrops.core.SmartDropsStats;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ConfigResetTest {
    private static final String PLAYER_ID = "00000000-0000-0000-0000-000000000001";

    @Test
    void resetRestoresEveryPersistedSettingPreservesStatisticsAndSurvivesReload(
            @TempDir final Path directory
    ) throws IOException {
        final Path path = directory.resolve("smart_resource_drops.json");
        assertTrue(ConfigManager.load(path));
        assertTrue(ConfigManager.update(ConfigResetTest::installComplexConfiguration, path));
        assertFalse(ConfigManager.configurationsEqual(SmartDropsConfig.defaults(), ConfigManager.get()));

        SmartDropsStats.reset();
        try {
            SmartDropsStats.recordDrops(3, 4);
            SmartDropsStats.recordExperience(2, 9);
            final SmartDropsStats.Snapshot statisticsBeforeReset = SmartDropsStats.snapshot();
            final long revisionBeforeReset = ConfigManager.revision();

            assertTrue(ConfigManager.reset(path));
            assertEquals(revisionBeforeReset + 1, ConfigManager.revision());
            assertDefaults(ConfigManager.get());
            assertTrue(ConfigManager.get().manualShearingDropsEnabled);
            assertFalse(ConfigManager.get().automatedShearingDropsEnabled);
            assertTrue(ConfigManager.get().inheritDefaultShearingMultiplier);
            assertTrue(ConfigManager.get().shearingEntityMultipliers.isEmpty());
            assertDefaults(ConfigManager.parseStoredConfig(Files.readString(path)).config());
            assertEquals(statisticsBeforeReset, SmartDropsStats.snapshot(),
                    "A configuration reset erased runtime statistics/history");

            // Change active memory through a different persisted target, then reload the reset file.
            // This proves the defaults came back from disk rather than merely remaining in memory.
            final Path alternate = directory.resolve("alternate.json");
            assertTrue(ConfigManager.update(config -> config.globalMultiplier = 11, alternate));
            assertEquals(11, ConfigManager.get().globalMultiplier);
            assertTrue(ConfigManager.load(path));
            assertDefaults(ConfigManager.get());
        } finally {
            SmartDropsStats.reset();
        }
    }

    @Test
    void failedResetLeavesActiveConfigurationRevisionAndPersistedFileUnchanged(
            @TempDir final Path directory
    ) throws IOException {
        final Path path = directory.resolve("smart_resource_drops.json");
        assertTrue(ConfigManager.load(path));
        assertTrue(ConfigManager.update(ConfigResetTest::installComplexConfiguration, path));

        final SmartDropsConfig activeBeforeReset = ConfigManager.snapshot();
        final String persistedBeforeReset = Files.readString(path);
        final long revisionBeforeReset = ConfigManager.revision();
        final Path invalidTarget = directory.resolve("existing-directory");
        Files.createDirectory(invalidTarget);

        assertFalse(ConfigManager.reset(invalidTarget));
        assertTrue(ConfigManager.configurationsEqual(activeBeforeReset, ConfigManager.get()));
        assertEquals(persistedBeforeReset, Files.readString(path));
        assertEquals(revisionBeforeReset, ConfigManager.revision());
    }

    @Test
    void acceptedResetAdvancesOneRevisionAndRejectsStaleMutations(@TempDir final Path directory) {
        final Path path = directory.resolve("smart_resource_drops.json");
        assertTrue(ConfigManager.load(path));
        assertTrue(ConfigManager.update(ConfigResetTest::installComplexConfiguration, path));

        final long revisionBeforeReset = ConfigManager.revision();
        final ConfigPatch stalePatch = new ConfigPatch();
        stalePatch.manualShearingDropsEnabled = true;
        stalePatch.shearingEntityMultipliers.put("minecraft:sheep", 13);
        final String staleJson = ConfigManager.encodeClientPatch(stalePatch);

        assertTrue(ConfigManager.reset(path));
        assertEquals(revisionBeforeReset + 1, ConfigManager.revision());
        assertDefaults(ConfigManager.get());

        assertFalse(ConfigManager.reset(revisionBeforeReset));
        assertFalse(ConfigManager.applyClientPatch(staleJson, revisionBeforeReset));
        assertEquals(revisionBeforeReset + 1, ConfigManager.revision());
        assertDefaults(ConfigManager.get());
    }

    private static void installComplexConfiguration(final SmartDropsConfig config) {
        config.enabled = false;
        config.globalMultiplier = 7;
        config.maximumMultiplier = 32;
        config.sourceMode = SmartDropsConfig.SourceMode.ALL;
        config.filterMode = SmartDropsConfig.FilterMode.WHITELIST;
        config.smartPlacementProtection = false;
        config.protectBlockEntities = false;
        config.playerMining = false;
        config.explosions = false;
        config.automatedMining = true;
        config.multiplyExperience = true;
        config.experienceMultiplier = 6;
        config.conservativePistonProtection = false;
        config.allowPlayerOverrides = true;
        config.maxPlayerMultiplier = 8;
        config.statisticsEnabled = true;
        config.entityDropsEnabled = true;
        config.inheritDefaultEntityMultiplier = false;
        config.defaultEntityMultiplier = 6;
        config.entityKillRequirement = SmartDropsConfig.EntityKillRequirement.ALL_STANDARD_DEATH_LOOT;
        config.entityFilterMode = SmartDropsConfig.FilterMode.WHITELIST;
        config.bossDropsEnabled = true;
        config.multiplyMobExperience = true;
        config.mobExperienceMultiplier = 5;
        config.multiplyBossExperience = true;
        config.manualShearingDropsEnabled = false;
        config.automatedShearingDropsEnabled = true;
        config.inheritDefaultShearingMultiplier = false;
        config.defaultShearingMultiplier = 6;

        config.dimensionMultipliers.clear();
        config.dimensionMultipliers.put("minecraft:the_nether", 3);
        config.categoryMultipliers.clear();
        config.categoryMultipliers.put("ores", 5);
        config.blockMultipliers.clear();
        config.blockMultipliers.put("minecraft:diamond_ore", 8);

        config.blacklist.clear();
        config.blacklist.add("minecraft:stone");
        config.whitelist.clear();
        config.whitelist.add("minecraft:diamond_ore");
        config.tagBlacklist.clear();
        config.tagBlacklist.add("minecraft:logs");
        config.tagWhitelist.clear();
        config.tagWhitelist.add("minecraft:mineable/pickaxe");
        config.blockEntityAllowlist.clear();
        config.blockEntityAllowlist.add("minecraft:chest");
        config.playerMultipliers.clear();
        config.playerMultipliers.put(PLAYER_ID, 7);
        config.entityCategoryMultipliers.clear();
        config.entityCategoryMultipliers.put("hostile", 4);
        config.entityMultipliers.clear();
        config.entityMultipliers.put("minecraft:zombie", 8);
        config.entityBlacklist.clear();
        config.entityBlacklist.add("minecraft:cow");
        config.entityWhitelist.clear();
        config.entityWhitelist.add("minecraft:zombie");
        config.entityTagBlacklist.clear();
        config.entityTagBlacklist.add("minecraft:raiders");
        config.entityTagWhitelist.clear();
        config.entityTagWhitelist.add("minecraft:undead");
        config.shearingEntityMultipliers.clear();
        config.shearingEntityMultipliers.put("minecraft:sheep", 8);
    }

    private static void assertDefaults(final SmartDropsConfig actual) {
        assertTrue(ConfigManager.configurationsEqual(SmartDropsConfig.defaults(), actual),
                "Configuration did not match the authoritative SmartDropsConfig.defaults() factory");
    }
}
