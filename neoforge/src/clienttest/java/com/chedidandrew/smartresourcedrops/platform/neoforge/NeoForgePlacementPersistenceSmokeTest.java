package com.chedidandrew.smartresourcedrops.platform.neoforge;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.provenance.PlacementTracker;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Three-JVM proof that the native Forge capability survives save and removal. */
@Mod.EventBusSubscriber(modid = SmartResourceDrops.MOD_ID, value = Dist.DEDICATED_SERVER)
public final class NeoForgePlacementPersistenceSmokeTest {
    private static final String PHASE_PROPERTY = "smart_resource_drops.persistenceTestPhase";
    private static final String DIRECTORY_PROPERTY = "smart_resource_drops.persistenceTestDirectory";
    private static final BlockPos TEST_POSITION = new BlockPos(8, 70, 8);
    private static final AtomicBoolean EXECUTED = new AtomicBoolean();

    private NeoForgePlacementPersistenceSmokeTest() {
    }

    @SubscribeEvent
    public static void onServerTick(final TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !EXECUTED.compareAndSet(false, true)) {
            return;
        }
        final String configuredPhase = System.getProperty(PHASE_PROPERTY);
        if (configuredPhase == null || configuredPhase.isBlank()) {
            return;
        }
        final Phase phase = Phase.parse(configuredPhase);
        try {
            final String configuredDirectory = System.getProperty(DIRECTORY_PROPERTY);
            if (configuredDirectory == null || configuredDirectory.isBlank()) {
                throw new IllegalStateException("Missing native persistence test directory");
            }
            final Path directory = Path.of(configuredDirectory).toAbsolutePath().normalize();
            switch (phase) {
                case MARK -> markAndSave(event.getServer(), directory);
                case REMOVE -> verifyRemoveAndSave(event.getServer(), directory);
                case VERIFY_ABSENT -> verifyAbsent(event.getServer(), directory);
            }
        } catch (Throwable failure) {
            SmartResourceDrops.LOGGER.error(
                    "NeoForge native placement persistence {} phase failed",
                    phase.name().toLowerCase(Locale.ROOT),
                    failure);
            throw failure instanceof Error error
                    ? error
                    : new AssertionError("NeoForge native placement persistence smoke failed", failure);
        }
    }

    private static void markAndSave(final MinecraftServer server, final Path directory)
            throws IOException {
        final ServerLevel level = server.overworld();
        level.getChunkAt(TEST_POSITION);
        require(!PlacementTracker.isMarked(level, TEST_POSITION),
                "Fresh persistence world unexpectedly contains the test mark");
        PlacementTracker.mark(level, TEST_POSITION);
        require(PlacementTracker.isMarked(level, TEST_POSITION),
                "Placement mark was not visible before save");
        require(server.saveEverything(false, true, true),
                "MinecraftServer.saveEverything reported no saved levels after mark");
        writeMarker(directory, "persistence-mark.success", "mark\n");
        SmartResourceDrops.LOGGER.info("NeoForge native placement mark/save phase passed");
        server.halt(false);
    }

    private static void verifyRemoveAndSave(final MinecraftServer server, final Path directory)
            throws IOException {
        requireMarker(directory, "persistence-mark.success");
        final ServerLevel level = server.overworld();
        level.getChunkAt(TEST_POSITION);
        require(PlacementTracker.isMarked(level, TEST_POSITION),
                "Native placement mark did not survive the second server JVM");
        require(PlacementTracker.remove(level, TEST_POSITION),
                "Persisted placement mark could not be removed");
        require(!PlacementTracker.isMarked(level, TEST_POSITION),
                "Removed placement mark remained visible before save");
        require(server.saveEverything(false, true, true),
                "MinecraftServer.saveEverything reported no saved levels after removal");
        writeMarker(directory, "persistence-remove.success", "remove\n");
        SmartResourceDrops.LOGGER.info("NeoForge native placement lookup/remove/save phase passed");
        server.halt(false);
    }

    private static void verifyAbsent(final MinecraftServer server, final Path directory)
            throws IOException {
        requireMarker(directory, "persistence-remove.success");
        final ServerLevel level = server.overworld();
        level.getChunkAt(TEST_POSITION);
        require(!PlacementTracker.isMarked(level, TEST_POSITION),
                "Removed native placement mark returned after the third server JVM");
        writeMarker(directory, "persistence-absent.success", "verify_absent\n");
        SmartResourceDrops.LOGGER.info("NeoForge native placement removal persistence phase passed");
        server.halt(false);
    }

    private static void requireMarker(final Path directory, final String marker) {
        require(Files.isRegularFile(directory.resolve(marker)),
                "Required prior persistence marker is missing: " + marker);
    }

    private static void writeMarker(
            final Path directory,
            final String marker,
            final String value
    ) throws IOException {
        Files.writeString(
                directory.resolve(marker),
                value,
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

    private enum Phase {
        MARK,
        REMOVE,
        VERIFY_ABSENT;

        private static Phase parse(final String value) {
            try {
                return valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException failure) {
                throw new IllegalStateException("Unsupported persistence phase: " + value, failure);
            }
        }
    }
}
