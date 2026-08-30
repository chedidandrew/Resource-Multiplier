package com.chedidandrew.smartresourcedrops.core.entity;

import com.chedidandrew.smartresourcedrops.core.util.StackConsolidator;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.function.Consumer;

/** Multiplies already-final, already-legal loot stacks while preserving protected outputs. */
public final class EntityLootMultiplier {
    private EntityLootMultiplier() {
    }

    public static void emit(Consumer<ItemStack> downstream, ItemStack stack, int multiplier) {
        java.util.Objects.requireNonNull(downstream, "downstream");
        java.util.Objects.requireNonNull(stack, "stack");
        if (isProtected(stack)) {
            downstream.accept(stack);
            return;
        }
        for (ItemStack multiplied : StackConsolidator.multiply(List.of(stack), Math.max(0, multiplier))) {
            downstream.accept(multiplied);
        }
    }

    public static boolean isProtected(ItemStack stack) {
        return stack.is(Items.SADDLE)
                || stack.is(Items.TOTEM_OF_UNDYING)
                || stack.is(EntityLootTags.PROTECTED_OUTPUTS);
    }

    static OutputPlan plan(ItemStack stack, int multiplier) {
        if (multiplier <= 0 || stack.isEmpty() || stack.getCount() <= 0) {
            return new OutputPlan(0L, 0L);
        }
        long itemCount;
        try {
            itemCount = Math.multiplyExact((long) stack.getCount(), (long) multiplier);
        } catch (ArithmeticException exception) {
            return OutputPlan.UNBOUNDED;
        }
        long maxStackSize = Math.max(1, stack.getMaxStackSize());
        long stackCount = itemCount / maxStackSize + (itemCount % maxStackSize == 0L ? 0L : 1L);
        return new OutputPlan(itemCount, stackCount);
    }

    record OutputPlan(long itemCount, long stackCount) {
        private static final OutputPlan UNBOUNDED = new OutputPlan(Long.MAX_VALUE, Long.MAX_VALUE);
    }
}
