package com.chedidandrew.smartresourcedrops.platform.neoforge;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.provenance.PlacedBlockData;
import com.chedidandrew.smartresourcedrops.provenance.PlacementTracker;
import com.mojang.serialization.DataResult;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/** Persistent LevelChunk capability for placed-block provenance on legacy Forge. */
final class NeoForgePlacementStorage implements PlacementTracker.Storage {
    private static final Capability<PlacedBlockData> PLACED_BLOCKS = CapabilityManager.get(
            new CapabilityToken<>() {
            });

    static void register(final IEventBus modBus) {
        modBus.addListener((RegisterCapabilitiesEvent event) ->
                event.register(PlacedBlockData.class));
        MinecraftForge.EVENT_BUS.addGenericListener(
                LevelChunk.class,
                NeoForgePlacementStorage::attachCapability);
    }

    private static void attachCapability(final AttachCapabilitiesEvent<LevelChunk> event) {
        final Provider provider = new Provider();
        event.addCapability(SmartResourceDrops.id("placed_blocks"), provider);
        event.addListener(provider::invalidate);
    }

    @Override
    public boolean contains(
            final ServerLevel level,
            final BlockPos pos,
            final int packedPosition
    ) {
        return data(level.getChunkAt(pos)).contains(packedPosition);
    }

    @Override
    public void mark(final ServerLevel level, final BlockPos pos, final int packedPosition) {
        final LevelChunk chunk = level.getChunkAt(pos);
        if (data(chunk).add(packedPosition)) {
            chunk.setUnsaved(true);
        }
    }

    @Override
    public boolean remove(final ServerLevel level, final BlockPos pos, final int packedPosition) {
        final LevelChunk chunk = level.getChunkAt(pos);
        if (!data(chunk).remove(packedPosition)) {
            return false;
        }
        chunk.setUnsaved(true);
        return true;
    }

    private static PlacedBlockData data(final LevelChunk chunk) {
        return chunk.getCapability(PLACED_BLOCKS).orElseThrow(() ->
                new IllegalStateException("Placed-block capability missing from chunk " + chunk.getPos()));
    }

    private static final class Provider implements ICapabilitySerializable<CompoundTag> {
        private final PlacedBlockData data = new PlacedBlockData();
        private final LazyOptional<PlacedBlockData> optional = LazyOptional.of(() -> data);

        @Override
        public <T> LazyOptional<T> getCapability(
                final Capability<T> capability,
                final Direction side
        ) {
            return capability == PLACED_BLOCKS ? optional.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            final DataResult<net.minecraft.nbt.Tag> encoded = PlacedBlockData.MAP_CODEC
                    .codec()
                    .encodeStart(NbtOps.INSTANCE, data);
            final net.minecraft.nbt.Tag tag = encoded.result().orElseGet(CompoundTag::new);
            return tag instanceof CompoundTag compound ? compound : new CompoundTag();
        }

        @Override
        public void deserializeNBT(final CompoundTag tag) {
            PlacedBlockData.MAP_CODEC.codec().parse(NbtOps.INSTANCE, tag).result()
                    .ifPresent(data::replaceWith);
        }

        private void invalidate() {
            optional.invalidate();
        }
    }
}
