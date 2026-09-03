package com.chedidandrew.smartresourcedrops.mixin;

import com.chedidandrew.smartresourcedrops.core.DropContext;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(BlockBehaviour.BlockStateBase.class)
abstract class BlockStateBaseDropsMixin {
    @ModifyReturnValue(method = "getDrops", at = @At("RETURN"))
    private List<ItemStack> smartResourceDrops$multiplyLoot(
            List<ItemStack> original,
            LootParams.Builder params
    ) {
        return DropContext.applyDrops(original, params, (BlockState) (Object) this);
    }
}
