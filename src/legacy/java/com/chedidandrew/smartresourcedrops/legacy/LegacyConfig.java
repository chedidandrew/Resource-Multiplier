package com.chedidandrew.smartresourcedrops.legacy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Java 8 schema-three configuration for the Minecraft 1.7.10 Forge edition. */
public final class LegacyConfig {
    public static final int CURRENT_SCHEMA = 3;
    public static final int ABSOLUTE_MAX_MULTIPLIER = 64;
    private static final int KEY_IDENTIFIER = 1;
    private static final int KEY_DIMENSION = 2;
    private static final int KEY_CATEGORY = 3;
    private static final int KEY_PLAYER = 4;
    private static final int KEY_ENTITY_CATEGORY = 5;
    private static final int KEY_ENTITY_IDENTIFIER = 6;
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
        LegacyConfig value = new LegacyConfig();
        value.installSafetyBlacklist();
        value.sanitize();
        return value;
    }

    public static LegacyConfig load(File file) {
        if (!file.isFile()) {
            LegacyConfig value = defaults();
            value.save(file);
            return value;
        }
        Reader reader = null;
        try {
            reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
            return parseStored(new JsonParser().parse(reader));
        } catch (Exception exception) {
            try {
                Files.copy(file.toPath(), new File(file.getParentFile(), file.getName() + ".broken").toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {}
            LegacyConfig safe = defaults();
            safe.manualShearingDropsEnabled = false;
            return safe;
        } finally {
            if (reader != null) try { reader.close(); } catch (IOException ignored) {}
        }
    }

    public synchronized boolean save(File file) {
        sanitize();
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) return false;
        File temporary = new File(parent, file.getName() + ".tmp");
        Writer writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(temporary), StandardCharsets.UTF_8);
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
            if (writer != null) try { writer.close(); } catch (IOException ignored) {}
            if (temporary.isFile() && !temporary.delete()) temporary.deleteOnExit();
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
        int remaining = 2048;
        blacklist = cleanSet(blacklist, remaining, false, KEY_IDENTIFIER);
        remaining -= blacklist.size();
        whitelist = cleanSet(whitelist, remaining, false, KEY_IDENTIFIER);
        remaining -= whitelist.size();
        tagBlacklist = cleanSet(tagBlacklist, remaining, true, KEY_IDENTIFIER);
        remaining -= tagBlacklist.size();
        tagWhitelist = cleanSet(tagWhitelist, remaining, true, KEY_IDENTIFIER);
        remaining -= tagWhitelist.size();
        blockEntityAllowlist = cleanSet(blockEntityAllowlist, remaining, false, KEY_IDENTIFIER);
        remaining -= blockEntityAllowlist.size();
        dimensionMultipliers = cleanMap(
                dimensionMultipliers, maximumMultiplier, remaining, KEY_DIMENSION);
        remaining -= dimensionMultipliers.size();
        categoryMultipliers = cleanMap(
                categoryMultipliers, maximumMultiplier, remaining, KEY_CATEGORY);
        migrateRawResourceCategoryAlias();
        remaining -= categoryMultipliers.size();
        blockMultipliers = cleanMap(
                blockMultipliers, maximumMultiplier, remaining, KEY_IDENTIFIER);
        remaining -= blockMultipliers.size();
        playerMultipliers = cleanMap(
                playerMultipliers, maxPlayerMultiplier, remaining, KEY_PLAYER);

        remaining = 512;
        entityCategoryMultipliers = cleanMap(
                entityCategoryMultipliers == null ? defaultEntityCategories() : entityCategoryMultipliers,
                maximumMultiplier,
                remaining,
                KEY_ENTITY_CATEGORY);
        remaining -= entityCategoryMultipliers.size();
        entityBlacklist = cleanSet(entityBlacklist, remaining, false, KEY_ENTITY_IDENTIFIER);
        remaining -= entityBlacklist.size();
        entityWhitelist = cleanSet(entityWhitelist, remaining, false, KEY_ENTITY_IDENTIFIER);
        remaining -= entityWhitelist.size();
        entityTagBlacklist = cleanSet(entityTagBlacklist, remaining, true, KEY_IDENTIFIER);
        remaining -= entityTagBlacklist.size();
        entityTagWhitelist = cleanSet(entityTagWhitelist, remaining, true, KEY_IDENTIFIER);
        remaining -= entityTagWhitelist.size();
        entityMultipliers = cleanMap(
                entityMultipliers, maximumMultiplier, remaining, KEY_ENTITY_IDENTIFIER);
        shearingEntityMultipliers = cleanMap(
                shearingEntityMultipliers, maximumMultiplier, 256, KEY_ENTITY_IDENTIFIER);
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

    /**
     * Returns whether a block action belongs to the multiplier domain, independently
     * of the configured item-drop multiplier. Block XP uses its own scalar after this
     * eligibility decision, matching the modern implementations.
     */
    public boolean isBlockEligible(String blockId, boolean playerPlaced, boolean hasBlockEntity) {
        return enabled
                && isBlockAllowed(blockId)
                && isSourceAllowed(playerPlaced)
                && (!hasBlockEntity || !protectBlockEntities || blockEntityAllowlist.contains(blockId));
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

    public boolean isBlockAllowed(String id) {
        return filterMode == FilterMode.WHITELIST ? whitelist.contains(id) : !blacklist.contains(id);
    }

    public boolean isEntityAllowed(String id) {
        return entityFilterMode == FilterMode.WHITELIST ? entityWhitelist.contains(id) : !entityBlacklist.contains(id);
    }

    public boolean isSourceAllowed(boolean placed) {
        if (sourceMode == SourceMode.ALL) return true;
        if (sourceMode == SourceMode.PLAYER_PLACED_ONLY) return placed;
        return !smartPlacementProtection || !placed;
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
        } else if (preset == Preset.FAST_PROGRESSION) globalMultiplier = 4;
        sanitize();
    }

    public void installSafetyBlacklist() {
        if (blacklist == null) blacklist = new LinkedHashSet<String>();
        blacklist.addAll(Arrays.asList(
                "minecraft:bedrock", "minecraft:barrier", "minecraft:command_block",
                "minecraft:chain_command_block", "minecraft:repeating_command_block",
                "minecraft:structure_block", "minecraft:jigsaw", "minecraft:end_portal",
                "minecraft:end_portal_frame", "minecraft:nether_portal", "minecraft:portal",
                "minecraft:spawner", "minecraft:mob_spawner", "minecraft:trial_spawner",
                "minecraft:vault", "minecraft:reinforced_deepslate",
                "minecraft:light", "minecraft:dragon_egg"));
    }

    public LegacyConfig copy() { return fromJson(toJson()); }
    public String toJson() { sanitize(); return GSON.toJson(this); }
    public static LegacyConfig fromJson(String json) {
        if (json == null || json.trim().length() == 0) {
            throw new IllegalArgumentException("Empty configuration");
        }
        return parseStored(new JsonParser().parse(json));
    }

    private static LegacyConfig parseStored(JsonElement root) {
        if (root == null || !root.isJsonObject()) {
            throw new IllegalArgumentException("Configuration root must be an object");
        }
        JsonObject object = root.getAsJsonObject();
        int storedSchema = 1;
        if (object.has("schemaVersion")) {
            JsonElement schema = object.get("schemaVersion");
            if (schema == null || !schema.isJsonPrimitive() || !schema.getAsJsonPrimitive().isNumber()) {
                throw new IllegalArgumentException("schemaVersion must be an integer");
            }
            try {
                storedSchema = new BigDecimal(schema.getAsString()).intValueExact();
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException("schemaVersion must be an integer", exception);
            }
        }
        if (storedSchema > CURRENT_SCHEMA) {
            throw new IllegalArgumentException("Unsupported future configuration schema " + storedSchema);
        }
        LegacyConfig value = GSON.fromJson(root, LegacyConfig.class);
        if (value == null) throw new IllegalArgumentException("Empty configuration");
        if (!object.has("blacklist") || object.get("blacklist").isJsonNull()) value.installSafetyBlacklist();
        if (storedSchema < 2) resetEntitySettingsForMigration(value);
        if (storedSchema < 3) resetShearingSettingsForMigration(value);
        value.schemaVersion = CURRENT_SCHEMA;
        value.sanitize();
        return value;
    }

    private static void resetEntitySettingsForMigration(LegacyConfig value) {
        LegacyConfig safe = defaults();
        value.entityDropsEnabled = safe.entityDropsEnabled;
        value.inheritDefaultEntityMultiplier = safe.inheritDefaultEntityMultiplier;
        value.defaultEntityMultiplier = safe.defaultEntityMultiplier;
        value.entityKillRequirement = safe.entityKillRequirement;
        value.entityFilterMode = safe.entityFilterMode;
        value.bossDropsEnabled = safe.bossDropsEnabled;
        value.multiplyMobExperience = safe.multiplyMobExperience;
        value.mobExperienceMultiplier = safe.mobExperienceMultiplier;
        value.multiplyBossExperience = safe.multiplyBossExperience;
        value.entityCategoryMultipliers = defaultEntityCategories();
        value.entityMultipliers = new LinkedHashMap<String, Integer>();
        value.entityBlacklist = new LinkedHashSet<String>();
        value.entityWhitelist = new LinkedHashSet<String>();
        value.entityTagBlacklist = new LinkedHashSet<String>();
        value.entityTagWhitelist = new LinkedHashSet<String>();
    }

    private static void resetShearingSettingsForMigration(LegacyConfig value) {
        value.manualShearingDropsEnabled = false;
        value.automatedShearingDropsEnabled = false;
        value.inheritDefaultShearingMultiplier = true;
        value.defaultShearingMultiplier = 2;
        value.shearingEntityMultipliers = new LinkedHashMap<String, Integer>();
    }

    private static Map<String, Integer> cleanMap(
            Map<String, Integer> input, int maximum, int limit, int keyType) {
        Map<String, Integer> output = new LinkedHashMap<String, Integer>();
        if (input == null) return output;
        for (Map.Entry<String, Integer> entry : input.entrySet()) {
            if (output.size() >= limit || entry.getKey() == null || entry.getValue() == null) continue;
            String key = normalize(entry.getKey());
            if (key.length() == 0 || key.length() > 256 || !validKey(key, keyType)) continue;
            output.put(key, Integer.valueOf(clamp(entry.getValue().intValue(), 0, maximum)));
        }
        return output;
    }

    private static Set<String> cleanSet(
            Set<String> input, int limit, boolean stripTagMarker, int keyType) {
        Set<String> output = new LinkedHashSet<String>();
        if (input == null) return output;
        for (String value : input) {
            if (output.size() >= limit || value == null) continue;
            String clean = normalize(value);
            if (stripTagMarker) while (clean.startsWith("#")) clean = clean.substring(1);
            if (clean.length() > 0 && clean.length() <= 256 && validKey(clean, keyType)) {
                output.add(clean);
            }
        }
        return output;
    }

    private static boolean validKey(String value, int keyType) {
        if (keyType == KEY_DIMENSION) return isDimensionKey(value);
        if (keyType == KEY_CATEGORY) return isBlockCategory(value);
        if (keyType == KEY_PLAYER) return isUuid(value);
        if (keyType == KEY_ENTITY_CATEGORY) return isEntityCategory(value);
        if (keyType == KEY_ENTITY_IDENTIFIER) {
            return isIdentifier(value) && !"minecraft:player".equals(value);
        }
        return isIdentifier(value);
    }

    private static boolean isDimensionKey(String value) {
        if (isIdentifier(value)) return true;
        int index = value.startsWith("-") ? 1 : 0;
        if (index == value.length()) return false;
        for (; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character < '0' || character > '9') return false;
        }
        return true;
    }

    private static boolean isBlockCategory(String value) {
        return "ores".equals(value)
                || "raw_resources".equals(value)
                || "raw_resource_blocks".equals(value)
                || "logs".equals(value)
                || "stone".equals(value)
                || "soil".equals(value)
                || "nether".equals(value)
                || "end".equals(value)
                || "crops".equals(value)
                || "plants".equals(value)
                || "leaves".equals(value)
                || "building_blocks".equals(value)
                || "miscellaneous".equals(value);
    }

    private static boolean isEntityCategory(String value) {
        return "bosses".equals(value)
                || "villagers_npcs".equals(value)
                || "golems".equals(value)
                || "neutral".equals(value)
                || "passive".equals(value)
                || "hostile".equals(value)
                || "aquatic".equals(value)
                || "ambient".equals(value)
                || "miscellaneous".equals(value);
    }

    private static boolean isIdentifier(String value) {
        int separator = value.indexOf(':');
        if (separator <= 0 || separator == value.length() - 1
                || value.indexOf(':', separator + 1) >= 0) return false;
        return validIdentifierPart(value, 0, separator, false)
                && validIdentifierPart(value, separator + 1, value.length(), true);
    }

    private static boolean validIdentifierPart(
            String value, int start, int end, boolean allowSlash) {
        for (int index = start; index < end; index++) {
            char character = value.charAt(index);
            boolean valid = character >= 'a' && character <= 'z'
                    || character >= '0' && character <= '9'
                    || character == '_'
                    || character == '-'
                    || character == '.'
                    || allowSlash && character == '/';
            if (!valid) return false;
        }
        return true;
    }

    private static boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static Map<String, Integer> defaultEntityCategories() {
        Map<String, Integer> values = new LinkedHashMap<String, Integer>();
        values.put("golems", Integer.valueOf(1));
        values.put("villagers_npcs", Integer.valueOf(1));
        values.put("bosses", Integer.valueOf(1));
        values.put("miscellaneous", Integer.valueOf(1));
        return values;
    }

    private static String dimensionName(int dimension) {
        if (dimension == -1) return "the_nether";
        if (dimension == 1) return "the_end";
        return dimension == 0 ? "overworld" : Integer.toString(dimension);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public enum SourceMode { NATURAL_ONLY, ALL, PLAYER_PLACED_ONLY }
    public enum FilterMode { BLACKLIST, WHITELIST }
    public enum EntityKillRequirement { PLAYER_KILLS_ONLY, PLAYER_OR_TAMED_ENTITY, ALL_STANDARD_DEATH_LOOT }
    public enum Preset { VANILLA_PLUS, FASTER_SURVIVAL, FAST_PROGRESSION, CUSTOM }
}
