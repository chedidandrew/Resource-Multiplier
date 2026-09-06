package com.chedidandrew.smartresourcedrops.legacy;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;

/** Persistent per-dimension provenance used by Natural Blocks Only mode. */
public final class PlacedBlockData extends WorldSavedData {
    private static final String DATA_NAME = "smart_resource_drops_placed_blocks";
    private static final int MAX_TRACKED_POSITIONS = 1000000;
    private final Set<Long> positions = new LinkedHashSet<Long>();
    private final Map<UUID, Long> fallingPositions = new LinkedHashMap<UUID, Long>();

    public PlacedBlockData() { super(DATA_NAME); }
    public PlacedBlockData(String name) { super(name); }

    public static PlacedBlockData get(World world) {
        MapStorage storage = world.perWorldStorage;
        PlacedBlockData data = (PlacedBlockData) storage.loadData(PlacedBlockData.class, DATA_NAME);
        if (data == null) {
            data = new PlacedBlockData();
            storage.setData(DATA_NAME, data);
        }
        return data;
    }

    public boolean contains(int x, int y, int z) { return positions.contains(Long.valueOf(pack(x, y, z))); }
    public void mark(int x, int y, int z) { markPacked(pack(x, y, z)); }
    public void unmark(int x, int y, int z) { unmarkPacked(pack(x, y, z)); }
    public void markPacked(long packed) {
        if (positions.size() < MAX_TRACKED_POSITIONS && positions.add(Long.valueOf(packed))) markDirty();
    }
    public void unmarkPacked(long packed) {
        if (positions.remove(Long.valueOf(packed))) markDirty();
    }

    public boolean beginFalling(UUID entityId, long packed) {
        if (entityId == null || (!fallingPositions.containsKey(entityId)
                && fallingPositions.size() >= MAX_TRACKED_POSITIONS)) return false;
        Long previous = fallingPositions.put(entityId, Long.valueOf(packed));
        if (previous == null || previous.longValue() != packed) markDirty();
        return true;
    }

    public boolean hasFalling(UUID entityId) {
        return entityId != null && fallingPositions.containsKey(entityId);
    }

    public long fallingPosition(UUID entityId) {
        Long value = fallingPositions.get(entityId);
        return value == null ? 0L : value.longValue();
    }

    public void updateFalling(UUID entityId, long packed) {
        if (entityId == null || !fallingPositions.containsKey(entityId)) return;
        Long previous = fallingPositions.put(entityId, Long.valueOf(packed));
        if (previous == null || previous.longValue() != packed) markDirty();
    }

    public void finishFalling(UUID entityId) {
        if (entityId != null && fallingPositions.remove(entityId) != null) markDirty();
    }

    public static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (long) (y & 0xFFF);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        positions.clear();
        fallingPositions.clear();
        NBTTagList list = nbt.getTagList("positions", 10);
        for (int index = 0; index < list.tagCount() && positions.size() < MAX_TRACKED_POSITIONS; index++) {
            positions.add(Long.valueOf(list.getCompoundTagAt(index).getLong("position")));
        }
        NBTTagList falling = nbt.getTagList("falling", 10);
        for (int index = 0; index < falling.tagCount()
                && fallingPositions.size() < MAX_TRACKED_POSITIONS; index++) {
            NBTTagCompound entry = falling.getCompoundTagAt(index);
            UUID entityId = new UUID(entry.getLong("uuidMost"), entry.getLong("uuidLeast"));
            fallingPositions.put(entityId, Long.valueOf(entry.getLong("position")));
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();
        for (Long packed : positions) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setLong("position", packed.longValue());
            list.appendTag(entry);
        }
        nbt.setTag("positions", list);
        NBTTagList falling = new NBTTagList();
        for (Map.Entry<UUID, Long> tracked : fallingPositions.entrySet()) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setLong("uuidMost", tracked.getKey().getMostSignificantBits());
            entry.setLong("uuidLeast", tracked.getKey().getLeastSignificantBits());
            entry.setLong("position", tracked.getValue().longValue());
            falling.appendTag(entry);
        }
        nbt.setTag("falling", falling);
    }
}
