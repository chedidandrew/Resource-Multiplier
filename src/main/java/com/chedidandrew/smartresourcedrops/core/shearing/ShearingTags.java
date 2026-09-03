package com.chedidandrew.smartresourcedrops.core.shearing;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

import java.util.Set;

/** Data-pack controlled certification and permanent-safety tags for shearable entities. */
public final class ShearingTags {
    /**
     * Minecraft 1.21.1 has no generic final-output shearing boundary. Sheep is the only vanilla
     * type whose completed item emissions can be intercepted without touching transformation or
     * equipment semantics. Extra standard-resource tag members therefore fail closed on this
     * backport instead of displaying a multiplier that runtime cannot apply.
     */
    public static final Set<String> SUPPORTED_STANDARD_IDS = Set.of("minecraft:sheep");
    /** Vanilla one-time/conversion/equipment outputs remain fixed even if a data pack replaces the tag. */
    public static final Set<String> KNOWN_VANILLA_SPECIAL_IDS = Set.of(
            "minecraft:bogged",
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

    public static boolean isSupportedStandardTarget(final String entityId) {
        return SUPPORTED_STANDARD_IDS.contains(entityId);
    }
}
