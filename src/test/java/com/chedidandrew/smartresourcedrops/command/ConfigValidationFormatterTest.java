package com.chedidandrew.smartresourcedrops.command;

import com.chedidandrew.smartresourcedrops.config.ConfigLoadDiagnostics;
import com.chedidandrew.smartresourcedrops.config.ConfigValidationReport;
import com.chedidandrew.smartresourcedrops.config.SmartDropsConfig;
import com.chedidandrew.smartresourcedrops.config.ValidationCode;
import com.chedidandrew.smartresourcedrops.config.ValidationIssue;
import com.chedidandrew.smartresourcedrops.config.ValidationSeverity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ConfigValidationFormatterTest {
    @Test
    void compactAndVerboseOutputEnforceIndependentHardCaps() {
        List<ValidationIssue> issues = new ArrayList<>();
        for (int index = 0; index < 120; index++) {
            issues.add(ValidationIssue.at(
                    ValidationSeverity.WARNING,
                    ValidationCode.UNKNOWN_BLOCK_ID,
                    "Missing configured block.",
                    "missing:block_" + index,
                    "blockMultipliers"));
        }
        ConfigValidationReport report = report(issues, 0, 120, 0, 0);

        List<Component> compact = ConfigValidationFormatter.format(report, false);
        List<Component> verbose = ConfigValidationFormatter.format(report, true);

        assertEquals(ConfigValidationFormatter.COMPACT_ISSUE_LIMIT, issueLineCount(compact));
        assertEquals(ConfigValidationFormatter.VERBOSE_ISSUE_LIMIT, issueLineCount(verbose));
        assertTrue(rendered(compact).contains("105 additional issue(s) omitted"));
        assertTrue(rendered(verbose).contains("20 additional issue(s) omitted"));
        assertTrue(rendered(compact).contains("No configuration or world data was changed."));
    }

    @Test
    void severityLabelsUseDistinctRestrainedColors() {
        List<ValidationIssue> issues = List.of(
                ValidationIssue.of(ValidationSeverity.ERROR, ValidationCode.CONFIG_WRITES_SUPPRESSED, "Error"),
                ValidationIssue.of(ValidationSeverity.WARNING, ValidationCode.EMPTY_BLOCK_WHITELIST, "Warning"),
                ValidationIssue.of(ValidationSeverity.INFO, ValidationCode.ENTITY_DROPS_ENABLED, "Info"));
        List<Component> lines = ConfigValidationFormatter.format(report(issues, 1, 1, 1, 0), true);

        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.RED), severityColor(lines, "[ERROR]"));
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.GOLD), severityColor(lines, "[WARNING]"));
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.YELLOW), severityColor(lines, "[INFO]"));
    }

    @Test
    void playerOverrideSummaryNeverEnumeratesUuidValues() {
        String uuid = "00000000-0000-0000-0000-000000000321";
        ValidationIssue notice = ValidationIssue.of(
                ValidationSeverity.INFO,
                ValidationCode.PLAYER_OVERRIDES_ENABLED,
                "Player multiplier overrides are enabled with 1 stored rule. UUID values are intentionally omitted.");
        ConfigValidationReport report = report(List.of(notice), 0, 0, 1, 1);

        String output = rendered(ConfigValidationFormatter.format(report, true));

        assertTrue(output.contains("Stored player overrides: 1"));
        assertFalse(output.contains(uuid));
    }

    @Test
    void compactOutputPrioritizesErrorsEvenWhenTheyArriveAfterWarnings() {
        List<ValidationIssue> issues = new ArrayList<>();
        for (int index = 0; index < 20; index++) {
            issues.add(ValidationIssue.at(
                    ValidationSeverity.WARNING,
                    ValidationCode.UNKNOWN_BLOCK_ID,
                    "Missing configured block.",
                    "missing:block_" + index,
                    "blockMultipliers"));
        }
        issues.add(ValidationIssue.of(
                ValidationSeverity.ERROR,
                ValidationCode.CONFIG_WRITE_FAILED,
                "The latest configuration write failed."));

        String output = rendered(ConfigValidationFormatter.format(report(issues, 1, 20, 0, 0), false));

        assertTrue(output.contains("CONFIG_WRITE_FAILED"));
        assertTrue(output.indexOf("CONFIG_WRITE_FAILED") < output.indexOf("UNKNOWN_BLOCK_ID"));
    }

    private static ConfigValidationReport report(
            final List<ValidationIssue> issues,
            final int errors,
            final int warnings,
            final int infos,
            final int playerOverrides
    ) {
        return new ConfigValidationReport(
                18L,
                SmartDropsConfig.CURRENT_SCHEMA,
                124,
                SmartDropsConfig.MAX_BLOCK_RULE_ENTRIES,
                16,
                SmartDropsConfig.MAX_ENTITY_RULE_ENTRIES,
                3,
                SmartDropsConfig.MAX_SHEARING_RULE_ENTRIES,
                0,
                playerOverrides,
                errors,
                warnings,
                infos,
                issues,
                0,
                ConfigLoadDiagnostics.empty());
    }

    private static long issueLineCount(final List<Component> lines) {
        return lines.stream().filter(line -> line.getString().startsWith("- [")).count();
    }

    private static TextColor severityColor(final List<Component> lines, final String label) {
        Component line = lines.stream().filter(component -> component.getString().contains(label)).findFirst().orElseThrow();
        return line.getSiblings().get(0).getStyle().getColor();
    }

    private static String rendered(final List<Component> lines) {
        return String.join("\n", lines.stream().map(Component::getString).toList());
    }
}
