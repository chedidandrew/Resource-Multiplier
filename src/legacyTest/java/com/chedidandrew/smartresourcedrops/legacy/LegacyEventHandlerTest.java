package com.chedidandrew.smartresourcedrops.legacy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class LegacyEventHandlerTest {
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
    public void mobExperienceComposesWithEarlierEventAdjustments() {
        assertEquals(21, LegacyEventHandler.multiplyExistingExperience(7, 3));
        assertEquals(Integer.MAX_VALUE,
                LegacyEventHandler.multiplyExistingExperience(Integer.MAX_VALUE, 64));
    }

    @Test
    public void outputBudgetIsAllOrNothingBeforeMaterialization() {
        assertTrue(LegacyEventHandler.fitsOutputBudget(64, 4096L, 64, 4096, 262144L));
        assertFalse(LegacyEventHandler.fitsOutputBudget(65, 4096L, 64, 4096, 262144L));
        assertFalse(LegacyEventHandler.fitsOutputBudget(64, 4097L, 64, 4096, 262144L));
    }
}
