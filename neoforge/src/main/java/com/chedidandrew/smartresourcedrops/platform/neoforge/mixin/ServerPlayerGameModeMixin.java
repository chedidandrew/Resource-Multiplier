package com.chedidandrew.smartresourcedrops.platform.neoforge.mixin;

import com.chedidandrew.smartresourcedrops.core.provenance.RecentRemovalCache;
import com.chedidandrew.smartresourcedrops.provenance.PlacementTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Defensive successful-break cleanup for blocks with custom NeoForge removal hooks. */
@Mixin(ServerPlayerGameMode.class)
abstract class ServerPlayerGameModeMixin {
    @Accessor("level")
    protected abstract ServerLevel smartResourceDrops$getLevel();

    @Inject(
            method = "removeBlock(Lnet/minecraft/core/BlockPos;Z)Z",
            at = @At("RETURN"),
            remap = false)
    private void smartdrops$afterSuccessfulRemoval(
            final BlockPos pos,
            final boolean canHarvest,
            final CallbackInfoReturnable<Boolean> callback
    ) {
        final ServerLevel level = smartResourceDrops$getLevel();
        if (callback.getReturnValueZ() && PlacementTracker.isMarked(level, pos)) {
            RecentRemovalCache.record(level, pos);
            PlacementTracker.remove(level, pos);
        }
    }
}
