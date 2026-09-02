package com.chedidandrew.smartresourcedrops.client;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.network.ConfigPatchPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigRequestPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigResetPayload;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Production client joining a server that does not install the production mod. */
@Mod(value = SmartResourceDrops.MOD_ID, dist = Dist.CLIENT)
public final class NeoForgeOptionalClientOnlySmokeTest {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();
    private static final int OBSERVATION_TICKS = 60;
    private static final int TIMEOUT_TICKS = 3_000;

    private int ticks;
    private int connectedTicks;
    private boolean checkedConfigRoute;

    public NeoForgeOptionalClientOnlySmokeTest() {
        if (Boolean.getBoolean("smart_resource_drops.optionalClientOnlyTest")
                && REGISTERED.compareAndSet(false, true)) {
            if (!ModList.get().isLoaded(SmartResourceDrops.MOD_ID)) {
                throw new AssertionError("Client-only matrix client did not load the production mod");
            }
            NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, this::onClientTick);
        }
    }

    private void onClientTick(final ClientTickEvent.Post event) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (++this.ticks > TIMEOUT_TICKS) {
            throw new AssertionError("Timed out while joining the client-only installation");
        }
        final var connection = minecraft.getConnection();
        if (connection == null || minecraft.player == null || minecraft.level == null) {
            return;
        }
        requireUnavailable(connection.hasChannel(ConfigRequestPayload.TYPE), "config request");
        requireUnavailable(connection.hasChannel(ConfigPatchPayload.TYPE), "config patch");
        requireUnavailable(connection.hasChannel(ConfigResetPayload.TYPE), "config reset");

        if (!this.checkedConfigRoute) {
            final ClientConfigState.RequestStart request = ClientConfigState.request(minecraft);
            if (request.started()
                    || request.failure() != ClientConfigState.StartFailure.CHANNEL_UNAVAILABLE) {
                throw new AssertionError(
                        "Client-only config request did not fail closed with CHANNEL_UNAVAILABLE");
            }
            minecraft.setScreen(SmartDropsConfigScreens.create(null));
            if (!(minecraft.screen instanceof SmartDropsConfigLoadingScreen)) {
                throw new AssertionError(
                        "Connected client-only config route fell back to local defaults");
            }
            this.checkedConfigRoute = true;
        }

        if (++this.connectedTicks < OBSERVATION_TICKS) {
            return;
        }
        if (!(minecraft.screen instanceof SmartDropsConfigLoadingScreen)) {
            throw new AssertionError("Unavailable-server error route did not remain on the loading bridge");
        }

        SmartResourceDrops.LOGGER.info(
                "NeoForge optional-channel client passed: client-only installation connected, server-bound channels unavailable, and config route failed closed");
        minecraft.stop();
    }

    private static void requireUnavailable(final boolean available, final String label) {
        if (available) {
            throw new AssertionError(
                    "Optional channel unexpectedly negotiated against an unmodded server: " + label);
        }
    }
}
