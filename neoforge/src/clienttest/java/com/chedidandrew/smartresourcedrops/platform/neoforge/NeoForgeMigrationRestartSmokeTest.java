package com.chedidandrew.smartresourcedrops.platform.neoforge;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.provenance.PlacedBlockData;
import com.chedidandrew.smartresourcedrops.provenance.PlacementTracker;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.storage.IOWorker;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentHolder;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Two-process, test-run-only proof of Fabric chunk provenance migration and native restart. */
@Mod(value = SmartResourceDrops.MOD_ID, dist = Dist.DEDICATED_SERVER)
public final class NeoForgeMigrationRestartSmokeTest {
    private static final String PHASE_PROPERTY = "smart_resource_drops.migrationTestPhase";
    private static final String DIRECTORY_PROPERTY = "smart_resource_drops.migrationTestDirectory";
    private static final String WORLD_NAME = "migration-smoke-world";
    private static final String IMPORT_MARKER = "migration-import.success";
    private static final String RESTART_MARKER = "migration-restart.success";
    private static final ChunkPos FABRIC_CHUNK_POS = new ChunkPos(-554625, -233041);
    private static final int FABRIC_PACKED_POSITION = -13958;
    private static final BlockPos FABRIC_BLOCK_POS = new BlockPos(-8873990, -55, -3728649);
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    private Phase phase;
    private Path testDirectory;
    private boolean executed;

    public NeoForgeMigrationRestartSmokeTest() {
        final String configuredPhase = System.getProperty(PHASE_PROPERTY);
        if (configuredPhase == null || configuredPhase.isBlank()) {
            return;
        }
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }

        this.phase = Phase.parse(configuredPhase);
        final String configuredDirectory = System.getProperty(DIRECTORY_PROPERTY);
        if (configuredDirectory == null || configuredDirectory.isBlank()) {
            throw new IllegalStateException("Missing migration test directory system property");
        }
        this.testDirectory = Path.of(configuredDirectory).toAbsolutePath().normalize();
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, this::onServerTick);
    }

    private void onServerTick(final ServerTickEvent.Post event) {
        if (this.executed) {
            return;
        }
        this.executed = true;

        try {
            switch (this.phase) {
                case IMPORT -> runImportPhase(event.getServer());
                case RESTART -> runRestartPhase(event.getServer());
            }
        } catch (Throwable failure) {
            SmartResourceDrops.LOGGER.error(
                    "NeoForge Fabric migration {} phase failed",
                    this.phase.name().toLowerCase(Locale.ROOT),
                    failure);
            throw failure instanceof Error error
                    ? error
                    : new AssertionError("NeoForge Fabric migration restart smoke test failed", failure);
        }
    }

    private void runImportPhase(final MinecraftServer server) throws IOException {
        final CompoundTag fabricChunk = readPersistedChunk();
        require(
                fabricChunk.contains(LegacyFabricProvenanceMigration.FABRIC_ATTACHMENT_ROOT),
                "Seeded chunk has no Fabric attachment envelope");
        require(
                !fabricChunk.contains(AttachmentHolder.ATTACHMENTS_NBT_KEY),
                "Seeded chunk unexpectedly has a NeoForge attachment envelope");
        final PlacedBlockData legacyData = LegacyFabricProvenanceMigration.decode(fabricChunk);
        require(
                legacyData != null && legacyData.contains(FABRIC_PACKED_POSITION),
                "Seeded Fabric attachment does not contain the captured packed position");

        final ServerLevel level = server.overworld();
        final LevelChunk chunk = level.getChunkAt(FABRIC_BLOCK_POS);
        require(
                PlacementTracker.peekPlaced(level, FABRIC_BLOCK_POS),
                "Gameplay provenance lookup did not see imported Fabric data");
        require(chunk.isUnsaved(), "Imported chunk was not marked unsaved for native persistence");
        requireNativePosition(
                chunk.writeAttachmentsToNBT(level.registryAccess()),
                "live imported chunk");

        require(
                server.saveEverything(false, true, true),
                "MinecraftServer.saveEverything reported no saved levels");
        writeSuccessMarker(IMPORT_MARKER);
        SmartResourceDrops.LOGGER.info(
                "NeoForge Fabric migration import phase passed: real chunk load, gameplay lookup, and native server save");
        server.halt(false);
    }

    private void runRestartPhase(final MinecraftServer server) throws IOException {
        require(
                Files.isRegularFile(this.testDirectory.resolve(IMPORT_MARKER)),
                "Restart phase started without a successful import marker");

        final CompoundTag savedChunk = readPersistedChunk();
        require(
                !savedChunk.contains(LegacyFabricProvenanceMigration.FABRIC_ATTACHMENT_ROOT),
                "Real server save retained the legacy Fabric attachment envelope");
        final CompoundTag nativeAttachments = savedChunk
                .getCompound(AttachmentHolder.ATTACHMENTS_NBT_KEY)
                .orElseThrow(() -> new AssertionError(
                        "Real server save did not write a NeoForge attachment envelope"));
        requireNativePosition(nativeAttachments, "persisted restarted chunk");

        final ServerLevel level = server.overworld();
        final LevelChunk chunk = level.getChunkAt(FABRIC_BLOCK_POS);
        require(
                PlacementTracker.peekPlaced(level, FABRIC_BLOCK_POS),
                "Gameplay provenance lookup did not survive the second server JVM");
        requireNativePosition(
                chunk.writeAttachmentsToNBT(level.registryAccess()),
                "live restarted chunk");

        writeSuccessMarker(RESTART_MARKER);
        SmartResourceDrops.LOGGER.info(
                "NeoForge Fabric migration restart phase passed: native disk data and gameplay provenance survived a second server JVM");
        server.halt(false);
    }

    private CompoundTag readPersistedChunk() throws IOException {
        final Path regionDirectory = this.testDirectory
                .resolve(WORLD_NAME)
                .resolve("region");
        final RegionStorageInfo storageInfo = new RegionStorageInfo(
                "fabric-migration-server-smoke",
                Level.OVERWORLD,
                "chunk");
        try (TestIOWorker storage = new TestIOWorker(storageInfo, regionDirectory, true)) {
            final CompoundTag chunk = storage.loadAsync(FABRIC_CHUNK_POS).join().orElse(null);
            if (chunk == null) {
                throw new AssertionError("Migration fixture chunk is missing from " + regionDirectory);
            }
            return chunk;
        }
    }

    private static void requireNativePosition(
            final CompoundTag attachments,
            final String context
    ) {
        final PlacedBlockData data = attachments
                .getCompound(LegacyFabricProvenanceMigration.PLACED_BLOCKS_ID)
                .flatMap(tag -> tag.read(PlacedBlockData.MAP_CODEC))
                .orElseThrow(() -> new AssertionError(
                        context + " has no decodable native placed-block attachment"));
        require(
                data.contains(FABRIC_PACKED_POSITION),
                context + " is missing the captured packed position");
    }

    private void writeSuccessMarker(final String markerName) throws IOException {
        Files.writeString(
                this.testDirectory.resolve(markerName),
                this.phase.name().toLowerCase(Locale.ROOT) + "\n",
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
    }

    private static void require(final boolean condition, final String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /** Exposes Minecraft's protected 1.21.11 region-I/O constructor to this test only. */
    private static final class TestIOWorker extends IOWorker {
        private TestIOWorker(
                final RegionStorageInfo storageInfo,
                final Path regionDirectory,
                final boolean sync
        ) {
            super(storageInfo, regionDirectory, sync);
        }
    }

    private enum Phase {
        IMPORT,
        RESTART;

        private static Phase parse(final String value) {
            try {
                return valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException failure) {
                throw new IllegalStateException("Unsupported migration test phase: " + value, failure);
            }
        }
    }
}
