package com.chedidandrew.smartresourcedrops.legacy;

import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

/** Persistent per-dimension provenance used by Natural Blocks Only mode. */
public final class PlacedBlockData extends WorldSavedData {
    private static final String DATA_NAME = "smart_resource_drops_placed_blocks";
    private static final int MAX_TRACKED_POSITIONS = 1_000_000;
    private final Set<Long> positions = new LinkedHashSet<Long>();

    public PlacedBlockData() {
        super(DATA_NAME);
    }

    public PlacedBlockData(String name) {
        super(name);
    }

    public static PlacedBlockData get(World world) {
        MapStorage storage = world.getPerWorldStorage();
        PlacedBlockData data = (PlacedBlockData) storage.getOrLoadData(PlacedBlockData.class, DATA_NAME);
        if (data == null) {
            data = new PlacedBlockData();
            storage.setData(DATA_NAME, data);
        }
        return data;
    }

    public boolean contains(BlockPos pos) {
        return positions.contains(Long.valueOf(pos.toLong()));
    }

    public void mark(BlockPos pos) {
        if (positions.size() >= MAX_TRACKED_POSITIONS) return;
        if (positions.add(Long.valueOf(pos.toLong()))) markDirty();
    }

    public void unmark(BlockPos pos) {
        if (positions.remove(Long.valueOf(pos.toLong()))) markDirty();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        positions.clear();
        NBTTagList list = nbt.getTagList("positions", 4);
        for (int index = 0; index < list.tagCount() && positions.size() < MAX_TRACKED_POSITIONS; index++) {
            positions.add(Long.valueOf(((NBTTagLong) list.get(index)).getLong()));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();
        for (Long packed : positions) list.appendTag(new NBTTagLong(packed.longValue()));
        nbt.setTag("positions", list);
        return nbt;
    }
}
