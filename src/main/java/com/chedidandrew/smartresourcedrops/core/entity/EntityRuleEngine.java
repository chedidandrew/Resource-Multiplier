package com.chedidandrew.smartresourcedrops.core.entity;

import com.chedidandrew.smartresourcedrops.config.SmartDropsConfig;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/** Authoritative, Minecraft-independent entity item and experience policy. */
public final class EntityRuleEngine {
    private EntityRuleEngine() {
    }

    public static Decision resolve(SmartDropsConfig config, EntityRuleInput input) {
        return trace(config, input).decision();
    }

    public static EntityRuleTrace trace(SmartDropsConfig config, EntityRuleInput input) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(input, "input");

        EntityClassification classification = input.classification();
        boolean exactBlacklisted = config.entityBlacklist.contains(input.entityId());
        boolean exactWhitelisted = config.entityWhitelist.contains(input.entityId());
        Set<String> matchingBlacklistTags = intersection(
                input.matchedFilterTags(),
                config.entityTagBlacklist);
        Set<String> matchingWhitelistTags = intersection(
                input.matchedFilterTags(),
                config.entityTagWhitelist);
        boolean filterEligible = switch (config.entityFilterMode) {
            case BLACKLIST -> !exactBlacklisted && matchingBlacklistTags.isEmpty();
            case WHITELIST -> exactWhitelisted || !matchingWhitelistTags.isEmpty();
        };
        boolean killEligible = killEligible(config.entityKillRequirement, input.attribution());

        int ruleValue = config.inheritDefaultEntityMultiplier
                ? config.globalMultiplier
                : config.defaultEntityMultiplier;
        EntityRuleTrace.RuleSource selectedRule = config.inheritDefaultEntityMultiplier
                ? EntityRuleTrace.RuleSource.GLOBAL
                : EntityRuleTrace.RuleSource.ENTITY_DEFAULT;

        Integer categoryOverride = config.entityCategoryMultipliers.get(classification.selectedCategory().key());
        if (categoryOverride != null) {
            ruleValue = categoryOverride;
            selectedRule = EntityRuleTrace.RuleSource.CATEGORY_OVERRIDE;
        }

        Integer entityOverride = config.entityMultipliers.get(input.entityId());
        if (entityOverride != null) {
            ruleValue = entityOverride;
            selectedRule = EntityRuleTrace.RuleSource.ENTITY_OVERRIDE;
        }

        int configuredMultiplier = SmartDropsConfig.clamp(ruleValue, 0, config.maximumMultiplier);
        EntityRuleTrace.ItemReason itemReason = itemRuleReason(selectedRule);
        boolean itemEligible = true;
        if (!config.enabled) {
            itemEligible = false;
            itemReason = EntityRuleTrace.ItemReason.MOD_DISABLED;
        } else if (!config.entityDropsEnabled) {
            itemEligible = false;
            itemReason = EntityRuleTrace.ItemReason.ENTITY_DROPS_DISABLED;
        } else if (input.permanentlyExcluded()) {
            itemEligible = false;
            itemReason = EntityRuleTrace.ItemReason.PERMANENTLY_EXCLUDED;
        } else if (!filterEligible) {
            itemEligible = false;
            itemReason = EntityRuleTrace.ItemReason.FILTERED;
        } else if (!killEligible) {
            itemEligible = false;
            itemReason = EntityRuleTrace.ItemReason.KILL_REQUIREMENT_NOT_MET;
        } else if (classification.boss() && !config.bossDropsEnabled) {
            itemEligible = false;
            itemReason = EntityRuleTrace.ItemReason.BOSS_DROPS_DISABLED;
        }

        EntityRuleTrace.ExperienceReason experienceReason = EntityRuleTrace.ExperienceReason.MOB_EXPERIENCE_RULE;
        boolean experienceEligible = true;
        if (!config.enabled) {
            experienceEligible = false;
            experienceReason = EntityRuleTrace.ExperienceReason.MOD_DISABLED;
        } else if (!config.multiplyMobExperience) {
            experienceEligible = false;
            experienceReason = EntityRuleTrace.ExperienceReason.MOB_EXPERIENCE_DISABLED;
        } else if (input.permanentlyExcluded()) {
            experienceEligible = false;
            experienceReason = EntityRuleTrace.ExperienceReason.PERMANENTLY_EXCLUDED;
        } else if (!filterEligible) {
            experienceEligible = false;
            experienceReason = EntityRuleTrace.ExperienceReason.FILTERED;
        } else if (!killEligible) {
            experienceEligible = false;
            experienceReason = EntityRuleTrace.ExperienceReason.KILL_REQUIREMENT_NOT_MET;
        } else if (classification.boss() && !config.multiplyBossExperience) {
            experienceEligible = false;
            experienceReason = EntityRuleTrace.ExperienceReason.BOSS_EXPERIENCE_DISABLED;
        }

        int appliedMultiplier = itemEligible ? configuredMultiplier : 1;
        int appliedExperienceMultiplier = experienceEligible
                ? SmartDropsConfig.clamp(config.mobExperienceMultiplier, 1, config.maximumMultiplier)
                : 1;

        return new EntityRuleTrace(
                input.entityId(),
                classification.matchedCategories(),
                classification.selectedCategory(),
                classification.categorySources(),
                classification.runtimeTags(),
                classification.boss(),
                classification.bossSources(),
                classification.miscellaneousFallback(),
                classification.reason(),
                input.permanentlyExcluded(),
                input.permanentExclusionReason(),
                config.enabled,
                config.entityDropsEnabled,
                config.entityFilterMode,
                exactBlacklisted,
                exactWhitelisted,
                matchingBlacklistTags,
                matchingWhitelistTags,
                filterEligible,
                config.entityKillRequirement,
                input.attribution().kind(),
                input.attribution().playerId(),
                input.attribution().vanillaPlayerKilled(),
                killEligible,
                input.invokingPlayerWouldQualify(),
                entityOverride,
                categoryOverride,
                config.inheritDefaultEntityMultiplier,
                config.defaultEntityMultiplier,
                config.globalMultiplier,
                config.maximumMultiplier,
                selectedRule,
                ruleValue,
                configuredMultiplier,
                appliedMultiplier,
                config.bossDropsEnabled,
                itemEligible,
                itemReason,
                config.multiplyMobExperience,
                config.mobExperienceMultiplier,
                config.multiplyBossExperience,
                experienceEligible,
                experienceReason,
                appliedExperienceMultiplier);
    }

    private static boolean killEligible(
            SmartDropsConfig.EntityKillRequirement requirement,
            EntityKillAttribution attribution
    ) {
        return switch (requirement) {
            case PLAYER_KILLS_ONLY -> attribution.kind() == EntityKillAttribution.Kind.DIRECT_PLAYER
                    && attribution.vanillaPlayerKilled();
            case PLAYER_OR_TAMED_ENTITY -> attribution.kind() == EntityKillAttribution.Kind.TAMED_ENTITY
                    || attribution.kind() == EntityKillAttribution.Kind.DIRECT_PLAYER
                    && attribution.vanillaPlayerKilled();
            case ALL_STANDARD_DEATH_LOOT -> true;
        };
    }

    private static EntityRuleTrace.ItemReason itemRuleReason(EntityRuleTrace.RuleSource source) {
        return switch (source) {
            case GLOBAL -> EntityRuleTrace.ItemReason.GLOBAL_RULE;
            case ENTITY_DEFAULT -> EntityRuleTrace.ItemReason.ENTITY_DEFAULT_RULE;
            case CATEGORY_OVERRIDE -> EntityRuleTrace.ItemReason.CATEGORY_RULE;
            case ENTITY_OVERRIDE -> EntityRuleTrace.ItemReason.ENTITY_RULE;
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

    public record Decision(
            int itemMultiplier,
            boolean itemEligible,
            EntityRuleTrace.ItemReason itemReason,
            int experienceMultiplier,
            boolean experienceEligible,
            EntityRuleTrace.ExperienceReason experienceReason,
            EntityCategory category,
            boolean boss
    ) {
    }
}
