package com.chedidandrew.smartresourcedrops.core.entity;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/** Ordered entity categories. The declaration order is the rule-selection priority. */
public enum EntityCategory {
    BOSSES("bosses"),
    VILLAGERS_NPCS("villagers_npcs"),
    GOLEMS("golems"),
    NEUTRAL("neutral"),
    PASSIVE("passive"),
    HOSTILE("hostile"),
    AQUATIC("aquatic"),
    AMBIENT("ambient"),
    MISCELLANEOUS("miscellaneous");

    private final String key;

    EntityCategory(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static Optional<EntityCategory> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(category -> category.key.equals(normalized)).findFirst();
    }
}
