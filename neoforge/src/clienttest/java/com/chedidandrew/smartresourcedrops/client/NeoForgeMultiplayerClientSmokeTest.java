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
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.ClientCommandHandler;
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
    private int expectedNearLimitBlockMultipliers;
    private long initialRevision;
    private long connectedPatchRevision;
    private long nearLimitRevision;
    private int initialGlobalMultiplier;
    private Object firstConnectionIdentity;

    public NeoForgeMultiplayerClientSmokeTest() {
        if (Boolean.getBoolean("smart_resource_drops.multiplayerTest")
                && REGISTERED.compareAndSet(false, true)) {
            NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, this::onClientTick);
        }
    }

    private void onClientTick(final ClientTickEvent.Post event) {
        final Minecraft minecraft = Minecraft.getInstance();
        try {
            if (minecraft.getOverlay() != null) {
                return;
            }
            if (++this.ticks > TIMEOUT_TICKS) {
                throw new AssertionError("Timed out during NeoForge multiplayer smoke test at " + this.phase);
            }

            switch (this.phase) {
                case WAIT_CONNECTION -> waitForConnection(minecraft);
                case WAIT_READ_ONLY -> waitForReadOnlySnapshot(minecraft);
                case WAIT_UNAUTHORIZED -> waitForUnauthorizedResponse(minecraft);
                case WAIT_PROMOTION -> waitForPromotion(minecraft);
                case WAIT_EDITABLE -> waitForEditableSnapshot(minecraft);
                case WAIT_CONNECTED_GUI_PATCH -> waitForConnectedGuiPatch(minecraft);
                case WAIT_NEAR_LIMIT_PATCH -> waitForNearLimitPatch(minecraft);
                case WAIT_RESET -> waitForReset(minecraft);
                case WAIT_DISCONNECT -> waitForDisconnect(minecraft);
                case WAIT_RECONNECT -> waitForReconnect(minecraft);
                case WAIT_RECONNECTED_SNAPSHOT -> waitForReconnectedSnapshot(minecraft);
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

        requireClientCommandRoute();
        transition(Phase.WAIT_READ_ONLY);
    }

    private void waitForReadOnlySnapshot(final Minecraft minecraft) {
        final Screen screen = minecraft.screen;
        if (screen instanceof SmartDropsConfigLoadingScreen) {
            return;
        }
        if (!(screen instanceof SmartDropsConfigScreen root)) {
            return;
        }
        if (root.editorSession().editable()) {
            throw new AssertionError("Fresh dedicated-server player unexpectedly had operator config access");
        }
        if (root.resetButton().active || root.applyButton().active) {
            throw new AssertionError("Non-operator screen exposed mutation controls");
        }
        if (root.editorSession().setGlobalMultiplier(
                root.editorSession().globalMultiplier() + 1)) {
            throw new AssertionError("Non-operator editor staged a GUI mutation");
        }

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
                this.initialRevision));
        transition(Phase.WAIT_UNAUTHORIZED);
    }

    private void waitForUnauthorizedResponse(final Minecraft minecraft) {
        final Screen screen = minecraft.screen;
        if (screen instanceof SmartDropsConfigLoadingScreen) {
            return;
        }
        if (!(screen instanceof SmartDropsConfigScreen root)) {
            return;
        }
        if (root.editorSession().revision() != this.initialRevision
                || root.editorSession().globalMultiplier() != this.initialGlobalMultiplier
                || !root.editorSession().status().equals(Component.translatable(
                        "smart_resource_drops.gui.patch_unauthorized").getString())) {
            throw new AssertionError(
                    "Unauthorized NeoForge patch did not return the exact denial status without mutation");
        }
        root.onClose();
        transition(Phase.WAIT_PROMOTION);
    }

    private void waitForPromotion(final Minecraft minecraft) {
        if (++this.phaseTicks < PROMOTION_WAIT_TICKS) {
            return;
        }
        requireClientCommandRoute();
        transition(Phase.WAIT_EDITABLE);
    }

    private void waitForEditableSnapshot(final Minecraft minecraft) {
        final Screen screen = minecraft.screen;
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
        if (root.editorSession().revision() != this.initialRevision) {
            throw new AssertionError("Permission promotion unexpectedly changed the config revision");
        }

        stageConnectedChildScreenEdits(minecraft, root);
        if (!root.applyButton().active) {
            throw new AssertionError("Root Apply was not enabled by connected child-screen edits");
        }
        press(root.applyButton());
        transition(Phase.WAIT_CONNECTED_GUI_PATCH);
    }

    private void waitForConnectedGuiPatch(final Minecraft minecraft) {
        final Screen screen = minecraft.screen;
        if (screen instanceof SmartDropsConfigLoadingScreen) {
            return;
        }
        if (!(screen instanceof SmartDropsConfigScreen root)) {
            return;
        }
        final ConfigEditorSession session = root.editorSession();
        if (!session.entityDropsEnabled()
                || !Integer.valueOf(0).equals(session.entityMultiplier("minecraft:cow"))
                || session.entityFilterState("minecraft:cow")
                        != ConfigEditorSession.FilterEntryState.BLACKLIST
                || session.manualShearingDropsEnabled()
                || !Integer.valueOf(0).equals(session.defaultShearingMultiplier())
                || !Integer.valueOf(0).equals(
                        session.shearingEntityMultiplier("minecraft:sheep"))) {
            throw new AssertionError("Connected child-screen Apply was not acknowledged authoritatively");
        }
        if (session.revision() <= this.initialRevision) {
            throw new AssertionError("Connected child-screen Apply did not advance the server revision");
        }
        this.connectedPatchRevision = session.revision();

        final ClientConfigState.CachedSnapshot snapshot = ClientConfigState.cachedSnapshot(minecraft)
                .orElseThrow(() -> new AssertionError("Connected-GUI server snapshot was not cached"));
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
        minecraft.setScreen(new SmartDropsConfigLoadingScreen(
                root,
                null,
                nearLimit,
                root.editorSession().revision()));
        transition(Phase.WAIT_NEAR_LIMIT_PATCH);
    }

    private void waitForNearLimitPatch(final Minecraft minecraft) {
        final Screen screen = minecraft.screen;
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
        if (snapshot.revision() <= this.connectedPatchRevision) {
            throw new AssertionError("Near-limit patch did not advance the server revision");
        }
        this.nearLimitRevision = snapshot.revision();

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

        press(root.resetButton());
        final ResetAllSettingsConfirmScreen confirmation = requireScreen(
                minecraft,
                ResetAllSettingsConfirmScreen.class,
                "Reset All confirmation");
        press(buttonWithLabel(confirmation, "Reset Everything"));
        transition(Phase.WAIT_RESET);
    }

    private void waitForReset(final Minecraft minecraft) {
        final Screen screen = minecraft.screen;
        if (screen instanceof SmartDropsConfigLoadingScreen) {
            return;
        }
        if (!(screen instanceof SmartDropsConfigScreen root)) {
            return;
        }
        final SmartDropsConfig defaults = SmartDropsConfig.defaults();
        if (root.editorSession().globalMultiplier() != defaults.globalMultiplier
                || !root.editorSession().blockMultipliers().isEmpty()
                || root.editorSession().entityDropsEnabled()
                || root.editorSession().entityMultiplier("minecraft:cow") != null
                || root.editorSession().entityFilterState("minecraft:cow")
                        != ConfigEditorSession.FilterEntryState.NONE
                || !root.editorSession().manualShearingDropsEnabled()
                || root.editorSession().defaultShearingMultiplier() != null
                || root.editorSession().shearingEntityMultiplier("minecraft:sheep") != null) {
            throw new AssertionError(
                    "Server-authoritative Reset confirmation did not restore all child-screen defaults");
        }
        if (root.editorSession().revision() <= this.nearLimitRevision) {
            throw new AssertionError("Server-authoritative Reset did not advance the revision");
        }

        if (ClientConfigState.cachedSnapshot(minecraft).isEmpty()) {
            throw new AssertionError("Reset response did not leave an authoritative first-session snapshot");
        }
        this.firstConnectionIdentity = ClientConfigState.connectionIdentity(minecraft);
        if (this.firstConnectionIdentity == null) {
            throw new AssertionError("First-session connection identity disappeared before disconnect");
        }

        // Begin another real server request, then tear down that exact connection.
        // The NeoForge LoggingOut adapter must invalidate both the request generation
        // and cached authority state before a new connection is allowed to reuse them.
        minecraft.setScreen(new SmartDropsConfigLoadingScreen(root));
        minecraft.disconnect(new TitleScreen(), false);
        transition(Phase.WAIT_DISCONNECT);
    }

    private void waitForDisconnect(final Minecraft minecraft) {
        if (minecraft.getConnection() != null || minecraft.player != null || minecraft.level != null) {
            return;
        }
        if (ClientConfigState.cachedSnapshot(minecraft).isPresent()) {
            throw new AssertionError("First-session config snapshot survived disconnect");
        }
        if (++this.phaseTicks < 20) {
            return;
        }

        final String address = "127.0.0.1:25578";
        ConnectScreen.startConnecting(
                new TitleScreen(),
                minecraft,
                ServerAddress.parseString(address),
                new ServerData("Smart Resource Multiplier reconnect smoke", address, ServerData.Type.OTHER),
                false,
                null);
        transition(Phase.WAIT_RECONNECT);
    }

    private void waitForReconnect(final Minecraft minecraft) {
        final var connection = minecraft.getConnection();
        if (connection == null || minecraft.player == null || minecraft.level == null) {
            return;
        }
        final Object reconnectedIdentity = ClientConfigState.connectionIdentity(minecraft);
        if (reconnectedIdentity == null || reconnectedIdentity == this.firstConnectionIdentity) {
            throw new AssertionError("Reconnect reused the first physical connection identity");
        }
        requireAllChannels(connection);
        if (ClientConfigState.cachedSnapshot(minecraft).isPresent()) {
            throw new AssertionError("Reconnect exposed a cached snapshot from the first connection");
        }

        minecraft.setScreen(SmartDropsConfigScreens.create(null));
        transition(Phase.WAIT_RECONNECTED_SNAPSHOT);
    }

    private void waitForReconnectedSnapshot(final Minecraft minecraft) {
        final Screen screen = minecraft.screen;
        if (screen instanceof SmartDropsConfigLoadingScreen) {
            return;
        }
        if (!(screen instanceof SmartDropsConfigScreen root)) {
            return;
        }
        if (!root.editorSession().editable()) {
            throw new AssertionError("Reconnected operator did not receive an editable authoritative snapshot");
        }
        final SmartDropsConfig defaults = SmartDropsConfig.defaults();
        if (root.editorSession().globalMultiplier() != defaults.globalMultiplier
                || !root.editorSession().blockMultipliers().isEmpty()) {
            throw new AssertionError("Reconnected snapshot did not preserve the server reset state");
        }
        if (ClientConfigState.cachedSnapshot(minecraft).isEmpty()) {
            throw new AssertionError("Reconnected authoritative snapshot was not cached");
        }

        this.phase = Phase.COMPLETE;
        SmartResourceDrops.LOGGER.info(
                "NeoForge multiplayer client smoke test passed: /smartdropsgui, non-op denial, operator connected entity/filter/shearing child Apply, six channels, near-limit patch, oversized rejection, confirmed reset, disconnect cleanup, and reconnect");
        minecraft.stop();
    }

    private static void stageConnectedChildScreenEdits(
            final Minecraft minecraft,
            final SmartDropsConfigScreen root
    ) {
        press(buttonWithLabel(root, "Entity Drops"));
        EntityDropsScreen entityDrops = requireScreen(
                minecraft,
                EntityDropsScreen.class,
                "Entity Drops root child");
        rowWithPrimary(onlyList(entityDrops), "Entity Drops").action().run();
        entityDrops = requireScreen(minecraft, EntityDropsScreen.class, "enabled Entity Drops child");
        if (!root.editorSession().entityDropsEnabled()) {
            throw new AssertionError("Entity Drops row did not stage through the connected screen");
        }

        rowWithPrimary(onlyList(entityDrops), "Entity Overrides").action().run();
        final EntityOverridesScreen overrides = requireScreen(
                minecraft,
                EntityOverridesScreen.class,
                "Entity Overrides child");
        onlySearchBox(overrides).setValue("minecraft:cow");
        rowWithSecondary(onlyList(overrides), "minecraft:cow").action().run();
        final EntityRuleEditScreen cowEditor = requireScreen(
                minecraft,
                EntityRuleEditScreen.class,
                "Cow entity override editor");
        press(buttonWithLabel(cowEditor, "+"));
        if (!Integer.valueOf(0).equals(root.editorSession().entityMultiplier("minecraft:cow"))) {
            throw new AssertionError("Cow editor increment did not stage an exact 0x override");
        }
        press(buttonWithLabel(cowEditor, "Back"));
        press(buttonWithLabel(requireScreen(
                minecraft,
                EntityOverridesScreen.class,
                "Entity Overrides after Cow edit"), "Back"));
        entityDrops = requireScreen(minecraft, EntityDropsScreen.class, "Entity Drops after override");

        rowWithPrimary(onlyList(entityDrops), "Entity Filters").action().run();
        final EntityFilterScreen filters = requireScreen(
                minecraft,
                EntityFilterScreen.class,
                "Entity Filters child");
        onlySearchBox(filters).setValue("minecraft:cow");
        rowWithSecondary(onlyList(filters), "minecraft:cow").action().run();
        if (root.editorSession().entityFilterState("minecraft:cow")
                != ConfigEditorSession.FilterEntryState.BLACKLIST) {
            throw new AssertionError("Cow filter row did not stage the active blacklist state");
        }
        press(buttonWithLabel(filters, "Back"));
        entityDrops = requireScreen(minecraft, EntityDropsScreen.class, "Entity Drops after filter");

        rowWithPrimary(onlyList(entityDrops), "Shearing Drops").action().run();
        ShearingDropsScreen shearing = requireScreen(
                minecraft,
                ShearingDropsScreen.class,
                "Shearing Drops child");
        rowWithPrimary(onlyList(shearing), "Manual Shearing").action().run();
        shearing = requireScreen(minecraft, ShearingDropsScreen.class, "updated Shearing Drops child");
        if (root.editorSession().manualShearingDropsEnabled()) {
            throw new AssertionError("Manual Shearing row did not stage OFF");
        }

        rowWithPrimary(onlyList(shearing), "Default Shearing Multiplier").action().run();
        final ShearingRuleEditScreen defaultShearing = requireScreen(
                minecraft,
                ShearingRuleEditScreen.class,
                "Default Shearing editor");
        press(buttonWithLabel(defaultShearing, "+"));
        if (!Integer.valueOf(0).equals(root.editorSession().defaultShearingMultiplier())) {
            throw new AssertionError("Default Shearing editor did not stage 0x");
        }
        press(buttonWithLabel(defaultShearing, "Back"));
        shearing = requireScreen(minecraft, ShearingDropsScreen.class, "Shearing Drops after default");

        rowWithPrimary(onlyList(shearing), "Shearing Entity Overrides").action().run();
        final ShearingOverridesScreen shearingOverrides = requireScreen(
                minecraft,
                ShearingOverridesScreen.class,
                "Shearing Entity Overrides child");
        onlySearchBox(shearingOverrides).setValue("minecraft:sheep");
        rowWithSecondary(onlyList(shearingOverrides), "minecraft:sheep").action().run();
        final ShearingRuleEditScreen sheepEditor = requireScreen(
                minecraft,
                ShearingRuleEditScreen.class,
                "Sheep shearing override editor");
        press(buttonWithLabel(sheepEditor, "+"));
        if (!Integer.valueOf(0).equals(
                root.editorSession().shearingEntityMultiplier("minecraft:sheep"))) {
            throw new AssertionError("Sheep editor increment did not stage an exact 0x override");
        }
        press(buttonWithLabel(sheepEditor, "Back"));
        press(buttonWithLabel(requireScreen(
                minecraft,
                ShearingOverridesScreen.class,
                "Shearing overrides after Sheep edit"), "Back"));
        press(buttonWithLabel(requireScreen(
                minecraft,
                ShearingDropsScreen.class,
                "Shearing Drops after Sheep edit"), "Back"));
        press(buttonWithLabel(requireScreen(
                minecraft,
                EntityDropsScreen.class,
                "Entity Drops after Shearing edit"), "Back"));
        requireScreen(minecraft, SmartDropsConfigScreen.class, "General after child edits");
        if (!root.editorSession().isDirty()) {
            throw new AssertionError("Connected child-screen edits did not dirty the shared session");
        }
    }

    private static void requireClientCommandRoute() {
        if (!ClientCommandHandler.runCommand("smartdropsgui")) {
            throw new AssertionError("NeoForge did not register the production /smartdropsgui command");
        }
    }

    private static StructuredConfigList onlyList(final Screen screen) {
        final List<StructuredConfigList> lists = screen.children().stream()
                .filter(StructuredConfigList.class::isInstance)
                .map(StructuredConfigList.class::cast)
                .toList();
        if (lists.size() != 1) {
            throw new AssertionError(
                    "Expected one structured list on " + screen.getClass().getSimpleName()
                            + ", found " + lists.size());
        }
        return lists.getFirst();
    }

    private static EditBox onlySearchBox(final Screen screen) {
        final List<EditBox> searches = screen.children().stream()
                .filter(EditBox.class::isInstance)
                .map(EditBox.class::cast)
                .toList();
        if (searches.size() != 1) {
            throw new AssertionError(
                    "Expected one search box on " + screen.getClass().getSimpleName()
                            + ", found " + searches.size());
        }
        return searches.getFirst();
    }

    private static StructuredConfigList.Row rowWithPrimary(
            final StructuredConfigList list,
            final String primary
    ) {
        return list.rows().stream()
                .filter(row -> primary.equals(row.primary().getString()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing structured row: " + primary));
    }

    private static StructuredConfigList.Row rowWithSecondary(
            final StructuredConfigList list,
            final String secondary
    ) {
        return list.rows().stream()
                .filter(row -> secondary.equals(row.secondary().getString()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing structured row ID: " + secondary));
    }

    private static Button buttonWithLabel(final Screen screen, final String label) {
        return screen.children().stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .filter(button -> label.equals(button.getMessage().getString()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Missing button '" + label + "' on " + screen.getClass().getSimpleName()));
    }

    private static void press(final Button button) {
        button.onPress(new KeyEvent(257, 0, 0));
    }

    private static <T extends Screen> T requireScreen(
            final Minecraft minecraft,
            final Class<T> type,
            final String label
    ) {
        final Screen screen = minecraft.screen;
        if (!type.isInstance(screen)) {
            throw new AssertionError(
                    label + " expected " + type.getSimpleName() + ", found "
                            + (screen == null ? "null" : screen.getClass().getName()));
        }
        return type.cast(screen);
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

    private static void requireAllChannels(final net.minecraft.client.multiplayer.ClientPacketListener connection) {
        requireChannel(connection.hasChannel(ConfigRequestPayload.TYPE), "config request");
        requireChannel(connection.hasChannel(ConfigPatchPayload.TYPE), "config patch");
        requireChannel(connection.hasChannel(ConfigResetPayload.TYPE), "config reset");
        requireChannel(connection.hasChannel(ConfigSnapshotPayload.TYPE), "config snapshot");
        requireChannel(connection.hasChannel(ConfigInvalidationPayload.TYPE), "config invalidation");
        requireChannel(connection.hasChannel(ConfigMutationResultPayload.TYPE), "mutation result");
    }

    private enum Phase {
        WAIT_CONNECTION,
        WAIT_READ_ONLY,
        WAIT_UNAUTHORIZED,
        WAIT_PROMOTION,
        WAIT_EDITABLE,
        WAIT_CONNECTED_GUI_PATCH,
        WAIT_NEAR_LIMIT_PATCH,
        WAIT_RESET,
        WAIT_DISCONNECT,
        WAIT_RECONNECT,
        WAIT_RECONNECTED_SNAPSHOT,
        COMPLETE
    }
}
