package com.chedidandrew.smartresourcedrops.core.shearing;

import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
        ItemStack first = stack(Items.SNOWBALL, 3);
        CompoundTag custom = new CompoundTag();
        custom.putString("marker", "preserved");
        first.setTag(custom);
        first.setHoverName(Component.literal("preserved"));
        ItemStack second = stack(Items.SNOWBALL, 2);
        second.setTag(first.getTag().copy());

        ShearingOutputBudget.Result result = ShearingOutputBudget.plan(
                List.of(List.of(first, second), List.of(first)),
                10);

        assertTrue(result.fits());
        assertEquals(80, result.multipliedItems());
        assertEquals(List.of(16, 16, 16, 2),
                result.outputBatches().get(0).stream().map(ItemStack::getCount).toList());
        assertEquals(List.of(16, 14),
                result.outputBatches().get(1).stream().map(ItemStack::getCount).toList());
        for (List<ItemStack> batch : result.outputBatches()) {
            for (ItemStack output : batch) {
                assertNotSame(first, output);
                assertTrue(ItemStack.isSameItemSameTags(first, output));
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
        ItemStack source = stack(Items.DIAMOND_SWORD, 5);

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
        assertTrue(result.outputBatches().get(0).isEmpty());
    }

    private static ItemStack stack(Item item, int count) {
        return new ItemStack(item, count);
    }
}
