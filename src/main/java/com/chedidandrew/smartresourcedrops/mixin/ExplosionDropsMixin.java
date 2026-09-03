package com.chedidandrew.smartresourcedrops.mixin;

import com.chedidandrew.smartresourcedrops.core.DropContext;
import com.chedidandrew.smartresourcedrops.core.DropSource;
import com.chedidandrew.smartresourcedrops.provenance.PlacementTracker;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/** Scopes the classic 1.20.1 explosion loop around each block's final loot generation. */
@Mixin(Explosion.class)
abstract class ExplosionDropsMixin {
    @WrapOperation(
            method = "finalizeExplosion(Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getDrops(Lnet/minecraft/world/level/storage/loot/LootParams$Builder;)Ljava/util/List;"))
    private List<ItemStack> smartResourceDrops$scopeExplosionLoot(
            BlockState state,
            LootParams.Builder params,
            Operation<List<ItemStack>> original
    ) {
        ServerLevel level = params.getLevel();
        Vec3 origin = params.getOptionalParameter(LootContextParams.ORIGIN);
        if (origin == null) {
            return original.call(state, params);
        }
        BlockPos pos = BlockPos.containing(origin);
        BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        Entity actor = ((Explosion) (Object) this).getIndirectSourceEntity();
        DropContext.beginExplosion(level, pos, state, blockEntity, actor);
        try {
            List<ItemStack> drops = original.call(state, params);
            PlacementTracker.remove(level, pos);
            return drops;
        } finally {
            DropContext.endExpected(DropSource.EXPLOSION);
        }
    }
}
