package com.chedidandrew.smartresourcedrops.config;

import com.chedidandrew.smartresourcedrops.core.shearing.ShearingTags;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Pure validation engine. It reads a detached config snapshot and a narrow registry view only. */
public final class ConfigValidator {
    static final int MAX_RETAINED_ISSUES = 512;
    static final int HIGH_MULTIPLIER_THRESHOLD = 16;
    private static final int NEAR_LIMIT_PERCENT = 90;
    private static final String STANDARD_SHEARING_TAG =
            ShearingTags.STANDARD_RESOURCES.location().toString();
    private static final String SPECIAL_SHEARING_TAG =
            ShearingTags.SPECIAL.location().toString();

    private ConfigValidator() {
    }

    public static ConfigValidationReport validate(
            final ConfigManager.ValidationSnapshot snapshot,
            final ConfigRegistryView registries
    ) {
        if (snapshot == null || registries == null) {
            throw new IllegalArgumentException("Validation inputs cannot be null");
        }

        final SmartDropsConfig config = snapshot.config();
        final ReportBuilder report = new ReportBuilder(snapshot);

        validateState(snapshot, config, report);
        validateBlockIds(config, registries, report);
        validateBlockTags(config, registries, report);
        validateEntityIds(config, registries, report);
        validateEntityTags(config, registries, report);
        validateShearing(config, registries, report);
        validateDimensions(config, registries, report);
        validateFilterConflicts(config, report);
        validateWhitelistModes(config, report);
        validateBlockEntitySafety(config, registries, report);
        validateRiskAdvisories(config, report);
        validateLoadDiagnostics(snapshot.loadDiagnostics(), report);

        return report.build();
    }

    private static void validateState(
            final ConfigManager.ValidationSnapshot snapshot,
            final SmartDropsConfig config,
            final ReportBuilder report
    ) {
        if (snapshot.writesSuppressed()) {
            report.add(ValidationIssue.of(
                    ValidationSeverity.ERROR,
                    ValidationCode.CONFIG_WRITES_SUPPRESSED,
                    "Configuration writes are suppressed. Resolve or explicitly reset the active file before editing."));
        }
        if (config.schemaVersion != SmartDropsConfig.CURRENT_SCHEMA) {
            report.add(ValidationIssue.at(
                    ValidationSeverity.ERROR,
                    ValidationCode.UNSUPPORTED_SCHEMA,
                    "The authoritative snapshot does not use supported schema "
                            + SmartDropsConfig.CURRENT_SCHEMA + ".",
                    String.valueOf(config.schemaVersion),
                    "schemaVersion"));
        }
        if (snapshot.revision() < 0L) {
            report.add(ValidationIssue.of(
                    ValidationSeverity.ERROR,
                    ValidationCode.INVALID_CONFIG_REVISION,
                    "The configuration revision is invalid."));
        }

        final int blockRules = config.blockRuleEntryCount();
        final int entityRules = config.entityRuleEntryCount();
        final int shearingRules = config.shearingRuleEntryCount();
        validateRuleBudget(
                blockRules,
                SmartDropsConfig.MAX_BLOCK_RULE_ENTRIES,
                ValidationCode.BLOCK_RULE_BUDGET_EXCEEDED,
                ValidationCode.BLOCK_RULE_BUDGET_NEAR_LIMIT,
                "Block",
                report);
        validateRuleBudget(
                entityRules,
                SmartDropsConfig.MAX_ENTITY_RULE_ENTRIES,
                ValidationCode.ENTITY_RULE_BUDGET_EXCEEDED,
                ValidationCode.ENTITY_RULE_BUDGET_NEAR_LIMIT,
                "Entity",
                report);
        validateRuleBudget(
                shearingRules,
                SmartDropsConfig.MAX_SHEARING_RULE_ENTRIES,
                ValidationCode.SHEARING_RULE_BUDGET_EXCEEDED,
                ValidationCode.SHEARING_RULE_BUDGET_NEAR_LIMIT,
                "Shearing",
                report);

        scalarInRange(config.maximumMultiplier, 1, SmartDropsConfig.ABSOLUTE_MAX_MULTIPLIER,
                "maximumMultiplier", report);
        final int configuredMaximum = Math.max(0, config.maximumMultiplier);
        scalarInRange(config.globalMultiplier, 0, configuredMaximum, "globalMultiplier", report);
        scalarInRange(config.experienceMultiplier, 1, configuredMaximum, "experienceMultiplier", report);
        scalarInRange(config.maxPlayerMultiplier, 1, configuredMaximum, "maxPlayerMultiplier", report);
        scalarInRange(config.defaultEntityMultiplier, 0, configuredMaximum, "defaultEntityMultiplier", report);
        scalarInRange(
                config.defaultShearingMultiplier,
                0,
                configuredMaximum,
                "defaultShearingMultiplier",
                report);
        scalarInRange(config.mobExperienceMultiplier, 1, configuredMaximum, "mobExperienceMultiplier", report);
        if (config.sourceMode == null) {
            invalidScalar("sourceMode", report);
        }
        if (config.filterMode == null) {
            invalidScalar("filterMode", report);
        }
        if (config.entityKillRequirement == null) {
            invalidScalar("entityKillRequirement", report);
        }
        if (config.entityFilterMode == null) {
            invalidScalar("entityFilterMode", report);
        }
    }

    private static void validateRuleBudget(
            final int count,
            final int limit,
            final ValidationCode exceededCode,
            final ValidationCode nearCode,
            final String label,
            final ReportBuilder report
    ) {
        if (count > limit) {
            report.add(ValidationIssue.of(
                    ValidationSeverity.ERROR,
                    exceededCode,
                    label + " rule count " + count + " exceeds the supported limit of " + limit + "."));
        } else if ((long) count * 100L >= (long) limit * NEAR_LIMIT_PERCENT) {
            report.add(ValidationIssue.of(
                    ValidationSeverity.WARNING,
                    nearCode,
                    label + " rule count " + count + " is approaching the supported limit of " + limit + "."));
        }
    }

    private static void scalarInRange(
            final int value,
            final int minimum,
            final int maximum,
            final String path,
            final ReportBuilder report
    ) {
        if (value < minimum || value > maximum) {
            report.add(ValidationIssue.at(
                    ValidationSeverity.ERROR,
                    ValidationCode.INVALID_SCALAR_SETTING,
                    "Value " + value + " is outside the supported range " + minimum + ".." + maximum + ".",
                    String.valueOf(value),
                    path));
        }
    }

    private static void invalidScalar(final String path, final ReportBuilder report) {
        report.add(ValidationIssue.at(
                ValidationSeverity.ERROR,
                ValidationCode.INVALID_SCALAR_SETTING,
                "Required scalar setting is missing.",
                null,
                path));
    }

    private static void validateBlockIds(
            final SmartDropsConfig config,
            final ConfigRegistryView registries,
            final ReportBuilder report
    ) {
        checkBlockIds(keys(config.blockMultipliers), "blockMultipliers", registries, report);
        checkBlockIds(values(config.blacklist), "blacklist", registries, report);
        checkBlockIds(values(config.whitelist), "whitelist", registries, report);
    }

    private static void checkBlockIds(
            final Collection<String> identifiers,
            final String path,
            final ConfigRegistryView registries,
            final ReportBuilder report
    ) {
        for (String identifier : identifiers) {
            if (!registries.blockExists(identifier)) {
                report.add(ValidationIssue.at(
                        ValidationSeverity.WARNING,
                        ValidationCode.UNKNOWN_BLOCK_ID,
                        "Referenced block is not present in the current registry; a temporarily removed mod may own it.",
                        identifier,
                        path));
            }
        }
    }

    private static void validateBlockTags(
            final SmartDropsConfig config,
            final ConfigRegistryView registries,
            final ReportBuilder report
    ) {
        checkBlockTags(values(config.tagBlacklist), "tagBlacklist", registries, report);
        checkBlockTags(values(config.tagWhitelist), "tagWhitelist", registries, report);
    }

    private static void checkBlockTags(
            final Collection<String> identifiers,
            final String path,
            final ConfigRegistryView registries,
            final ReportBuilder report
    ) {
        for (String identifier : identifiers) {
            if (!registries.blockTagBound(identifier)) {
                report.add(ValidationIssue.at(
                        ValidationSeverity.WARNING,
                        ValidationCode.MISSING_BLOCK_TAG,
                        "Configured block tag is not bound in the current data-pack registry.",
                        identifier,
                        path));
            }
        }
    }

    private static void validateEntityIds(
            final SmartDropsConfig config,
            final ConfigRegistryView registries,
            final ReportBuilder report
    ) {
        checkEntityIds(keys(config.entityMultipliers), "entityMultipliers", registries, report);
        checkEntityIds(values(config.entityBlacklist), "entityBlacklist", registries, report);
        checkEntityIds(values(config.entityWhitelist), "entityWhitelist", registries, report);
    }

    private static void checkEntityIds(
            final Collection<String> identifiers,
            final String path,
            final ConfigRegistryView registries,
            final ReportBuilder report
    ) {
        for (String identifier : identifiers) {
            if ("minecraft:player".equals(identifier)) {
                report.add(ValidationIssue.at(
                        ValidationSeverity.ERROR,
                        ValidationCode.INVALID_ENTITY_TARGET,
                        "minecraft:player is not a supported configurable entity target.",
                        identifier,
                        path));
            } else if (!registries.entityExists(identifier)) {
                report.add(ValidationIssue.at(
                        ValidationSeverity.WARNING,
                        ValidationCode.UNKNOWN_ENTITY_ID,
                        "Referenced entity type is not present in the current registry; a temporarily removed mod may own it.",
                        identifier,
                        path));
            }
        }
    }

    private static void validateEntityTags(
            final SmartDropsConfig config,
            final ConfigRegistryView registries,
            final ReportBuilder report
    ) {
        checkEntityTags(values(config.entityTagBlacklist), "entityTagBlacklist", registries, report);
        checkEntityTags(values(config.entityTagWhitelist), "entityTagWhitelist", registries, report);
    }

    private static void validateShearing(
            final SmartDropsConfig config,
            final ConfigRegistryView registries,
            final ReportBuilder report
    ) {
        final boolean standardTagBound = registries.entityTagBound(STANDARD_SHEARING_TAG);
        final boolean specialTagBound = registries.entityTagBound(SPECIAL_SHEARING_TAG);
        if (!standardTagBound) {
            report.add(ValidationIssue.at(
                    ValidationSeverity.WARNING,
                    ValidationCode.MISSING_SHEARING_TAG,
                    "The project standard-resource shearing tag is not bound; all shearables fail closed to vanilla 1x.",
                    STANDARD_SHEARING_TAG,
                    null));
        }
        if (!specialTagBound) {
            report.add(ValidationIssue.at(
                    ValidationSeverity.WARNING,
                    ValidationCode.MISSING_SHEARING_TAG,
                    "The project special-shearable safety tag is not bound; review the active data packs.",
                    SPECIAL_SHEARING_TAG,
                    null));
        }

        final Set<String> standard = standardTagBound
                ? registries.entityIdsInTag(STANDARD_SHEARING_TAG)
                : Set.of();
        final Set<String> special = specialTagBound
                ? registries.entityIdsInTag(SPECIAL_SHEARING_TAG)
                : Set.of();

        // Bound holder sets expose only successfully resolved registry members. They cannot
        // reveal unresolved optional/required identifiers that appeared in the source JSON.
        for (String identifier : standard) {
            if (!registries.entityExists(identifier)) {
                report.add(ValidationIssue.at(
                        ValidationSeverity.WARNING,
                        ValidationCode.UNKNOWN_SHEARING_ENTITY,
                        "A bound standard-resource shearing member is absent from the current entity registry.",
                        identifier,
                        STANDARD_SHEARING_TAG));
            }
            if (special.contains(identifier) || ShearingTags.isKnownVanillaSpecial(identifier)) {
                report.add(ValidationIssue.at(
                        ValidationSeverity.WARNING,
                        ValidationCode.SHEARING_TAG_CONFLICT,
                        "Entity has conflicting standard-resource and special safety classification; special safety wins and remains vanilla 1x.",
                        identifier,
                        null));
            }
        }
        for (String identifier : special) {
            if (!registries.entityExists(identifier)) {
                report.add(ValidationIssue.at(
                        ValidationSeverity.WARNING,
                        ValidationCode.UNKNOWN_SHEARING_ENTITY,
                        "A bound special-shearable member is absent from the current entity registry.",
                        identifier,
                        SPECIAL_SHEARING_TAG));
            }
        }

        final Map<String, Integer> overrides = config.shearingEntityMultipliers == null
                ? Map.of()
                : config.shearingEntityMultipliers;
        final int maximum = Math.max(0, config.maximumMultiplier);
        for (Map.Entry<String, Integer> entry : overrides.entrySet()) {
            final String identifier = entry.getKey();
            final Integer multiplier = entry.getValue();
            if (!registries.entityExists(identifier)) {
                report.add(ValidationIssue.at(
                        ValidationSeverity.WARNING,
                        ValidationCode.UNKNOWN_SHEARING_ENTITY,
                        "Referenced shearing entity type is not present in the current registry.",
                        identifier,
                        "shearingEntityMultipliers"));
            }
            if (special.contains(identifier) || ShearingTags.isKnownVanillaSpecial(identifier)) {
                report.add(ValidationIssue.at(
                        ValidationSeverity.WARNING,
                        ValidationCode.SPECIAL_SHEARING_OVERRIDE_IGNORED,
                        "This override is unreachable because special shearing transformations are fixed at vanilla 1x.",
                        identifier,
                        "shearingEntityMultipliers"));
            } else if (!standard.contains(identifier)) {
                report.add(ValidationIssue.at(
                        ValidationSeverity.WARNING,
                        ValidationCode.UNSAFE_SHEARING_OVERRIDE,
                        "This override cannot apply until the entity is certified by the standard-resources shearing tag.",
                        identifier,
                        "shearingEntityMultipliers"));
            }
            if (multiplier == null || multiplier < 0 || multiplier > maximum) {
                report.add(ValidationIssue.at(
                        ValidationSeverity.ERROR,
                        ValidationCode.INVALID_SCALAR_SETTING,
                        "Shearing multiplier is outside the supported range 0.." + maximum + ".",
                        String.valueOf(multiplier),
                        "shearingEntityMultipliers." + identifier));
            }
        }

        if (config.automatedShearingDropsEnabled) {
            report.add(ValidationIssue.of(
                    ValidationSeverity.WARNING,
                    ValidationCode.SHEARING_AUTOMATION_ENABLED,
                    "Vanilla dispenser entity shearing multiplication is enabled; review automated farm throughput."));
            final int shearingDefault = config.inheritDefaultShearingMultiplier
                    ? config.globalMultiplier
                    : config.defaultShearingMultiplier;
            int highestReachable = 0;
            for (String identifier : standard) {
                if (special.contains(identifier) || ShearingTags.isKnownVanillaSpecial(identifier)) {
                    continue;
                }
                final Integer exact = overrides.get(identifier);
                highestReachable = Math.max(
                        highestReachable,
                        exact == null ? shearingDefault : exact);
            }
            if (highestReachable >= HIGH_MULTIPLIER_THRESHOLD) {
                report.add(ValidationIssue.of(
                        ValidationSeverity.WARNING,
                        ValidationCode.SHEARING_HIGH_OUTPUT_CONFIGURATION,
                        "Automated shearing is enabled with a reachable multiplier up to "
                                + highestReachable
                                + "x. The action output budget remains enforced."));
            }
        }
    }

    private static void checkEntityTags(
            final Collection<String> identifiers,
            final String path,
            final ConfigRegistryView registries,
            final ReportBuilder report
    ) {
        for (String identifier : identifiers) {
            if (!registries.entityTagBound(identifier)) {
                report.add(ValidationIssue.at(
                        ValidationSeverity.WARNING,
                        ValidationCode.MISSING_ENTITY_TAG,
                        "Configured entity-type tag is not bound in the current data-pack registry.",
                        identifier,
                        path));
            }
        }
    }

    private static void validateDimensions(
            final SmartDropsConfig config,
            final ConfigRegistryView registries,
            final ReportBuilder report
    ) {
        for (String identifier : keys(config.dimensionMultipliers)) {
            if (!registries.dimensionExists(identifier)) {
                report.add(ValidationIssue.at(
                        ValidationSeverity.WARNING,
                        ValidationCode.UNKNOWN_DIMENSION_ID,
                        "Configured block dimension is not loaded on the current server.",
                        identifier,
                        "dimensionMultipliers"));
            }
        }
    }

    private static void validateFilterConflicts(
            final SmartDropsConfig config,
            final ReportBuilder report
    ) {
        conflicts(values(config.blacklist), values(config.whitelist), ValidationCode.CONFLICTING_BLOCK_FILTER,
                "Block ID exists in both blacklist and whitelist; only the selected filter mode is active.", report);
        conflicts(values(config.tagBlacklist), values(config.tagWhitelist), ValidationCode.CONFLICTING_BLOCK_TAG_FILTER,
                "Block tag exists in both tag lists; only the selected filter mode is active.", report);
        conflicts(values(config.entityBlacklist), values(config.entityWhitelist), ValidationCode.CONFLICTING_ENTITY_FILTER,
                "Entity ID exists in both blacklist and whitelist; only the selected filter mode is active.", report);
        conflicts(
                values(config.entityTagBlacklist),
                values(config.entityTagWhitelist),
                ValidationCode.CONFLICTING_ENTITY_TAG_FILTER,
                "Entity tag exists in both tag lists; only the selected filter mode is active.",
                report);
    }

    private static void conflicts(
            final Set<String> first,
            final Set<String> second,
            final ValidationCode code,
            final String message,
            final ReportBuilder report
    ) {
        for (String identifier : first) {
            if (second.contains(identifier)) {
                report.add(ValidationIssue.at(ValidationSeverity.WARNING, code, message, identifier, null));
            }
        }
    }

    private static void validateWhitelistModes(
            final SmartDropsConfig config,
            final ReportBuilder report
    ) {
        if (config.filterMode == SmartDropsConfig.FilterMode.WHITELIST
                && values(config.whitelist).isEmpty()
                && values(config.tagWhitelist).isEmpty()) {
            report.add(ValidationIssue.of(
                    ValidationSeverity.WARNING,
                    ValidationCode.EMPTY_BLOCK_WHITELIST,
                    "Block whitelist mode is active but both exact and tag whitelists are empty; no blocks qualify."));
        }
        if (config.entityFilterMode == SmartDropsConfig.FilterMode.WHITELIST
                && values(config.entityWhitelist).isEmpty()
                && values(config.entityTagWhitelist).isEmpty()) {
            report.add(ValidationIssue.of(
                    config.entityDropsEnabled ? ValidationSeverity.WARNING : ValidationSeverity.INFO,
                    ValidationCode.EMPTY_ENTITY_WHITELIST,
                    config.entityDropsEnabled
                            ? "Entity whitelist mode is active but both exact and tag whitelists are empty; no entities qualify."
                            : "Entity whitelist mode is empty, but entity drops are currently disabled."));
        }
    }

    private static void validateBlockEntitySafety(
            final SmartDropsConfig config,
            final ConfigRegistryView registries,
            final ReportBuilder report
    ) {
        if (!config.protectBlockEntities) {
            report.add(ValidationIssue.of(
                    ValidationSeverity.WARNING,
                    ValidationCode.BLOCK_ENTITY_PROTECTION_DISABLED,
                    "Block-entity protection is disabled. This is a compatibility-sensitive setting; review recommended."));
        }
        final Set<String> allowlist = values(config.blockEntityAllowlist);
        if (!allowlist.isEmpty()) {
            report.add(ValidationIssue.of(
                    ValidationSeverity.INFO,
                    ValidationCode.BLOCK_ENTITY_ALLOWLIST_CONFIGURED,
                    "Block-entity allowlist contains " + allowlist.size()
                            + " entries. Capability checks use registered default states and are intentionally limited."));
        }
        for (String identifier : allowlist) {
            final ConfigRegistryView.BlockEntityCapability capability = registries.blockEntityCapability(identifier);
            if (capability == ConfigRegistryView.BlockEntityCapability.UNKNOWN) {
                report.add(ValidationIssue.at(
                        ValidationSeverity.WARNING,
                        ValidationCode.BLOCK_ENTITY_ALLOWLIST_ENTRY_UNRESOLVED,
                        "Allowlisted block is not present in the current block registry.",
                        identifier,
                        "blockEntityAllowlist"));
            } else if (capability == ConfigRegistryView.BlockEntityCapability.NO) {
                report.add(ValidationIssue.at(
                        ValidationSeverity.WARNING,
                        ValidationCode.BLOCK_ENTITY_ALLOWLIST_ENTRY_NOT_BLOCK_ENTITY,
                        "The registered default state does not report a block entity; unusual modded states may require manual review.",
                        identifier,
                        "blockEntityAllowlist"));
            }
        }
    }

    private static void validateRiskAdvisories(
            final SmartDropsConfig config,
            final ReportBuilder report
    ) {
        final int highestBlockMultiplier = maximum(
                config.globalMultiplier,
                config.dimensionMultipliers,
                config.categoryMultipliers,
                config.blockMultipliers);
        if (highestBlockMultiplier >= HIGH_MULTIPLIER_THRESHOLD) {
            report.add(ValidationIssue.of(
                    ValidationSeverity.INFO,
                    ValidationCode.HIGH_BLOCK_MULTIPLIER,
                    "High-output block configuration reaches " + highestBlockMultiplier + "x; review recommended."));
        }
        if (config.automatedMining && highestBlockMultiplier >= HIGH_MULTIPLIER_THRESHOLD) {
            report.add(ValidationIssue.of(
                    ValidationSeverity.WARNING,
                    ValidationCode.AUTOMATION_WITH_HIGH_MULTIPLIER,
                    "Automated mining is enabled with a configured block multiplier up to "
                            + highestBlockMultiplier + "x. This is a high-output configuration."));
        }

        final int entityDefault = config.inheritDefaultEntityMultiplier
                ? config.globalMultiplier
                : config.defaultEntityMultiplier;
        final int highestEntityMultiplier = maximum(
                entityDefault,
                config.entityCategoryMultipliers,
                config.entityMultipliers);
        if (config.entityDropsEnabled) {
            report.add(ValidationIssue.of(
                    ValidationSeverity.INFO,
                    ValidationCode.ENTITY_DROPS_ENABLED,
                    "Entity death-loot multiplication is enabled."));
            if (highestEntityMultiplier >= HIGH_MULTIPLIER_THRESHOLD) {
                report.add(ValidationIssue.of(
                        ValidationSeverity.INFO,
                        ValidationCode.HIGH_ENTITY_MULTIPLIER,
                        "High-output entity configuration reaches " + highestEntityMultiplier + "x; review recommended."));
            }
            if (entityDefault == 1
                    && allValuesEqual(config.entityCategoryMultipliers, 1)
                    && allValuesEqual(config.entityMultipliers, 1)) {
                report.add(ValidationIssue.of(
                        ValidationSeverity.INFO,
                        ValidationCode.ENTITY_DROPS_EFFECTIVELY_VANILLA,
                        "Entity drops are enabled, but every effective default/category/exact entity multiplier is 1x."));
            }
        }

        if (config.multiplyMobExperience
                && config.mobExperienceMultiplier >= HIGH_MULTIPLIER_THRESHOLD) {
            report.add(ValidationIssue.of(
                    ValidationSeverity.WARNING,
                    ValidationCode.HIGH_MOB_XP_MULTIPLIER,
                    "Mob XP multiplication is enabled at " + config.mobExperienceMultiplier
                            + "x. This is a high-output configuration; review recommended."));
        }

        if (config.entityKillRequirement == SmartDropsConfig.EntityKillRequirement.ALL_STANDARD_DEATH_LOOT) {
            report.add(ValidationIssue.of(
                    highestEntityMultiplier >= HIGH_MULTIPLIER_THRESHOLD
                            ? ValidationSeverity.WARNING
                            : ValidationSeverity.INFO,
                    highestEntityMultiplier >= HIGH_MULTIPLIER_THRESHOLD
                            ? ValidationCode.ALL_DEATHS_WITH_HIGH_MULTIPLIER
                            : ValidationCode.ALL_STANDARD_DEATH_LOOT,
                    highestEntityMultiplier >= HIGH_MULTIPLIER_THRESHOLD
                            ? "All standard death loot is enabled with a multiplier up to "
                                    + highestEntityMultiplier + "x; compatibility review recommended."
                            : "All standard death loot is enabled; environmental and automated deaths may qualify."));
        }
        if (config.bossDropsEnabled) {
            report.add(ValidationIssue.of(
                    ValidationSeverity.WARNING,
                    ValidationCode.BOSS_DROPS_ENABLED,
                    "Boss item multiplication is enabled. This is a compatibility-sensitive setting; review recommended."));
        }
        if (config.multiplyBossExperience) {
            report.add(ValidationIssue.of(
                    ValidationSeverity.WARNING,
                    ValidationCode.BOSS_XP_ENABLED,
                    "Boss XP multiplication is enabled when mob XP multiplication applies; review recommended."));
        }
        if (!config.conservativePistonProtection) {
            report.add(ValidationIssue.of(
                    ValidationSeverity.WARNING,
                    ValidationCode.CONSERVATIVE_PISTON_PROTECTION_DISABLED,
                    "Conservative piston protection is disabled. Custom movement systems may need manual compatibility review."));
        }
        if (config.allowPlayerOverrides) {
            report.add(ValidationIssue.of(
                    ValidationSeverity.INFO,
                    ValidationCode.PLAYER_OVERRIDES_ENABLED,
                    "Player multiplier overrides are enabled with " + size(config.playerMultipliers)
                            + " stored rule(s). UUID values are intentionally omitted."));
        }

        final int zeroRules = countZeros(config.dimensionMultipliers)
                + countZeros(config.categoryMultipliers)
                + countZeros(config.blockMultipliers)
                + countZeros(config.entityCategoryMultipliers)
                + countZeros(config.entityMultipliers)
                + countZeros(config.shearingEntityMultipliers);
        if (zeroRules > 0) {
            report.add(ValidationIssue.of(
                    ValidationSeverity.INFO,
                    ValidationCode.ZERO_MULTIPLIER_RULE,
                    zeroRules + " configured multiplier rule(s) suppress qualifying output at 0x."));
        }
    }

    private static void validateLoadDiagnostics(
            final ConfigLoadDiagnostics diagnostics,
            final ReportBuilder report
    ) {
        if (diagnostics.unsupportedSchemaRejected()) {
            report.add(ValidationIssue.of(
                    ValidationSeverity.ERROR,
                    ValidationCode.UNSUPPORTED_SCHEMA,
                    "The most recent load rejected a newer unsupported schema and retained safe in-memory settings."));
        }
        if (diagnostics.readFailed()) {
            report.add(ValidationIssue.of(
                    ValidationSeverity.ERROR,
                    ValidationCode.CONFIG_READ_FAILED,
                    "The most recent configuration read failed; the existing file was not overwritten."));
        }
        if (diagnostics.writeFailed()) {
            report.add(ValidationIssue.of(
                    ValidationSeverity.ERROR,
                    ValidationCode.CONFIG_WRITE_FAILED,
                    "The most recent load could not safely persist its candidate configuration."));
        }
        if (diagnostics.migratedFromSchema() != ConfigLoadDiagnostics.NO_MIGRATION) {
            report.add(ValidationIssue.of(
                    ValidationSeverity.INFO,
                    ValidationCode.CONFIG_MIGRATED,
                    "The most recent load migrated schema " + diagnostics.migratedFromSchema()
                            + " to schema " + SmartDropsConfig.CURRENT_SCHEMA + backupSuffix(diagnostics) + "."));
        }
        if (diagnostics.malformedFileRecovered()) {
            report.add(ValidationIssue.of(
                    ValidationSeverity.WARNING,
                    ValidationCode.MALFORMED_CONFIG_RECOVERED,
                    "A malformed configuration was detected during the most recent load"
                            + backupSuffix(diagnostics) + "."));
        }
        if (diagnostics.oversizedFileDetected()) {
            report.add(ValidationIssue.of(
                    ValidationSeverity.WARNING,
                    ValidationCode.OVERSIZED_CONFIG_RECOVERED,
                    "The most recent load exceeded a rule-domain budget; a bounded candidate was prepared"
                            + backupSuffix(diagnostics) + "."));
        }
        final int invalidEntries = diagnostics.invalidIdentifiersRemoved()
                + diagnostics.invalidCategoryNamesRemoved()
                + diagnostics.invalidPlayerOverridesRemoved();
        if (invalidEntries > 0) {
            report.add(ValidationIssue.of(
                    ValidationSeverity.INFO,
                    ValidationCode.LOAD_INVALID_ENTRIES_REMOVED,
                    "The most recent load removed " + invalidEntries + " invalid entr"
                            + (invalidEntries == 1 ? "y" : "ies") + "."));
        }
        if (diagnostics.valuesAdjusted() > 0) {
            report.add(ValidationIssue.of(
                    ValidationSeverity.INFO,
                    ValidationCode.LOAD_VALUES_ADJUSTED,
                    "The most recent load adjusted " + diagnostics.valuesAdjusted() + " out-of-range or missing value(s)."));
        }
        if (diagnostics.blockEntriesOverBudget() > 0) {
            report.add(ValidationIssue.of(
                    ValidationSeverity.WARNING,
                    ValidationCode.BLOCK_RULES_TRUNCATED_AT_LOAD,
                    diagnostics.blockEntriesOverBudget() + " block-domain entr"
                            + (diagnostics.blockEntriesOverBudget() == 1 ? "y was" : "ies were")
                            + " beyond the load budget."));
        }
        if (diagnostics.entityEntriesOverBudget() > 0) {
            report.add(ValidationIssue.of(
                    ValidationSeverity.WARNING,
                    ValidationCode.ENTITY_RULES_TRUNCATED_AT_LOAD,
                    diagnostics.entityEntriesOverBudget() + " entity-domain entr"
                            + (diagnostics.entityEntriesOverBudget() == 1 ? "y was" : "ies were")
                            + " beyond the load budget."));
        }
        if (diagnostics.shearingEntriesOverBudget() > 0) {
            report.add(ValidationIssue.of(
                    ValidationSeverity.WARNING,
                    ValidationCode.SHEARING_RULES_TRUNCATED,
                    diagnostics.shearingEntriesOverBudget() + " shearing-domain entr"
                            + (diagnostics.shearingEntriesOverBudget() == 1 ? "y was" : "ies were")
                            + " beyond the load budget."));
        }
        for (ConfigLoadDiagnostics.Sample sample : diagnostics.samples()) {
            report.add(ValidationIssue.at(
                    ValidationSeverity.INFO,
                    "adjusted value".equals(sample.action())
                            ? ValidationCode.LOAD_VALUE_ADJUSTED
                            : ValidationCode.LOAD_ENTRY_REMOVED,
                    "Load diagnostic: " + sample.action() + ".",
                    sample.value(),
                    sample.settingPath()));
        }
    }

    private static String backupSuffix(final ConfigLoadDiagnostics diagnostics) {
        if (diagnostics.backupFileNames().isEmpty()) {
            return "";
        }
        return "; backup: " + String.join(", ", diagnostics.backupFileNames());
    }

    @SafeVarargs
    private static int maximum(final int scalar, final Map<String, Integer>... maps) {
        int maximum = scalar;
        for (Map<String, Integer> map : maps) {
            if (map == null) {
                continue;
            }
            for (Integer value : map.values()) {
                if (value != null) {
                    maximum = Math.max(maximum, value);
                }
            }
        }
        return maximum;
    }

    private static int countZeros(final Map<String, Integer> map) {
        if (map == null) {
            return 0;
        }
        int count = 0;
        for (Integer value : map.values()) {
            if (value != null && value == 0) {
                count++;
            }
        }
        return count;
    }

    private static boolean allValuesEqual(final Map<String, Integer> map, final int expected) {
        if (map == null) {
            return true;
        }
        for (Integer value : map.values()) {
            if (value == null || value != expected) {
                return false;
            }
        }
        return true;
    }

    private static Set<String> keys(final Map<String, ?> map) {
        return map == null ? Set.of() : map.keySet();
    }

    private static Set<String> values(final Set<String> set) {
        return set == null ? Set.of() : set;
    }

    private static int size(final Map<?, ?> map) {
        return map == null ? 0 : map.size();
    }

    private static final class ReportBuilder {
        private final ConfigManager.ValidationSnapshot snapshot;
        private final List<ValidationIssue> issues = new ArrayList<>();
        private int errors;
        private int warnings;
        private int infos;
        private int omitted;

        private ReportBuilder(final ConfigManager.ValidationSnapshot snapshot) {
            this.snapshot = snapshot;
        }

        private void add(final ValidationIssue issue) {
            switch (issue.severity()) {
                case ERROR -> errors++;
                case WARNING -> warnings++;
                case INFO -> infos++;
            }
            int insertionIndex = issues.size();
            for (int index = 0; index < issues.size(); index++) {
                if (issue.severity().ordinal() < issues.get(index).severity().ordinal()) {
                    insertionIndex = index;
                    break;
                }
            }
            if (issues.size() < MAX_RETAINED_ISSUES) {
                issues.add(insertionIndex, issue);
                return;
            }
            omitted++;
            if (insertionIndex < MAX_RETAINED_ISSUES) {
                issues.add(insertionIndex, issue);
                issues.remove(MAX_RETAINED_ISSUES);
            }
        }

        private ConfigValidationReport build() {
            final SmartDropsConfig config = snapshot.config();
            return new ConfigValidationReport(
                    snapshot.revision(),
                    config.schemaVersion,
                    config.blockRuleEntryCount(),
                    SmartDropsConfig.MAX_BLOCK_RULE_ENTRIES,
                    config.entityRuleEntryCount(),
                    SmartDropsConfig.MAX_ENTITY_RULE_ENTRIES,
                    config.shearingRuleEntryCount(),
                    SmartDropsConfig.MAX_SHEARING_RULE_ENTRIES,
                    values(config.blockEntityAllowlist).size(),
                    size(config.playerMultipliers),
                    errors,
                    warnings,
                    infos,
                    issues,
                    omitted,
                    snapshot.loadDiagnostics());
        }
    }
}
