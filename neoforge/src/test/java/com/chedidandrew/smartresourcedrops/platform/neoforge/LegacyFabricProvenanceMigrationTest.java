package com.chedidandrew.smartresourcedrops.platform.neoforge;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.chedidandrew.smartresourcedrops.provenance.PlacedBlockData;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkDataEvent;
import org.junit.jupiter.api.Test;

final class LegacyFabricProvenanceMigrationTest {
    @Test
    void decodesValidFabricData() {
        final CompoundTag chunkData = new CompoundTag();
        final CompoundTag fabricAttachments = new CompoundTag();
        final PlacedBlockData legacyData = dataWith(17);
        fabricAttachments.store(
                LegacyFabricProvenanceMigration.PLACED_BLOCKS_ID,
                PlacedBlockData.CODEC,
                legacyData);
        chunkData.put(LegacyFabricProvenanceMigration.FABRIC_ATTACHMENT_ROOT, fabricAttachments);

        final PlacedBlockData decoded = LegacyFabricProvenanceMigration.decode(chunkData);
        assertTrue(decoded != null && decoded.contains(17));
    }

    @Test
    void parseMixinCarriesLegacyDataWithoutChangingTheRawAttachmentEnvelope() {
        final CompoundTag chunkData = fabricChunkWith(dataWith(31));
        chunkData.putString("Status", "minecraft:empty");
        final SerializableChunkData parsed = SerializableChunkData.parse(
                LevelHeightAccessor.create(-64, 384),
                new PalettedContainerFactory(null, null, null, null, null, null),
                chunkData);

        final Object parsedObject = parsed;
        assertTrue(parsedObject instanceof LegacyFabricProvenanceMigration.Carrier);
        final PlacedBlockData carried = ((LegacyFabricProvenanceMigration.Carrier) parsedObject)
                .smart_resource_drops$getLegacyFabricProvenance();
        assertTrue(carried != null && carried.contains(31));
        assertTrue(chunkData.contains(LegacyFabricProvenanceMigration.FABRIC_ATTACHMENT_ROOT));
        assertTrue(!chunkData.contains("neoforge:attachments"));
    }

    @Test
    void malformedOrEmptyFabricDataIsIgnored() {
        final CompoundTag malformedChunk = new CompoundTag();
        final CompoundTag malformedRoot = new CompoundTag();
        malformedRoot.putString(LegacyFabricProvenanceMigration.PLACED_BLOCKS_ID, "not-a-position-list");
        malformedChunk.put(LegacyFabricProvenanceMigration.FABRIC_ATTACHMENT_ROOT, malformedRoot);

        assertTrue(LegacyFabricProvenanceMigration.decode(malformedChunk) == null);

        final CompoundTag emptyChunk = fabricChunkWith(new PlacedBlockData());
        assertTrue(LegacyFabricProvenanceMigration.decode(emptyChunk) == null);

        assertTrue(LegacyFabricProvenanceMigration.decode(new CompoundTag()) == null);
    }

    @Test
    void loadEventImportsOnceSerializesNativeDataAndKeepsNativePrecedence() {
        final LevelHeightAccessor emptyHeight = LevelHeightAccessor.create(0, 0);
        final PalettedContainerFactory factory =
                new PalettedContainerFactory(null, null, null, null, null, null);
        final ProtoChunk chunk = new ProtoChunk(
                new ChunkPos(0, 0),
                UpgradeData.EMPTY,
                emptyHeight,
                factory,
                null);
        chunk.tryMarkSaved();
        assertFalse(chunk.isUnsaved());

        NeoForge.EVENT_BUS.post(new ChunkDataEvent.Load(chunk, parsedFabricData(41)));
        assertTrue(chunk.isUnsaved());

        final PlacedBlockData imported = serializedNativeData(chunk);
        assertTrue(imported.contains(41));

        chunk.tryMarkSaved();
        assertFalse(chunk.isUnsaved());
        NeoForge.EVENT_BUS.post(new ChunkDataEvent.Load(chunk, parsedFabricData(73)));
        assertFalse(chunk.isUnsaved());

        final PlacedBlockData afterStaleImport = serializedNativeData(chunk);
        assertTrue(afterStaleImport.contains(41));
        assertFalse(afterStaleImport.contains(73));
    }

    private static CompoundTag fabricChunkWith(final PlacedBlockData data) {
        final CompoundTag chunkData = new CompoundTag();
        final CompoundTag fabricAttachments = new CompoundTag();
        fabricAttachments.store(
                LegacyFabricProvenanceMigration.PLACED_BLOCKS_ID,
                PlacedBlockData.CODEC,
                data);
        chunkData.put(LegacyFabricProvenanceMigration.FABRIC_ATTACHMENT_ROOT, fabricAttachments);
        return chunkData;
    }

    private static SerializableChunkData parsedFabricData(final int packedPosition) {
        final CompoundTag chunkData = fabricChunkWith(dataWith(packedPosition));
        chunkData.putString("Status", "minecraft:empty");
        return SerializableChunkData.parse(
                LevelHeightAccessor.create(-64, 384),
                new PalettedContainerFactory(null, null, null, null, null, null),
                chunkData);
    }

    private static PlacedBlockData serializedNativeData(final ProtoChunk chunk) {
        final CompoundTag attachments = chunk.writeAttachmentsToNBT(RegistryAccess.EMPTY);
        return attachments
                .getCompound(LegacyFabricProvenanceMigration.PLACED_BLOCKS_ID)
                .flatMap(tag -> tag.read(PlacedBlockData.MAP_CODEC))
                .orElseThrow();
    }

    private static PlacedBlockData dataWith(final int packedPosition) {
        final PlacedBlockData data = new PlacedBlockData();
        data.add(packedPosition);
        return data;
    }
}
