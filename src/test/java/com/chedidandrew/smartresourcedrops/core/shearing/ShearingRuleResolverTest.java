package com.chedidandrew.smartresourcedrops.core.shearing;

import com.chedidandrew.smartresourcedrops.config.SmartDropsConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ShearingRuleResolverTest {
    @Test
    void freshManualStandardResourceInheritsGlobalMultiplier() {
        SmartDropsConfig config = SmartDropsConfig.defaults();

        ShearingRuleTrace trace = ShearingRuleResolver.trace(
                config,
                "minecraft:sheep",
                true,
                false,
                ShearingSource.MANUAL_PLAYER);

        assertEquals(ShearingClassification.STANDARD_RESOURCE, trace.classification());
        assertEquals(2, trace.appliedMultiplier());
        assertEquals(ShearingRuleTrace.RuleSource.GLOBAL_DEFAULT, trace.selectedRule());
        assertTrue(trace.multiplicationEligible());
    }

    @Test
    void automatedSourceDefaultsToVanilla() {
        SmartDropsConfig config = SmartDropsConfig.defaults();

        ShearingRuleTrace trace = ShearingRuleResolver.trace(
                config,
                "minecraft:sheep",
                true,
                false,
                ShearingSource.VANILLA_DISPENSER);

        assertFalse(trace.sourceEnabled());
        assertEquals(1, trace.appliedMultiplier());
        assertEquals(ShearingRuleTrace.RuleSource.SOURCE_DISABLED, trace.selectedRule());
    }

    @Test
    void exactOverrideWinsForCertifiedStandardResource() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        config.inheritDefaultShearingMultiplier = false;
        config.defaultShearingMultiplier = 3;
        config.shearingEntityMultipliers.put("minecraft:sheep", 7);

        ShearingRuleTrace trace = ShearingRuleResolver.trace(
                config,
                "minecraft:sheep",
                true,
                false,
                ShearingSource.MANUAL_PLAYER);

        assertEquals(7, trace.appliedMultiplier());
        assertEquals(7, trace.exactOverride());
        assertEquals(ShearingRuleTrace.RuleSource.ENTITY_OVERRIDE, trace.selectedRule());
    }

    @Test
    void specialTagWinsConflictAndOverride() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        config.shearingEntityMultipliers.put("minecraft:mooshroom", 64);

        ShearingRuleTrace trace = ShearingRuleResolver.trace(
                config,
                "minecraft:mooshroom",
                true,
                true,
                ShearingSource.MANUAL_PLAYER);

        assertEquals(ShearingClassification.SPECIAL, trace.classification());
        assertTrue(trace.tagConflict());
        assertFalse(trace.overrideReachable());
        assertEquals(64, trace.exactOverride());
        assertEquals(1, trace.appliedMultiplier());
        assertEquals(ShearingRuleTrace.RuleSource.SPECIAL_SAFETY, trace.selectedRule());
    }

    @Test
    void knownVanillaSpecialRemainsFixedIfDataPackReplacesSpecialTag() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        config.shearingEntityMultipliers.put("minecraft:mooshroom", 64);

        ShearingRuleTrace trace = ShearingRuleResolver.trace(
                config,
                "minecraft:mooshroom",
                true,
                false,
                ShearingSource.MANUAL_PLAYER);

        assertEquals(ShearingClassification.SPECIAL, trace.classification());
        assertTrue(trace.knownVanillaSpecial());
        assertFalse(trace.specialTagged());
        assertTrue(trace.safetyConflict());
        assertEquals(1, trace.appliedMultiplier());
    }

    @Test
    void unknownEntityRemainsVanillaDespiteOverride() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        config.shearingEntityMultipliers.put("example:progression_shearable", 64);

        ShearingRuleTrace trace = ShearingRuleResolver.trace(
                config,
                "example:progression_shearable",
                false,
                false,
                ShearingSource.MANUAL_PLAYER);

        assertEquals(ShearingClassification.UNKNOWN, trace.classification());
        assertEquals(64, trace.exactOverride());
        assertEquals(1, trace.appliedMultiplier());
        assertEquals(ShearingRuleTrace.RuleSource.UNKNOWN_SAFETY, trace.selectedRule());
    }

    @Test
    void globalDisableKeepsCertifiedEntityVanilla() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        config.enabled = false;

        ShearingRuleTrace trace = ShearingRuleResolver.trace(
                config,
                "minecraft:sheep",
                true,
                false,
                ShearingSource.MANUAL_PLAYER);

        assertEquals(1, trace.appliedMultiplier());
        assertEquals(ShearingRuleTrace.RuleSource.MOD_DISABLED, trace.selectedRule());
    }
}
