package com.chedidandrew.smartresourcedrops.core.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.minecraft.world.item.ItemStack;

/**
 * Multiplies already-calculated loot while emitting legal stack sizes.
 * This preserves item components and avoids one item entity per multiplier copy.
 */
public final class StackConsolidator {
    private StackConsolidator() {
    }

    public static List<ItemStack> multiply(final List<ItemStack> source, final int copies) {
        Objects.requireNonNull(source, "source");
        if (copies <= 0) {
            return new ArrayList<>();
        }
        if (copies == 1) {
            return source;
        }

        final PreparedMultiplication prepared = prepare(
                source,
                copies,
                LootOutputBudget.MAX_MULTIPLIED_ITEMS,
                LootOutputBudget.MAX_MULTIPLIED_STACKS);
        return prepared.fits() ? prepared.materialize() : source;
    }

    static PreparedMultiplication prepare(
            final List<ItemStack> source,
            final int copies,
            final long maximumItems,
            final long maximumStacks
    ) {
        Objects.requireNonNull(source, "source");
        if (copies <= 1) {
            throw new IllegalArgumentException("Bounded preparation requires at least two copies");
        }
        if (maximumItems < 1L || maximumStacks < 1L) {
            throw new IllegalArgumentException("Output limits must be positive");
        }

        long originalItems = 0L;
        long perEntryStackUpperBound = 0L;
        for (ItemStack stack : source) {
            if (stack == null || stack.isEmpty() || stack.getCount() <= 0) {
                continue;
            }
            originalItems = LootOutputBudget.saturatedAdd(originalItems, stack.getCount());
            final long multipliedEntry = LootOutputBudget.saturatedMultiply(stack.getCount(), copies);
            perEntryStackUpperBound = LootOutputBudget.saturatedAdd(
                    perEntryStackUpperBound,
                    LootOutputBudget.legalStackCount(multipliedEntry, stack.getMaxStackSize()));
        }

        final long multipliedItems = LootOutputBudget.saturatedMultiply(originalItems, copies);
        if (multipliedItems > maximumItems) {
            return PreparedMultiplication.itemLimit(
                    originalItems,
                    multipliedItems,
                    perEntryStackUpperBound);
        }

        final Map<StackKey, Group> groups = new LinkedHashMap<>();
        long legalStacks = 0L;
        for (ItemStack stack : source) {
            if (stack == null || stack.isEmpty() || stack.getCount() <= 0) {
                continue;
            }

            final StackKey lookup = StackKey.lookup(stack);
            Group group = groups.get(lookup);
            final long multipliedEntry = LootOutputBudget.saturatedMultiply(stack.getCount(), copies);
            if (group == null) {
                if (groups.size() >= maximumStacks) {
                    return PreparedMultiplication.stackLimit(
                            originalItems,
                            multipliedItems,
                            maximumStacks + 1L);
                }
                final StackKey retained = StackKey.retained(stack);
                group = new Group(retained.prototype, 0L);
                groups.put(retained, group);
            }

            final long previousGroupStacks = LootOutputBudget.legalStackCount(
                    group.count,
                    group.prototype.getMaxStackSize());
            group.count = LootOutputBudget.saturatedAdd(group.count, multipliedEntry);
            final long updatedGroupStacks = LootOutputBudget.legalStackCount(
                    group.count,
                    group.prototype.getMaxStackSize());
            legalStacks = LootOutputBudget.saturatedAdd(
                    legalStacks,
                    updatedGroupStacks - previousGroupStacks);
            if (legalStacks > maximumStacks) {
                return PreparedMultiplication.stackLimit(
                        originalItems,
                        multipliedItems,
                        legalStacks);
            }
        }

        return PreparedMultiplication.fits(
                originalItems,
                multipliedItems,
                legalStacks,
                List.copyOf(groups.values()));
    }

    static long countItems(final List<ItemStack> source) {
        Objects.requireNonNull(source, "source");
        long count = 0L;
        for (ItemStack stack : source) {
            if (stack != null && !stack.isEmpty() && stack.getCount() > 0) {
                count = LootOutputBudget.saturatedAdd(count, stack.getCount());
            }
        }
        return count;
    }

    private static final class Group {
        private final ItemStack prototype;
        private long count;

        private Group(final ItemStack prototype, final long count) {
            this.prototype = prototype;
            this.count = count;
        }
    }

    private static final class StackKey {
        private final ItemStack prototype;
        private final int hash;

        private StackKey(final ItemStack prototype, final boolean retain) {
            this.prototype = retain ? prototype.copyWithCount(1) : prototype;
            this.hash = ItemStack.hashItemAndComponents(this.prototype);
        }

        private static StackKey lookup(final ItemStack stack) {
            return new StackKey(stack, false);
        }

        private static StackKey retained(final ItemStack stack) {
            return new StackKey(stack, true);
        }

        @Override
        public boolean equals(final Object other) {
            return this == other
                    || other instanceof StackKey key
                    && ItemStack.isSameItemSameComponents(prototype, key.prototype);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    enum LimitExceeded {
        NONE,
        ITEMS,
        STACKS
    }

    enum StackEstimateKind {
        EXACT,
        LOWER_BOUND,
        UPPER_BOUND
    }

    static final class PreparedMultiplication {
        private final long originalItems;
        private final long multipliedItems;
        private final long legalStacks;
        private final StackEstimateKind stackEstimateKind;
        private final LimitExceeded limitExceeded;
        private final List<Group> groups;

        private PreparedMultiplication(
                final long originalItems,
                final long multipliedItems,
                final long legalStacks,
                final StackEstimateKind stackEstimateKind,
                final LimitExceeded limitExceeded,
                final List<Group> groups
        ) {
            this.originalItems = originalItems;
            this.multipliedItems = multipliedItems;
            this.legalStacks = legalStacks;
            this.stackEstimateKind = stackEstimateKind;
            this.limitExceeded = limitExceeded;
            this.groups = groups;
        }

        private static PreparedMultiplication fits(
                final long originalItems,
                final long multipliedItems,
                final long legalStacks,
                final List<Group> groups
        ) {
            return new PreparedMultiplication(
                    originalItems,
                    multipliedItems,
                    legalStacks,
                    StackEstimateKind.EXACT,
                    LimitExceeded.NONE,
                    groups);
        }

        private static PreparedMultiplication itemLimit(
                final long originalItems,
                final long multipliedItems,
                final long legalStackUpperBound
        ) {
            return new PreparedMultiplication(
                    originalItems,
                    multipliedItems,
                    legalStackUpperBound,
                    StackEstimateKind.UPPER_BOUND,
                    LimitExceeded.ITEMS,
                    List.of());
        }

        private static PreparedMultiplication stackLimit(
                final long originalItems,
                final long multipliedItems,
                final long legalStackLowerBound
        ) {
            return new PreparedMultiplication(
                    originalItems,
                    multipliedItems,
                    legalStackLowerBound,
                    StackEstimateKind.LOWER_BOUND,
                    LimitExceeded.STACKS,
                    List.of());
        }

        long originalItems() {
            return originalItems;
        }

        long multipliedItems() {
            return multipliedItems;
        }

        long legalStacks() {
            return legalStacks;
        }

        StackEstimateKind stackEstimateKind() {
            return stackEstimateKind;
        }

        LimitExceeded limitExceeded() {
            return limitExceeded;
        }

        boolean fits() {
            return limitExceeded == LimitExceeded.NONE;
        }

        List<ItemStack> materialize() {
            if (!fits()) {
                throw new IllegalStateException("Cannot materialize an over-budget loot plan");
            }
            final List<ItemStack> result = new ArrayList<>((int) legalStacks);
            for (Group group : groups) {
                long remaining = group.count;
                final int maximumStackSize = Math.max(1, group.prototype.getMaxStackSize());
                while (remaining > 0L) {
                    final int amount = (int) Math.min((long) maximumStackSize, remaining);
                    result.add(group.prototype.copyWithCount(amount));
                    remaining -= amount;
                }
            }
            return result;
        }
    }
}
