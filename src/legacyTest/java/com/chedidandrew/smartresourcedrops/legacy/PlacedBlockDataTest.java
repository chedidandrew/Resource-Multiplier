package com.chedidandrew.smartresourcedrops.legacy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import org.junit.Test;

public final class PlacedBlockDataTest {
    @Test
    public void trackedPositionsRoundTripThroughWorldData() {
        BlockPos first = new BlockPos(12, 64, -7);
        BlockPos second = new BlockPos(-30, 4, 90);
        PlacedBlockData original = new PlacedBlockData();
        original.mark(first);
        original.mark(second);

        PlacedBlockData restored = new PlacedBlockData();
        restored.readFromNBT(original.writeToNBT(new NBTTagCompound()));

        assertTrue(restored.contains(first));
        assertTrue(restored.contains(second));
        restored.unmark(first);
        assertFalse(restored.contains(first));
        assertTrue(restored.contains(second));
    }
}
