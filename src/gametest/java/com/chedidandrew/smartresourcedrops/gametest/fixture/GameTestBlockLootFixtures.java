package com.chedidandrew.smartresourcedrops.gametest.fixture;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Loader-neutral state used to exercise whole-event block-loot budget fallback. */
public final class GameTestBlockLootFixtures {
    public static final int PATHOLOGICAL_STACKS = 65;
    public static final int ITEMS_PER_STACK = 64;
    public static final ResourceKey<LootTable> DIRT_LOOT = ResourceKey.create(
            Registries.LOOT_TABLE,
            Identifier.fromNamespaceAndPath("minecraft", "blocks/dirt"));

    private static final AtomicBoolean ARMED = new AtomicBoolean();

    private GameTestBlockLootFixtures() {
    }

    public static void appendPathologicalDropsIfArmed(
            final boolean dirtLoot,
            final List<ItemStack> drops
    ) {
        if (!dirtLoot || !ARMED.compareAndSet(true, false)) {
            return;
        }
        for (int index = 0; index < PATHOLOGICAL_STACKS; index++) {
            drops.add(new ItemStack(Items.DIAMOND, ITEMS_PER_STACK));
        }
    }

    public static void arm() {
        if (!ARMED.compareAndSet(false, true)) {
            throw new IllegalStateException("The pathological block-loot fixture is already armed");
        }
    }

    public static void reset() {
        ARMED.set(false);
    }
}
