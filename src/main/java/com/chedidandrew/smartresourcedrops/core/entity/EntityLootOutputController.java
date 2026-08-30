package com.chedidandrew.smartresourcedrops.core.entity;

import com.chedidandrew.smartresourcedrops.core.util.LootOutputBudget;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Owns one death session's amplified standard-loot invocation.
 * The first real output claims the session, so an empty preliminary table cannot consume the claim.
 */
final class EntityLootOutputController {
    static final long MAX_MULTIPLIED_ITEMS = LootOutputBudget.MAX_MULTIPLIED_ITEMS;
    static final long MAX_MULTIPLIED_STACKS = LootOutputBudget.MAX_MULTIPLIED_STACKS;

    private Object ownerToken;
    private long multipliedItems;
    private long multipliedStacks;
    private boolean budgetExceeded;

    Consumer<ItemStack> wrap(
            Consumer<ItemStack> downstream,
            int multiplier,
            BooleanSupplier sessionIsActive,
            Runnable onBudgetExceeded
    ) {
        Objects.requireNonNull(downstream, "downstream");
        Objects.requireNonNull(sessionIsActive, "sessionIsActive");
        Objects.requireNonNull(onBudgetExceeded, "onBudgetExceeded");
        if (multiplier == 1) {
            return downstream;
        }

        Object token = new Object();
        int safeMultiplier = Math.max(0, multiplier);
        return stack -> {
            if (!sessionIsActive.getAsBoolean()) {
                downstream.accept(stack);
                return;
            }
            if (stack == null || stack.isEmpty() || stack.getCount() <= 0) {
                downstream.accept(stack);
                return;
            }
            if (ownerToken == null) {
                ownerToken = token;
            }
            if (ownerToken != token || budgetExceeded || EntityLootMultiplier.isProtected(stack)) {
                downstream.accept(stack);
                return;
            }

            EntityLootMultiplier.OutputPlan plan = EntityLootMultiplier.plan(stack, safeMultiplier);
            if (!fits(plan)) {
                budgetExceeded = true;
                onBudgetExceeded.run();
                downstream.accept(stack);
                return;
            }

            multipliedItems += plan.itemCount();
            multipliedStacks += plan.stackCount();
            EntityLootMultiplier.emit(downstream, stack, safeMultiplier);
        };
    }

    boolean claimed() {
        return ownerToken != null;
    }

    boolean budgetExceeded() {
        return budgetExceeded;
    }

    long multipliedItems() {
        return multipliedItems;
    }

    long multipliedStacks() {
        return multipliedStacks;
    }

    private boolean fits(EntityLootMultiplier.OutputPlan plan) {
        return plan.itemCount() <= MAX_MULTIPLIED_ITEMS - multipliedItems
                && plan.stackCount() <= MAX_MULTIPLIED_STACKS - multipliedStacks;
    }
}
