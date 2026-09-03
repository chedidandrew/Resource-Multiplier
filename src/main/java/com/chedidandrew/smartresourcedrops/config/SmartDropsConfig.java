package com.chedidandrew.smartresourcedrops.config;

import com.chedidandrew.smartresourcedrops.core.Category;
import com.chedidandrew.smartresourcedrops.core.entity.EntityCategory;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public final class SmartDropsConfig {
    public static final int CURRENT_SCHEMA = 3;
    public static final int ABSOLUTE_MAX_MULTIPLIER = 64;
    public static final int MAX_BLOCK_RULE_ENTRIES = 2048;
    public static final int MAX_ENTITY_RULE_ENTRIES = 512;
    public static final int MAX_SHEARING_RULE_ENTRIES = 256;
    public static final int MAX_TOTAL_RULE_ENTRIES =
            MAX_BLOCK_RULE_ENTRIES + MAX_ENTITY_RULE_ENTRIES + MAX_SHEARING_RULE_ENTRIES;
    public static final int MAX_RULE_KEY_LENGTH = 256;

    public int schemaVersion = CURRENT_SCHEMA;
    public boolean enabled = true;
    public int globalMultiplier = 2;
    public int maximumMultiplier = ABSOLUTE_MAX_MULTIPLIER;
    public SourceMode sourceMode = SourceMode.NATURAL_ONLY;
    public FilterMode filterMode = FilterMode.BLACKLIST;
    public boolean smartPlacementProtection = true;
    public boolean protectBlockEntities = true;
    public boolean playerMining = true;
    public boolean explosions = true;
    public boolean automatedMining = false;
    public boolean multiplyExperience = false;
    public int experienceMultiplier = 2;
    public boolean conservativePistonProtection = true;
    public boolean allowPlayerOverrides = false;
    public int maxPlayerMultiplier = 4;
    public boolean statisticsEnabled = false;

    public boolean entityDropsEnabled = false;
    public boolean inheritDefaultEntityMultiplier = true;
    public int defaultEntityMultiplier = 2;
    public EntityKillRequirement entityKillRequirement = EntityKillRequirement.PLAYER_KILLS_ONLY;
    public FilterMode entityFilterMode = FilterMode.BLACKLIST;
    public boolean bossDropsEnabled = false;
    public boolean multiplyMobExperience = false;
    public int mobExperienceMultiplier = 2;
    public boolean multiplyBossExperience = false;

    public boolean manualShearingDropsEnabled = true;
    public boolean automatedShearingDropsEnabled = false;
    public boolean inheritDefaultShearingMultiplier = true;
    public int defaultShearingMultiplier = 2;

    public Map<String, Integer> dimensionMultipliers = new LinkedHashMap<>();
    public Map<String, Integer> categoryMultipliers = new LinkedHashMap<>();
    public Map<String, Integer> blockMultipliers = new LinkedHashMap<>();
    public Set<String> blacklist = new LinkedHashSet<>();
    public Set<String> whitelist = new LinkedHashSet<>();
    public Set<String> tagBlacklist = new LinkedHashSet<>();
    public Set<String> tagWhitelist = new LinkedHashSet<>();
    public Set<String> blockEntityAllowlist = new LinkedHashSet<>();
    public Map<String, Integer> playerMultipliers = new LinkedHashMap<>();
    public Map<String, Integer> entityCategoryMultipliers = defaultEntityCategoryMultipliers();
    public Map<String, Integer> entityMultipliers = new LinkedHashMap<>();
    public Set<String> entityBlacklist = new LinkedHashSet<>();
    public Set<String> entityWhitelist = new LinkedHashSet<>();
    public Set<String> entityTagBlacklist = new LinkedHashSet<>();
    public Set<String> entityTagWhitelist = new LinkedHashSet<>();
    public Map<String, Integer> shearingEntityMultipliers = new LinkedHashMap<>();

    public static SmartDropsConfig defaults() {
        SmartDropsConfig config = new SmartDropsConfig();
        config.installSafetyBlacklist();
        config.sanitize();
        return config;
    }

    /**
     * Safe in-memory/recovery state used when an existing configuration cannot be trusted.
     * A confirmed fresh install and an explicit reset deliberately continue to use {@link #defaults()}.
     */
    static SmartDropsConfig safeExistingFileDefaults() {
        SmartDropsConfig config = defaults();
        config.manualShearingDropsEnabled = false;
        config.automatedShearingDropsEnabled = false;
        config.inheritDefaultShearingMultiplier = true;
        config.defaultShearingMultiplier = 2;
        config.shearingEntityMultipliers.clear();
        return config;
    }

    public void sanitize() {
        sanitize(null);
    }

    void sanitize(final ConfigLoadDiagnostics.Builder diagnostics) {
        if (schemaVersion <= 0) {
            adjusted(diagnostics, "schemaVersion", schemaVersion, CURRENT_SCHEMA);
            schemaVersion = CURRENT_SCHEMA;
        }
        maximumMultiplier = clampTracked(
                maximumMultiplier, 1, ABSOLUTE_MAX_MULTIPLIER, "maximumMultiplier", diagnostics);
        globalMultiplier = clampTracked(globalMultiplier, 0, maximumMultiplier, "globalMultiplier", diagnostics);
        experienceMultiplier = clampTracked(
                experienceMultiplier, 1, maximumMultiplier, "experienceMultiplier", diagnostics);
        maxPlayerMultiplier = clampTracked(
                maxPlayerMultiplier, 1, maximumMultiplier, "maxPlayerMultiplier", diagnostics);
        if (sourceMode == null) {
            adjusted(diagnostics, "sourceMode", null, SourceMode.NATURAL_ONLY);
            sourceMode = SourceMode.NATURAL_ONLY;
        }
        if (filterMode == null) {
            adjusted(diagnostics, "filterMode", null, FilterMode.BLACKLIST);
            filterMode = FilterMode.BLACKLIST;
        }

        if (entityKillRequirement == null) {
            adjusted(diagnostics, "entityKillRequirement", null, EntityKillRequirement.PLAYER_KILLS_ONLY);
            entityKillRequirement = EntityKillRequirement.PLAYER_KILLS_ONLY;
        }
        if (entityFilterMode == null) {
            adjusted(diagnostics, "entityFilterMode", null, FilterMode.BLACKLIST);
            entityFilterMode = FilterMode.BLACKLIST;
        }
        defaultEntityMultiplier = clampTracked(
                defaultEntityMultiplier, 0, maximumMultiplier, "defaultEntityMultiplier", diagnostics);
        mobExperienceMultiplier = clampTracked(
                mobExperienceMultiplier, 1, maximumMultiplier, "mobExperienceMultiplier", diagnostics);
        defaultShearingMultiplier = clampTracked(
                defaultShearingMultiplier, 0, maximumMultiplier, "defaultShearingMultiplier", diagnostics);

        int remaining = MAX_BLOCK_RULE_ENTRIES;
        blacklist = sanitizeIds(
                blacklist, remaining, SmartDropsConfig::isResourceLocation, diagnostics, "blacklist", DiagnosticKind.IDENTIFIER);
        remaining -= blacklist.size();
        whitelist = sanitizeIds(
                whitelist, remaining, SmartDropsConfig::isResourceLocation, diagnostics, "whitelist", DiagnosticKind.IDENTIFIER);
        remaining -= whitelist.size();
        tagBlacklist = sanitizeTagIds(tagBlacklist, remaining, diagnostics, "tagBlacklist");
        remaining -= tagBlacklist.size();
        tagWhitelist = sanitizeTagIds(tagWhitelist, remaining, diagnostics, "tagWhitelist");
        remaining -= tagWhitelist.size();
        blockEntityAllowlist = sanitizeIds(
                blockEntityAllowlist,
                remaining,
                SmartDropsConfig::isResourceLocation,
                diagnostics,
                "blockEntityAllowlist",
                DiagnosticKind.IDENTIFIER);
        remaining -= blockEntityAllowlist.size();
        dimensionMultipliers = sanitizeMultiplierMap(
                dimensionMultipliers,
                maximumMultiplier,
                remaining,
                SmartDropsConfig::isResourceLocation,
                diagnostics,
                "dimensionMultipliers",
                DiagnosticKind.IDENTIFIER);
        remaining -= dimensionMultipliers.size();
        categoryMultipliers = sanitizeMultiplierMap(
                categoryMultipliers,
                maximumMultiplier,
                remaining,
                SmartDropsConfig::isCategory,
                diagnostics,
                "categoryMultipliers",
                DiagnosticKind.CATEGORY);
        remaining -= categoryMultipliers.size();
        blockMultipliers = sanitizeMultiplierMap(
                blockMultipliers,
                maximumMultiplier,
                remaining,
                SmartDropsConfig::isResourceLocation,
                diagnostics,
                "blockMultipliers",
                DiagnosticKind.IDENTIFIER);
        remaining -= blockMultipliers.size();
        playerMultipliers = sanitizeMultiplierMap(
                playerMultipliers,
                maxPlayerMultiplier,
                remaining,
                SmartDropsConfig::isUuid,
                diagnostics,
                "playerMultipliers",
                DiagnosticKind.PLAYER);

        remaining = MAX_ENTITY_RULE_ENTRIES;
        if (entityCategoryMultipliers == null) {
            adjusted(diagnostics, "entityCategoryMultipliers", null, "safe defaults");
        }
        entityCategoryMultipliers = sanitizeMultiplierMap(
                entityCategoryMultipliers == null ? defaultEntityCategoryMultipliers() : entityCategoryMultipliers,
                maximumMultiplier,
                remaining,
                SmartDropsConfig::isEntityCategory,
                diagnostics,
                "entityCategoryMultipliers",
                DiagnosticKind.CATEGORY);
        remaining -= entityCategoryMultipliers.size();
        entityBlacklist = sanitizeIds(
                entityBlacklist,
                remaining,
                SmartDropsConfig::isEntityResourceLocation,
                diagnostics,
                "entityBlacklist",
                DiagnosticKind.IDENTIFIER);
        remaining -= entityBlacklist.size();
        entityWhitelist = sanitizeIds(
                entityWhitelist,
                remaining,
                SmartDropsConfig::isEntityResourceLocation,
                diagnostics,
                "entityWhitelist",
                DiagnosticKind.IDENTIFIER);
        remaining -= entityWhitelist.size();
        entityTagBlacklist = sanitizeTagIds(entityTagBlacklist, remaining, diagnostics, "entityTagBlacklist");
        remaining -= entityTagBlacklist.size();
        entityTagWhitelist = sanitizeTagIds(entityTagWhitelist, remaining, diagnostics, "entityTagWhitelist");
        remaining -= entityTagWhitelist.size();
        entityMultipliers = sanitizeMultiplierMap(
                entityMultipliers,
                maximumMultiplier,
                remaining,
                SmartDropsConfig::isEntityResourceLocation,
                diagnostics,
                "entityMultipliers",
                DiagnosticKind.IDENTIFIER);

        shearingEntityMultipliers = sanitizeMultiplierMap(
                shearingEntityMultipliers,
                maximumMultiplier,
                MAX_SHEARING_RULE_ENTRIES,
                SmartDropsConfig::isEntityResourceLocation,
                diagnostics,
                "shearingEntityMultipliers",
                DiagnosticKind.IDENTIFIER);
    }

    public void applyPreset(Preset preset) {
        if (preset == null || preset == Preset.CUSTOM) {
            return;
        }

        dimensionMultipliers.clear();
        categoryMultipliers.clear();
        blockMultipliers.clear();

        switch (preset) {
            case VANILLA_PLUS -> {
                globalMultiplier = 1;
                categoryMultipliers.put(Category.ORES.key(), 2);
                categoryMultipliers.put(Category.LOGS.key(), 2);
            }
            case FASTER_SURVIVAL -> {
                globalMultiplier = 2;
                categoryMultipliers.put(Category.LOGS.key(), 3);
                categoryMultipliers.put(Category.ORES.key(), 2);
                categoryMultipliers.put(Category.STONE.key(), 2);
                categoryMultipliers.put(Category.CROPS.key(), 2);
            }
            case FAST_PROGRESSION -> globalMultiplier = 4;
            case CUSTOM -> {
                // Kept for exhaustive switch safety.
            }
        }
        sanitize();
    }

    public void installSafetyBlacklist() {
        blacklist.add("minecraft:bedrock");
        blacklist.add("minecraft:barrier");
        blacklist.add("minecraft:command_block");
        blacklist.add("minecraft:chain_command_block");
        blacklist.add("minecraft:repeating_command_block");
        blacklist.add("minecraft:structure_block");
        blacklist.add("minecraft:jigsaw");
        blacklist.add("minecraft:end_portal");
        blacklist.add("minecraft:end_portal_frame");
        blacklist.add("minecraft:nether_portal");
        blacklist.add("minecraft:spawner");
        blacklist.add("minecraft:reinforced_deepslate");
        blacklist.add("minecraft:light");
        blacklist.add("minecraft:dragon_egg");
    }

    public int ruleEntryCount() {
        return rawRuleEntryCount();
    }

    public int blockRuleEntryCount() {
        return rawBlockRuleEntryCount();
    }

    public int entityRuleEntryCount() {
        return rawEntityRuleEntryCount();
    }

    public int shearingRuleEntryCount() {
        return rawShearingRuleEntryCount();
    }

    int rawRuleEntryCount() {
        return rawBlockRuleEntryCount() + rawEntityRuleEntryCount() + rawShearingRuleEntryCount();
    }

    int rawBlockRuleEntryCount() {
        return sizeOf(dimensionMultipliers)
                + sizeOf(categoryMultipliers)
                + sizeOf(blockMultipliers)
                + sizeOf(playerMultipliers)
                + sizeOf(blacklist)
                + sizeOf(whitelist)
                + sizeOf(tagBlacklist)
                + sizeOf(tagWhitelist)
                + sizeOf(blockEntityAllowlist);
    }

    int rawEntityRuleEntryCount() {
        return sizeOf(entityCategoryMultipliers)
                + sizeOf(entityMultipliers)
                + sizeOf(entityBlacklist)
                + sizeOf(entityWhitelist)
                + sizeOf(entityTagBlacklist)
                + sizeOf(entityTagWhitelist);
    }

    int rawShearingRuleEntryCount() {
        return sizeOf(shearingEntityMultipliers);
    }

    private static int sizeOf(java.util.Collection<?> values) {
        return values == null ? 0 : values.size();
    }

    private static int sizeOf(Map<?, ?> values) {
        return values == null ? 0 : values.size();
    }

    private static LinkedHashMap<String, Integer> sanitizeMultiplierMap(
            Map<String, Integer> input,
            int max,
            int limit,
            Predicate<String> keyValidator,
            ConfigLoadDiagnostics.Builder diagnostics,
            String settingPath,
            DiagnosticKind diagnosticKind
    ) {
        LinkedHashMap<String, Integer> output = new LinkedHashMap<>();
        if (input == null || limit <= 0) {
            return output;
        }
        input.forEach((key, value) -> {
            if (output.size() >= limit) {
                return;
            }
            if (key == null || value == null) {
                recordInvalid(diagnostics, diagnosticKind, settingPath, key);
                return;
            }
            String cleanKey = key.trim().toLowerCase(Locale.ROOT);
            if (!validLength(cleanKey) || !keyValidator.test(cleanKey)) {
                recordInvalid(diagnostics, diagnosticKind, settingPath, key);
                return;
            }
            final int bounded = clamp(value, 0, max);
            if (bounded != value) {
                final String samplePath = diagnosticKind == DiagnosticKind.PLAYER
                        ? settingPath
                        : settingPath + "[" + cleanKey + "]";
                adjusted(diagnostics, samplePath, value, bounded);
            }
            output.put(cleanKey, bounded);
        });
        return output;
    }

    private static LinkedHashSet<String> sanitizeIds(
            Set<String> input,
            int limit,
            Predicate<String> validator,
            ConfigLoadDiagnostics.Builder diagnostics,
            String settingPath,
            DiagnosticKind diagnosticKind
    ) {
        LinkedHashSet<String> output = new LinkedHashSet<>();
        if (input == null || limit <= 0) {
            return output;
        }
        for (String value : input) {
            if (output.size() >= limit) {
                break;
            }
            if (value == null) {
                recordInvalid(diagnostics, diagnosticKind, settingPath, null);
                continue;
            }
            String clean = value.trim().toLowerCase(Locale.ROOT);
            if (validLength(clean) && validator.test(clean)) {
                output.add(clean);
            } else {
                recordInvalid(diagnostics, diagnosticKind, settingPath, value);
            }
        }
        return output;
    }

    private static LinkedHashSet<String> sanitizeTagIds(
            Set<String> input,
            int limit,
            ConfigLoadDiagnostics.Builder diagnostics,
            String settingPath
    ) {
        LinkedHashSet<String> output = new LinkedHashSet<>();
        if (input == null || limit <= 0) {
            return output;
        }
        for (String value : input) {
            if (output.size() >= limit) {
                break;
            }
            if (value == null) {
                recordInvalid(diagnostics, DiagnosticKind.IDENTIFIER, settingPath, null);
                continue;
            }
            String clean = value.trim().toLowerCase(Locale.ROOT);
            while (clean.startsWith("#")) {
                clean = clean.substring(1);
            }
            if (validLength(clean) && isResourceLocation(clean)) {
                output.add(clean);
            } else {
                recordInvalid(diagnostics, DiagnosticKind.IDENTIFIER, settingPath, value);
            }
        }
        return output;
    }

    private static int clampTracked(
            final int value,
            final int minimum,
            final int maximum,
            final String settingPath,
            final ConfigLoadDiagnostics.Builder diagnostics
    ) {
        final int bounded = clamp(value, minimum, maximum);
        if (bounded != value) {
            adjusted(diagnostics, settingPath, value, bounded);
        }
        return bounded;
    }

    private static void adjusted(
            final ConfigLoadDiagnostics.Builder diagnostics,
            final String settingPath,
            final Object before,
            final Object after
    ) {
        if (diagnostics != null) {
            diagnostics.valueAdjusted(settingPath, before, after);
        }
    }

    private static void recordInvalid(
            final ConfigLoadDiagnostics.Builder diagnostics,
            final DiagnosticKind kind,
            final String settingPath,
            final String value
    ) {
        if (diagnostics == null) {
            return;
        }
        switch (kind) {
            case IDENTIFIER -> diagnostics.invalidResourceLocation(settingPath, value);
            case CATEGORY -> diagnostics.invalidCategory(settingPath, value);
            case PLAYER -> diagnostics.invalidPlayerOverride();
        }
    }

    private enum DiagnosticKind {
        IDENTIFIER,
        CATEGORY,
        PLAYER
    }

    private static boolean validLength(String value) {
        return !value.isEmpty() && value.length() <= MAX_RULE_KEY_LENGTH;
    }

    private static boolean isResourceLocation(String value) {
        int separator = value.indexOf(':');
        if (separator <= 0 || separator == value.length() - 1 || value.indexOf(':', separator + 1) >= 0) {
            return false;
        }
        return validResourceLocationPart(value, 0, separator, false)
                && validResourceLocationPart(value, separator + 1, value.length(), true);
    }

    private static boolean validResourceLocationPart(String value, int start, int end, boolean allowSlash) {
        for (int index = start; index < end; index++) {
            char character = value.charAt(index);
            boolean valid = character >= 'a' && character <= 'z'
                    || character >= '0' && character <= '9'
                    || character == '_'
                    || character == '-'
                    || character == '.'
                    || allowSlash && character == '/';
            if (!valid) {
                return false;
            }
        }
        return true;
    }

    private static boolean isCategory(String value) {
        return Category.parse(value).isPresent();
    }

    private static boolean isEntityCategory(String value) {
        return EntityCategory.parse(value).isPresent();
    }

    private static boolean isEntityResourceLocation(String value) {
        return isResourceLocation(value) && !"minecraft:player".equals(value);
    }

    private static LinkedHashMap<String, Integer> defaultEntityCategoryMultipliers() {
        LinkedHashMap<String, Integer> defaults = new LinkedHashMap<>();
        defaults.put(EntityCategory.GOLEMS.key(), 1);
        defaults.put(EntityCategory.VILLAGERS_NPCS.key(), 1);
        defaults.put(EntityCategory.BOSSES.key(), 1);
        defaults.put(EntityCategory.MISCELLANEOUS.key(), 1);
        return defaults;
    }

    private static boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public enum SourceMode {
        NATURAL_ONLY,
        ALL,
        PLAYER_PLACED_ONLY
    }

    public enum FilterMode {
        BLACKLIST,
        WHITELIST
    }

    public enum EntityKillRequirement {
        PLAYER_KILLS_ONLY,
        PLAYER_OR_TAMED_ENTITY,
        ALL_STANDARD_DEATH_LOOT
    }

    public enum Preset {
        VANILLA_PLUS,
        FASTER_SURVIVAL,
        FAST_PROGRESSION,
        CUSTOM
    }
}
