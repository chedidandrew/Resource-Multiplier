package com.chedidandrew.smartresourcedrops.mixin;

import com.chedidandrew.smartresourcedrops.provenance.PlacementTracker;
import com.chedidandrew.smartresourcedrops.provenance.ProtectedFallingBlock;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FallingBlockEntity.class)
abstract class FallingBlockEntityMixin implements ProtectedFallingBlock {
    @Unique
    private static final String SMART_RESOURCE_DROPS_PROTECTED = "SmartResourceDropsProtected";

    @Unique
    private boolean smartResourceDrops$protectedOrigin;

    @WrapMethod(method = "fall")
    private static FallingBlockEntity smartResourceDrops$wrapFall(
            Level level,
            BlockPos pos,
            BlockState state,
            Operation<FallingBlockEntity> original
    ) {
        final boolean protectedOrigin = level instanceof ServerLevel serverLevel
                && PlacementTracker.remove(serverLevel, pos);
        try {
            final FallingBlockEntity falling = original.call(level, pos, state);
            if (protectedOrigin && falling instanceof ProtectedFallingBlock carrier) {
                carrier.smartResourceDrops$setProtectedOrigin(true);
            }
            return falling;
        } catch (RuntimeException | Error exception) {
            if (protectedOrigin
                    && level instanceof ServerLevel serverLevel
                    && level.getBlockState(pos).is(state.getBlock())) {
                PlacementTracker.mark(serverLevel, pos);
            }
            throw exception;
        }
    }

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean smartResourceDrops$trackLanding(
            Level level,
            BlockPos pos,
            BlockState state,
            int updateFlags
    ) {
        boolean placed = level.setBlock(pos, state, updateFlags);
        if (placed && smartResourceDrops$protectedOrigin && level instanceof ServerLevel serverLevel) {
            PlacementTracker.mark(serverLevel, pos);
            smartResourceDrops$protectedOrigin = false;
        }
        return placed;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void smartResourceDrops$saveProtectedOrigin(ValueOutput output, CallbackInfo callback) {
        output.putBoolean(SMART_RESOURCE_DROPS_PROTECTED, smartResourceDrops$protectedOrigin);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void smartResourceDrops$loadProtectedOrigin(ValueInput input, CallbackInfo callback) {
        smartResourceDrops$protectedOrigin = input.getBooleanOr(SMART_RESOURCE_DROPS_PROTECTED, false);
    }

    @Override
    public void smartResourceDrops$setProtectedOrigin(boolean value) {
        smartResourceDrops$protectedOrigin = value;
    }

    @Override
    public boolean smartResourceDrops$isProtectedOrigin() {
        return smartResourceDrops$protectedOrigin;
    }
}
