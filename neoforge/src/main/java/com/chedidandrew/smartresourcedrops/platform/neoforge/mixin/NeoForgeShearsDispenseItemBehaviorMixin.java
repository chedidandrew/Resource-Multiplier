package com.chedidandrew.smartresourcedrops.platform.neoforge.mixin;

import java.util.List;

import com.chedidandrew.smartresourcedrops.config.ConfigManager;
import com.chedidandrew.smartresourcedrops.core.shearing.ShearingOutputBudget;
import com.chedidandrew.smartresourcedrops.core.shearing.ShearingRuleResolver;
import com.chedidandrew.smartresourcedrops.core.shearing.ShearingRuleTrace;
import com.chedidandrew.smartresourcedrops.core.shearing.ShearingSource;
import com.chedidandrew.smartresourcedrops.platform.PlatformPlayerSupport;
import com.chedidandrew.smartresourcedrops.platform.neoforge.NeoForgeShearingRoute;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.IForgeShearable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Multiplies legacy Forge IForgeShearable-only manual output before it is spawned. */
@Mixin(ShearsItem.class)
abstract class NeoForgeShearsDispenseItemBehaviorMixin {
    @WrapOperation(
            method = "interactLivingEntity(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/common/IForgeShearable;onSheared(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;I)Ljava/util/List;",
                    remap = false),
            require = 1,
            expect = 1)
    private List<ItemStack> smartdrops$multiplyForgeManualShearing(
            final IForgeShearable shearable,
            final Player player,
            final ItemStack tool,
            final Level level,
            final BlockPos pos,
            final int fortune,
            final Operation<List<ItemStack>> original
    ) {
        final List<ItemStack> vanilla = original.call(shearable, player, tool, level, pos, fortune);
        if (!NeoForgeShearingRoute.ownsForgeOnlyPath(shearable)
                || !(shearable instanceof LivingEntity target)
                || !(level instanceof ServerLevel)
                || !(player instanceof ServerPlayer)
                || PlatformPlayerSupport.isFakePlayer(player)) {
            return vanilla;
        }
        final ShearingRuleTrace trace = ShearingRuleResolver.trace(
                ConfigManager.get(),
                target.getType(),
                ShearingSource.MANUAL_PLAYER);
        if (!trace.multiplicationEligible() || trace.appliedMultiplier() == 1) {
            return vanilla;
        }
        final ShearingOutputBudget.Result planned = ShearingOutputBudget.plan(
                List.of(vanilla),
                trace.appliedMultiplier());
        return planned.fits() ? planned.outputBatches().get(0) : vanilla;
    }
}
