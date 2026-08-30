package com.chedidandrew.smartresourcedrops.config;

import java.util.List;

/** Complete immutable result of one server-authoritative, read-only validation pass. */
public record ConfigValidationReport(
        long revision,
        int schemaVersion,
        int blockRuleCount,
        int blockRuleLimit,
        int entityRuleCount,
        int entityRuleLimit,
        int shearingRuleCount,
        int shearingRuleLimit,
        int blockEntityAllowlistCount,
        int playerOverrideCount,
        int errorCount,
        int warningCount,
        int infoCount,
        List<ValidationIssue> issues,
        int omittedIssueCount,
        ConfigLoadDiagnostics loadDiagnostics
) {
    public ConfigValidationReport {
        blockRuleCount = Math.max(0, blockRuleCount);
        blockRuleLimit = Math.max(0, blockRuleLimit);
        entityRuleCount = Math.max(0, entityRuleCount);
        entityRuleLimit = Math.max(0, entityRuleLimit);
        shearingRuleCount = Math.max(0, shearingRuleCount);
        shearingRuleLimit = Math.max(0, shearingRuleLimit);
        blockEntityAllowlistCount = Math.max(0, blockEntityAllowlistCount);
        playerOverrideCount = Math.max(0, playerOverrideCount);
        errorCount = Math.max(0, errorCount);
        warningCount = Math.max(0, warningCount);
        infoCount = Math.max(0, infoCount);
        issues = issues == null ? List.of() : List.copyOf(issues);
        omittedIssueCount = Math.max(0, omittedIssueCount);
        loadDiagnostics = loadDiagnostics == null ? ConfigLoadDiagnostics.empty() : loadDiagnostics;
    }

    public boolean hasErrors() {
        return errorCount > 0;
    }

    public boolean hasWarnings() {
        return warningCount > 0;
    }

    public int totalIssueCount() {
        return errorCount + warningCount + infoCount;
    }
}
