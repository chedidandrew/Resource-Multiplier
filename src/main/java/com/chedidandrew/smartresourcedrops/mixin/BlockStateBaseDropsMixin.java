package com.chedidandrew.smartresourcedrops.mixin;

import com.chedidandrew.smartresourcedrops.core.DropContext;
import com.chedidandrew.smartresourcedrops.core.DropSource;
import com.chedidandrew.smartresourcedrops.provenance.PlacementTracker;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.function.BiConsumer;

@Mixin(BlockBehaviour.BlockStateBase.class)
abstract class BlockStateBaseDropsMixin {
    @ModifyReturnValue(method = "getDrops", at = @At("RETURN"))
    private List<ItemStack> smartResourceDrops$multiplyLoot(
            List<ItemStack> original,
            LootParams.Builder params
    ) {
        return DropContext.applyDrops(original, params, (BlockState) (Object) this);
    }

    @WrapMethod(method = "onExplosionHit")
    private void smartResourceDrops$wrapExplosion(
            Level level,
            BlockPos pos,
            Explosion explosion,
            BiConsumer<ItemStack, BlockPos> onHit,
            Operation<Void> original
    ) {
        if (!(level instanceof ServerLevel serverLevel)) {
            original.call(level, pos, explosion, onHit);
            return;
        }
        BlockState state = (BlockState) (Object) this;
        BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        Entity actor = explosion.getIndirectSourceEntity();
        DropContext.beginExplosion(serverLevel, pos, state, blockEntity, actor);
        try {
            original.call(level, pos, explosion, onHit);
        } finally {
            BlockState current = level.getBlockState(pos);
            if (current.isAir() || !current.is(state.getBlock())) {
                PlacementTracker.remove(serverLevel, pos);
            }
            DropContext.endExpected(DropSource.EXPLOSION);
        }
    }
}
