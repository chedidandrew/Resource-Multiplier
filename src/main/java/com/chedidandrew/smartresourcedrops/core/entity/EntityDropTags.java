package com.chedidandrew.smartresourcedrops.core.entity;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Smart Resource Multiplier category tags plus runtime entity-type tag discovery. */
public final class EntityDropTags {
    private static final Map<EntityCategory, TagKey<EntityType<?>>> CATEGORY_TAGS =
            new EnumMap<>(EntityCategory.class);

    static {
        for (EntityCategory category : EntityCategory.values()) {
            CATEGORY_TAGS.put(category, TagKey.create(
                    Registries.ENTITY_TYPE,
                    SmartResourceDrops.id("categories/" + category.key())));
        }
    }

    private EntityDropTags() {
    }

    public static TagKey<EntityType<?>> categoryTag(EntityCategory category) {
        return CATEGORY_TAGS.get(category);
    }

    public static Set<String> runtimeTags(EntityType<?> type) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type).tags()
                .map(tag -> tag.location().toString())
                .sorted()
                .forEach(tags::add);
        return tags;
    }
}
