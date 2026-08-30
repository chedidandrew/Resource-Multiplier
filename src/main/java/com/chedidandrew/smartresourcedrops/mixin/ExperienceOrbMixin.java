package com.chedidandrew.smartresourcedrops.mixin;

import com.chedidandrew.smartresourcedrops.core.DropContext;
import com.chedidandrew.smartresourcedrops.core.entity.EntityDeathContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ExperienceOrb.class)
abstract class ExperienceOrbMixin {
    @ModifyVariable(
            method = "award(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0)
    private static int smartResourceDrops$multiplyExperience(int amount, ServerLevel level, Vec3 pos) {
        if (EntityDeathContext.consumeExpectedMobExperienceAward(level, pos, amount)) {
            return amount;
        }
        return DropContext.multiplyExperience(amount, level, pos);
    }
}
