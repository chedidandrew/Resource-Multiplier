package com.chedidandrew.smartresourcedrops.core;

public final class PackedBlockPosition {
    private PackedBlockPosition() {
    }

    /**
     * Packs a position relative to its chunk. The low 8 bits store local X/Z and the remaining
     * bits store signed Y. Minecraft's practical build heights are far inside this range.
     */
    public static int pack(int x, int y, int z) {
        return (y << 8) | ((z & 15) << 4) | (x & 15);
    }
}
