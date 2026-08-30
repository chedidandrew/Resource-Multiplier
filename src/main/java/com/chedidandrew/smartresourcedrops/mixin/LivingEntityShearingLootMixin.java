package com.chedidandrew.smartresourcedrops.mixin;

import com.chedidandrew.smartresourcedrops.core.shearing.ShearingActionContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;

import java.util.function.BiConsumer;

/** Buffers only final stacks from the standard SHEARING loot-context helper. */
@Mixin(LivingEntity.class)
abstract class LivingEntityShearingLootMixin {
    @WrapMethod(
            method = "dropFromShearingLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/item/ItemInstance;Ljava/util/function/BiConsumer;)V")
    private void smartResourceDrops$bufferFinalShearingLoot(
            ServerLevel level,
            ResourceKey<LootTable> lootTable,
            ItemInstance tool,
            BiConsumer<ServerLevel, ItemStack> consumer,
            Operation<Void> original
    ) {
        LivingEntity self = (LivingEntity) (Object) this;
        BiConsumer<ServerLevel, ItemStack> buffered =
                ShearingActionContext.wrapLootConsumer(self, level, consumer);
        original.call(level, lootTable, tool, buffered);
    }
}
