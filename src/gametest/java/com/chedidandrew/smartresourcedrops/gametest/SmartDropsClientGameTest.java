package com.chedidandrew.smartresourcedrops.gametest;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.chedidandrew.smartresourcedrops.client.ConfigEditorSession;
import com.chedidandrew.smartresourcedrops.client.ResetAllSettingsConfirmScreen;
import com.chedidandrew.smartresourcedrops.client.SmartDropsConfigScreen;
import com.chedidandrew.smartresourcedrops.client.SmartDropsConfigScreens;
import com.chedidandrew.smartresourcedrops.client.SmartDropsSubScreen;
import com.chedidandrew.smartresourcedrops.client.StructuredConfigList;
import com.chedidandrew.smartresourcedrops.config.ConfigManager;
import com.chedidandrew.smartresourcedrops.config.ConfigPatch;
import com.chedidandrew.smartresourcedrops.config.ConfigScreenOpenPolicy;
import com.chedidandrew.smartresourcedrops.config.SmartDropsConfig;
import com.chedidandrew.smartresourcedrops.core.Category;
import com.chedidandrew.smartresourcedrops.core.entity.EntityCategory;
import com.chedidandrew.smartresourcedrops.core.shearing.ShearingClassification;
import com.chedidandrew.smartresourcedrops.network.ConfigPatchPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigResetPayload;
import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarrationThunk;
import net.minecraft.client.gui.screens.inventory.tooltip.MenuTooltipPositioner;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Vector2ic;

/** End-to-end GUI and authority checks in real Minecraft client/server runtimes. */
@SuppressWarnings("UnstableApiUsage")
public final class SmartDropsClientGameTest implements FabricClientGameTest {
    private static final int TEST_FRAMERATE_LIMIT = 20;
    private static final int TEST_RENDER_DISTANCE = 2;
    private static final int TEST_SIMULATION_DISTANCE = 5;
    private static final double TEST_ENTITY_DISTANCE_SCALE = 0.5D;
    private static final int BLOCK_RESULT_LIMIT = 200;
    private static final int VANILLA_TOOLTIP_WIDTH = 170;
    private static final String APPLY_KEY = "Apply Changes";
    private static final String BACK_KEY = "Back";
    private static final String DONE_KEY = "Done";
    private static final String DISCARD_CHANGES_KEY = "Discard Changes";
    private static final String CATEGORIES_KEY = "Block Categories";
    private static final String BLOCK_OVERRIDES_KEY = "Block Overrides";
    private static final String DIMENSIONS_KEY = "Dimensions";
    private static final String FILTERS_KEY = "Block Filters";
    private static final String ADVANCED_KEY = "Advanced";
    private static final String ENTITY_DROPS_KEY = "Entity Drops";
    private static final String SHEARING_DROPS_KEY = "Shearing Drops";
    private static final String SHEARING_OVERRIDES_KEY = "Shearing Entity Overrides";
    private static final String RESET_KEY = "Reset All Settings";
    private static final String RESET_EVERYTHING_KEY = "Reset Everything";
    private static final String CANCEL_KEY = "Cancel";
    private static final String GLOBAL_MULTIPLIER_TOOLTIP =
            "Default multiplier used when no more specific override exists.";
    private static final String MULTIPLY_XP_TOOLTIP =
            "Multiply XP produced by eligible block breaks. Mob XP is configured separately under Entity Drops.";
    private static final String XP_MULTIPLIER_TOOLTIP =
            "Sets the multiplier for XP produced by eligible block breaks. Mob XP uses its own multiplier under Entity Drops.";
    private static final Map<String, String> ROOT_NAVIGATION_TOOLTIPS = Map.of(
            DIMENSIONS_KEY,
            "Configure block-drop multipliers for specific dimensions.",
            ADVANCED_KEY,
            "Configure presets, block sources, block-entity safety, piston handling, personal overrides, and runtime statistics.",
            ENTITY_DROPS_KEY,
            "Configure entity death loot, mob XP, and supported entity shearing. These settings are separate from block drops.");
    private static final Set<String> ROOT_NAVIGATION_WITHOUT_TOOLTIPS = Set.of(
            CATEGORIES_KEY,
            BLOCK_OVERRIDES_KEY,
            FILTERS_KEY);
    private static final Map<String, String> ADVANCED_TOOLTIPS = Map.of(
            "Mod Enabled",
            "Master switch for Smart Resource Multiplier. Saved settings remain unchanged while multiplication is disabled.",
            "Player Mining",
            "Allow eligible block drops caused by player mining to use block multipliers.",
            "Explosion Drops",
            "Allow eligible block drops caused by explosions to use block multipliers.",
            "Automated Mining",
            "Allow supported non-player Block.dropResources paths to use block multipliers. Systems that create or insert items directly remain unchanged.",
            "Protect Block Entities",
            "Keep blocks with block entities at vanilla 1x unless explicitly allowlisted, protecting inventories and special data.",
            "Conservative Piston Safety",
            "Treat piston-moved destination blocks as protected so placement provenance cannot be lost or exploited.",
            "Allow Player Overrides",
            "Allow players to use personal block multipliers within the limits configured by the server.",
            "Collect Session Statistics",
            "Track block-multiplication activity in memory for the current server session. This does not change drop behavior.");
    private static final Map<String, String> PRESET_TOOLTIPS = Map.of(
            "Vanilla Plus",
            "Vanilla Plus\nGlobal 1x; Ores and Logs 2x.",
            "Faster Survival",
            "Faster Survival\nGlobal 2x; Logs 3x; Ores, Stone, and Crops 2x.",
            "Fast Progression",
            "Fast Progression\nGlobal 4x with no category, block, or dimension overrides.");
    private static final Set<String> ROOT_NAVIGATION_LABELS = Set.of(
            CATEGORIES_KEY,
            BLOCK_OVERRIDES_KEY,
            DIMENSIONS_KEY,
            FILTERS_KEY,
            ADVANCED_KEY,
            ENTITY_DROPS_KEY,
            DONE_KEY);

    @Override
    public void runTest(final ClientGameTestContext context) {
        configureResourceEfficientClientOptions(context);
        verifyIntegratedServerOwnerAndApply(context);
        verifyTitleScreenLocalDefaults(context);
        verifyDedicatedServerPermissions(context);
    }

    private static void configureResourceEfficientClientOptions(final ClientGameTestContext context) {
        context.runOnClient(client -> {
            // Xvfb uses CPU-backed rendering on GitHub-hosted runners. Keep enough CPU available
            // for the integrated server while preserving every GUI and authority assertion.
            client.options.framerateLimit().set(TEST_FRAMERATE_LIMIT);
            client.options.renderDistance().set(TEST_RENDER_DISTANCE);
            client.options.simulationDistance().set(TEST_SIMULATION_DISTANCE);
            client.options.enableVsync().set(false);
            client.options.cloudStatus().set(CloudStatus.OFF);
            client.options.particles().set(ParticleStatus.MINIMAL);
            client.options.entityDistanceScaling().set(TEST_ENTITY_DISTANCE_SCALE);
            client.options.entityShadows().set(false);
            client.options.ambientOcclusion().set(false);
            client.options.biomeBlendRadius().set(0);
            client.options.menuBackgroundBlurriness().set(0);
        });
    }

    private static void verifyTitleScreenLocalDefaults(final ClientGameTestContext context) {
        context.setScreen(() -> SmartDropsConfigScreens.create(null));
        context.waitForScreen(SmartDropsConfigScreen.class);
        SmartDropsConfigScreen root = currentConfigScreen(context);
        assertGeneralRoot(root, true);
        assertRootClarityTooltips(context, root);
        assertRootTooltipLayouts(context, root);
        assertXpControlsDisabledWhileOff(context, root);
        assertGlobalDirtyRoundTrip(context, root);
        assertCommonControlRoundTrips(context, root);
        assertRuntimeCatalog(root.editorSession());
        verifyShearingSessionModels(context);
        takeRequiredScreenshot(context, "smart-drops-title-general");
        verifyNavigationAndScreenshots(context, root);
        root = verifyUnsavedChangesIndicatorAndDiscard(context, root);
        verifyLocalResetConfirmation(context, root);
        context.setScreen(() -> null);
    }

    private static void verifyShearingSessionModels(final ClientGameTestContext context) {
        context.runOnClient(client -> verifyShearingSessionModelsOnClient());
    }

    private static void verifyShearingSessionModelsOnClient() {
        final SmartDropsConfig fresh = SmartDropsConfig.defaults();
        final ConfigEditorSession freshSession = new ConfigEditorSession(
                null,
                fresh,
                true,
                "",
                ConfigScreenOpenPolicy.Authority.LOCAL_DEFAULTS,
                0L);
        require(freshSession.manualShearingDropsEnabled(),
                "Fresh client session did not display Manual Shearing ON");
        require(!freshSession.automatedShearingDropsEnabled(),
                "Fresh client session did not display Automated Shearing OFF");
        require(freshSession.defaultShearingMultiplier() == null
                        && freshSession.effectiveDefaultShearingMultiplier()
                                == freshSession.globalMultiplier(),
                "Fresh client session did not display shearing inheritance from Global");
        require(freshSession.setManualShearingDropsEnabled(false)
                        && freshSession.setAutomatedShearingDropsEnabled(true)
                        && freshSession.setDefaultShearingMultiplier(3)
                        && freshSession.setShearingEntityMultiplier("minecraft:sheep", 4),
                "Fresh client session could not stage every shearing setting kind");
        final ConfigPatch shearingPatch = freshSession.buildPatch();
        require(Boolean.FALSE.equals(shearingPatch.manualShearingDropsEnabled)
                        && Boolean.TRUE.equals(shearingPatch.automatedShearingDropsEnabled)
                        && Boolean.FALSE.equals(shearingPatch.inheritDefaultShearingMultiplier)
                        && Integer.valueOf(3).equals(shearingPatch.defaultShearingMultiplier)
                        && Integer.valueOf(4).equals(
                                shearingPatch.shearingEntityMultipliers.get("minecraft:sheep")),
                "Shared editor did not build a complete atomic shearing patch");

        final SmartDropsConfig migrated = SmartDropsConfig.defaults();
        migrated.manualShearingDropsEnabled = false;
        migrated.automatedShearingDropsEnabled = false;
        migrated.shearingEntityMultipliers.clear();
        final ConfigEditorSession migratedSession = new ConfigEditorSession(
                null,
                migrated,
                true,
                "",
                ConfigScreenOpenPolicy.Authority.LOCAL_DEFAULTS,
                0L);
        require(!migratedSession.manualShearingDropsEnabled()
                        && !migratedSession.automatedShearingDropsEnabled()
                        && migratedSession.defaultShearingMultiplier() == null,
                "Migrated snapshot values were not represented safely by the shearing editor");

        final SmartDropsConfig unsafe = SmartDropsConfig.defaults();
        unsafe.shearingEntityMultipliers.put("example:unknown_shearable", 4);
        unsafe.shearingEntityMultipliers.put("minecraft:mooshroom", 4);
        final ConfigEditorSession safetySession = new ConfigEditorSession(
                null,
                unsafe,
                true,
                "",
                ConfigScreenOpenPolicy.Authority.LOCAL_DEFAULTS,
                0L);
        require(safetySession.searchShearingEntities("").size() == 2,
                "Blank shearing search did not show exactly the loaded overrides");
        require(safetySession.shearingClassification("example:unknown_shearable")
                        == ShearingClassification.UNKNOWN
                        && safetySession.effectiveShearingMultiplier("example:unknown_shearable") == 1
                        && !safetySession.setShearingEntityMultiplier(
                                "example:unknown_shearable",
                                3),
                "Unknown shearing override bypassed fixed vanilla safety in the editor");
        require(safetySession.shearingClassification("minecraft:mooshroom")
                        == ShearingClassification.SPECIAL
                        && safetySession.effectiveShearingMultiplier("minecraft:mooshroom") == 1
                        && !safetySession.setShearingEntityMultiplier("minecraft:mooshroom", 3),
                "Special shearing override bypassed fixed vanilla safety in the editor");
        require(!safetySession.isDirty(),
                "Read-only safety checks mutated a shearing draft");
    }

    private static void verifyLocalResetConfirmation(
            final ClientGameTestContext context,
            final SmartDropsConfigScreen root
    ) {
        final SmartDropsConfig persistedBeforeTest = ConfigManager.snapshot();
        final ConfigEditorSession session = root.editorSession();
        try {
            require(root.resetButton() != null && root.resetButton().active,
                    "Editable local defaults did not expose an active Reset All Settings button");
            require(session.revision() == ConfigManager.revision(),
                    "Local editor did not retain the authoritative config revision");

            stageComplexDraft(session);
            require(session.isDirty(), "The reset confirmation test did not create a dirty staged draft");
            require(session.searchBlocks("").stream()
                            .anyMatch(info -> "minecraft:diamond_ore".equals(info.id())),
                    "The staged block override was not visible before reset");
            require(session.searchFilterBlocks("").stream()
                            .anyMatch(info -> "minecraft:diamond_ore".equals(info.id())),
                    "The staged filter entry was not visible before reset");
            require(session.searchShearingEntities("").stream()
                            .anyMatch(info -> "minecraft:sheep".equals(info.id())),
                    "The staged shearing override was not visible before reset");

            final SmartDropsConfig draftBeforeConfirmation = session.workingSnapshot();
            final SmartDropsConfig persistedBeforeConfirmation = ConfigManager.snapshot();
            final long revisionBeforeConfirmation = ConfigManager.revision();

            context.clickScreenButton(RESET_KEY);
            context.waitForScreen(ResetAllSettingsConfirmScreen.class);
            takeRequiredScreenshot(context, "smart-drops-reset-confirmation");
            final Screen confirmation = context.computeOnClient(client -> client.screen);
            require("Reset Smart Resource Multiplier?".equals(confirmation.getTitle().getString()),
                    "Reset confirmation had an unexpected public title");
            require(hasWidgetLabel(confirmation, RESET_EVERYTHING_KEY)
                            && hasWidgetLabel(confirmation, CANCEL_KEY),
                    "Reset confirmation omitted its destructive or Cancel action");
            final Button destructive = buttons(confirmation).stream()
                    .filter(button -> RESET_EVERYTHING_KEY.equals(button.getMessage().getString()))
                    .findFirst()
                    .orElseThrow();
            require(destructive.getMessage().getStyle().getColor() != null,
                    "Reset Everything did not receive destructive warning styling");
            require(ConfigManager.revision() == revisionBeforeConfirmation
                            && sameConfiguration(persistedBeforeConfirmation, ConfigManager.snapshot())
                            && sameConfiguration(draftBeforeConfirmation, session.workingSnapshot()),
                    "Opening reset confirmation mutated persisted or staged configuration");

            context.clickScreenButton(CANCEL_KEY);
            context.waitForScreen(SmartDropsConfigScreen.class);
            require(currentConfigScreen(context) == root && root.editorSession() == session,
                    "Cancel did not return to the exact staged root/session");
            require(sameConfiguration(draftBeforeConfirmation, session.workingSnapshot())
                            && ConfigManager.revision() == revisionBeforeConfirmation,
                    "Cancel changed the staged or persisted configuration");
            final Screen childAfterCancel = openChild(
                    context,
                    ADVANCED_KEY,
                    "AdvancedConfigScreen",
                    "Advanced");
            assertUnsavedChangesIndicator(childAfterCancel, true,
                    "Reset Cancel hid the staged session's unsaved-changes indicator");
            returnToSameRoot(context, root, session);

            context.clickScreenButton(RESET_KEY);
            context.waitForScreen(ResetAllSettingsConfirmScreen.class);
            context.runOnClient(client -> client.screen.keyPressed(
                    new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0)));
            context.waitForScreen(SmartDropsConfigScreen.class);
            require(currentConfigScreen(context) == root && root.editorSession() == session,
                    "Escape did not behave exactly like Cancel");
            require(sameConfiguration(draftBeforeConfirmation, session.workingSnapshot())
                            && ConfigManager.revision() == revisionBeforeConfirmation,
                    "Escape changed the staged or persisted configuration");

            context.clickScreenButton(RESET_KEY);
            context.waitForScreen(ResetAllSettingsConfirmScreen.class);
            context.clickScreenButton(RESET_EVERYTHING_KEY);
            context.waitForScreen(SmartDropsConfigScreen.class);
            final SmartDropsConfigScreen refreshed = currentConfigScreen(context);
            require(refreshed != root && refreshed.editorSession() != session,
                    "Successful local reset retained the stale staged screen/session");
            require(ConfigManager.revision() == revisionBeforeConfirmation + 1,
                    "Successful local reset did not advance exactly one config revision");
            assertCleanDefaultEditor(refreshed);
            require(sameConfiguration(SmartDropsConfig.defaults(), ConfigManager.snapshot()),
                    "Local Reset Everything did not persist authoritative defaults");
            require(refreshed.editorSession().searchBlocks("").stream()
                            .noneMatch(info -> "minecraft:diamond_ore".equals(info.id())),
                    "The refreshed Overrides Only cache retained a removed block override");
            require(refreshed.editorSession().searchFilterBlocks("").stream()
                            .noneMatch(info -> "minecraft:diamond_ore".equals(info.id())),
                    "The refreshed filter cache retained a removed custom entry");
            require(refreshed.editorSession().searchShearingEntities("").isEmpty(),
                    "The refreshed shearing cache retained a removed exact override");
        } finally {
            require(ConfigManager.update(config -> copyConfiguration(config, persistedBeforeTest)),
                    "Could not restore the local configuration after the reset client test");
        }
    }

    private static void verifyIntegratedServerOwnerAndApply(final ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder()
                .setUseConsistentSettings(true)
                .create()) {
            singleplayer.getClientWorld().waitForChunksRender(false);
            openAuthoritativeScreen(context);
            SmartDropsConfigScreen root = currentConfigScreen(context);
            assertGeneralRoot(root, true);
            takeRequiredScreenshot(context, "smart-drops-singleplayer-authoritative-general");

            // Re-enter through the production factory while the connection-scoped snapshot is cached.
            context.setScreen(() -> SmartDropsConfigScreens.create(null));
            context.waitForScreen(SmartDropsConfigScreen.class);
            root = currentConfigScreen(context);
            assertGeneralRoot(root, true);

            final SmartDropsConfig originalConfig = singleplayer.getServer()
                    .computeOnServer(server -> ConfigManager.snapshot());
            final int original = originalConfig.globalMultiplier;
            final boolean originalEntityDrops = originalConfig.entityDropsEnabled;
            final boolean originalManualShearing = originalConfig.manualShearingDropsEnabled;
            final int direction = original < root.editorSession().maximumMultiplier() ? 1 : -1;
            final int changed = original + direction;
            try {
                clickGlobalAdjustment(context, root, direction);
                require(root.editorSession().setEntityDropsEnabled(!originalEntityDrops),
                        "Entity Drops toggle did not stage alongside the global edit");
                require(root.editorSession().setManualShearingDropsEnabled(!originalManualShearing),
                        "Manual Shearing toggle did not stage alongside the global edit");
                require(root.editorSession().globalMultiplier() == changed,
                        "Global adjustment button did not stage the expected value");
                require(root.editorSession().isDirty(), "Global adjustment did not dirty the session");
                require(root.applyButton().active, "Apply was not enabled for a staged server change");
                final Screen dirtyChild = openChild(
                        context,
                        ADVANCED_KEY,
                        "AdvancedConfigScreen",
                        "Advanced");
                assertUnsavedChangesIndicator(dirtyChild, true,
                        "A staged server edit was not visible on a child screen");
                returnToSameRoot(context, root, root.editorSession());
                require(root.applyButton().active,
                        "Returning from a dirty child disabled root Apply");

                context.clickScreenButton(APPLY_KEY);
                context.waitForScreen(SmartDropsConfigScreen.class);
                require(
                        singleplayer.getServer().computeOnServer(server ->
                                ConfigManager.get().globalMultiplier == changed
                                        && ConfigManager.get().entityDropsEnabled != originalEntityDrops
                                        && ConfigManager.get().manualShearingDropsEnabled
                                                != originalManualShearing),
                        "Integrated server did not persist the complete block/entity/shearing GUI patch");

                root = currentConfigScreen(context);
                assertGeneralRoot(root, true);
                require(!root.editorSession().isDirty(), "Server acknowledgement returned a dirty editor");
                require(!root.applyButton().active, "Apply stayed active after server acknowledgement");
                final Screen cleanChild = openChild(
                        context,
                        ADVANCED_KEY,
                        "AdvancedConfigScreen",
                        "Advanced");
                assertUnsavedChangesIndicator(cleanChild, false,
                        "Successful Apply left the child unsaved-changes indicator visible");
                returnToSameRoot(context, root, root.editorSession());

                clickGlobalAdjustment(context, root, -direction);
                require(root.editorSession().setEntityDropsEnabled(originalEntityDrops),
                        "Entity Drops toggle did not stage its restore value");
                require(root.editorSession().setManualShearingDropsEnabled(originalManualShearing),
                        "Manual Shearing toggle did not stage its restore value");
                context.clickScreenButton(APPLY_KEY);
                context.waitForScreen(SmartDropsConfigScreen.class);
                require(
                        singleplayer.getServer().computeOnServer(server ->
                                ConfigManager.get().globalMultiplier == original
                                        && ConfigManager.get().entityDropsEnabled == originalEntityDrops
                                        && ConfigManager.get().manualShearingDropsEnabled
                                                == originalManualShearing),
                        "Integrated-server smoke test did not restore the original staged domains");
                root = currentConfigScreen(context);
                assertGeneralRoot(root, true);

                final long revisionBeforeReset = singleplayer.getServer()
                        .computeOnServer(server -> ConfigManager.revision());
                clickGlobalAdjustment(context, root, direction);
                require(root.editorSession().isDirty(),
                        "Integrated reset test did not stage a pending GUI edit");
                context.clickScreenButton(RESET_KEY);
                context.waitForScreen(ResetAllSettingsConfirmScreen.class);
                context.clickScreenButton(RESET_EVERYTHING_KEY);
                context.waitForScreen(SmartDropsConfigScreen.class);

                require(singleplayer.getServer().computeOnServer(server ->
                                sameConfiguration(SmartDropsConfig.defaults(), ConfigManager.snapshot())),
                        "Integrated server did not authoritatively replace the complete configuration");
                require(singleplayer.getServer().computeOnServer(server -> ConfigManager.revision())
                                == revisionBeforeReset + 1,
                        "Integrated reset did not advance exactly one authoritative revision");
                root = currentConfigScreen(context);
                assertCleanDefaultEditor(root);
            } finally {
                final boolean restored = singleplayer.getServer().computeOnServer(server ->
                        ConfigManager.update(config -> copyConfiguration(config, originalConfig)));
                require(restored, "Integrated-server smoke-test cleanup failed");
                context.setScreen(() -> null);
            }
        }
    }

    private static void verifyDedicatedServerPermissions(final ClientGameTestContext context) {
        try (TestDedicatedServerContext server = context.worldBuilder().createServer()) {
            final SmartDropsConfig originalConfig = server.computeOnServer(ignored -> ConfigManager.snapshot());
            final int original = originalConfig.globalMultiplier;
            final int immediateValue = original == 7 ? 8 : 7;
            final int queuedValue = immediateValue == 8 ? 9 : 8;
            final int unauthorizedValue = original == 5 ? 6 : 5;
            try {
                try (TestServerConnection connection = server.connect()) {
                    connection.getClientWorld().waitForChunksRender(false);

                    openAuthoritativeScreen(context);
                    SmartDropsConfigScreen root = currentConfigScreen(context);
                    assertGeneralRoot(root, false);
                    assertReadOnlyRoot(root);
                    assertRootClarityTooltips(context, root);
                    verifyReadOnlyChildNavigation(context, root);
                    takeRequiredScreenshot(context, "smart-drops-dedicated-nonop-general");

                    final long unauthorizedRevision = root.editorSession().revision();
                    sendResetRequest(context, 10_000, unauthorizedRevision);
                    context.waitTicks(2);
                    require(server.computeOnServer(ignored -> ConfigManager.revision()) == unauthorizedRevision
                                    && server.computeOnServer(ignored ->
                                            sameConfiguration(originalConfig, ConfigManager.snapshot())),
                            "A non-operator reset request changed server configuration or revision");

                    sendGlobalPatch(context, 10_001, unauthorizedRevision, unauthorizedValue);
                    context.waitTicks(2);
                    sendGlobalPatch(context, 10_002, unauthorizedRevision, unauthorizedValue);
                    context.waitTicks(2);

                    final UUID playerId = context.computeOnClient(client ->
                            Objects.requireNonNull(
                                    client.player,
                                    "Dedicated connection did not create a client player")
                                    .getUUID());
                    final String playerName = server.computeOnServer(minecraftServer ->
                            Objects.requireNonNull(
                                    minecraftServer.getPlayerList().getPlayer(playerId),
                                    "Dedicated connection did not create a server player")
                                    .getScoreboardName());
                    server.runCommand("op " + playerName);
                    context.waitTicks(22);
                    require(
                            server.computeOnServer(ignored -> ConfigManager.get().globalMultiplier) == original,
                            "A patch sent while unauthorized was retained and applied after promotion");
                    // Exit through the production button so the cached read-only snapshot is invalidated.
                    context.clickScreenButton(DONE_KEY);
                    context.waitFor(client -> client.screen == null);
                    openAuthoritativeScreen(context);
                    root = currentConfigScreen(context);
                    assertGeneralRoot(root, true);
                    takeRequiredScreenshot(context, "smart-drops-dedicated-op-general");

                    final long revisionBeforeGuiReset = server.computeOnServer(ignored -> ConfigManager.revision());
                    final int resetDirection = root.editorSession().globalMultiplier()
                            < root.editorSession().maximumMultiplier() ? 1 : -1;
                    clickGlobalAdjustment(context, root, resetDirection);
                    context.clickScreenButton(RESET_KEY);
                    context.waitForScreen(ResetAllSettingsConfirmScreen.class);
                    context.clickScreenButton(RESET_EVERYTHING_KEY);
                    context.waitForScreen(SmartDropsConfigScreen.class);
                    root = currentConfigScreen(context);
                    assertCleanDefaultEditor(root);
                    require(server.computeOnServer(ignored ->
                                    sameConfiguration(SmartDropsConfig.defaults(), ConfigManager.snapshot())),
                            "Dedicated-server operator reset did not restore complete defaults");
                    require(server.computeOnServer(ignored -> ConfigManager.revision())
                                    == revisionBeforeGuiReset + 1,
                            "Dedicated-server operator reset did not advance exactly one revision");

                    // Let the destructive-action cooldown expire before the raw queued-patch reset scenario.
                    context.waitTicks(42);
                    final long revisionAfterGuiReset = root.editorSession().revision();
                    sendGlobalPatch(context, 20_001, revisionAfterGuiReset, immediateValue);
                    context.waitTicks(2);
                    require(
                            server.computeOnServer(ignored -> ConfigManager.get().globalMultiplier) == immediateValue,
                            "The first authorized patch was not applied before the cooldown test");
                    final long revisionAfterImmediatePatch = server.computeOnServer(ignored -> ConfigManager.revision());
                    sendGlobalPatch(context, 20_002, revisionAfterImmediatePatch, queuedValue);
                    context.waitTicks(2);
                    require(
                            server.computeOnServer(ignored -> ConfigManager.get().globalMultiplier) == immediateValue,
                            "The cooldown patch applied before its eligible tick");

                    sendResetRequest(context, 20_003, revisionAfterImmediatePatch);
                    context.waitTicks(2);
                    final long revisionAfterQueuedReset = server.computeOnServer(ignored -> ConfigManager.revision());
                    require(revisionAfterQueuedReset == revisionAfterImmediatePatch + 1
                                    && server.computeOnServer(ignored ->
                                            sameConfiguration(SmartDropsConfig.defaults(), ConfigManager.snapshot())),
                            "A reset did not atomically supersede the queued pre-reset patch");
                    context.waitTicks(22);
                    require(server.computeOnServer(ignored ->
                                    sameConfiguration(SmartDropsConfig.defaults(), ConfigManager.snapshot())),
                            "A queued patch fired after Reset Everything");

                    sendGlobalPatch(context, 20_004, revisionAfterImmediatePatch, queuedValue);
                    context.waitTicks(2);
                    require(server.computeOnServer(ignored -> ConfigManager.revision()) == revisionAfterQueuedReset
                                    && server.computeOnServer(ignored ->
                                            sameConfiguration(SmartDropsConfig.defaults(), ConfigManager.snapshot())),
                            "A stale mutation created before reset overwrote the defaults");

                    // Queue one valid mutation, disconnect, and retain the existing disconnect invariant.
                    sendGlobalPatch(context, 20_005, revisionAfterQueuedReset, queuedValue);
                    context.waitTicks(2);
                    require(server.computeOnServer(ignored ->
                                    sameConfiguration(SmartDropsConfig.defaults(), ConfigManager.snapshot())),
                            "The post-reset cooldown patch applied before disconnect");
                    context.setScreen(() -> null);
                }

                context.waitTicks(22);
                require(
                        server.computeOnServer(ignored ->
                                sameConfiguration(SmartDropsConfig.defaults(), ConfigManager.snapshot())),
                        "A queued config patch survived the player disconnect");
            } finally {
                final boolean restored = server.computeOnServer(ignored ->
                        ConfigManager.update(config -> copyConfiguration(config, originalConfig)));
                require(restored, "Dedicated-server smoke-test cleanup failed");
            }
        }
    }

    private static void verifyNavigationAndScreenshots(
            final ClientGameTestContext context,
            final SmartDropsConfigScreen root
    ) {
        final ConfigEditorSession session = root.editorSession();

        Screen child = openChild(context, CATEGORIES_KEY, "RuleListScreen", "Categories");
        final StructuredConfigList categories = onlyList(child);
        require(categories.rowCount() == session.categories().size(),
                "Category screen did not show every category");
        assertBoundedChild(child, 20);
        takeRequiredScreenshot(context, "smart-drops-categories");

        final StructuredConfigList.Row logsRow = categories.rows().stream()
                .filter(row -> "Logs / Wood".equals(row.primary().getString()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Categories omitted Logs / Wood"));
        final Screen categoryList = child;
        context.runOnClient(client -> onlySearchBox(categoryList).setValue("logs"));
        context.runOnClient(client -> logsRow.action().run());
        Screen editor = waitForSimpleScreen(context, "RuleEditScreen", "Logs / Wood");
        takeRequiredScreenshot(context, "smart-drops-category-editor");
        context.clickScreenButton("View Blocks in Category");
        final Screen categoryBlocks = waitForSimpleScreen(
                context,
                "BlockOverridesScreen",
                "Block Overrides");
        final int logBlockCount = session.categoryBlockCount(Category.LOGS);
        require(logBlockCount > 0, "Logs / Wood category still contained no blocks");
        require(onlyList(categoryBlocks).rowCount() == Math.min(logBlockCount, 200),
                "Category-filtered block browser did not populate Logs / Wood");
        takeRequiredScreenshot(context, "smart-drops-category-logs-blocks");
        context.clickScreenButton(BACK_KEY);
        waitForSimpleScreen(context, "RuleEditScreen", "Logs / Wood");
        context.clickScreenButton(BACK_KEY);
        final Screen restoredCategoryList = waitForSimpleScreen(
                context,
                "RuleListScreen",
                "Categories");
        require("logs".equals(onlySearchBox(restoredCategoryList).getValue()),
                "Returning from a category editor lost the category search query");
        returnToSameRoot(context, root, session);

        child = openChild(context, BLOCK_OVERRIDES_KEY, "BlockOverridesScreen", "Block Overrides");
        StructuredConfigList overrides = onlyList(child);
        require(overrides.rowCount() == session.blockMultipliers().size(),
                "Blank block search did not show exactly the configured overrides");
        require(onlySearchBox(child).active, "Block search was not interactive");
        assertBoundedChild(child, 200);
        takeRequiredScreenshot(context, "smart-drops-block-overrides");

        final Screen blockBrowser = child;
        context.runOnClient(client -> onlySearchBox(blockBrowser).setValue("diamond"));
        overrides = onlyList(blockBrowser);
        require(overrides.rows().stream()
                        .anyMatch(row -> "minecraft:diamond_ore".equals(row.secondary().getString())),
                "Display-name query did not render Diamond Ore");
        takeRequiredScreenshot(context, "smart-drops-block-search-diamond");

        context.runOnClient(client -> onlySearchBox(blockBrowser).setValue("minecraft:diamond_ore"));
        require(onlyList(blockBrowser).rows().stream()
                        .anyMatch(row -> "minecraft:diamond_ore".equals(row.secondary().getString())),
                "Exact registry-ID query did not render Diamond Ore");
        context.runOnClient(client -> onlySearchBox(blockBrowser).setValue("acacia"));
        require(onlyList(blockBrowser).rows().stream()
                        .anyMatch(row -> row.secondary().getString().startsWith("minecraft:acacia_")),
                "Acacia query did not render relevant blocks");
        context.runOnClient(client -> onlySearchBox(blockBrowser).setValue("minecraft:"));
        require(onlyList(blockBrowser).rowCount() == BLOCK_RESULT_LIMIT,
                "Broad search was not capped to the lightweight result limit");

        context.runOnClient(client -> onlySearchBox(blockBrowser).setValue("diamond"));
        final StructuredConfigList.Row diamondRow = onlyList(blockBrowser).rows().stream()
                .filter(row -> "minecraft:diamond_ore".equals(row.secondary().getString()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Diamond Ore row disappeared"));
        context.runOnClient(client -> diamondRow.action().run());
        editor = waitForSimpleScreen(context, "RuleEditScreen", "Diamond Ore");
        takeRequiredScreenshot(context, "smart-drops-block-editor-diamond-ore");
        context.clickScreenButton(BACK_KEY);
        final Screen restoredBlockBrowser = waitForSimpleScreen(
                context,
                "BlockOverridesScreen",
                "Block Overrides");
        require("diamond".equals(onlySearchBox(restoredBlockBrowser).getValue()),
                "Returning from a block editor lost the block search query");
        context.runOnClient(client -> onlySearchBox(restoredBlockBrowser).setValue(""));
        require(onlyList(restoredBlockBrowser).rowCount() == session.blockMultipliers().size(),
                "Clearing search did not return to configured overrides only");
        returnToSameRoot(context, root, session);

        child = openChild(context, DIMENSIONS_KEY, "RuleListScreen", "Dimensions");
        final StructuredConfigList dimensions = onlyList(child);
        require(dimensions.rowCount() == session.dimensionIds().size(),
                "Dimension screen did not show every discovered dimension");
        require(dimensions.rowCount() >= 3, "Dimension screen omitted vanilla dimensions");
        assertBoundedChild(child, 32);
        takeRequiredScreenshot(context, "smart-drops-dimensions");
        returnToSameRoot(context, root, session);

        child = openChild(context, FILTERS_KEY, "FilterConfigScreen", "Filters");
        require(onlySearchBox(child).active, "Filter search was not interactive");
        assertBoundedChild(child, 200);
        takeRequiredScreenshot(context, "smart-drops-filters");
        final Screen filters = child;
        final String unconfiguredFilterId = session.blockCatalog().stream()
                .map(ConfigEditorSession.BlockInfo::id)
                .filter(id -> session.filterState(id) == ConfigEditorSession.FilterEntryState.NONE)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No unconfigured block was available for filter testing"));
        context.runOnClient(client -> onlySearchBox(filters).setValue(unconfiguredFilterId));
        StructuredConfigList.Row filterRow = onlyList(filters).rows().stream()
                .filter(row -> unconfiguredFilterId.equals(row.secondary().getString()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Exact filter search omitted " + unconfiguredFilterId));
        final ConfigEditorSession.FilterEntryState activeFilter = session.filterMode()
                == com.chedidandrew.smartresourcedrops.config.SmartDropsConfig.FilterMode.BLACKLIST
                ? ConfigEditorSession.FilterEntryState.BLACKLIST
                : ConfigEditorSession.FilterEntryState.WHITELIST;
        final StructuredConfigList.Row addFilterRow = filterRow;
        context.runOnClient(client -> addFilterRow.action().run());
        require(session.filterState(unconfiguredFilterId) == activeFilter && session.isDirty(),
                "Filter row did not stage the active filter mode");
        filterRow = onlyList(filters).rows().stream()
                .filter(row -> unconfiguredFilterId.equals(row.secondary().getString()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Staged filter row disappeared"));
        final StructuredConfigList.Row removeFilterRow = filterRow;
        context.runOnClient(client -> removeFilterRow.action().run());
        require(session.filterState(unconfiguredFilterId) == ConfigEditorSession.FilterEntryState.NONE
                        && !session.isDirty(),
                "Removing the staged filter did not restore a clean session");
        returnToSameRoot(context, root, session);

        child = openChild(context, ADVANCED_KEY, "AdvancedConfigScreen", "Advanced");
        final StructuredConfigList advanced = onlyList(child);
        require(advanced.rowCount() == 11, "Advanced screen did not show eight settings and three presets");
        context.runOnClient(client -> assertAdvancedTooltips(client, advanced, false));
        assertBoundedChild(child, 16);
        takeRequiredScreenshot(context, "smart-drops-advanced");
        final StructuredConfigList.Row presetRow = advanced.rows().get(8);
        context.runOnClient(client -> presetRow.action().run());
        final Screen preview = waitForSimpleScreen(
                context,
                "PresetPreviewScreen",
                "Preset Preview");
        require(!session.isDirty(), "Opening a preset preview mutated the session");
        takeRequiredScreenshot(context, "smart-drops-preset-preview");
        context.clickScreenButton(BACK_KEY);
        waitForSimpleScreen(context, "AdvancedConfigScreen", "Advanced");
        returnToSameRoot(context, root, session);

        child = openChild(
                context,
                ENTITY_DROPS_KEY,
                "EntityDropsScreen",
                "Entity and Mob Drops");
        final StructuredConfigList entityDrops = onlyList(child);
        require(entityDrops.rowCount() == 11,
                "Entity and Mob Drops screen omitted one or more authoritative settings/routes");
        require(session.searchEntities("minecraft:cow").stream()
                        .anyMatch(info -> "minecraft:cow".equals(info.id())),
                "Entity registry search did not find minecraft:cow by namespaced ID");
        assertBoundedChild(child, 20);
        takeRequiredScreenshot(context, "smart-drops-entity-drops");

        final Integer originalEntityCategory = session.entityCategoryMultiplier(EntityCategory.PASSIVE);
        context.runOnClient(client -> rowWithPrimary(entityDrops, "Entity Categories").action().run());
        final Screen entityCategories = waitForSimpleScreen(
                context, "EntityCategoryScreen", "Entity Categories");
        final StructuredConfigList entityCategoryList = onlyList(entityCategories);
        require(entityCategoryList.rowCount() == session.entityCategories().size(),
                "Entity Categories child omitted registry categories");
        context.runOnClient(client -> assertStructuredTooltipWrapping(client, entityCategoryList));
        require(session.setEntityCategoryMultiplier(EntityCategory.PASSIVE,
                        Objects.equals(originalEntityCategory, 3) ? 2 : 3),
                "Entity category child could not stage a category override");
        require(session.isDirty(), "Entity category child edit did not dirty the shared session");
        require(session.setEntityCategoryMultiplier(EntityCategory.PASSIVE, originalEntityCategory),
                "Entity category child could not restore its staged override");
        context.clickScreenButton(BACK_KEY);
        waitForSimpleScreen(context, "EntityDropsScreen", "Entity and Mob Drops");

        context.runOnClient(client -> rowWithPrimary(
                onlyList(client.screen),
                "Entity Overrides").action().run());
        final Screen entityOverrides = waitForSimpleScreen(
                context, "EntityOverridesScreen", "Entity Overrides");
        final Integer originalCow = session.entityMultiplier("minecraft:cow");
        require(session.setEntityMultiplier("minecraft:cow", Objects.equals(originalCow, 4) ? 2 : 4),
                "Entity Overrides child could not stage an exact entity override");
        require(session.searchEntities("minecraft:cow").stream()
                        .anyMatch(info -> "minecraft:cow".equals(info.id())),
                "Entity Overrides child lost its staged registry result");
        require(session.setEntityMultiplier("minecraft:cow", originalCow),
                "Entity Overrides child could not restore its staged override");
        context.clickScreenButton(BACK_KEY);
        waitForSimpleScreen(context, "EntityDropsScreen", "Entity and Mob Drops");

        context.runOnClient(client -> rowWithPrimary(
                onlyList(client.screen),
                "Entity Filters").action().run());
        waitForSimpleScreen(context, "EntityFilterScreen", "Entity Filters");
        final ConfigEditorSession.FilterEntryState originalCowFilter =
                session.entityFilterState("minecraft:cow");
        final ConfigEditorSession.FilterEntryState stagedCowFilter =
                originalCowFilter == ConfigEditorSession.FilterEntryState.WHITELIST
                        ? ConfigEditorSession.FilterEntryState.BLACKLIST
                        : ConfigEditorSession.FilterEntryState.WHITELIST;
        require(session.setEntityFilterState("minecraft:cow", stagedCowFilter)
                        && session.entityFilterState("minecraft:cow") == stagedCowFilter,
                "Entity Filters child could not stage its tri-state entry");
        require(session.setEntityFilterState("minecraft:cow", originalCowFilter),
                "Entity Filters child could not restore its staged entry");
        context.clickScreenButton(BACK_KEY);
        waitForSimpleScreen(context, "EntityDropsScreen", "Entity and Mob Drops");

        final boolean originalEntityDropsEnabled = session.entityDropsEnabled();
        if (originalEntityDropsEnabled) {
            require(session.setEntityDropsEnabled(false),
                    "Could not disable death loot before testing the independent Shearing route");
        }
        context.runOnClient(client -> rowWithPrimary(
                onlyList(client.screen),
                SHEARING_DROPS_KEY).action().run());
        final Screen shearingDrops = waitForSimpleScreen(
                context,
                "ShearingDropsScreen",
                SHEARING_DROPS_KEY);
        require(onlyList(shearingDrops).rowCount() == 5,
                "Shearing Drops omitted source toggles, default, overrides, or safety guidance");
        assertBoundedChild(shearingDrops, 8);
        require(!session.entityDropsEnabled(),
                "Opening Shearing Drops re-enabled the independent death-loot subsystem");
        takeRequiredScreenshot(context, "smart-drops-shearing-drops");

        final Integer configuredShearingDefault = session.defaultShearingMultiplier();
        if (configuredShearingDefault == null) {
            require(rowWithPrimary(
                            onlyList(shearingDrops),
                            "Default Shearing Multiplier")
                            .rightDetail().getString().contains("Inherit -> "),
                    "Inherited shearing default did not display its effective Global value");
        }
        context.runOnClient(client -> rowWithPrimary(
                onlyList(client.screen),
                "Default Shearing Multiplier").action().run());
        waitForSimpleScreen(
                context,
                "ShearingRuleEditScreen",
                "Default Shearing Multiplier");
        final Integer stagedShearingDefault = configuredShearingDefault == null ? 0 : null;
        require(session.setDefaultShearingMultiplier(stagedShearingDefault),
                "Default Shearing editor could not stage a multiplier");
        context.clickScreenButton(BACK_KEY);
        waitForSimpleScreen(context, "ShearingDropsScreen", SHEARING_DROPS_KEY);
        require(Objects.equals(session.defaultShearingMultiplier(), stagedShearingDefault),
                "Back discarded the staged Default Shearing multiplier");
        require(session.setDefaultShearingMultiplier(configuredShearingDefault),
                "Default Shearing multiplier could not be restored after Back");
        final boolean originalManualShearing = session.manualShearingDropsEnabled();
        context.runOnClient(client -> rowWithPrimary(
                onlyList(client.screen),
                "Manual Shearing").action().run());
        require(session.manualShearingDropsEnabled() != originalManualShearing,
                "Manual Shearing row did not stage through the shared editor session");
        context.runOnClient(client -> rowWithPrimary(
                onlyList(client.screen),
                "Manual Shearing").action().run());
        require(session.manualShearingDropsEnabled() == originalManualShearing,
                "Manual Shearing row could not restore its original staged value");

        context.runOnClient(client -> rowWithPrimary(
                onlyList(client.screen),
                SHEARING_OVERRIDES_KEY).action().run());
        final Screen shearingOverrides = waitForSimpleScreen(
                context,
                "ShearingOverridesScreen",
                SHEARING_OVERRIDES_KEY);
        assertBoundedChild(shearingOverrides, 200);
        require(onlyList(shearingOverrides).rowCount() == session.shearingEntityMultipliers().size(),
                "Blank shearing search dumped candidates instead of showing configured overrides only");
        context.runOnClient(client -> onlySearchBox(shearingOverrides).setValue("minecraft:sheep"));
        final StructuredConfigList.Row sheepRow = onlyList(shearingOverrides).rows().stream()
                .filter(row -> "minecraft:sheep".equals(row.secondary().getString()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Certified Sheep was absent from shearing search"));
        require(session.shearingClassification("minecraft:sheep")
                        == ShearingClassification.STANDARD_RESOURCE,
                "Sheep was not classified from the standard-resources tag");
        final Integer originalSheep = session.shearingEntityMultiplier("minecraft:sheep");
        final Integer stagedSheep = originalSheep == null ? 0 : null;
        require(session.setShearingEntityMultiplier("minecraft:sheep", stagedSheep),
                "Certified Sheep could not receive an exact shearing override");
        require(Objects.equals(session.shearingEntityMultiplier("minecraft:sheep"), stagedSheep)
                        && session.effectiveShearingMultiplier("minecraft:sheep")
                                == (stagedSheep == null
                                        ? session.effectiveDefaultShearingMultiplier()
                                        : stagedSheep),
                "Staged Sheep shearing rule did not become effective");
        require(session.setShearingEntityMultiplier("minecraft:sheep", originalSheep),
                "Certified Sheep override could not be restored");
        context.runOnClient(client -> sheepRow.action().run());
        waitForSimpleScreen(context, "ShearingRuleEditScreen", "Sheep");
        context.clickScreenButton(BACK_KEY);
        waitForSimpleScreen(context, "ShearingOverridesScreen", SHEARING_OVERRIDES_KEY);

        final Screen restoredShearingOverrides = context.computeOnClient(client -> client.screen);
        context.runOnClient(client -> onlySearchBox(restoredShearingOverrides).setValue("mooshroom"));
        final StructuredConfigList.Row mooshroomRow = onlyList(restoredShearingOverrides).rows().stream()
                .filter(row -> "minecraft:mooshroom".equals(row.secondary().getString()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Known special Mooshroom was absent from search"));
        require(session.shearingClassification("minecraft:mooshroom")
                        == ShearingClassification.SPECIAL,
                "Mooshroom was not fixed behind special shearing safety");
        final Screen beforeSpecialAction = restoredShearingOverrides;
        context.runOnClient(client -> mooshroomRow.action().run());
        require(context.computeOnClient(client -> client.screen) == beforeSpecialAction,
                "Special Mooshroom unexpectedly opened an editable multiplier screen");
        require(!session.setShearingEntityMultiplier("minecraft:mooshroom", 4)
                        && !session.setShearingEntityMultiplier("minecraft:cow", 4),
                "Special or unknown entity accepted an exact shearing override");
        takeRequiredScreenshot(context, "smart-drops-shearing-overrides");

        context.clickScreenButton(BACK_KEY);
        waitForSimpleScreen(context, "ShearingDropsScreen", SHEARING_DROPS_KEY);
        context.clickScreenButton(BACK_KEY);
        waitForSimpleScreen(context, "EntityDropsScreen", "Entity and Mob Drops");
        if (originalEntityDropsEnabled) {
            require(session.setEntityDropsEnabled(true),
                    "Could not restore death loot after independent Shearing navigation");
        }
        require(!session.isDirty(),
                "Restoring all Entity child edits did not return the shared session to clean state");
        returnToSameRoot(context, root, session);

        require(!session.isDirty(), "Navigation unexpectedly dirtied the editor session");
    }

    private static SmartDropsConfigScreen verifyUnsavedChangesIndicatorAndDiscard(
            final ClientGameTestContext context,
            final SmartDropsConfigScreen root
    ) {
        final ConfigEditorSession session = root.editorSession();
        require(!session.isDirty(), "Unsaved-changes test did not begin with a clean session");

        final Screen categories = openChild(
                context,
                CATEGORIES_KEY,
                "RuleListScreen",
                "Categories");
        assertUnsavedChangesIndicator(categories, false,
                "A clean category screen showed unsaved changes");
        final StructuredConfigList.Row oresRow = onlyList(categories).rows().stream()
                .filter(row -> Category.ORES.key().equals(row.tooltip().getString()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Categories omitted Ores"));
        context.runOnClient(client -> oresRow.action().run());
        final Screen categoryEditor = waitForSimpleScreen(context, "RuleEditScreen", "Ores");
        assertUnsavedChangesIndicator(categoryEditor, false,
                "A clean focused category editor showed unsaved changes");

        final Integer originalCategory = session.categoryMultiplier(Category.ORES);
        final Integer originalEntity = session.entityMultiplier("minecraft:cow");
        final Integer originalShearing = session.shearingEntityMultiplier("minecraft:sheep");
        final String categoryAdjustment = originalCategory == null
                || originalCategory < session.maximumMultiplier()
                ? "+"
                : "-";
        context.clickScreenButton(categoryAdjustment);
        require(session.setEntityMultiplier("minecraft:cow", Objects.equals(originalEntity, 3) ? 2 : 3),
                "Discard test could not stage an entity child value");
        require(session.setShearingEntityMultiplier(
                        "minecraft:sheep",
                        originalShearing == null ? 0 : null),
                "Discard test could not stage a shearing child value");
        context.waitFor(client -> !Objects.equals(
                session.categoryMultiplier(Category.ORES),
                originalCategory));
        final Integer changedCategory = session.categoryMultiplier(Category.ORES);
        require(!Objects.equals(changedCategory, originalCategory),
                "The focused category multiplier control did not change the shared session");
        require(session.isDirty(), "Category edit did not mark the shared session dirty");
        assertUnsavedChangesIndicator(categoryEditor, true,
                "A category edit did not show unsaved changes on its focused editor");
        context.runOnClient(client -> categoryEditor.resize(320, 180));
        assertCompactUnsavedChangesLayout(categoryEditor);
        takeRequiredScreenshot(context, "smart-drops-unsaved-compact-320x180");

        context.clickScreenButton(BACK_KEY);
        final Screen dirtyCategories = waitForSimpleScreen(context, "RuleListScreen", "Categories");
        assertUnsavedChangesIndicator(dirtyCategories, true,
                "Returning to Categories lost the unsaved-changes indicator");
        returnToSameRoot(context, root, session);
        require(root.applyButton().active,
                "Root Apply was not active after a child-screen edit");
        require(hasWidgetLabel(root, DISCARD_CHANGES_KEY),
                "Root Done did not become Discard Changes for a child-screen edit");

        final Screen anotherChild = openChild(
                context,
                BLOCK_OVERRIDES_KEY,
                "BlockOverridesScreen",
                "Block Overrides");
        assertUnsavedChangesIndicator(anotherChild, true,
                "Navigating from Categories to Block Overrides lost the dirty indicator");
        returnToSameRoot(context, root, session);

        final Button discard = buttons(root).stream()
                .filter(button -> DISCARD_CHANGES_KEY.equals(button.getMessage().getString()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Dirty root omitted Discard Changes"));
        context.runOnClient(client -> discard.onPress(
                new KeyEvent(InputConstants.KEY_RETURN, 0, 0)));
        context.waitFor(client -> session.originalParent() == null
                ? client.screen instanceof TitleScreen
                : client.screen == session.originalParent());
        context.setScreen(() -> SmartDropsConfigScreens.create(null));
        context.waitForScreen(SmartDropsConfigScreen.class);
        final SmartDropsConfigScreen refreshed = currentConfigScreen(context);
        require(refreshed != root && refreshed.editorSession() != session,
                "Discard retained the abandoned dirty root/session");
        require(Objects.equals(
                        refreshed.editorSession().categoryMultiplier(Category.ORES),
                        originalCategory),
                "Discard persisted the abandoned category edit");
        require(Objects.equals(
                        refreshed.editorSession().entityMultiplier("minecraft:cow"),
                        originalEntity),
                "Discard persisted the abandoned entity override");
        require(Objects.equals(
                        refreshed.editorSession().shearingEntityMultiplier("minecraft:sheep"),
                        originalShearing),
                "Discard persisted the abandoned shearing override");
        assertGeneralRoot(refreshed, true);
        final Screen cleanChild = openChild(
                context,
                ADVANCED_KEY,
                "AdvancedConfigScreen",
                "Advanced");
        assertUnsavedChangesIndicator(cleanChild, false,
                "A fresh editor after Discard still showed unsaved changes");
        returnToSameRoot(context, refreshed, refreshed.editorSession());
        return refreshed;
    }

    private static void verifyReadOnlyChildNavigation(
            final ClientGameTestContext context,
            final SmartDropsConfigScreen root
    ) {
        final ConfigEditorSession session = root.editorSession();
        final int original = session.globalMultiplier();
        final int candidate = original < session.maximumMultiplier() ? original + 1 : original - 1;
        require(!session.setGlobalMultiplier(candidate), "Read-only session accepted a direct mutation");
        require(session.globalMultiplier() == original && !session.isDirty(),
                "Read-only mutation attempt changed editor state");

        final Screen filters = openChild(context, FILTERS_KEY, "FilterConfigScreen", "Filters");
        require(onlySearchBox(filters).active, "Read-only filter search should remain active");
        final List<Button> childButtons = buttons(filters);
        require(childButtons.size() == 2, "Filter screen should contain only mode and Back buttons");
        for (Button button : childButtons) {
            if ("Back".equals(button.getMessage().getString())) {
                require(button.active, "Back was disabled on a read-only child screen");
            } else {
                require(!button.active, "Filter mutation button was active for a non-operator");
            }
        }
        returnToSameRoot(context, root, session);

        final Screen advancedScreen = openChild(
                context,
                ADVANCED_KEY,
                "AdvancedConfigScreen",
                "Advanced");
        final StructuredConfigList advanced = onlyList(advancedScreen);
        context.runOnClient(client -> assertAdvancedTooltips(client, advanced, true));
        final SmartDropsConfig beforeAdvancedActions = session.workingSnapshot();
        context.runOnClient(client -> advanced.rows().subList(0, 8)
                .forEach(row -> row.action().run()));
        require(sameConfiguration(beforeAdvancedActions, session.workingSnapshot())
                        && !session.isDirty(),
                "Read-only Advanced rows accepted a staged mutation");
        returnToSameRoot(context, root, session);

        final Screen entityDrops = openChild(
                context,
                ENTITY_DROPS_KEY,
                "EntityDropsScreen",
                "Entity and Mob Drops");
        final boolean entityDropsEnabled = session.entityDropsEnabled();
        context.runOnClient(client -> onlyList(entityDrops).rows().get(0).action().run());
        require(session.entityDropsEnabled() == entityDropsEnabled && !session.isDirty(),
                "Read-only Entity and Mob Drops screen accepted a staged mutation");
        context.runOnClient(client -> rowWithPrimary(
                onlyList(client.screen),
                SHEARING_DROPS_KEY).action().run());
        final Screen shearing = waitForSimpleScreen(
                context,
                "ShearingDropsScreen",
                SHEARING_DROPS_KEY);
        final boolean manualShearing = session.manualShearingDropsEnabled();
        final boolean automatedShearing = session.automatedShearingDropsEnabled();
        context.runOnClient(client -> rowWithPrimary(
                onlyList(shearing),
                "Manual Shearing").action().run());
        context.runOnClient(client -> rowWithPrimary(
                onlyList(shearing),
                "Automated Shearing").action().run());
        require(session.manualShearingDropsEnabled() == manualShearing
                        && session.automatedShearingDropsEnabled() == automatedShearing
                        && !session.setDefaultShearingMultiplier(4)
                        && !session.setShearingEntityMultiplier("minecraft:sheep", 4)
                        && !session.isDirty(),
                "Read-only Shearing Drops accepted a staged mutation");
        context.clickScreenButton(BACK_KEY);
        waitForSimpleScreen(context, "EntityDropsScreen", "Entity and Mob Drops");
        returnToSameRoot(context, root, session);
    }

    private static void assertGeneralRoot(final SmartDropsConfigScreen screen, final boolean editable) {
        require("Smart Resource Multiplier".equals(screen.getTitle().getString()),
                "Root config screen had an unexpected title");
        require(screen.editorSession() != null, "Root screen did not expose its editor session");
        require(screen.editorSession().editable() == editable, "Unexpected root editability");
        require(screen.children().size() >= 14 && screen.children().size() <= 20,
                "General screen constructed an unbounded or incomplete widget set: " + screen.children().size());
        require(screen.children().stream().noneMatch(EditBox.class::isInstance),
                "General screen still contains the old all-view search field");
        require(screen.children().stream().noneMatch(StructuredConfigList.class::isInstance),
                "General screen unexpectedly contains a registry list");
        require(hasWidgetLabel(screen, "Global Multiplier"),
                "General screen omitted the global multiplier control");
        require(hasWidgetLabel(screen, "Block XP Multiplier"),
                "General screen omitted the block XP multiplier control");
        require(!screen.editorSession().isDirty(), "A newly opened General screen was dirty");
        require(screen.applyButton() != null, "General screen did not expose Apply");
        require(!screen.applyButton().active, "Apply must be disabled for a clean session");
        require(screen.resetButton() != null, "General screen did not expose Reset All Settings");
        require(screen.resetButton().active == editable,
                "Reset All Settings availability did not follow authoritative edit permission");

        final List<String> labels = buttons(screen).stream()
                .map(button -> button.getMessage().getString())
                .toList();
        require(labels.stream().noneMatch(label -> label.contains("Previous")
                        || label.contains("Next") || label.startsWith("Page ")),
                "Legacy Previous/Next/Page controls are still present");
        for (String navigationLabel : ROOT_NAVIGATION_LABELS) {
            require(labels.contains(navigationLabel), "Missing General navigation button: " + navigationLabel);
        }
        require(!labels.contains("Categories") && !labels.contains("Filters"),
                "Root navigation reused a child-only Categories or Filters title");
        require(labels.contains(RESET_KEY), "General screen omitted Reset All Settings");
    }

    private static void assertRootClarityTooltips(
            final ClientGameTestContext context,
            final SmartDropsConfigScreen screen
    ) {
        context.runOnClient(client -> assertRootClarityTooltips(client, screen));
    }

    private static void assertRootClarityTooltips(
            final Minecraft client,
            final SmartDropsConfigScreen screen
    ) {
        for (Map.Entry<String, String> expected : ROOT_NAVIGATION_TOOLTIPS.entrySet()) {
            assertWidgetTooltip(
                    buttonWithLabel(screen, expected.getKey()),
                    expected.getValue(),
                    "Root navigation tooltip for " + expected.getKey());
        }
        for (String label : ROOT_NAVIGATION_WITHOUT_TOOLTIPS) {
            require(tooltipText(buttonWithLabel(screen, label)).isEmpty(),
                    "Self-explanatory root navigation unexpectedly gained a tooltip: " + label);
        }

        final Button multiplyExperience = buttons(screen).stream()
                .filter(button -> button.getMessage().getString().startsWith("Multiply Block XP:"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Root omitted Multiply Block XP"));
        assertWidgetTooltip(
                multiplyExperience,
                MULTIPLY_XP_TOOLTIP,
                "Multiply Block XP tooltip");
        assertMultiplierTooltipPropagation(
                client,
                screen,
                "Global Multiplier",
                GLOBAL_MULTIPLIER_TOOLTIP);
        assertMultiplierTooltipPropagation(
                client,
                screen,
                "Block XP Multiplier",
                XP_MULTIPLIER_TOOLTIP);

        for (AbstractWidget widget : widgets(screen)) {
            final String tooltip = tooltipText(widget);
            if (!tooltip.isEmpty()) {
                assertWrappedTooltipLines(client, Tooltip.splitTooltip(client, Component.literal(tooltip)));
            }
        }
    }

    private static void assertRootTooltipLayouts(
            final ClientGameTestContext context,
            final SmartDropsConfigScreen root
    ) {
        context.runOnClient(client -> {
            final int originalWidth = root.width;
            final int originalHeight = root.height;
            try {
                for (int[] size : List.of(
                        new int[] {320, 180},
                        new int[] {426, 240},
                        new int[] {640, 360},
                        new int[] {1280, 720})) {
                    root.resize(size[0], size[1]);
                    assertGeneralRoot(root, true);
                    assertRootClarityTooltips(client, root);
                    for (AbstractWidget widget : widgets(root)) {
                        final String tooltip = tooltipText(widget);
                        if (!tooltip.isEmpty()) {
                            assertWidgetTooltipOnScreen(client, root, widget, tooltip);
                        }
                    }
                }
            } finally {
                root.resize(originalWidth, originalHeight);
            }
        });
    }

    private static void assertAdvancedTooltips(
            final Minecraft client,
            final StructuredConfigList list,
            final boolean readOnly
    ) {
        require(list.rowCount() == 11,
                "Advanced tooltip regression requires eight settings and three presets");
        final List<StructuredConfigList.Row> settings = list.rows().subList(0, 8);
        require(Set.copyOf(settings.stream().map(row -> row.primary().getString()).toList())
                        .equals(ADVANCED_TOOLTIPS.keySet()),
                "Advanced visible setting labels changed unexpectedly");
        for (StructuredConfigList.Row row : settings) {
            final String label = row.primary().getString();
            final String expected = ADVANCED_TOOLTIPS.get(label);
            final String displayedValue = row.rightDetail().getString();
            require(expected != null && expected.equals(row.tooltip().getString()),
                    "Advanced tooltip did not match its setting-specific explanation: " + label);
            require(displayedValue.contains("ON") || displayedValue.contains("OFF"),
                    "Advanced row stopped displaying its ON/OFF value: " + label);
            require(readOnly == displayedValue.contains("(read-only)"),
                    "Advanced read-only presentation did not match authority: " + label);
            require(!row.tooltip().getString().equals(label + ": " + displayedValue),
                    "Advanced tooltip merely repeated its label and value: " + label);
        }

        final List<StructuredConfigList.Row> presets = list.rows().subList(8, 11);
        require(Set.copyOf(presets.stream().map(row -> row.secondary().getString()).toList())
                        .equals(PRESET_TOOLTIPS.keySet()),
                "Advanced preset labels changed unexpectedly");
        for (StructuredConfigList.Row row : presets) {
            final String name = row.secondary().getString();
            require(PRESET_TOOLTIPS.get(name).equals(row.tooltip().getString()),
                    "Advanced preset tooltip changed unexpectedly: " + name);
        }
        assertStructuredTooltipWrapping(client, list);
    }

    private static void assertMultiplierTooltipPropagation(
            final Minecraft client,
            final Screen screen,
            final String label,
            final String expectedTooltip
    ) {
        final List<AbstractWidget> tooltipWidgets = widgets(screen).stream()
                .filter(widget -> expectedTooltip.equals(tooltipText(widget)))
                .toList();
        final List<String> messages = tooltipWidgets.stream()
                .map(widget -> widget.getMessage().getString())
                .toList();
        require(tooltipWidgets.size() == 4,
                label + " tooltip was not attached to exactly four MultiplierControl widgets");
        require(messages.contains(label) && messages.contains("-") && messages.contains("+")
                        && messages.stream()
                                .filter(message -> !label.equals(message)
                                        && !"-".equals(message)
                                        && !"+".equals(message))
                                .count() == 1,
                label + " tooltip did not cover its label, value, minus, and plus widgets");
        for (AbstractWidget widget : tooltipWidgets) {
            assertWrappedTooltipLines(
                    client,
                    Tooltip.splitTooltip(client, Component.literal(tooltipText(widget))));
        }
    }

    private static void assertWidgetTooltip(
            final AbstractWidget widget,
            final String expected,
            final String description
    ) {
        require(expected.equals(tooltipText(widget)),
                description + " was missing or had unexpected text");
    }

    private static void assertWidgetTooltipOnScreen(
            final Minecraft client,
            final Screen screen,
            final AbstractWidget widget,
            final String tooltip
    ) {
        final List<FormattedCharSequence> lines = Tooltip.splitTooltip(
                client,
                Component.literal(tooltip));
        final int tooltipWidth = lines.stream()
                .mapToInt(client.font::width)
                .max()
                .orElseThrow();
        final int tooltipHeight = lines.size() == 1 ? 8 : lines.size() * 10;
        final int mouseX = Math.max(0, Math.min(screen.width - 1, widget.getX() + widget.getWidth() / 2));
        final int mouseY = Math.max(0, Math.min(screen.height - 1, widget.getY() + widget.getHeight() / 2));
        final Vector2ic position = new MenuTooltipPositioner(widget.getRectangle()).positionTooltip(
                screen.width,
                screen.height,
                mouseX,
                mouseY,
                tooltipWidth,
                tooltipHeight);
        require(position.x() - 3 >= 0
                        && position.y() - 3 >= 0
                        && position.x() + tooltipWidth + 3 <= screen.width
                        && position.y() + tooltipHeight + 3 <= screen.height,
                "Tooltip left the " + screen.width + "x" + screen.height
                        + " viewport for widget " + widget.getMessage().getString());
    }

    private static void assertReadOnlyRoot(final SmartDropsConfigScreen screen) {
        for (Button button : buttons(screen)) {
            final String label = button.getMessage().getString();
            if (ROOT_NAVIGATION_LABELS.contains(label)) {
                require(button.active, "Read-only navigation was disabled: " + label);
            } else {
                require(!button.active, "Mutation button was active for a non-operator: " + label);
            }
        }
    }

    private static void assertXpControlsDisabledWhileOff(
            final ClientGameTestContext context,
            final SmartDropsConfigScreen root
    ) {
        final boolean originallyEnabled = root.editorSession().multiplyExperience();
        if (originallyEnabled) {
            clickButtonWithPrefix(context, root, "Multiply Block XP:");
        }

        final List<Button> decrements = buttonsWithLabel(root, "-");
        final List<Button> increments = buttonsWithLabel(root, "+");
        require(decrements.size() == 2 && increments.size() == 2,
                "Expected independent global and XP multiplier adjustment buttons");
        require(!decrements.getLast().active && !increments.getLast().active,
                "XP multiplier adjustments stayed active while XP multiplication was off");

        if (originallyEnabled) {
            clickButtonWithPrefix(context, root, "Multiply Block XP:");
            require(!root.editorSession().isDirty(), "Restoring the XP toggle did not return to clean state");
        }
    }

    private static void assertGlobalDirtyRoundTrip(
            final ClientGameTestContext context,
        final SmartDropsConfigScreen root
    ) {
        final int original = root.editorSession().globalMultiplier();
        final int direction = original < root.editorSession().maximumMultiplier() ? 1 : -1;
        final String firstLabel = direction > 0 ? "+" : "-";
        final String revertLabel = direction > 0 ? "-" : "+";
        final Button globalAdjustment = buttonsWithLabel(root, firstLabel).get(0);
        require(globalAdjustment.active, "Global adjustment button was unexpectedly disabled");
        context.clickScreenButton(firstLabel);
        require(root.editorSession().globalMultiplier() == original + direction,
                "Global adjustment button did not update the staged multiplier");
        require(root.editorSession().isDirty(), "Global adjustment button did not dirty the editor session");
        require(root.applyButton().active, "Apply did not activate when the session became dirty");
        require(buttons(root).stream()
                        .anyMatch(button -> "Discard Changes".equals(button.getMessage().getString())),
                "Dirty root did not make the discard action explicit");

        context.clickScreenButton(revertLabel);
        require(root.editorSession().globalMultiplier() == original,
                "Global minus button did not restore the original multiplier");
        require(!root.editorSession().isDirty(), "Reverting the global multiplier did not clear dirty state");
        require(!root.applyButton().active, "Apply did not disable after reverting all changes");
    }

    private static void assertCommonControlRoundTrips(
            final ClientGameTestContext context,
            final SmartDropsConfigScreen root
    ) {
        final ConfigEditorSession session = root.editorSession();

        final boolean protection = session.smartPlacementProtection();
        clickButtonWithPrefix(context, root, "Smart Placement Protection:");
        require(session.smartPlacementProtection() != protection && session.isDirty(),
                "Protection toggle did not stage a change");
        clickButtonWithPrefix(context, root, "Smart Placement Protection:");
        require(session.smartPlacementProtection() == protection && !session.isDirty(),
                "Protection toggle did not restore clean state");

        final var source = session.sourceMode();
        clickButtonWithPrefix(context, root, "Multiplier Source:");
        require(session.sourceMode() != source && session.isDirty(),
                "Source selector did not stage a change");
        for (int attempts = 0; attempts < 3 && session.sourceMode() != source; attempts++) {
            clickButtonWithPrefix(context, root, "Multiplier Source:");
        }
        require(session.sourceMode() == source && !session.isDirty(),
                "Source selector did not cycle back to clean state");

        final boolean multiplyXp = session.multiplyExperience();
        clickButtonWithPrefix(context, root, "Multiply Block XP:");
        require(session.multiplyExperience() != multiplyXp && session.isDirty(),
                "Experience toggle did not stage a change");
        clickButtonWithPrefix(context, root, "Multiply Block XP:");
        require(session.multiplyExperience() == multiplyXp && !session.isDirty(),
                "Experience toggle did not restore clean state");

        final int original = session.globalMultiplier();
        session.setGlobalMultiplier(0);
        require(session.globalMultiplier() == 0, "0x was not accepted as a global multiplier");
        session.setGlobalMultiplier(session.maximumMultiplier());
        require(session.globalMultiplier() == session.maximumMultiplier(),
                "Maximum global multiplier was not accepted");
        session.setGlobalMultiplier(original);
        require(!session.isDirty(), "Multiplier boundary checks did not restore clean state");
    }

    private static void assertRuntimeCatalog(final ConfigEditorSession session) {
        require(session.blockCatalog().size() >= 1_000,
                "Runtime block catalog was unexpectedly small: " + session.blockCatalog().size());
        require(session.searchBlocks("diamond").stream()
                        .anyMatch(info -> "minecraft:diamond_ore".equals(info.id())),
                "Display-name block search did not find Diamond Ore");
        require(session.searchBlocks("minecraft:diamond_ore").stream()
                        .anyMatch(info -> "minecraft:diamond_ore".equals(info.id())),
                "Exact registry-ID block search did not find Diamond Ore");
        require(session.searchBlocks("acacia").stream()
                        .anyMatch(info -> info.id().startsWith("minecraft:acacia_")),
                "Block search did not find Acacia blocks");
        require(session.categoryBlockCount(Category.LOGS) > 0,
                "Runtime category catalog did not resolve Logs / Wood blocks");
        require(session.categoryBlockCount(Category.ORES) > 0,
                "Runtime category catalog did not resolve Ore blocks");

        final Integer originalCategory = session.categoryMultiplier(Category.ORES);
        final Integer originalBlock = session.blockMultiplier("minecraft:diamond_ore");
        final int categoryValue = originalCategory == null || originalCategory != 3 ? 3 : 4;
        session.setBlockMultiplier("minecraft:diamond_ore", null);
        session.setCategoryMultiplier(Category.ORES, categoryValue);
        ConfigEditorSession.EffectiveValue effective =
                session.effectiveBlockValue("minecraft:diamond_ore");
        require(effective.multiplier() == categoryValue
                        && effective.sourceTier() == ConfigEditorSession.SourceTier.CATEGORY,
                "Inherited Diamond Ore value did not update from its category");
        session.setBlockMultiplier("minecraft:diamond_ore", 0);
        effective = session.effectiveBlockValue("minecraft:diamond_ore");
        require(effective.multiplier() == 0
                        && effective.sourceTier() == ConfigEditorSession.SourceTier.BLOCK,
                "An explicit 0x block override was confused with Inherit");

        session.setBlockMultiplier("minecraft:diamond_ore", originalBlock);
        session.setCategoryMultiplier(Category.ORES, originalCategory);
        require(!session.isDirty(), "Restoring hierarchy test values did not return to clean state");
    }

    private static void stageComplexDraft(final ConfigEditorSession session) {
        final int alternateGlobal = session.globalMultiplier() == 0
                ? Math.min(4, session.maximumMultiplier())
                : 0;
        final int ruleValue = Math.min(8, session.maximumMultiplier());
        final int alternateRuleValue = ruleValue == 0 ? 1 : ruleValue;
        final int dimensionValue = Math.min(3, session.maximumMultiplier());

        session.setGlobalMultiplier(alternateGlobal);
        session.setSmartPlacementProtection(!session.smartPlacementProtection());
        session.setSourceMode(switch (session.sourceMode()) {
            case NATURAL_ONLY -> SmartDropsConfig.SourceMode.ALL;
            case ALL, PLAYER_PLACED_ONLY -> SmartDropsConfig.SourceMode.NATURAL_ONLY;
        });
        session.setMultiplyExperience(!session.multiplyExperience());
        session.setExperienceMultiplier(Math.min(6, session.maximumMultiplier()));
        session.setCategoryMultiplier(Category.ORES,
                Objects.equals(session.categoryMultiplier(Category.ORES), alternateRuleValue)
                        ? 0
                        : alternateRuleValue);
        session.setBlockMultiplier("minecraft:diamond_ore",
                Objects.equals(session.blockMultiplier("minecraft:diamond_ore"), alternateRuleValue)
                        ? 0
                        : alternateRuleValue);
        session.setDimensionMultiplier("minecraft:the_nether",
                Objects.equals(session.dimensionMultiplier("minecraft:the_nether"), dimensionValue)
                        ? 0
                        : dimensionValue);
        session.setFilterMode(session.filterMode() == SmartDropsConfig.FilterMode.BLACKLIST
                ? SmartDropsConfig.FilterMode.WHITELIST
                : SmartDropsConfig.FilterMode.BLACKLIST);
        session.setFilterState("minecraft:diamond_ore",
                session.filterState("minecraft:diamond_ore") == ConfigEditorSession.FilterEntryState.WHITELIST
                        ? ConfigEditorSession.FilterEntryState.BLACKLIST
                        : ConfigEditorSession.FilterEntryState.WHITELIST);
        session.setEnabled(!session.enabled());
        session.setProtectBlockEntities(!session.protectBlockEntities());
        session.setPlayerMining(!session.playerMining());
        session.setExplosions(!session.explosions());
        session.setAutomatedMining(!session.automatedMining());
        session.setConservativePistonProtection(!session.conservativePistonProtection());
        session.setAllowPlayerOverrides(!session.allowPlayerOverrides());
        session.setStatisticsEnabled(!session.statisticsEnabled());
        session.setEntityDropsEnabled(!session.entityDropsEnabled());
        session.setDefaultEntityMultiplier(session.defaultEntityMultiplier() == null
                ? Math.min(5, session.maximumMultiplier())
                : null);
        session.setEntityKillRequirement(switch (session.entityKillRequirement()) {
            case PLAYER_KILLS_ONLY -> SmartDropsConfig.EntityKillRequirement.PLAYER_OR_TAMED_ENTITY;
            case PLAYER_OR_TAMED_ENTITY, ALL_STANDARD_DEATH_LOOT ->
                    SmartDropsConfig.EntityKillRequirement.PLAYER_KILLS_ONLY;
        });
        session.setEntityFilterMode(session.entityFilterMode() == SmartDropsConfig.FilterMode.BLACKLIST
                ? SmartDropsConfig.FilterMode.WHITELIST
                : SmartDropsConfig.FilterMode.BLACKLIST);
        session.setBossDropsEnabled(!session.bossDropsEnabled());
        session.setMultiplyMobExperience(!session.multiplyMobExperience());
        session.setMobExperienceMultiplier(session.mobExperienceMultiplier() == 1
                ? Math.min(3, session.maximumMultiplier())
                : 1);
        session.setMultiplyBossExperience(!session.multiplyBossExperience());
        session.setEntityCategoryMultiplier(
                EntityCategory.PASSIVE,
                Objects.equals(session.entityCategoryMultiplier(EntityCategory.PASSIVE), alternateRuleValue)
                        ? 0
                        : alternateRuleValue);
        session.setEntityMultiplier(
                "minecraft:cow",
                Objects.equals(session.entityMultiplier("minecraft:cow"), alternateRuleValue)
                        ? 0
                        : alternateRuleValue);
        session.setEntityFilterState(
                "minecraft:cow",
                session.entityFilterState("minecraft:cow") == ConfigEditorSession.FilterEntryState.WHITELIST
                        ? ConfigEditorSession.FilterEntryState.BLACKLIST
                        : ConfigEditorSession.FilterEntryState.WHITELIST);
        session.setEntityTagFilterState(
                "minecraft:raiders",
                session.entityTagFilterState("minecraft:raiders")
                                == ConfigEditorSession.FilterEntryState.WHITELIST
                        ? ConfigEditorSession.FilterEntryState.BLACKLIST
                        : ConfigEditorSession.FilterEntryState.WHITELIST);
        session.setManualShearingDropsEnabled(!session.manualShearingDropsEnabled());
        session.setAutomatedShearingDropsEnabled(!session.automatedShearingDropsEnabled());
        session.setDefaultShearingMultiplier(session.defaultShearingMultiplier() == null
                ? Math.min(5, session.maximumMultiplier())
                : null);
        session.setShearingEntityMultiplier(
                "minecraft:sheep",
                Objects.equals(
                        session.shearingEntityMultiplier("minecraft:sheep"),
                        alternateRuleValue)
                        ? 0
                        : alternateRuleValue);
    }

    private static void assertCleanDefaultEditor(final SmartDropsConfigScreen screen) {
        assertGeneralRoot(screen, true);
        require(!screen.editorSession().isDirty(), "Reset acknowledgement returned a dirty editor session");
        require(!screen.applyButton().active, "Apply remained active after Reset Everything");
        require(sameConfiguration(SmartDropsConfig.defaults(), screen.editorSession().workingSnapshot()),
                "Refreshed editor did not immediately display complete authoritative defaults");
    }

    private static boolean sameConfiguration(
            final SmartDropsConfig left,
            final SmartDropsConfig right
    ) {
        return left.schemaVersion == right.schemaVersion
                && left.enabled == right.enabled
                && left.globalMultiplier == right.globalMultiplier
                && left.maximumMultiplier == right.maximumMultiplier
                && left.sourceMode == right.sourceMode
                && left.filterMode == right.filterMode
                && left.smartPlacementProtection == right.smartPlacementProtection
                && left.protectBlockEntities == right.protectBlockEntities
                && left.playerMining == right.playerMining
                && left.explosions == right.explosions
                && left.automatedMining == right.automatedMining
                && left.multiplyExperience == right.multiplyExperience
                && left.experienceMultiplier == right.experienceMultiplier
                && left.conservativePistonProtection == right.conservativePistonProtection
                && left.allowPlayerOverrides == right.allowPlayerOverrides
                && left.maxPlayerMultiplier == right.maxPlayerMultiplier
                && left.statisticsEnabled == right.statisticsEnabled
                && left.entityDropsEnabled == right.entityDropsEnabled
                && left.inheritDefaultEntityMultiplier == right.inheritDefaultEntityMultiplier
                && left.defaultEntityMultiplier == right.defaultEntityMultiplier
                && left.entityKillRequirement == right.entityKillRequirement
                && left.entityFilterMode == right.entityFilterMode
                && left.bossDropsEnabled == right.bossDropsEnabled
                && left.multiplyMobExperience == right.multiplyMobExperience
                && left.mobExperienceMultiplier == right.mobExperienceMultiplier
                && left.multiplyBossExperience == right.multiplyBossExperience
                && left.manualShearingDropsEnabled == right.manualShearingDropsEnabled
                && left.automatedShearingDropsEnabled == right.automatedShearingDropsEnabled
                && left.inheritDefaultShearingMultiplier == right.inheritDefaultShearingMultiplier
                && left.defaultShearingMultiplier == right.defaultShearingMultiplier
                && Objects.equals(left.dimensionMultipliers, right.dimensionMultipliers)
                && Objects.equals(left.categoryMultipliers, right.categoryMultipliers)
                && Objects.equals(left.blockMultipliers, right.blockMultipliers)
                && Objects.equals(left.blacklist, right.blacklist)
                && Objects.equals(left.whitelist, right.whitelist)
                && Objects.equals(left.tagBlacklist, right.tagBlacklist)
                && Objects.equals(left.tagWhitelist, right.tagWhitelist)
                && Objects.equals(left.blockEntityAllowlist, right.blockEntityAllowlist)
                && Objects.equals(left.playerMultipliers, right.playerMultipliers)
                && Objects.equals(left.entityCategoryMultipliers, right.entityCategoryMultipliers)
                && Objects.equals(left.entityMultipliers, right.entityMultipliers)
                && Objects.equals(left.entityBlacklist, right.entityBlacklist)
                && Objects.equals(left.entityWhitelist, right.entityWhitelist)
                && Objects.equals(left.entityTagBlacklist, right.entityTagBlacklist)
                && Objects.equals(left.entityTagWhitelist, right.entityTagWhitelist)
                && Objects.equals(left.shearingEntityMultipliers, right.shearingEntityMultipliers);
    }

    private static void copyConfiguration(
            final SmartDropsConfig target,
            final SmartDropsConfig source
    ) {
        target.schemaVersion = source.schemaVersion;
        target.enabled = source.enabled;
        target.globalMultiplier = source.globalMultiplier;
        target.maximumMultiplier = source.maximumMultiplier;
        target.sourceMode = source.sourceMode;
        target.filterMode = source.filterMode;
        target.smartPlacementProtection = source.smartPlacementProtection;
        target.protectBlockEntities = source.protectBlockEntities;
        target.playerMining = source.playerMining;
        target.explosions = source.explosions;
        target.automatedMining = source.automatedMining;
        target.multiplyExperience = source.multiplyExperience;
        target.experienceMultiplier = source.experienceMultiplier;
        target.conservativePistonProtection = source.conservativePistonProtection;
        target.allowPlayerOverrides = source.allowPlayerOverrides;
        target.maxPlayerMultiplier = source.maxPlayerMultiplier;
        target.statisticsEnabled = source.statisticsEnabled;
        target.entityDropsEnabled = source.entityDropsEnabled;
        target.inheritDefaultEntityMultiplier = source.inheritDefaultEntityMultiplier;
        target.defaultEntityMultiplier = source.defaultEntityMultiplier;
        target.entityKillRequirement = source.entityKillRequirement;
        target.entityFilterMode = source.entityFilterMode;
        target.bossDropsEnabled = source.bossDropsEnabled;
        target.multiplyMobExperience = source.multiplyMobExperience;
        target.mobExperienceMultiplier = source.mobExperienceMultiplier;
        target.multiplyBossExperience = source.multiplyBossExperience;
        target.manualShearingDropsEnabled = source.manualShearingDropsEnabled;
        target.automatedShearingDropsEnabled = source.automatedShearingDropsEnabled;
        target.inheritDefaultShearingMultiplier = source.inheritDefaultShearingMultiplier;
        target.defaultShearingMultiplier = source.defaultShearingMultiplier;
        target.dimensionMultipliers.clear();
        target.dimensionMultipliers.putAll(source.dimensionMultipliers);
        target.categoryMultipliers.clear();
        target.categoryMultipliers.putAll(source.categoryMultipliers);
        target.blockMultipliers.clear();
        target.blockMultipliers.putAll(source.blockMultipliers);
        target.blacklist.clear();
        target.blacklist.addAll(source.blacklist);
        target.whitelist.clear();
        target.whitelist.addAll(source.whitelist);
        target.tagBlacklist.clear();
        target.tagBlacklist.addAll(source.tagBlacklist);
        target.tagWhitelist.clear();
        target.tagWhitelist.addAll(source.tagWhitelist);
        target.blockEntityAllowlist.clear();
        target.blockEntityAllowlist.addAll(source.blockEntityAllowlist);
        target.playerMultipliers.clear();
        target.playerMultipliers.putAll(source.playerMultipliers);
        target.entityCategoryMultipliers.clear();
        target.entityCategoryMultipliers.putAll(source.entityCategoryMultipliers);
        target.entityMultipliers.clear();
        target.entityMultipliers.putAll(source.entityMultipliers);
        target.entityBlacklist.clear();
        target.entityBlacklist.addAll(source.entityBlacklist);
        target.entityWhitelist.clear();
        target.entityWhitelist.addAll(source.entityWhitelist);
        target.entityTagBlacklist.clear();
        target.entityTagBlacklist.addAll(source.entityTagBlacklist);
        target.entityTagWhitelist.clear();
        target.entityTagWhitelist.addAll(source.entityTagWhitelist);
        target.shearingEntityMultipliers.clear();
        target.shearingEntityMultipliers.putAll(source.shearingEntityMultipliers);
    }

    private static void clickGlobalAdjustment(
            final ClientGameTestContext context,
            final SmartDropsConfigScreen expectedScreen,
            final int direction
    ) {
        final String label = direction > 0 ? "+" : "-";
        final Button adjustment = buttonsWithLabel(expectedScreen, label).get(0);
        require(adjustment.active, "Requested global adjustment was inactive: " + label);
        context.clickScreenButton(label);
    }

    private static Screen openChild(
            final ClientGameTestContext context,
            final String navigationKey,
            final String simpleClassName,
            final String expectedTitle
    ) {
        context.clickScreenButton(navigationKey);
        context.waitFor(client -> client.screen != null
                && simpleClassName.equals(client.screen.getClass().getSimpleName()));
        return context.computeOnClient(client -> {
            final Screen screen = client.screen;
            require(screen != null && simpleClassName.equals(screen.getClass().getSimpleName()),
                    "Expected " + simpleClassName + ", found "
                            + (screen == null ? "null" : screen.getClass().getName()));
            require(expectedTitle.equals(screen.getTitle().getString()),
                    "Unexpected title for " + simpleClassName + ": " + screen.getTitle().getString());
            return screen;
        });
    }

    private static Screen waitForSimpleScreen(
            final ClientGameTestContext context,
            final String simpleClassName,
            final String expectedTitle
    ) {
        context.waitFor(client -> client.screen != null
                && simpleClassName.equals(client.screen.getClass().getSimpleName()));
        return context.computeOnClient(client -> {
            final Screen screen = client.screen;
            require(screen != null && simpleClassName.equals(screen.getClass().getSimpleName()),
                    "Expected " + simpleClassName + ", found "
                            + (screen == null ? "null" : screen.getClass().getName()));
            require(expectedTitle.equals(screen.getTitle().getString()),
                    "Unexpected title for " + simpleClassName + ": " + screen.getTitle().getString());
            return screen;
        });
    }

    private static void returnToSameRoot(
            final ClientGameTestContext context,
            final SmartDropsConfigScreen expectedRoot,
            final ConfigEditorSession expectedSession
    ) {
        context.clickScreenButton(BACK_KEY);
        context.waitForScreen(SmartDropsConfigScreen.class);
        final SmartDropsConfigScreen returned = currentConfigScreen(context);
        require(returned == expectedRoot, "Back rebuilt the General screen instead of returning to its root");
        require(returned.editorSession() == expectedSession,
                "Back discarded the shared editor session");
    }

    private static void assertBoundedChild(final Screen screen, final int maximumRows) {
        require(screen.children().size() >= 2 && screen.children().size() <= 5,
                "Child screen constructed an unbounded widget set: " + screen.children().size());
        final List<StructuredConfigList> lists = screen.children().stream()
                .filter(StructuredConfigList.class::isInstance)
                .map(StructuredConfigList.class::cast)
                .toList();
        require(lists.size() == 1, "Expected exactly one lightweight scrolling list");
        require(lists.get(0).rowCount() <= maximumRows,
                "Child screen materialized too many rows: " + lists.get(0).rowCount());
    }

    private static void assertUnsavedChangesIndicator(
            final Screen screen,
            final boolean expected,
            final String message
    ) {
        require(screen instanceof SmartDropsSubScreen,
                "Expected a Smart Resource Multiplier child screen, found "
                        + screen.getClass().getName());
        require(((SmartDropsSubScreen)screen).unsavedChangesIndicatorVisible() == expected, message);
    }

    private static void assertCompactUnsavedChangesLayout(final Screen screen) {
        require(screen instanceof SmartDropsSubScreen,
                "Compact indicator check requires a Smart Resource Multiplier child screen");
        final SmartDropsSubScreen child = (SmartDropsSubScreen)screen;
        final SmartDropsSubScreen.UnsavedChangesIndicatorLayout layout =
                child.unsavedChangesIndicatorLayout();
        final Button back = buttons(screen).stream()
                .filter(button -> BACK_KEY.equals(button.getMessage().getString()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Compact child omitted Back"));
        require(layout.stacked(),
                "320x180 did not use the compact above-Back indicator layout");
        require(layout.x() >= 0 && layout.x() + layout.width() <= 320
                        && layout.y() >= 0 && layout.y() + layout.height() <= 180,
                "Compact unsaved-changes indicator left the 320x180 viewport");
        require(layout.y() >= child.contentBottom() + 4,
                "Compact unsaved-changes indicator overlapped child content");
        require(layout.y() + layout.height() <= back.getY(),
                "Compact unsaved-changes indicator overlapped Back");
    }

    private static StructuredConfigList onlyList(final Screen screen) {
        final List<StructuredConfigList> lists = screen.children().stream()
                .filter(StructuredConfigList.class::isInstance)
                .map(StructuredConfigList.class::cast)
                .toList();
        require(lists.size() == 1, "Expected exactly one structured list on "
                + screen.getClass().getSimpleName());
        return lists.get(0);
    }

    private static StructuredConfigList.Row rowWithPrimary(
            final StructuredConfigList list,
            final String primary
    ) {
        return list.rows().stream()
                .filter(row -> primary.equals(row.primary().getString()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Missing structured row '" + primary + "'"));
    }

    private static EditBox onlySearchBox(final Screen screen) {
        final List<EditBox> searches = screen.children().stream()
                .filter(EditBox.class::isInstance)
                .map(EditBox.class::cast)
                .toList();
        require(searches.size() == 1, "Expected exactly one search box on "
                + screen.getClass().getSimpleName());
        return searches.get(0);
    }

    private static boolean hasWidgetLabel(final Screen screen, final String label) {
        return widgets(screen).stream()
                .anyMatch(widget -> label.equals(widget.getMessage().getString()));
    }

    private static List<AbstractWidget> widgets(final Screen screen) {
        return screen.children().stream()
                .filter(AbstractWidget.class::isInstance)
                .map(AbstractWidget.class::cast)
                .toList();
    }

    private static List<Button> buttons(final Screen screen) {
        return screen.children().stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .toList();
    }

    private static Button buttonWithLabel(final Screen screen, final String label) {
        return buttons(screen).stream()
                .filter(button -> label.equals(button.getMessage().getString()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing button: " + label));
    }

    private static String tooltipText(final AbstractWidget widget) {
        final NarrationHintCapture capture = new NarrationHintCapture();
        widget.updateNarration(capture);
        require(capture.hints.size() <= 1,
                "Widget exposed more than one tooltip narration hint: "
                        + widget.getMessage().getString());
        return capture.hints.isEmpty() ? "" : capture.hints.get(0);
    }

    private static List<Button> buttonsWithLabel(final Screen screen, final String label) {
        return buttons(screen).stream()
                .filter(button -> label.equals(button.getMessage().getString()))
                .sorted(Comparator.comparingInt(Button::getY))
                .toList();
    }

    private static void clickButtonWithPrefix(
            final ClientGameTestContext context,
            final Screen screen,
            final String prefix
    ) {
        final String label = buttons(screen).stream()
                .map(button -> button.getMessage().getString())
                .filter(value -> value.startsWith(prefix))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing button beginning with: " + prefix));
        context.clickScreenButton(label);
    }

    private static void takeRequiredScreenshot(final ClientGameTestContext context, final String name) {
        final Path screenshot = context.takeScreenshot(name);
        require(screenshot != null, "Screenshot was not created: " + name);
    }

    private static void openAuthoritativeScreen(final ClientGameTestContext context) {
        context.setScreen(() -> SmartDropsConfigScreens.create(null));
        context.waitForScreen(SmartDropsConfigScreen.class);
    }

    private static SmartDropsConfigScreen currentConfigScreen(final ClientGameTestContext context) {
        return context.computeOnClient(client -> {
            require(
                    client.screen instanceof SmartDropsConfigScreen,
                    "Expected SmartDropsConfigScreen, found "
                            + (client.screen == null ? "null" : client.screen.getClass().getName()));
            return (SmartDropsConfigScreen)client.screen;
        });
    }

    private static void sendGlobalPatch(
            final ClientGameTestContext context,
            final int requestId,
            final long expectedRevision,
            final int value
    ) {
        final ConfigPatch patch = new ConfigPatch();
        patch.globalMultiplier = value;
        final String json = ConfigManager.encodeClientPatch(patch);
        context.runOnClient(client ->
                ClientPlayNetworking.send(new ConfigPatchPayload(requestId, expectedRevision, json)));
    }

    private static void sendResetRequest(
            final ClientGameTestContext context,
            final int requestId,
            final long expectedRevision
    ) {
        context.runOnClient(client -> ClientPlayNetworking.send(
                new ConfigResetPayload(requestId, expectedRevision)));
    }

    private static void assertStructuredTooltipWrapping(
            final Minecraft client,
            final StructuredConfigList list
    ) {
        final List<FormattedCharSequence> explicitLines = Tooltip.splitTooltip(
                client,
                Component.literal("First line\nSecond line"));
        require(explicitLines.size() >= 2,
                "Minecraft tooltip splitting did not consume an explicit line break");
        assertWrappedTooltipLines(client, explicitLines);

        for (StructuredConfigList.Row row : list.rows()) {
            if (!row.tooltip().getString().isEmpty()) {
                assertWrappedTooltipLines(client, Tooltip.splitTooltip(client, row.tooltip()));
            }
        }
    }

    private static void assertWrappedTooltipLines(
            final Minecraft client,
            final List<FormattedCharSequence> lines
    ) {
        require(!lines.isEmpty(), "Wrapped tooltip unexpectedly produced no lines");
        for (FormattedCharSequence line : lines) {
            require(client.font.width(line) <= VANILLA_TOOLTIP_WIDTH,
                    "Structured tooltip line exceeded vanilla's 170-pixel width");
            final boolean[] controlBreak = {false};
            line.accept((index, style, codePoint) -> {
                if (codePoint == '\n' || codePoint == '\r') {
                    controlBreak[0] = true;
                }
                return true;
            });
            require(!controlBreak[0],
                    "Wrapped tooltip still emitted a line-break control glyph");
        }
    }

    private static final class NarrationHintCapture implements NarrationElementOutput {
        private final List<String> hints = new ArrayList<>();

        @Override
        public void add(final NarratedElementType type, final NarrationThunk<?> narration) {
            if (type == NarratedElementType.HINT) {
                narration.getText(hints::add);
            }
        }

        @Override
        public NarrationElementOutput nest() {
            return this;
        }
    }

    private static void require(final boolean condition, final String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
