package com.chedidandrew.smartresourcedrops.mixin;

import com.chedidandrew.smartresourcedrops.core.shearing.ShearingActionContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.dispenser.ShearsDispenseItemBehavior;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Wraps only the dispenser's entity shear call; beehives and leash removal never enter this scope. */
@Mixin(ShearsDispenseItemBehavior.class)
abstract class ShearsDispenseItemBehaviorMixin {
    @WrapOperation(
            method = "execute(Lnet/minecraft/core/BlockSource;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/dispenser/ShearsDispenseItemBehavior;tryShearLivingEntity(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean smartResourceDrops$removeLeashBeforeLegacyEntityShearing(
            ServerLevel level,
            BlockPos pos,
            Operation<Boolean> original
    ) {
        // Vanilla gained this ordering after 1.20.1. Port it so the same
        // dispenser action cannot both remove a leash and shear its target.
        for (Mob mob : level.getEntitiesOfClass(
                Mob.class,
                new AABB(pos),
                mob -> EntitySelector.NO_SPECTATORS.test(mob) && mob.isLeashed())) {
            mob.dropLeash(true, true);
            level.gameEvent(null, GameEvent.SHEAR, pos);
            return true;
        }
        return original.call(level, pos);
    }

    @WrapOperation(
            method = "tryShearLivingEntity(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Shearable;shear(Lnet/minecraft/sounds/SoundSource;)V"),
            require = 1,
            expect = 1)
    private static void smartResourceDrops$scopeVanillaDispenserShearing(
            Shearable shearable,
            SoundSource soundSource,
            Operation<Void> original
    ) {
        if (!(shearable instanceof LivingEntity target)) {
            original.call(shearable, soundSource);
            return;
        }
        ServerLevel level = (ServerLevel) target.level();

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
