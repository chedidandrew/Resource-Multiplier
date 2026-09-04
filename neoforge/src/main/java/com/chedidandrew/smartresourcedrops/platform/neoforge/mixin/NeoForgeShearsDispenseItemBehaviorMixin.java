package com.chedidandrew.smartresourcedrops.platform.neoforge.mixin;

import java.util.List;

import com.chedidandrew.smartresourcedrops.core.shearing.ShearingActionContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.ShearsDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.IShearable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Scopes NeoForge's IShearable dispenser path without touching beehives or leash removal. */
@Mixin(ShearsDispenseItemBehavior.class)
abstract class NeoForgeShearsDispenseItemBehaviorMixin {
    @WrapOperation(
            method = "tryShearEntity(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/common/IShearable;onSheared(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Ljava/util/List;"),
            require = 1,
            expect = 1)
    private static List<ItemStack> smartdrops$scopeNeoForgeDispenserShearing(
            final IShearable shearable,
            final Player player,
            final ItemStack tool,
            final Level level,
            final BlockPos pos,
            final Operation<List<ItemStack>> original
    ) {
        if (!(shearable instanceof LivingEntity target) || !(level instanceof ServerLevel serverLevel)) {
            return original.call(shearable, player, tool, level, pos);
        }
        try (ShearingActionContext.Scope scope = ShearingActionContext.beginDispenser(target, serverLevel)) {
            try {
                final List<ItemStack> result = original.call(shearable, player, tool, level, pos);
                scope.complete();
                return result;
            } catch (RuntimeException | Error exception) {
                scope.abort();
                throw exception;
            }
        }
    }
}
