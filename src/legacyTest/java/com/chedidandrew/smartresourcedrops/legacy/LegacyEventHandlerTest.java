package com.chedidandrew.smartresourcedrops.legacy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.Test;

public final class LegacyEventHandlerTest {
    @Test
    public void finalLootAndExplosionHooksRunAtTheRequiredEventPriority() throws Exception {
        assertPriority("onHarvestDrops", EventPriority.LOWEST, false);
        assertPriority("onExplosion", EventPriority.LOWEST, false);
        assertPriority("onLivingDrops", EventPriority.LOWEST, true);
    }

    @Test
    public void blockExperienceUsesDedicatedMultiplierRatherThanDropMultiplier() {
        LegacyConfig config = LegacyConfig.defaults();
        config.globalMultiplier = 64;
        config.multiplyExperience = true;
        config.experienceMultiplier = 2;

        assertEquals(6, LegacyEventHandler.multiplyBlockExperience(3, config, true, true));
        assertEquals(3, LegacyEventHandler.multiplyBlockExperience(3, config, false, true));
        assertEquals(3, LegacyEventHandler.multiplyBlockExperience(3, config, true, false));
    }

    @Test
    public void mobExperienceComposesWithExistingOrbValue() {
        assertEquals(21, LegacyEventHandler.multiplyExistingExperience(7, 3));
        assertEquals(634112, LegacyEventHandler.multiplyExistingExperience(9908, 64));
        assertEquals(10000, LegacyEventHandler.multiplyExistingExperience(10000, 64));
        assertEquals(Integer.MAX_VALUE,
                LegacyEventHandler.multiplyExistingExperience(Integer.MAX_VALUE, 64));
    }

    @Test
    public void outputBudgetIsAllOrNothingAndOverflowSafe() {
        assertEquals(true, LegacyEventHandler.fitsOutputBudget(64, 4096L, 64, 4096, 262144L));
        assertEquals(false, LegacyEventHandler.fitsOutputBudget(65, 4096L, 64, 4096, 262144L));
        assertEquals(false, LegacyEventHandler.fitsOutputBudget(1, 4097L, 64, 4096, 262144L));
        assertEquals(false, LegacyEventHandler.fitsOutputBudget(
                Integer.MAX_VALUE, Long.MAX_VALUE, Integer.MAX_VALUE, 4096, 262144L));
    }

    @Test
    public void zeroEntityMultiplierPreservesSaddlesOnly() {
        Object saddle = new Object();
        Object ordinary = new Object();
        assertTrue(LegacyEventHandler.isProtectedItemIdentity(saddle, saddle));
        assertFalse(LegacyEventHandler.isProtectedItemIdentity(ordinary, saddle));
        assertTrue(LegacyEventHandler.retainEntityLootAtMultiplier(0, true));
        assertFalse(LegacyEventHandler.retainEntityLootAtMultiplier(0, false));
        assertTrue(LegacyEventHandler.retainEntityLootAtMultiplier(2, false));
    }

    @Test
    public void mobExperienceUsesEntityFilterKillAndBossPolicies() {
        LegacyConfig config = LegacyConfig.defaults();
        config.multiplyMobExperience = true;
        assertTrue(LegacyEventHandler.entityExperienceEligible(
                config, "minecraft:zombie", false, true));
        assertFalse(LegacyEventHandler.entityExperienceEligible(
                config, "minecraft:zombie", false, false));

        config.entityBlacklist.add("minecraft:zombie");
        assertFalse(LegacyEventHandler.entityExperienceEligible(
                config, "minecraft:zombie", false, true));
        assertFalse(LegacyEventHandler.entityExperienceEligible(
                config, "minecraft:wither", true, true));
        config.multiplyBossExperience = true;
        assertTrue(LegacyEventHandler.entityExperienceEligible(
                config, "minecraft:wither", true, true));
    }

    @Test
    public void killRequirementsRequireVanillaAttributionAndRejectWrongOrigins() {
        LegacyConfig.EntityKillRequirement player =
                LegacyConfig.EntityKillRequirement.PLAYER_KILLS_ONLY;
        LegacyConfig.EntityKillRequirement tamed =
                LegacyConfig.EntityKillRequirement.PLAYER_OR_TAMED_ENTITY;
        LegacyConfig.EntityKillRequirement all =
                LegacyConfig.EntityKillRequirement.ALL_STANDARD_DEATH_LOOT;

        assertTrue(LegacyEventHandler.killRequirementEligible(
                player, true, LegacyEventHandler.KillOrigin.DIRECT_PLAYER));
        assertFalse(LegacyEventHandler.killRequirementEligible(
                player, false, LegacyEventHandler.KillOrigin.DIRECT_PLAYER));
        assertFalse(LegacyEventHandler.killRequirementEligible(
                player, true, LegacyEventHandler.KillOrigin.TAMED_ENTITY));
        assertTrue(LegacyEventHandler.killRequirementEligible(
                tamed, true, LegacyEventHandler.KillOrigin.TAMED_ENTITY));
        assertFalse(LegacyEventHandler.killRequirementEligible(
                tamed, true, LegacyEventHandler.KillOrigin.NONE));
        assertTrue(LegacyEventHandler.killRequirementEligible(
                all, false, LegacyEventHandler.KillOrigin.NONE));
    }

    @Test
    public void missingPlayerIsNeverClassifiedAsRealPlayerMining() {
        assertFalse(LegacyEventHandler.isRealPlayer(null));
    }

    @Test
    public void pistonProtectionMovesKnownProvenanceAndHonorsConservativeFallback() {
        assertTrue(LegacyEventHandler.shouldProtectPistonDestination(true, false));
        assertTrue(LegacyEventHandler.shouldProtectPistonDestination(true, true));
        assertTrue(LegacyEventHandler.shouldProtectPistonDestination(false, true));
        assertFalse(LegacyEventHandler.shouldProtectPistonDestination(false, false));
    }

    @Test
    public void pistonChainKeepsCoordinatesThatAreAlsoProtectedDestinations() {
        Long firstSource = Long.valueOf(PlacedBlockData.pack(10, 64, 10));
        Long middle = Long.valueOf(PlacedBlockData.pack(11, 64, 10));
        Long finalDestination = Long.valueOf(PlacedBlockData.pack(12, 64, 10));
        Set<Long> sources = new LinkedHashSet<Long>();
        sources.add(firstSource);
        sources.add(middle);
        Set<Long> destinations = new LinkedHashSet<Long>();
        destinations.add(middle);
        destinations.add(finalDestination);

        Set<Long> removals = LegacyEventHandler.pistonSourcesToRemove(sources, destinations);
        assertTrue(removals.contains(firstSource));
        assertFalse(removals.contains(middle));
        assertEquals(1, removals.size());
    }

    @Test
    public void legacyVanillaEntityNamesMapToModernConfigKeys() {
        assertEquals("minecraft:mooshroom", LegacyRules.normalizeVanillaEntityId("MushroomCow"));
        assertEquals("minecraft:iron_golem", LegacyRules.normalizeVanillaEntityId("VillagerGolem"));
        assertEquals("minecraft:wither", LegacyRules.normalizeVanillaEntityId("WitherBoss"));
        assertTrue(LegacyRules.isSpecialShearingEntityId("minecraft:mooshroom"));
        assertFalse(LegacyRules.isSpecialShearingEntityId("minecraft:sheep"));
        assertTrue(LegacyRules.isSafeShearingEntityId("minecraft:sheep", false));
        assertFalse(LegacyRules.isSafeShearingEntityId("minecraft:mooshroom", true));
        assertFalse(LegacyRules.isSafeShearingEntityId("example:unknown_shearable", false));
        assertTrue(LegacyRules.isSafeShearingEntityId("example:certified_shearable", true));
    }

    @Test
    public void registryNameFallbacksDoNotMisclassifyWoodenDoorsOrWordsContainingOre() {
        assertTrue(LegacyRules.isLogPath("rubber_wood"));
        assertTrue(LegacyRules.isLogPath("silver_log"));
        assertFalse(LegacyRules.isLogPath("wooden_door"));
        assertFalse(LegacyRules.isLogPath("catalogue"));
        assertTrue(LegacyRules.isOrePath("copper_ore"));
        assertFalse(LegacyRules.isOrePath("more_stone"));
    }

    private static void assertPriority(
            String methodName, EventPriority expected, boolean receiveCanceled) throws Exception {
        Method selected = null;
        for (Method method : LegacyEventHandler.class.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                selected = method;
                break;
            }
        }
        assertTrue("Missing event handler " + methodName, selected != null);
        SubscribeEvent annotation = selected.getAnnotation(SubscribeEvent.class);
        assertTrue("Missing @SubscribeEvent on " + methodName, annotation != null);
        assertEquals(expected, annotation.priority());
        assertEquals(receiveCanceled, annotation.receiveCanceled());
    }
}
