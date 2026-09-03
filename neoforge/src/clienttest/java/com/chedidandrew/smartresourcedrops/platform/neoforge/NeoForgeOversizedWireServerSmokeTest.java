package com.chedidandrew.smartresourcedrops.platform.neoforge;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.config.ConfigManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Proves malformed fragment sequences fail closed without mutating server state. */
@Mod.EventBusSubscriber(modid = SmartResourceDrops.MOD_ID, value = Dist.DEDICATED_SERVER)
public final class NeoForgeOversizedWireServerSmokeTest {
    private static final int HEALTH_CHECK_TICKS = 40;
    private static final NeoForgeOversizedWireServerSmokeTest INSTANCE =
            new NeoForgeOversizedWireServerSmokeTest();

    private ServerPlayer attackedPlayer;
    private String baselineJson;
    private long baselineRevision;
    private int ticksAfterDisconnect = -1;

    private NeoForgeOversizedWireServerSmokeTest() {
    }

    @SubscribeEvent
    public static void loggedIn(final PlayerEvent.PlayerLoggedInEvent event) {
        if (Boolean.getBoolean("smart_resource_drops.oversizedWireTest")) {
            INSTANCE.onPlayerLoggedIn(event);
        }
    }

    @SubscribeEvent
    public static void loggedOut(final PlayerEvent.PlayerLoggedOutEvent event) {
        if (Boolean.getBoolean("smart_resource_drops.oversizedWireTest")) {
            INSTANCE.onPlayerLoggedOut(event);
        }
    }

    @SubscribeEvent
    public static void tick(final TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END
                && Boolean.getBoolean("smart_resource_drops.oversizedWireTest")) {
            INSTANCE.onServerTick();
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
        final MinecraftServer server = player.level().getServer();
        server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack(),
                "op " + player.getScoreboardName());
        SmartResourceDrops.LOGGER.info(
                "NeoForge oversized-wire server captured immutable baseline revision {} and promoted the hostile fixture client",
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
                "NeoForge oversized-wire server observed the hostile fixture exit; beginning post-disconnect health check");
    }

    private void onServerTick() {
        if (this.baselineJson == null) {
            return;
        }
        assertConfigurationUnchanged();
        if (this.ticksAfterDisconnect < 0 || ++this.ticksAfterDisconnect < HEALTH_CHECK_TICKS) {
            return;
        }

        final MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server.getCommands().getDispatcher().getRoot().getChild("smartdrops") == null) {
            throw new AssertionError("Smart Resource Multiplier command disappeared after hostile payload");
        }
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "smartdrops status");
        assertConfigurationUnchanged();
        SmartResourceDrops.LOGGER.info(
                "NeoForge oversized-wire server smoke passed: malformed fragments failed closed, unchanged revision/config, {} healthy ticks, and responsive command dispatcher",
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
