package com.chedidandrew.smartresourcedrops.optionaltest;

import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/** Unmodded-with-respect-to-production client joining a modded server. */
@Mod(value = OptionalChannelIds.PROBE_MOD_ID, dist = Dist.CLIENT)
public final class NeoForgeOptionalServerOnlyClientSmokeTest {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();
    private static final int OBSERVATION_TICKS = 60;
    private static final int TIMEOUT_TICKS = 3_000;

    private int ticks;
    private int connectedTicks;

    public NeoForgeOptionalServerOnlyClientSmokeTest() {
        if (Boolean.getBoolean("smart_resource_drops.optionalServerOnlyClientTest")
                && REGISTERED.compareAndSet(false, true)) {
            if (ModList.get().isLoaded(OptionalChannelIds.PRODUCTION_MOD_ID)) {
                throw new AssertionError("Server-only matrix client unexpectedly loaded the production mod");
            }
            NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, this::onClientTick);
        }
    }

    private void onClientTick(final ClientTickEvent.Post event) {
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
