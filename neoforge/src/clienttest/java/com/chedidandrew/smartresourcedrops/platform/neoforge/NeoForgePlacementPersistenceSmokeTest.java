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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Three-JVM proof that native NeoForge placement data survives save and removal. */
@Mod(value = SmartResourceDrops.MOD_ID, dist = Dist.DEDICATED_SERVER)
public final class NeoForgePlacementPersistenceSmokeTest {
    private static final String PHASE_PROPERTY = "smart_resource_drops.persistenceTestPhase";
    private static final String DIRECTORY_PROPERTY = "smart_resource_drops.persistenceTestDirectory";
    private static final BlockPos TEST_POSITION = new BlockPos(8, 70, 8);
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    private final Phase phase;
    private final Path testDirectory;
    private boolean executed;

    public NeoForgePlacementPersistenceSmokeTest() {
        final String configuredPhase = System.getProperty(PHASE_PROPERTY);
        if (configuredPhase == null || configuredPhase.isBlank()) {
            this.phase = null;
            this.testDirectory = null;
            return;
        }
        if (!REGISTERED.compareAndSet(false, true)) {
            this.phase = null;
            this.testDirectory = null;
            return;
        }
        this.phase = Phase.parse(configuredPhase);
        final String configuredDirectory = System.getProperty(DIRECTORY_PROPERTY);
        if (configuredDirectory == null || configuredDirectory.isBlank()) {
            throw new IllegalStateException("Missing native persistence test directory");
        }
        this.testDirectory = Path.of(configuredDirectory).toAbsolutePath().normalize();
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, this::onServerTick);
    }

    private void onServerTick(final ServerTickEvent.Post event) {
        if (this.executed || this.phase == null) {
            return;
        }
        this.executed = true;
        try {
            switch (this.phase) {
                case MARK -> markAndSave(event.getServer());
                case REMOVE -> verifyRemoveAndSave(event.getServer());
                case VERIFY_ABSENT -> verifyAbsent(event.getServer());
            }
        } catch (Throwable failure) {
            SmartResourceDrops.LOGGER.error(
                    "NeoForge native placement persistence {} phase failed",
                    this.phase.name().toLowerCase(Locale.ROOT),
                    failure);
            throw failure instanceof Error error
                    ? error
                    : new AssertionError("NeoForge native placement persistence smoke failed", failure);
        }
    }

    private void markAndSave(final MinecraftServer server) throws IOException {
        final ServerLevel level = server.overworld();
        level.getChunkAt(TEST_POSITION);
        require(!PlacementTracker.isMarked(level, TEST_POSITION),
                "Fresh persistence world unexpectedly contains the test mark");
        PlacementTracker.mark(level, TEST_POSITION);
        require(PlacementTracker.isMarked(level, TEST_POSITION),
                "Placement mark was not visible before save");
        require(server.saveEverything(false, true, true),
                "MinecraftServer.saveEverything reported no saved levels after mark");
        writeMarker("persistence-mark.success");
        SmartResourceDrops.LOGGER.info("NeoForge native placement mark/save phase passed");
        server.halt(false);
    }

    private void verifyRemoveAndSave(final MinecraftServer server) throws IOException {
        requireMarker("persistence-mark.success");
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
        writeMarker("persistence-remove.success");
        SmartResourceDrops.LOGGER.info("NeoForge native placement lookup/remove/save phase passed");
        server.halt(false);
    }

    private void verifyAbsent(final MinecraftServer server) throws IOException {
        requireMarker("persistence-remove.success");
        final ServerLevel level = server.overworld();
        level.getChunkAt(TEST_POSITION);
        require(!PlacementTracker.isMarked(level, TEST_POSITION),
                "Removed native placement mark returned after the third server JVM");
        writeMarker("persistence-absent.success");
        SmartResourceDrops.LOGGER.info("NeoForge native placement removal persistence phase passed");
        server.halt(false);
    }

    private void requireMarker(final String marker) {
        require(Files.isRegularFile(this.testDirectory.resolve(marker)),
                "Required prior persistence marker is missing: " + marker);
    }

    private void writeMarker(final String marker) throws IOException {
        Files.writeString(
                this.testDirectory.resolve(marker),
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
