package com.chedidandrew.smartresourcedrops.gametest.mixin;

import com.chedidandrew.smartresourcedrops.gametest.fixture.GameTestBlockLootFixtures;
import com.chedidandrew.smartresourcedrops.gametest.fixture.GameTestEntityFixtures;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Recreates the loader-final loot fixture stage on Fabric 1.20.1, whose
 * legacy loot API does not expose the modern final-drop event used by the
 * newer test suite.
 */
@Mixin(LootTable.class)
abstract class GameTestLootTableMixin {
    @ModifyReturnValue(
            method = "getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;",
            at = @At("RETURN"))
    private ObjectArrayList<ItemStack> smartResourceDrops$applyFinalBlockGameTestDrops(
            final ObjectArrayList<ItemStack> drops,
            final LootParams params
    ) {
        final BlockState blockState = params.getParamOrNull(LootContextParams.BLOCK_STATE);
        GameTestBlockLootFixtures.appendPathologicalDropsIfArmed(
                blockState != null && blockState.is(net.minecraft.world.level.block.Blocks.DIRT),
                drops);
        return drops;
    }

    @WrapMethod(method = "getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;JLjava/util/function/Consumer;)V")
    private void smartResourceDrops$applyFinalGameTestDrops(
            final LootParams params,
            final long seed,
            final Consumer<ItemStack> output,
            final Operation<Void> original
    ) {
        final List<ItemStack> drops = new ArrayList<>();
        original.call(params, seed, (Consumer<ItemStack>) drops::add);

        final Entity entity = params.getParamOrNull(LootContextParams.THIS_ENTITY);
        if (entity != null && entity.getType() == GameTestEntityFixtures.EXCEPTION) {
            GameTestEntityFixtures.throwIfArmedExceptionLoot();
        }
        GameTestEntityFixtures.applyFinalDropFixtures(
                entity != null && entity.getType() == GameTestEntityFixtures.COMPONENT_RICH,
                entity != null && entity.getType() == GameTestEntityFixtures.NESTED_OUTER,
                params,
                drops);

        final BlockState blockState = params.getParamOrNull(LootContextParams.BLOCK_STATE);
        GameTestBlockLootFixtures.appendPathologicalDropsIfArmed(
                blockState != null && blockState.is(net.minecraft.world.level.block.Blocks.DIRT),
                drops);
        drops.forEach(output);
    }
}
