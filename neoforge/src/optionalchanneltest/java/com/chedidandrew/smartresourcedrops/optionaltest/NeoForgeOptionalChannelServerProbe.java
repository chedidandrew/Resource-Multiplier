package com.chedidandrew.smartresourcedrops.optionaltest;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/** Test-only server observer for the real optional-channel installation matrix. */
@Mod.EventBusSubscriber(modid = OptionalChannelIds.PROBE_MOD_ID, value = Dist.DEDICATED_SERVER)
public final class NeoForgeOptionalChannelServerProbe {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int OBSERVATION_TICKS = 40;
    private static final NeoForgeOptionalChannelServerProbe INSTANCE =
            new NeoForgeOptionalChannelServerProbe();

    private final String mode;
    private ServerPlayer player;
    private int connectedTicks;
    private boolean observedJoin;
    private boolean observedStableConnection;

    private NeoForgeOptionalChannelServerProbe() {
        this.mode = System.getProperty("smart_resource_drops.optionalChannelTest", "");
        if (!this.mode.isEmpty()) {
            final boolean productionLoaded = ModList.get().isLoaded(OptionalChannelIds.PRODUCTION_MOD_ID);
            final boolean expectedProductionLoaded = this.mode.equals("serverOnly");
            if (productionLoaded != expectedProductionLoaded) {
                throw new AssertionError(
                        "Optional-channel server production-mod state mismatch for " + this.mode
                                + ": loaded=" + productionLoaded);
            }
        }
    }

    @SubscribeEvent
    public static void loggedIn(final PlayerEvent.PlayerLoggedInEvent event) {
        if (!INSTANCE.mode.isEmpty()) {
            INSTANCE.onPlayerLoggedIn(event);
        }
    }

    @SubscribeEvent
    public static void loggedOut(final PlayerEvent.PlayerLoggedOutEvent event) {
        if (!INSTANCE.mode.isEmpty()) {
            INSTANCE.onPlayerLoggedOut(event);
        }
    }

    @SubscribeEvent
    public static void tick(final TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !INSTANCE.mode.isEmpty()) {
            INSTANCE.onServerTick();
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

    private void onServerTick() {
        if (this.player == null || this.observedStableConnection) {
            return;
        }
        if (++this.connectedTicks < OBSERVATION_TICKS) {
            return;
        }
        // The production client-only side independently checks
        // SimpleChannel#isRemotePresent. This production-absent observer proves
        // only what it can inspect truthfully: the mixed installation remains
        // connected for the full observation window.
        this.observedStableConnection = true;
        LOGGER.info(
                "NeoForge optional-channel {} server confirmed a stable mixed-mod connection",
                this.mode);
    }

    private void onPlayerLoggedOut(final PlayerEvent.PlayerLoggedOutEvent event) {
        if (this.player == null || event.getEntity() != this.player) {
            return;
        }
        final MinecraftServer server = this.player.level().getServer();
        if (!this.observedJoin || !this.observedStableConnection) {
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
