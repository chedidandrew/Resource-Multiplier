package com.chedidandrew.smartresourcedrops.core.util;

/** Shared, non-configurable safety limits for amplified loot output. */
public final class LootOutputBudget {
    public static final long MAX_MULTIPLIED_ITEMS = 262_144L;
    public static final long MAX_MULTIPLIED_STACKS = 4_096L;

    private LootOutputBudget() {
    }

    public static long saturatedAdd(final long left, final long right) {
        if (left <= 0L) {
            return Math.max(0L, right);
        }
        if (right <= 0L) {
            return left;
        }
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    public static long saturatedMultiply(final long left, final long right) {
        if (left <= 0L || right <= 0L) {
            return 0L;
        }
        return left > Long.MAX_VALUE / right ? Long.MAX_VALUE : left * right;
    }

    public static long legalStackCount(final long itemCount, final int maximumStackSize) {
        if (itemCount <= 0L) {
            return 0L;
        }
        final long safeMaximum = Math.max(1, maximumStackSize);
        return itemCount / safeMaximum + (itemCount % safeMaximum == 0L ? 0L : 1L);
    }
}
