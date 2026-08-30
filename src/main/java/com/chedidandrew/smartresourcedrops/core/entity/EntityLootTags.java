package com.chedidandrew.smartresourcedrops.core.entity;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Datapack-extensible item protections for normal entity death-table output. */
public final class EntityLootTags {
    public static final TagKey<Item> PROTECTED_OUTPUTS = TagKey.create(
            Registries.ITEM,
            SmartResourceDrops.id("protected_entity_loot"));

    private EntityLootTags() {
    }
}
