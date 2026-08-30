package com.chedidandrew.smartresourcedrops.core;

import com.chedidandrew.smartresourcedrops.config.SmartDropsConfig;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RuleResolutionTraceTest {
    private static final String PLAYER_ID = "00000000-0000-0000-0000-000000000001";

    @Test
    void naturalAndPlacedBlocksExposeTheAuthoritativeProvenanceResult() {
        SmartDropsConfig config = SmartDropsConfig.defaults();

        RuleResolutionTrace natural = RuleEngine.trace(config, input(
                "minecraft:stone", categories(Category.STONE), false, false, Set.of(), DropSource.PLAYER));
        assertTrue(natural.provenanceEligible());
        assertTrue(natural.eligible());
        assertEquals(2, natural.configuredMultiplier());
        assertEquals(2, natural.appliedMultiplier());
        assertEquals(RuleEngine.Reason.GLOBAL_RULE, natural.reason());

        RuleResolutionTrace placed = RuleEngine.trace(config, input(
                "minecraft:stone", categories(Category.STONE), true, false, Set.of(), DropSource.PLAYER));
        assertFalse(placed.provenanceEligible());
        assertFalse(placed.eligible());
        assertEquals(2, placed.configuredMultiplier());
        assertEquals(1, placed.appliedMultiplier());
        assertEquals(RuleEngine.Reason.PLAYER_PLACED_PROTECTED, placed.reason());
        assertEquals(placed.decision(), RuleEngine.resolve(config, input(
                "minecraft:stone", categories(Category.STONE), true, false, Set.of(), DropSource.PLAYER)));
    }

    @Test
    void logOreAndEmptyCategoryInputsRemainVisibleInTheTrace() {
        SmartDropsConfig config = SmartDropsConfig.defaults();

        RuleResolutionTrace log = RuleEngine.trace(config, input(
                "example:redwood_log", categories(Category.LOGS), false, false, Set.of(), DropSource.PLAYER));
        assertEquals(List.of(Category.LOGS), log.matchedCategories());
        assertEquals(Category.LOGS, log.selectedCategory());

        RuleResolutionTrace ore = RuleEngine.trace(config, input(
                "example:ruby_ore", categories(Category.ORES), false, false, Set.of("c:ores"), DropSource.PLAYER));
        assertEquals(List.of(Category.ORES), ore.matchedCategories());
        assertEquals(Category.ORES, ore.selectedCategory());

        RuleResolutionTrace miscellaneous = RuleEngine.trace(config, input(
                "example:limestone", new LinkedHashSet<>(), false, false, Set.of(), DropSource.PLAYER));
        assertEquals(List.of(Category.MISCELLANEOUS), miscellaneous.matchedCategories());
        assertEquals(Category.MISCELLANEOUS, miscellaneous.selectedCategory());
        assertEquals(RuleResolutionTrace.RuleSource.GLOBAL, miscellaneous.selectedRule());
    }

    @Test
    void overridePrecedenceAndEveryCandidateAreReported() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        config.globalMultiplier = 2;
        config.dimensionMultipliers.put("minecraft:overworld", 3);
        config.categoryMultipliers.put(Category.LOGS.key(), 4);
        config.blockMultipliers.put("example:redwood_log", 5);
        config.allowPlayerOverrides = true;
        config.maxPlayerMultiplier = 10;
        config.playerMultipliers.put(PLAYER_ID, 6);
        RuleEngine.RuleInput input = input(
                "example:redwood_log", categories(Category.LOGS), false, false, Set.of(), DropSource.PLAYER);

        RuleResolutionTrace trace = RuleEngine.trace(config, input);
        assertEquals(2, trace.globalMultiplier());
        assertEquals(3, trace.dimensionOverride());
        assertEquals(Map.of(Category.LOGS, 4), trace.categoryOverrides());
        assertEquals(Category.LOGS, trace.categoryRuleCategory());
        assertEquals(4, trace.categoryOverride());
        assertEquals(5, trace.blockOverride());
        assertEquals(6, trace.storedPlayerOverride());
        assertEquals(6, trace.effectivePlayerOverride());
        assertEquals(RuleResolutionTrace.RuleSource.PLAYER_OVERRIDE, trace.selectedRule());
        assertEquals(6, trace.configuredMultiplier());

        config.allowPlayerOverrides = false;
        trace = RuleEngine.trace(config, input);
        assertEquals(6, trace.storedPlayerOverride());
        assertNull(trace.effectivePlayerOverride());
        assertEquals(RuleResolutionTrace.RuleSource.BLOCK_OVERRIDE, trace.selectedRule());
        assertEquals(5, trace.configuredMultiplier());

        config.blockMultipliers.clear();
        trace = RuleEngine.trace(config, input);
        assertEquals(RuleResolutionTrace.RuleSource.CATEGORY_OVERRIDE, trace.selectedRule());
        assertEquals(4, trace.configuredMultiplier());

        config.categoryMultipliers.clear();
        trace = RuleEngine.trace(config, input);
        assertEquals(RuleResolutionTrace.RuleSource.DIMENSION_OVERRIDE, trace.selectedRule());
        assertEquals(3, trace.configuredMultiplier());

        config.dimensionMultipliers.clear();
        trace = RuleEngine.trace(config, input);
        assertEquals(RuleResolutionTrace.RuleSource.GLOBAL, trace.selectedRule());
        assertEquals(2, trace.configuredMultiplier());
    }

    @Test
    void exactAndTagBlacklistDiagnosticsAreIndependent() {
        SmartDropsConfig exactConfig = SmartDropsConfig.defaults();
        exactConfig.blacklist.add("example:ruby_ore");
        RuleResolutionTrace exact = RuleEngine.trace(exactConfig, input(
                "example:ruby_ore", categories(Category.ORES), false, false, Set.of(), DropSource.PLAYER));
        assertTrue(exact.exactBlacklisted());
        assertTrue(exact.matchingBlacklistTags().isEmpty());
        assertFalse(exact.filterEligible());
        assertEquals(RuleEngine.Reason.FILTERED, exact.reason());

        SmartDropsConfig tagConfig = SmartDropsConfig.defaults();
        tagConfig.tagBlacklist.add("c:ores");
        RuleResolutionTrace tagged = RuleEngine.trace(tagConfig, input(
                "example:ruby_ore", categories(Category.ORES), false, false,
                Set.of("c:ores", "example:gem_ores"), DropSource.PLAYER));
        assertFalse(tagged.exactBlacklisted());
        assertEquals(Set.of("c:ores"), tagged.matchingBlacklistTags());
        assertFalse(tagged.filterEligible());
        assertEquals(RuleEngine.Reason.FILTERED, tagged.reason());
    }

    @Test
    void whitelistAcceptsExactOrTagMatchesAndRejectsNoMatch() {
        SmartDropsConfig exactConfig = SmartDropsConfig.defaults();
        exactConfig.filterMode = SmartDropsConfig.FilterMode.WHITELIST;
        exactConfig.whitelist.add("example:ruby_ore");
        RuleResolutionTrace exact = RuleEngine.trace(exactConfig, input(
                "example:ruby_ore", categories(Category.ORES), false, false, Set.of(), DropSource.PLAYER));
        assertTrue(exact.exactWhitelisted());
        assertTrue(exact.filterEligible());
        assertTrue(exact.eligible());

        SmartDropsConfig tagConfig = SmartDropsConfig.defaults();
        tagConfig.filterMode = SmartDropsConfig.FilterMode.WHITELIST;
        tagConfig.tagWhitelist.add("c:ores");
        RuleResolutionTrace tagged = RuleEngine.trace(tagConfig, input(
                "example:ruby_ore", categories(Category.ORES), false, false,
                Set.of("c:ores", "example:gem_ores"), DropSource.PLAYER));
        assertFalse(tagged.exactWhitelisted());
        assertEquals(Set.of("c:ores"), tagged.matchingWhitelistTags());
        assertTrue(tagged.filterEligible());
        assertTrue(tagged.eligible());

        RuleResolutionTrace absent = RuleEngine.trace(tagConfig, input(
                "example:limestone", categories(Category.MISCELLANEOUS), false, false,
                Set.of("example:stone"), DropSource.PLAYER));
        assertFalse(absent.exactWhitelisted());
        assertTrue(absent.matchingWhitelistTags().isEmpty());
        assertFalse(absent.filterEligible());
        assertFalse(absent.eligible());
        assertEquals(RuleEngine.Reason.FILTERED, absent.reason());
    }

    @Test
    void blockEntityProtectionAndAllowlistingAreReportedSeparately() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        RuleEngine.RuleInput input = input(
                "example:wooden_crate", categories(Category.BUILDING_BLOCKS), false, true,
                Set.of(), DropSource.PLAYER);

        RuleResolutionTrace protectedTrace = RuleEngine.trace(config, input);
        assertTrue(protectedTrace.hasBlockEntity());
        assertTrue(protectedTrace.blockEntityProtectionEnabled());
        assertFalse(protectedTrace.blockEntityAllowlisted());
        assertTrue(protectedTrace.blockEntityProtected());
        assertFalse(protectedTrace.eligible());
        assertEquals(RuleEngine.Reason.BLOCK_ENTITY_PROTECTED, protectedTrace.reason());

        config.blockEntityAllowlist.add("example:wooden_crate");
        RuleResolutionTrace allowlisted = RuleEngine.trace(config, input);
        assertTrue(allowlisted.blockEntityAllowlisted());
        assertFalse(allowlisted.blockEntityProtected());
        assertTrue(allowlisted.eligible());

        config.blockEntityAllowlist.clear();
        config.protectBlockEntities = false;
        RuleResolutionTrace protectionDisabled = RuleEngine.trace(config, input);
        assertFalse(protectionDisabled.blockEntityProtectionEnabled());
        assertFalse(protectionDisabled.blockEntityProtected());
        assertTrue(protectionDisabled.eligible());
    }

    @Test
    void everySourceToggleUsesTheSameTraceReason() {
        SmartDropsConfig playerConfig = SmartDropsConfig.defaults();
        playerConfig.playerMining = false;
        assertSourceDisabled(playerConfig, DropSource.PLAYER);

        SmartDropsConfig explosionConfig = SmartDropsConfig.defaults();
        explosionConfig.explosions = false;
        assertSourceDisabled(explosionConfig, DropSource.EXPLOSION);

        SmartDropsConfig automationConfig = SmartDropsConfig.defaults();
        automationConfig.automatedMining = false;
        assertSourceDisabled(automationConfig, DropSource.AUTOMATION);

        automationConfig.automatedMining = true;
        RuleResolutionTrace enabled = RuleEngine.trace(automationConfig, input(
                "minecraft:stone", categories(Category.STONE), false, false, Set.of(), DropSource.AUTOMATION));
        assertTrue(enabled.sourceEnabled());
        assertTrue(enabled.eligible());
    }

    @Test
    void sourceModeAndProtectionMatrixMatchesGameplay() {
        SmartDropsConfig config = SmartDropsConfig.defaults();

        assertProvenance(config, false, true, null);
        assertProvenance(config, true, false, RuleEngine.Reason.PLAYER_PLACED_PROTECTED);

        config.smartPlacementProtection = false;
        assertProvenance(config, true, true, null);

        config.sourceMode = SmartDropsConfig.SourceMode.ALL;
        config.smartPlacementProtection = true;
        assertProvenance(config, false, true, null);
        assertProvenance(config, true, true, null);

        config.sourceMode = SmartDropsConfig.SourceMode.PLAYER_PLACED_ONLY;
        assertProvenance(config, false, false, RuleEngine.Reason.NATURAL_BLOCK_EXCLUDED);
        assertProvenance(config, true, true, null);
    }

    @Test
    void zeroOneAndMaximumMultipliersRemainEligible() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        config.maximumMultiplier = 64;

        for (int multiplier : List.of(0, 1, 64)) {
            config.blockMultipliers.put("minecraft:stone", multiplier);
            RuleResolutionTrace trace = RuleEngine.trace(config, input(
                    "minecraft:stone", categories(Category.STONE), false, false, Set.of(), DropSource.PLAYER));
            assertTrue(trace.eligible(), () -> multiplier + "x should remain eligible");
            assertEquals(multiplier, trace.selectedRuleValue());
            assertEquals(multiplier, trace.configuredMultiplier());
            assertEquals(multiplier, trace.appliedMultiplier());
            assertEquals(RuleResolutionTrace.RuleSource.BLOCK_OVERRIDE, trace.selectedRule());
        }
    }

    @Test
    void multipleCategoryOverridesHonorInputOrder() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        config.categoryMultipliers.put(Category.LOGS.key(), 3);
        config.categoryMultipliers.put(Category.PLANTS.key(), 4);

        RuleResolutionTrace plantsFirst = RuleEngine.trace(config, input(
                "example:living_log", categories(Category.PLANTS, Category.LOGS), false, false,
                Set.of(), DropSource.PLAYER));
        assertEquals(List.of(Category.PLANTS, Category.LOGS), plantsFirst.matchedCategories());
        assertEquals(List.of(Category.PLANTS, Category.LOGS),
                new ArrayList<>(plantsFirst.categoryOverrides().keySet()));
        assertEquals(Category.PLANTS, plantsFirst.categoryRuleCategory());
        assertEquals(4, plantsFirst.categoryOverride());
        assertEquals(4, plantsFirst.configuredMultiplier());

        RuleResolutionTrace logsFirst = RuleEngine.trace(config, input(
                "example:living_log", categories(Category.LOGS, Category.PLANTS), false, false,
                Set.of(), DropSource.PLAYER));
        assertEquals(List.of(Category.LOGS, Category.PLANTS), logsFirst.matchedCategories());
        assertEquals(Category.LOGS, logsFirst.categoryRuleCategory());
        assertEquals(3, logsFirst.categoryOverride());
        assertEquals(3, logsFirst.configuredMultiplier());
    }

    @Test
    void blockedTraceRetainsConfiguredRuleButAppliesVanilla() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        config.blockMultipliers.put("minecraft:stone", 7);
        RuleEngine.RuleInput input = input(
                "minecraft:stone", categories(Category.STONE), true, false, Set.of(), DropSource.PLAYER);

        RuleResolutionTrace trace = RuleEngine.trace(config, input);
        assertEquals(RuleResolutionTrace.RuleSource.BLOCK_OVERRIDE, trace.selectedRule());
        assertEquals(7, trace.selectedRuleValue());
        assertEquals(7, trace.configuredMultiplier());
        assertEquals(1, trace.appliedMultiplier());
        assertFalse(trace.eligible());
        assertEquals(RuleEngine.Reason.PLAYER_PLACED_PROTECTED, trace.reason());
        assertEquals(new RuleEngine.Decision(
                1, false, RuleEngine.Reason.PLAYER_PLACED_PROTECTED, Category.MISCELLANEOUS), trace.decision());
        assertEquals(trace.decision(), RuleEngine.resolve(config, input));
    }

    @Test
    void blockingReasonPrecedenceRemainsStableWhileRuleSelectionIsPreserved() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        config.enabled = false;
        config.playerMining = false;
        config.blockMultipliers.put("example:crate", 7);
        config.tagBlacklist.add("c:blocked");
        RuleEngine.RuleInput input = input(
                "example:crate", categories(Category.BUILDING_BLOCKS), true, true,
                Set.of("c:blocked"), DropSource.PLAYER);

        assertBlocked(config, input, RuleEngine.Reason.MOD_DISABLED);
        config.enabled = true;
        assertBlocked(config, input, RuleEngine.Reason.SOURCE_DISABLED);
        config.playerMining = true;
        assertBlocked(config, input, RuleEngine.Reason.BLOCK_ENTITY_PROTECTED);
        config.blockEntityAllowlist.add("example:crate");
        assertBlocked(config, input, RuleEngine.Reason.FILTERED);
        config.tagBlacklist.clear();
        assertBlocked(config, input, RuleEngine.Reason.PLAYER_PLACED_PROTECTED);

        config.smartPlacementProtection = false;
        RuleResolutionTrace eligible = RuleEngine.trace(config, input);
        assertTrue(eligible.eligible());
        assertEquals(7, eligible.appliedMultiplier());
        assertEquals(RuleEngine.Reason.BLOCK_RULE, eligible.reason());
    }

    private static void assertSourceDisabled(SmartDropsConfig config, DropSource source) {
        RuleResolutionTrace trace = RuleEngine.trace(config, input(
                "minecraft:stone", categories(Category.STONE), false, false, Set.of(), source));
        assertFalse(trace.sourceEnabled());
        assertFalse(trace.eligible());
        assertEquals(1, trace.appliedMultiplier());
        assertEquals(RuleEngine.Reason.SOURCE_DISABLED, trace.reason());
    }

    private static void assertProvenance(
            SmartDropsConfig config,
            boolean placed,
            boolean expectedEligible,
            RuleEngine.Reason blockedReason
    ) {
        RuleResolutionTrace trace = RuleEngine.trace(config, input(
                "minecraft:stone", categories(Category.STONE), placed, false, Set.of(), DropSource.PLAYER));
        assertEquals(expectedEligible, trace.provenanceEligible());
        assertEquals(expectedEligible, trace.eligible());
        if (blockedReason != null) {
            assertEquals(blockedReason, trace.reason());
        }
    }

    private static void assertBlocked(
            SmartDropsConfig config,
            RuleEngine.RuleInput input,
            RuleEngine.Reason expectedReason
    ) {
        RuleResolutionTrace trace = RuleEngine.trace(config, input);
        assertFalse(trace.eligible());
        assertEquals(expectedReason, trace.reason());
        assertEquals(RuleResolutionTrace.RuleSource.BLOCK_OVERRIDE, trace.selectedRule());
        assertEquals(7, trace.configuredMultiplier());
        assertEquals(1, trace.appliedMultiplier());
    }

    private static RuleEngine.RuleInput input(
            String blockId,
            LinkedHashSet<Category> categories,
            boolean placed,
            boolean blockEntity,
            Set<String> filterTags,
            DropSource source
    ) {
        return new RuleEngine.RuleInput(
                blockId,
                "minecraft:overworld",
                categories,
                placed,
                blockEntity,
                filterTags,
                source,
                PLAYER_ID);
    }

    private static LinkedHashSet<Category> categories(Category... values) {
        return new LinkedHashSet<>(List.of(values));
    }
}
