package com.chedidandrew.smartresourcedrops.platform.fabric;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.config.ConfigManager;
import com.chedidandrew.smartresourcedrops.config.SmartDropsConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.atomic.AtomicBoolean;

/** Server observer for the separate-process Fabric permission/network smoke gate. */
public final class FabricMultiplayerServerSmokeTest implements ModInitializer {
    private static final int PROMOTION_TICKS = 120;
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    private ServerPlayer player;
    private int connectedTicks;
    private boolean promoted;
    private boolean sawPatch;
    private boolean sawReset;
    private boolean sawFirstLogout;
    private int loginCount;

    @Override
    public void onInitialize() {
        if (!Boolean.getBoolean("smart_resource_drops.fabricMultiplayerSmoke")
                || !REGISTERED.compareAndSet(false, true)) {
            return;
        }
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                this.onPlayerLoggedIn(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                this.onPlayerLoggedOut(handler.getPlayer(), server));
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
    }

    private void onPlayerLoggedIn(final ServerPlayer serverPlayer) {
        this.loginCount++;
        if (this.loginCount > 2) {
            throw new AssertionError("Fabric authority smoke client logged in more than twice");
        }
        this.player = serverPlayer;
        this.connectedTicks = 0;
        SmartResourceDrops.LOGGER.info(
                "Fabric authority smoke client connected for session {}: {}",
                this.loginCount,
                serverPlayer.getScoreboardName());
    }

    private void onServerTick(final MinecraftServer server) {
        if (this.player == null) {
            return;
        }
        this.connectedTicks++;
        if (!this.promoted && this.connectedTicks >= PROMOTION_TICKS) {
            server.getCommands().performPrefixedCommand(
                    server.createCommandSourceStack(),
                    "op " + this.player.getScoreboardName());
            this.promoted = true;
            SmartResourceDrops.LOGGER.info("Fabric authority smoke client promoted to operator");
        }

        final SmartDropsConfig config = ConfigManager.snapshot();
        final SmartDropsConfig defaults = SmartDropsConfig.defaults();
        if (this.promoted && config.globalMultiplier != defaults.globalMultiplier) {
            this.sawPatch = true;
        }
        if (this.sawPatch
                && config.globalMultiplier == defaults.globalMultiplier
                && config.blockMultipliers.isEmpty()
                && !config.entityDropsEnabled
                && config.manualShearingDropsEnabled) {
            this.sawReset = true;
        }
    }

    private void onPlayerLoggedOut(
            final ServerPlayer disconnected,
            final MinecraftServer server
    ) {
        if (this.player == null || disconnected != this.player) {
            return;
        }
        if (!this.promoted || !this.sawPatch || !this.sawReset) {
            throw new AssertionError(
                    "Fabric authority server missed a required phase: promoted=" + this.promoted
                            + ", patch=" + this.sawPatch + ", reset=" + this.sawReset);
        }
        if (this.loginCount == 1) {
            this.sawFirstLogout = true;
            this.player = null;
            SmartResourceDrops.LOGGER.info(
                    "Fabric authority smoke first session disconnected; awaiting reconnect");
            return;
        }
        if (this.loginCount != 2 || !this.sawFirstLogout) {
            throw new AssertionError(
                    "Fabric authority reconnect sequence was incomplete: logins="
                            + this.loginCount + ", firstLogout=" + this.sawFirstLogout);
        }
        this.player = null;
        SmartResourceDrops.LOGGER.info(
                "Fabric multiplayer server smoke passed: non-op denial, operator promotion, authoritative patch/reset, disconnect cleanup, and reconnect");
        server.execute(() -> server.halt(false));
    }
}
