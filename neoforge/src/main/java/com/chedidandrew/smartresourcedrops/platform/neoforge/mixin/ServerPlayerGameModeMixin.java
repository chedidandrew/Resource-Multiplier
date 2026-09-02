package com.chedidandrew.smartresourcedrops.platform.neoforge.mixin;

import com.chedidandrew.smartresourcedrops.core.provenance.RecentRemovalCache;
import com.chedidandrew.smartresourcedrops.provenance.PlacementTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Defensive successful-break cleanup for blocks with custom NeoForge removal hooks. */
@Mixin(ServerPlayerGameMode.class)
abstract class ServerPlayerGameModeMixin {
    @Shadow
    protected ServerLevel level;

    @Inject(
            method = "removeBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;ZLnet/minecraft/world/item/ItemStack;)Z",
            at = @At("RETURN"),
            remap = false)
    private void smartdrops$afterSuccessfulRemoval(
            final BlockPos pos,
            final BlockState state,
            final boolean canHarvest,
            final ItemStack toolStack,
            final CallbackInfoReturnable<Boolean> callback
    ) {
        if (callback.getReturnValueZ() && PlacementTracker.isMarked(level, pos)) {
            RecentRemovalCache.record(level, pos);
            PlacementTracker.remove(level, pos);
        }
    }
}
