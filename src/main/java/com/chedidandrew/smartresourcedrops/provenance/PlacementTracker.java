package com.chedidandrew.smartresourcedrops.provenance;


import com.chedidandrew.smartresourcedrops.core.provenance.RecentRemovalCache;
import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.core.PackedBlockPosition;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

public final class PlacementTracker {
    private static final AttachmentType<PlacedBlockData> PLACED_BLOCKS = AttachmentRegistry.create(
            SmartResourceDrops.id("placed_blocks"),
            builder -> builder.initializer(PlacedBlockData::new).persistent(PlacedBlockData.CODEC));

    private PlacementTracker() {
    }

    public static void bootstrap() {
        // Forces attachment registration during mod initialization.
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
        LevelChunk chunk = level.getChunkAt(pos);
        PlacedBlockData data = ((AttachmentTarget) (Object) chunk).getAttached(PLACED_BLOCKS);
        return data != null && data.contains(pack(pos));
    }

    public static void mark(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        AttachmentTarget target = (AttachmentTarget) (Object) chunk;
        PlacedBlockData data = target.getAttachedOrCreate(PLACED_BLOCKS);
        if (data.add(pack(pos))) {
            chunk.markUnsaved();
        }
    }

    public static boolean remove(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        AttachmentTarget target = (AttachmentTarget) (Object) chunk;
        PlacedBlockData data = target.getAttached(PLACED_BLOCKS);
        if (data == null || !data.remove(pack(pos))) {
            return false;
        }

        if (data.isEmpty()) {
            target.removeAttached(PLACED_BLOCKS);
        }
        chunk.markUnsaved();
        return true;
    }

    private static int pack(BlockPos pos) {
        return PackedBlockPosition.pack(pos.getX(), pos.getY(), pos.getZ());
    }
}
