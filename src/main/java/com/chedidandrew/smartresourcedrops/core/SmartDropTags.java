package com.chedidandrew.smartresourcedrops.core;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class SmartDropTags {
    private static final Map<Category, TagKey<Block>> CATEGORY_TAGS = new EnumMap<>(Category.class);

    static {
        for (Category category : Category.values()) {
            if (category != Category.MISCELLANEOUS) {
                CATEGORY_TAGS.put(category, TagKey.create(
                        Registries.BLOCK,
                        SmartResourceDrops.id("categories/" + category.key())));
            }
        }
    }

    private SmartDropTags() {
    }

    public static LinkedHashSet<Category> categoriesFor(BlockState state) {
        LinkedHashSet<Category> categories = new LinkedHashSet<>();
        for (Category category : Category.values()) {
            TagKey<Block> tag = CATEGORY_TAGS.get(category);
            if (tag != null && state.is(tag)) {
                categories.add(category);
            }
        }
        if (categories.isEmpty()) {
            categories.add(Category.MISCELLANEOUS);
        }
        return categories;
    }

    public static Set<String> matchingConfiguredTags(BlockState state, Set<String> configuredTags) {
        LinkedHashSet<String> matches = new LinkedHashSet<>();
        state.getBlock().builtInRegistryHolder().tags().forEach(tag -> {
            String id = tag.location().toString();
            if (configuredTags.contains(id) || configuredTags.contains("#" + id)) {
                matches.add(id);
            }
        });
        return matches;
    }

    public static Set<String> allMatchingFilterTags(BlockState state, Set<String> first, Set<String> second) {
        LinkedHashSet<String> matches = new LinkedHashSet<>();
        state.getBlock().builtInRegistryHolder().tags().forEach(tag -> {
            String id = tag.location().toString();
            if (first.contains(id) || second.contains(id)) {
                matches.add(id);
            }
        });
        return matches;
    }

    public static String normalizeTagId(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("#") ? normalized.substring(1) : normalized;
    }
}
