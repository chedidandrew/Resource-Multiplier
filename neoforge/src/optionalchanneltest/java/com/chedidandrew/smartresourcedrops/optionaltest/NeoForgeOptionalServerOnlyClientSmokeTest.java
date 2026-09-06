package com.chedidandrew.smartresourcedrops.optionaltest;

import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TickEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/** Unmodded-with-respect-to-production client joining a modded server. */
public final class NeoForgeOptionalServerOnlyClientSmokeTest {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int OBSERVATION_TICKS = 60;
    private static final int TIMEOUT_TICKS = 3_000;

    private int ticks;
    private int connectedTicks;

    private NeoForgeOptionalServerOnlyClientSmokeTest() {
    }

    static void register() {
        if (Boolean.getBoolean("smart_resource_drops.optionalServerOnlyClientTest")) {
            if (ModList.get().isLoaded(OptionalChannelIds.PRODUCTION_MOD_ID)) {
                throw new AssertionError("Server-only matrix client unexpectedly loaded the production mod");
            }
            final NeoForgeOptionalServerOnlyClientSmokeTest test =
                    new NeoForgeOptionalServerOnlyClientSmokeTest();
            NeoForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
                if (event.phase == TickEvent.Phase.END) {
                    test.onClientTick();
                }
            });
        }
    }

    private void onClientTick() {
        final Minecraft minecraft = Minecraft.getInstance();
        if (++this.ticks > TIMEOUT_TICKS) {
            throw new AssertionError("Timed out while joining the server-only installation");
        }
        final var connection = minecraft.getConnection();
        if (connection == null || minecraft.player == null || minecraft.level == null) {
            return;
        }
        if (++this.connectedTicks < OBSERVATION_TICKS) {
            return;
        }

        LOGGER.info(
                "NeoForge optional-channel client passed: production-unmodded client remained connected to the server-only installation");
        minecraft.stop();
    }
}
