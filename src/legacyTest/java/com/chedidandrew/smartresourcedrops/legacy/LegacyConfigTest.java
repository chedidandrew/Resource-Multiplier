package com.chedidandrew.smartresourcedrops.legacy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.io.FileWriter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class LegacyConfigTest {
    @Rule public final TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void multiplierPrecedenceIsStable() {
        LegacyConfig config = LegacyConfig.defaults();
        config.dimensionMultipliers.put("0", Integer.valueOf(3));
        config.categoryMultipliers.put("ores", Integer.valueOf(4));
        config.blockMultipliers.put("minecraft:diamond_ore", Integer.valueOf(5));
        assertEquals(5, config.blockMultiplier("minecraft:diamond_ore", "ores", 0, false, false, null));
        assertEquals(4, config.blockMultiplier("minecraft:iron_ore", "ores", 0, false, false, null));
        assertEquals(3, config.blockMultiplier("minecraft:dirt", "soil", 0, false, false, null));
    }

    @Test
    public void playerOverrideWinsAfterExactCategoryAndDimensionRules() {
        LegacyConfig config = LegacyConfig.defaults();
        config.globalMultiplier = 2;
        config.dimensionMultipliers.put("minecraft:overworld", Integer.valueOf(3));
        config.categoryMultipliers.put("ores", Integer.valueOf(4));
        config.blockMultipliers.put("minecraft:diamond_ore", Integer.valueOf(5));
        config.allowPlayerOverrides = true;
        config.maxPlayerMultiplier = 6;
        config.playerMultipliers.put("00000000-0000-0000-0000-000000000001", Integer.valueOf(6));

        assertEquals(5, config.blockMultiplier(
                "minecraft:diamond_ore", "ores", 0, false, false, null));
        assertEquals(6, config.blockMultiplier(
                "minecraft:diamond_ore", "ores", 0, false, false,
                "00000000-0000-0000-0000-000000000001"));
    }

    @Test
    public void naturalOnlyAndSafetyDefaultsAreConservative() {
        LegacyConfig config = LegacyConfig.defaults();
        assertEquals(1, config.blockMultiplier("minecraft:diamond_ore", "ores", 0, true, false, null));
        assertEquals(1, config.blockMultiplier("minecraft:bedrock", "stone", 0, false, false, null));
        assertEquals(1, config.blockMultiplier("minecraft:chest", "building_blocks", 0, false, true, null));
        assertEquals(false, config.isBlockEligible("minecraft:diamond_ore", true, false));
        assertEquals(false, config.isBlockEligible("minecraft:bedrock", false, false));
        assertEquals(false, config.isBlockEligible("minecraft:trial_spawner", false, false));
        assertEquals(false, config.isBlockEligible("minecraft:vault", false, false));
        assertEquals(false, config.isBlockEligible("minecraft:chest", false, true));
        assertEquals(true, config.isBlockEligible("minecraft:diamond_ore", false, false));
    }

    @Test
    public void jsonRoundTripClampsValues() throws Exception {
        LegacyConfig config = LegacyConfig.defaults();
        config.globalMultiplier = 999;
        config.defaultEntityMultiplier = -8;
        File file = new File(temporary.getRoot(), "smart_resource_drops.json");
        assertTrue(config.save(file));
        assertEquals(64, LegacyConfig.load(file).globalMultiplier);
        assertEquals(0, LegacyConfig.load(file).defaultEntityMultiplier);
    }

    @Test
    public void legacyRawResourceAliasMigratesToModernSchemaKey() {
        LegacyConfig config = LegacyConfig.defaults();
        config.categoryMultipliers.put("raw_resources", Integer.valueOf(5));
        config.sanitize();

        assertEquals(null, config.categoryMultipliers.get("raw_resources"));
        assertEquals(Integer.valueOf(5), config.categoryMultipliers.get("raw_resource_blocks"));
    }

    @Test
    public void entityDefaultsRemainConservative() {
        LegacyConfig config = LegacyConfig.defaults();
        config.entityDropsEnabled = true;
        assertEquals(1, config.entityMultiplier("minecraft:villager", "villagers_npcs", false));
        assertEquals(1, config.entityMultiplier("minecraft:wither", "bosses", true));
        assertEquals(2, config.entityMultiplier("minecraft:zombie", "hostile", false));
    }

    @Test
    public void emptyAndFutureNetworkConfigurationsAreRejected() {
        try {
            LegacyConfig.fromJson("");
            throw new AssertionError("Empty config should be rejected");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
        try {
            LegacyConfig.fromJson("{\"schemaVersion\":4}");
            throw new AssertionError("Future config schema should be rejected");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    @Test
    public void futureConfigFileIsPreservedAndFailsClosed() throws Exception {
        File file = new File(temporary.getRoot(), "smart_resource_drops.json");
        FileWriter writer = new FileWriter(file);
        writer.write("{\"schemaVersion\":4,\"manualShearingDropsEnabled\":true}");
        writer.close();

        LegacyConfig loaded = LegacyConfig.load(file);
        assertFalse(loaded.manualShearingDropsEnabled);
        assertTrue(new File(temporary.getRoot(), "smart_resource_drops.json.broken").isFile());
    }

    @Test
    public void olderSchemasCannotSilentlyEnableNewEntityOrShearingDomains() {
        LegacyConfig migrated = LegacyConfig.fromJson(
                "{\"schemaVersion\":1,\"entityDropsEnabled\":true,"
                        + "\"multiplyMobExperience\":true,\"manualShearingDropsEnabled\":true}");

        assertEquals(LegacyConfig.CURRENT_SCHEMA, migrated.schemaVersion);
        assertFalse(migrated.entityDropsEnabled);
        assertFalse(migrated.multiplyMobExperience);
        assertFalse(migrated.manualShearingDropsEnabled);
        assertFalse(migrated.automatedShearingDropsEnabled);
    }

    @Test
    public void explicitNullEntityCategoriesRestoreConservativeDefaults() {
        LegacyConfig loaded = LegacyConfig.fromJson(
                "{\"schemaVersion\":3,\"entityCategoryMultipliers\":null}");
        assertEquals(Integer.valueOf(1), loaded.entityCategoryMultipliers.get("golems"));
        assertEquals(Integer.valueOf(1), loaded.entityCategoryMultipliers.get("villagers_npcs"));
        assertEquals(Integer.valueOf(1), loaded.entityCategoryMultipliers.get("bosses"));
    }

    @Test
    public void presetsReplaceBlockOverridesWithTheirDocumentedValues() {
        LegacyConfig config = LegacyConfig.defaults();
        config.dimensionMultipliers.put("0", Integer.valueOf(8));
        config.blockMultipliers.put("minecraft:diamond_ore", Integer.valueOf(8));
        config.categoryMultipliers.put("soil", Integer.valueOf(8));

        config.applyPreset(LegacyConfig.Preset.VANILLA_PLUS);
        assertEquals(1, config.globalMultiplier);
        assertEquals(Integer.valueOf(2), config.categoryMultipliers.get("ores"));
        assertEquals(Integer.valueOf(2), config.categoryMultipliers.get("logs"));
        assertEquals(2, config.categoryMultipliers.size());
        assertTrue(config.dimensionMultipliers.isEmpty());
        assertTrue(config.blockMultipliers.isEmpty());

        config.applyPreset(LegacyConfig.Preset.FASTER_SURVIVAL);
        assertEquals(2, config.globalMultiplier);
        assertEquals(Integer.valueOf(3), config.categoryMultipliers.get("logs"));
        assertEquals(Integer.valueOf(2), config.categoryMultipliers.get("ores"));
        assertEquals(Integer.valueOf(2), config.categoryMultipliers.get("stone"));
        assertEquals(Integer.valueOf(2), config.categoryMultipliers.get("crops"));
        assertEquals(4, config.categoryMultipliers.size());

        config.applyPreset(LegacyConfig.Preset.FAST_PROGRESSION);
        assertEquals(4, config.globalMultiplier);
        assertTrue(config.categoryMultipliers.isEmpty());
    }

    @Test
    public void blockRuleCollectionsShareOneBoundedBudget() {
        LegacyConfig config = new LegacyConfig();
        config.blacklist.clear();
        for (int index = 0; index < 2048; index++) {
            config.blacklist.add("example:block_" + index);
        }
        config.categoryMultipliers.put("ores", Integer.valueOf(64));
        config.blockMultipliers.put("minecraft:diamond_ore", Integer.valueOf(64));

        config.sanitize();

        assertEquals(2048, config.blacklist.size());
        assertTrue(config.categoryMultipliers.isEmpty());
        assertTrue(config.blockMultipliers.isEmpty());
    }

    @Test
    public void ruleKeysUseModernIdentifierValidationAndOnlyTagSetsStripHashes() {
        LegacyConfig config = new LegacyConfig();
        config.blacklist.clear();
        config.blacklist.add("#minecraft:bedrock");
        config.blacklist.add("minecraft:bedrock");
        config.tagBlacklist.add("##minecraft:ores");
        config.dimensionMultipliers.put("-1", Integer.valueOf(3));
        config.categoryMultipliers.put("not_a_category", Integer.valueOf(4));
        config.playerMultipliers.put("not-a-uuid", Integer.valueOf(4));
        config.entityMultipliers.put("minecraft:player", Integer.valueOf(4));

        config.sanitize();

        assertEquals(1, config.blacklist.size());
        assertTrue(config.blacklist.contains("minecraft:bedrock"));
        assertTrue(config.tagBlacklist.contains("minecraft:ores"));
        assertEquals(Integer.valueOf(3), config.dimensionMultipliers.get("-1"));
        assertTrue(config.categoryMultipliers.isEmpty());
        assertTrue(config.playerMultipliers.isEmpty());
        assertTrue(config.entityMultipliers.isEmpty());
    }
}
