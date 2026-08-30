package com.chedidandrew.smartresourcedrops.core.entity;

import com.chedidandrew.smartresourcedrops.config.SmartDropsConfig;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Complete immutable explanation of an entity item/experience rule evaluation. */
public record EntityRuleTrace(
        String entityId,
        List<EntityCategory> matchedCategories,
        EntityCategory selectedCategory,
        Map<EntityCategory, Set<EntityClassification.MatchSource>> categorySources,
        Set<String> runtimeTags,
        boolean boss,
        Set<EntityClassification.MatchSource> bossSources,
        boolean miscellaneousFallback,
        String classificationReason,
        boolean permanentlyExcluded,
        String permanentExclusionReason,
        boolean modEnabled,
        boolean entityDropsEnabled,
        SmartDropsConfig.FilterMode filterMode,
        boolean exactBlacklisted,
        boolean exactWhitelisted,
        Set<String> matchingBlacklistTags,
        Set<String> matchingWhitelistTags,
        boolean filterEligible,
        SmartDropsConfig.EntityKillRequirement killRequirement,
        EntityKillAttribution.Kind attribution,
        UUID attributedPlayerId,
        boolean vanillaPlayerKilled,
        boolean killEligible,
        boolean invokingPlayerWouldQualify,
        Integer entityOverride,
        Integer categoryOverride,
        boolean inheritDefaultEntityMultiplier,
        int entityDefaultMultiplier,
        int globalMultiplier,
        int maximumMultiplier,
        RuleSource selectedRule,
        int selectedRuleValue,
        int configuredMultiplier,
        int appliedMultiplier,
        boolean bossDropsEnabled,
        boolean itemEligible,
        ItemReason itemReason,
        boolean multiplyMobExperience,
        int mobExperienceMultiplier,
        boolean multiplyBossExperience,
        boolean experienceEligible,
        ExperienceReason experienceReason,
        int appliedExperienceMultiplier
) {
    public EntityRuleTrace {
        Objects.requireNonNull(entityId, "entityId");
        Objects.requireNonNull(matchedCategories, "matchedCategories");
        Objects.requireNonNull(selectedCategory, "selectedCategory");
        Objects.requireNonNull(categorySources, "categorySources");
        Objects.requireNonNull(runtimeTags, "runtimeTags");
        Objects.requireNonNull(bossSources, "bossSources");
        Objects.requireNonNull(classificationReason, "classificationReason");
        Objects.requireNonNull(permanentExclusionReason, "permanentExclusionReason");
        Objects.requireNonNull(filterMode, "filterMode");
        Objects.requireNonNull(matchingBlacklistTags, "matchingBlacklistTags");
        Objects.requireNonNull(matchingWhitelistTags, "matchingWhitelistTags");
        Objects.requireNonNull(killRequirement, "killRequirement");
        Objects.requireNonNull(attribution, "attribution");
        Objects.requireNonNull(selectedRule, "selectedRule");
        Objects.requireNonNull(itemReason, "itemReason");
        Objects.requireNonNull(experienceReason, "experienceReason");

        matchedCategories = List.copyOf(matchedCategories);
        EnumMap<EntityCategory, Set<EntityClassification.MatchSource>> copiedSources =
                new EnumMap<>(EntityCategory.class);
        categorySources.forEach((category, sources) -> copiedSources.put(
                category,
                Collections.unmodifiableSet(new LinkedHashSet<>(sources))));
        categorySources = Collections.unmodifiableMap(copiedSources);
        runtimeTags = Collections.unmodifiableSet(new LinkedHashSet<>(runtimeTags));
        bossSources = Collections.unmodifiableSet(new LinkedHashSet<>(bossSources));
        matchingBlacklistTags = Collections.unmodifiableSet(new LinkedHashSet<>(matchingBlacklistTags));
        matchingWhitelistTags = Collections.unmodifiableSet(new LinkedHashSet<>(matchingWhitelistTags));
    }

    public EntityRuleEngine.Decision decision() {
        return new EntityRuleEngine.Decision(
                appliedMultiplier,
                itemEligible,
                itemReason,
                appliedExperienceMultiplier,
                experienceEligible,
                experienceReason,
                selectedCategory,
                boss);
    }

    public enum RuleSource {
        GLOBAL,
        ENTITY_DEFAULT,
        CATEGORY_OVERRIDE,
        ENTITY_OVERRIDE
    }

    public enum ItemReason {
        MOD_DISABLED,
        ENTITY_DROPS_DISABLED,
        PERMANENTLY_EXCLUDED,
        FILTERED,
        KILL_REQUIREMENT_NOT_MET,
        BOSS_DROPS_DISABLED,
        GLOBAL_RULE,
        ENTITY_DEFAULT_RULE,
        CATEGORY_RULE,
        ENTITY_RULE
    }

    public enum ExperienceReason {
        MOD_DISABLED,
        MOB_EXPERIENCE_DISABLED,
        PERMANENTLY_EXCLUDED,
        FILTERED,
        KILL_REQUIREMENT_NOT_MET,
        BOSS_EXPERIENCE_DISABLED,
        MOB_EXPERIENCE_RULE
    }
}
