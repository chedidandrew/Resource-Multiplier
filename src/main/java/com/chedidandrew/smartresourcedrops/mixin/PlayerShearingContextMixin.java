package com.chedidandrew.smartresourcedrops.mixin;

import com.chedidandrew.smartresourcedrops.core.shearing.ShearingActionContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;

/** Scopes both entity-owned and custom-item manual shearing interaction paths. */
@Mixin(Player.class)
abstract class PlayerShearingContextMixin {
    @WrapMethod(
            method = "interactOn(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;")
    private InteractionResult smartResourceDrops$scopeManualShearing(
            Entity entity,
            InteractionHand hand,
            Operation<InteractionResult> original
    ) {
        Player self = (Player) (Object) this;
        if (!(self.level() instanceof ServerLevel level)
                || !(entity instanceof LivingEntity target)
                || !(entity instanceof Shearable)) {
            return original.call(entity, hand);
        }

        try (ShearingActionContext.Scope scope =
                     ShearingActionContext.beginManual(target, level, self)) {
            try {
                InteractionResult result = original.call(entity, hand);
                scope.complete();
                return result;
            } catch (RuntimeException | Error exception) {
                scope.abort();
                throw exception;
            }
        }
    }
}
