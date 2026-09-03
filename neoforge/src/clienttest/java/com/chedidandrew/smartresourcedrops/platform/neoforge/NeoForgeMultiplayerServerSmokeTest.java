package com.chedidandrew.smartresourcedrops.platform.neoforge;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.config.ConfigManager;
import com.chedidandrew.smartresourcedrops.config.SmartDropsConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server-side observer for the separate-process NeoForge multiplayer smoke test. */
@Mod.EventBusSubscriber(modid = SmartResourceDrops.MOD_ID, value = Dist.DEDICATED_SERVER)
public final class NeoForgeMultiplayerServerSmokeTest {
    private static final int PROMOTION_TICKS = 120;
    private static final NeoForgeMultiplayerServerSmokeTest INSTANCE =
            new NeoForgeMultiplayerServerSmokeTest();

    private ServerPlayer player;
    private int connectedTicks;
    private boolean promoted;
    private boolean sawConnectedGuiPatch;
    private boolean sawNearLimitPatch;
    private boolean sawReset;
    private boolean sawFirstLogout;
    private int loginCount;

    private NeoForgeMultiplayerServerSmokeTest() {
    }

    @SubscribeEvent
    public static void loggedIn(final PlayerEvent.PlayerLoggedInEvent event) {
        if (Boolean.getBoolean("smart_resource_drops.multiplayerTest")) {
            INSTANCE.onPlayerLoggedIn(event);
        }
    }

    @SubscribeEvent
    public static void loggedOut(final PlayerEvent.PlayerLoggedOutEvent event) {
        if (Boolean.getBoolean("smart_resource_drops.multiplayerTest")) {
            INSTANCE.onPlayerLoggedOut(event);
        }
    }

    @SubscribeEvent
    public static void tick(final TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END
                && Boolean.getBoolean("smart_resource_drops.multiplayerTest")) {
            INSTANCE.onServerTick();
        }
    }

    private void onPlayerLoggedIn(final PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            this.loginCount++;
            if (this.loginCount > 2) {
                throw new AssertionError("NeoForge multiplayer smoke client logged in more than twice");
            }
            this.player = serverPlayer;
            this.connectedTicks = 0;
            SmartResourceDrops.LOGGER.info(
                    "NeoForge multiplayer smoke client connected for session {}: {}",
                    this.loginCount,
                    serverPlayer.getScoreboardName());
        }
    }

    private void onServerTick() {
        if (this.player == null) {
            return;
        }
        this.connectedTicks++;
        final MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (!this.promoted && this.connectedTicks >= PROMOTION_TICKS) {
            server.getCommands().performPrefixedCommand(
                    server.createCommandSourceStack(),
                    "op " + this.player.getScoreboardName());
            this.promoted = true;
            SmartResourceDrops.LOGGER.info("NeoForge multiplayer smoke client promoted to operator");
        }

        final SmartDropsConfig config = ConfigManager.snapshot();
        final SmartDropsConfig defaults = SmartDropsConfig.defaults();
        if (this.promoted
                && !this.sawConnectedGuiPatch
                && config.entityDropsEnabled
                && Integer.valueOf(0).equals(config.entityMultipliers.get("minecraft:cow"))
                && config.entityBlacklist.contains("minecraft:cow")
                && !config.manualShearingDropsEnabled
                && !config.inheritDefaultShearingMultiplier
                && config.defaultShearingMultiplier == 0
                && Integer.valueOf(0).equals(
                        config.shearingEntityMultipliers.get("minecraft:sheep"))) {
            this.sawConnectedGuiPatch = true;
        }
        if (this.sawConnectedGuiPatch
                && !this.sawNearLimitPatch
                && config.blockRuleEntryCount() == SmartDropsConfig.MAX_BLOCK_RULE_ENTRIES) {
            this.sawNearLimitPatch = true;
        }
        if (this.sawNearLimitPatch
                && config.globalMultiplier == defaults.globalMultiplier
                && config.blockMultipliers.isEmpty()
                && !config.entityDropsEnabled
                && config.entityMultipliers.isEmpty()
                && config.entityBlacklist.isEmpty()
                && config.manualShearingDropsEnabled
                && config.inheritDefaultShearingMultiplier
                && config.shearingEntityMultipliers.isEmpty()) {
            this.sawReset = true;
        }
    }

    private void onPlayerLoggedOut(final PlayerEvent.PlayerLoggedOutEvent event) {
        if (this.player == null || event.getEntity() != this.player) {
            return;
        }
        final MinecraftServer server = this.player.level().getServer();
        if (!this.promoted
                || !this.sawConnectedGuiPatch
                || !this.sawNearLimitPatch
                || !this.sawReset) {
            throw new AssertionError(
                    "NeoForge multiplayer server did not observe every authority/payload phase: "
                            + "promoted=" + this.promoted
                            + ", connectedGui=" + this.sawConnectedGuiPatch
                            + ", nearLimit=" + this.sawNearLimitPatch
                            + ", reset=" + this.sawReset);
        }
        if (this.loginCount == 1) {
            this.sawFirstLogout = true;
            SmartResourceDrops.LOGGER.info(
                    "NeoForge multiplayer smoke first session disconnected; awaiting reconnect");
            this.player = null;
            return;
        }
        if (this.loginCount != 2 || !this.sawFirstLogout) {
            throw new AssertionError(
                    "NeoForge multiplayer reconnect sequence was incomplete: logins="
                            + this.loginCount + ", firstLogout=" + this.sawFirstLogout);
        }
        SmartResourceDrops.LOGGER.info(
                "NeoForge multiplayer server smoke test passed: non-op, promotion, connected entity/filter/shearing GUI patch, near-limit patch, confirmed reset, disconnect, and reconnect");
        this.player = null;
        server.execute(() -> server.halt(false));
    }
}
