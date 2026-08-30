package com.chedidandrew.smartresourcedrops.mixin;

import com.chedidandrew.smartresourcedrops.config.ConfigManager;
import com.chedidandrew.smartresourcedrops.provenance.PlacementTracker;
import com.chedidandrew.smartresourcedrops.provenance.ProtectedPistonMovement;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PistonMovingBlockEntity.class)
abstract class PistonMovingBlockEntityMixin implements ProtectedPistonMovement {
    @Unique
    private static final String SMART_RESOURCE_DROPS_CAPTURED = "SmartResourceDropsCaptured";

    @Unique
    private static final String SMART_RESOURCE_DROPS_PROTECT_DESTINATION = "SmartResourceDropsProtectDestination";

    @Unique
    private boolean smartResourceDrops$captured;

    @Unique
    private boolean smartResourceDrops$protectDestination;

    @Inject(method = "tick", at = @At("HEAD"))
    private static void smartResourceDrops$captureBeforeTick(
            Level level,
            BlockPos pos,
            BlockState state,
            PistonMovingBlockEntity entity,
            CallbackInfo callback
    ) {
        smartResourceDrops$capture(level, pos, entity);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private static void smartResourceDrops$finishAfterTick(
            Level level,
            BlockPos pos,
            BlockState state,
            PistonMovingBlockEntity entity,
            CallbackInfo callback
    ) {
        smartResourceDrops$finish(level, pos, entity);
    }

    @Inject(method = "finalTick", at = @At("HEAD"))
    private void smartResourceDrops$captureBeforeFinalTick(CallbackInfo callback) {
        PistonMovingBlockEntity entity = (PistonMovingBlockEntity) (Object) this;
        Level level = entity.getLevel();
        if (level != null) {
            smartResourceDrops$capture(level, entity.getBlockPos(), entity);
        }
    }

    @Inject(method = "finalTick", at = @At("TAIL"))
    private void smartResourceDrops$finishAfterFinalTick(CallbackInfo callback) {
        PistonMovingBlockEntity entity = (PistonMovingBlockEntity) (Object) this;
        Level level = entity.getLevel();
        if (level != null) {
            smartResourceDrops$finish(level, entity.getBlockPos(), entity);
        }
    }


    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void smartResourceDrops$savePistonProvenance(ValueOutput output, CallbackInfo callback) {
        output.putBoolean(SMART_RESOURCE_DROPS_CAPTURED, smartResourceDrops$captured);
        output.putBoolean(SMART_RESOURCE_DROPS_PROTECT_DESTINATION, smartResourceDrops$protectDestination);
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void smartResourceDrops$loadPistonProvenance(ValueInput input, CallbackInfo callback) {
        smartResourceDrops$captured = input.getBooleanOr(SMART_RESOURCE_DROPS_CAPTURED, false);
        smartResourceDrops$protectDestination = input.getBooleanOr(SMART_RESOURCE_DROPS_PROTECT_DESTINATION, false);
    }

    @Override
    public boolean smartResourceDrops$isCaptured() {
        return smartResourceDrops$captured;
    }

    @Override
    public void smartResourceDrops$setCaptured(boolean value) {
        smartResourceDrops$captured = value;
    }

    @Override
    public boolean smartResourceDrops$shouldProtectDestination() {
        return smartResourceDrops$protectDestination;
    }

    @Override
    public void smartResourceDrops$setProtectDestination(boolean value) {
        smartResourceDrops$protectDestination = value;
    }

    @Unique
    private static void smartResourceDrops$capture(
            Level level,
            BlockPos destination,
            PistonMovingBlockEntity entity
    ) {
        ProtectedPistonMovement carrier = (ProtectedPistonMovement) (Object) entity;
        if (carrier.smartResourceDrops$isCaptured() || entity.isSourcePiston() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        carrier.smartResourceDrops$setCaptured(true);
        BlockPos source = destination.relative(entity.getMovementDirection().getOpposite());
        boolean sourceWasPlaced = PlacementTracker.remove(serverLevel, source);
        carrier.smartResourceDrops$setProtectDestination(
                sourceWasPlaced || ConfigManager.get().conservativePistonProtection);
    }

    @Unique
    private static void smartResourceDrops$finish(
            Level level,
            BlockPos destination,
            PistonMovingBlockEntity entity
    ) {
        ProtectedPistonMovement carrier = (ProtectedPistonMovement) (Object) entity;
        if (!carrier.smartResourceDrops$isCaptured()
                || !carrier.smartResourceDrops$shouldProtectDestination()
                || entity.isSourcePiston()
                || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockState current = level.getBlockState(destination);
        if (!current.is(Blocks.MOVING_PISTON) && !current.isAir()) {
            PlacementTracker.mark(serverLevel, destination);
            carrier.smartResourceDrops$setProtectDestination(false);
        }
    }
}
