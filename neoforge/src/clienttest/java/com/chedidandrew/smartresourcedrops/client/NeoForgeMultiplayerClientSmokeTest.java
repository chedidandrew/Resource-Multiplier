package com.chedidandrew.smartresourcedrops.client;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.config.ConfigPatch;
import com.chedidandrew.smartresourcedrops.config.SmartDropsConfig;
import com.chedidandrew.smartresourcedrops.network.ConfigInvalidationPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigMutationResultPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigPatchPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigRequestPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigResetPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigSnapshotPayload;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Separate-process client/server authority and payload-boundary smoke test. */
@Mod(value = SmartResourceDrops.MOD_ID, dist = Dist.CLIENT)
public final class NeoForgeMultiplayerClientSmokeTest {
    private static final int TIMEOUT_TICKS = 6_000;
    private static final int PROMOTION_WAIT_TICKS = 180;
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    private Phase phase = Phase.WAIT_CONNECTION;
    private int ticks;
    private int phaseTicks;
    private int changedGlobalMultiplier;
    private int expectedNearLimitBlockMultipliers;

    public NeoForgeMultiplayerClientSmokeTest() {
        if (Boolean.getBoolean("smart_resource_drops.multiplayerTest")
                && REGISTERED.compareAndSet(false, true)) {
            NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, this::onClientTick);
        }
    }

    private void onClientTick(final ClientTickEvent.Post event) {
        final Minecraft minecraft = Minecraft.getInstance();
        try {
            if (minecraft.gui.overlay() != null) {
                return;
            }
            if (++this.ticks > TIMEOUT_TICKS) {
                throw new AssertionError("Timed out during NeoForge multiplayer smoke test at " + this.phase);
            }

            switch (this.phase) {
                case WAIT_CONNECTION -> waitForConnection(minecraft);
                case WAIT_READ_ONLY -> waitForReadOnlySnapshot(minecraft);
                case WAIT_PROMOTION -> waitForPromotion(minecraft);
                case WAIT_EDITABLE -> waitForEditableSnapshot(minecraft);
                case WAIT_BASIC_PATCH -> waitForBasicPatch(minecraft);
                case WAIT_NEAR_LIMIT_PATCH -> waitForNearLimitPatch(minecraft);
                case WAIT_RESET -> waitForReset(minecraft);
                case COMPLETE -> {
                    // The client is already stopping.
                }
            }
        } catch (Throwable failure) {
            SmartResourceDrops.LOGGER.error("NeoForge multiplayer client smoke test failed", failure);
            throw failure instanceof Error error
                    ? error
                    : new AssertionError("NeoForge multiplayer client smoke test failed", failure);
        }
    }

    private void waitForConnection(final Minecraft minecraft) {
        final var connection = minecraft.getConnection();
        if (connection == null || minecraft.player == null || minecraft.level == null) {
            return;
        }
        requireChannel(connection.hasChannel(ConfigRequestPayload.TYPE), "config request");
        requireChannel(connection.hasChannel(ConfigPatchPayload.TYPE), "config patch");
        requireChannel(connection.hasChannel(ConfigResetPayload.TYPE), "config reset");
        requireChannel(connection.hasChannel(ConfigSnapshotPayload.TYPE), "config snapshot");
        requireChannel(connection.hasChannel(ConfigInvalidationPayload.TYPE), "config invalidation");
        requireChannel(connection.hasChannel(ConfigMutationResultPayload.TYPE), "mutation result");

        minecraft.gui.setScreen(SmartDropsConfigScreens.create(null));
        transition(Phase.WAIT_READ_ONLY);
    }

    private void waitForReadOnlySnapshot(final Minecraft minecraft) {
        final Screen screen = minecraft.gui.screen();
        if (screen instanceof SmartDropsConfigLoadingScreen) {
            return;
        }
        if (!(screen instanceof SmartDropsConfigScreen root)) {
            return;
        }
        if (root.editorSession().editable()) {
            throw new AssertionError("Fresh dedicated-server player unexpectedly had operator config access");
        }
        root.onClose();
        transition(Phase.WAIT_PROMOTION);
    }

    private void waitForPromotion(final Minecraft minecraft) {
        if (++this.phaseTicks < PROMOTION_WAIT_TICKS) {
            return;
        }
        minecraft.gui.setScreen(SmartDropsConfigScreens.create(null));
        transition(Phase.WAIT_EDITABLE);
    }

    private void waitForEditableSnapshot(final Minecraft minecraft) {
        final Screen screen = minecraft.gui.screen();
        if (screen instanceof SmartDropsConfigLoadingScreen) {
            return;
        }
        if (!(screen instanceof SmartDropsConfigScreen root)) {
            return;
        }
        if (!root.editorSession().editable()) {
            root.onClose();
            transition(Phase.WAIT_PROMOTION);
            return;
        }

        final int current = root.editorSession().globalMultiplier();
        this.changedGlobalMultiplier = current == 7 ? 8 : 7;
        final ConfigPatch patch = new ConfigPatch();
        patch.globalMultiplier = this.changedGlobalMultiplier;
        minecraft.gui.setScreen(new SmartDropsConfigLoadingScreen(
                root,
                null,
                patch,
                root.editorSession().revision()));
        transition(Phase.WAIT_BASIC_PATCH);
    }

    private void waitForBasicPatch(final Minecraft minecraft) {
        final Screen screen = minecraft.gui.screen();
        if (screen instanceof SmartDropsConfigLoadingScreen) {
            return;
        }
        if (!(screen instanceof SmartDropsConfigScreen root)) {
            return;
        }
        if (root.editorSession().globalMultiplier() != this.changedGlobalMultiplier) {
            throw new AssertionError("Server-authoritative GUI patch was not acknowledged");
        }

        final ClientConfigState.CachedSnapshot snapshot = ClientConfigState.cachedSnapshot(minecraft)
                .orElseThrow(() -> new AssertionError("Basic-patch server snapshot was not cached"));
        final int remainingBlockRuleCapacity = SmartDropsConfig.MAX_BLOCK_RULE_ENTRIES
                - snapshot.config().blockRuleEntryCount();
        if (remainingBlockRuleCapacity <= 0) {
            throw new AssertionError("Default configuration left no capacity for a near-limit block patch");
        }

        final ConfigPatch nearLimit = new ConfigPatch();
        for (int index = 0; index < remainingBlockRuleCapacity; index++) {
            nearLimit.blockMultipliers.put("example:block_" + index, 2);
        }
        this.expectedNearLimitBlockMultipliers = snapshot.config().blockMultipliers.size()
                + remainingBlockRuleCapacity;
        minecraft.gui.setScreen(new SmartDropsConfigLoadingScreen(
                root,
                null,
                nearLimit,
                root.editorSession().revision()));
        transition(Phase.WAIT_NEAR_LIMIT_PATCH);
    }

    private void waitForNearLimitPatch(final Minecraft minecraft) {
        final Screen screen = minecraft.gui.screen();
        if (screen instanceof SmartDropsConfigLoadingScreen) {
            return;
        }
        if (!(screen instanceof SmartDropsConfigScreen root)) {
            return;
        }
        final ClientConfigState.CachedSnapshot snapshot = ClientConfigState.cachedSnapshot(minecraft)
                .orElseThrow(() -> new AssertionError("Near-limit server snapshot was not cached"));
        if (snapshot.config().blockRuleEntryCount() != SmartDropsConfig.MAX_BLOCK_RULE_ENTRIES
                || snapshot.config().blockMultipliers.size() != this.expectedNearLimitBlockMultipliers) {
            throw new AssertionError(
                    "Near-limit patch returned " + snapshot.config().blockRuleEntryCount()
                            + " total block rules and " + snapshot.config().blockMultipliers.size()
                            + " exact multipliers; expected " + SmartDropsConfig.MAX_BLOCK_RULE_ENTRIES
                            + " and " + this.expectedNearLimitBlockMultipliers);
        }

        final ConfigPatch oversized = new ConfigPatch();
        for (int index = 0; index <= ConfigPatch.MAX_COLLECTION_EDITS; index++) {
            oversized.blockMultipliers.put("example:oversized_" + index, 2);
        }
        final ClientConfigState.RequestStart rejected = ClientConfigState.submit(
                minecraft,
                snapshot.revision(),
                oversized);
        if (rejected.started() || rejected.failure() != ClientConfigState.StartFailure.INVALID_PATCH) {
            throw new AssertionError("Oversized client patch was not rejected before transport");
        }

        minecraft.gui.setScreen(SmartDropsConfigLoadingScreen.forReset(
                root,
                null,
                snapshot.revision()));
        transition(Phase.WAIT_RESET);
    }

    private void waitForReset(final Minecraft minecraft) {
        final Screen screen = minecraft.gui.screen();
        if (screen instanceof SmartDropsConfigLoadingScreen) {
            return;
        }
        if (!(screen instanceof SmartDropsConfigScreen root)) {
            return;
        }
        final SmartDropsConfig defaults = SmartDropsConfig.defaults();
        if (root.editorSession().globalMultiplier() != defaults.globalMultiplier
                || !root.editorSession().blockMultipliers().isEmpty()) {
            throw new AssertionError("Server-authoritative reset did not restore clean defaults");
        }

        this.phase = Phase.COMPLETE;
        SmartResourceDrops.LOGGER.info(
                "NeoForge multiplayer client smoke test passed: permissions, GUI authority, six channels, near-limit patch, oversized rejection, and reset");
        minecraft.stop();
    }

    private void transition(final Phase next) {
        this.phase = next;
        this.phaseTicks = 0;
    }

    private static void requireChannel(final boolean available, final String label) {
        if (!available) {
            throw new AssertionError("Negotiated NeoForge channel is unavailable: " + label);
        }
    }

    private enum Phase {
        WAIT_CONNECTION,
        WAIT_READ_ONLY,
        WAIT_PROMOTION,
        WAIT_EDITABLE,
        WAIT_BASIC_PATCH,
        WAIT_NEAR_LIMIT_PATCH,
        WAIT_RESET,
        COMPLETE
    }
}
