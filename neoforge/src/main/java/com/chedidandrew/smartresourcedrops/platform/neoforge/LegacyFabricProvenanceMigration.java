package com.chedidandrew.smartresourcedrops.platform.neoforge;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.provenance.PlacedBlockData;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

/** One-way importer for Fabric chunk attachments when a world moves to NeoForge. */
public final class LegacyFabricProvenanceMigration {
    static final String FABRIC_ATTACHMENT_ROOT = "fabric:attachments";
    static final String PLACED_BLOCKS_ID = SmartResourceDrops.MOD_ID + ":placed_blocks";

    private LegacyFabricProvenanceMigration() {
    }

    /** Decodes Fabric's direct-list attachment without touching registries or chunk state. */
    public static PlacedBlockData decode(final CompoundTag chunkData) {
        final CompoundTag fabricAttachments = chunkData
                .getCompound(FABRIC_ATTACHMENT_ROOT)
                .orElse(null);
        if (fabricAttachments == null || !fabricAttachments.contains(PLACED_BLOCKS_ID)) {
            return null;
        }

        final Tag rawData = fabricAttachments.get(PLACED_BLOCKS_ID);
        final PlacedBlockData legacyData = rawData == null
                ? null
                : PlacedBlockData.CODEC.parse(NbtOps.INSTANCE, rawData).result().orElse(null);
        if (legacyData == null) {
            SmartResourceDrops.LOGGER.warn(
                    "Ignoring malformed Fabric placement provenance in chunk [{}, {}]",
                    chunkData.getIntOr("xPos", 0),
                    chunkData.getIntOr("zPos", 0));
            return null;
        }
        if (legacyData.isEmpty()) {
            return null;
        }
        return legacyData;
    }

    /** Transient data bridge from background NBT parsing to the supported main-thread load event. */
    public interface Carrier {
        PlacedBlockData smart_resource_drops$getLegacyFabricProvenance();

        void smart_resource_drops$setLegacyFabricProvenance(PlacedBlockData data);
    }
}
