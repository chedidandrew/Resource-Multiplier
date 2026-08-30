package com.chedidandrew.smartresourcedrops.config;

import java.util.Objects;

/** One bounded immutable finding; it never retains a registry object or world reference. */
public record ValidationIssue(
        ValidationSeverity severity,
        ValidationCode code,
        String message,
        String identifier,
        String settingPath
) {
    private static final int MAX_MESSAGE_LENGTH = 384;
    private static final int MAX_IDENTIFIER_LENGTH = 256;
    private static final int MAX_SETTING_PATH_LENGTH = 128;

    public ValidationIssue {
        severity = Objects.requireNonNull(severity, "severity");
        code = Objects.requireNonNull(code, "code");
        message = bound(Objects.requireNonNull(message, "message"), MAX_MESSAGE_LENGTH);
        identifier = nullableBound(identifier, MAX_IDENTIFIER_LENGTH);
        settingPath = nullableBound(settingPath, MAX_SETTING_PATH_LENGTH);
    }

    public static ValidationIssue of(
            final ValidationSeverity severity,
            final ValidationCode code,
            final String message
    ) {
        return new ValidationIssue(severity, code, message, null, null);
    }

    public static ValidationIssue at(
            final ValidationSeverity severity,
            final ValidationCode code,
            final String message,
            final String identifier,
            final String settingPath
    ) {
        return new ValidationIssue(severity, code, message, identifier, settingPath);
    }

    private static String nullableBound(final String value, final int maximumLength) {
        return value == null || value.isBlank() ? null : bound(value, maximumLength);
    }

    private static String bound(final String value, final int maximumLength) {
        if (value.length() <= maximumLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maximumLength - 1)) + "\u2026";
    }
}
