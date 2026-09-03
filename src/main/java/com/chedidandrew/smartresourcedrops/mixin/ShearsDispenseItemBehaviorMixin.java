package com.chedidandrew.smartresourcedrops.mixin;

import com.chedidandrew.smartresourcedrops.core.shearing.ShearingActionContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.dispenser.ShearsDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Shearable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Wraps only the dispenser's entity shear call; beehives and leash removal never enter this scope. */
@Mixin(ShearsDispenseItemBehavior.class)
abstract class ShearsDispenseItemBehaviorMixin {
    @WrapOperation(
            method = "tryShearLivingEntity(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Shearable;shear(Lnet/minecraft/sounds/SoundSource;)V"),
            // NeoForge replaces this call with IShearable; its loader-specific mixin handles that path.
            require = 0,
            expect = 1)
    private static void smartResourceDrops$scopeVanillaDispenserShearing(
            Shearable shearable,
            SoundSource soundSource,
            Operation<Void> original
    ) {
        if (!(shearable instanceof LivingEntity target)
                || !(target.level() instanceof ServerLevel level)) {
            original.call(shearable, soundSource);
            return;
        }

        try (ShearingActionContext.Scope scope =
                     ShearingActionContext.beginDispenser(target, level)) {
            try {
                original.call(shearable, soundSource);
                scope.complete();
            } catch (RuntimeException | Error exception) {
                scope.abort();
                throw exception;
            }
        }
    }
}
