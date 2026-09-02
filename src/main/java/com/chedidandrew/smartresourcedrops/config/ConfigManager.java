package com.chedidandrew.smartresourcedrops.config;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.core.Category;
import com.chedidandrew.smartresourcedrops.core.entity.EntityCategory;
import com.chedidandrew.smartresourcedrops.core.util.AtomicConfigWriter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static volatile SmartDropsConfig config = SmartDropsConfig.safeExistingFileDefaults();
    private static volatile Path configPath;
    private static boolean writesSuppressed;
    private static volatile String cachedClientSnapshotJson;
    private static long configRevision;
    private static ConfigLoadDiagnostics loadDiagnostics = ConfigLoadDiagnostics.empty();
    private static volatile PublicationListener publicationListener = (revision, kind) -> {
    };

    private static final Set<String> SCHEMA_TWO_ENTITY_FIELDS = Set.of(
            "entityDropsEnabled",
            "inheritDefaultEntityMultiplier",
            "defaultEntityMultiplier",
            "entityKillRequirement",
            "entityFilterMode",
            "bossDropsEnabled",
            "multiplyMobExperience",
            "mobExperienceMultiplier",
            "multiplyBossExperience",
            "entityCategoryMultipliers",
            "entityMultipliers",
            "entityBlacklist",
            "entityWhitelist",
            "entityTagBlacklist",
            "entityTagWhitelist");

    private static final Set<String> SCHEMA_THREE_SHEARING_FIELDS = Set.of(
            "manualShearingDropsEnabled",
            "automatedShearingDropsEnabled",
            "inheritDefaultShearingMultiplier",
            "defaultShearingMultiplier",
            "shearingEntityMultipliers");

    private ConfigManager() {
    }

    public static synchronized boolean load() {
        return load(configPath());
    }

    static synchronized boolean load(Path path) {
        if (Files.notExists(path)) {
            final SmartDropsConfig defaults = SmartDropsConfig.defaults();
            loadDiagnostics = ConfigLoadDiagnostics.empty();
            if (!writeCandidate(path, defaults, "create the default configuration")) {
                writesSuppressed = true;
                loadDiagnostics = loadDiagnostics.withWriteFailure();
                return false;
            }
            publishConfig(defaults, PublicationKind.UPDATE);
            writesSuppressed = false;
            return true;
        }

        final String json;
        try {
            json = Files.readString(path);
        } catch (IOException exception) {
            writesSuppressed = true;
            loadDiagnostics = ConfigLoadDiagnostics.readFailure();
            SmartResourceDrops.LOGGER.error(
                    "Could not read Smart Resource Multiplier config; the existing file will not be overwritten",
                    exception);
            return false;
        }

        final ParsedConfig parsed;
        try {
            parsed = parseStoredConfig(json);
        } catch (RuntimeException exception) {
            return preserveMalformedConfig(path, exception);
        }
        loadDiagnostics = parsed.diagnostics();

        if (parsed.isFutureSchema()) {
            publishConfig(SmartDropsConfig.safeExistingFileDefaults(), PublicationKind.UPDATE);
            writesSuppressed = true;
            SmartResourceDrops.LOGGER.error(
                    "Smart Resource Multiplier config schema {} is newer than supported schema {}; "
                            + "using safe defaults without overwriting the file",
                    parsed.schemaVersion(),
                    SmartDropsConfig.CURRENT_SCHEMA);
            return false;
        }

        if ((parsed.blockRuleEntriesTruncated()
                || parsed.entityRuleEntriesTruncated()
                || parsed.shearingRuleEntriesTruncated())
                && !preserveOversizedConfig(path, parsed)) {
            writesSuppressed = true;
            SmartResourceDrops.LOGGER.error(
                    "The oversized Smart Resource Multiplier config was sanitized in memory, but a backup could not be "
                            + "created; the original file will not be overwritten");
            return false;
        }

        if (parsed.migrationRequired()
                && !parsed.blockRuleEntriesTruncated()
                && !parsed.entityRuleEntriesTruncated()
                && !parsed.shearingRuleEntriesTruncated()
                && !preserveMigratedConfig(path, parsed.schemaVersion())) {
            writesSuppressed = true;
            return false;
        }

        if (!writeCandidate(path, parsed.config(), "persist the loaded configuration")) {
            writesSuppressed = true;
            loadDiagnostics = loadDiagnostics.withWriteFailure();
            return false;
        }
        publishConfig(parsed.config(), PublicationKind.UPDATE);
        writesSuppressed = false;
        return true;
    }

    private static boolean preserveOversizedConfig(Path path, ParsedConfig parsed) {
        Path backup = path.resolveSibling(
                "smart_resource_drops.oversized-" + Instant.now().toEpochMilli() + ".json");
        try {
            Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING);
            loadDiagnostics = loadDiagnostics.withBackupFile(backup.getFileName().toString());
            SmartResourceDrops.LOGGER.warn(
                    "The config exceeded a rule budget (block {}/{}, entity {}/{}, shearing {}/{}). "
                             + "A complete copy was preserved as {}; the active file will contain bounded rules.",
                    parsed.rawBlockRuleEntries(),
                    SmartDropsConfig.MAX_BLOCK_RULE_ENTRIES,
                    parsed.rawEntityRuleEntries(),
                    SmartDropsConfig.MAX_ENTITY_RULE_ENTRIES,
                    parsed.rawShearingRuleEntries(),
                    SmartDropsConfig.MAX_SHEARING_RULE_ENTRIES,
                    backup.getFileName());
            return true;
        } catch (IOException exception) {
            SmartResourceDrops.LOGGER.error("Could not preserve an oversized configuration", exception);
            return false;
        }
    }

    private static boolean preserveMigratedConfig(final Path path, final int sourceSchema) {
        final Path backup = path.resolveSibling(
                "smart_resource_drops.schema-" + sourceSchema + "-" + Instant.now().toEpochMilli() + ".json");
        try {
            Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING);
            loadDiagnostics = loadDiagnostics.withBackupFile(backup.getFileName().toString());
            SmartResourceDrops.LOGGER.info(
                    "Preserved the pre-migration configuration as {}",
                    backup.getFileName());
            return true;
        } catch (IOException exception) {
            SmartResourceDrops.LOGGER.error(
                    "Could not preserve the pre-migration configuration; the original file was not changed",
                    exception);
            return false;
        }
    }

    private static boolean writeCandidate(
            final Path path,
            final SmartDropsConfig candidate,
            final String operation
    ) {
        try {
            AtomicConfigWriter.write(path, GSON.toJson(candidate));
            return true;
        } catch (IOException exception) {
            SmartResourceDrops.LOGGER.error("Could not " + operation + "; the active configuration was not changed", exception);
            return false;
        }
    }

    private static boolean preserveMalformedConfig(Path path, RuntimeException exception) {
        Path brokenPath = path.resolveSibling(
                "smart_resource_drops.broken-" + Instant.now().toEpochMilli() + ".json");
        loadDiagnostics = ConfigLoadDiagnostics.empty().withMalformedRecovery();
        try {
            Files.copy(path, brokenPath, StandardCopyOption.REPLACE_EXISTING);
            loadDiagnostics = loadDiagnostics.withBackupFile(brokenPath.getFileName().toString());
        } catch (IOException copyFailure) {
            writesSuppressed = true;
            SmartResourceDrops.LOGGER.error(
                    "Malformed Smart Resource Multiplier config could not be preserved; "
                            + "the original file will not be overwritten",
                    copyFailure);
            SmartResourceDrops.LOGGER.error("Rejected malformed Smart Resource Multiplier config", exception);
            return false;
        }

        final SmartDropsConfig defaults = SmartDropsConfig.safeExistingFileDefaults();
        SmartResourceDrops.LOGGER.error(
                "Malformed Smart Resource Multiplier config was preserved as " + brokenPath.getFileName()
                        + " and replaced with safe defaults",
                exception);
        if (!writeCandidate(path, defaults, "replace the malformed configuration")) {
            writesSuppressed = true;
            loadDiagnostics = loadDiagnostics.withWriteFailure();
            return false;
        }
        publishConfig(defaults, PublicationKind.UPDATE);
        writesSuppressed = false;
        return true;
    }

    public static SmartDropsConfig get() {
        return config;
    }

    public static synchronized SmartDropsConfig snapshot() {
        return GSON.fromJson(GSON.toJson(config), SmartDropsConfig.class);
    }


    public static synchronized SmartDropsConfig snapshotForClient() {
        SmartDropsConfig copy = snapshot();
        copy.playerMultipliers.clear();
        return copy;
    }

    public static synchronized String snapshotJsonForClient() {
        return clientSnapshot().json();
    }

    /** Returns serialized client state and its revision under one configuration lock. */
    public static synchronized ClientSnapshot clientSnapshot() {
        String json = cachedClientSnapshotJson;
        if (json == null) {
            json = GSON.toJson(snapshotForClient());
            cachedClientSnapshotJson = json;
        }
        return new ClientSnapshot(json, configRevision);
    }

    public static synchronized long revision() {
        return configRevision;
    }

    /** Atomically captures every bounded input used by the read-only validation command. */
    public static synchronized ValidationSnapshot validationSnapshot() {
        return new ValidationSnapshot(snapshot(), configRevision, writesSuppressed, loadDiagnostics);
    }

    public static String encodeClientPatch(ConfigPatch patch) {
        if (patch == null || !patch.hasValidShape()) {
            throw new IllegalArgumentException("Invalid configuration patch");
        }
        final String json = GSON.toJson(patch);
        if (json.length() > ConfigPatch.MAX_JSON_LENGTH) {
            throw new IllegalArgumentException("Configuration patch is too large");
        }
        return json;
    }

    /**
     * Applies only explicitly edited operator settings in one atomic file replacement.
     * Per-player rules and unrelated server settings never enter the client patch.
     */
    public static synchronized boolean applyClientPatch(String json) {
        return applyClientPatch(json, configPath());
    }

    /** Applies a client patch only when it was created from the current authoritative snapshot. */
    public static synchronized boolean applyClientPatch(String json, long expectedRevision) {
        if (expectedRevision != configRevision) {
            SmartResourceDrops.LOGGER.debug(
                    "Rejected stale configuration patch at revision {} (current revision {})",
                    expectedRevision,
                    configRevision);
            return false;
        }
        return applyClientPatch(json, configPath());
    }

    /** Applies one validated local/default GUI patch with the same atomic persistence as server edits. */
    public static synchronized boolean applyLocalPatch(final ConfigPatch patch) {
        return applyLocalPatch(patch, configPath());
    }

    /** Applies a local/default GUI patch only against the revision from which it was staged. */
    public static synchronized boolean applyLocalPatch(
            final ConfigPatch patch,
            final long expectedRevision
    ) {
        if (expectedRevision != configRevision) {
            SmartResourceDrops.LOGGER.debug(
                    "Rejected stale local configuration patch at revision {} (current revision {})",
                    expectedRevision,
                    configRevision);
            return false;
        }
        return applyLocalPatch(patch, configPath());
    }

    static synchronized boolean applyLocalPatch(
            final ConfigPatch patch,
            final long expectedRevision,
            final Path path
    ) {
        if (expectedRevision != configRevision) {
            return false;
        }
        return applyLocalPatch(patch, path);
    }

    static synchronized boolean applyLocalPatch(final ConfigPatch patch, final Path path) {
        if (patch == null || !patch.hasValidShape()) {
            return false;
        }
        try {
            return applyClientPatch(encodeClientPatch(patch), path);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    static synchronized boolean applyClientPatch(String json, Path path) {
        if (writesSuppressed
                || json == null
                || json.isBlank()
                || json.length() > ConfigPatch.MAX_JSON_LENGTH) {
            return false;
        }

        final ConfigPatch patch;
        try {
            validatePatchJsonShape(json);
            patch = GSON.fromJson(json, ConfigPatch.class);
        } catch (RuntimeException exception) {
            SmartResourceDrops.LOGGER.warn("Rejected malformed configuration patch from client", exception);
            return false;
        }
        if (patch == null
                || !patch.hasValidShape()
                || !patch.hasValuesWithinBounds(config.maximumMultiplier)
                || !hasValidPatchKeys(patch)) {
            SmartResourceDrops.LOGGER.warn("Rejected invalid configuration patch from client");
            return false;
        }

        final SmartDropsConfig candidate = snapshot();
        applyPatch(candidate, patch);
        if (candidate.blockRuleEntryCount() > SmartDropsConfig.MAX_BLOCK_RULE_ENTRIES
                || candidate.entityRuleEntryCount() > SmartDropsConfig.MAX_ENTITY_RULE_ENTRIES
                || candidate.shearingRuleEntryCount() > SmartDropsConfig.MAX_SHEARING_RULE_ENTRIES) {
            SmartResourceDrops.LOGGER.warn("Rejected configuration patch that exceeds a rule-domain limit");
            return false;
        }
        candidate.sanitize();
        if (configurationsEqual(config, candidate)) {
            return true;
        }

        try {
            AtomicConfigWriter.write(path, GSON.toJson(candidate));
        } catch (IOException exception) {
            SmartResourceDrops.LOGGER.error(
                    "Could not persist the configuration patch; the active configuration was not changed",
                    exception);
            return false;
        }
        publishConfig(candidate, PublicationKind.UPDATE);
        return true;
    }

    private static void applyPatch(SmartDropsConfig candidate, ConfigPatch patch) {
        if (patch.preset != null) {
            candidate.applyPreset(patch.preset);
        }
        if (patch.enabled != null) candidate.enabled = patch.enabled;
        if (patch.globalMultiplier != null) candidate.globalMultiplier = patch.globalMultiplier;
        if (patch.smartPlacementProtection != null) {
            candidate.smartPlacementProtection = patch.smartPlacementProtection;
        }
        if (patch.protectBlockEntities != null) candidate.protectBlockEntities = patch.protectBlockEntities;
        if (patch.playerMining != null) candidate.playerMining = patch.playerMining;
        if (patch.explosions != null) candidate.explosions = patch.explosions;
        if (patch.automatedMining != null) candidate.automatedMining = patch.automatedMining;
        if (patch.multiplyExperience != null) candidate.multiplyExperience = patch.multiplyExperience;
        if (patch.experienceMultiplier != null) candidate.experienceMultiplier = patch.experienceMultiplier;
        if (patch.conservativePistonProtection != null) {
            candidate.conservativePistonProtection = patch.conservativePistonProtection;
        }
        if (patch.allowPlayerOverrides != null) candidate.allowPlayerOverrides = patch.allowPlayerOverrides;
        if (patch.statisticsEnabled != null) candidate.statisticsEnabled = patch.statisticsEnabled;
        if (patch.filterMode != null) candidate.filterMode = patch.filterMode;
        if (patch.sourceMode != null) candidate.sourceMode = patch.sourceMode;
        if (patch.entityDropsEnabled != null) candidate.entityDropsEnabled = patch.entityDropsEnabled;
        if (patch.inheritDefaultEntityMultiplier != null) {
            candidate.inheritDefaultEntityMultiplier = patch.inheritDefaultEntityMultiplier;
        }
        if (patch.defaultEntityMultiplier != null) {
            candidate.defaultEntityMultiplier = patch.defaultEntityMultiplier;
        }
        if (patch.entityKillRequirement != null) candidate.entityKillRequirement = patch.entityKillRequirement;
        if (patch.entityFilterMode != null) candidate.entityFilterMode = patch.entityFilterMode;
        if (patch.bossDropsEnabled != null) candidate.bossDropsEnabled = patch.bossDropsEnabled;
        if (patch.multiplyMobExperience != null) candidate.multiplyMobExperience = patch.multiplyMobExperience;
        if (patch.mobExperienceMultiplier != null) {
            candidate.mobExperienceMultiplier = patch.mobExperienceMultiplier;
        }
        if (patch.multiplyBossExperience != null) {
            candidate.multiplyBossExperience = patch.multiplyBossExperience;
        }
        if (patch.manualShearingDropsEnabled != null) {
            candidate.manualShearingDropsEnabled = patch.manualShearingDropsEnabled;
        }
        if (patch.automatedShearingDropsEnabled != null) {
            candidate.automatedShearingDropsEnabled = patch.automatedShearingDropsEnabled;
        }
        if (patch.inheritDefaultShearingMultiplier != null) {
            candidate.inheritDefaultShearingMultiplier = patch.inheritDefaultShearingMultiplier;
        }
        if (patch.defaultShearingMultiplier != null) {
            candidate.defaultShearingMultiplier = patch.defaultShearingMultiplier;
        }

        applyMapPatch(candidate.blockMultipliers, patch.blockMultipliers, patch.inheritedBlocks, false);
        applyMapPatch(candidate.categoryMultipliers, patch.categoryMultipliers, patch.inheritedCategories, true);
        applyMapPatch(candidate.dimensionMultipliers, patch.dimensionMultipliers, patch.inheritedDimensions, false);
        patch.blockFilters.forEach((rawKey, state) -> {
            final String key = normalizedIdentifier(rawKey);
            candidate.blacklist.remove(key);
            candidate.whitelist.remove(key);
            switch (state) {
                case NONE -> {
                }
                case WHITELIST -> candidate.whitelist.add(key);
                case BLACKLIST -> candidate.blacklist.add(key);
            }
        });
        applyMapPatch(
                candidate.entityMultipliers,
                patch.entityMultipliers,
                patch.inheritedEntities,
                false);
        patch.inheritedEntityCategories.forEach(rawKey ->
                candidate.entityCategoryMultipliers.remove(normalizedEntityCategory(rawKey)));
        patch.entityCategoryMultipliers.forEach((rawKey, value) ->
                candidate.entityCategoryMultipliers.put(normalizedEntityCategory(rawKey), value));
        applyFilterPatch(candidate.entityBlacklist, candidate.entityWhitelist, patch.entityFilters, false);
        applyFilterPatch(
                candidate.entityTagBlacklist,
                candidate.entityTagWhitelist,
                patch.entityTagFilters,
                true);
        applyMapPatch(
                candidate.shearingEntityMultipliers,
                patch.shearingEntityMultipliers,
                patch.inheritedShearingEntities,
                false);
    }

    private static void applyFilterPatch(
            final Set<String> blacklist,
            final Set<String> whitelist,
            final Map<String, ConfigPatch.FilterEntryState> changes,
            final boolean tag
    ) {
        changes.forEach((rawKey, state) -> {
            final String key = tag ? normalizedTagIdentifier(rawKey) : normalizedEntityIdentifier(rawKey);
            blacklist.remove(key);
            whitelist.remove(key);
            switch (state) {
                case NONE -> {
                }
                case WHITELIST -> whitelist.add(key);
                case BLACKLIST -> blacklist.add(key);
            }
        });
    }

    private static void applyMapPatch(
            Map<String, Integer> destination,
            Map<String, Integer> values,
            Set<String> inherited,
            boolean category
    ) {
        inherited.forEach(rawKey -> destination.remove(normalizedPatchKey(rawKey, category)));
        values.forEach((rawKey, value) -> destination.put(normalizedPatchKey(rawKey, category), value));
    }

    private static boolean hasValidPatchKeys(ConfigPatch patch) {
        return validKeys(patch.blockMultipliers.keySet(), false)
                && validKeys(patch.inheritedBlocks, false)
                && validKeys(patch.categoryMultipliers.keySet(), true)
                && validKeys(patch.inheritedCategories, true)
                && validKeys(patch.dimensionMultipliers.keySet(), false)
                && validKeys(patch.inheritedDimensions, false)
                && validKeys(patch.blockFilters.keySet(), false)
                && validEntityKeys(patch.entityMultipliers.keySet())
                && validEntityKeys(patch.inheritedEntities)
                && validEntityCategoryKeys(patch.entityCategoryMultipliers.keySet())
                && validEntityCategoryKeys(patch.inheritedEntityCategories)
                && validEntityKeys(patch.entityFilters.keySet())
                && validTagKeys(patch.entityTagFilters.keySet())
                && validEntityKeys(patch.shearingEntityMultipliers.keySet())
                && validEntityKeys(patch.inheritedShearingEntities);
    }

    private static boolean validKeys(Set<String> keys, boolean category) {
        return keys.stream().allMatch(key -> normalizedPatchKey(key, category) != null);
    }

    private static boolean validEntityKeys(final Set<String> keys) {
        return keys.stream().allMatch(key -> normalizedEntityIdentifier(key) != null);
    }

    private static boolean validEntityCategoryKeys(final Set<String> keys) {
        return keys.stream().allMatch(key -> normalizedEntityCategory(key) != null);
    }

    private static boolean validTagKeys(final Set<String> keys) {
        return keys.stream().allMatch(key -> normalizedTagIdentifier(key) != null);
    }

    private static String normalizedPatchKey(String raw, boolean category) {
        if (raw == null) {
            return null;
        }
        final String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty() || normalized.length() > SmartDropsConfig.MAX_RULE_KEY_LENGTH) {
            return null;
        }
        if (category) {
            return Category.parse(normalized).map(Category::key).orElse(null);
        }
        return normalizedIdentifier(normalized);
    }

    private static String normalizedIdentifier(String raw) {
        if (raw == null) {
            return null;
        }
        final String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty() || normalized.length() > SmartDropsConfig.MAX_RULE_KEY_LENGTH) {
            return null;
        }
        final Identifier identifier = Identifier.tryParse(normalized);
        return identifier == null ? null : identifier.toString();
    }

    private static String normalizedEntityIdentifier(final String raw) {
        final String normalized = normalizedIdentifier(raw);
        return normalized == null || "minecraft:player".equals(normalized) ? null : normalized;
    }

    private static String normalizedTagIdentifier(final String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        while (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        return normalizedIdentifier(normalized);
    }

    private static String normalizedEntityCategory(final String raw) {
        return EntityCategory.parse(raw).map(EntityCategory::key).orElse(null);
    }

    private static void validatePatchJsonShape(final String json) {
        final JsonElement root = JsonParser.parseString(json);
        if (!root.isJsonObject()) {
            throw new JsonParseException("Configuration patch root must be an object");
        }
        final JsonObject object = root.getAsJsonObject();
        for (String field : new String[]{
                "enabled",
                "smartPlacementProtection",
                "protectBlockEntities",
                "playerMining",
                "explosions",
                "automatedMining",
                "multiplyExperience",
                "conservativePistonProtection",
                "allowPlayerOverrides",
                "statisticsEnabled",
                "entityDropsEnabled",
                "inheritDefaultEntityMultiplier",
                "bossDropsEnabled",
                "multiplyMobExperience",
                "multiplyBossExperience",
                "manualShearingDropsEnabled",
                "automatedShearingDropsEnabled",
                "inheritDefaultShearingMultiplier"
        }) {
            validateBooleanField(object, field);
        }
        for (String field : new String[]{
                "globalMultiplier",
                "experienceMultiplier",
                "defaultEntityMultiplier",
                "mobExperienceMultiplier",
                "defaultShearingMultiplier"
        }) {
            validateIntegerField(object, field);
        }
        validateEnumField(object, "preset", SmartDropsConfig.Preset.class);
        validateEnumField(object, "filterMode", SmartDropsConfig.FilterMode.class);
        validateEnumField(object, "sourceMode", SmartDropsConfig.SourceMode.class);
        validateEnumField(object, "entityFilterMode", SmartDropsConfig.FilterMode.class);
        validateEnumField(
                object,
                "entityKillRequirement",
                SmartDropsConfig.EntityKillRequirement.class);
    }

    private static void validateBooleanField(final JsonObject object, final String field) {
        if (!object.has(field)) {
            return;
        }
        final JsonElement value = object.get(field);
        if (value == null
                || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isBoolean()) {
            throw new JsonParseException(field + " must be a boolean");
        }
    }

    private static void validateIntegerField(final JsonObject object, final String field) {
        if (!object.has(field)) {
            return;
        }
        final JsonElement value = object.get(field);
        if (value == null
                || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isNumber()) {
            throw new JsonParseException(field + " must be an integer");
        }
        try {
            value.getAsBigDecimal().intValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new JsonParseException(field + " must be an integer", exception);
        }
    }

    private static <E extends Enum<E>> void validateEnumField(
            final JsonObject object,
            final String field,
            final Class<E> type
    ) {
        if (!object.has(field)) {
            return;
        }
        final JsonElement value = object.get(field);
        if (value == null
                || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isString()) {
            throw new JsonParseException(field + " must be a string enum value");
        }
        try {
            Enum.valueOf(type, value.getAsString());
        } catch (IllegalArgumentException exception) {
            throw new JsonParseException("Unknown " + field + " value", exception);
        }
    }

    public static Optional<SmartDropsConfig> tryParseSnapshotJson(String json) {
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            ParsedConfig stored = parseStoredConfig(json);
            if (stored.isFutureSchema()) {
                SmartResourceDrops.LOGGER.warn(
                        "Rejected configuration snapshot with unsupported schema {}",
                        stored.schemaVersion());
                return Optional.empty();
            }
            SmartDropsConfig parsed = stored.config();
            parsed.playerMultipliers.clear();
            return Optional.of(parsed);
        } catch (RuntimeException exception) {
            SmartResourceDrops.LOGGER.warn("Rejected malformed configuration snapshot from server", exception);
            return Optional.empty();
        }
    }

    /** Compatibility helper for non-authoritative callers that explicitly want safe defaults on failure. */
    public static SmartDropsConfig parseSnapshotJson(String json) {
        return tryParseSnapshotJson(json).orElseGet(SmartDropsConfig::safeExistingFileDefaults);
    }

    public static synchronized boolean update(Consumer<SmartDropsConfig> update) {
        return update(update, configPath());
    }

    static synchronized boolean update(Consumer<SmartDropsConfig> update, Path path) {
        if (writesSuppressed) {
            SmartResourceDrops.LOGGER.error(
                    "Configuration update rejected because writes are suppressed; resolve or reset the active file");
            return false;
        }
        SmartDropsConfig copy = snapshot();
        update.accept(copy);
        if (copy.rawBlockRuleEntryCount() > SmartDropsConfig.MAX_BLOCK_RULE_ENTRIES
                || copy.rawEntityRuleEntryCount() > SmartDropsConfig.MAX_ENTITY_RULE_ENTRIES
                || copy.rawShearingRuleEntryCount() > SmartDropsConfig.MAX_SHEARING_RULE_ENTRIES) {
            SmartResourceDrops.LOGGER.warn(
                    "Rejected configuration update that exceeds a rule-domain limit; no rules were displaced");
            return false;
        }
        copy.sanitize();
        if (configurationsEqual(config, copy)) {
            return true;
        }
        try {
            AtomicConfigWriter.write(path, GSON.toJson(copy));
        } catch (IOException exception) {
            SmartResourceDrops.LOGGER.error(
                    "Could not persist the configuration update; the active configuration was not changed",
                    exception);
            return false;
        }
        publishConfig(copy, PublicationKind.UPDATE);
        return true;
    }

    public static synchronized boolean reset() {
        return reset(configPath());
    }

    /** Resets only when the confirmation was based on the current authoritative snapshot. */
    public static synchronized boolean reset(final long expectedRevision) {
        if (expectedRevision != configRevision) {
            SmartResourceDrops.LOGGER.debug(
                    "Rejected stale configuration reset at revision {} (current revision {})",
                    expectedRevision,
                    configRevision);
            return false;
        }
        return reset(configPath());
    }

    static synchronized boolean reset(final Path path) {
        final SmartDropsConfig defaults = SmartDropsConfig.defaults();
        try {
            AtomicConfigWriter.write(path, GSON.toJson(defaults));
        } catch (IOException exception) {
            SmartResourceDrops.LOGGER.error(
                    "Could not persist the configuration reset; the active configuration was not changed",
                    exception);
            return false;
        }
        publishConfig(defaults, PublicationKind.RESET);
        writesSuppressed = false;
        return true;
    }

    public static synchronized boolean save() {
        return save(configPath());
    }

    static synchronized boolean save(Path path) {
        if (writesSuppressed) {
            return false;
        }
        final SmartDropsConfig candidate = snapshot();
        candidate.sanitize();
        try {
            AtomicConfigWriter.write(path, GSON.toJson(candidate));
        } catch (IOException exception) {
            SmartResourceDrops.LOGGER.error("Could not save Smart Resource Multiplier config", exception);
            return false;
        }
        publishConfig(candidate, PublicationKind.UPDATE);
        return true;
    }

    private static void invalidateClientSnapshot() {
        cachedClientSnapshotJson = null;
    }

    private static void publishConfig(
            final SmartDropsConfig replacement,
            final PublicationKind kind
    ) {
        config = replacement;
        configRevision++;
        invalidateClientSnapshot();
        try {
            publicationListener.onPublished(configRevision, kind);
        } catch (RuntimeException exception) {
            SmartResourceDrops.LOGGER.error(
                    "Configuration was published, but editor invalidation notification failed",
                    exception);
        }
    }

    /** Installs the process-local hook used by the server networking layer for editor invalidation. */
    public static void setPublicationListener(final PublicationListener listener) {
        publicationListener = listener == null ? (revision, kind) -> {
        } : listener;
    }

    static ParsedConfig parseStoredConfig(String json) {
        JsonElement root = JsonParser.parseString(json);
        if (!root.isJsonObject()) {
            throw new JsonParseException("Configuration root must be a JSON object");
        }

        JsonObject object = root.getAsJsonObject();
        final ConfigLoadDiagnostics.Builder diagnostics = new ConfigLoadDiagnostics.Builder();
        int schemaVersion = 1;
        if (object.has("schemaVersion")) {
            JsonElement schemaElement = object.get("schemaVersion");
            if (schemaElement == null
                    || !schemaElement.isJsonPrimitive()
                    || !schemaElement.getAsJsonPrimitive().isNumber()) {
                throw new JsonParseException("schemaVersion must be an integer");
            }
            try {
                schemaVersion = schemaElement.getAsBigDecimal().intValueExact();
            } catch (ArithmeticException | NumberFormatException exception) {
                throw new JsonParseException("schemaVersion must be an integer", exception);
            }
        }

        if (schemaVersion > SmartDropsConfig.CURRENT_SCHEMA) {
            diagnostics.unsupportedSchema();
            return new ParsedConfig(
                    null,
                    schemaVersion,
                    0,
                    0,
                    0,
                    false,
                    false,
                    false,
                    false,
                    diagnostics.build());
        }

        final boolean migrateEntityDomain = schemaVersion < 2;
        final boolean migrateShearingDomain = schemaVersion < 3;
        final boolean migrationRequired = migrateEntityDomain || migrateShearingDomain;
        if (migrationRequired) {
            diagnostics.migratedFrom(schemaVersion);
        }
        final JsonObject decodedObject;
        if (migrationRequired) {
            decodedObject = object.deepCopy();
            if (migrateEntityDomain) {
                SCHEMA_TWO_ENTITY_FIELDS.forEach(decodedObject::remove);
            }
            if (migrateShearingDomain) {
                SCHEMA_THREE_SHEARING_FIELDS.forEach(decodedObject::remove);
            }
        } else {
            decodedObject = object;
        }

        SmartDropsConfig loaded = GSON.fromJson(decodedObject, SmartDropsConfig.class);
        if (loaded == null) {
            throw new JsonParseException("Configuration object could not be decoded");
        }
        if (!object.has("blacklist") || object.get("blacklist").isJsonNull()) {
            loaded.installSafetyBlacklist();
        }
        if (migrateEntityDomain) {
            resetEntitySettingsForMigration(loaded);
        }
        if (migrateShearingDomain) {
            resetShearingSettingsForMigration(loaded);
        }
        final int rawBlockRuleEntries = loaded.rawBlockRuleEntryCount();
        final int rawEntityRuleEntries = loaded.rawEntityRuleEntryCount();
        final int rawShearingRuleEntries = loaded.rawShearingRuleEntryCount();
        final boolean blockRuleEntriesTruncated = rawBlockRuleEntries > SmartDropsConfig.MAX_BLOCK_RULE_ENTRIES;
        final boolean entityRuleEntriesTruncated = rawEntityRuleEntries > SmartDropsConfig.MAX_ENTITY_RULE_ENTRIES;
        final boolean shearingRuleEntriesTruncated =
                rawShearingRuleEntries > SmartDropsConfig.MAX_SHEARING_RULE_ENTRIES;
        diagnostics.ruleBudgets(rawBlockRuleEntries, rawEntityRuleEntries, rawShearingRuleEntries);
        loaded.sanitize(diagnostics);
        loaded.schemaVersion = SmartDropsConfig.CURRENT_SCHEMA;
        return new ParsedConfig(
                loaded,
                schemaVersion,
                rawBlockRuleEntries,
                rawEntityRuleEntries,
                rawShearingRuleEntries,
                blockRuleEntriesTruncated,
                entityRuleEntriesTruncated,
                shearingRuleEntriesTruncated,
                migrationRequired,
                diagnostics.build());
    }

    /**
     * Schema-1 files predate entity drops. Ignore any coincidentally named fields so an upgrade can
     * never enable or relax the new subsystem without an explicit schema-2 edit.
     */
    private static void resetEntitySettingsForMigration(final SmartDropsConfig loaded) {
        final SmartDropsConfig safe = SmartDropsConfig.defaults();
        loaded.entityDropsEnabled = safe.entityDropsEnabled;
        loaded.inheritDefaultEntityMultiplier = safe.inheritDefaultEntityMultiplier;
        loaded.defaultEntityMultiplier = safe.defaultEntityMultiplier;
        loaded.entityKillRequirement = safe.entityKillRequirement;
        loaded.entityFilterMode = safe.entityFilterMode;
        loaded.bossDropsEnabled = safe.bossDropsEnabled;
        loaded.multiplyMobExperience = safe.multiplyMobExperience;
        loaded.mobExperienceMultiplier = safe.mobExperienceMultiplier;
        loaded.multiplyBossExperience = safe.multiplyBossExperience;
        loaded.entityCategoryMultipliers = new java.util.LinkedHashMap<>(safe.entityCategoryMultipliers);
        loaded.entityMultipliers = new java.util.LinkedHashMap<>();
        loaded.entityBlacklist = new java.util.LinkedHashSet<>();
        loaded.entityWhitelist = new java.util.LinkedHashSet<>();
        loaded.entityTagBlacklist = new java.util.LinkedHashSet<>();
        loaded.entityTagWhitelist = new java.util.LinkedHashSet<>();
    }

    /** Schema-1 and schema-2 files predate shearing and must remain behaviorally unchanged. */
    private static void resetShearingSettingsForMigration(final SmartDropsConfig loaded) {
        final SmartDropsConfig safe = SmartDropsConfig.safeExistingFileDefaults();
        loaded.manualShearingDropsEnabled = safe.manualShearingDropsEnabled;
        loaded.automatedShearingDropsEnabled = safe.automatedShearingDropsEnabled;
        loaded.inheritDefaultShearingMultiplier = safe.inheritDefaultShearingMultiplier;
        loaded.defaultShearingMultiplier = safe.defaultShearingMultiplier;
        loaded.shearingEntityMultipliers = new java.util.LinkedHashMap<>();
    }

    static boolean configurationsEqual(SmartDropsConfig first, SmartDropsConfig second) {
        return GSON.toJsonTree(first).equals(GSON.toJsonTree(second));
    }

    record ParsedConfig(
            SmartDropsConfig config,
            int schemaVersion,
            int rawBlockRuleEntries,
            int rawEntityRuleEntries,
            int rawShearingRuleEntries,
            boolean blockRuleEntriesTruncated,
            boolean entityRuleEntriesTruncated,
            boolean shearingRuleEntriesTruncated,
            boolean migrationRequired,
            ConfigLoadDiagnostics diagnostics
    ) {
        boolean isFutureSchema() {
            return config == null;
        }
    }

    public record ClientSnapshot(String json, long revision) {
    }

    public record ValidationSnapshot(
            SmartDropsConfig config,
            long revision,
            boolean writesSuppressed,
            ConfigLoadDiagnostics loadDiagnostics
    ) {
        public ValidationSnapshot {
            if (config == null) {
                throw new IllegalArgumentException("Validation config cannot be null");
            }
            loadDiagnostics = loadDiagnostics == null ? ConfigLoadDiagnostics.empty() : loadDiagnostics;
        }
    }

    public enum PublicationKind {
        UPDATE,
        RESET
    }

    @FunctionalInterface
    public interface PublicationListener {
        void onPublished(long revision, PublicationKind kind);
    }

    public static Path path() {
        return configPath();
    }

    /** Installs the loader's configuration directory before the common initializer loads the file. */
    public static synchronized void configureConfigDirectory(final Path directory) {
        if (directory == null) {
            throw new IllegalArgumentException("Configuration directory cannot be null");
        }
        configPath = directory.resolve("smart_resource_drops.json");
    }

    private static Path configPath() {
        Path path = configPath;
        if (path == null) {
            synchronized (ConfigManager.class) {
                path = configPath;
                if (path == null) {
                    path = Path.of("config", "smart_resource_drops.json");
                    configPath = path;
                }
            }
        }
        return path;
    }
}
