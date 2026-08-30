package com.chedidandrew.smartresourcedrops.core.shearing;

import org.jspecify.annotations.Nullable;

/** Read-only explanation of one server-side shearing multiplier decision. */
public record ShearingRuleTrace(
        String entityId,
        ShearingClassification classification,
        boolean standardTagged,
        boolean specialTagged,
        boolean knownVanillaSpecial,
        ShearingSource source,
        boolean modEnabled,
        boolean sourceEnabled,
        boolean inheritsGlobalDefault,
        int configuredDefaultMultiplier,
        @Nullable Integer exactOverride,
        int appliedMultiplier,
        RuleSource selectedRule,
        String reason
) {
    public boolean tagConflict() {
        return standardTagged && specialTagged;
    }

    public boolean safetyConflict() {
        return standardTagged && classification == ShearingClassification.SPECIAL;
    }

    public boolean overrideReachable() {
        return classification == ShearingClassification.STANDARD_RESOURCE;
    }

    public boolean multiplicationEligible() {
        return classification == ShearingClassification.STANDARD_RESOURCE
                && modEnabled
                && sourceEnabled;
    }

    public boolean fixedVanilla() {
        return classification != ShearingClassification.STANDARD_RESOURCE;
    }

    public enum RuleSource {
        SPECIAL_SAFETY,
        UNKNOWN_SAFETY,
        MOD_DISABLED,
        SOURCE_DISABLED,
        ENTITY_OVERRIDE,
        GLOBAL_DEFAULT,
        SHEARING_DEFAULT
    }
}
