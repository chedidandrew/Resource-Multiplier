package com.chedidandrew.smartresourcedrops.client;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.network.ConfigPatchFragmentPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigRequestPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigResetPayload;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/** Production client joining a server that does not install the production mod. */
@Mod.EventBusSubscriber(modid = SmartResourceDrops.MOD_ID, value = Dist.CLIENT)
public final class NeoForgeOptionalClientOnlySmokeTest {
    private static final NeoForgeOptionalClientOnlySmokeTest INSTANCE =
            new NeoForgeOptionalClientOnlySmokeTest();
    private static final int OBSERVATION_TICKS = 60;
    private static final int TIMEOUT_TICKS = 3_000;

    private int ticks;
    private int connectedTicks;
    private boolean checkedConfigRoute;

    private NeoForgeOptionalClientOnlySmokeTest() {
    }

    @SubscribeEvent
    public static void tick(final TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END
                && Boolean.getBoolean("smart_resource_drops.optionalClientOnlyTest")) {
            if (!ModList.get().isLoaded(SmartResourceDrops.MOD_ID)) {
                throw new AssertionError("Client-only matrix client did not load the production mod");
            }
            INSTANCE.onClientTick();
        }
    }

    private void onClientTick() {
        final Minecraft minecraft = Minecraft.getInstance();
        if (++this.ticks > TIMEOUT_TICKS) {
            throw new AssertionError("Timed out while joining the client-only installation");
        }
        final var connection = minecraft.getConnection();
        if (connection == null || minecraft.player == null || minecraft.level == null) {
            return;
        }
        requireUnavailable(ClientNetworkBridge.canSend(ConfigRequestPayload.TYPE), "config request");
        requireUnavailable(ClientNetworkBridge.canSend(ConfigPatchFragmentPayload.TYPE), "config patch");
        requireUnavailable(ClientNetworkBridge.canSend(ConfigResetPayload.TYPE), "config reset");

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
