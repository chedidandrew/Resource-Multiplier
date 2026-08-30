package com.chedidandrew.smartresourcedrops.mixin;

import com.chedidandrew.smartresourcedrops.core.DropContext;
import com.chedidandrew.smartresourcedrops.core.DropSource;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Block.class)
abstract class BlockDropResourcesMixin {
    @WrapMethod(
            method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V")
    private static void smartResourceDrops$wrapSimpleDrop(
            BlockState state,
            Level level,
            BlockPos pos,
            Operation<Void> original
    ) {
        DropContext.beginAutomation(level, pos, state, null, null);
        try {
            original.call(state, level, pos);
        } finally {
            DropContext.endAutomation();
        }
    }

    @WrapMethod(
            method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)V")
    private static void smartResourceDrops$wrapBlockEntityDrop(
            BlockState state,
            LevelAccessor level,
            BlockPos pos,
            BlockEntity blockEntity,
            Operation<Void> original
    ) {
        DropContext.beginAutomation(level, pos, state, blockEntity, null);
        try {
            original.call(state, level, pos, blockEntity);
        } finally {
            DropContext.endAutomation();
        }
    }

    @WrapMethod(
            method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V")
    private static void smartResourceDrops$wrapFullDrop(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockEntity blockEntity,
            Entity breaker,
            ItemStack tool,
            Operation<Void> original
    ) {
        final boolean realPlayer = level instanceof ServerLevel
                && breaker instanceof Player
                && !(breaker instanceof FakePlayer);
        if (realPlayer) {
            DropContext.beginPlayer((ServerLevel) level, (Player) breaker, pos, state, blockEntity);
        } else {
            DropContext.beginAutomation(level, pos, state, blockEntity, breaker);
        }
        try {
            original.call(state, level, pos, blockEntity, breaker, tool);
        } finally {
            if (realPlayer) {
                DropContext.endExpected(DropSource.PLAYER);
            } else {
                DropContext.endAutomation();
            }
        }
    }
}
