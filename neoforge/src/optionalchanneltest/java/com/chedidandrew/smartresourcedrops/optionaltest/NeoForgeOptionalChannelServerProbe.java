package com.chedidandrew.smartresourcedrops.optionaltest;

import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/** Test-only server observer for the real optional-channel installation matrix. */
@Mod(value = OptionalChannelIds.PROBE_MOD_ID, dist = Dist.DEDICATED_SERVER)
public final class NeoForgeOptionalChannelServerProbe {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();
    private static final int OBSERVATION_TICKS = 40;

    private final String mode;
    private ServerPlayer player;
    private int connectedTicks;
    private boolean observedJoin;
    private boolean observedUnavailableChannels;

    public NeoForgeOptionalChannelServerProbe() {
        this.mode = System.getProperty("smart_resource_drops.optionalChannelTest", "");
        if (!this.mode.isEmpty() && REGISTERED.compareAndSet(false, true)) {
            final boolean productionLoaded = ModList.get().isLoaded(OptionalChannelIds.PRODUCTION_MOD_ID);
            final boolean expectedProductionLoaded = this.mode.equals("serverOnly");
            if (productionLoaded != expectedProductionLoaded) {
                throw new AssertionError(
                        "Optional-channel server production-mod state mismatch for " + this.mode
                                + ": loaded=" + productionLoaded);
            }
            NeoForge.EVENT_BUS.addListener(
                    PlayerEvent.PlayerLoggedInEvent.class,
                    this::onPlayerLoggedIn);
            NeoForge.EVENT_BUS.addListener(
                    PlayerEvent.PlayerLoggedOutEvent.class,
                    this::onPlayerLoggedOut);
            NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, this::onServerTick);
        }
    }

    private void onPlayerLoggedIn(final PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (this.player != null) {
            throw new AssertionError("Optional-channel smoke accepted an unexpected second player");
        }
        this.player = serverPlayer;
        this.observedJoin = true;
        this.connectedTicks = 0;
        LOGGER.info("NeoForge optional-channel {} client connected", this.mode);
    }

    private void onServerTick(final ServerTickEvent.Post event) {
        if (this.player == null || this.observedUnavailableChannels) {
            return;
        }
        if (++this.connectedTicks < OBSERVATION_TICKS) {
            return;
        }
        final var unavailableTypes = this.mode.equals("clientOnly")
                ? OptionalChannelIds.clientToServer()
                : OptionalChannelIds.serverToClient();
        for (var type : unavailableTypes) {
            if (this.player.connection.hasChannel(type)) {
                throw new AssertionError(
                        "Optional Smart Resource Multiplier destination channel unexpectedly available in "
                                + this.mode + " mode: " + type.id());
            }
        }
        this.observedUnavailableChannels = true;
        LOGGER.info(
                "NeoForge optional-channel {} server confirmed absent-destination channels unavailable",
                this.mode);
    }

    private void onPlayerLoggedOut(final PlayerEvent.PlayerLoggedOutEvent event) {
        if (this.player == null || event.getEntity() != this.player) {
            return;
        }
        final MinecraftServer server = this.player.level().getServer();
        if (!this.observedJoin || !this.observedUnavailableChannels) {
            throw new AssertionError(
                    "Optional-channel " + this.mode + " client disconnected before validation completed");
        }
        LOGGER.info(
                "NeoForge optional-channel server probe passed: {} installation and clean disconnect",
                this.mode);
        this.player = null;
        server.execute(() -> server.halt(false));
    }
}
