package com.chedidandrew.smartresourcedrops.provenance;

import java.util.Objects;

import com.chedidandrew.smartresourcedrops.core.PackedBlockPosition;
import com.chedidandrew.smartresourcedrops.core.provenance.RecentRemovalCache;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class PlacementTracker {
    private static volatile Storage storage;

    private PlacementTracker() {
    }

    public static void installStorage(final Storage installedStorage) {
        storage = Objects.requireNonNull(installedStorage, "installedStorage");
    }

    public static void bootstrap() {
        requireStorage();
    }

    public static boolean isPlaced(ServerLevel level, BlockPos pos) {
        if (RecentRemovalCache.wasRecentlyRemoved(level, pos)) {
            return true;
        }
        return isMarked(level, pos);
    }

    /** Read-only provenance lookup used by diagnostics. */
    public static boolean peekPlaced(ServerLevel level, BlockPos pos) {
        if (RecentRemovalCache.peekWasRecentlyRemoved(level, pos)) {
            return true;
        }
        return isMarked(level, pos);
    }

    public static boolean isMarked(ServerLevel level, BlockPos pos) {
        return requireStorage().contains(level, pos, pack(pos));
    }

    public static void mark(ServerLevel level, BlockPos pos) {
        requireStorage().mark(level, pos, pack(pos));
    }

    public static boolean remove(ServerLevel level, BlockPos pos) {
        return requireStorage().remove(level, pos, pack(pos));
    }

    private static int pack(BlockPos pos) {
        return PackedBlockPosition.pack(pos.getX(), pos.getY(), pos.getZ());
    }

    private static Storage requireStorage() {
        final Storage current = storage;
        if (current == null) {
            throw new IllegalStateException("Placed-block storage has not been installed by the active loader");
        }
        return current;
    }

    /** Loader adapter for one persistent placed-block data attachment per chunk. */
    public interface Storage {
        boolean contains(ServerLevel level, BlockPos pos, int packedPosition);

        void mark(ServerLevel level, BlockPos pos, int packedPosition);

        boolean remove(ServerLevel level, BlockPos pos, int packedPosition);
    }
}
