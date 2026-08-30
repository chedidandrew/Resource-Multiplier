package com.chedidandrew.smartresourcedrops.config;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Bounded set of explicitly edited operator settings. This deliberately omits
 * per-player rules and unrelated server configuration.
 */
public final class ConfigPatch {
    public static final int MAX_COLLECTION_EDITS = SmartDropsConfig.MAX_TOTAL_RULE_ENTRIES;
    public static final int MAX_JSON_LENGTH = 1_048_576;

    public SmartDropsConfig.Preset preset;
    public Boolean enabled;
    public Integer globalMultiplier;
    public Boolean smartPlacementProtection;
    public Boolean protectBlockEntities;
    public Boolean playerMining;
    public Boolean explosions;
    public Boolean automatedMining;
    public Boolean multiplyExperience;
    public Integer experienceMultiplier;
    public Boolean conservativePistonProtection;
    public Boolean allowPlayerOverrides;
    public Boolean statisticsEnabled;
    public SmartDropsConfig.FilterMode filterMode;
    public SmartDropsConfig.SourceMode sourceMode;
    public Boolean entityDropsEnabled;
    public Boolean inheritDefaultEntityMultiplier;
    public Integer defaultEntityMultiplier;
    public SmartDropsConfig.EntityKillRequirement entityKillRequirement;
    public SmartDropsConfig.FilterMode entityFilterMode;
    public Boolean bossDropsEnabled;
    public Boolean multiplyMobExperience;
    public Integer mobExperienceMultiplier;
    public Boolean multiplyBossExperience;
    public Boolean manualShearingDropsEnabled;
    public Boolean automatedShearingDropsEnabled;
    public Boolean inheritDefaultShearingMultiplier;
    public Integer defaultShearingMultiplier;

    public Map<String, Integer> blockMultipliers = new LinkedHashMap<>();
    public Set<String> inheritedBlocks = new LinkedHashSet<>();
    public Map<String, Integer> categoryMultipliers = new LinkedHashMap<>();
    public Set<String> inheritedCategories = new LinkedHashSet<>();
    public Map<String, Integer> dimensionMultipliers = new LinkedHashMap<>();
    public Set<String> inheritedDimensions = new LinkedHashSet<>();
    public Map<String, FilterEntryState> blockFilters = new LinkedHashMap<>();
    public Map<String, Integer> entityMultipliers = new LinkedHashMap<>();
    public Set<String> inheritedEntities = new LinkedHashSet<>();
    public Map<String, Integer> entityCategoryMultipliers = new LinkedHashMap<>();
    public Set<String> inheritedEntityCategories = new LinkedHashSet<>();
    public Map<String, FilterEntryState> entityFilters = new LinkedHashMap<>();
    public Map<String, FilterEntryState> entityTagFilters = new LinkedHashMap<>();
    public Map<String, Integer> shearingEntityMultipliers = new LinkedHashMap<>();
    public Set<String> inheritedShearingEntities = new LinkedHashSet<>();

    public boolean isEmpty() {
        return preset == null
                && enabled == null
                && globalMultiplier == null
                && smartPlacementProtection == null
                && protectBlockEntities == null
                && playerMining == null
                && explosions == null
                && automatedMining == null
                && multiplyExperience == null
                && experienceMultiplier == null
                && conservativePistonProtection == null
                && allowPlayerOverrides == null
                && statisticsEnabled == null
                && filterMode == null
                && sourceMode == null
                && entityDropsEnabled == null
                && inheritDefaultEntityMultiplier == null
                && defaultEntityMultiplier == null
                && entityKillRequirement == null
                && entityFilterMode == null
                && bossDropsEnabled == null
                && multiplyMobExperience == null
                && mobExperienceMultiplier == null
                && multiplyBossExperience == null
                && manualShearingDropsEnabled == null
                && automatedShearingDropsEnabled == null
                && inheritDefaultShearingMultiplier == null
                && defaultShearingMultiplier == null
                && blockMultipliers.isEmpty()
                && inheritedBlocks.isEmpty()
                && categoryMultipliers.isEmpty()
                && inheritedCategories.isEmpty()
                && dimensionMultipliers.isEmpty()
                && inheritedDimensions.isEmpty()
                && blockFilters.isEmpty()
                && entityMultipliers.isEmpty()
                && inheritedEntities.isEmpty()
                && entityCategoryMultipliers.isEmpty()
                && inheritedEntityCategories.isEmpty()
                && entityFilters.isEmpty()
                && entityTagFilters.isEmpty()
                && shearingEntityMultipliers.isEmpty()
                && inheritedShearingEntities.isEmpty();
    }

    public boolean hasValidShape() {
        if (blockMultipliers == null
                || inheritedBlocks == null
                || categoryMultipliers == null
                || inheritedCategories == null
                || dimensionMultipliers == null
                || inheritedDimensions == null
                || blockFilters == null
                || entityMultipliers == null
                || inheritedEntities == null
                || entityCategoryMultipliers == null
                || inheritedEntityCategories == null
                || entityFilters == null
                || entityTagFilters == null
                || shearingEntityMultipliers == null
                || inheritedShearingEntities == null) {
            return false;
        }
        long edits = (long) blockMultipliers.size()
                + inheritedBlocks.size()
                + categoryMultipliers.size()
                + inheritedCategories.size()
                + dimensionMultipliers.size()
                + inheritedDimensions.size()
                + blockFilters.size()
                + entityMultipliers.size()
                + inheritedEntities.size()
                + entityCategoryMultipliers.size()
                + inheritedEntityCategories.size()
                + entityFilters.size()
                + entityTagFilters.size()
                + shearingEntityMultipliers.size()
                + inheritedShearingEntities.size();
        return edits <= MAX_COLLECTION_EDITS
                && shearingEntityMultipliers.size() <= SmartDropsConfig.MAX_SHEARING_RULE_ENTRIES
                && inheritedShearingEntities.size() <= SmartDropsConfig.MAX_SHEARING_RULE_ENTRIES
                && noNullEntries(blockMultipliers)
                && noNullEntries(categoryMultipliers)
                && noNullEntries(dimensionMultipliers)
                && noNullEntries(blockFilters)
                && noNullEntries(entityMultipliers)
                && noNullEntries(entityCategoryMultipliers)
                && noNullEntries(entityFilters)
                && noNullEntries(entityTagFilters)
                && noNullEntries(shearingEntityMultipliers)
                && inheritedBlocks.stream().noneMatch(java.util.Objects::isNull)
                && inheritedCategories.stream().noneMatch(java.util.Objects::isNull)
                && inheritedDimensions.stream().noneMatch(java.util.Objects::isNull)
                && inheritedEntities.stream().noneMatch(java.util.Objects::isNull)
                && inheritedEntityCategories.stream().noneMatch(java.util.Objects::isNull)
                && inheritedShearingEntities.stream().noneMatch(java.util.Objects::isNull);
    }

    /** Server-side range validation; malformed values are rejected, never silently clamped. */
    public boolean hasValuesWithinBounds(final int maximumMultiplier) {
        return inRange(globalMultiplier, 0, maximumMultiplier)
                && inRange(experienceMultiplier, 1, maximumMultiplier)
                && inRange(defaultEntityMultiplier, 0, maximumMultiplier)
                && inRange(mobExperienceMultiplier, 1, maximumMultiplier)
                && inRange(defaultShearingMultiplier, 0, maximumMultiplier)
                && multiplierValuesInRange(blockMultipliers, maximumMultiplier)
                && multiplierValuesInRange(categoryMultipliers, maximumMultiplier)
                && multiplierValuesInRange(dimensionMultipliers, maximumMultiplier)
                && multiplierValuesInRange(entityMultipliers, maximumMultiplier)
                && multiplierValuesInRange(entityCategoryMultipliers, maximumMultiplier)
                && multiplierValuesInRange(shearingEntityMultipliers, maximumMultiplier);
    }

    private static boolean inRange(final Integer value, final int minimum, final int maximum) {
        return value == null || value >= minimum && value <= maximum;
    }

    private static boolean multiplierValuesInRange(
            final Map<String, Integer> values,
            final int maximumMultiplier
    ) {
        return values != null && values.values().stream()
                .allMatch(value -> value != null && value >= 0 && value <= maximumMultiplier);
    }

    private static boolean noNullEntries(Map<?, ?> values) {
        return values.entrySet().stream().noneMatch(entry -> entry.getKey() == null || entry.getValue() == null);
    }

    public enum FilterEntryState {
        NONE,
        WHITELIST,
        BLACKLIST
    }
}
