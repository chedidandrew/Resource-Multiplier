package com.chedidandrew.smartresourcedrops.core.util;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Emits multiplied loot in legal stack sizes. */
public final class StackEmitter {
    private StackEmitter() {
    }

    public static void emit(final Level level, final BlockPos pos, final List<ItemStack> source, final int copies) {
        for (ItemStack stack : StackConsolidator.multiply(source, copies)) {
            Block.popResource(level, pos, stack);
        }
    }
}
