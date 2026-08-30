package com.chedidandrew.smartresourcedrops.core.util;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Plans one complete block-loot result before allocating any amplified output stacks. */
public final class BlockLootOutputBudget {
    private BlockLootOutputBudget() {
    }

    public static Result multiply(final List<ItemStack> original, final int multiplier) {
        Objects.requireNonNull(original, "original");
        final int safeMultiplier = Math.max(0, multiplier);
        if (safeMultiplier == 0) {
            return new Result(
                    new ArrayList<>(),
                    Outcome.SUPPRESSED,
                    StackConsolidator.countItems(original),
                    0L,
                    0L,
                    EstimateKind.EXACT);
        }
        if (safeMultiplier == 1) {
            final long originalItems = StackConsolidator.countItems(original);
            return new Result(
                    original,
                    Outcome.IDENTITY,
                    originalItems,
                    originalItems,
                    original.size(),
                    EstimateKind.UPPER_BOUND);
        }

        final StackConsolidator.PreparedMultiplication prepared = StackConsolidator.prepare(
                original,
                safeMultiplier,
                LootOutputBudget.MAX_MULTIPLIED_ITEMS,
                LootOutputBudget.MAX_MULTIPLIED_STACKS);
        if (prepared.fits()) {
            return new Result(
                    prepared.materialize(),
                    Outcome.MULTIPLIED,
                    prepared.originalItems(),
                    prepared.multipliedItems(),
                    prepared.legalStacks(),
                    estimateKind(prepared.stackEstimateKind()));
        }

        final Outcome fallback = prepared.limitExceeded() == StackConsolidator.LimitExceeded.ITEMS
                ? Outcome.FALLBACK_ITEM_LIMIT
                : Outcome.FALLBACK_STACK_LIMIT;
        return new Result(
                original,
                fallback,
                prepared.originalItems(),
                prepared.multipliedItems(),
                prepared.legalStacks(),
                estimateKind(prepared.stackEstimateKind()));
    }

    private static EstimateKind estimateKind(final StackConsolidator.StackEstimateKind kind) {
        return switch (kind) {
            case EXACT -> EstimateKind.EXACT;
            case LOWER_BOUND -> EstimateKind.LOWER_BOUND;
            case UPPER_BOUND -> EstimateKind.UPPER_BOUND;
        };
    }

    public enum Outcome {
        SUPPRESSED,
        IDENTITY,
        MULTIPLIED,
        FALLBACK_ITEM_LIMIT,
        FALLBACK_STACK_LIMIT
    }

    public enum EstimateKind {
        EXACT,
        LOWER_BOUND,
        UPPER_BOUND
    }

    public record Result(
            List<ItemStack> output,
            Outcome outcome,
            long originalItemCount,
            long multipliedItemEstimate,
            long legalStackEstimate,
            EstimateKind stackEstimateKind
    ) {
        public Result {
            Objects.requireNonNull(output, "output");
            Objects.requireNonNull(outcome, "outcome");
            Objects.requireNonNull(stackEstimateKind, "stackEstimateKind");
        }

        public boolean fellBackToVanilla() {
            return outcome == Outcome.FALLBACK_ITEM_LIMIT
                    || outcome == Outcome.FALLBACK_STACK_LIMIT;
        }
    }
}
