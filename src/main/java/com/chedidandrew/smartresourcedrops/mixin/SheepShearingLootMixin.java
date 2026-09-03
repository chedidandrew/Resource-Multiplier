package com.chedidandrew.smartresourcedrops.mixin;

import com.chedidandrew.smartresourcedrops.core.shearing.ShearingActionContext;
import com.chedidandrew.smartresourcedrops.core.shearing.ShearingRuleTrace;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Multiplies only Sheep's vanilla final wool emissions on Minecraft 1.21.1. */
@Mixin(Sheep.class)
abstract class SheepShearingLootMixin {
    @WrapOperation(
            method = "shear(Lnet/minecraft/sounds/SoundSource;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Sheep;spawnAtLocation(Lnet/minecraft/world/level/ItemLike;I)Lnet/minecraft/world/entity/item/ItemEntity;"),
            require = 1,
            expect = 1)
    private ItemEntity smartResourceDrops$multiplyVanillaWoolOutput(
            final Sheep sheep,
            final ItemLike wool,
            final int verticalOffset,
            final Operation<ItemEntity> original
    ) {
        final ShearingRuleTrace trace = ShearingActionContext.activeTrace(sheep);
        final int multiplier = trace == null ? 1 : trace.appliedMultiplier();
        ItemEntity last = null;
        for (int copy = 0; copy < multiplier; copy++) {
            last = original.call(sheep, wool, verticalOffset);
        }
        return last;
    }
}
