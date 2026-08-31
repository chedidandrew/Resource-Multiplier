package com.chedidandrew.smartresourcedrops.core;

import com.chedidandrew.smartresourcedrops.config.SmartDropsConfig;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RuleEngineTest {
    private static int assertions;

    private RuleEngineTest() {
    }

    public static void main(String[] args) {
        naturalBlockUsesGlobalMultiplier();
        placedBlockIsProtectedByDefault();
        protectionCanBeDisabled();
        overridePrecedenceIsCorrect();
        whitelistModeIsClosedByDefault();
        blockEntitiesAreProtectedUnlessAllowed();
        sourceTogglesAreRespected();
        zeroMultiplierSuppressesLoot();
        playerOverridesAreCapped();
        placedOnlyModeWorks();
        packedCoordinatesStayUniqueInPracticalRange();
        sanitizationClampsUnsafeValues();
        masterSwitchDisablesMultiplication();
        exactAndTagFiltersWork();
        allSourceModeAllowsPlacedBlocks();
        sourceModeProtectionMatrixIsExact();
        dragonEggIsAConfigurableDefaultSafetyRule();
        ruleValuesRespectMaximum();
        blockEntityProtectionCanBeDisabled();
        traceExposesTheCompleteOverrideChain();
        blockedTraceKeepsConfiguredRuleAndAppliedVanillaValue();
        traceFilterDiagnosticsAreExact();
        multipleCategoryTraceHonorsInputOrder();
        System.out.println("PASS: " + assertions + " Smart Resource Multiplier core assertions");
    }

    private static void naturalBlockUsesGlobalMultiplier() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        RuleEngine.Decision decision = RuleEngine.resolve(config, input(false, false, DropSource.PLAYER));
        equal(2, decision.multiplier(), "natural block global multiplier");
        truth(decision.eligible(), "natural block should be eligible");
        equal(RuleEngine.Reason.GLOBAL_RULE, decision.reason(), "global reason");
    }

    private static void placedBlockIsProtectedByDefault() {
        RuleEngine.Decision decision = RuleEngine.resolve(
                SmartDropsConfig.defaults(),
                input(true, false, DropSource.PLAYER));
        falsity(decision.eligible(), "placed block should not be multiplied");
        equal(RuleEngine.Reason.PLAYER_PLACED_PROTECTED, decision.reason(), "placed block reason");
    }

    private static void protectionCanBeDisabled() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        config.smartPlacementProtection = false;
        RuleEngine.Decision decision = RuleEngine.resolve(config, input(true, false, DropSource.PLAYER));
        truth(decision.eligible(), "placed block can be multiplied when protection is off");
        equal(2, decision.multiplier(), "placed block multiplier with protection off");
    }

    private static void overridePrecedenceIsCorrect() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        config.dimensionMultipliers.put("minecraft:overworld", 3);
        config.categoryMultipliers.put(Category.STONE.key(), 4);
        config.blockMultipliers.put("minecraft:stone", 5);
        RuleEngine.Decision decision = RuleEngine.resolve(config, input(false, false, DropSource.PLAYER));
        equal(5, decision.multiplier(), "block override wins");
        equal(RuleEngine.Reason.BLOCK_RULE, decision.reason(), "block override reason");

        config.blockMultipliers.clear();
        decision = RuleEngine.resolve(config, input(false, false, DropSource.PLAYER));
        equal(4, decision.multiplier(), "category wins over dimension");

        config.categoryMultipliers.clear();
        decision = RuleEngine.resolve(config, input(false, false, DropSource.PLAYER));
        equal(3, decision.multiplier(), "dimension wins over global");
    }

    private static void whitelistModeIsClosedByDefault() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        config.filterMode = SmartDropsConfig.FilterMode.WHITELIST;
        RuleEngine.Decision blocked = RuleEngine.resolve(config, input(false, false, DropSource.PLAYER));
        falsity(blocked.eligible(), "empty whitelist should block multiplication");

        config.whitelist.add("minecraft:stone");
        RuleEngine.Decision allowed = RuleEngine.resolve(config, input(false, false, DropSource.PLAYER));
        truth(allowed.eligible(), "exact whitelist should allow multiplication");
    }

    private static void blockEntitiesAreProtectedUnlessAllowed() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        RuleEngine.Decision blocked = RuleEngine.resolve(config, input(false, true, DropSource.PLAYER));
        falsity(blocked.eligible(), "block entity should be protected");
        equal(RuleEngine.Reason.BLOCK_ENTITY_PROTECTED, blocked.reason(), "block entity reason");

        config.blockEntityAllowlist.add("minecraft:stone");
        RuleEngine.Decision allowed = RuleEngine.resolve(config, input(false, true, DropSource.PLAYER));
        truth(allowed.eligible(), "explicitly allowed block entity should pass");
    }

    private static void sourceTogglesAreRespected() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        RuleEngine.Decision automation = RuleEngine.resolve(config, input(false, false, DropSource.AUTOMATION));
        falsity(automation.eligible(), "automation disabled by default");
        equal(RuleEngine.Reason.SOURCE_DISABLED, automation.reason(), "automation reason");

        config.explosions = false;
        RuleEngine.Decision explosion = RuleEngine.resolve(config, input(false, false, DropSource.EXPLOSION));
        falsity(explosion.eligible(), "explosions toggle");

        config.playerMining = false;
        RuleEngine.Decision player = RuleEngine.resolve(config, input(false, false, DropSource.PLAYER));
        falsity(player.eligible(), "player mining toggle");
    }

    private static void zeroMultiplierSuppressesLoot() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        config.blockMultipliers.put("minecraft:stone", 0);
        RuleEngine.Decision decision = RuleEngine.resolve(config, input(false, false, DropSource.PLAYER));
        truth(decision.eligible(), "0x is an eligible rule");
        equal(0, decision.multiplier(), "0x suppresses loot");
    }

    private static void playerOverridesAreCapped() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        config.allowPlayerOverrides = true;
        config.maxPlayerMultiplier = 4;
        config.playerMultipliers.put("player-1", 20);
        RuleEngine.RuleInput input = new RuleEngine.RuleInput(
                "minecraft:stone",
                "minecraft:overworld",
                new LinkedHashSet<>(Set.of(Category.STONE)),
                false,
                false,
                Set.of(),
                DropSource.PLAYER,
                "player-1");
        RuleEngine.Decision decision = RuleEngine.resolve(config, input);
        equal(4, decision.multiplier(), "player override cap");
        equal(RuleEngine.Reason.PLAYER_OVERRIDE, decision.reason(), "player override reason");
    }

    private static void placedOnlyModeWorks() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        config.sourceMode = SmartDropsConfig.SourceMode.PLAYER_PLACED_ONLY;
        RuleEngine.Decision natural = RuleEngine.resolve(config, input(false, false, DropSource.PLAYER));
        falsity(natural.eligible(), "natural block excluded in placed-only mode");
        RuleEngine.Decision placed = RuleEngine.resolve(config, input(true, false, DropSource.PLAYER));
        truth(placed.eligible(), "placed block allowed in placed-only mode");
    }

    private static void packedCoordinatesStayUniqueInPracticalRange() {
        int a = PackedBlockPosition.pack(0, -64, 0);
        int b = PackedBlockPosition.pack(15, -64, 15);
        int c = PackedBlockPosition.pack(0, -63, 0);
        int d = PackedBlockPosition.pack(16, -64, 0);
        falsity(a == b, "local coordinates differ");
        falsity(a == c, "vertical coordinates differ");
        equal(a, d, "x is intentionally chunk-local");
    }

    private static void sanitizationClampsUnsafeValues() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        config.maximumMultiplier = 200;
        config.globalMultiplier = -8;
        config.experienceMultiplier = 900;
        config.maxPlayerMultiplier = 500;
        config.sanitize();
        equal(64, config.maximumMultiplier, "absolute maximum");
        equal(0, config.globalMultiplier, "global lower bound");
        equal(64, config.experienceMultiplier, "experience upper bound");
        equal(64, config.maxPlayerMultiplier, "player upper bound");
    }


    private static void masterSwitchDisablesMultiplication() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        config.enabled = false;
        RuleEngine.Decision decision = RuleEngine.resolve(config, input(false, false, DropSource.PLAYER));
        falsity(decision.eligible(), "master switch should disable multiplication");
        equal(RuleEngine.Reason.MOD_DISABLED, decision.reason(), "master switch reason");
    }

    private static void exactAndTagFiltersWork() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        config.blacklist.add("minecraft:stone");
        RuleEngine.Decision exactBlocked = RuleEngine.resolve(config, input(false, false, DropSource.PLAYER));
        falsity(exactBlocked.eligible(), "exact blacklist should block");

        config.blacklist.clear();
        config.tagBlacklist.add("c:stone");
        RuleEngine.RuleInput tagged = new RuleEngine.RuleInput(
                "minecraft:stone",
                "minecraft:overworld",
                new LinkedHashSet<>(Set.of(Category.STONE)),
                false,
                false,
                Set.of("c:stone"),
                DropSource.PLAYER,
                "player-1");
        RuleEngine.Decision tagBlocked = RuleEngine.resolve(config, tagged);
        falsity(tagBlocked.eligible(), "tag blacklist should block");

        config.filterMode = SmartDropsConfig.FilterMode.WHITELIST;
        config.tagBlacklist.clear();
        config.tagWhitelist.add("c:stone");
        RuleEngine.Decision tagAllowed = RuleEngine.resolve(config, tagged);
        truth(tagAllowed.eligible(), "tag whitelist should allow");
    }

    private static void allSourceModeAllowsPlacedBlocks() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        config.sourceMode = SmartDropsConfig.SourceMode.ALL;
        RuleEngine.Decision decision = RuleEngine.resolve(config, input(true, false, DropSource.PLAYER));
        truth(decision.eligible(), "ALL source mode should allow placed blocks");
        equal(2, decision.multiplier(), "ALL source mode multiplier");
    }

    private static void sourceModeProtectionMatrixIsExact() {
        assertSourceEligibility(SmartDropsConfig.SourceMode.NATURAL_ONLY, true, false, true,
                "natural-only protection-on natural block");
        assertSourceEligibility(SmartDropsConfig.SourceMode.NATURAL_ONLY, true, true, false,
                "natural-only protection-on placed block");
        assertSourceEligibility(SmartDropsConfig.SourceMode.NATURAL_ONLY, false, false, true,
                "natural-only protection-off natural block");
        assertSourceEligibility(SmartDropsConfig.SourceMode.NATURAL_ONLY, false, true, true,
                "natural-only protection-off placed block");

        assertSourceEligibility(SmartDropsConfig.SourceMode.PLAYER_PLACED_ONLY, true, false, false,
                "placed-only protection-on natural block");
        assertSourceEligibility(SmartDropsConfig.SourceMode.PLAYER_PLACED_ONLY, true, true, true,
                "placed-only protection-on placed block");
        assertSourceEligibility(SmartDropsConfig.SourceMode.PLAYER_PLACED_ONLY, false, false, false,
                "placed-only protection-off natural block");
        assertSourceEligibility(SmartDropsConfig.SourceMode.PLAYER_PLACED_ONLY, false, true, true,
                "placed-only protection-off placed block");

        assertSourceEligibility(SmartDropsConfig.SourceMode.ALL, true, false, true,
                "all protection-on natural block");
        assertSourceEligibility(SmartDropsConfig.SourceMode.ALL, true, true, true,
                "all protection-on placed block");
        assertSourceEligibility(SmartDropsConfig.SourceMode.ALL, false, false, true,
                "all protection-off natural block");
        assertSourceEligibility(SmartDropsConfig.SourceMode.ALL, false, true, true,
                "all protection-off placed block");
    }

    private static void dragonEggIsAConfigurableDefaultSafetyRule() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        truth(config.blacklist.contains("minecraft:dragon_egg"),
                "dragon egg should be in the authoritative default blacklist");

        RuleEngine.RuleInput dragonEgg = input(
                "minecraft:dragon_egg",
                false,
                false,
                DropSource.PLAYER);
        RuleEngine.Decision blocked = RuleEngine.resolve(config, dragonEgg);
        falsity(blocked.eligible(), "default dragon egg safety rule should block multiplication");
        equal(RuleEngine.Reason.FILTERED, blocked.reason(), "default dragon egg safety rule reason");

        config.blacklist.remove("minecraft:dragon_egg");
        RuleEngine.Decision allowed = RuleEngine.resolve(config, dragonEgg);
        truth(allowed.eligible(), "removing the dragon egg safety rule should restore normal eligibility");
        equal(2, allowed.multiplier(), "dragon egg should use ordinary rule resolution after removal");
    }

    private static void assertSourceEligibility(
            SmartDropsConfig.SourceMode sourceMode,
            boolean protection,
            boolean placed,
            boolean expected,
            String message
    ) {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        config.sourceMode = sourceMode;
        config.smartPlacementProtection = protection;
        RuleEngine.Decision decision = RuleEngine.resolve(config, input(placed, false, DropSource.PLAYER));
        equal(expected, decision.eligible(), message);
        if (!expected) {
            equal(
                    sourceMode == SmartDropsConfig.SourceMode.PLAYER_PLACED_ONLY
                            ? RuleEngine.Reason.NATURAL_BLOCK_EXCLUDED
                            : RuleEngine.Reason.PLAYER_PLACED_PROTECTED,
                    decision.reason(),
                    message + " reason");
        }
    }

    private static void ruleValuesRespectMaximum() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        config.maximumMultiplier = 4;
        config.blockMultipliers.put("minecraft:stone", 64);
        RuleEngine.Decision decision = RuleEngine.resolve(config, input(false, false, DropSource.PLAYER));
        equal(4, decision.multiplier(), "block rule must respect maximum");
    }

    private static void blockEntityProtectionCanBeDisabled() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        config.protectBlockEntities = false;
        RuleEngine.Decision decision = RuleEngine.resolve(config, input(false, true, DropSource.PLAYER));
        truth(decision.eligible(), "block entity protection toggle should be honored");
    }

    private static void traceExposesTheCompleteOverrideChain() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        config.globalMultiplier = 2;
        config.dimensionMultipliers.put("minecraft:overworld", 3);
        config.categoryMultipliers.put(Category.LOGS.key(), 4);
        config.blockMultipliers.put("example:redwood_log", 5);
        config.allowPlayerOverrides = true;
        config.maxPlayerMultiplier = 10;
        config.playerMultipliers.put("player-1", 6);
        RuleEngine.RuleInput input = new RuleEngine.RuleInput(
                "example:redwood_log",
                "minecraft:overworld",
                new LinkedHashSet<>(List.of(Category.LOGS)),
                false,
                false,
                Set.of(),
                DropSource.PLAYER,
                "player-1");

        RuleResolutionTrace trace = RuleEngine.trace(config, input);
        equal(2, trace.globalMultiplier(), "trace global candidate");
        equal(3, trace.dimensionOverride(), "trace dimension candidate");
        equal(Map.of(Category.LOGS, 4), trace.categoryOverrides(), "trace category candidates");
        equal(Category.LOGS, trace.categoryRuleCategory(), "trace category rule source");
        equal(4, trace.categoryOverride(), "trace category candidate");
        equal(5, trace.blockOverride(), "trace block candidate");
        equal(6, trace.storedPlayerOverride(), "trace stored player candidate");
        equal(6, trace.effectivePlayerOverride(), "trace effective player candidate");
        equal(RuleResolutionTrace.RuleSource.PLAYER_OVERRIDE, trace.selectedRule(), "trace selected player rule");
        equal(6, trace.configuredMultiplier(), "trace configured player multiplier");
        equal(trace.decision(), RuleEngine.resolve(config, input), "resolve delegates to trace decision");
    }

    private static void blockedTraceKeepsConfiguredRuleAndAppliedVanillaValue() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        config.blockMultipliers.put("minecraft:stone", 7);
        RuleResolutionTrace trace = RuleEngine.trace(config, input(true, false, DropSource.PLAYER));

        equal(RuleResolutionTrace.RuleSource.BLOCK_OVERRIDE, trace.selectedRule(), "blocked trace rule source");
        equal(7, trace.selectedRuleValue(), "blocked trace selected value");
        equal(7, trace.configuredMultiplier(), "blocked trace configured value");
        equal(1, trace.appliedMultiplier(), "blocked trace vanilla applied value");
        falsity(trace.eligible(), "blocked trace eligibility");
        equal(RuleEngine.Reason.PLAYER_PLACED_PROTECTED, trace.reason(), "blocked trace reason");
        equal(Category.STONE, trace.selectedCategory(), "blocked trace diagnostic category");
        equal(Category.MISCELLANEOUS, trace.decision().category(), "blocked gameplay category compatibility");
    }

    private static void traceFilterDiagnosticsAreExact() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        config.tagBlacklist.add("c:ores");
        config.tagWhitelist.add("example:gem_ores");
        RuleEngine.RuleInput input = new RuleEngine.RuleInput(
                "example:ruby_ore",
                "minecraft:overworld",
                new LinkedHashSet<>(List.of(Category.ORES)),
                false,
                false,
                Set.of("c:ores", "example:gem_ores", "example:unconfigured"),
                DropSource.PLAYER,
                "player-1");

        RuleResolutionTrace blacklisted = RuleEngine.trace(config, input);
        equal(Set.of("c:ores"), blacklisted.matchingBlacklistTags(), "blacklist tag diagnostics");
        equal(Set.of("example:gem_ores"), blacklisted.matchingWhitelistTags(), "inactive whitelist diagnostics");
        falsity(blacklisted.filterEligible(), "blacklist tag should reject");

        config.filterMode = SmartDropsConfig.FilterMode.WHITELIST;
        RuleResolutionTrace whitelisted = RuleEngine.trace(config, input);
        truth(whitelisted.filterEligible(), "whitelist tag should allow");
        equal(Set.of("example:gem_ores"), whitelisted.matchingWhitelistTags(), "whitelist tag diagnostics");
    }

    private static void multipleCategoryTraceHonorsInputOrder() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        config.categoryMultipliers.put(Category.LOGS.key(), 3);
        config.categoryMultipliers.put(Category.PLANTS.key(), 4);
        RuleEngine.RuleInput plantsFirst = new RuleEngine.RuleInput(
                "example:living_log",
                "minecraft:overworld",
                new LinkedHashSet<>(List.of(Category.PLANTS, Category.LOGS)),
                false,
                false,
                Set.of(),
                DropSource.PLAYER,
                "player-1");

        RuleResolutionTrace trace = RuleEngine.trace(config, plantsFirst);
        equal(List.of(Category.PLANTS, Category.LOGS), trace.matchedCategories(), "matched category order");
        equal(Category.PLANTS, trace.categoryRuleCategory(), "first category override wins");
        equal(4, trace.configuredMultiplier(), "first category override multiplier");
        equal(RuleEngine.Reason.CATEGORY_RULE, trace.reason(), "category override reason");
    }

    private static RuleEngine.RuleInput input(boolean placed, boolean blockEntity, DropSource source) {
        return input("minecraft:stone", placed, blockEntity, source);
    }

    private static RuleEngine.RuleInput input(
            String blockId,
            boolean placed,
            boolean blockEntity,
            DropSource source
    ) {
        return new RuleEngine.RuleInput(
                blockId,
                "minecraft:overworld",
                new LinkedHashSet<>(Set.of(Category.STONE)),
                placed,
                blockEntity,
                Set.of(),
                source,
                "player-1");
    }

    private static void truth(boolean value, String message) {
        assertions++;
        if (!value) {
            throw new AssertionError(message);
        }
    }

    private static void falsity(boolean value, String message) {
        truth(!value, message);
    }

    private static void equal(Object expected, Object actual, String message) {
        assertions++;
        if (!expected.equals(actual)) {
            throw new AssertionError(message + ": expected=" + expected + ", actual=" + actual);
        }
    }
}
