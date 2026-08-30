package com.chedidandrew.smartresourcedrops.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Bounded, immutable summary of the most recent configuration load.
 *
 * <p>The summary deliberately retains neither the original JSON nor absolute paths. Samples are
 * limited to non-sensitive setting values; player UUIDs are counted but never retained.</p>
 */
public record ConfigLoadDiagnostics(
        int invalidIdentifiersRemoved,
        int invalidCategoryNamesRemoved,
        int invalidPlayerOverridesRemoved,
        int valuesAdjusted,
        int blockEntriesOverBudget,
        int entityEntriesOverBudget,
        int shearingEntriesOverBudget,
        int migratedFromSchema,
        boolean malformedFileRecovered,
        boolean oversizedFileDetected,
        boolean unsupportedSchemaRejected,
        boolean readFailed,
        boolean writeFailed,
        List<String> backupFileNames,
        List<Sample> samples
) {
    public static final int NO_MIGRATION = Integer.MIN_VALUE;
    private static final int MAX_BACKUP_NAMES = 3;
    private static final int MAX_SAMPLES = 16;
    private static final int MAX_FILENAME_LENGTH = 160;
    private static final int MAX_PATH_LENGTH = 128;
    private static final int MAX_VALUE_LENGTH = 256;

    public ConfigLoadDiagnostics {
        invalidIdentifiersRemoved = Math.max(0, invalidIdentifiersRemoved);
        invalidCategoryNamesRemoved = Math.max(0, invalidCategoryNamesRemoved);
        invalidPlayerOverridesRemoved = Math.max(0, invalidPlayerOverridesRemoved);
        valuesAdjusted = Math.max(0, valuesAdjusted);
        blockEntriesOverBudget = Math.max(0, blockEntriesOverBudget);
        entityEntriesOverBudget = Math.max(0, entityEntriesOverBudget);
        shearingEntriesOverBudget = Math.max(0, shearingEntriesOverBudget);
        backupFileNames = boundedBackupNames(backupFileNames);
        samples = samples == null
                ? List.of()
                : List.copyOf(samples.subList(0, Math.min(MAX_SAMPLES, samples.size())));
    }

    public static ConfigLoadDiagnostics empty() {
        return new Builder().build();
    }

    static ConfigLoadDiagnostics readFailure() {
        Builder builder = new Builder();
        builder.readFailed = true;
        return builder.build();
    }

    ConfigLoadDiagnostics withBackupFile(final String fileName) {
        Builder builder = new Builder(this);
        if (fileName != null && !fileName.isBlank() && builder.backupFileNames.size() < MAX_BACKUP_NAMES) {
            builder.backupFileNames.add(bound(fileName, MAX_FILENAME_LENGTH));
        }
        return builder.build();
    }

    ConfigLoadDiagnostics withMalformedRecovery() {
        Builder builder = new Builder(this);
        builder.malformedFileRecovered = true;
        return builder.build();
    }

    ConfigLoadDiagnostics withWriteFailure() {
        Builder builder = new Builder(this);
        builder.writeFailed = true;
        return builder.build();
    }

    private static List<String> boundedBackupNames(final List<String> names) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }
        ArrayList<String> bounded = new ArrayList<>(Math.min(MAX_BACKUP_NAMES, names.size()));
        for (String name : names) {
            if (name != null && !name.isBlank() && bounded.size() < MAX_BACKUP_NAMES) {
                bounded.add(bound(name, MAX_FILENAME_LENGTH));
            }
        }
        return List.copyOf(bounded);
    }

    private static String bound(final String value, final int maximumLength) {
        if (value == null || value.length() <= maximumLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maximumLength - 1)) + "\u2026";
    }

    /** One bounded non-sensitive example of a value changed during load. */
    public record Sample(String settingPath, String value, String action) {
        public Sample {
            settingPath = bound(settingPath == null ? "" : settingPath, MAX_PATH_LENGTH);
            value = bound(value == null ? "" : value, MAX_VALUE_LENGTH);
            action = bound(action == null ? "" : action, 96);
        }
    }

    static final class Builder {
        private int invalidIdentifiersRemoved;
        private int invalidCategoryNamesRemoved;
        private int invalidPlayerOverridesRemoved;
        private int valuesAdjusted;
        private int blockEntriesOverBudget;
        private int entityEntriesOverBudget;
        private int shearingEntriesOverBudget;
        private int migratedFromSchema = NO_MIGRATION;
        private boolean malformedFileRecovered;
        private boolean oversizedFileDetected;
        private boolean unsupportedSchemaRejected;
        private boolean readFailed;
        private boolean writeFailed;
        private final List<String> backupFileNames = new ArrayList<>();
        private final List<Sample> samples = new ArrayList<>();

        Builder() {
        }

        private Builder(final ConfigLoadDiagnostics diagnostics) {
            invalidIdentifiersRemoved = diagnostics.invalidIdentifiersRemoved;
            invalidCategoryNamesRemoved = diagnostics.invalidCategoryNamesRemoved;
            invalidPlayerOverridesRemoved = diagnostics.invalidPlayerOverridesRemoved;
            valuesAdjusted = diagnostics.valuesAdjusted;
            blockEntriesOverBudget = diagnostics.blockEntriesOverBudget;
            entityEntriesOverBudget = diagnostics.entityEntriesOverBudget;
            shearingEntriesOverBudget = diagnostics.shearingEntriesOverBudget;
            migratedFromSchema = diagnostics.migratedFromSchema;
            malformedFileRecovered = diagnostics.malformedFileRecovered;
            oversizedFileDetected = diagnostics.oversizedFileDetected;
            unsupportedSchemaRejected = diagnostics.unsupportedSchemaRejected;
            readFailed = diagnostics.readFailed;
            writeFailed = diagnostics.writeFailed;
            backupFileNames.addAll(diagnostics.backupFileNames);
            samples.addAll(diagnostics.samples);
        }

        void invalidIdentifier(final String settingPath, final String value) {
            invalidIdentifiersRemoved++;
            sample(settingPath, value, "removed invalid identifier");
        }

        void invalidCategory(final String settingPath, final String value) {
            invalidCategoryNamesRemoved++;
            sample(settingPath, value, "removed invalid category");
        }

        void invalidPlayerOverride() {
            invalidPlayerOverridesRemoved++;
        }

        void valueAdjusted(final String settingPath, final Object before, final Object after) {
            valuesAdjusted++;
            sample(settingPath, String.valueOf(before) + " -> " + after, "adjusted value");
        }

        void ruleBudgets(
                final int rawBlockEntries,
                final int rawEntityEntries,
                final int rawShearingEntries
        ) {
            blockEntriesOverBudget = Math.max(0, rawBlockEntries - SmartDropsConfig.MAX_BLOCK_RULE_ENTRIES);
            entityEntriesOverBudget = Math.max(0, rawEntityEntries - SmartDropsConfig.MAX_ENTITY_RULE_ENTRIES);
            shearingEntriesOverBudget = Math.max(
                    0,
                    rawShearingEntries - SmartDropsConfig.MAX_SHEARING_RULE_ENTRIES);
            oversizedFileDetected = blockEntriesOverBudget > 0
                    || entityEntriesOverBudget > 0
                    || shearingEntriesOverBudget > 0;
        }

        /** Compatibility seam for existing validation tests that only exercise the older domains. */
        void ruleBudgets(final int rawBlockEntries, final int rawEntityEntries) {
            ruleBudgets(rawBlockEntries, rawEntityEntries, 0);
        }

        void migratedFrom(final int schema) {
            migratedFromSchema = schema;
        }

        void unsupportedSchema() {
            unsupportedSchemaRejected = true;
        }

        private void sample(final String settingPath, final String value, final String action) {
            if (samples.size() < MAX_SAMPLES) {
                samples.add(new Sample(settingPath, value, action));
            }
        }

        ConfigLoadDiagnostics build() {
            return new ConfigLoadDiagnostics(
                    invalidIdentifiersRemoved,
                    invalidCategoryNamesRemoved,
                    invalidPlayerOverridesRemoved,
                    valuesAdjusted,
                    blockEntriesOverBudget,
                    entityEntriesOverBudget,
                    shearingEntriesOverBudget,
                    migratedFromSchema,
                    malformedFileRecovered,
                    oversizedFileDetected,
                    unsupportedSchemaRejected,
                    readFailed,
                    writeFailed,
                    backupFileNames,
                    samples);
        }
    }
}
