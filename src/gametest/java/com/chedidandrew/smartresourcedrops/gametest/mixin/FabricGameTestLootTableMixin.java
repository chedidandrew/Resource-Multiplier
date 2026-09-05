package com.chedidandrew.smartresourcedrops.gametest.mixin;

import com.chedidandrew.smartresourcedrops.gametest.fixture.GameTestBlockLootFixtures;
import com.chedidandrew.smartresourcedrops.gametest.fixture.GameTestEntityFixtures;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;

import java.util.function.Consumer;

/** Final-output fixture adapter for the target Fabric API's loot-table consumer boundary. */
@Mixin(LootTable.class)
abstract class FabricGameTestLootTableMixin {
    @WrapMethod(method = "getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;JLjava/util/function/Consumer;)V")
    private void smartResourceDrops$wrapConsumerOutput(
            final LootParams params,
            final long seed,
            final Consumer<ItemStack> downstream,
            final Operation<Void> original
    ) {
        original.call(params, seed, (Consumer<ItemStack>) stack -> {
            final ObjectArrayList<ItemStack> blockDrops = new ObjectArrayList<>();
            blockDrops.add(stack);
            GameTestBlockLootFixtures.appendPathologicalDropsIfArmed(stack.is(Items.DIRT), blockDrops);
            for (ItemStack finalStack : blockDrops) {
                GameTestEntityFixtures.acceptFabricFinalDrop(finalStack, params, downstream);
            }
        });
    }

    @WrapMethod(method = "getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;")
    private ObjectArrayList<ItemStack> smartResourceDrops$wrapListOutput(
            final LootParams params,
            final Operation<ObjectArrayList<ItemStack>> original
    ) {
        final ObjectArrayList<ItemStack> generated = original.call(params);
        GameTestBlockLootFixtures.appendPathologicalDropsIfArmed(
                generated.stream().anyMatch(stack -> stack.is(Items.DIRT)),
                generated);
        return generated;
    }
}
