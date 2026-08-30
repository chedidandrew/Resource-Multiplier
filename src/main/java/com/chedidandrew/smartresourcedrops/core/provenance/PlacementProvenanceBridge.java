package com.chedidandrew.smartresourcedrops.core.provenance;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/** Internal bridge to the persistent placed-block tracker. */
public final class PlacementProvenanceBridge {
    private PlacementProvenanceBridge() {
    }

    public static boolean isPlaced(final Level level, final BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        return com.chedidandrew.smartresourcedrops.provenance.PlacementTracker.isPlaced(serverLevel, pos);
    }

    /** Returns only the persistent marker, excluding the short remove-before-drop cache. */
    public static boolean isPersistentlyPlaced(final Level level, final BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        return com.chedidandrew.smartresourcedrops.provenance.PlacementTracker.isMarked(serverLevel, pos);
    }

    public static void mark(final Level level, final BlockPos pos) {
        if (level instanceof ServerLevel serverLevel && !level.getBlockState(pos).isAir()) {
            com.chedidandrew.smartresourcedrops.provenance.PlacementTracker.mark(serverLevel, pos);
        }
    }

    public static void unmark(final Level level, final BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            com.chedidandrew.smartresourcedrops.provenance.PlacementTracker.remove(serverLevel, pos);
        }
    }
}
