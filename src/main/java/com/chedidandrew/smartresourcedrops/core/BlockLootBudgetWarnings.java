package com.chedidandrew.smartresourcedrops.core;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.core.util.BlockLootOutputBudget;
import com.chedidandrew.smartresourcedrops.core.util.BoundedRateLimiter;
import com.chedidandrew.smartresourcedrops.core.util.LootOutputBudget;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.time.Duration;

/** Emits bounded, farm-safe diagnostics when one complete block-loot event falls back. */
final class BlockLootBudgetWarnings {
    private static final int MAX_WARNING_KEYS = 256;
    private static final int MAX_VISIBLE_ID_LENGTH = 256;
    private static final BoundedRateLimiter LIMITER = new BoundedRateLimiter(
            MAX_WARNING_KEYS,
            Duration.ofMinutes(5L).toNanos());

    private BlockLootBudgetWarnings() {
    }

    static void warn(
            final ServerLevel level,
            final BlockPos pos,
            final BlockState state,
            final int multiplier,
            final BlockLootOutputBudget.Result result
    ) {
        final String blockId = bounded(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
        final String dimensionId = bounded(level.dimension().location().toString());
        final String reason = result.outcome().name();
        if (!LIMITER.tryAcquire(blockId + '|' + reason, System.nanoTime())) {
            return;
        }

        SmartResourceDrops.LOGGER.warn(
                "Block loot multiplication budget exceeded for {} at {} in {} (requested {}x; "
                        + "estimated output: {} items / {} legal stacks; limits: {} items / {} stacks). "
                        + "Using the original vanilla 1x loot for this event.",
                blockId,
                pos,
                dimensionId,
                multiplier,
                estimate(result.multipliedItemEstimate(), BlockLootOutputBudget.EstimateKind.EXACT),
                estimate(result.legalStackEstimate(), result.stackEstimateKind()),
                LootOutputBudget.MAX_MULTIPLIED_ITEMS,
                LootOutputBudget.MAX_MULTIPLIED_STACKS);
    }

    private static String estimate(
            final long value,
            final BlockLootOutputBudget.EstimateKind kind
    ) {
        final String number = value == Long.MAX_VALUE ? "overflow" : Long.toString(value);
        return switch (kind) {
            case EXACT -> number;
            case LOWER_BOUND -> "at least " + number;
            case UPPER_BOUND -> "at most " + number;
        };
    }

    private static String bounded(final String value) {
        if (value.length() <= MAX_VISIBLE_ID_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_VISIBLE_ID_LENGTH - 1) + '…';
    }
}
