package com.chedidandrew.smartresourcedrops.platform.neoforge.mixin;

import com.chedidandrew.smartresourcedrops.core.DropContext;
import com.chedidandrew.smartresourcedrops.core.DropSource;
import com.chedidandrew.smartresourcedrops.core.provenance.RecentRemovalCache;
import com.chedidandrew.smartresourcedrops.platform.PlatformPlayerSupport;
import com.chedidandrew.smartresourcedrops.provenance.PlacementTracker;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** NeoForge 20.5's removal hook predates the BlockState parameter added in 20.6. */
@Mixin(ServerPlayerGameMode.class)
abstract class ServerPlayerGameModeMixin {
    @Shadow
    protected ServerLevel level;

    @Shadow
    protected ServerPlayer player;

    /**
     * NeoForge 20.5 awards player loot and block XP in two separate calls inside
     * destroyBlock. Keep one decision context alive across both calls so its
     * patched seven-argument dropResources path and later popExperience path
     * behave like Fabric's vanilla pipeline.
     */
    @WrapMethod(method = "destroyBlock(Lnet/minecraft/core/BlockPos;)Z")
    private boolean smartdrops$wrapDestroyBlock(
            final BlockPos pos,
            final Operation<Boolean> original
    ) {
        final BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return original.call(pos);
        }

        final BlockEntity blockEntity = level.getBlockEntity(pos);
        final boolean realPlayer = !PlatformPlayerSupport.isFakePlayer(player);
        if (realPlayer) {
            DropContext.beginPlayer(level, player, pos, state, blockEntity);
        } else {
            DropContext.beginAutomation(level, pos, state, blockEntity, player);
        }
        try {
            return original.call(pos);
        } finally {
            if (realPlayer) {
                DropContext.endExpected(DropSource.PLAYER);
            } else {
                DropContext.endAutomation();
            }
        }
    }

    @Inject(
            method = "removeBlock(Lnet/minecraft/core/BlockPos;Z)Z",
            at = @At("RETURN"),
            remap = false,
            require = 1,
            expect = 1)
    private void smartdrops$afterSuccessfulRemoval(
            final BlockPos pos,
            final boolean canHarvest,
            final CallbackInfoReturnable<Boolean> callback
    ) {
        if (callback.getReturnValueZ() && PlacementTracker.isMarked(level, pos)) {
            RecentRemovalCache.record(level, pos);
            PlacementTracker.remove(level, pos);
        }
    }
}
