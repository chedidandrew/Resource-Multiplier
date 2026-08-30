package com.chedidandrew.smartresourcedrops.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;

import com.chedidandrew.smartresourcedrops.core.provenance.PlacementCapture;

@Mixin(BlockItem.class)
abstract class BlockItemPlacementCaptureMixin {
    @WrapMethod(method = "place")
    private InteractionResult smartdrops$capturePlacement(
            final BlockPlaceContext context,
            final Operation<InteractionResult> original
    ) {
        PlacementCapture.begin(context.getLevel());
        boolean succeeded = false;
        try {
            final InteractionResult result = original.call(context);
            succeeded = result instanceof InteractionResult.Success;
            return result;
        } finally {
            PlacementCapture.end(succeeded);
        }
    }
}
