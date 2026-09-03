package com.chedidandrew.smartresourcedrops.mixin;

import com.chedidandrew.smartresourcedrops.core.shearing.ShearingActionContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Buffers direct ItemEntity output used by classic 1.20.1 shearable entities. */
@Mixin(Entity.class)
abstract class EntityShearingDropMixin {
    @WrapOperation(
            method = "spawnAtLocation(Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean smartResourceDrops$bufferClassicShearingSpawn(
            Level level,
            Entity spawned,
            Operation<Boolean> original
    ) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof LivingEntity target)
                || !(level instanceof ServerLevel serverLevel)
                || !(spawned instanceof ItemEntity itemEntity)) {
            return original.call(level, spawned);
        }
        return ShearingActionContext.captureSpawn(
                target,
                serverLevel,
                itemEntity,
                () -> original.call(level, spawned));
    }
}
