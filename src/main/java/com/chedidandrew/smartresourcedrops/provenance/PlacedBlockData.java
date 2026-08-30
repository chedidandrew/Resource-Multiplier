package com.chedidandrew.smartresourcedrops.provenance;

import com.mojang.serialization.Codec;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PlacedBlockData {
    public static final Codec<PlacedBlockData> CODEC = Codec.INT.listOf().xmap(
            PlacedBlockData::new,
            PlacedBlockData::asSortedList);

    private final Set<Integer> positions;

    public PlacedBlockData() {
        this.positions = new HashSet<>();
    }

    private PlacedBlockData(List<Integer> positions) {
        this.positions = new HashSet<>(positions);
    }

    public boolean contains(int packedPosition) {
        return positions.contains(packedPosition);
    }

    public boolean add(int packedPosition) {
        return positions.add(packedPosition);
    }

    public boolean remove(int packedPosition) {
        return positions.remove(packedPosition);
    }

    public boolean isEmpty() {
        return positions.isEmpty();
    }

    private List<Integer> asSortedList() {
        ArrayList<Integer> sorted = new ArrayList<>(positions);
        sorted.sort(Integer::compareTo);
        return sorted;
    }
}
