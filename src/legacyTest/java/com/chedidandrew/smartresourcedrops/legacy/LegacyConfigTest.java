package com.chedidandrew.smartresourcedrops.legacy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class LegacyConfigTest {
    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void exactCategoryDimensionGlobalPrecedenceIsStable() {
        LegacyConfig config = LegacyConfig.defaults();
        config.globalMultiplier = 2;
        config.dimensionMultipliers.put("0", Integer.valueOf(3));
        config.categoryMultipliers.put("ores", Integer.valueOf(4));
        config.blockMultipliers.put("minecraft:diamond_ore", Integer.valueOf(5));
        assertEquals(5, config.blockMultiplier("minecraft:diamond_ore", "ores", 0, false, false, null));
        assertEquals(4, config.blockMultiplier("minecraft:iron_ore", "ores", 0, false, false, null));
        assertEquals(3, config.blockMultiplier("minecraft:dirt", "soil", 0, false, false, null));
        assertEquals(2, config.blockMultiplier("minecraft:dirt", "soil", 7, false, false, null));
    }

    @Test
    public void naturalOnlyAndSafetyFiltersPreventDuplication() {
        LegacyConfig config = LegacyConfig.defaults();
        assertEquals(1, config.blockMultiplier("minecraft:diamond_ore", "ores", 0, true, false, null));
        assertEquals(1, config.blockMultiplier("minecraft:bedrock", "stone", 0, false, false, null));
        assertEquals(1, config.blockMultiplier("minecraft:chest", "building_blocks", 0, false, true, null));
    }

    @Test
    public void jsonRoundTripAndBoundsAreSafe() throws Exception {
        LegacyConfig config = LegacyConfig.defaults();
        config.globalMultiplier = 999;
        config.defaultEntityMultiplier = -8;
        File target = new File(temporary.getRoot(), "smart_resource_drops.json");
        assertTrue(config.save(target));
        LegacyConfig loaded = LegacyConfig.load(target);
        assertEquals(64, loaded.globalMultiplier);
        assertEquals(0, loaded.defaultEntityMultiplier);
        assertTrue(target.isFile());
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
    public void realPlayerOverrideHasFinalPrecedenceAndIsNotAppliedWithoutAPlayer() {
        LegacyConfig config = LegacyConfig.defaults();
        config.globalMultiplier = 2;
        config.dimensionMultipliers.put("0", Integer.valueOf(3));
        config.categoryMultipliers.put("ores", Integer.valueOf(4));
        config.blockMultipliers.put("minecraft:diamond_ore", Integer.valueOf(5));
        config.allowPlayerOverrides = true;
        config.maxPlayerMultiplier = 4;
        config.playerMultipliers.put("player-id", Integer.valueOf(3));

        assertEquals(3, config.blockMultiplier(
                "minecraft:diamond_ore", "ores", 0, false, false, "PLAYER-ID"));
        assertEquals(5, config.blockMultiplier(
                "minecraft:diamond_ore", "ores", 0, false, false, null));
    }

    @Test
    public void zeroDropRuleDoesNotMakeEligibleBlockExperienceIneligible() {
        LegacyConfig config = LegacyConfig.defaults();
        config.blockMultipliers.put("minecraft:diamond_ore", Integer.valueOf(0));

        assertEquals(0, config.blockMultiplier(
                "minecraft:diamond_ore", "ores", 0, false, false, null));
        assertTrue(config.isBlockEligible("minecraft:diamond_ore", false, false));
        assertFalse(config.isBlockEligible("minecraft:diamond_ore", true, false));
    }
}
