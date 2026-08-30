package com.chedidandrew.smartresourcedrops.core.shearing;

import com.chedidandrew.smartresourcedrops.core.util.LootOutputBudget;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** One action's delayed, consumer-preserving output. Package-private for focused tests. */
final class ShearingOutputBuffer<L> {
    private final int multiplier;
    private final Consumer<ShearingOutputBudget.Result> fallbackListener;
    private final Consumer<Throwable> rollbackFailureListener;
    private final List<Batch<L>> batches = new ArrayList<>();

    private State state = State.ACTIVE;
    private boolean passthroughFallback;
    private long collectedSourceStacks;
    private long proposedItems;

    ShearingOutputBuffer(
            int multiplier,
            Consumer<ShearingOutputBudget.Result> fallbackListener,
            Consumer<Throwable> rollbackFailureListener
    ) {
        this.multiplier = Math.max(0, multiplier);
        this.fallbackListener = Objects.requireNonNull(fallbackListener, "fallbackListener");
        this.rollbackFailureListener = Objects.requireNonNull(
                rollbackFailureListener,
                "rollbackFailureListener");
    }

    BiConsumer<L, ItemStack> openBatch(L expectedLevel, BiConsumer<L, ItemStack> downstream) {
        Objects.requireNonNull(expectedLevel, "expectedLevel");
        Objects.requireNonNull(downstream, "downstream");
        if (state != State.ACTIVE || passthroughFallback) {
            return downstream;
        }

        Batch<L> batch = new Batch<>(expectedLevel, downstream);
        batches.add(batch);
        return (outputLevel, stack) -> collect(batch, outputLevel, stack);
    }

    void complete() {
        if (state != State.ACTIVE) {
            return;
        }
        if (passthroughFallback) {
            state = State.FINISHED;
            batches.clear();
            return;
        }

        ShearingOutputBudget.Result result = ShearingOutputBudget.plan(sourceBatches(), multiplier);
        if (!result.fits()) {
            fallbackListener.accept(result);
        }

        state = State.COMMITTING;
        try {
            if (result.fits()) {
                emitMultiplied(result.outputBatches());
            } else {
                emitOriginals();
            }
            state = State.FINISHED;
            batches.clear();
        } catch (RuntimeException | Error exception) {
            state = State.EMISSION_FAILED;
            batches.clear();
            throw exception;
        }
    }

    /** Emits the closest vanilla 1x output without masking the action's original exception. */
    void abort() {
        if (state != State.ACTIVE) {
            return;
        }
        state = State.ABORTING;
        try {
            if (!passthroughFallback) {
                emitOriginals();
            }
        } catch (RuntimeException | Error rollbackFailure) {
            try {
                rollbackFailureListener.accept(rollbackFailure);
            } catch (RuntimeException | Error ignored) {
                // The original shearing failure must remain the exception propagated by the caller.
            }
        } finally {
            state = State.FINISHED;
            batches.clear();
        }
    }

    boolean finished() {
        return state == State.FINISHED || state == State.EMISSION_FAILED;
    }

    private void collect(Batch<L> batch, L outputLevel, ItemStack stack) {
        if (state != State.ACTIVE || passthroughFallback) {
            batch.downstream.accept(outputLevel, stack);
            return;
        }

        ItemStack snapshot = stack == null ? null : stack.copy();
        batch.entries.add(new Entry<>(outputLevel, snapshot));
        if (hasOutput(snapshot)) {
            collectedSourceStacks = LootOutputBudget.saturatedAdd(collectedSourceStacks, 1L);
            proposedItems = LootOutputBudget.saturatedAdd(
                    proposedItems,
                    LootOutputBudget.saturatedMultiply(snapshot.getCount(), multiplier));
        }
        if (collectedSourceStacks > ShearingOutputBudget.MAX_SOURCE_OR_MATERIALIZED_STACKS
                || proposedItems > ShearingOutputBudget.MAX_MULTIPLIED_ITEMS) {
            switchToPassthroughFallback();
        }
    }

    private void switchToPassthroughFallback() {
        ShearingOutputBudget.Result result = ShearingOutputBudget.plan(sourceBatches(), multiplier);
        passthroughFallback = true;
        fallbackListener.accept(result);
        try {
            emitOriginals();
            batches.clear();
        } catch (RuntimeException | Error exception) {
            state = State.EMISSION_FAILED;
            batches.clear();
            throw exception;
        }
    }

    private List<List<ItemStack>> sourceBatches() {
        List<List<ItemStack>> source = new ArrayList<>(batches.size());
        for (Batch<L> batch : batches) {
            List<ItemStack> stacks = new ArrayList<>(batch.entries.size());
            for (Entry<L> entry : batch.entries) {
                stacks.add(entry.stack);
            }
            source.add(stacks);
        }
        return source;
    }

    private void emitMultiplied(List<List<ItemStack>> outputBatches) {
        if (outputBatches.size() != batches.size()) {
            throw new IllegalStateException("Shearing output plan lost its consumer batches");
        }
        for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
            Batch<L> batch = batches.get(batchIndex);
            for (ItemStack output : outputBatches.get(batchIndex)) {
                batch.downstream.accept(batch.expectedLevel, output);
            }
        }
    }

    private void emitOriginals() {
        for (Batch<L> batch : batches) {
            for (Entry<L> entry : batch.entries) {
                if (entry.emitted) {
                    continue;
                }
                entry.emitted = true;
                batch.downstream.accept(entry.outputLevel, entry.stack);
            }
        }
    }

    private static boolean hasOutput(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getCount() > 0;
    }

    private enum State {
        ACTIVE,
        COMMITTING,
        ABORTING,
        FINISHED,
        EMISSION_FAILED
    }

    private static final class Batch<L> {
        private final L expectedLevel;
        private final BiConsumer<L, ItemStack> downstream;
        private final List<Entry<L>> entries = new ArrayList<>();

        private Batch(L expectedLevel, BiConsumer<L, ItemStack> downstream) {
            this.expectedLevel = expectedLevel;
            this.downstream = downstream;
        }
    }

    private static final class Entry<L> {
        private final L outputLevel;
        private final ItemStack stack;
        private boolean emitted;

        private Entry(L outputLevel, ItemStack stack) {
            this.outputLevel = outputLevel;
            this.stack = stack;
        }
    }
}
