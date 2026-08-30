package com.chedidandrew.smartresourcedrops.command;

import com.chedidandrew.smartresourcedrops.config.ConfigValidationReport;
import com.chedidandrew.smartresourcedrops.config.ValidationIssue;
import com.chedidandrew.smartresourcedrops.config.ValidationSeverity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Bounded server-safe Component rendering for configuration validation reports. */
final class ConfigValidationFormatter {
    static final int COMPACT_ISSUE_LIMIT = 15;
    static final int VERBOSE_ISSUE_LIMIT = 100;

    private ConfigValidationFormatter() {
    }

    static List<Component> format(final ConfigValidationReport report, final boolean verbose) {
        final int issueLimit = verbose ? VERBOSE_ISSUE_LIMIT : COMPACT_ISSUE_LIMIT;
        final List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("Resource Multiplier Validation")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        lines.add(valueLine("Configuration revision", String.valueOf(report.revision()), ChatFormatting.WHITE));
        lines.add(statusLine(report));
        lines.add(valueLine(
                "Block domain",
                report.blockRuleCount() + " / " + report.blockRuleLimit(),
                ChatFormatting.WHITE));
        lines.add(valueLine(
                "Entity domain",
                report.entityRuleCount() + " / " + report.entityRuleLimit(),
                ChatFormatting.WHITE));
        lines.add(valueLine(
                "Shearing domain",
                report.shearingRuleCount() + " / " + report.shearingRuleLimit(),
                ChatFormatting.WHITE));
        if (verbose) {
            lines.add(valueLine(
                    "Block-entity allowlist",
                    String.valueOf(report.blockEntityAllowlistCount()),
                    ChatFormatting.WHITE));
            lines.add(valueLine(
                    "Stored player overrides",
                    String.valueOf(report.playerOverrideCount()),
                    ChatFormatting.WHITE));
        }

        if (report.totalIssueCount() == 0) {
            lines.add(Component.literal("No validation issues found.").withStyle(ChatFormatting.GREEN));
        } else {
            lines.add(Component.literal("Issues:").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
            final List<ValidationIssue> prioritizedIssues = report.issues().stream()
                    .sorted(Comparator.comparingInt(issue -> issue.severity().ordinal()))
                    .toList();
            final int shown = Math.min(issueLimit, prioritizedIssues.size());
            for (int index = 0; index < shown; index++) {
                lines.add(issueLine(prioritizedIssues.get(index), verbose));
            }
            final int omitted = Math.max(0, report.totalIssueCount() - shown);
            if (omitted > 0) {
                lines.add(Component.literal(omitted + " additional issue(s) omitted; use "
                                + "/smartdrops validate verbose for a larger bounded report.")
                        .withStyle(ChatFormatting.GRAY));
            }
        }
        lines.add(Component.literal("No configuration or world data was changed.")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        return List.copyOf(lines);
    }

    private static Component statusLine(final ConfigValidationReport report) {
        final String value;
        final ChatFormatting color;
        if (report.hasErrors()) {
            value = "Invalid: " + report.errorCount() + " error(s), "
                    + report.warningCount() + " warning(s)";
            color = ChatFormatting.RED;
        } else if (report.hasWarnings()) {
            value = "Valid with " + report.warningCount() + " warning(s)";
            color = ChatFormatting.GOLD;
        } else if (report.infoCount() > 0) {
            value = "Valid with " + report.infoCount() + " informational notice(s)";
            color = ChatFormatting.YELLOW;
        } else {
            value = "Valid";
            color = ChatFormatting.GREEN;
        }
        return valueLine("Status", value, color);
    }

    private static Component valueLine(
            final String label,
            final String value,
            final ChatFormatting valueColor
    ) {
        return Component.literal(label + ": ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(value).withStyle(valueColor));
    }

    private static Component issueLine(final ValidationIssue issue, final boolean verbose) {
        final MutableComponent line = Component.literal("- ").withStyle(ChatFormatting.GRAY);
        line.append(Component.literal("[" + issue.severity().name() + "] ")
                .withStyle(color(issue.severity()), ChatFormatting.BOLD));
        line.append(Component.literal(issue.code().name() + ": ").withStyle(ChatFormatting.DARK_GRAY));
        if (issue.identifier() != null) {
            line.append(Component.literal("`" + issue.identifier() + "` ").withStyle(ChatFormatting.GRAY));
        }
        line.append(Component.literal(issue.message()).withStyle(ChatFormatting.WHITE));
        if (verbose && issue.settingPath() != null) {
            line.append(Component.literal(" [" + issue.settingPath() + "]").withStyle(ChatFormatting.DARK_GRAY));
        }
        return line;
    }

    private static ChatFormatting color(final ValidationSeverity severity) {
        return switch (severity) {
            case ERROR -> ChatFormatting.RED;
            case WARNING -> ChatFormatting.GOLD;
            case INFO -> ChatFormatting.YELLOW;
        };
    }
}
