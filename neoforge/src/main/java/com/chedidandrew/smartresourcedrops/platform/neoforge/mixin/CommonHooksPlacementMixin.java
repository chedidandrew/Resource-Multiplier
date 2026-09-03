package com.chedidandrew.smartresourcedrops.platform.neoforge.mixin;

import com.chedidandrew.smartresourcedrops.core.provenance.PlacementCapture;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraftforge.common.ForgeHooks;
import org.spongepowered.asm.mixin.Mixin;

/** Keeps the placement transaction open through NeoForge's cancellable snapshot rollback. */
@Mixin(value = ForgeHooks.class, remap = false)
abstract class CommonHooksPlacementMixin {
    @WrapMethod(method = "onPlaceItemIntoWorld")
    private static InteractionResult smartdrops$captureNeoForgePlacement(
            final UseOnContext context,
            final Operation<InteractionResult> original
    ) {
        PlacementCapture.beginBoundary(context.getLevel());
        boolean succeeded = false;
        try {
            final InteractionResult result = original.call(context);
            succeeded = result.consumesAction();
            return result;
        } finally {
            PlacementCapture.end(succeeded);
        }
    }
}
