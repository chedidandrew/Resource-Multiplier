package com.chedidandrew.smartresourcedrops.platform.neoforge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.chedidandrew.smartresourcedrops.provenance.PlacedBlockData;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.storage.IOWorker;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.neoforged.neoforge.attachment.AttachmentHolder;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkDataEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class LegacyFabricProvenanceMigrationTest {
    private static final String FABRIC_CHUNK_FIXTURE =
            "/fixtures/fabric-placement-provenance-chunk--554625--233041.nbt.b64";
    private static final String FABRIC_CHUNK_SHA256 =
            "c390fc16519a7b9f9a1fc29feab66209bca96b5db8bff6e659b239d16d36a38d";
    private static final ChunkPos FABRIC_CHUNK_POS = new ChunkPos(-554625, -233041);
    private static final int FABRIC_PACKED_POSITION = -13958;
    private static final int FABRIC_DATA_VERSION = 4671;

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
        final ProtoChunk chunk = emptyChunk(new ChunkPos(0, 0));
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

    @Test
    void fabric12111ChunkImportsAndSurvivesNativeRegionReopen(
            @TempDir final Path regionDirectory
    ) throws Exception {
        final CompoundTag fabricChunk = readFabricChunkFixture();
        assertEquals(FABRIC_DATA_VERSION, fabricChunk.getIntOr("DataVersion", 0));
        assertEquals(FABRIC_CHUNK_POS.x, fabricChunk.getIntOr("xPos", 0));
        assertEquals(FABRIC_CHUNK_POS.z, fabricChunk.getIntOr("zPos", 0));
        assertTrue(fabricChunk.contains(LegacyFabricProvenanceMigration.FABRIC_ATTACHMENT_ROOT));
        assertFalse(fabricChunk.contains("neoforge:attachments"));

        final PlacedBlockData decoded = LegacyFabricProvenanceMigration.decode(fabricChunk);
        assertTrue(decoded != null && decoded.contains(FABRIC_PACKED_POSITION));

        final SerializableChunkData parsed = parseChunk(fabricChunk);
        final ProtoChunk importedChunk = emptyChunk(FABRIC_CHUNK_POS);
        importedChunk.tryMarkSaved();
        NeoForge.EVENT_BUS.post(new ChunkDataEvent.Load(importedChunk, parsed));
        assertTrue(importedChunk.isUnsaved());
        assertTrue(serializedNativeData(importedChunk).contains(FABRIC_PACKED_POSITION));

        final CompoundTag nativeSave = importedChunk.writeAttachmentsToNBT(RegistryAccess.EMPTY);
        assertTrue(nativeSave.contains(LegacyFabricProvenanceMigration.PLACED_BLOCKS_ID));
        assertFalse(nativeSave.contains(LegacyFabricProvenanceMigration.FABRIC_ATTACHMENT_ROOT));

        final CompoundTag neoForgeSavedChunk = fabricChunk.copy();
        neoForgeSavedChunk.remove(LegacyFabricProvenanceMigration.FABRIC_ATTACHMENT_ROOT);
        neoForgeSavedChunk.put(AttachmentHolder.ATTACHMENTS_NBT_KEY, nativeSave);
        final RegionStorageInfo storageInfo = new RegionStorageInfo(
                "fabric-migration-fixture",
                Level.OVERWORLD,
                "chunk");
        try (TestIOWorker storage = new TestIOWorker(storageInfo, regionDirectory, true)) {
            storage.store(FABRIC_CHUNK_POS, neoForgeSavedChunk).join();
            storage.synchronize(true).join();
        }

        final CompoundTag reopenedChunk;
        try (TestIOWorker storage = new TestIOWorker(storageInfo, regionDirectory, true)) {
            reopenedChunk = storage.loadAsync(FABRIC_CHUNK_POS).join().orElse(null);
        }
        assertTrue(reopenedChunk != null, "Native chunk disappeared after region close and reopen");
        assertFalse(reopenedChunk.contains(LegacyFabricProvenanceMigration.FABRIC_ATTACHMENT_ROOT));
        assertTrue(reopenedChunk.contains(AttachmentHolder.ATTACHMENTS_NBT_KEY));

        final SerializableChunkData restartedData = parseChunk(reopenedChunk);
        assertTrue(restartedData != null && restartedData.attachmentData() != null);
        final ProtoChunk restartedChunk = emptyChunk(FABRIC_CHUNK_POS);
        restartedChunk.readAttachmentsFromNBT(RegistryAccess.EMPTY, restartedData.attachmentData());
        assertTrue(serializedNativeData(restartedChunk).contains(FABRIC_PACKED_POSITION));

        restartedChunk.tryMarkSaved();
        NeoForge.EVENT_BUS.post(new ChunkDataEvent.Load(restartedChunk, parsed));
        assertFalse(restartedChunk.isUnsaved());
        assertTrue(serializedNativeData(restartedChunk).contains(FABRIC_PACKED_POSITION));
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
        return parseChunk(chunkData);
    }

    private static SerializableChunkData parseChunk(final CompoundTag chunkData) {
        return SerializableChunkData.parse(
                // The migration carrier is independent of section decoding. An empty height
                // keeps this focused fixture test registry-free while parsing the real chunk.
                LevelHeightAccessor.create(0, 0),
                new PalettedContainerFactory(null, null, null, null, null, null),
                chunkData);
    }

    private static ProtoChunk emptyChunk(final ChunkPos position) {
        return new ProtoChunk(
                position,
                UpgradeData.EMPTY,
                LevelHeightAccessor.create(0, 0),
                new PalettedContainerFactory(null, null, null, null, null, null),
                null);
    }

    private static CompoundTag readFabricChunkFixture() throws Exception {
        final byte[] bytes;
        try (InputStream resource = LegacyFabricProvenanceMigrationTest.class
                .getResourceAsStream(FABRIC_CHUNK_FIXTURE)) {
            assertTrue(resource != null, "Missing Fabric 1.21.11 migration fixture");
            bytes = Base64.getMimeDecoder().decode(resource.readAllBytes());
        }
        assertEquals(10861, bytes.length);
        assertEquals(
                FABRIC_CHUNK_SHA256,
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
            return NbtIo.read(input, NbtAccounter.unlimitedHeap());
        }
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

    /** Exposes Minecraft's protected 1.21.9 region-I/O constructor to this test only. */
    private static final class TestIOWorker extends IOWorker {
        private TestIOWorker(
                final RegionStorageInfo storageInfo,
                final Path regionDirectory,
                final boolean sync
        ) {
            super(storageInfo, regionDirectory, sync);
        }
    }
}
