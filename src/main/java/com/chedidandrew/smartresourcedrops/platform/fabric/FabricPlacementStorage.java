package com.chedidandrew.smartresourcedrops.platform.fabric;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.provenance.PlacedBlockData;
import com.chedidandrew.smartresourcedrops.provenance.PlacementTracker;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

/** Fabric persistent chunk-attachment implementation for placed-block provenance. */
final class FabricPlacementStorage implements PlacementTracker.Storage {
    private static final AttachmentType<PlacedBlockData> PLACED_BLOCKS = AttachmentRegistry.create(
            SmartResourceDrops.id("placed_blocks"),
            builder -> builder.initializer(PlacedBlockData::new).persistent(PlacedBlockData.CODEC));

    @Override
    public boolean contains(
            final ServerLevel level,
            final BlockPos pos,
            final int packedPosition
    ) {
        final LevelChunk chunk = level.getChunkAt(pos);
        final PlacedBlockData data = ((AttachmentTarget) (Object) chunk).getAttached(PLACED_BLOCKS);
        return data != null && data.contains(packedPosition);
    }

    @Override
    public void mark(final ServerLevel level, final BlockPos pos, final int packedPosition) {
        final LevelChunk chunk = level.getChunkAt(pos);
        final AttachmentTarget target = (AttachmentTarget) (Object) chunk;
        final PlacedBlockData data = target.getAttachedOrCreate(PLACED_BLOCKS);
        if (data.add(packedPosition)) {
            chunk.setUnsaved(true);
        }
    }

    @Override
    public boolean remove(final ServerLevel level, final BlockPos pos, final int packedPosition) {
        final LevelChunk chunk = level.getChunkAt(pos);
        final AttachmentTarget target = (AttachmentTarget) (Object) chunk;
        final PlacedBlockData data = target.getAttached(PLACED_BLOCKS);
        if (data == null || !data.remove(packedPosition)) {
            return false;
        }
        if (data.isEmpty()) {
            target.removeAttached(PLACED_BLOCKS);
        }
        chunk.setUnsaved(true);
        return true;
    }
}
