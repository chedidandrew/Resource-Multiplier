package com.chedidandrew.smartresourcedrops.core.entity;

import com.chedidandrew.smartresourcedrops.config.SmartDropsConfig;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EntityRuleEngineTest {
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void entityFeatureDefaultsAreVanillaPassThrough() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        EntityRuleTrace trace = EntityRuleEngine.trace(config, input(
                "minecraft:zombie",
                EntityCategory.HOSTILE,
                EntityKillAttribution.direct(PLAYER, true)));

        assertFalse(trace.itemEligible());
        assertEquals(EntityRuleTrace.ItemReason.ENTITY_DROPS_DISABLED, trace.itemReason());
        assertEquals(1, trace.appliedMultiplier());
        assertFalse(trace.experienceEligible());
        assertEquals(EntityRuleTrace.ExperienceReason.MOB_EXPERIENCE_DISABLED, trace.experienceReason());
        assertEquals(1, trace.appliedExperienceMultiplier());
    }

    @Test
    void exactRuleOverridesSelectedCategoryWhichOverridesEntityDefault() {
        SmartDropsConfig config = enabledConfig();
        config.inheritDefaultEntityMultiplier = false;
        config.defaultEntityMultiplier = 3;
        config.entityCategoryMultipliers.put(EntityCategory.HOSTILE.key(), 5);
        config.entityMultipliers.put("minecraft:zombie", 7);

        EntityRuleTrace exact = EntityRuleEngine.trace(config, input(
                "minecraft:zombie", EntityCategory.HOSTILE, EntityKillAttribution.direct(PLAYER, true)));
        assertEquals(7, exact.appliedMultiplier());
        assertEquals(EntityRuleTrace.RuleSource.ENTITY_OVERRIDE, exact.selectedRule());
        assertEquals(5, exact.categoryOverride());
        assertEquals(7, exact.entityOverride());

        config.entityMultipliers.clear();
        EntityRuleTrace category = EntityRuleEngine.trace(config, input(
                "minecraft:zombie", EntityCategory.HOSTILE, EntityKillAttribution.direct(PLAYER, true)));
        assertEquals(5, category.appliedMultiplier());
        assertEquals(EntityRuleTrace.RuleSource.CATEGORY_OVERRIDE, category.selectedRule());

        config.entityCategoryMultipliers.clear();
        EntityRuleTrace defaultRule = EntityRuleEngine.trace(config, input(
                "minecraft:zombie", EntityCategory.HOSTILE, EntityKillAttribution.direct(PLAYER, true)));
        assertEquals(3, defaultRule.appliedMultiplier());
        assertEquals(EntityRuleTrace.RuleSource.ENTITY_DEFAULT, defaultRule.selectedRule());
    }

    @Test
    void entityFiltersAreIndependentAndSupportExactAndTagRules() {
        SmartDropsConfig config = enabledConfig();
        config.entityBlacklist.add("minecraft:zombie");
        EntityRuleTrace exactBlocked = EntityRuleEngine.trace(config, input(
                "minecraft:zombie", EntityCategory.HOSTILE, EntityKillAttribution.direct(PLAYER, true)));
        assertFalse(exactBlocked.filterEligible());
        assertTrue(exactBlocked.exactBlacklisted());

        config.entityBlacklist.clear();
        config.entityTagBlacklist.add("minecraft:undead");
        EntityRuleTrace tagBlocked = EntityRuleEngine.trace(config, input(
                "minecraft:zombie",
                EntityCategory.HOSTILE,
                EntityKillAttribution.direct(PLAYER, true),
                Set.of("minecraft:undead")));
        assertFalse(tagBlocked.filterEligible());
        assertEquals(Set.of("minecraft:undead"), tagBlocked.matchingBlacklistTags());

        config.entityFilterMode = SmartDropsConfig.FilterMode.WHITELIST;
        config.entityTagBlacklist.clear();
        config.entityTagWhitelist.add("minecraft:undead");
        EntityRuleTrace tagAllowed = EntityRuleEngine.trace(config, input(
                "minecraft:zombie",
                EntityCategory.HOSTILE,
                EntityKillAttribution.direct(PLAYER, true),
                Set.of("minecraft:undead")));
        assertTrue(tagAllowed.filterEligible());
        assertTrue(tagAllowed.itemEligible());
    }

    @Test
    void directKillsRequireVanillaCreditWhileSafelyResolvedTamedKillsCanQualify() {
        SmartDropsConfig config = enabledConfig();
        EntityRuleTrace craftedDirect = EntityRuleEngine.trace(config, input(
                "minecraft:cow", EntityCategory.PASSIVE, EntityKillAttribution.direct(PLAYER, false)));
        assertFalse(craftedDirect.killEligible());
        assertEquals(EntityRuleTrace.ItemReason.KILL_REQUIREMENT_NOT_MET, craftedDirect.itemReason());

        config.entityKillRequirement = SmartDropsConfig.EntityKillRequirement.PLAYER_OR_TAMED_ENTITY;
        EntityRuleTrace tamed = EntityRuleEngine.trace(config, input(
                "minecraft:cow", EntityCategory.PASSIVE, EntityKillAttribution.tamed(PLAYER, false)));
        assertTrue(tamed.killEligible());
        assertTrue(tamed.itemEligible());

        config.entityKillRequirement = SmartDropsConfig.EntityKillRequirement.ALL_STANDARD_DEATH_LOOT;
        EntityRuleTrace unattributed = EntityRuleEngine.trace(config, input(
                "minecraft:cow", EntityCategory.PASSIVE, EntityKillAttribution.none(false)));
        assertTrue(unattributed.killEligible());
        assertTrue(unattributed.itemEligible());
    }

    @Test
    void bossItemAndExperienceGatesAreSeparate() {
        SmartDropsConfig config = enabledConfig();
        config.multiplyMobExperience = true;
        config.mobExperienceMultiplier = 4;
        EntityRuleTrace blocked = EntityRuleEngine.trace(config, input(
                "minecraft:warden", EntityCategory.BOSSES, EntityKillAttribution.direct(PLAYER, true)));
        assertFalse(blocked.itemEligible());
        assertEquals(EntityRuleTrace.ItemReason.BOSS_DROPS_DISABLED, blocked.itemReason());
        assertFalse(blocked.experienceEligible());
        assertEquals(EntityRuleTrace.ExperienceReason.BOSS_EXPERIENCE_DISABLED, blocked.experienceReason());

        config.bossDropsEnabled = true;
        EntityRuleTrace itemOnly = EntityRuleEngine.trace(config, input(
                "minecraft:warden", EntityCategory.BOSSES, EntityKillAttribution.direct(PLAYER, true)));
        assertTrue(itemOnly.itemEligible());
        assertFalse(itemOnly.experienceEligible());

        config.multiplyBossExperience = true;
        EntityRuleTrace both = EntityRuleEngine.trace(config, input(
                "minecraft:warden", EntityCategory.BOSSES, EntityKillAttribution.direct(PLAYER, true)));
        assertTrue(both.itemEligible());
        assertTrue(both.experienceEligible());
        assertEquals(4, both.appliedExperienceMultiplier());
    }

    @Test
    void permanentExclusionWinsAndTraceDecisionMatchesResolve() {
        SmartDropsConfig config = enabledConfig();
        EntityRuleInput input = input(
                "minecraft:player",
                EntityCategory.MISCELLANEOUS,
                EntityKillAttribution.direct(PLAYER, true),
                Set.of(),
                true,
                "players are excluded");
        EntityRuleTrace trace = EntityRuleEngine.trace(config, input);

        assertFalse(trace.itemEligible());
        assertEquals(EntityRuleTrace.ItemReason.PERMANENTLY_EXCLUDED, trace.itemReason());
        assertEquals(trace.decision(), EntityRuleEngine.resolve(config, input));
    }

    private static SmartDropsConfig enabledConfig() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        config.entityDropsEnabled = true;
        config.entityCategoryMultipliers.clear();
        return config;
    }

    private static EntityRuleInput input(
            String id,
            EntityCategory category,
            EntityKillAttribution attribution
    ) {
        return input(id, category, attribution, Set.of());
    }

    private static EntityRuleInput input(
            String id,
            EntityCategory category,
            EntityKillAttribution attribution,
            Set<String> tags
    ) {
        return input(id, category, attribution, tags, false, "none");
    }

    private static EntityRuleInput input(
            String id,
            EntityCategory category,
            EntityKillAttribution attribution,
            Set<String> tags,
            boolean permanentlyExcluded,
            String exclusionReason
    ) {
        Map<EntityCategory, Set<EntityClassification.MatchSource>> sources = new EnumMap<>(EntityCategory.class);
        sources.put(category, Set.of(EntityClassification.MatchSource.VANILLA_CLASS));
        EntityClassification classification = new EntityClassification(
                id,
                List.of(category),
                category,
                sources,
                tags,
                category == EntityCategory.BOSSES,
                category == EntityCategory.BOSSES
                        ? Set.of(EntityClassification.MatchSource.KNOWN_VANILLA_TYPE)
                        : Set.of(),
                category == EntityCategory.MISCELLANEOUS,
                "test classification");
        return new EntityRuleInput(
                id,
                classification,
                tags,
                attribution,
                permanentlyExcluded,
                exclusionReason,
                attribution.kind() == EntityKillAttribution.Kind.DIRECT_PLAYER);
    }
}
