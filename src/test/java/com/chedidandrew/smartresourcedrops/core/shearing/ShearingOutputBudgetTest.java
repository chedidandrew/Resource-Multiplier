package com.chedidandrew.smartresourcedrops.core.shearing;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ShearingOutputBudgetTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void preservesComponentsAndConsumerBatchBoundaries() {
        ItemStack first = stack(Items.DIAMOND, 3);
        CompoundTag custom = new CompoundTag();
        custom.putString("marker", "preserved");
        first.set(DataComponents.CUSTOM_NAME, Component.literal("preserved"));
        first.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
        first.set(DataComponents.MAX_STACK_SIZE, 16);
        ItemStack second = stack(Items.DIAMOND, 2);
        second.applyComponents(first.getComponents());

        ShearingOutputBudget.Result result = ShearingOutputBudget.plan(
                List.of(List.of(first, second), List.of(first)),
                10);

        assertTrue(result.fits());
        assertEquals(80, result.multipliedItems());
        assertEquals(List.of(16, 16, 16, 2),
                result.outputBatches().getFirst().stream().map(ItemStack::getCount).toList());
        assertEquals(List.of(16, 14),
                result.outputBatches().get(1).stream().map(ItemStack::getCount).toList());
        for (List<ItemStack> batch : result.outputBatches()) {
            for (ItemStack output : batch) {
                assertNotSame(first, output);
                assertTrue(ItemStack.isSameItemSameComponents(first, output));
            }
        }
    }

    @Test
    void sourceStackLimitFallsBackBeforeMaterialization() {
        ItemStack source = stack(Items.DIAMOND, 1);
        List<ItemStack> entries = new ArrayList<>();
        for (int i = 0; i <= ShearingOutputBudget.MAX_SOURCE_OR_MATERIALIZED_STACKS; i++) {
            entries.add(source);
        }

        ShearingOutputBudget.Result result = ShearingOutputBudget.plan(List.of(entries), 2);

        assertFalse(result.fits());
        assertEquals(ShearingOutputBudget.LimitExceeded.SOURCE_STACKS, result.limitExceeded());
        assertEquals(257, result.sourceStacks());
    }

    @Test
    void cumulativeItemLimitFallsBackWithSaturatingArithmetic() {
        ItemStack first = stack(Items.DIAMOND, 9);
        ItemStack second = stack(Items.EMERALD, 8);

        ShearingOutputBudget.Result result = ShearingOutputBudget.plan(
                List.of(List.of(first), List.of(second)),
                64);

        assertFalse(result.fits());
        assertEquals(ShearingOutputBudget.LimitExceeded.ITEMS, result.limitExceeded());
        assertEquals(1_088, result.multipliedItems());
    }

    @Test
    void materializedStackLimitFallsBackForUnstackableOutput() {
        ItemStack source = stack(Items.DIAMOND, 5);
        source.set(DataComponents.MAX_STACK_SIZE, 1);

        ShearingOutputBudget.Result result = ShearingOutputBudget.plan(
                List.of(List.of(source)),
                64);

        assertFalse(result.fits());
        assertEquals(ShearingOutputBudget.LimitExceeded.MATERIALIZED_STACKS, result.limitExceeded());
        assertEquals(320, result.materializedStacks());
        assertTrue(result.outputBatches().isEmpty());
    }

    @Test
    void zeroMultiplierProducesNoOutputWithinBudget() {
        ShearingOutputBudget.Result result = ShearingOutputBudget.plan(
                List.of(List.of(stack(Items.DIAMOND, 3))),
                0);

        assertTrue(result.fits());
        assertEquals(0, result.multipliedItems());
        assertEquals(0, result.materializedStacks());
        assertTrue(result.outputBatches().getFirst().isEmpty());
    }

    private static ItemStack stack(Item item, int count) {
        ItemStack stack = new ItemStack(item, count);
        stack.set(DataComponents.MAX_STACK_SIZE, 64);
        return stack;
    }
}
