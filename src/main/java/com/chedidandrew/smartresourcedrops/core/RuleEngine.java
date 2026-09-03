package com.chedidandrew.smartresourcedrops.core;

import com.chedidandrew.smartresourcedrops.config.SmartDropsConfig;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Locale;
import java.util.Set;

public final class RuleEngine {
    private RuleEngine() {
    }

    public static Decision resolve(SmartDropsConfig config, RuleInput input) {
        return trace(config, input).decision();
    }

    /**
     * Evaluates the same policy used by gameplay while retaining every relevant
     * intermediate value for read-only diagnostics.
     */
    public static RuleResolutionTrace trace(SmartDropsConfig config, RuleInput input) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(input, "input");

        List<Category> matchedCategories = List.copyOf(input.categories());
        boolean sourceEnabled = sourceEnabled(config, input.source());
        boolean blockEntityAllowlisted = config.blockEntityAllowlist.contains(input.blockId());
        boolean blockEntityProtected = config.protectBlockEntities
                && input.hasBlockEntity()
                && !blockEntityAllowlisted;

        boolean exactBlacklisted = config.blacklist.contains(input.blockId());
        boolean exactWhitelisted = config.whitelist.contains(input.blockId());
        Set<String> matchingBlacklistTags = intersection(input.matchedFilterTags(), config.tagBlacklist);
        Set<String> matchingWhitelistTags = intersection(input.matchedFilterTags(), config.tagWhitelist);
        boolean filterEligible = switch (config.filterMode) {
            case BLACKLIST -> !exactBlacklisted && matchingBlacklistTags.isEmpty();
            case WHITELIST -> exactWhitelisted || !matchingWhitelistTags.isEmpty();
        };

        boolean provenanceEligible = !(config.sourceMode == SmartDropsConfig.SourceMode.PLAYER_PLACED_ONLY
                && !input.playerPlaced())
                && !(config.sourceMode == SmartDropsConfig.SourceMode.NATURAL_ONLY
                && config.smartPlacementProtection
                && input.playerPlaced());

        Category category = matchedCategories.get(0);
        int multiplier = config.globalMultiplier;
        RuleResolutionTrace.RuleSource selectedRule = RuleResolutionTrace.RuleSource.GLOBAL;

        Integer dimensionRule = config.dimensionMultipliers.get(input.dimensionId());
        if (dimensionRule != null) {
            multiplier = dimensionRule;
            selectedRule = RuleResolutionTrace.RuleSource.DIMENSION_OVERRIDE;
        }

        Map<Category, Integer> categoryOverrides = new LinkedHashMap<>();
        for (Category candidate : matchedCategories) {
            Integer categoryRule = config.categoryMultipliers.get(candidate.key());
            if (categoryRule != null) {
                categoryOverrides.put(candidate, categoryRule);
            }
        }

        Category categoryRuleCategory = null;
        Integer categoryRuleValue = null;
        for (Category candidate : matchedCategories) {
            Integer categoryRule = categoryOverrides.get(candidate);
            if (categoryRule != null) {
                multiplier = categoryRule;
                category = candidate;
                categoryRuleCategory = candidate;
                categoryRuleValue = categoryRule;
                selectedRule = RuleResolutionTrace.RuleSource.CATEGORY_OVERRIDE;
                break;
            }
        }

        Integer blockRule = config.blockMultipliers.get(input.blockId());
        if (blockRule != null) {
            multiplier = blockRule;
            selectedRule = RuleResolutionTrace.RuleSource.BLOCK_OVERRIDE;
        }

        Integer storedPlayerRule = input.playerId() == null
                ? null
                : config.playerMultipliers.get(input.playerId());
        Integer effectivePlayerRule = null;
        if (config.allowPlayerOverrides && input.playerId() != null && input.source() == DropSource.PLAYER) {
            if (storedPlayerRule != null) {
                effectivePlayerRule = Math.min(storedPlayerRule, config.maxPlayerMultiplier);
                multiplier = effectivePlayerRule;
                selectedRule = RuleResolutionTrace.RuleSource.PLAYER_OVERRIDE;
            }
        }

        int selectedRuleValue = multiplier;
        int configuredMultiplier = SmartDropsConfig.clamp(multiplier, 0, config.maximumMultiplier);

        boolean eligible = true;
        Reason reason = reasonFor(selectedRule);
        if (!config.enabled) {
            eligible = false;
            reason = Reason.MOD_DISABLED;
        } else if (!sourceEnabled) {
            eligible = false;
            reason = Reason.SOURCE_DISABLED;
        } else if (blockEntityProtected) {
            eligible = false;
            reason = Reason.BLOCK_ENTITY_PROTECTED;
        } else if (!filterEligible) {
            eligible = false;
            reason = Reason.FILTERED;
        } else if (config.sourceMode == SmartDropsConfig.SourceMode.PLAYER_PLACED_ONLY
                && !input.playerPlaced()) {
            eligible = false;
            reason = Reason.NATURAL_BLOCK_EXCLUDED;
        } else if (config.sourceMode == SmartDropsConfig.SourceMode.NATURAL_ONLY
                && config.smartPlacementProtection
                && input.playerPlaced()) {
            eligible = false;
            reason = Reason.PLAYER_PLACED_PROTECTED;
        }

        int appliedMultiplier = eligible ? configuredMultiplier : 1;
        return new RuleResolutionTrace(
                input.blockId(),
                input.dimensionId(),
                input.source(),
                matchedCategories,
                category,
                input.playerPlaced(),
                input.hasBlockEntity(),
                config.enabled,
                sourceEnabled,
                config.playerMining,
                config.explosions,
                config.automatedMining,
                config.sourceMode,
                config.smartPlacementProtection,
                provenanceEligible,
                config.protectBlockEntities,
                blockEntityAllowlisted,
                blockEntityProtected,
                config.filterMode,
                exactBlacklisted,
                exactWhitelisted,
                matchingBlacklistTags,
                matchingWhitelistTags,
                filterEligible,
                config.allowPlayerOverrides,
                storedPlayerRule,
                effectivePlayerRule,
                config.maxPlayerMultiplier,
                blockRule,
                categoryOverrides,
                categoryRuleCategory,
                categoryRuleValue,
                dimensionRule,
                config.globalMultiplier,
                config.maximumMultiplier,
                selectedRule,
                selectedRuleValue,
                configuredMultiplier,
                appliedMultiplier,
                eligible,
                reason);
    }

    private static boolean sourceEnabled(SmartDropsConfig config, DropSource source) {
        return switch (source) {
            case PLAYER -> config.playerMining;
            case EXPLOSION -> config.explosions;
            case AUTOMATION -> config.automatedMining;
        };
    }

    private static Reason reasonFor(RuleResolutionTrace.RuleSource source) {
        return switch (source) {
            case GLOBAL -> Reason.GLOBAL_RULE;
            case DIMENSION_OVERRIDE -> Reason.DIMENSION_RULE;
            case CATEGORY_OVERRIDE -> Reason.CATEGORY_RULE;
            case BLOCK_OVERRIDE -> Reason.BLOCK_RULE;
            case PLAYER_OVERRIDE -> Reason.PLAYER_OVERRIDE;
        };
    }

    private static Set<String> intersection(Set<String> left, Set<String> right) {
        LinkedHashSet<String> matches = new LinkedHashSet<>();
        for (String value : left) {
            if (right.contains(value)) {
                matches.add(value);
            }
        }
        return matches;
    }

    public record RuleInput(
            String blockId,
            String dimensionId,
            LinkedHashSet<Category> categories,
            boolean playerPlaced,
            boolean hasBlockEntity,
            Set<String> matchedFilterTags,
            DropSource source,
            String playerId
    ) {
        public RuleInput {
            Objects.requireNonNull(blockId, "blockId");
            Objects.requireNonNull(dimensionId, "dimensionId");
            Objects.requireNonNull(categories, "categories");
            Objects.requireNonNull(matchedFilterTags, "matchedFilterTags");
            Objects.requireNonNull(source, "source");
            blockId = blockId.toLowerCase(Locale.ROOT);
            dimensionId = dimensionId.toLowerCase(Locale.ROOT);
            playerId = playerId == null ? null : playerId.toLowerCase(Locale.ROOT);
            categories = categories.isEmpty()
                    ? new LinkedHashSet<>(Set.of(Category.MISCELLANEOUS))
                    : new LinkedHashSet<>(categories);
            matchedFilterTags = Collections.unmodifiableSet(new LinkedHashSet<>(matchedFilterTags));
        }
    }

    public record Decision(int multiplier, boolean eligible, Reason reason, Category category) {
        public static Decision blocked(Reason reason) {
            return new Decision(1, false, reason, Category.MISCELLANEOUS);
        }
    }

    public enum Reason {
        MOD_DISABLED,
        SOURCE_DISABLED,
        BLOCK_ENTITY_PROTECTED,
        FILTERED,
        PLAYER_PLACED_PROTECTED,
        NATURAL_BLOCK_EXCLUDED,
        GLOBAL_RULE,
        DIMENSION_RULE,
        CATEGORY_RULE,
        BLOCK_RULE,
        PLAYER_OVERRIDE
    }
}
