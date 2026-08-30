package com.chedidandrew.smartresourcedrops.core.shearing;

import com.chedidandrew.smartresourcedrops.config.SmartDropsConfig;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

/** Package-local test access for the deliberately internal action buffer and synthetic tag states. */
public final class ShearingGameTestAccess {
    private ShearingGameTestAccess() {
    }

    public static ShearingRuleTrace syntheticTrace(
            final SmartDropsConfig config,
            final String entityId,
            final boolean standardTagged,
            final boolean specialTagged,
            final ShearingSource source
    ) {
        return ShearingRuleResolver.trace(
                config,
                entityId,
                standardTagged,
                specialTagged,
                source);
    }

    public static BufferRun complete(
            final int multiplier,
            final List<? extends List<ItemStack>> sourceBatches
    ) {
        return run(multiplier, sourceBatches, false);
    }

    public static BufferRun abort(
            final int multiplier,
            final List<? extends List<ItemStack>> sourceBatches
    ) {
        return run(multiplier, sourceBatches, true);
    }

    private static BufferRun run(
            final int multiplier,
            final List<? extends List<ItemStack>> sourceBatches,
            final boolean abort
    ) {
        final AtomicReference<ShearingOutputBudget.LimitExceeded> fallback =
                new AtomicReference<>(ShearingOutputBudget.LimitExceeded.NONE);
        final AtomicBoolean rollbackWarning = new AtomicBoolean();
        final ShearingOutputBuffer<String> buffer = new ShearingOutputBuffer<>(
                multiplier,
                result -> fallback.set(result.limitExceeded()),
                ignored -> rollbackWarning.set(true));
        final List<List<ItemStack>> emitted = new ArrayList<>(sourceBatches.size());
        final List<BiConsumer<String, ItemStack>> consumers = new ArrayList<>(sourceBatches.size());

        for (int batchIndex = 0; batchIndex < sourceBatches.size(); batchIndex++) {
            final List<ItemStack> batchOutput = new ArrayList<>();
            emitted.add(batchOutput);
            consumers.add(buffer.openBatch(
                    "level",
                    (ignored, stack) -> batchOutput.add(stack == null ? null : stack.copy())));
        }
        for (int batchIndex = 0; batchIndex < sourceBatches.size(); batchIndex++) {
            for (ItemStack stack : sourceBatches.get(batchIndex)) {
                consumers.get(batchIndex).accept("level", stack);
            }
        }

        if (abort) {
            buffer.abort();
        } else {
            buffer.complete();
        }
        return new BufferRun(
                copyBatches(emitted),
                fallback.get(),
                rollbackWarning.get());
    }

    private static List<List<ItemStack>> copyBatches(final List<List<ItemStack>> batches) {
        final List<List<ItemStack>> result = new ArrayList<>(batches.size());
        for (List<ItemStack> batch : batches) {
            final List<ItemStack> copy = new ArrayList<>(batch.size());
            for (ItemStack stack : batch) {
                copy.add(stack == null ? null : stack.copy());
            }
            result.add(List.copyOf(copy));
        }
        return List.copyOf(result);
    }

    public record BufferRun(
            List<List<ItemStack>> emittedBatches,
            ShearingOutputBudget.LimitExceeded fallback,
            boolean rollbackWarning
    ) {
    }
}
