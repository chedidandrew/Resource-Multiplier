package com.chedidandrew.smartresourcedrops.legacy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;

public final class PlacedBlockDataTest {
    @Test
    public void placedAndInFlightFallingProvenanceSurvivesWorldSaveRoundTrip() {
        PlacedBlockData original = new PlacedBlockData();
        original.mark(-12345, 255, 23456);
        UUID fallingEntity = UUID.randomUUID();
        long origin = PlacedBlockData.pack(-20, 200, 40);
        long inFlight = PlacedBlockData.pack(-20, 170, 40);
        assertTrue(original.beginFalling(fallingEntity, origin));
        original.updateFalling(fallingEntity, inFlight);

        NBTTagCompound serialized = new NBTTagCompound();
        original.writeToNBT(serialized);
        PlacedBlockData restored = new PlacedBlockData();
        restored.readFromNBT(serialized);

        assertTrue(restored.contains(-12345, 255, 23456));
        assertTrue(restored.hasFalling(fallingEntity));
        assertEquals(inFlight, restored.fallingPosition(fallingEntity));
        restored.finishFalling(fallingEntity);
        assertFalse(restored.hasFalling(fallingEntity));
    }

    @Test
    public void packedCoordinatesKeepSignedHorizontalValuesDistinct() {
        assertTrue(PlacedBlockData.pack(-1, 64, 0) != PlacedBlockData.pack(1, 64, 0));
        assertTrue(PlacedBlockData.pack(0, 64, -1) != PlacedBlockData.pack(0, 64, 1));
    }
}
