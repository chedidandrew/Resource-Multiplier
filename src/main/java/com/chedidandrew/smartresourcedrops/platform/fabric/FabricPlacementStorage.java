package com.chedidandrew.smartresourcedrops.platform.fabric;

import com.chedidandrew.smartresourcedrops.provenance.PlacedBlockData;
import com.chedidandrew.smartresourcedrops.provenance.PlacementTracker;
import com.mojang.serialization.DataResult;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/**
 * Persistent per-dimension provenance storage for Fabric 1.19.2.
 *
 * <p>The Fabric data-attachment API was introduced after this Minecraft
 * version. Vanilla SavedData supplies the same restart-safe semantics without
 * adding a third-party runtime dependency.</p>
 */
final class FabricPlacementStorage implements PlacementTracker.Storage {
    private static final String DATA_NAME = "smart_resource_drops_placed_blocks";

    @Override
    public boolean contains(
            final ServerLevel level,
            final BlockPos pos,
            final int packedPosition
    ) {
        final PlacedBlockData data = state(level).chunks.get(ChunkPos.asLong(pos));
        return data != null && data.contains(packedPosition);
    }

    @Override
    public void mark(final ServerLevel level, final BlockPos pos, final int packedPosition) {
        final State state = state(level);
        final PlacedBlockData data = state.chunks.computeIfAbsent(
                ChunkPos.asLong(pos),
                ignored -> new PlacedBlockData());
        if (data.add(packedPosition)) {
            state.setDirty();
        }
    }

    @Override
    public boolean remove(final ServerLevel level, final BlockPos pos, final int packedPosition) {
        final State state = state(level);
        final long chunk = ChunkPos.asLong(pos);
        final PlacedBlockData data = state.chunks.get(chunk);
        if (data == null || !data.remove(packedPosition)) {
            return false;
        }
        if (data.isEmpty()) {
            state.chunks.remove(chunk);
        }
        state.setDirty();
        return true;
    }

    private static State state(final ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(State::load, State::new, DATA_NAME);
    }

    private static final class State extends SavedData {
        private final Map<Long, PlacedBlockData> chunks = new HashMap<>();

        private static State load(final CompoundTag root) {
            final State state = new State();
            final ListTag entries = root.getList("chunks", Tag.TAG_COMPOUND);
            for (Tag rawEntry : entries) {
                if (!(rawEntry instanceof CompoundTag entry) || !entry.contains("data")) {
                    continue;
                }
                PlacedBlockData.CODEC.parse(NbtOps.INSTANCE, entry.get("data")).result()
                        .filter(data -> !data.isEmpty())
                        .ifPresent(data -> state.chunks.put(entry.getLong("chunk"), data));
            }
            return state;
        }

        @Override
        public CompoundTag save(final CompoundTag root) {
            final ListTag entries = new ListTag();
            chunks.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(chunkEntry -> {
                        final DataResult<Tag> encoded = PlacedBlockData.CODEC.encodeStart(
                                NbtOps.INSTANCE,
                                chunkEntry.getValue());
                        encoded.result().ifPresent(dataTag -> {
                            final CompoundTag entry = new CompoundTag();
                            entry.putLong("chunk", chunkEntry.getKey());
                            entry.put("data", dataTag);
                            entries.add(entry);
                        });
                    });
            root.put("chunks", entries);
            return root;
        }
    }
}
