package com.chedidandrew.smartresourcedrops.client;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.config.ConfigManager;
import com.chedidandrew.smartresourcedrops.config.ConfigPatch;
import com.chedidandrew.smartresourcedrops.config.ConfigScreenOpenPolicy;
import com.chedidandrew.smartresourcedrops.config.SmartDropsConfig;
import com.chedidandrew.smartresourcedrops.network.ConfigPatchPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigRequestPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigResetPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/** Real separate-process Fabric server authority, mutation, reset, and reconnect gate. */
public final class FabricMultiplayerClientSmokeTest implements ClientModInitializer {
    private static final int TIMEOUT_TICKS = 6_000;
    private static final int PROMOTION_WAIT_TICKS = 180;
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    private Phase phase = Phase.WAIT_CONNECTION;
    private int ticks;
    private int phaseTicks;
    private long initialRevision;
    private int initialGlobalMultiplier;
    private int patchedGlobal;
    private Object firstConnectionIdentity;
    private boolean stopped;

    @Override
    public void onInitializeClient() {
        if (Boolean.getBoolean("smart_resource_drops.fabricMultiplayerSmoke")
                && REGISTERED.compareAndSet(false, true)) {
            ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        }
    }

    private void onClientTick(final Minecraft minecraft) {
        if (this.stopped || minecraft.getOverlay() != null) {
            return;
        }
        try {
            if (++this.ticks > TIMEOUT_TICKS) {
                throw new AssertionError(
                        "Timed out during Fabric multiplayer smoke at " + this.phase);
            }
            switch (this.phase) {
                case WAIT_CONNECTION -> this.waitForConnection(minecraft);
                case WAIT_READ_ONLY -> this.waitForReadOnlySnapshot(minecraft);
                case WAIT_UNAUTHORIZED -> this.waitForUnauthorizedResponse(minecraft);
                case WAIT_PROMOTION -> this.waitForPromotion(minecraft);
                case WAIT_EDITABLE -> this.waitForEditableSnapshot(minecraft);
                case WAIT_PATCH -> this.waitForPatch(minecraft);
                case WAIT_RESET -> this.waitForReset(minecraft);
                case WAIT_DISCONNECT -> this.waitForDisconnect(minecraft);
                case WAIT_RECONNECT -> this.waitForReconnect(minecraft);
                case WAIT_RECONNECTED_SNAPSHOT -> this.waitForReconnectedSnapshot(minecraft);
                case COMPLETE -> {
                    // Client is stopping.
                }
            }
        } catch (Throwable failure) {
            this.stopped = true;
            SmartResourceDrops.LOGGER.error("Fabric multiplayer client smoke failed", failure);
            minecraft.stop();
        }
    }

    private void waitForConnection(final Minecraft minecraft) {
        if (minecraft.getConnection() == null || minecraft.player == null || minecraft.level == null) {
            return;
        }
        require(ClientPlayNetworking.canSend(ConfigRequestPayload.TYPE),
                "Config request channel was not negotiated");
        require(ClientPlayNetworking.canSend(ConfigPatchPayload.TYPE),
                "Config patch channel was not negotiated");
        require(ClientPlayNetworking.canSend(ConfigResetPayload.TYPE),
                "Config reset channel was not negotiated");
        minecraft.setScreen(SmartDropsConfigScreens.create(null));
        transition(Phase.WAIT_READ_ONLY);
    }

    private void waitForReadOnlySnapshot(final Minecraft minecraft) {
        if (minecraft.screen instanceof SmartDropsConfigLoadingScreen) {
            return;
        }
        if (!(minecraft.screen instanceof SmartDropsConfigScreen root)) {
            return;
        }
        final ConfigEditorSession session = root.editorSession();
        require(session.authority() == ConfigScreenOpenPolicy.Authority.CONNECTED_SERVER,
                "Dedicated-server snapshot used non-server authority");
        require(!session.editable(),
                "Fresh dedicated-server player unexpectedly received operator access");
        require(!root.resetButton().active && !root.applyButton().active,
                "Non-operator screen exposed mutation controls");
        require(!session.setGlobalMultiplier(session.globalMultiplier() + 1) && !session.isDirty(),
                "Non-operator staged a GUI mutation");

        final ClientConfigState.CachedSnapshot snapshot = ClientConfigState.cachedSnapshot(minecraft)
                .orElseThrow(() -> new AssertionError("Read-only server snapshot was not cached"));
        this.initialRevision = snapshot.revision();
        this.initialGlobalMultiplier = snapshot.config().globalMultiplier;
        final ConfigPatch unauthorized = new ConfigPatch();
        unauthorized.globalMultiplier = this.initialGlobalMultiplier + 1;
        minecraft.setScreen(new SmartDropsConfigLoadingScreen(
                root,
                null,
                unauthorized,
                snapshot.revision()));
        transition(Phase.WAIT_UNAUTHORIZED);
    }

    private void waitForUnauthorizedResponse(final Minecraft minecraft) {
        if (minecraft.screen instanceof SmartDropsConfigLoadingScreen) {
            return;
        }
        if (!(minecraft.screen instanceof SmartDropsConfigScreen root)) {
            return;
        }
        require(root.editorSession().revision() == this.initialRevision,
                "Unauthorized patch changed the authoritative revision");
        require(root.editorSession().globalMultiplier()
                        == this.initialGlobalMultiplier,
                "Unauthorized patch changed server configuration");
        require(root.editorSession().status().equals(Component.translatable(
                        "smart_resource_drops.gui.patch_unauthorized").getString()),
                "Unauthorized patch did not return the exact denial status");
        root.onClose();
        transition(Phase.WAIT_PROMOTION);
    }

    private void waitForPromotion(final Minecraft minecraft) {
        if (++this.phaseTicks < PROMOTION_WAIT_TICKS) {
            return;
        }
        // Permission changes do not mutate config and therefore do not publish a config
        // invalidation. Force a fresh production request across the same connection.
        ClientConfigState.invalidatePendingMutations();
        minecraft.setScreen(SmartDropsConfigScreens.create(null));
        transition(Phase.WAIT_EDITABLE);
    }

    private void waitForEditableSnapshot(final Minecraft minecraft) {
        if (minecraft.screen instanceof SmartDropsConfigLoadingScreen) {
            return;
        }
        if (!(minecraft.screen instanceof SmartDropsConfigScreen root)) {
            return;
        }
        if (!root.editorSession().editable()) {
            root.onClose();
            transition(Phase.WAIT_PROMOTION);
            return;
        }
        require(root.editorSession().authority()
                        == ConfigScreenOpenPolicy.Authority.CONNECTED_SERVER,
                "Promoted snapshot used non-server authority");
        final int current = root.editorSession().globalMultiplier();
        this.patchedGlobal = current < root.editorSession().maximumMultiplier()
                ? current + 1
                : current - 1;
        require(root.editorSession().setGlobalMultiplier(this.patchedGlobal),
                "Operator could not stage a connected change");
        minecraft.setScreen(root);
        require(root.applyButton().active,
                "Operator dirty draft did not enable Apply");
        press(root.applyButton());
        transition(Phase.WAIT_PATCH);
    }

    private void waitForPatch(final Minecraft minecraft) {
        if (minecraft.screen instanceof SmartDropsConfigLoadingScreen) {
            return;
        }
        if (!(minecraft.screen instanceof SmartDropsConfigScreen root)) {
            return;
        }
        require(root.editorSession().editable(),
                "Operator lost editability after successful Apply");
        require(root.editorSession().globalMultiplier() == this.patchedGlobal,
                "Server did not acknowledge the operator patch");
        require(root.editorSession().revision() > this.initialRevision,
                "Operator patch did not advance the server revision");
        require(!root.editorSession().isDirty(),
                "Acknowledged operator patch returned a dirty editor");

        press(root.resetButton());
        require(minecraft.screen instanceof ResetAllSettingsConfirmScreen,
                "Connected Reset did not open the confirmation gate");
        press(buttonWithLabel(minecraft.screen, "Reset Everything"));
        transition(Phase.WAIT_RESET);
    }

    private void waitForReset(final Minecraft minecraft) {
        if (minecraft.screen instanceof SmartDropsConfigLoadingScreen) {
            return;
        }
        if (!(minecraft.screen instanceof SmartDropsConfigScreen root)) {
            return;
        }
        final SmartDropsConfig defaults = SmartDropsConfig.defaults();
        require(root.editorSession().globalMultiplier() == defaults.globalMultiplier,
                "Server-authoritative Reset did not restore defaults");
        require(root.editorSession().revision() > this.initialRevision + 1,
                "Server-authoritative Reset did not advance revision after Apply");
        require(root.editorSession().editable() && !root.editorSession().isDirty(),
                "Reset acknowledgement did not return a clean operator editor");
        require(ClientConfigState.cachedSnapshot(minecraft).isPresent(),
                "Reset response did not cache its authoritative snapshot");

        this.firstConnectionIdentity = ClientConfigState.connectionIdentity(minecraft);
        require(this.firstConnectionIdentity != null,
                "First connection identity disappeared before disconnect");
        minecraft.setScreen(new SmartDropsConfigLoadingScreen(root));
        minecraft.disconnect(new TitleScreen(), false);
        transition(Phase.WAIT_DISCONNECT);
    }

    private void waitForDisconnect(final Minecraft minecraft) {
        if (minecraft.getConnection() != null || minecraft.player != null || minecraft.level != null) {
            return;
        }
        require(ClientConfigState.cachedSnapshot(minecraft).isEmpty(),
                "Authoritative snapshot survived physical disconnect");
        if (++this.phaseTicks < 20) {
            return;
        }
        final String address = "127.0.0.1:25577";
        ConnectScreen.startConnecting(
                new TitleScreen(),
                minecraft,
                ServerAddress.parseString(address),
                new ServerData(
                        "Smart Resource Multiplier Fabric reconnect smoke",
                        address,
                        ServerData.Type.OTHER),
                false,
                null);
        transition(Phase.WAIT_RECONNECT);
    }

    private void waitForReconnect(final Minecraft minecraft) {
        if (minecraft.getConnection() == null || minecraft.player == null || minecraft.level == null) {
            return;
        }
        final Object connection = ClientConfigState.connectionIdentity(minecraft);
        require(connection != null && connection != this.firstConnectionIdentity,
                "Reconnect reused the first physical connection identity");
        require(ClientConfigState.cachedSnapshot(minecraft).isEmpty(),
                "Reconnect exposed the first connection's cached snapshot");
        require(ClientPlayNetworking.canSend(ConfigRequestPayload.TYPE)
                        && ClientPlayNetworking.canSend(ConfigPatchPayload.TYPE)
                        && ClientPlayNetworking.canSend(ConfigResetPayload.TYPE),
                "Reconnect did not renegotiate every C2S config channel");
        minecraft.setScreen(SmartDropsConfigScreens.create(null));
        transition(Phase.WAIT_RECONNECTED_SNAPSHOT);
    }

    private void waitForReconnectedSnapshot(final Minecraft minecraft) {
        if (minecraft.screen instanceof SmartDropsConfigLoadingScreen) {
            return;
        }
        if (!(minecraft.screen instanceof SmartDropsConfigScreen root)) {
            return;
        }
        require(root.editorSession().editable(),
                "Reconnected operator did not receive edit permission");
        require(root.editorSession().globalMultiplier()
                        == SmartDropsConfig.defaults().globalMultiplier,
                "Reconnected snapshot did not preserve reset state");
        require(ClientConfigState.cachedSnapshot(minecraft).isPresent(),
                "Reconnected authoritative snapshot was not cached");

        this.phase = Phase.COMPLETE;
        this.stopped = true;
        SmartResourceDrops.LOGGER.info(
                "Fabric multiplayer client smoke passed: real non-op denial, operator Apply/Reset revisions, disconnect cleanup, channel renegotiation, and reconnect");
        minecraft.stop();
    }

    private void transition(final Phase next) {
        this.phase = next;
        this.phaseTicks = 0;
    }

    private static Button buttonWithLabel(final Screen screen, final String label) {
        return screen.children().stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .filter(button -> label.equals(button.getMessage().getString()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        screen.getClass().getSimpleName() + " omitted button " + label));
    }

    private static void press(final Button button) {
        button.onPress();
    }

    private static void require(final boolean condition, final String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private enum Phase {
        WAIT_CONNECTION,
        WAIT_READ_ONLY,
        WAIT_UNAUTHORIZED,
        WAIT_PROMOTION,
        WAIT_EDITABLE,
        WAIT_PATCH,
        WAIT_RESET,
        WAIT_DISCONNECT,
        WAIT_RECONNECT,
        WAIT_RECONNECTED_SNAPSHOT,
        COMPLETE
    }
}
