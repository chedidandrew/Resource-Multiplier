package com.chedidandrew.smartresourcedrops.config;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ConfigValidatorTest {
    private static final ConfigRegistryView REGISTRIES = new FakeRegistryView();

    @Test
    void cleanDefaultsProduceNoErrorsOrWarnings() {
        ConfigValidationReport report = validate(SmartDropsConfig.defaults());

        assertEquals(0, report.errorCount());
        assertEquals(0, report.warningCount());
        assertEquals(0, report.totalIssueCount());
    }

    @Test
    void unknownConfiguredIdsAndDimensionsWarnWithoutMutatingConfiguration() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        config.blockMultipliers.put("missing:block", 3);
        config.entityMultipliers.put("missing:entity", 4);
        config.entityWhitelist.add("minecraft:player");
        config.dimensionMultipliers.put("missing:dimension", 5);

        ConfigValidationReport report = validate(config);

        assertCode(report, ValidationCode.UNKNOWN_BLOCK_ID, ValidationSeverity.WARNING);
        assertCode(report, ValidationCode.UNKNOWN_ENTITY_ID, ValidationSeverity.WARNING);
        assertCode(report, ValidationCode.INVALID_ENTITY_TARGET, ValidationSeverity.ERROR);
        assertCode(report, ValidationCode.UNKNOWN_DIMENSION_ID, ValidationSeverity.WARNING);
        assertTrue(config.blockMultipliers.containsKey("missing:block"));
        assertTrue(config.entityMultipliers.containsKey("missing:entity"));
        assertTrue(config.dimensionMultipliers.containsKey("missing:dimension"));
    }

    @Test
    void missingTagsWarnButBoundEmptyTagsRemainValid() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        config.tagBlacklist.add("missing:block_tag");
        config.tagWhitelist.add("example:present_empty");
        config.entityTagBlacklist.add("missing:entity_tag");
        config.entityTagWhitelist.add("example:present_empty");

        ConfigValidationReport report = validate(config);

        assertCode(report, ValidationCode.MISSING_BLOCK_TAG, ValidationSeverity.WARNING);
        assertCode(report, ValidationCode.MISSING_ENTITY_TAG, ValidationSeverity.WARNING);
        assertFalse(report.issues().stream().anyMatch(issue ->
                "example:present_empty".equals(issue.identifier())
                        && (issue.code() == ValidationCode.MISSING_BLOCK_TAG
                        || issue.code() == ValidationCode.MISSING_ENTITY_TAG)));
    }

    @Test
    void exactAndTagFilterConflictsAreReportedForBothDomains() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        config.blacklist.add("minecraft:stone");
        config.whitelist.add("minecraft:stone");
        config.tagBlacklist.add("minecraft:mineable/pickaxe");
        config.tagWhitelist.add("minecraft:mineable/pickaxe");
        config.entityBlacklist.add("minecraft:zombie");
        config.entityWhitelist.add("minecraft:zombie");
        config.entityTagBlacklist.add("minecraft:undead");
        config.entityTagWhitelist.add("minecraft:undead");

        ConfigValidationReport report = validate(config);

        assertCode(report, ValidationCode.CONFLICTING_BLOCK_FILTER, ValidationSeverity.WARNING);
        assertCode(report, ValidationCode.CONFLICTING_BLOCK_TAG_FILTER, ValidationSeverity.WARNING);
        assertCode(report, ValidationCode.CONFLICTING_ENTITY_FILTER, ValidationSeverity.WARNING);
        assertCode(report, ValidationCode.CONFLICTING_ENTITY_TAG_FILTER, ValidationSeverity.WARNING);
    }

    @Test
    void emptyWhitelistSeverityReflectsWhetherEntityFeatureIsActive() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        config.filterMode = SmartDropsConfig.FilterMode.WHITELIST;
        config.blacklist.clear();
        config.whitelist.clear();
        config.tagWhitelist.clear();
        config.entityFilterMode = SmartDropsConfig.FilterMode.WHITELIST;
        config.entityWhitelist.clear();
        config.entityTagWhitelist.clear();

        ConfigValidationReport disabled = validate(config);
        assertCode(disabled, ValidationCode.EMPTY_BLOCK_WHITELIST, ValidationSeverity.WARNING);
        assertCode(disabled, ValidationCode.EMPTY_ENTITY_WHITELIST, ValidationSeverity.INFO);

        config.entityDropsEnabled = true;
        ConfigValidationReport enabled = validate(config);
        assertCode(enabled, ValidationCode.EMPTY_ENTITY_WHITELIST, ValidationSeverity.WARNING);
    }

    @Test
    void blockEntitySafetyReportsProtectionAllowlistCountAndLimitedCapability() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        config.protectBlockEntities = false;
        config.blockEntityAllowlist.add("minecraft:chest");
        config.blockEntityAllowlist.add("minecraft:stone");
        config.blockEntityAllowlist.add("missing:block_entity");

        ConfigValidationReport report = validate(config);

        assertEquals(3, report.blockEntityAllowlistCount());
        assertCode(report, ValidationCode.BLOCK_ENTITY_PROTECTION_DISABLED, ValidationSeverity.WARNING);
        assertCode(report, ValidationCode.BLOCK_ENTITY_ALLOWLIST_CONFIGURED, ValidationSeverity.INFO);
        assertCode(report, ValidationCode.BLOCK_ENTITY_ALLOWLIST_ENTRY_NOT_BLOCK_ENTITY, ValidationSeverity.WARNING);
        assertCode(report, ValidationCode.BLOCK_ENTITY_ALLOWLIST_ENTRY_UNRESOLVED, ValidationSeverity.WARNING);
        assertFalse(report.issues().stream().map(ValidationIssue::message)
                .anyMatch(message -> message.contains("Items") || message.contains("LootTable")));
    }

    @Test
    void highRiskEntityAndAutomationSettingsAreAdvisoryRatherThanSilentlyRejected() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        config.globalMultiplier = 32;
        config.automatedMining = true;
        config.entityDropsEnabled = true;
        config.entityKillRequirement = SmartDropsConfig.EntityKillRequirement.ALL_STANDARD_DEATH_LOOT;
        config.bossDropsEnabled = true;
        config.multiplyBossExperience = true;
        config.multiplyMobExperience = true;
        config.mobExperienceMultiplier = 32;

        ConfigValidationReport report = validate(config);

        assertCode(report, ValidationCode.HIGH_BLOCK_MULTIPLIER, ValidationSeverity.INFO);
        assertCode(report, ValidationCode.AUTOMATION_WITH_HIGH_MULTIPLIER, ValidationSeverity.WARNING);
        assertCode(report, ValidationCode.HIGH_ENTITY_MULTIPLIER, ValidationSeverity.INFO);
        assertCode(report, ValidationCode.ALL_DEATHS_WITH_HIGH_MULTIPLIER, ValidationSeverity.WARNING);
        assertCode(report, ValidationCode.BOSS_DROPS_ENABLED, ValidationSeverity.WARNING);
        assertCode(report, ValidationCode.BOSS_XP_ENABLED, ValidationSeverity.WARNING);
        assertCode(report, ValidationCode.HIGH_MOB_XP_MULTIPLIER, ValidationSeverity.WARNING);
    }

    @Test
    void allDeathsAtNormalMultiplierAndVanillaOnlyEntityFeatureAreStillExplained() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        config.globalMultiplier = 1;
        config.entityDropsEnabled = true;
        config.entityKillRequirement = SmartDropsConfig.EntityKillRequirement.ALL_STANDARD_DEATH_LOOT;

        ConfigValidationReport report = validate(config);

        assertCode(report, ValidationCode.ALL_STANDARD_DEATH_LOOT, ValidationSeverity.INFO);
        assertCode(report, ValidationCode.ENTITY_DROPS_EFFECTIVELY_VANILLA, ValidationSeverity.INFO);

        config.entityMultipliers.put("minecraft:zombie", 0);
        ConfigValidationReport suppressive = validate(config);
        assertFalse(suppressive.issues().stream().anyMatch(issue ->
                issue.code() == ValidationCode.ENTITY_DROPS_EFFECTIVELY_VANILLA));
    }

    @Test
    void boundedRetentionAndRenderingPriorityCannotHideErrorsBehindWarnings() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        for (int index = 0; index < 600; index++) {
            config.blockMultipliers.put("missing:block_" + index, 2);
        }
        config.entityMultipliers.put("minecraft:player", 2);

        ConfigValidationReport report = validate(config);

        assertEquals(ConfigValidator.MAX_RETAINED_ISSUES, report.issues().size());
        assertEquals(ValidationSeverity.ERROR, report.issues().get(0).severity());
        assertCode(report, ValidationCode.INVALID_ENTITY_TARGET, ValidationSeverity.ERROR);
        assertTrue(report.omittedIssueCount() > 0);
    }

    @Test
    void nearLimitDomainWarnsAndOverLimitLoadIsRetainedAsDiagnostic() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        for (int index = 0; config.blockRuleEntryCount() * 100
                < SmartDropsConfig.MAX_BLOCK_RULE_ENTRIES * 90; index++) {
            config.blockMultipliers.put("example:block_" + index, 2);
        }

        ConfigValidationReport nearLimit = validate(config);
        assertCode(nearLimit, ValidationCode.BLOCK_RULE_BUDGET_NEAR_LIMIT, ValidationSeverity.WARNING);

        ConfigLoadDiagnostics.Builder diagnostics = new ConfigLoadDiagnostics.Builder();
        diagnostics.ruleBudgets(
                SmartDropsConfig.MAX_BLOCK_RULE_ENTRIES + 7,
                SmartDropsConfig.MAX_ENTITY_RULE_ENTRIES + 3,
                SmartDropsConfig.MAX_SHEARING_RULE_ENTRIES + 2);
        ConfigValidationReport loadReport = ConfigValidator.validate(
                new ConfigManager.ValidationSnapshot(config, 4L, false, diagnostics.build()),
                REGISTRIES);
        assertCode(loadReport, ValidationCode.BLOCK_RULES_TRUNCATED_AT_LOAD, ValidationSeverity.WARNING);
        assertCode(loadReport, ValidationCode.ENTITY_RULES_TRUNCATED_AT_LOAD, ValidationSeverity.WARNING);
        assertCode(loadReport, ValidationCode.SHEARING_RULES_TRUNCATED, ValidationSeverity.WARNING);
    }

    @Test
    void shearingValidationReportsUnreachableRulesConflictsAndAutomationRisk() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        config.automatedShearingDropsEnabled = true;
        config.globalMultiplier = 32;
        config.shearingEntityMultipliers.put("minecraft:mooshroom", 64);
        config.shearingEntityMultipliers.put("minecraft:zombie", 4);
        config.shearingEntityMultipliers.put("missing:shearable", 3);

        ConfigRegistryView conflictRegistry = new FakeRegistryView(
                Set.of("minecraft:sheep", "minecraft:mooshroom"),
                Set.of("minecraft:mooshroom"),
                true,
                true);
        ConfigValidationReport report = ConfigValidator.validate(
                new ConfigManager.ValidationSnapshot(config, 1L, false, ConfigLoadDiagnostics.empty()),
                conflictRegistry);

        assertCode(report, ValidationCode.SHEARING_AUTOMATION_ENABLED, ValidationSeverity.WARNING);
        assertCode(report, ValidationCode.SHEARING_HIGH_OUTPUT_CONFIGURATION, ValidationSeverity.WARNING);
        assertCode(report, ValidationCode.SHEARING_TAG_CONFLICT, ValidationSeverity.WARNING);
        assertCode(report, ValidationCode.SPECIAL_SHEARING_OVERRIDE_IGNORED, ValidationSeverity.WARNING);
        assertCode(report, ValidationCode.UNSAFE_SHEARING_OVERRIDE, ValidationSeverity.WARNING);
        assertCode(report, ValidationCode.UNKNOWN_SHEARING_ENTITY, ValidationSeverity.WARNING);
        assertEquals(3, report.shearingRuleCount());
        assertEquals(SmartDropsConfig.MAX_SHEARING_RULE_ENTRIES, report.shearingRuleLimit());
    }

    @Test
    void missingProjectShearingTagsFailClosedWithoutInventingUnresolvedMembers() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        ConfigRegistryView missingTags = new FakeRegistryView(Set.of(), Set.of(), false, false);

        ConfigValidationReport report = ConfigValidator.validate(
                new ConfigManager.ValidationSnapshot(config, 1L, false, ConfigLoadDiagnostics.empty()),
                missingTags);

        assertEquals(2L, report.issues().stream()
                .filter(issue -> issue.code() == ValidationCode.MISSING_SHEARING_TAG)
                .count());
        assertFalse(report.issues().stream().anyMatch(issue ->
                issue.code() == ValidationCode.UNKNOWN_SHEARING_ENTITY));
    }

    @Test
    void writesSuppressedAndUnsupportedSchemaAreErrors() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        config.schemaVersion = SmartDropsConfig.CURRENT_SCHEMA + 1;
        ConfigLoadDiagnostics.Builder diagnostics = new ConfigLoadDiagnostics.Builder();
        diagnostics.unsupportedSchema();

        ConfigValidationReport report = ConfigValidator.validate(
                new ConfigManager.ValidationSnapshot(config, 8L, true, diagnostics.build()),
                REGISTRIES);

        assertCode(report, ValidationCode.CONFIG_WRITES_SUPPRESSED, ValidationSeverity.ERROR);
        assertCode(report, ValidationCode.UNSUPPORTED_SCHEMA, ValidationSeverity.ERROR);
    }

    @Test
    void validationDoesNotMutateCollectionsRevisionOrPlayerResourceLocations() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        String playerId = "00000000-0000-0000-0000-000000000123";
        config.allowPlayerOverrides = true;
        config.playerMultipliers.put(playerId, 3);
        config.blockMultipliers.put("missing:block", 4);
        Map<String, Integer> blocksBefore = new LinkedHashMap<>(config.blockMultipliers);
        Set<String> blacklistBefore = new LinkedHashSet<>(config.blacklist);

        ConfigValidationReport report = ConfigValidator.validate(
                new ConfigManager.ValidationSnapshot(config, 91L, false, ConfigLoadDiagnostics.empty()),
                REGISTRIES);

        assertEquals(91L, report.revision());
        assertEquals(blocksBefore, config.blockMultipliers);
        assertEquals(blacklistBefore, config.blacklist);
        assertTrue(config.playerMultipliers.containsKey(playerId));
        assertFalse(report.issues().stream().anyMatch(issue ->
                issue.message().contains(playerId)
                        || playerId.equals(issue.identifier())
                        || playerId.equals(issue.settingPath())));
    }

    @Test
    void loadSanitizationDiagnosticsAreBoundedAndNeverSamplePlayerUuids() {
        SmartDropsConfig config = SmartDropsConfig.defaults();
        String playerId = "not-a-valid-player-uuid";
        config.globalMultiplier = 999;
        config.blockMultipliers.put("not an id", 999);
        config.categoryMultipliers.put("not_a_category", 2);
        config.playerMultipliers.put(playerId, 3);
        ConfigLoadDiagnostics.Builder diagnostics = new ConfigLoadDiagnostics.Builder();

        config.sanitize(diagnostics);
        ConfigLoadDiagnostics result = diagnostics.build();

        assertTrue(result.invalidResourceLocationsRemoved() >= 1);
        assertTrue(result.invalidCategoryNamesRemoved() >= 1);
        assertEquals(1, result.invalidPlayerOverridesRemoved());
        assertTrue(result.valuesAdjusted() >= 1);
        assertTrue(result.samples().size() <= 16);
        assertFalse(result.samples().stream().anyMatch(sample -> sample.value().contains(playerId)));
    }

    private static ConfigValidationReport validate(final SmartDropsConfig config) {
        return ConfigValidator.validate(
                new ConfigManager.ValidationSnapshot(config, 1L, false, ConfigLoadDiagnostics.empty()),
                REGISTRIES);
    }

    private static void assertCode(
            final ConfigValidationReport report,
            final ValidationCode code,
            final ValidationSeverity severity
    ) {
        assertTrue(report.issues().stream().anyMatch(issue ->
                        issue.code() == code && issue.severity() == severity),
                () -> "Missing " + severity + " " + code + " in " + report.issues());
    }

    private static final class FakeRegistryView implements ConfigRegistryView {
        private final Set<String> standardShearing;
        private final Set<String> specialShearing;
        private final boolean standardTagBound;
        private final boolean specialTagBound;

        private FakeRegistryView() {
            this(
                    Set.of("minecraft:sheep"),
                    Set.of(
                            "minecraft:mooshroom",
                            "minecraft:snow_golem",
                            "minecraft:bogged",
                            "minecraft:copper_golem",
                            "minecraft:sulfur_cube"),
                    true,
                    true);
        }

        private FakeRegistryView(
                final Set<String> standardShearing,
                final Set<String> specialShearing,
                final boolean standardTagBound,
                final boolean specialTagBound
        ) {
            this.standardShearing = Set.copyOf(standardShearing);
            this.specialShearing = Set.copyOf(specialShearing);
            this.standardTagBound = standardTagBound;
            this.specialTagBound = specialTagBound;
        }

        @Override
        public boolean blockExists(final String identifier) {
            return identifier != null && !identifier.startsWith("missing:");
        }

        @Override
        public boolean entityExists(final String identifier) {
            return identifier != null && !identifier.startsWith("missing:");
        }

        @Override
        public boolean dimensionExists(final String identifier) {
            return Set.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end")
                    .contains(identifier);
        }

        @Override
        public boolean blockTagBound(final String identifier) {
            return identifier != null && !identifier.startsWith("missing:");
        }

        @Override
        public boolean entityTagBound(final String identifier) {
            if ("smart_resource_drops:shearing/standard_resources".equals(identifier)) {
                return standardTagBound;
            }
            if ("smart_resource_drops:shearing/special".equals(identifier)) {
                return specialTagBound;
            }
            return identifier != null && !identifier.startsWith("missing:");
        }

        @Override
        public Set<String> entityIdsInTag(final String identifier) {
            if ("smart_resource_drops:shearing/standard_resources".equals(identifier)) {
                return standardShearing;
            }
            if ("smart_resource_drops:shearing/special".equals(identifier)) {
                return specialShearing;
            }
            return Set.of();
        }

        @Override
        public BlockEntityCapability blockEntityCapability(final String identifier) {
            if (!blockExists(identifier)) {
                return BlockEntityCapability.UNKNOWN;
            }
            return "minecraft:stone".equals(identifier)
                    ? BlockEntityCapability.NO
                    : BlockEntityCapability.YES;
        }
    }
}
