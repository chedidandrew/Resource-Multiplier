package com.chedidandrew.smartresourcedrops.core;

import com.chedidandrew.smartresourcedrops.config.ConfigManager;
import com.chedidandrew.smartresourcedrops.core.util.LootOutputBudget;

import java.util.concurrent.atomic.AtomicLong;

public final class SmartDropsStats {
    private static final AtomicLong BLOCKS_EVALUATED = new AtomicLong();
    private static final AtomicLong BLOCKS_MULTIPLIED = new AtomicLong();
    private static final AtomicLong VANILLA_ITEMS = new AtomicLong();
    private static final AtomicLong BONUS_ITEMS = new AtomicLong();
    private static final AtomicLong SUPPRESSED_ITEMS = new AtomicLong();
    private static final AtomicLong BONUS_EXPERIENCE = new AtomicLong();
    private static final AtomicLong BLOCK_BUDGET_FALLBACKS = new AtomicLong();

    private SmartDropsStats() {
    }

    public static void recordDrops(int multiplier, long originalCount) {
        if (!ConfigManager.get().statisticsEnabled) {
            return;
        }
        increment(BLOCKS_EVALUATED);
        add(VANILLA_ITEMS, Math.max(0L, originalCount));
        if (multiplier > 1) {
            increment(BLOCKS_MULTIPLIED);
            add(BONUS_ITEMS, LootOutputBudget.saturatedMultiply(
                    Math.max(0L, originalCount),
                    multiplier - 1L));
        } else if (multiplier == 0) {
            add(SUPPRESSED_ITEMS, Math.max(0L, originalCount));
        }
    }

    public static void recordBlockBudgetFallback(final long originalCount) {
        if (!ConfigManager.get().statisticsEnabled) {
            return;
        }
        increment(BLOCKS_EVALUATED);
        add(VANILLA_ITEMS, Math.max(0L, originalCount));
        increment(BLOCK_BUDGET_FALLBACKS);
    }

    public static void recordExperience(int originalAmount, int multipliedAmount) {
        if (!ConfigManager.get().statisticsEnabled) {
            return;
        }
        add(BONUS_EXPERIENCE, Math.max(0L, (long) multipliedAmount - originalAmount));
    }

    public static Snapshot snapshot() {
        return new Snapshot(
                BLOCKS_EVALUATED.get(),
                BLOCKS_MULTIPLIED.get(),
                VANILLA_ITEMS.get(),
                BONUS_ITEMS.get(),
                SUPPRESSED_ITEMS.get(),
                BONUS_EXPERIENCE.get(),
                BLOCK_BUDGET_FALLBACKS.get());
    }

    public static void reset() {
        BLOCKS_EVALUATED.set(0);
        BLOCKS_MULTIPLIED.set(0);
        VANILLA_ITEMS.set(0);
        BONUS_ITEMS.set(0);
        SUPPRESSED_ITEMS.set(0);
        BONUS_EXPERIENCE.set(0);
        BLOCK_BUDGET_FALLBACKS.set(0);
    }

    private static void increment(final AtomicLong counter) {
        add(counter, 1L);
    }

    private static void add(final AtomicLong counter, final long amount) {
        if (amount <= 0L) {
            return;
        }
        counter.getAndUpdate(current -> LootOutputBudget.saturatedAdd(current, amount));
    }

    public record Snapshot(
            long blocksEvaluated,
            long blocksMultiplied,
            long vanillaItems,
            long bonusItems,
            long suppressedItems,
            long bonusExperience,
            long blockBudgetFallbacks
    ) {
    }
}
