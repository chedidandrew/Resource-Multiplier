package com.chedidandrew.smartresourcedrops.core;

import com.chedidandrew.smartresourcedrops.config.SmartDropsConfig;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable explanation of one authoritative rule evaluation. Gameplay uses
 * {@link #decision()}, while diagnostics can safely display the complete chain.
 */
public record RuleResolutionTrace(
        String blockId,
        String dimensionId,
        DropSource source,
        List<Category> matchedCategories,
        Category selectedCategory,
        boolean playerPlaced,
        boolean hasBlockEntity,
        boolean modEnabled,
        boolean sourceEnabled,
        boolean playerMiningEnabled,
        boolean explosionsEnabled,
        boolean automationEnabled,
        SmartDropsConfig.SourceMode sourceMode,
        boolean smartPlacementProtectionEnabled,
        boolean provenanceEligible,
        boolean blockEntityProtectionEnabled,
        boolean blockEntityAllowlisted,
        boolean blockEntityProtected,
        SmartDropsConfig.FilterMode filterMode,
        boolean exactBlacklisted,
        boolean exactWhitelisted,
        Set<String> matchingBlacklistTags,
        Set<String> matchingWhitelistTags,
        boolean filterEligible,
        boolean playerOverridesEnabled,
        Integer storedPlayerOverride,
        Integer effectivePlayerOverride,
        int maxPlayerMultiplier,
        Integer blockOverride,
        Map<Category, Integer> categoryOverrides,
        Category categoryRuleCategory,
        Integer categoryOverride,
        Integer dimensionOverride,
        int globalMultiplier,
        int maximumMultiplier,
        RuleSource selectedRule,
        int selectedRuleValue,
        int configuredMultiplier,
        int appliedMultiplier,
        boolean eligible,
        RuleEngine.Reason reason
) {
    public RuleResolutionTrace {
        Objects.requireNonNull(blockId, "blockId");
        Objects.requireNonNull(dimensionId, "dimensionId");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(matchedCategories, "matchedCategories");
        Objects.requireNonNull(selectedCategory, "selectedCategory");
        Objects.requireNonNull(sourceMode, "sourceMode");
        Objects.requireNonNull(filterMode, "filterMode");
        Objects.requireNonNull(matchingBlacklistTags, "matchingBlacklistTags");
        Objects.requireNonNull(matchingWhitelistTags, "matchingWhitelistTags");
        Objects.requireNonNull(categoryOverrides, "categoryOverrides");
        Objects.requireNonNull(selectedRule, "selectedRule");
        Objects.requireNonNull(reason, "reason");

        matchedCategories = List.copyOf(matchedCategories);
        matchingBlacklistTags = Collections.unmodifiableSet(new LinkedHashSet<>(matchingBlacklistTags));
        matchingWhitelistTags = Collections.unmodifiableSet(new LinkedHashSet<>(matchingWhitelistTags));
        categoryOverrides = Collections.unmodifiableMap(new LinkedHashMap<>(categoryOverrides));
    }

    /** Returns the exact compact decision consumed by the drop pipeline. */
    public RuleEngine.Decision decision() {
        Category gameplayCategory = eligible ? selectedCategory : Category.MISCELLANEOUS;
        return new RuleEngine.Decision(appliedMultiplier, eligible, reason, gameplayCategory);
    }

    public enum RuleSource {
        GLOBAL,
        DIMENSION_OVERRIDE,
        CATEGORY_OVERRIDE,
        BLOCK_OVERRIDE,
        PLAYER_OVERRIDE
    }
}
