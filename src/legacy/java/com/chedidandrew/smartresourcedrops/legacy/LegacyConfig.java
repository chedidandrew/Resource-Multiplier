package com.chedidandrew.smartresourcedrops.legacy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Schema-three configuration shared conceptually with the modern editions.
 *
 * <p>The old Forge runtime bundles Gson, so keeping the public field names stable
 * lets existing Smart Resource Multiplier JSON files retain the settings that
 * make sense on Minecraft 1.12.2.</p>
 */
public final class LegacyConfig {
    public static final int CURRENT_SCHEMA = 3;
    public static final int ABSOLUTE_MAX_MULTIPLIER = 64;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

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

    public Map<String, Integer> dimensionMultipliers = new LinkedHashMap<String, Integer>();
    public Map<String, Integer> categoryMultipliers = new LinkedHashMap<String, Integer>();
    public Map<String, Integer> blockMultipliers = new LinkedHashMap<String, Integer>();
    public Set<String> blacklist = new LinkedHashSet<String>();
    public Set<String> whitelist = new LinkedHashSet<String>();
    public Set<String> tagBlacklist = new LinkedHashSet<String>();
    public Set<String> tagWhitelist = new LinkedHashSet<String>();
    public Set<String> blockEntityAllowlist = new LinkedHashSet<String>();
    public Map<String, Integer> playerMultipliers = new LinkedHashMap<String, Integer>();
    public Map<String, Integer> entityCategoryMultipliers = defaultEntityCategories();
    public Map<String, Integer> entityMultipliers = new LinkedHashMap<String, Integer>();
    public Set<String> entityBlacklist = new LinkedHashSet<String>();
    public Set<String> entityWhitelist = new LinkedHashSet<String>();
    public Set<String> entityTagBlacklist = new LinkedHashSet<String>();
    public Set<String> entityTagWhitelist = new LinkedHashSet<String>();
    public Map<String, Integer> shearingEntityMultipliers = new LinkedHashMap<String, Integer>();

    public static LegacyConfig defaults() {
        LegacyConfig config = new LegacyConfig();
        config.installSafetyBlacklist();
        config.sanitize();
        return config;
    }

    public static LegacyConfig load(File file) {
        if (!file.isFile()) {
            LegacyConfig created = defaults();
            created.save(file);
            return created;
        }
        FileReader reader = null;
        try {
            reader = new FileReader(file);
            LegacyConfig loaded = GSON.fromJson(reader, LegacyConfig.class);
            if (loaded == null) {
                throw new IOException("Configuration contained no object");
            }
            loaded.sanitize();
            return loaded;
        } catch (Exception exception) {
            File broken = new File(file.getParentFile(), file.getName() + ".broken");
            try {
                Files.copy(file.toPath(), broken.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
                // The safe in-memory configuration still lets the game start.
            }
            LegacyConfig safe = defaults();
            safe.manualShearingDropsEnabled = false;
            return safe;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                    // Nothing else can be done while loading.
                }
            }
        }
    }

    public synchronized boolean save(File file) {
        sanitize();
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            return false;
        }
        File temporary = new File(parent, file.getName() + ".tmp");
        FileWriter writer = null;
        try {
            writer = new FileWriter(temporary);
            GSON.toJson(this, writer);
            writer.flush();
            writer.close();
            writer = null;
            try {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException exception) {
            return false;
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignored) {
                    // Preserve the original result.
                }
            }
            if (temporary.isFile() && !temporary.delete()) {
                temporary.deleteOnExit();
            }
        }
    }

    public void sanitize() {
        schemaVersion = CURRENT_SCHEMA;
        maximumMultiplier = clamp(maximumMultiplier, 1, ABSOLUTE_MAX_MULTIPLIER);
        globalMultiplier = clamp(globalMultiplier, 0, maximumMultiplier);
        experienceMultiplier = clamp(experienceMultiplier, 1, maximumMultiplier);
        defaultEntityMultiplier = clamp(defaultEntityMultiplier, 0, maximumMultiplier);
        mobExperienceMultiplier = clamp(mobExperienceMultiplier, 1, maximumMultiplier);
        defaultShearingMultiplier = clamp(defaultShearingMultiplier, 0, maximumMultiplier);
        maxPlayerMultiplier = clamp(maxPlayerMultiplier, 1, maximumMultiplier);
        if (sourceMode == null) sourceMode = SourceMode.NATURAL_ONLY;
        if (filterMode == null) filterMode = FilterMode.BLACKLIST;
        if (entityFilterMode == null) entityFilterMode = FilterMode.BLACKLIST;
        if (entityKillRequirement == null) entityKillRequirement = EntityKillRequirement.PLAYER_KILLS_ONLY;

        dimensionMultipliers = cleanMap(dimensionMultipliers, maximumMultiplier, 2048);
        categoryMultipliers = cleanMap(categoryMultipliers, maximumMultiplier, 2048);
        migrateRawResourceCategoryAlias();
        blockMultipliers = cleanMap(blockMultipliers, maximumMultiplier, 2048);
        playerMultipliers = cleanMap(playerMultipliers, maxPlayerMultiplier, 2048);
        entityCategoryMultipliers = cleanMap(entityCategoryMultipliers, maximumMultiplier, 512);
        entityMultipliers = cleanMap(entityMultipliers, maximumMultiplier, 512);
        shearingEntityMultipliers = cleanMap(shearingEntityMultipliers, maximumMultiplier, 256);
        blacklist = cleanSet(blacklist, 2048);
        whitelist = cleanSet(whitelist, 2048);
        tagBlacklist = cleanSet(tagBlacklist, 2048);
        tagWhitelist = cleanSet(tagWhitelist, 2048);
        blockEntityAllowlist = cleanSet(blockEntityAllowlist, 2048);
        entityBlacklist = cleanSet(entityBlacklist, 512);
        entityWhitelist = cleanSet(entityWhitelist, 512);
        entityTagBlacklist = cleanSet(entityTagBlacklist, 512);
        entityTagWhitelist = cleanSet(entityTagWhitelist, 512);
        installSafetyBlacklist();
    }

    private void migrateRawResourceCategoryAlias() {
        Integer oldValue = categoryMultipliers.remove("raw_resources");
        if (oldValue != null && !categoryMultipliers.containsKey("raw_resource_blocks")) {
            categoryMultipliers.put("raw_resource_blocks", oldValue);
        }
    }

    public int blockMultiplier(String blockId, String category, int dimension, boolean playerPlaced,
            boolean hasBlockEntity, String playerUuid) {
        if (!isBlockEligible(blockId, playerPlaced, hasBlockEntity)) return 1;
        int result = globalMultiplier;
        Integer dimensionValue = dimensionMultipliers.get(Integer.toString(dimension));
        if (dimensionValue == null) dimensionValue = dimensionMultipliers.get("minecraft:" + dimensionName(dimension));
        if (dimensionValue != null) result = dimensionValue.intValue();
        Integer categoryValue = categoryMultipliers.get(normalize(category));
        if (categoryValue != null) result = categoryValue.intValue();
        Integer exact = blockMultipliers.get(blockId);
        if (exact != null) result = exact.intValue();
        if (allowPlayerOverrides && playerUuid != null) {
            Integer player = playerMultipliers.get(playerUuid.toLowerCase(Locale.ROOT));
            if (player != null) result = Math.min(player.intValue(), maxPlayerMultiplier);
        }
        return result;
    }

    public boolean isBlockEligible(String blockId, boolean playerPlaced, boolean hasBlockEntity) {
        if (!enabled || !isBlockAllowed(blockId) || !isSourceAllowed(playerPlaced)) return false;
        return !hasBlockEntity || !protectBlockEntities || blockEntityAllowlist.contains(blockId);
    }

    public int entityMultiplier(String entityId, String category, boolean boss) {
        if (!enabled || !entityDropsEnabled || !isEntityAllowed(entityId)) return 1;
        if (boss && !bossDropsEnabled) return 1;
        Integer exact = entityMultipliers.get(entityId);
        if (exact != null) return exact.intValue();
        Integer categoryValue = entityCategoryMultipliers.get(normalize(category));
        if (categoryValue != null) return categoryValue.intValue();
        return inheritDefaultEntityMultiplier ? globalMultiplier : defaultEntityMultiplier;
    }

    public int shearingMultiplier(String entityId) {
        Integer exact = shearingEntityMultipliers.get(entityId);
        if (exact != null) return exact.intValue();
        return inheritDefaultShearingMultiplier ? globalMultiplier : defaultShearingMultiplier;
    }

    public boolean isBlockAllowed(String blockId) {
        if (filterMode == FilterMode.WHITELIST) return whitelist.contains(blockId);
        return !blacklist.contains(blockId);
    }

    public boolean isEntityAllowed(String entityId) {
        if (entityFilterMode == FilterMode.WHITELIST) return entityWhitelist.contains(entityId);
        return !entityBlacklist.contains(entityId);
    }

    public boolean isSourceAllowed(boolean playerPlaced) {
        if (sourceMode == SourceMode.ALL) return true;
        if (sourceMode == SourceMode.PLAYER_PLACED_ONLY) return playerPlaced;
        return !smartPlacementProtection || !playerPlaced;
    }

    public void applyPreset(Preset preset) {
        if (preset == null || preset == Preset.CUSTOM) return;
        dimensionMultipliers.clear();
        categoryMultipliers.clear();
        blockMultipliers.clear();
        if (preset == Preset.VANILLA_PLUS) {
            globalMultiplier = 1;
            categoryMultipliers.put("ores", Integer.valueOf(2));
            categoryMultipliers.put("logs", Integer.valueOf(2));
        } else if (preset == Preset.FASTER_SURVIVAL) {
            globalMultiplier = 2;
            categoryMultipliers.put("logs", Integer.valueOf(3));
            categoryMultipliers.put("ores", Integer.valueOf(2));
            categoryMultipliers.put("stone", Integer.valueOf(2));
            categoryMultipliers.put("crops", Integer.valueOf(2));
        } else if (preset == Preset.FAST_PROGRESSION) {
            globalMultiplier = 4;
        }
        sanitize();
    }

    public void installSafetyBlacklist() {
        if (blacklist == null) blacklist = new LinkedHashSet<String>();
        blacklist.addAll(Arrays.asList(
                "minecraft:bedrock", "minecraft:barrier", "minecraft:command_block",
                "minecraft:chain_command_block", "minecraft:repeating_command_block",
                "minecraft:structure_block", "minecraft:end_portal", "minecraft:end_portal_frame",
                "minecraft:nether_portal", "minecraft:mob_spawner", "minecraft:dragon_egg"));
    }

    public LegacyConfig copy() {
        return GSON.fromJson(GSON.toJson(this), LegacyConfig.class);
    }

    public String toJson() {
        sanitize();
        return GSON.toJson(this);
    }

    public static LegacyConfig fromJson(String json) {
        LegacyConfig value = GSON.fromJson(json, LegacyConfig.class);
        if (value == null) value = defaults();
        value.sanitize();
        return value;
    }

    private static Map<String, Integer> cleanMap(Map<String, Integer> input, int maximum, int limit) {
        Map<String, Integer> output = new LinkedHashMap<String, Integer>();
        if (input == null) return output;
        for (Map.Entry<String, Integer> entry : input.entrySet()) {
            if (output.size() >= limit || entry.getKey() == null || entry.getValue() == null) continue;
            String key = normalize(entry.getKey());
            if (key.length() == 0 || key.length() > 256) continue;
            output.put(key, Integer.valueOf(clamp(entry.getValue().intValue(), 0, maximum)));
        }
        return output;
    }

    private static Set<String> cleanSet(Set<String> input, int limit) {
        Set<String> output = new LinkedHashSet<String>();
        if (input == null) return output;
        for (String value : input) {
            if (output.size() >= limit || value == null) continue;
            String clean = normalize(value);
            while (clean.startsWith("#")) clean = clean.substring(1);
            if (clean.length() > 0 && clean.length() <= 256) output.add(clean);
        }
        return output;
    }

    private static Map<String, Integer> defaultEntityCategories() {
        Map<String, Integer> defaults = new LinkedHashMap<String, Integer>();
        defaults.put("golems", Integer.valueOf(1));
        defaults.put("villagers_npcs", Integer.valueOf(1));
        defaults.put("bosses", Integer.valueOf(1));
        defaults.put("miscellaneous", Integer.valueOf(1));
        return defaults;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String dimensionName(int dimension) {
        if (dimension == -1) return "the_nether";
        if (dimension == 1) return "the_end";
        return dimension == 0 ? "overworld" : Integer.toString(dimension);
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public enum SourceMode { NATURAL_ONLY, ALL, PLAYER_PLACED_ONLY }
    public enum FilterMode { BLACKLIST, WHITELIST }
    public enum EntityKillRequirement { PLAYER_KILLS_ONLY, PLAYER_OR_TAMED_ENTITY, ALL_STANDARD_DEATH_LOOT }
    public enum Preset { VANILLA_PLUS, FASTER_SURVIVAL, FAST_PROGRESSION, CUSTOM }
}
