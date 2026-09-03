package com.chedidandrew.smartresourcedrops.core.shearing;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

import java.util.Set;

/** Data-pack controlled certification and permanent-safety tags for shearable entities. */
public final class ShearingTags {
    /** Vanilla one-time/conversion/equipment outputs remain fixed even if a data pack replaces the tag. */
    public static final Set<String> KNOWN_VANILLA_SPECIAL_IDS = Set.of(
            "minecraft:mooshroom",
            "minecraft:snow_golem");
    public static final TagKey<EntityType<?>> STANDARD_RESOURCES = TagKey.create(
            Registries.ENTITY_TYPE,
            SmartResourceDrops.id("shearing/standard_resources"));
    public static final TagKey<EntityType<?>> SPECIAL = TagKey.create(
            Registries.ENTITY_TYPE,
            SmartResourceDrops.id("shearing/special"));

    private ShearingTags() {
    }

    public static boolean isKnownVanillaSpecial(String entityId) {
        return KNOWN_VANILLA_SPECIAL_IDS.contains(entityId);
    }
}
