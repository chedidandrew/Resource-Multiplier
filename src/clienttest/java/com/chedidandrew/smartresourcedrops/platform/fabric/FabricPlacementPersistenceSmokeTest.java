package com.chedidandrew.smartresourcedrops.platform.fabric;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.provenance.PlacementTracker;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/** Three-fresh-JVM proof that Fabric chunk attachments survive save and removal. */
public final class FabricPlacementPersistenceSmokeTest implements ModInitializer {
    private static final String PHASE_PROPERTY = "smart_resource_drops.fabricPersistenceTestPhase";
    private static final String DIRECTORY_PROPERTY = "smart_resource_drops.fabricPersistenceTestDirectory";
    private static final BlockPos TEST_POSITION = new BlockPos(8, 70, 8);
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    @Override
    public void onInitialize() {
        final String configuredPhase = System.getProperty(PHASE_PROPERTY);
        if (configuredPhase == null || configuredPhase.isBlank()
                || !REGISTERED.compareAndSet(false, true)) {
            return;
        }
        final Phase phase = Phase.parse(configuredPhase);
        final String configuredDirectory = System.getProperty(DIRECTORY_PROPERTY);
        if (configuredDirectory == null || configuredDirectory.isBlank()) {
            throw new IllegalStateException("Missing Fabric persistence test directory");
        }
        final Path testDirectory = Path.of(configuredDirectory).toAbsolutePath().normalize();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                switch (phase) {
                    case MARK -> markAndSave(server, testDirectory);
                    case REMOVE -> verifyRemoveAndSave(server, testDirectory);
                    case VERIFY_ABSENT -> verifyAbsent(server, testDirectory);
                }
            } catch (Throwable failure) {
                SmartResourceDrops.LOGGER.error(
                        "Fabric native placement persistence {} phase failed",
                        phase.name().toLowerCase(Locale.ROOT),
                        failure);
                throw failure instanceof Error error
                        ? error
                        : new AssertionError("Fabric native placement persistence smoke failed", failure);
            }
        });
    }

    private static void markAndSave(
            final MinecraftServer server,
            final Path testDirectory
    ) throws IOException {
        final ServerLevel level = server.overworld();
        level.getChunkAt(TEST_POSITION);
        require(!PlacementTracker.isMarked(level, TEST_POSITION),
                "Fresh Fabric persistence world unexpectedly contains the test mark");
        PlacementTracker.mark(level, TEST_POSITION);
        require(PlacementTracker.isMarked(level, TEST_POSITION),
                "Fabric placement mark was not visible before save");
        require(server.saveEverything(false, true, true),
                "MinecraftServer.saveEverything reported no saved levels after Fabric mark");
        writeMarker(testDirectory, "fabric-persistence-mark.success", Phase.MARK);
        SmartResourceDrops.LOGGER.info("Fabric native placement mark/save phase passed");
        server.halt(false);
    }

    private static void verifyRemoveAndSave(
            final MinecraftServer server,
            final Path testDirectory
    ) throws IOException {
        requireMarker(testDirectory, "fabric-persistence-mark.success");
        final ServerLevel level = server.overworld();
        level.getChunkAt(TEST_POSITION);
        require(PlacementTracker.isMarked(level, TEST_POSITION),
                "Fabric placement mark did not survive the second server JVM");
        require(PlacementTracker.remove(level, TEST_POSITION),
                "Persisted Fabric placement mark could not be removed");
        require(!PlacementTracker.isMarked(level, TEST_POSITION),
                "Removed Fabric placement mark remained visible before save");
        require(server.saveEverything(false, true, true),
                "MinecraftServer.saveEverything reported no saved levels after Fabric removal");
        writeMarker(testDirectory, "fabric-persistence-remove.success", Phase.REMOVE);
        SmartResourceDrops.LOGGER.info("Fabric native placement lookup/remove/save phase passed");
        server.halt(false);
    }

    private static void verifyAbsent(
            final MinecraftServer server,
            final Path testDirectory
    ) throws IOException {
        requireMarker(testDirectory, "fabric-persistence-remove.success");
        final ServerLevel level = server.overworld();
        level.getChunkAt(TEST_POSITION);
        require(!PlacementTracker.isMarked(level, TEST_POSITION),
                "Removed Fabric placement mark returned after the third server JVM");
        writeMarker(testDirectory, "fabric-persistence-absent.success", Phase.VERIFY_ABSENT);
        SmartResourceDrops.LOGGER.info("Fabric native placement removal persistence phase passed");
        server.halt(false);
    }

    private static void requireMarker(final Path testDirectory, final String marker) {
        require(Files.isRegularFile(testDirectory.resolve(marker)),
                "Required prior Fabric persistence marker is missing: " + marker);
    }

    private static void writeMarker(
            final Path testDirectory,
            final String marker,
            final Phase phase
    ) throws IOException {
        Files.writeString(
                testDirectory.resolve(marker),
                phase.name().toLowerCase(Locale.ROOT) + "\n",
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
                throw new IllegalStateException("Unsupported Fabric persistence phase: " + value, failure);
            }
        }
    }
}
