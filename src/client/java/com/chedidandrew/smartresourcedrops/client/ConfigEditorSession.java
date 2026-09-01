package com.chedidandrew.smartresourcedrops.client;

import com.chedidandrew.smartresourcedrops.config.ConfigPatch;
import com.chedidandrew.smartresourcedrops.config.ConfigScreenOpenPolicy;
import com.chedidandrew.smartresourcedrops.config.SmartDropsConfig;
import com.chedidandrew.smartresourcedrops.core.Category;
import com.chedidandrew.smartresourcedrops.core.SmartDropTags;
import com.chedidandrew.smartresourcedrops.core.entity.EntityCategory;
import com.chedidandrew.smartresourcedrops.core.entity.EntityClassifier;
import com.chedidandrew.smartresourcedrops.core.entity.EntityDropTags;
import com.chedidandrew.smartresourcedrops.core.shearing.ShearingClassification;
import com.chedidandrew.smartresourcedrops.core.shearing.ShearingRuleResolver;
import com.chedidandrew.smartresourcedrops.core.shearing.ShearingRuleTrace;
import com.chedidandrew.smartresourcedrops.core.shearing.ShearingSource;
import com.chedidandrew.smartresourcedrops.core.shearing.ShearingTags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Shared, staged state for the configuration screen hierarchy.
 *
 * <p>The session deliberately exposes mutations through methods instead of exposing its working
 * config directly. This keeps read-only server snapshots read-only and lets every child screen
 * share one semantic dirty state.</p>
 */
public final class ConfigEditorSession {
    private static final List<String> VANILLA_DIMENSIONS = List.of(
            "minecraft:overworld",
            "minecraft:the_nether",
            "minecraft:the_end");

    private static final Comparator<BlockInfo> BLOCK_ORDER = Comparator
            .comparing((BlockInfo info) -> info.displayName().toLowerCase(Locale.ROOT))
            .thenComparing(BlockInfo::id);
    private static final Comparator<EntityInfo> ENTITY_ORDER = Comparator
            .comparing((EntityInfo info) -> info.displayName().toLowerCase(Locale.ROOT))
            .thenComparing(EntityInfo::id);
    private static final Comparator<ShearingInfo> SHEARING_ORDER = Comparator
            .comparing((ShearingInfo info) -> info.displayName().toLowerCase(Locale.ROOT))
            .thenComparing(ShearingInfo::id);
    private static final int SEARCH_CACHE_LIMIT = 32;

    private final Screen originalParent;
    private final ConfigScreenOpenPolicy.Authority authority;
    private final boolean editable;
    private final long revision;
    private final Object connectionToken;
    private final SmartDropsConfig baseline;
    private final SmartDropsConfig working;
    private final String currentDimensionId;
    private final LinkedHashSet<String> dimensionIds = new LinkedHashSet<>();
    private final List<Category> categories = List.of(Category.values());
    private final List<EntityCategory> entityCategories = List.of(EntityCategory.values());
    private final Map<String, List<BlockInfo>> blockSearchCache = new LinkedHashMap<>();
    private final Map<String, List<BlockInfo>> filterBlockSearchCache = new LinkedHashMap<>();
    private final Map<String, List<String>> filterTagSearchCache = new LinkedHashMap<>();
    private final Map<String, List<EntityInfo>> entitySearchCache = new LinkedHashMap<>();
    private final Map<String, List<EntityInfo>> entityFilterSearchCache = new LinkedHashMap<>();
    private final Map<String, List<String>> entityTagFilterSearchCache = new LinkedHashMap<>();
    private Catalog catalog;
    private EntityCatalog entityCatalog;
    private ShearingCatalog localShearingCatalog;
    private String status;
    private SmartDropsConfig.Preset selectedPreset;
    private long latestKnownServerRevision;

    public ConfigEditorSession(
            final Screen originalParent,
            final SmartDropsConfig snapshot,
            final boolean editable,
            final String status,
            final ConfigScreenOpenPolicy.Authority authority,
            final long revision
    ) {
        this(originalParent, snapshot, editable, status, authority, revision, Minecraft.getInstance());
    }

    /** Compatibility seam for callers that do not participate in revision-guarded networking. */
    public ConfigEditorSession(
            final Screen originalParent,
            final SmartDropsConfig snapshot,
            final boolean editable,
            final String status,
            final ConfigScreenOpenPolicy.Authority authority
    ) {
        this(originalParent, snapshot, editable, status, authority, 0L);
    }

    /** Package-visible context seam used by client tests that do not have a live level. */
    ConfigEditorSession(
            final Screen originalParent,
            final SmartDropsConfig snapshot,
            final boolean editable,
            final String status,
            final ConfigScreenOpenPolicy.Authority authority,
            final long revision,
            final Minecraft minecraft
    ) {
        this.originalParent = originalParent;
        this.authority = Objects.requireNonNull(authority, "authority");
        this.editable = editable;
        this.revision = Math.max(0L, revision);
        this.connectionToken = authority == ConfigScreenOpenPolicy.Authority.CONNECTED_SERVER
                && minecraft != null
                ? minecraft.getConnection()
                : null;
        this.status = status == null ? "" : status;
        this.latestKnownServerRevision = this.revision;
        this.baseline = copyOf(Objects.requireNonNull(snapshot, "snapshot"));
        this.working = copyOf(snapshot);

        this.currentDimensionId = currentDimensionId(minecraft);
        collectDimensionIds(minecraft);
    }

    /** Compatibility seam for client tests that predate revision-guarded mutations. */
    ConfigEditorSession(
            final Screen originalParent,
            final SmartDropsConfig snapshot,
            final boolean editable,
            final String status,
            final ConfigScreenOpenPolicy.Authority authority,
            final Minecraft minecraft
    ) {
        this(originalParent, snapshot, editable, status, authority, 0L, minecraft);
    }

    public Screen originalParent() {
        return originalParent;
    }

    public ConfigScreenOpenPolicy.Authority authority() {
        return authority;
    }

    public boolean editable() {
        return editable;
    }

    /** Authoritative server/config generation from which this staged editor was created. */
    public long revision() {
        return revision;
    }

    /** Marks a connected draft stale without discarding any of its staged values. */
    public boolean markServerRevisionAdvanced(final long serverRevision) {
        if (this.authority != ConfigScreenOpenPolicy.Authority.CONNECTED_SERVER
                || serverRevision <= this.latestKnownServerRevision) {
            return false;
        }
        this.latestKnownServerRevision = serverRevision;
        return true;
    }

    public boolean serverRevisionStale() {
        return this.latestKnownServerRevision > this.revision;
    }

    public long latestKnownServerRevision() {
        return this.latestKnownServerRevision;
    }

    /** Prevents an editor opened on one connection from mutating a later server session. */
    public boolean belongsToCurrentConnection(final Minecraft minecraft) {
        return this.authority != ConfigScreenOpenPolicy.Authority.CONNECTED_SERVER
                || minecraft != null
                && this.connectionToken != null
                && this.connectionToken == minecraft.getConnection();
    }

    public String status() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status == null ? "" : status;
    }

    public void clearStatus() {
        this.status = "";
    }

    public String currentDimensionId() {
        return currentDimensionId;
    }

    public List<String> dimensionIds() {
        return List.copyOf(dimensionIds);
    }

    public List<Category> categories() {
        return categories;
    }

    public List<EntityCategory> entityCategories() {
        return entityCategories;
    }

    public List<BlockInfo> blockCatalog() {
        return this.catalog().blocks();
    }

    public Optional<BlockInfo> blockInfo(final String blockId) {
        return Optional.ofNullable(this.catalog().byId().get(normalizeKey(blockId)));
    }

    public List<BlockInfo> categoryBlocks(final Category category) {
        return category == null
                ? List.of()
                : this.catalog().byCategory().getOrDefault(category, List.of());
    }

    public List<BlockInfo> categoryBlocks(final String categoryKey) {
        return Category.parse(categoryKey).map(this::categoryBlocks).orElseGet(List::of);
    }

    public int categoryBlockCount(final Category category) {
        return categoryBlocks(category).size();
    }

    public int categoryBlockCount(final String categoryKey) {
        return categoryBlocks(categoryKey).size();
    }

    /**
     * Searches the runtime block catalog. An empty query intentionally returns only configured
     * block multiplier overrides, never the entire registry.
     */
    public List<BlockInfo> searchBlocks(final String query) {
        final String normalized = normalizeQuery(query);
        return cachedSearch(blockSearchCache, normalized, this::computeBlockSearch);
    }

    /** Empty filter searches show only explicitly listed block filters. */
    public List<BlockInfo> searchFilterBlocks(final String query) {
        final String normalized = normalizeQuery(query);
        return cachedSearch(filterBlockSearchCache, normalized, this::computeFilterBlockSearch);
    }

    /** Empty tag searches show configured tag filters; non-empty searches include runtime tags. */
    public List<String> searchFilterTags(final String query) {
        final String normalized = normalizeTagQuery(query);
        return cachedSearch(filterTagSearchCache, normalized, this::computeFilterTagSearch);
    }

    public List<String> runtimeTagIds() {
        return this.catalog().tagIds();
    }

    public List<EntityInfo> entityCatalog() {
        return this.entityCatalogInternal().entities();
    }

    public Optional<EntityInfo> entityInfo(final String entityId) {
        return Optional.ofNullable(this.entityCatalogInternal().byId().get(normalizeKey(entityId)));
    }

    public List<EntityInfo> categoryEntities(final EntityCategory category) {
        return category == null
                ? List.of()
                : this.entityCatalogInternal().byCategory().getOrDefault(category, List.of());
    }

    public int categoryEntityCount(final EntityCategory category) {
        return categoryEntities(category).size();
    }

    public long estimatedCategoryEntityCount(final EntityCategory category) {
        return categoryEntities(category).stream().filter(EntityInfo::categoryEstimated).count();
    }

    /** Empty searches show configured exact overrides; populated searches use cached metadata. */
    public List<EntityInfo> searchEntities(final String query) {
        final String normalized = normalizeQuery(query);
        return cachedSearch(entitySearchCache, normalized, this::computeEntitySearch);
    }

    /** Empty searches show configured exact filters; populated searches use the entity catalog. */
    public List<EntityInfo> searchEntityFilterTargets(final String query) {
        final String normalized = normalizeQuery(query);
        return cachedSearch(entityFilterSearchCache, normalized, this::computeEntityFilterSearch);
    }

    /** Empty searches show configured tag filters; populated searches use runtime type tags. */
    public List<String> searchEntityFilterTags(final String query) {
        final String normalized = normalizeTagQuery(query);
        return cachedSearch(entityTagFilterSearchCache, normalized, this::computeEntityTagFilterSearch);
    }

    /**
     * Searches the bounded shearing catalog. An empty query deliberately exposes only exact
     * overrides already present in the draft, never a complete entity registry dump.
     */
    public List<ShearingInfo> searchShearingEntities(final String query) {
        final String normalized = normalizeQuery(query);
        // Certification tags are reloadable server data. Rebuild this deliberately small,
        // metadata-only catalog for each search instead of retaining stale membership.
        return this.computeShearingSearch(normalized);
    }

    public Optional<ShearingInfo> shearingInfo(final String entityId) {
        final String key = normalizeEntityId(entityId);
        if (key.isEmpty()) {
            return Optional.empty();
        }
        final ShearingCatalog current = this.shearingCatalogInternal();
        return Optional.of(current.byId().getOrDefault(key, syntheticShearingInfo(key)));
    }

    public SmartDropsConfig baselineSnapshot() {
        return copyOf(baseline);
    }

    public SmartDropsConfig workingSnapshot() {
        return copyOf(working);
    }

    public int maximumMultiplier() {
        return working.maximumMultiplier;
    }

    public int maxPlayerMultiplier() {
        return working.maxPlayerMultiplier;
    }

    public int globalMultiplier() {
        return working.globalMultiplier;
    }

    public boolean setGlobalMultiplier(final int value) {
        return setInt(
                working.globalMultiplier,
                SmartDropsConfig.clamp(value, 0, working.maximumMultiplier),
                next -> working.globalMultiplier = next);
    }

    public boolean enabled() {
        return working.enabled;
    }

    public boolean setEnabled(final boolean value) {
        return setBoolean(working.enabled, value, next -> working.enabled = next);
    }

    public boolean smartPlacementProtection() {
        return working.smartPlacementProtection;
    }

    public boolean setSmartPlacementProtection(final boolean value) {
        return setBoolean(
                working.smartPlacementProtection,
                value,
                next -> working.smartPlacementProtection = next);
    }

    public boolean protectBlockEntities() {
        return working.protectBlockEntities;
    }

    public boolean setProtectBlockEntities(final boolean value) {
        return setBoolean(working.protectBlockEntities, value, next -> working.protectBlockEntities = next);
    }

    public boolean playerMining() {
        return working.playerMining;
    }

    public boolean setPlayerMining(final boolean value) {
        return setBoolean(working.playerMining, value, next -> working.playerMining = next);
    }

    public boolean explosions() {
        return working.explosions;
    }

    public boolean setExplosions(final boolean value) {
        return setBoolean(working.explosions, value, next -> working.explosions = next);
    }

    public boolean automatedMining() {
        return working.automatedMining;
    }

    public boolean setAutomatedMining(final boolean value) {
        return setBoolean(working.automatedMining, value, next -> working.automatedMining = next);
    }

    public boolean multiplyExperience() {
        return working.multiplyExperience;
    }

    public boolean setMultiplyExperience(final boolean value) {
        return setBoolean(working.multiplyExperience, value, next -> working.multiplyExperience = next);
    }

    public int experienceMultiplier() {
        return working.experienceMultiplier;
    }

    public boolean setExperienceMultiplier(final int value) {
        return setInt(
                working.experienceMultiplier,
                SmartDropsConfig.clamp(value, 1, working.maximumMultiplier),
                next -> working.experienceMultiplier = next);
    }

    public boolean conservativePistonProtection() {
        return working.conservativePistonProtection;
    }

    public boolean setConservativePistonProtection(final boolean value) {
        return setBoolean(
                working.conservativePistonProtection,
                value,
                next -> working.conservativePistonProtection = next);
    }

    public boolean allowPlayerOverrides() {
        return working.allowPlayerOverrides;
    }

    public boolean setAllowPlayerOverrides(final boolean value) {
        return setBoolean(working.allowPlayerOverrides, value, next -> working.allowPlayerOverrides = next);
    }

    public boolean statisticsEnabled() {
        return working.statisticsEnabled;
    }

    public boolean setStatisticsEnabled(final boolean value) {
        return setBoolean(working.statisticsEnabled, value, next -> working.statisticsEnabled = next);
    }

    public SmartDropsConfig.FilterMode filterMode() {
        return working.filterMode;
    }

    public boolean setFilterMode(final SmartDropsConfig.FilterMode value) {
        if (!editable || value == null || value == working.filterMode) {
            return false;
        }
        working.filterMode = value;
        return true;
    }

    public SmartDropsConfig.SourceMode sourceMode() {
        return working.sourceMode;
    }

    public boolean setSourceMode(final SmartDropsConfig.SourceMode value) {
        if (!editable || value == null || value == working.sourceMode) {
            return false;
        }
        working.sourceMode = value;
        return true;
    }

    public boolean entityDropsEnabled() {
        return working.entityDropsEnabled;
    }

    public boolean setEntityDropsEnabled(final boolean value) {
        return setBoolean(working.entityDropsEnabled, value, next -> working.entityDropsEnabled = next);
    }

    /** Null means the entity default inherits the global block/resource multiplier. */
    public Integer defaultEntityMultiplier() {
        return working.inheritDefaultEntityMultiplier ? null : working.defaultEntityMultiplier;
    }

    public boolean setDefaultEntityMultiplier(final Integer value) {
        if (!editable) {
            return false;
        }
        if (value == null) {
            if (working.inheritDefaultEntityMultiplier) {
                return false;
            }
            working.inheritDefaultEntityMultiplier = true;
            return true;
        }
        final int clamped = SmartDropsConfig.clamp(value, 0, working.maximumMultiplier);
        final boolean changed = working.inheritDefaultEntityMultiplier
                || working.defaultEntityMultiplier != clamped;
        if (changed) {
            working.inheritDefaultEntityMultiplier = false;
            working.defaultEntityMultiplier = clamped;
        }
        return changed;
    }

    public int effectiveDefaultEntityMultiplier() {
        return working.inheritDefaultEntityMultiplier
                ? working.globalMultiplier
                : working.defaultEntityMultiplier;
    }

    public SmartDropsConfig.EntityKillRequirement entityKillRequirement() {
        return working.entityKillRequirement;
    }

    public boolean setEntityKillRequirement(final SmartDropsConfig.EntityKillRequirement value) {
        if (!editable || value == null || value == working.entityKillRequirement) {
            return false;
        }
        working.entityKillRequirement = value;
        return true;
    }

    public SmartDropsConfig.FilterMode entityFilterMode() {
        return working.entityFilterMode;
    }

    public boolean setEntityFilterMode(final SmartDropsConfig.FilterMode value) {
        if (!editable || value == null || value == working.entityFilterMode) {
            return false;
        }
        working.entityFilterMode = value;
        return true;
    }

    public boolean bossDropsEnabled() {
        return working.bossDropsEnabled;
    }

    public boolean setBossDropsEnabled(final boolean value) {
        return setBoolean(working.bossDropsEnabled, value, next -> working.bossDropsEnabled = next);
    }

    public boolean multiplyMobExperience() {
        return working.multiplyMobExperience;
    }

    public boolean setMultiplyMobExperience(final boolean value) {
        return setBoolean(
                working.multiplyMobExperience,
                value,
                next -> working.multiplyMobExperience = next);
    }

    public int mobExperienceMultiplier() {
        return working.mobExperienceMultiplier;
    }

    public boolean setMobExperienceMultiplier(final int value) {
        return setInt(
                working.mobExperienceMultiplier,
                SmartDropsConfig.clamp(value, 1, working.maximumMultiplier),
                next -> working.mobExperienceMultiplier = next);
    }

    public boolean multiplyBossExperience() {
        return working.multiplyBossExperience;
    }

    public boolean setMultiplyBossExperience(final boolean value) {
        return setBoolean(
                working.multiplyBossExperience,
                value,
                next -> working.multiplyBossExperience = next);
    }

    public Map<String, Integer> entityMultipliers() {
        return Collections.unmodifiableMap(working.entityMultipliers);
    }

    public Map<String, Integer> entityCategoryMultipliers() {
        return Collections.unmodifiableMap(working.entityCategoryMultipliers);
    }

    public Integer entityMultiplier(final String entityId) {
        return working.entityMultipliers.get(normalizeEntityId(entityId));
    }

    public boolean setEntityMultiplier(final String entityId, final Integer value) {
        final String key = normalizeEntityId(entityId);
        if (!editable || key.isEmpty()) {
            return false;
        }
        if (!replaceNullable(working.entityMultipliers, key, clampNullableMultiplier(value))) {
            return false;
        }
        entitySearchCache.clear();
        return true;
    }

    public boolean manualShearingDropsEnabled() {
        return working.manualShearingDropsEnabled;
    }

    public boolean setManualShearingDropsEnabled(final boolean value) {
        return setBoolean(
                working.manualShearingDropsEnabled,
                value,
                next -> working.manualShearingDropsEnabled = next);
    }

    public boolean automatedShearingDropsEnabled() {
        return working.automatedShearingDropsEnabled;
    }

    public boolean setAutomatedShearingDropsEnabled(final boolean value) {
        return setBoolean(
                working.automatedShearingDropsEnabled,
                value,
                next -> working.automatedShearingDropsEnabled = next);
    }

    /** Null means the shearing default inherits the global multiplier. */
    public Integer defaultShearingMultiplier() {
        return working.inheritDefaultShearingMultiplier ? null : working.defaultShearingMultiplier;
    }

    public boolean setDefaultShearingMultiplier(final Integer value) {
        if (!editable) {
            return false;
        }
        if (value == null) {
            if (working.inheritDefaultShearingMultiplier) {
                return false;
            }
            working.inheritDefaultShearingMultiplier = true;
            return true;
        }
        final int clamped = SmartDropsConfig.clamp(value, 0, working.maximumMultiplier);
        final boolean changed = working.inheritDefaultShearingMultiplier
                || working.defaultShearingMultiplier != clamped;
        if (changed) {
            working.inheritDefaultShearingMultiplier = false;
            working.defaultShearingMultiplier = clamped;
        }
        return changed;
    }

    public int effectiveDefaultShearingMultiplier() {
        return working.inheritDefaultShearingMultiplier
                ? working.globalMultiplier
                : working.defaultShearingMultiplier;
    }

    public Map<String, Integer> shearingEntityMultipliers() {
        return Collections.unmodifiableMap(working.shearingEntityMultipliers);
    }

    public Integer shearingEntityMultiplier(final String entityId) {
        return working.shearingEntityMultipliers.get(normalizeEntityId(entityId));
    }

    /**
     * Stages an exact shearing rule only for a certified standard-resource type. Unknown and
     * special types remain non-editable even when an unreachable rule was loaded from JSON.
     */
    public boolean setShearingEntityMultiplier(final String entityId, final Integer value) {
        final String key = normalizeEntityId(entityId);
        if (!editable || key.isEmpty() || !shearingEntityEditable(key)) {
            return false;
        }
        if (!replaceNullable(
                working.shearingEntityMultipliers,
                key,
                clampNullableMultiplier(value))) {
            return false;
        }
        return true;
    }

    public ShearingClassification shearingClassification(final String entityId) {
        final String key = normalizeEntityId(entityId);
        if (key.isEmpty()) {
            return ShearingClassification.UNKNOWN;
        }
        if (ShearingTags.isKnownVanillaSpecial(key)) {
            return ShearingClassification.SPECIAL;
        }
        if (this.authority != ConfigScreenOpenPolicy.Authority.LOCAL_DEFAULTS) {
            final Identifier identifier = Identifier.tryParse(key);
            final EntityType<?> type = identifier == null
                    ? null
                    : BuiltInRegistries.ENTITY_TYPE.getValue(identifier);
            if (type != null) {
                return runtimeShearingClassification(type);
            }
        }
        return shearingInfo(key)
                .map(ShearingInfo::classification)
                .orElse(ShearingClassification.UNKNOWN);
    }

    public boolean shearingEntityEditable(final String entityId) {
        return shearingClassification(entityId) == ShearingClassification.STANDARD_RESOURCE;
    }

    public int effectiveShearingMultiplier(final String entityId) {
        final String key = normalizeEntityId(entityId);
        if (key.isEmpty() || !shearingEntityEditable(key)) {
            return 1;
        }
        final Integer exact = working.shearingEntityMultipliers.get(key);
        return exact == null ? effectiveDefaultShearingMultiplier() : exact;
    }

    public Integer entityCategoryMultiplier(final EntityCategory category) {
        return category == null ? null : working.entityCategoryMultipliers.get(category.key());
    }

    public boolean setEntityCategoryMultiplier(final EntityCategory category, final Integer value) {
        if (!editable || category == null) {
            return false;
        }
        return replaceNullable(
                working.entityCategoryMultipliers,
                category.key(),
                clampNullableMultiplier(value));
    }

    public EffectiveValue inheritedEntityCategoryValue(final EntityCategory category) {
        return entityDefaultValue();
    }

    public EffectiveValue effectiveEntityCategoryValue(final EntityCategory category) {
        if (category != null) {
            final Integer configured = working.entityCategoryMultipliers.get(category.key());
            if (configured != null) {
                return new EffectiveValue(configured, SourceTier.ENTITY_CATEGORY, category.key());
            }
        }
        return entityDefaultValue();
    }

    public EffectiveValue inheritedEntityValue(final String entityId) {
        final EntityInfo info = this.entityCatalogInternal().byId().get(normalizeEntityId(entityId));
        return effectiveEntityCategoryValue(info == null
                ? EntityCategory.MISCELLANEOUS
                : info.selectedCategory());
    }

    public EffectiveValue effectiveEntityValue(final String entityId) {
        final String key = normalizeEntityId(entityId);
        final Integer configured = working.entityMultipliers.get(key);
        return configured == null
                ? inheritedEntityValue(key)
                : new EffectiveValue(configured, SourceTier.ENTITY, key);
    }

    public Set<String> entityBlacklist() {
        return Collections.unmodifiableSet(working.entityBlacklist);
    }

    public Set<String> entityWhitelist() {
        return Collections.unmodifiableSet(working.entityWhitelist);
    }

    public Set<String> entityTagBlacklist() {
        return Collections.unmodifiableSet(working.entityTagBlacklist);
    }

    public Set<String> entityTagWhitelist() {
        return Collections.unmodifiableSet(working.entityTagWhitelist);
    }

    public FilterEntryState entityFilterState(final String entityId) {
        return filterState(
                working.entityBlacklist,
                working.entityWhitelist,
                normalizeEntityId(entityId));
    }

    public boolean setEntityFilterState(final String entityId, final FilterEntryState state) {
        final String key = normalizeEntityId(entityId);
        final boolean changed = setFilterState(
                working.entityBlacklist,
                working.entityWhitelist,
                key,
                state);
        if (changed) {
            entityFilterSearchCache.clear();
        }
        return changed;
    }

    public FilterEntryState entityTagFilterState(final String tagId) {
        return filterState(
                working.entityTagBlacklist,
                working.entityTagWhitelist,
                normalizeTagId(tagId));
    }

    public boolean setEntityTagFilterState(final String tagId, final FilterEntryState state) {
        final String key = normalizeTagId(tagId);
        final boolean changed = setFilterState(
                working.entityTagBlacklist,
                working.entityTagWhitelist,
                key,
                state);
        if (changed) {
            entityTagFilterSearchCache.clear();
        }
        return changed;
    }

    public Map<String, Integer> blockMultipliers() {
        return Collections.unmodifiableMap(working.blockMultipliers);
    }

    public Map<String, Integer> categoryMultipliers() {
        return Collections.unmodifiableMap(working.categoryMultipliers);
    }

    public Map<String, Integer> dimensionMultipliers() {
        return Collections.unmodifiableMap(working.dimensionMultipliers);
    }

    public Integer blockMultiplier(final String blockId) {
        return working.blockMultipliers.get(normalizeKey(blockId));
    }

    public boolean setBlockMultiplier(final String blockId, final Integer value) {
        final String key = normalizeKey(blockId);
        if (!editable || key.isEmpty()) {
            return false;
        }
        final Integer clamped = clampNullableMultiplier(value);
        if (!replaceNullable(working.blockMultipliers, key, clamped)) {
            return false;
        }
        blockSearchCache.clear();
        return true;
    }

    public Integer categoryMultiplier(final Category category) {
        return category == null ? null : working.categoryMultipliers.get(category.key());
    }

    public Integer categoryMultiplier(final String categoryKey) {
        return Category.parse(categoryKey)
                .map(Category::key)
                .map(working.categoryMultipliers::get)
                .orElse(null);
    }

    public boolean setCategoryMultiplier(final Category category, final Integer value) {
        if (!editable || category == null) {
            return false;
        }
        return replaceNullable(working.categoryMultipliers, category.key(), clampNullableMultiplier(value));
    }

    public boolean setCategoryMultiplier(final String categoryKey, final Integer value) {
        return Category.parse(categoryKey)
                .map(category -> setCategoryMultiplier(category, value))
                .orElse(false);
    }

    public Integer dimensionMultiplier(final String dimensionId) {
        return working.dimensionMultipliers.get(normalizeKey(dimensionId));
    }

    public boolean setDimensionMultiplier(final String dimensionId, final Integer value) {
        final String key = normalizeKey(dimensionId);
        if (!editable || key.isEmpty()) {
            return false;
        }
        final Integer clamped = clampNullableMultiplier(value);
        final boolean changed = replaceNullable(working.dimensionMultipliers, key, clamped);
        if (changed || clamped != null) {
            dimensionIds.add(key);
        }
        return changed;
    }

    public Set<String> blacklist() {
        return Collections.unmodifiableSet(working.blacklist);
    }

    public Set<String> whitelist() {
        return Collections.unmodifiableSet(working.whitelist);
    }

    public Set<String> tagBlacklist() {
        return Collections.unmodifiableSet(working.tagBlacklist);
    }

    public Set<String> tagWhitelist() {
        return Collections.unmodifiableSet(working.tagWhitelist);
    }

    public Set<String> blockEntityAllowlist() {
        return Collections.unmodifiableSet(working.blockEntityAllowlist);
    }

    public FilterEntryState filterState(final String blockId) {
        return filterState(working, normalizeKey(blockId));
    }

    public boolean setFilterState(final String blockId, final FilterEntryState state) {
        final String key = normalizeKey(blockId);
        if (!editable || key.isEmpty() || state == null || hasExactFilterState(working, key, state)) {
            return false;
        }
        working.blacklist.remove(key);
        working.whitelist.remove(key);
        switch (state) {
            case NONE -> {
                // Removing both explicit entries restores inherited filtering.
            }
            case WHITELIST -> working.whitelist.add(key);
            case BLACKLIST -> working.blacklist.add(key);
        }
        filterBlockSearchCache.clear();
        return true;
    }

    public FilterEntryState tagFilterState(final String tagId) {
        final String key = normalizeTagId(tagId);
        if (working.tagBlacklist.contains(key) || working.tagBlacklist.contains("#" + key)) {
            return FilterEntryState.BLACKLIST;
        }
        if (working.tagWhitelist.contains(key) || working.tagWhitelist.contains("#" + key)) {
            return FilterEntryState.WHITELIST;
        }
        return FilterEntryState.NONE;
    }

    public EffectiveValue inheritedBlockValue(final String blockId) {
        final BlockInfo info = this.catalog().byId().get(normalizeKey(blockId));
        final List<Category> blockCategories = info == null
                ? List.of(Category.MISCELLANEOUS)
                : info.categories();
        for (Category category : Category.values()) {
            if (blockCategories.contains(category)) {
                final Integer value = working.categoryMultipliers.get(category.key());
                if (value != null) {
                    return new EffectiveValue(value, SourceTier.CATEGORY, category.key());
                }
            }
        }
        return currentDimensionOrGlobalValue();
    }

    public EffectiveValue effectiveBlockValue(final String blockId) {
        final String key = normalizeKey(blockId);
        final Integer configured = working.blockMultipliers.get(key);
        return configured == null
                ? inheritedBlockValue(key)
                : new EffectiveValue(configured, SourceTier.BLOCK, key);
    }

    public EffectiveValue inheritedCategoryValue(final Category category) {
        return currentDimensionOrGlobalValue();
    }

    public EffectiveValue inheritedCategoryValue(final String categoryKey) {
        return currentDimensionOrGlobalValue();
    }

    public EffectiveValue effectiveCategoryValue(final Category category) {
        if (category != null) {
            final Integer configured = working.categoryMultipliers.get(category.key());
            if (configured != null) {
                return new EffectiveValue(configured, SourceTier.CATEGORY, category.key());
            }
        }
        return currentDimensionOrGlobalValue();
    }

    public EffectiveValue effectiveCategoryValue(final String categoryKey) {
        return Category.parse(categoryKey)
                .map(this::effectiveCategoryValue)
                .orElseGet(this::currentDimensionOrGlobalValue);
    }

    public EffectiveValue inheritedDimensionValue(final String dimensionId) {
        return globalValue();
    }

    public EffectiveValue effectiveDimensionValue(final String dimensionId) {
        final String key = normalizeKey(dimensionId);
        final Integer configured = working.dimensionMultipliers.get(key);
        return configured == null
                ? globalValue()
                : new EffectiveValue(configured, SourceTier.DIMENSION, key);
    }

    public Optional<SmartDropsConfig.Preset> selectedPreset() {
        return Optional.ofNullable(selectedPreset);
    }

    public boolean applyPreset(final SmartDropsConfig.Preset preset) {
        if (!editable || preset == null) {
            return false;
        }
        if (preset == SmartDropsConfig.Preset.CUSTOM) {
            final boolean changed = selectedPreset != null;
            selectedPreset = null;
            return changed;
        }
        final SmartDropsConfig before = copyOf(working);
        working.applyPreset(preset);
        selectedPreset = preset;
        blockSearchCache.clear();
        return !sameSupportedState(before, working);
    }

    /** True only when a value representable by {@link ConfigPatch} differs from the snapshot. */
    public boolean isDirty() {
        return !sameSupportedState(baseline, working);
    }

    /** Builds an exact semantic diff without exposing or modifying either stored config. */
    public ConfigPatch buildPatch() {
        final ConfigPatch patch = new ConfigPatch();
        if (!isDirty()) {
            return patch;
        }

        SmartDropsConfig comparisonBase = baseline;
        if (selectedPreset != null && !samePresetDomain(baseline, working)) {
            patch.preset = selectedPreset;
            comparisonBase = copyOf(baseline);
            comparisonBase.applyPreset(selectedPreset);
        }

        if (working.enabled != comparisonBase.enabled) {
            patch.enabled = working.enabled;
        }
        if (working.globalMultiplier != comparisonBase.globalMultiplier) {
            patch.globalMultiplier = working.globalMultiplier;
        }
        if (working.smartPlacementProtection != comparisonBase.smartPlacementProtection) {
            patch.smartPlacementProtection = working.smartPlacementProtection;
        }
        if (working.protectBlockEntities != comparisonBase.protectBlockEntities) {
            patch.protectBlockEntities = working.protectBlockEntities;
        }
        if (working.playerMining != comparisonBase.playerMining) {
            patch.playerMining = working.playerMining;
        }
        if (working.explosions != comparisonBase.explosions) {
            patch.explosions = working.explosions;
        }
        if (working.automatedMining != comparisonBase.automatedMining) {
            patch.automatedMining = working.automatedMining;
        }
        if (working.multiplyExperience != comparisonBase.multiplyExperience) {
            patch.multiplyExperience = working.multiplyExperience;
        }
        if (working.experienceMultiplier != comparisonBase.experienceMultiplier) {
            patch.experienceMultiplier = working.experienceMultiplier;
        }
        if (working.conservativePistonProtection != comparisonBase.conservativePistonProtection) {
            patch.conservativePistonProtection = working.conservativePistonProtection;
        }
        if (working.allowPlayerOverrides != comparisonBase.allowPlayerOverrides) {
            patch.allowPlayerOverrides = working.allowPlayerOverrides;
        }
        if (working.statisticsEnabled != comparisonBase.statisticsEnabled) {
            patch.statisticsEnabled = working.statisticsEnabled;
        }
        if (working.filterMode != comparisonBase.filterMode) {
            patch.filterMode = working.filterMode;
        }
        if (working.sourceMode != comparisonBase.sourceMode) {
            patch.sourceMode = working.sourceMode;
        }
        if (working.entityDropsEnabled != comparisonBase.entityDropsEnabled) {
            patch.entityDropsEnabled = working.entityDropsEnabled;
        }
        if (working.inheritDefaultEntityMultiplier != comparisonBase.inheritDefaultEntityMultiplier) {
            patch.inheritDefaultEntityMultiplier = working.inheritDefaultEntityMultiplier;
        }
        if (working.defaultEntityMultiplier != comparisonBase.defaultEntityMultiplier) {
            patch.defaultEntityMultiplier = working.defaultEntityMultiplier;
        }
        if (working.entityKillRequirement != comparisonBase.entityKillRequirement) {
            patch.entityKillRequirement = working.entityKillRequirement;
        }
        if (working.entityFilterMode != comparisonBase.entityFilterMode) {
            patch.entityFilterMode = working.entityFilterMode;
        }
        if (working.bossDropsEnabled != comparisonBase.bossDropsEnabled) {
            patch.bossDropsEnabled = working.bossDropsEnabled;
        }
        if (working.multiplyMobExperience != comparisonBase.multiplyMobExperience) {
            patch.multiplyMobExperience = working.multiplyMobExperience;
        }
        if (working.mobExperienceMultiplier != comparisonBase.mobExperienceMultiplier) {
            patch.mobExperienceMultiplier = working.mobExperienceMultiplier;
        }
        if (working.multiplyBossExperience != comparisonBase.multiplyBossExperience) {
            patch.multiplyBossExperience = working.multiplyBossExperience;
        }
        if (working.manualShearingDropsEnabled != comparisonBase.manualShearingDropsEnabled) {
            patch.manualShearingDropsEnabled = working.manualShearingDropsEnabled;
        }
        if (working.automatedShearingDropsEnabled != comparisonBase.automatedShearingDropsEnabled) {
            patch.automatedShearingDropsEnabled = working.automatedShearingDropsEnabled;
        }
        if (working.inheritDefaultShearingMultiplier
                != comparisonBase.inheritDefaultShearingMultiplier) {
            patch.inheritDefaultShearingMultiplier = working.inheritDefaultShearingMultiplier;
        }
        if (!working.inheritDefaultShearingMultiplier
                && working.defaultShearingMultiplier != comparisonBase.defaultShearingMultiplier) {
            patch.defaultShearingMultiplier = working.defaultShearingMultiplier;
        }

        diffMultiplierMap(
                comparisonBase.blockMultipliers,
                working.blockMultipliers,
                patch.blockMultipliers,
                patch.inheritedBlocks);
        diffMultiplierMap(
                comparisonBase.categoryMultipliers,
                working.categoryMultipliers,
                patch.categoryMultipliers,
                patch.inheritedCategories);
        diffMultiplierMap(
                comparisonBase.dimensionMultipliers,
                working.dimensionMultipliers,
                patch.dimensionMultipliers,
                patch.inheritedDimensions);
        diffBlockFilters(comparisonBase, working, patch.blockFilters);
        diffMultiplierMap(
                comparisonBase.entityMultipliers,
                working.entityMultipliers,
                patch.entityMultipliers,
                patch.inheritedEntities);
        diffMultiplierMap(
                comparisonBase.entityCategoryMultipliers,
                working.entityCategoryMultipliers,
                patch.entityCategoryMultipliers,
                patch.inheritedEntityCategories);
        diffFilterSets(
                comparisonBase.entityBlacklist,
                comparisonBase.entityWhitelist,
                working.entityBlacklist,
                working.entityWhitelist,
                patch.entityFilters);
        diffFilterSets(
                comparisonBase.entityTagBlacklist,
                comparisonBase.entityTagWhitelist,
                working.entityTagBlacklist,
                working.entityTagWhitelist,
                patch.entityTagFilters);
        diffMultiplierMap(
                comparisonBase.shearingEntityMultipliers,
                working.shearingEntityMultipliers,
                patch.shearingEntityMultipliers,
                patch.inheritedShearingEntities);
        return patch;
    }

    private List<BlockInfo> computeBlockSearch(final String query) {
        if (query.isEmpty()) {
            return blockInfosForIds(working.blockMultipliers.keySet());
        }
        return matchingBlocks(query, working.blockMultipliers.keySet());
    }

    private List<BlockInfo> computeFilterBlockSearch(final String query) {
        final LinkedHashSet<String> configured = new LinkedHashSet<>(working.blacklist);
        configured.addAll(working.whitelist);
        if (query.isEmpty()) {
            return blockInfosForIds(configured);
        }
        return matchingBlocks(query, configured);
    }

    private List<String> computeFilterTagSearch(final String query) {
        final LinkedHashSet<String> configured = new LinkedHashSet<>();
        working.tagBlacklist.forEach(value -> configured.add(normalizeTagId(value)));
        working.tagWhitelist.forEach(value -> configured.add(normalizeTagId(value)));

        final Collection<String> source;
        if (query.isEmpty()) {
            source = configured;
        } else {
            final LinkedHashSet<String> all = new LinkedHashSet<>(this.catalog().tagIds());
            all.addAll(configured);
            source = all;
        }
        return source.stream()
                .filter(value -> query.isEmpty() || value.contains(query))
                .sorted()
                .toList();
    }

    private List<EntityInfo> computeEntitySearch(final String query) {
        if (query.isEmpty()) {
            return entityInfosForIds(working.entityMultipliers.keySet());
        }
        return matchingEntities(query, working.entityMultipliers.keySet());
    }

    private List<ShearingInfo> computeShearingSearch(final String query) {
        final ShearingCatalog currentCatalog = this.shearingCatalogInternal();
        if (query.isEmpty()) {
            return shearingInfosForIds(working.shearingEntityMultipliers.keySet());
        }
        return currentCatalog.entities().stream()
                .filter(info -> info.searchText().contains(query))
                .toList();
    }

    private List<EntityInfo> computeEntityFilterSearch(final String query) {
        final LinkedHashSet<String> configured = new LinkedHashSet<>(working.entityBlacklist);
        configured.addAll(working.entityWhitelist);
        if (query.isEmpty()) {
            return entityInfosForIds(configured);
        }
        return matchingEntities(query, configured);
    }

    private List<String> computeEntityTagFilterSearch(final String query) {
        final LinkedHashSet<String> configured = new LinkedHashSet<>();
        working.entityTagBlacklist.forEach(value -> configured.add(normalizeTagId(value)));
        working.entityTagWhitelist.forEach(value -> configured.add(normalizeTagId(value)));
        final Collection<String> source;
        if (query.isEmpty()) {
            source = configured;
        } else {
            final LinkedHashSet<String> all = new LinkedHashSet<>(this.entityCatalogInternal().tagIds());
            all.addAll(configured);
            if (Identifier.tryParse(query) != null) {
                all.add(query);
            }
            source = all;
        }
        return source.stream()
                .filter(value -> query.isEmpty() || value.contains(query))
                .sorted()
                .toList();
    }

    private List<EntityInfo> matchingEntities(final String query, final Set<String> configuredIds) {
        final EntityCatalog currentCatalog = this.entityCatalogInternal();
        final ArrayList<EntityInfo> result = new ArrayList<>();
        for (EntityInfo info : currentCatalog.entities()) {
            if (info.searchText().contains(query)) {
                result.add(info);
            }
        }
        boolean addedSynthetic = false;
        for (String id : configuredIds) {
            final String normalized = normalizeEntityId(id);
            if (!normalized.isEmpty() && !currentCatalog.byId().containsKey(normalized)) {
                final EntityInfo synthetic = syntheticEntityInfo(normalized);
                if (synthetic.searchText().contains(query)) {
                    result.add(synthetic);
                    addedSynthetic = true;
                }
            }
        }
        if (addedSynthetic) {
            result.sort(ENTITY_ORDER);
        }
        return List.copyOf(result);
    }

    private List<EntityInfo> entityInfosForIds(final Collection<String> ids) {
        final EntityCatalog currentCatalog = this.entityCatalogInternal();
        final ArrayList<EntityInfo> result = new ArrayList<>();
        for (String id : ids) {
            final String normalized = normalizeEntityId(id);
            if (!normalized.isEmpty()) {
                result.add(currentCatalog.byId().getOrDefault(normalized, syntheticEntityInfo(normalized)));
            }
        }
        result.sort(ENTITY_ORDER);
        return List.copyOf(result);
    }

    private List<ShearingInfo> shearingInfosForIds(final Collection<String> ids) {
        final ShearingCatalog currentCatalog = this.shearingCatalogInternal();
        final ArrayList<ShearingInfo> result = new ArrayList<>();
        for (String id : ids) {
            final String normalized = normalizeEntityId(id);
            if (!normalized.isEmpty()) {
                result.add(currentCatalog.byId().getOrDefault(
                        normalized,
                        syntheticShearingInfo(normalized)));
            }
        }
        result.sort(SHEARING_ORDER);
        return List.copyOf(result);
    }

    private List<BlockInfo> matchingBlocks(final String query, final Set<String> configuredIds) {
        final Catalog currentCatalog = this.catalog();
        final ArrayList<BlockInfo> result = new ArrayList<>();
        for (BlockInfo info : currentCatalog.blocks()) {
            if (info.searchText().contains(query)) {
                result.add(info);
            }
        }
        boolean addedSynthetic = false;
        for (String id : configuredIds) {
            final String normalized = normalizeKey(id);
            if (!normalized.isEmpty() && !currentCatalog.byId().containsKey(normalized)) {
                final BlockInfo synthetic = syntheticBlockInfo(normalized);
                if (synthetic.searchText().contains(query)) {
                    result.add(synthetic);
                    addedSynthetic = true;
                }
            }
        }
        if (addedSynthetic) {
            result.sort(BLOCK_ORDER);
        }
        return List.copyOf(result);
    }

    private List<BlockInfo> blockInfosForIds(final Collection<String> ids) {
        final Catalog currentCatalog = this.catalog();
        final ArrayList<BlockInfo> result = new ArrayList<>();
        for (String id : ids) {
            final String normalized = normalizeKey(id);
            if (!normalized.isEmpty()) {
                result.add(currentCatalog.byId().getOrDefault(normalized, syntheticBlockInfo(normalized)));
            }
        }
        result.sort(BLOCK_ORDER);
        return List.copyOf(result);
    }

    private Catalog catalog() {
        if (this.catalog == null) {
            this.catalog = buildCatalog();
        }
        return this.catalog;
    }

    private EntityCatalog entityCatalogInternal() {
        if (this.entityCatalog == null) {
            this.entityCatalog = buildEntityCatalog(
                    this.authority == ConfigScreenOpenPolicy.Authority.LOCAL_DEFAULTS);
        }
        return this.entityCatalog;
    }

    private ShearingCatalog shearingCatalogInternal() {
        if (this.authority != ConfigScreenOpenPolicy.Authority.LOCAL_DEFAULTS) {
            // Connected holder tags are reloadable and must never be retained across searches.
            return buildShearingCatalog(false);
        }
        // The title-screen fallback reads installed mod resources, not a reloadable world pack.
        // Cache it so focused-screen rendering never scans files or the registry per frame.
        if (this.localShearingCatalog == null) {
            this.localShearingCatalog = buildShearingCatalog(true);
        }
        return this.localShearingCatalog;
    }

    private static <T> T cachedSearch(
            final Map<String, T> cache,
            final String query,
            final Function<String, T> loader
    ) {
        final T existing = cache.get(query);
        if (existing != null) {
            return existing;
        }
        final T loaded = loader.apply(query);
        if (cache.size() >= SEARCH_CACHE_LIMIT) {
            final var iterator = cache.keySet().iterator();
            if (iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }
        }
        cache.put(query, loaded);
        return loaded;
    }

    private EffectiveValue currentDimensionOrGlobalValue() {
        if (currentDimensionId != null) {
            final Integer configured = working.dimensionMultipliers.get(currentDimensionId);
            if (configured != null) {
                return new EffectiveValue(configured, SourceTier.DIMENSION, currentDimensionId);
            }
        }
        return globalValue();
    }

    private EffectiveValue globalValue() {
        return new EffectiveValue(working.globalMultiplier, SourceTier.GLOBAL, "global");
    }

    private EffectiveValue entityDefaultValue() {
        return working.inheritDefaultEntityMultiplier
                ? globalValue()
                : new EffectiveValue(
                        working.defaultEntityMultiplier,
                        SourceTier.ENTITY_DEFAULT,
                        "entity_default");
    }

    private Integer clampNullableMultiplier(final Integer value) {
        return value == null ? null : SmartDropsConfig.clamp(value, 0, working.maximumMultiplier);
    }

    private boolean setBoolean(
            final boolean current,
            final boolean value,
            final java.util.function.Consumer<Boolean> setter
    ) {
        if (!editable || current == value) {
            return false;
        }
        setter.accept(value);
        return true;
    }

    private boolean setInt(
            final int current,
            final int value,
            final java.util.function.IntConsumer setter
    ) {
        if (!editable || current == value) {
            return false;
        }
        setter.accept(value);
        return true;
    }

    private static boolean replaceNullable(
            final Map<String, Integer> target,
            final String key,
            final Integer value
    ) {
        final Integer current = target.get(key);
        if (value == null) {
            if (!target.containsKey(key)) {
                return false;
            }
            target.remove(key);
            return true;
        }
        if (Objects.equals(current, value) && target.containsKey(key)) {
            return false;
        }
        target.put(key, value);
        return true;
    }

    private void collectDimensionIds(final Minecraft minecraft) {
        dimensionIds.addAll(VANILLA_DIMENSIONS);
        if (currentDimensionId != null) {
            dimensionIds.add(currentDimensionId);
        }
        if (minecraft != null && minecraft.getConnection() != null) {
            minecraft.getConnection().levels().stream()
                    .map(key -> key.identifier().toString())
                    .sorted()
                    .forEach(dimensionIds::add);
        }
        working.dimensionMultipliers.keySet().stream().sorted().forEach(dimensionIds::add);
        baseline.dimensionMultipliers.keySet().stream().sorted().forEach(dimensionIds::add);
    }

    private static String currentDimensionId(final Minecraft minecraft) {
        if (minecraft == null || minecraft.level == null) {
            return null;
        }
        return minecraft.level.dimension().identifier().toString();
    }

    /** The only full registry scan performed by the session. */
    private static Catalog buildCatalog() {
        final ArrayList<BlockInfo> blocks = new ArrayList<>();
        final LinkedHashMap<String, BlockInfo> byId = new LinkedHashMap<>();
        final EnumMap<Category, List<BlockInfo>> mutableByCategory = new EnumMap<>(Category.class);
        final LinkedHashSet<String> tagIds = new LinkedHashSet<>();
        final Map<Category, Set<String>> declaredCategoryBlocks = ClientCategoryTagIndex.load();
        for (Category category : Category.values()) {
            mutableByCategory.put(category, new ArrayList<>());
        }

        final List<Identifier> ids = BuiltInRegistries.BLOCK.keySet().stream().sorted().toList();
        for (Identifier identifier : ids) {
            final Block block = BuiltInRegistries.BLOCK.getValue(identifier);
            if (block == null) {
                continue;
            }
            final BlockState state = block.defaultBlockState();
            final LinkedHashSet<Category> resolvedCategories = SmartDropTags.categoriesFor(state);
            resolvedCategories.remove(Category.MISCELLANEOUS);
            for (Category category : Category.values()) {
                if (declaredCategoryBlocks.getOrDefault(category, Set.of()).contains(identifier.toString())) {
                    resolvedCategories.add(category);
                }
            }
            if (resolvedCategories.isEmpty()) {
                resolvedCategories.add(Category.MISCELLANEOUS);
            }
            final List<Category> categories = List.copyOf(resolvedCategories);
            final List<String> blockTagIds = state.typeHolder().tags()
                    .map(tag -> tag.location().toString())
                    .sorted()
                    .toList();
            tagIds.addAll(blockTagIds);

            final String id = identifier.toString();
            final String displayName = block.getName().getString();
            final BlockInfo info = new BlockInfo(
                    id,
                    displayName,
                    (displayName + " " + id).toLowerCase(Locale.ROOT),
                    categories,
                    blockTagIds);
            blocks.add(info);
            byId.put(id, info);
            for (Category category : categories) {
                mutableByCategory.get(category).add(info);
            }
        }

        blocks.sort(BLOCK_ORDER);
        final EnumMap<Category, List<BlockInfo>> immutableByCategory = new EnumMap<>(Category.class);
        mutableByCategory.forEach((category, values) -> {
            values.sort(BLOCK_ORDER);
            immutableByCategory.put(category, List.copyOf(values));
        });
        final List<String> sortedTags = tagIds.stream().sorted().toList();
        return new Catalog(
                List.copyOf(blocks),
                Collections.unmodifiableMap(byId),
                Collections.unmodifiableMap(immutableByCategory),
                sortedTags);
    }

    /** One lazy registry scan; entity instances are never created for GUI classification. */
    private static EntityCatalog buildEntityCatalog(final boolean useLocalResourceFallback) {
        final ArrayList<EntityInfo> entities = new ArrayList<>();
        final LinkedHashMap<String, EntityInfo> byId = new LinkedHashMap<>();
        final EnumMap<EntityCategory, List<EntityInfo>> mutableByCategory =
                new EnumMap<>(EntityCategory.class);
        final LinkedHashSet<String> tagIds = new LinkedHashSet<>();
        final Map<EntityCategory, Set<String>> declaredCategoryTypes = useLocalResourceFallback
                ? ClientEntityCategoryTagIndex.load()
                : Map.of();
        for (EntityCategory category : EntityCategory.values()) {
            mutableByCategory.put(category, new ArrayList<>());
        }

        final List<Identifier> ids = BuiltInRegistries.ENTITY_TYPE.keySet().stream().sorted().toList();
        for (Identifier identifier : ids) {
            final String id = identifier.toString();
            final EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(identifier);
            if (type == null
                    || "minecraft:player".equals(id)
                    || type == EntityTypes.ARMOR_STAND
                    || type == EntityTypes.MANNEQUIN
                    || type.getDefaultLootTable().isEmpty()) {
                continue;
            }

            final List<String> typeTagIds = List.copyOf(EntityDropTags.runtimeTags(type));
            tagIds.addAll(typeTagIds);
            final LinkedHashSet<EntityCategory> explicitMatches = new LinkedHashSet<>();
            for (EntityCategory category : EntityCategory.values()) {
                final String categoryTag = EntityDropTags.categoryTag(category).location().toString();
                if (typeTagIds.contains(categoryTag)
                        || declaredCategoryTypes.getOrDefault(category, Set.of()).contains(id)) {
                    explicitMatches.add(category);
                }
            }
            if (EntityClassifier.isKnownBossType(id)) {
                explicitMatches.add(EntityCategory.BOSSES);
            }
            final EntityCategory fallback = fallbackEntityCategory(type.getCategory());
            final LinkedHashSet<EntityCategory> matches = new LinkedHashSet<>(explicitMatches);
            matches.add(fallback);
            final List<EntityCategory> orderedMatches = java.util.Arrays.stream(EntityCategory.values())
                    .filter(matches::contains)
                    .toList();
            final EntityCategory selected;
            if (explicitMatches.contains(EntityCategory.BOSSES)) {
                selected = EntityCategory.BOSSES;
            } else {
                selected = java.util.Arrays.stream(EntityCategory.values())
                        .filter(explicitMatches::contains)
                        .findFirst()
                        .orElse(fallback);
            }
            final boolean categoryEstimated = explicitMatches.isEmpty();
            final String displayName = type.getDescription().getString();
            final EntityInfo info = new EntityInfo(
                    id,
                    displayName,
                    (displayName + " " + id).toLowerCase(Locale.ROOT),
                    orderedMatches,
                    selected,
                    typeTagIds,
                    categoryEstimated);
            entities.add(info);
            byId.put(id, info);
            for (EntityCategory category : orderedMatches) {
                mutableByCategory.get(category).add(info);
            }
        }

        entities.sort(ENTITY_ORDER);
        final EnumMap<EntityCategory, List<EntityInfo>> immutableByCategory =
                new EnumMap<>(EntityCategory.class);
        mutableByCategory.forEach((category, values) -> {
            values.sort(ENTITY_ORDER);
            immutableByCategory.put(category, List.copyOf(values));
        });
        return new EntityCatalog(
                List.copyOf(entities),
                Collections.unmodifiableMap(byId),
                Collections.unmodifiableMap(immutableByCategory),
                tagIds.stream().sorted().toList());
    }

    /**
     * Builds a small metadata-only catalog from certified tags, known vanilla shearing types, and
     * configured overrides. Entity factories are never invoked.
     */
    private ShearingCatalog buildShearingCatalog(final boolean useLocalResourceFallback) {
        final ClientShearingTagIndex.Entries declared = useLocalResourceFallback
                ? ClientShearingTagIndex.load()
                : ClientShearingTagIndex.Entries.empty();
        final LinkedHashSet<String> candidateIds = new LinkedHashSet<>(List.of("minecraft:sheep"));
        candidateIds.addAll(ShearingTags.KNOWN_VANILLA_SPECIAL_IDS);
        candidateIds.addAll(declared.standardResources());
        candidateIds.addAll(declared.special());
        candidateIds.addAll(working.shearingEntityMultipliers.keySet());

        // Runtime holder tags are authoritative in-world. This scan reads registry metadata only.
        for (Identifier identifier : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            final EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(identifier);
            if (type != null
                    && runtimeShearingTrace(type).classification()
                            != ShearingClassification.UNKNOWN) {
                candidateIds.add(identifier.toString());
            }
        }

        final ArrayList<ShearingInfo> entities = new ArrayList<>();
        final LinkedHashMap<String, ShearingInfo> byId = new LinkedHashMap<>();
        for (String rawId : candidateIds) {
            final String id = normalizeEntityId(rawId);
            if (id.isEmpty()) {
                continue;
            }
            final Identifier identifier = Identifier.tryParse(id);
            final EntityType<?> type = identifier == null
                    ? null
                    : BuiltInRegistries.ENTITY_TYPE.getValue(identifier);
            final ShearingRuleTrace runtime = type == null ? null : runtimeShearingTrace(type);
            final boolean standardTagged = declared.standardResources().contains(id)
                    || (runtime != null && runtime.standardTagged());
            final boolean specialTagged = ShearingTags.isKnownVanillaSpecial(id)
                    || declared.special().contains(id)
                    || (runtime != null
                            && (runtime.specialTagged() || runtime.knownVanillaSpecial()));
            final ShearingClassification classification = specialTagged
                    ? ShearingClassification.SPECIAL
                    : standardTagged
                            ? ShearingClassification.STANDARD_RESOURCE
                            : ShearingClassification.UNKNOWN;
            final String displayName = type == null ? id : type.getDescription().getString();
            final ShearingInfo info = new ShearingInfo(
                    id,
                    displayName,
                    (displayName + " " + id).toLowerCase(Locale.ROOT),
                    classification,
                    standardTagged && specialTagged);
            entities.add(info);
            byId.put(id, info);
        }
        entities.sort(SHEARING_ORDER);
        return new ShearingCatalog(List.copyOf(entities), Collections.unmodifiableMap(byId));
    }

    private ShearingClassification runtimeShearingClassification(final EntityType<?> type) {
        return runtimeShearingTrace(type).classification();
    }

    private ShearingRuleTrace runtimeShearingTrace(final EntityType<?> type) {
        return ShearingRuleResolver.trace(working, type, ShearingSource.MANUAL_PLAYER);
    }

    private static EntityCategory fallbackEntityCategory(final MobCategory category) {
        if (category == null) {
            return EntityCategory.MISCELLANEOUS;
        }
        return switch (category) {
            case MONSTER -> EntityCategory.HOSTILE;
            case CREATURE -> EntityCategory.PASSIVE;
            case AMBIENT -> EntityCategory.AMBIENT;
            case AXOLOTLS, UNDERGROUND_WATER_CREATURE, WATER_CREATURE, WATER_AMBIENT ->
                    EntityCategory.AQUATIC;
            case MISC -> EntityCategory.MISCELLANEOUS;
            // Fabric mods can extend this enum at runtime. Keep their entities configurable even
            // when they introduce a category that did not exist on the compile-time classpath.
            default -> EntityCategory.MISCELLANEOUS;
        };
    }

    private static BlockInfo syntheticBlockInfo(final String id) {
        return new BlockInfo(
                id,
                id,
                id.toLowerCase(Locale.ROOT),
                List.of(Category.MISCELLANEOUS),
                List.of());
    }

    private static EntityInfo syntheticEntityInfo(final String id) {
        return new EntityInfo(
                id,
                id,
                id.toLowerCase(Locale.ROOT),
                List.of(EntityCategory.MISCELLANEOUS),
                EntityCategory.MISCELLANEOUS,
                List.of(),
                true);
    }

    private static ShearingInfo syntheticShearingInfo(final String id) {
        return new ShearingInfo(
                id,
                id,
                id.toLowerCase(Locale.ROOT),
                ShearingTags.isKnownVanillaSpecial(id)
                        ? ShearingClassification.SPECIAL
                        : ShearingClassification.UNKNOWN,
                false);
    }

    private static void diffMultiplierMap(
            final Map<String, Integer> before,
            final Map<String, Integer> after,
            final Map<String, Integer> replacements,
            final Set<String> inherited
    ) {
        final LinkedHashSet<String> keys = new LinkedHashSet<>(before.keySet());
        keys.addAll(after.keySet());
        for (String key : keys) {
            if (Objects.equals(before.get(key), after.get(key))
                    && before.containsKey(key) == after.containsKey(key)) {
                continue;
            }
            final Integer value = after.get(key);
            if (value == null) {
                inherited.add(key);
            } else {
                replacements.put(key, value);
            }
        }
    }

    private static void diffBlockFilters(
            final SmartDropsConfig before,
            final SmartDropsConfig after,
            final Map<String, ConfigPatch.FilterEntryState> changes
    ) {
        final LinkedHashSet<String> keys = new LinkedHashSet<>(before.blacklist);
        keys.addAll(before.whitelist);
        keys.addAll(after.blacklist);
        keys.addAll(after.whitelist);
        for (String key : keys) {
            final boolean beforeBlacklisted = before.blacklist.contains(key);
            final boolean beforeWhitelisted = before.whitelist.contains(key);
            final boolean afterBlacklisted = after.blacklist.contains(key);
            final boolean afterWhitelisted = after.whitelist.contains(key);
            if (beforeBlacklisted != afterBlacklisted || beforeWhitelisted != afterWhitelisted) {
                final FilterEntryState afterState = filterState(after, key);
                changes.put(key, switch (afterState) {
                    case NONE -> ConfigPatch.FilterEntryState.NONE;
                    case WHITELIST -> ConfigPatch.FilterEntryState.WHITELIST;
                    case BLACKLIST -> ConfigPatch.FilterEntryState.BLACKLIST;
                });
            }
        }
    }

    private static void diffFilterSets(
            final Set<String> beforeBlacklist,
            final Set<String> beforeWhitelist,
            final Set<String> afterBlacklist,
            final Set<String> afterWhitelist,
            final Map<String, ConfigPatch.FilterEntryState> changes
    ) {
        final LinkedHashSet<String> keys = new LinkedHashSet<>(beforeBlacklist);
        keys.addAll(beforeWhitelist);
        keys.addAll(afterBlacklist);
        keys.addAll(afterWhitelist);
        for (String key : keys) {
            final FilterEntryState before = filterState(beforeBlacklist, beforeWhitelist, key);
            final FilterEntryState after = filterState(afterBlacklist, afterWhitelist, key);
            if (before != after) {
                changes.put(key, switch (after) {
                    case NONE -> ConfigPatch.FilterEntryState.NONE;
                    case WHITELIST -> ConfigPatch.FilterEntryState.WHITELIST;
                    case BLACKLIST -> ConfigPatch.FilterEntryState.BLACKLIST;
                });
            }
        }
    }

    private boolean setFilterState(
            final Set<String> blacklist,
            final Set<String> whitelist,
            final String key,
            final FilterEntryState state
    ) {
        if (!editable
                || key == null
                || key.isEmpty()
                || state == null
                || filterState(blacklist, whitelist, key) == state) {
            return false;
        }
        blacklist.remove(key);
        whitelist.remove(key);
        switch (state) {
            case NONE -> {
            }
            case WHITELIST -> whitelist.add(key);
            case BLACKLIST -> blacklist.add(key);
        }
        return true;
    }

    private static FilterEntryState filterState(
            final Set<String> blacklist,
            final Set<String> whitelist,
            final String key
    ) {
        if (blacklist.contains(key)) {
            return FilterEntryState.BLACKLIST;
        }
        if (whitelist.contains(key)) {
            return FilterEntryState.WHITELIST;
        }
        return FilterEntryState.NONE;
    }

    private static FilterEntryState filterState(final SmartDropsConfig config, final String key) {
        if (config.blacklist.contains(key)) {
            return FilterEntryState.BLACKLIST;
        }
        if (config.whitelist.contains(key)) {
            return FilterEntryState.WHITELIST;
        }
        return FilterEntryState.NONE;
    }

    private static boolean hasExactFilterState(
            final SmartDropsConfig config,
            final String key,
            final FilterEntryState state
    ) {
        final boolean blacklisted = config.blacklist.contains(key);
        final boolean whitelisted = config.whitelist.contains(key);
        return switch (state) {
            case NONE -> !blacklisted && !whitelisted;
            case WHITELIST -> !blacklisted && whitelisted;
            case BLACKLIST -> blacklisted && !whitelisted;
        };
    }

    private static boolean sameSupportedState(final SmartDropsConfig left, final SmartDropsConfig right) {
        return left.enabled == right.enabled
                && left.globalMultiplier == right.globalMultiplier
                && left.smartPlacementProtection == right.smartPlacementProtection
                && left.protectBlockEntities == right.protectBlockEntities
                && left.playerMining == right.playerMining
                && left.explosions == right.explosions
                && left.automatedMining == right.automatedMining
                && left.multiplyExperience == right.multiplyExperience
                && left.experienceMultiplier == right.experienceMultiplier
                && left.conservativePistonProtection == right.conservativePistonProtection
                && left.allowPlayerOverrides == right.allowPlayerOverrides
                && left.statisticsEnabled == right.statisticsEnabled
                && left.filterMode == right.filterMode
                && left.sourceMode == right.sourceMode
                && left.entityDropsEnabled == right.entityDropsEnabled
                && left.inheritDefaultEntityMultiplier == right.inheritDefaultEntityMultiplier
                && left.defaultEntityMultiplier == right.defaultEntityMultiplier
                && left.entityKillRequirement == right.entityKillRequirement
                && left.entityFilterMode == right.entityFilterMode
                && left.bossDropsEnabled == right.bossDropsEnabled
                && left.multiplyMobExperience == right.multiplyMobExperience
                && left.mobExperienceMultiplier == right.mobExperienceMultiplier
                && left.multiplyBossExperience == right.multiplyBossExperience
                && left.manualShearingDropsEnabled == right.manualShearingDropsEnabled
                && left.automatedShearingDropsEnabled == right.automatedShearingDropsEnabled
                && left.inheritDefaultShearingMultiplier == right.inheritDefaultShearingMultiplier
                && (left.inheritDefaultShearingMultiplier
                        || left.defaultShearingMultiplier == right.defaultShearingMultiplier)
                && Objects.equals(left.blockMultipliers, right.blockMultipliers)
                && Objects.equals(left.categoryMultipliers, right.categoryMultipliers)
                && Objects.equals(left.dimensionMultipliers, right.dimensionMultipliers)
                && Objects.equals(left.blacklist, right.blacklist)
                && Objects.equals(left.whitelist, right.whitelist)
                && Objects.equals(left.entityCategoryMultipliers, right.entityCategoryMultipliers)
                && Objects.equals(left.entityMultipliers, right.entityMultipliers)
                && Objects.equals(left.entityBlacklist, right.entityBlacklist)
                && Objects.equals(left.entityWhitelist, right.entityWhitelist)
                && Objects.equals(left.entityTagBlacklist, right.entityTagBlacklist)
                && Objects.equals(left.entityTagWhitelist, right.entityTagWhitelist)
                && Objects.equals(left.shearingEntityMultipliers, right.shearingEntityMultipliers);
    }

    private static boolean samePresetDomain(final SmartDropsConfig left, final SmartDropsConfig right) {
        return left.globalMultiplier == right.globalMultiplier
                && Objects.equals(left.blockMultipliers, right.blockMultipliers)
                && Objects.equals(left.categoryMultipliers, right.categoryMultipliers)
                && Objects.equals(left.dimensionMultipliers, right.dimensionMultipliers);
    }

    private static SmartDropsConfig copyOf(final SmartDropsConfig source) {
        final SmartDropsConfig copy = new SmartDropsConfig();
        copy.schemaVersion = source.schemaVersion;
        copy.enabled = source.enabled;
        copy.globalMultiplier = source.globalMultiplier;
        copy.maximumMultiplier = source.maximumMultiplier;
        copy.sourceMode = source.sourceMode;
        copy.filterMode = source.filterMode;
        copy.smartPlacementProtection = source.smartPlacementProtection;
        copy.protectBlockEntities = source.protectBlockEntities;
        copy.playerMining = source.playerMining;
        copy.explosions = source.explosions;
        copy.automatedMining = source.automatedMining;
        copy.multiplyExperience = source.multiplyExperience;
        copy.experienceMultiplier = source.experienceMultiplier;
        copy.conservativePistonProtection = source.conservativePistonProtection;
        copy.allowPlayerOverrides = source.allowPlayerOverrides;
        copy.maxPlayerMultiplier = source.maxPlayerMultiplier;
        copy.statisticsEnabled = source.statisticsEnabled;
        copy.entityDropsEnabled = source.entityDropsEnabled;
        copy.inheritDefaultEntityMultiplier = source.inheritDefaultEntityMultiplier;
        copy.defaultEntityMultiplier = source.defaultEntityMultiplier;
        copy.entityKillRequirement = source.entityKillRequirement;
        copy.entityFilterMode = source.entityFilterMode;
        copy.bossDropsEnabled = source.bossDropsEnabled;
        copy.multiplyMobExperience = source.multiplyMobExperience;
        copy.mobExperienceMultiplier = source.mobExperienceMultiplier;
        copy.multiplyBossExperience = source.multiplyBossExperience;
        copy.manualShearingDropsEnabled = source.manualShearingDropsEnabled;
        copy.automatedShearingDropsEnabled = source.automatedShearingDropsEnabled;
        copy.inheritDefaultShearingMultiplier = source.inheritDefaultShearingMultiplier;
        copy.defaultShearingMultiplier = source.defaultShearingMultiplier;
        copy.dimensionMultipliers = copyMap(source.dimensionMultipliers);
        copy.categoryMultipliers = copyMap(source.categoryMultipliers);
        copy.blockMultipliers = copyMap(source.blockMultipliers);
        copy.blacklist = copySet(source.blacklist);
        copy.whitelist = copySet(source.whitelist);
        copy.tagBlacklist = copySet(source.tagBlacklist);
        copy.tagWhitelist = copySet(source.tagWhitelist);
        copy.blockEntityAllowlist = copySet(source.blockEntityAllowlist);
        copy.playerMultipliers = copyMap(source.playerMultipliers);
        copy.entityCategoryMultipliers = copyMap(source.entityCategoryMultipliers);
        copy.entityMultipliers = copyMap(source.entityMultipliers);
        copy.entityBlacklist = copySet(source.entityBlacklist);
        copy.entityWhitelist = copySet(source.entityWhitelist);
        copy.entityTagBlacklist = copySet(source.entityTagBlacklist);
        copy.entityTagWhitelist = copySet(source.entityTagWhitelist);
        copy.shearingEntityMultipliers = copyMap(source.shearingEntityMultipliers);
        return copy;
    }

    private static <V> LinkedHashMap<String, V> copyMap(final Map<String, V> source) {
        return source == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source);
    }

    private static LinkedHashSet<String> copySet(final Set<String> source) {
        return source == null ? new LinkedHashSet<>() : new LinkedHashSet<>(source);
    }

    private static String normalizeKey(final String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeEntityId(final String value) {
        final String normalized = normalizeKey(value);
        return "minecraft:player".equals(normalized) ? "" : normalized;
    }

    private static String normalizeQuery(final String value) {
        return normalizeKey(value);
    }

    private static String normalizeTagQuery(final String value) {
        return normalizeTagId(value);
    }

    private static String normalizeTagId(final String value) {
        return SmartDropTags.normalizeTagId(value);
    }

    public enum SourceTier {
        BLOCK,
        CATEGORY,
        DIMENSION,
        ENTITY,
        ENTITY_CATEGORY,
        ENTITY_DEFAULT,
        GLOBAL
    }

    public enum FilterEntryState {
        NONE,
        WHITELIST,
        BLACKLIST
    }

    public record BlockInfo(
            String id,
            String displayName,
            String searchText,
            List<Category> categories,
            List<String> tagIds
    ) {
        public BlockInfo {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(displayName, "displayName");
            Objects.requireNonNull(searchText, "searchText");
            categories = List.copyOf(categories);
            tagIds = List.copyOf(tagIds);
        }
    }

    public record EntityInfo(
            String id,
            String displayName,
            String searchText,
            List<EntityCategory> categories,
            EntityCategory selectedCategory,
            List<String> tagIds,
            boolean categoryEstimated
    ) {
        public EntityInfo {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(displayName, "displayName");
            Objects.requireNonNull(searchText, "searchText");
            categories = List.copyOf(categories);
            Objects.requireNonNull(selectedCategory, "selectedCategory");
            tagIds = List.copyOf(tagIds);
        }
    }

    public record ShearingInfo(
            String id,
            String displayName,
            String searchText,
            ShearingClassification classification,
            boolean tagConflict
    ) {
        public ShearingInfo {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(displayName, "displayName");
            Objects.requireNonNull(searchText, "searchText");
            Objects.requireNonNull(classification, "classification");
        }
    }

    public record EffectiveValue(int multiplier, SourceTier sourceTier, String sourceKey) {
        public EffectiveValue {
            Objects.requireNonNull(sourceTier, "sourceTier");
            Objects.requireNonNull(sourceKey, "sourceKey");
        }
    }

    private record Catalog(
            List<BlockInfo> blocks,
            Map<String, BlockInfo> byId,
            Map<Category, List<BlockInfo>> byCategory,
            List<String> tagIds
    ) {
    }

    private record EntityCatalog(
            List<EntityInfo> entities,
            Map<String, EntityInfo> byId,
            Map<EntityCategory, List<EntityInfo>> byCategory,
            List<String> tagIds
    ) {
    }

    private record ShearingCatalog(
            List<ShearingInfo> entities,
            Map<String, ShearingInfo> byId
    ) {
    }
}
