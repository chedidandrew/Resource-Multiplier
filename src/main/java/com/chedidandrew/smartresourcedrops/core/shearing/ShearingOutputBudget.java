package com.chedidandrew.smartresourcedrops.core.shearing;

import com.chedidandrew.smartresourcedrops.core.util.LootOutputBudget;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Plans a complete shearing action before any multiplied stack is emitted.
 * Batches stay separate so each vanilla or mod-provided placement consumer is preserved.
 */
public final class ShearingOutputBudget {
    public static final long MAX_MULTIPLIED_ITEMS = 1_024L;
    public static final long MAX_SOURCE_OR_MATERIALIZED_STACKS = 256L;

    private ShearingOutputBudget() {
    }

    public static Result plan(List<? extends List<ItemStack>> sourceBatches, int multiplier) {
        Objects.requireNonNull(sourceBatches, "sourceBatches");
        int safeMultiplier = Math.max(0, multiplier);
        long originalItems = 0L;
        long sourceStacks = 0L;
        for (List<ItemStack> batch : sourceBatches) {
            Objects.requireNonNull(batch, "source batch");
            for (ItemStack stack : batch) {
                if (!hasOutput(stack)) {
                    continue;
                }
                sourceStacks = LootOutputBudget.saturatedAdd(sourceStacks, 1L);
                originalItems = LootOutputBudget.saturatedAdd(originalItems, stack.getCount());
            }
        }

        long multipliedItems = LootOutputBudget.saturatedMultiply(originalItems, safeMultiplier);
        if (sourceStacks > MAX_SOURCE_OR_MATERIALIZED_STACKS) {
            return Result.exceeded(
                    LimitExceeded.SOURCE_STACKS,
                    originalItems,
                    multipliedItems,
                    sourceStacks,
                    0L);
        }
        if (multipliedItems > MAX_MULTIPLIED_ITEMS) {
            return Result.exceeded(
                    LimitExceeded.ITEMS,
                    originalItems,
                    multipliedItems,
                    sourceStacks,
                    0L);
        }

        if (safeMultiplier == 1) {
            List<List<ItemStack>> unchanged = new ArrayList<>(sourceBatches.size());
            for (List<ItemStack> batch : sourceBatches) {
                List<ItemStack> retained = new ArrayList<>();
                for (ItemStack stack : batch) {
                    if (hasOutput(stack)) {
                        retained.add(stack);
                    }
                }
                unchanged.add(List.copyOf(retained));
            }
            return Result.fits(
                    originalItems,
                    multipliedItems,
                    sourceStacks,
                    sourceStacks,
                    List.copyOf(unchanged));
        }

        List<BatchPlan> plans = new ArrayList<>(sourceBatches.size());
        long materializedStacks = 0L;
        for (List<ItemStack> batch : sourceBatches) {
            BatchPlan plan = BatchPlan.prepare(batch, safeMultiplier);
            plans.add(plan);
            materializedStacks = LootOutputBudget.saturatedAdd(
                    materializedStacks,
                    plan.materializedStackCount());
            if (materializedStacks > MAX_SOURCE_OR_MATERIALIZED_STACKS) {
                return Result.exceeded(
                        LimitExceeded.MATERIALIZED_STACKS,
                        originalItems,
                        multipliedItems,
                        sourceStacks,
                        materializedStacks);
            }
        }

        List<List<ItemStack>> outputBatches = new ArrayList<>(plans.size());
        for (BatchPlan plan : plans) {
            outputBatches.add(plan.materialize());
        }
        return Result.fits(
                originalItems,
                multipliedItems,
                sourceStacks,
                materializedStacks,
                List.copyOf(outputBatches));
    }

    private static boolean hasOutput(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getCount() > 0;
    }

    public enum LimitExceeded {
        NONE,
        SOURCE_STACKS,
        ITEMS,
        MATERIALIZED_STACKS
    }

    public record Result(
            LimitExceeded limitExceeded,
            long originalItems,
            long multipliedItems,
            long sourceStacks,
            long materializedStacks,
            List<List<ItemStack>> outputBatches
    ) {
        private static Result fits(
                long originalItems,
                long multipliedItems,
                long sourceStacks,
                long materializedStacks,
                List<List<ItemStack>> outputBatches
        ) {
            return new Result(
                    LimitExceeded.NONE,
                    originalItems,
                    multipliedItems,
                    sourceStacks,
                    materializedStacks,
                    outputBatches);
        }

        private static Result exceeded(
                LimitExceeded limitExceeded,
                long originalItems,
                long multipliedItems,
                long sourceStacks,
                long materializedStacks
        ) {
            return new Result(
                    limitExceeded,
                    originalItems,
                    multipliedItems,
                    sourceStacks,
                    materializedStacks,
                    List.of());
        }

        public boolean fits() {
            return limitExceeded == LimitExceeded.NONE;
        }
    }

    private static final class BatchPlan {
        private final List<Group> groups;
        private final long materializedStackCount;

        private BatchPlan(List<Group> groups, long materializedStackCount) {
            this.groups = groups;
            this.materializedStackCount = materializedStackCount;
        }

        private static BatchPlan prepare(List<ItemStack> source, int multiplier) {
            if (multiplier == 0) {
                return new BatchPlan(List.of(), 0L);
            }

            Map<StackKey, Group> groups = new LinkedHashMap<>();
            for (ItemStack stack : source) {
                if (!hasOutput(stack)) {
                    continue;
                }
                StackKey lookup = StackKey.lookup(stack);
                Group group = groups.get(lookup);
                if (group == null) {
                    StackKey retained = StackKey.retained(stack);
                    group = new Group(retained.prototype);
                    groups.put(retained, group);
                }
                group.count = LootOutputBudget.saturatedAdd(
                        group.count,
                        LootOutputBudget.saturatedMultiply(stack.getCount(), multiplier));
            }

            long materializedStackCount = 0L;
            for (Group group : groups.values()) {
                materializedStackCount = LootOutputBudget.saturatedAdd(
                        materializedStackCount,
                        LootOutputBudget.legalStackCount(
                                group.count,
                                group.prototype.getMaxStackSize()));
            }
            return new BatchPlan(List.copyOf(groups.values()), materializedStackCount);
        }

        private long materializedStackCount() {
            return materializedStackCount;
        }

        private List<ItemStack> materialize() {
            List<ItemStack> output = new ArrayList<>((int) materializedStackCount);
            for (Group group : groups) {
                long remaining = group.count;
                int maximumStackSize = Math.max(1, group.prototype.getMaxStackSize());
                while (remaining > 0L) {
                    int amount = (int) Math.min((long) maximumStackSize, remaining);
                    output.add(group.prototype.copyWithCount(amount));
                    remaining -= amount;
                }
            }
            return List.copyOf(output);
        }
    }

    private static final class Group {
        private final ItemStack prototype;
        private long count;

        private Group(ItemStack prototype) {
            this.prototype = prototype;
        }
    }

    private static final class StackKey {
        private final ItemStack prototype;
        private final int hash;

        private StackKey(ItemStack prototype, boolean retain) {
            this.prototype = retain ? prototype.copyWithCount(1) : prototype;
            this.hash = ItemStack.hashItemAndComponents(this.prototype);
        }

        private static StackKey lookup(ItemStack stack) {
            return new StackKey(stack, false);
        }

        private static StackKey retained(ItemStack stack) {
            return new StackKey(stack, true);
        }

        @Override
        public boolean equals(Object other) {
            return this == other
                    || other instanceof StackKey key
                    && ItemStack.isSameItemSameComponents(prototype, key.prototype);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
