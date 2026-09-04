package com.chedidandrew.smartresourcedrops.platform.neoforge;

import java.util.function.Supplier;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.provenance.PlacedBlockData;
import com.chedidandrew.smartresourcedrops.provenance.PlacementTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.level.ChunkDataEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** NeoForge persistent chunk data attachment for placed-block provenance. */
final class NeoForgePlacementStorage implements PlacementTracker.Storage {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, SmartResourceDrops.MOD_ID);
    private static final Supplier<AttachmentType<PlacedBlockData>> PLACED_BLOCKS = ATTACHMENTS.register(
            "placed_blocks",
            () -> AttachmentType.builder(PlacedBlockData::new)
                    .serialize(PlacedBlockData.MAP_CODEC, data -> !data.isEmpty())
                    .build());

    static void register(final IEventBus modBus) {
        ATTACHMENTS.register(modBus);
        NeoForge.EVENT_BUS.addListener(
                EventPriority.HIGHEST,
                ChunkDataEvent.Load.class,
                NeoForgePlacementStorage::onChunkDataLoad);
    }

    private static void onChunkDataLoad(final ChunkDataEvent.Load event) {
        final Object serializedData = event.getData();
        if (!(serializedData instanceof LegacyFabricProvenanceMigration.Carrier carrier)) {
            return;
        }
        final PlacedBlockData legacyData = carrier.smart_resource_drops$getLegacyFabricProvenance();
        if (legacyData == null || legacyData.isEmpty()) {
            return;
        }

        final AttachmentType<PlacedBlockData> attachmentType = PLACED_BLOCKS.get();
        if (event.getChunk().hasData(attachmentType)) {
            return;
        }

        event.getChunk().setData(attachmentType, legacyData);
        event.getChunk().markUnsaved();
        SmartResourceDrops.LOGGER.debug(
                "Imported Fabric placement provenance for chunk {}",
                event.getChunk().getPos());
    }

    @Override
    public boolean contains(
            final ServerLevel level,
            final BlockPos pos,
            final int packedPosition
    ) {
        final LevelChunk chunk = level.getChunkAt(pos);
        final PlacedBlockData data = chunk.getExistingData(PLACED_BLOCKS).orElse(null);
        return data != null && data.contains(packedPosition);
    }

    @Override
    public void mark(final ServerLevel level, final BlockPos pos, final int packedPosition) {
        final LevelChunk chunk = level.getChunkAt(pos);
        final PlacedBlockData data = chunk.getData(PLACED_BLOCKS);
        if (data.add(packedPosition)) {
            chunk.markUnsaved();
        }
    }

    @Override
    public boolean remove(final ServerLevel level, final BlockPos pos, final int packedPosition) {
        final LevelChunk chunk = level.getChunkAt(pos);
        final PlacedBlockData data = chunk.getExistingData(PLACED_BLOCKS).orElse(null);
        if (data == null || !data.remove(packedPosition)) {
            return false;
        }
        if (data.isEmpty()) {
            chunk.removeData(PLACED_BLOCKS);
        }
        chunk.markUnsaved();
        return true;
    }
}
