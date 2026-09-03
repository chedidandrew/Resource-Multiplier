package com.chedidandrew.smartresourcedrops.provenance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PlacedBlockData {
    public static final Codec<PlacedBlockData> CODEC = Codec.INT.listOf().xmap(
            PlacedBlockData::new,
            PlacedBlockData::asSortedList);
    public static final MapCodec<PlacedBlockData> MAP_CODEC = CODEC.fieldOf("positions");

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

    public void replaceWith(final PlacedBlockData other) {
        positions.clear();
        positions.addAll(other.positions);
    }

    public void mergeFrom(final PlacedBlockData other) {
        positions.addAll(other.positions);
    }

    private List<Integer> asSortedList() {
        ArrayList<Integer> sorted = new ArrayList<>(positions);
        sorted.sort(Integer::compareTo);
        return sorted;
    }
}
