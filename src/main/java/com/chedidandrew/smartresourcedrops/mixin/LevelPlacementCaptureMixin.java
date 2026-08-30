package com.chedidandrew.smartresourcedrops.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.chedidandrew.smartresourcedrops.core.provenance.PlacementCapture;
import com.chedidandrew.smartresourcedrops.core.provenance.PlacementProvenanceBridge;
import com.chedidandrew.smartresourcedrops.core.provenance.ProvenanceTransitionPolicy;
import com.chedidandrew.smartresourcedrops.core.provenance.RecentRemovalCache;

@Mixin(Level.class)
abstract class LevelPlacementCaptureMixin {
    @Unique
    private static final int SMARTDROPS_MOVED_BY_PISTON = Block.UPDATE_MOVE_BY_PISTON;

    @WrapMethod(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z")
    private boolean smartdrops$trackSetBlock(
        final BlockPos pos,
        final BlockState requestedState,
        final int updateFlags,
        final int updateLimit,
        final Operation<Boolean> original
    ) {
        final Level level = (Level) (Object) this;
        if (level.isClientSide()) {
            return original.call(pos, requestedState, updateFlags, updateLimit);
        }

        final BlockState oldState = level.getBlockState(pos);
        final PendingChange change = new PendingChange(
            level,
            pos.immutable(),
            oldState,
            PlacementProvenanceBridge.isPersistentlyPlaced(level, pos),
            updateFlags
        );
        final boolean changed = original.call(pos, requestedState, updateFlags, updateLimit);
        if (!changed) {
            return false;
        }

        final BlockState finalState = change.level().getBlockState(change.pos());
        if (PlacementCapture.active(change.level()) && !finalState.isAir() && finalState != change.oldState()) {
            PlacementCapture.recordCandidate(change.level(), change.pos());
        }

        if (!change.wasPlaced() || (change.updateFlags() & SMARTDROPS_MOVED_BY_PISTON) != 0) {
            return true;
        }

        switch (ProvenanceTransitionPolicy.classify(change.oldState(), finalState)) {
            case PRESERVE -> {
            }
            case GENERATED -> PlacementProvenanceBridge.unmark(change.level(), change.pos());
            case REMOVE -> {
                RecentRemovalCache.record(change.level(), change.pos());
                PlacementProvenanceBridge.unmark(change.level(), change.pos());
            }
        }
        return true;
    }

    private record PendingChange(Level level, BlockPos pos, BlockState oldState, boolean wasPlaced, int updateFlags) {
    }
}
