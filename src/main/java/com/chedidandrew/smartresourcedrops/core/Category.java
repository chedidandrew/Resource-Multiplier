package com.chedidandrew.smartresourcedrops.core;

import java.util.Locale;
import java.util.Optional;

public enum Category {
    ORES("ores"),
    RAW_RESOURCE_BLOCKS("raw_resource_blocks"),
    LOGS("logs"),
    STONE("stone"),
    SOIL("soil"),
    NETHER("nether"),
    END("end"),
    CROPS("crops"),
    PLANTS("plants"),
    LEAVES("leaves"),
    BUILDING_BLOCKS("building_blocks"),
    MISCELLANEOUS("miscellaneous");

    private final String key;

    Category(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static Optional<Category> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        for (Category category : values()) {
            if (category.key.equals(normalized) || category.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return Optional.of(category);
            }
        }
        return Optional.empty();
    }
}
