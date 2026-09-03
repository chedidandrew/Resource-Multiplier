package com.chedidandrew.smartresourcedrops.core.shearing;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Buffers the classic 1.20.1 shearing path, where shearable entities spawn
 * {@link ItemEntity} instances directly instead of using a shearing loot-table consumer.
 */
final class ShearingSpawnOutputBuffer {
    private final ServerLevel level;
    private final int multiplier;
    private final Consumer<ShearingOutputBudget.Result> fallbackListener;
    private final Consumer<Throwable> rollbackFailureListener;
    private final List<ItemEntity> originals = new ArrayList<>();
    private boolean finished;

    ShearingSpawnOutputBuffer(
            ServerLevel level,
            int multiplier,
            Consumer<ShearingOutputBudget.Result> fallbackListener,
            Consumer<Throwable> rollbackFailureListener
    ) {
        this.level = Objects.requireNonNull(level, "level");
        this.multiplier = Math.max(0, multiplier);
        this.fallbackListener = Objects.requireNonNull(fallbackListener, "fallbackListener");
        this.rollbackFailureListener = Objects.requireNonNull(
                rollbackFailureListener,
                "rollbackFailureListener");
    }

    boolean capture(ItemEntity entity) {
        if (finished) {
            return false;
        }
        originals.add(Objects.requireNonNull(entity, "entity"));
        return true;
    }

    void complete() {
        if (finished) {
            return;
        }
        finished = true;
        List<List<ItemStack>> source = originals.stream()
                .map(entity -> List.of(entity.getItem().copy()))
                .toList();
        ShearingOutputBudget.Result result = ShearingOutputBudget.plan(source, multiplier);
        if (!result.fits()) {
            fallbackListener.accept(result);
            emitOriginals();
            return;
        }
        emitPlanned(result.outputBatches());
    }

    void abort() {
        if (finished) {
            return;
        }
        finished = true;
        try {
            emitOriginals();
        } catch (RuntimeException | Error failure) {
            try {
                rollbackFailureListener.accept(failure);
            } catch (RuntimeException | Error ignored) {
                // Preserve the original shearing failure.
            }
        }
    }

    boolean finished() {
        return finished;
    }

    private void emitOriginals() {
        for (ItemEntity entity : originals) {
            level.addFreshEntity(entity);
        }
        originals.clear();
    }

    private void emitPlanned(List<List<ItemStack>> batches) {
        if (batches.size() != originals.size()) {
            throw new IllegalStateException("Shearing output plan lost a direct-spawn batch");
        }
        for (int index = 0; index < originals.size(); index++) {
            ItemEntity original = originals.get(index);
            List<ItemStack> outputs = batches.get(index);
            if (outputs.isEmpty()) {
                continue;
            }

            original.setItem(outputs.get(0));
            level.addFreshEntity(original);
            Vec3 motion = original.getDeltaMovement();
            for (int outputIndex = 1; outputIndex < outputs.size(); outputIndex++) {
                ItemEntity extra = new ItemEntity(
                        level,
                        original.getX(),
                        original.getY(),
                        original.getZ(),
                        outputs.get(outputIndex),
                        motion.x,
                        motion.y,
                        motion.z);
                extra.setDefaultPickUpDelay();
                level.addFreshEntity(extra);
            }
        }
        originals.clear();
    }
}
