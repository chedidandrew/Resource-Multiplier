package com.chedidandrew.smartresourcedrops.platform.neoforge;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.config.ConfigManager;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Proves a malformed wire patch only disconnects its sender and cannot mutate server state. */
@Mod(value = SmartResourceDrops.MOD_ID, dist = Dist.DEDICATED_SERVER)
public final class NeoForgeOversizedWireServerSmokeTest {
    private static final int HEALTH_CHECK_TICKS = 40;
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    private ServerPlayer attackedPlayer;
    private String baselineJson;
    private long baselineRevision;
    private int ticksAfterDisconnect = -1;

    public NeoForgeOversizedWireServerSmokeTest() {
        if (Boolean.getBoolean("smart_resource_drops.oversizedWireTest")
                && REGISTERED.compareAndSet(false, true)) {
            NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, this::onPlayerLoggedIn);
            NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, this::onPlayerLoggedOut);
            NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, this::onServerTick);
        }
    }

    private void onPlayerLoggedIn(final PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (this.attackedPlayer != null || this.ticksAfterDisconnect >= 0) {
            throw new AssertionError("Oversized-wire gate expected exactly one hostile client connection");
        }
        this.attackedPlayer = player;
        final ConfigManager.ClientSnapshot baseline = ConfigManager.clientSnapshot();
        this.baselineJson = baseline.json();
        this.baselineRevision = baseline.revision();
        SmartResourceDrops.LOGGER.info(
                "NeoForge oversized-wire server captured immutable baseline revision {}",
                this.baselineRevision);
    }

    private void onPlayerLoggedOut(final PlayerEvent.PlayerLoggedOutEvent event) {
        if (this.attackedPlayer == null || event.getEntity() != this.attackedPlayer) {
            return;
        }
        assertConfigurationUnchanged();
        this.attackedPlayer = null;
        this.ticksAfterDisconnect = 0;
        SmartResourceDrops.LOGGER.info(
                "NeoForge oversized-wire server observed rejection; beginning post-disconnect health check");
    }

    private void onServerTick(final ServerTickEvent.Post event) {
        if (this.baselineJson == null) {
            return;
        }
        assertConfigurationUnchanged();
        if (this.ticksAfterDisconnect < 0 || ++this.ticksAfterDisconnect < HEALTH_CHECK_TICKS) {
            return;
        }

        final MinecraftServer server = event.getServer();
        if (server.getCommands().getDispatcher().getRoot().getChild("smartdrops") == null) {
            throw new AssertionError("Smart Resource Multiplier command disappeared after hostile payload");
        }
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "smartdrops status");
        assertConfigurationUnchanged();
        SmartResourceDrops.LOGGER.info(
                "NeoForge oversized-wire server smoke passed: decoder rejection, unchanged revision/config, {} healthy ticks, and responsive command dispatcher",
                HEALTH_CHECK_TICKS);
        this.baselineJson = null;
        server.execute(() -> server.halt(false));
    }

    private void assertConfigurationUnchanged() {
        final ConfigManager.ClientSnapshot current = ConfigManager.clientSnapshot();
        if (current.revision() != this.baselineRevision || !current.json().equals(this.baselineJson)) {
            throw new AssertionError(
                    "Hostile wire patch changed server configuration: baseline revision="
                            + this.baselineRevision + ", current revision=" + current.revision());
        }
    }
}
