package com.chedidandrew.smartresourcedrops.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

final class StackConsolidatorTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void preservesComponentsAndUsesLegalStackSizes() {
        final ItemStack source = stack(Items.DIAMOND, 3);
        final CompoundTag tag = new CompoundTag();
        tag.putString("marker", "preserve-me");
        source.set(DataComponents.CUSTOM_NAME, Component.literal("preserve-me"));
        source.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        source.set(DataComponents.MAX_STACK_SIZE, 16);

        final List<ItemStack> result = StackConsolidator.multiply(List.of(source), 10);

        assertEquals(List.of(16, 14), result.stream().map(ItemStack::getCount).toList());
        assertEquals(3, source.getCount());
        for (ItemStack output : result) {
            assertNotSame(source, output);
            assertTrue(output.getCount() <= output.getMaxStackSize());
            assertTrue(ItemStack.isSameItemSameComponents(source, output));
        }
        result.getFirst().set(DataComponents.CUSTOM_NAME, Component.literal("changed"));
        assertEquals(Component.literal("preserve-me"), source.get(DataComponents.CUSTOM_NAME));
    }

    @Test
    void mergesIdenticalPartialLootEntriesBeforeSplitting() {
        final ItemStack first = stack(Items.EMERALD, 40);
        final ItemStack second = stack(Items.EMERALD, 30);
        first.set(DataComponents.MAX_STACK_SIZE, 64);
        second.set(DataComponents.MAX_STACK_SIZE, 64);

        final List<ItemStack> result = StackConsolidator.multiply(List.of(first, second), 2);

        assertEquals(List.of(64, 64, 12), result.stream().map(ItemStack::getCount).toList());
        assertEquals(70, first.getCount() + second.getCount());
    }

    private static ItemStack stack(final net.minecraft.world.item.Item item, final int count) {
        return new ItemStack(Holder.direct(item, DataComponentMap.EMPTY), count);
    }
}
