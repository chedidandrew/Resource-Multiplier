package com.chedidandrew.smartresourcedrops.client;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.config.ConfigManager;
import com.chedidandrew.smartresourcedrops.config.ConfigScreenOpenPolicy;
import com.chedidandrew.smartresourcedrops.config.SmartDropsConfig;
import com.chedidandrew.smartresourcedrops.core.entity.EntityCategory;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Target-native physical client smoke gate for the Fabric 1.21.2-1.21.3 lane.
 *
 * <p>Fabric API 0.116.x has no client GameTest API. This run-only mod therefore drives the real
 * production screens from a client-tick state machine, writes a deterministic marker, and exits.
 * It is compiled and loaded only by {@code runClientSmoke}; it is never packaged in the mod JAR.</p>
 */
public final class FabricClientSmokeTest implements ClientModInitializer {
    private static final int TIMEOUT_TICKS = 2_400;
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();
    private static final List<Navigation> ROOT_NAVIGATION = List.of(
            new Navigation("Block Categories", "RuleListScreen"),
            new Navigation("Block Overrides", "BlockOverridesScreen"),
            new Navigation("Dimensions", "RuleListScreen"),
            new Navigation("Block Filters", "FilterConfigScreen"),
            new Navigation("Advanced", "AdvancedConfigScreen"),
            new Navigation("Entity Drops", "EntityDropsScreen"));
    private static final List<Navigation> ENTITY_NAVIGATION = List.of(
            new Navigation("Entity Categories", "EntityCategoryScreen"),
            new Navigation("Entity Overrides", "EntityOverridesScreen"),
            new Navigation("Entity Filters", "EntityFilterScreen"),
            new Navigation("Shearing Drops", "ShearingDropsScreen"));
    private static final Set<String> ROOT_NAVIGATION_LABELS = Set.of(
            "Block Categories",
            "Block Overrides",
            "Dimensions",
            "Block Filters",
            "Advanced",
            "Entity Drops");

    private int ticks;
    private int phase;
    private int navigationIndex;
    private Screen initialMenu;
    private SmartDropsConfigScreen root;
    private ConfigEditorSession session;
    private EntityDropsScreen entityParent;
    private int appliedGlobal;
    private boolean stopped;

    @Override
    public void onInitializeClient() {
        if (Boolean.getBoolean("smart_resource_drops.fabricClientSmoke")
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
                throw new AssertionError("Timed out waiting for the Fabric 1.21.2-1.21.3 client smoke test");
            }
            switch (this.phase) {
                case 0 -> this.openLocalRoot(minecraft);
                case 1 -> this.verifyLocalRootAndStartNavigation(minecraft);
                case 2 -> this.verifyRootChildAndReturn(minecraft);
                case 3 -> this.continueRootNavigation(minecraft);
                case 4 -> this.openEntityParent(minecraft);
                case 5 -> this.verifyEntityParentAndOpenChild(minecraft);
                case 6 -> this.verifyEntityChildAndReturn(minecraft);
                case 7 -> this.continueEntityNavigation(minecraft);
                case 8 -> this.returnFromEntityParent(minecraft);
                case 9 -> this.stageAndApplyLocalChange(minecraft);
                case 10 -> this.verifyAppliedLocalChangeAndOpenReset(minecraft);
                case 11 -> this.verifyResetConfirmationAndCancel(minecraft);
                case 12 -> this.verifyResetCancelAndReopen(minecraft);
                case 13 -> this.confirmReset(minecraft);
                case 14 -> this.verifyResetAndConnectedAuthorityScreens(minecraft);
                case 15 -> this.verifyConnectedOperatorScreen(minecraft);
                case 16 -> this.verifyConnectedNonOperatorScreen(minecraft);
                case 17 -> this.finish(minecraft);
                default -> throw new AssertionError("Unexpected client smoke phase " + this.phase);
            }
        } catch (Throwable failure) {
            this.stopped = true;
            SmartResourceDrops.LOGGER.error("Fabric 1.21.2-1.21.3 client smoke test failed", failure);
            minecraft.stop();
        }
    }

    private void openLocalRoot(final Minecraft minecraft) {
        if (!(minecraft.screen instanceof TitleScreen)) {
            return;
        }
        this.initialMenu = minecraft.screen;
        verifyAuthorityPolicy();
        final Screen routed = SmartDropsConfigScreens.create(this.initialMenu);
        require(routed instanceof SmartDropsConfigScreen,
                "Title-menu route did not open editable local defaults");
        this.root = (SmartDropsConfigScreen) routed;
        this.session = this.root.editorSession();
        require(this.session.authority() == ConfigScreenOpenPolicy.Authority.LOCAL_DEFAULTS,
                "Title-menu route did not select local-default authority");
        minecraft.setScreen(this.root);
        this.phase = 1;
    }

    private void verifyLocalRootAndStartNavigation(final Minecraft minecraft) {
        if (minecraft.screen != this.root) {
            return;
        }
        assertGeneralRoot(this.root, true);
        assertBlockExperienceWording(this.root);
        assertEntityCatalog(this.session);
        this.navigationIndex = 0;
        press(this.root, ROOT_NAVIGATION.get(0).label());
        this.phase = 2;
    }

    private void verifyRootChildAndReturn(final Minecraft minecraft) {
        final Navigation expected = ROOT_NAVIGATION.get(this.navigationIndex);
        if (!isScreen(minecraft.screen, expected.className())) {
            return;
        }
        assertChild(minecraft.screen, this.root, this.session, expected.label());
        press(minecraft.screen, "Back");
        this.phase = 3;
    }

    private void continueRootNavigation(final Minecraft minecraft) {
        if (minecraft.screen != this.root) {
            return;
        }
        if (++this.navigationIndex < ROOT_NAVIGATION.size()) {
            press(this.root, ROOT_NAVIGATION.get(this.navigationIndex).label());
            this.phase = 2;
            return;
        }
        this.phase = 4;
    }

    private void openEntityParent(final Minecraft minecraft) {
        if (minecraft.screen != this.root) {
            return;
        }
        press(this.root, "Entity Drops");
        this.phase = 5;
    }

    private void verifyEntityParentAndOpenChild(final Minecraft minecraft) {
        if (!(minecraft.screen instanceof EntityDropsScreen current)) {
            return;
        }
        if (this.entityParent == null) {
            this.entityParent = current;
            assertStructuredTooltipComposition(row(current, "Entity Overrides"));
            if (!this.session.entityDropsEnabled()) {
                row(current, "Entity Drops").action().run();
                require(this.session.entityDropsEnabled(),
                        "Entity Drops row did not enable real category editing");
                require(minecraft.screen == current,
                        "Entity Drops toggle replaced its staged parent screen");
            }
            this.navigationIndex = 0;
        }
        require(current == this.entityParent,
                "Entity child Back navigation replaced the staged Entity Drops screen");
        final Navigation expected = ENTITY_NAVIGATION.get(this.navigationIndex);
        row(current, expected.label()).action().run();
        this.phase = 6;
    }

    private void verifyEntityChildAndReturn(final Minecraft minecraft) {
        final Navigation expected = ENTITY_NAVIGATION.get(this.navigationIndex);
        if (!isScreen(minecraft.screen, expected.className())) {
            return;
        }
        assertChild(minecraft.screen, this.root, this.session, expected.label());
        if (minecraft.screen instanceof EntityCategoryScreen categoryScreen) {
            final StructuredConfigList list = onlyList(categoryScreen);
            require(list.rowCount() == EntityCategory.values().length,
                    "Entity Categories was blank or incomplete: " + list.rowCount());
            final EntityCategory target = EntityCategory.PASSIVE;
            require(this.session.entityCategoryMultiplier(target) == null,
                    "Fresh compatibility-lane defaults unexpectedly configured the Passive entity category");
            row(categoryScreen, ConfigUiText.entityCategoryName(target).getString())
                    .action().run();
            require(minecraft.screen instanceof EntityRuleEditScreen,
                    "Entity Categories Configure did not open its focused editor");
            require(buttons(minecraft.screen).stream()
                            .filter(button -> "+".equals(button.getMessage().getString()))
                            .allMatch(button -> button.active),
                    "Entity Categories multiplier control was not physically editable");
            press(minecraft.screen, "+");
            require(Integer.valueOf(0).equals(this.session.entityCategoryMultiplier(target)),
                    "Entity Categories editor did not stage an explicit 0x override");
            press(minecraft.screen, "Back");
            require(minecraft.screen == categoryScreen
                            && categoryScreen.unsavedChangesIndicatorVisible(),
                    "Entity category edit did not propagate dirty state back to its list");
        }
        press(minecraft.screen, "Back");
        this.phase = 7;
    }

    private void continueEntityNavigation(final Minecraft minecraft) {
        if (minecraft.screen != this.entityParent) {
            return;
        }
        if (++this.navigationIndex < ENTITY_NAVIGATION.size()) {
            this.phase = 5;
            return;
        }
        press(this.entityParent, "Back");
        this.phase = 8;
    }

    private void returnFromEntityParent(final Minecraft minecraft) {
        if (minecraft.screen != this.root) {
            return;
        }
        this.phase = 9;
    }

    private void stageAndApplyLocalChange(final Minecraft minecraft) {
        if (minecraft.screen != this.root) {
            return;
        }
        final int original = this.session.globalMultiplier();
        this.appliedGlobal = original < this.session.maximumMultiplier() ? original + 1 : original - 1;
        require(this.session.setGlobalMultiplier(this.appliedGlobal),
                "Could not stage a local global-multiplier change");
        minecraft.setScreen(this.root);
        require(this.session.isDirty(), "Staged local change did not mark the editor dirty");
        require(this.root.applyButton().active, "Dirty local draft did not enable Apply Changes");
        require(hasWidgetLabel(this.root, "Discard Changes"),
                "Dirty local draft did not make discard behavior explicit");
        press(this.root, "Apply Changes");
        this.phase = 10;
    }

    private void verifyAppliedLocalChangeAndOpenReset(final Minecraft minecraft) {
        if (!(minecraft.screen instanceof SmartDropsConfigScreen current) || current == this.root) {
            return;
        }
        this.root = current;
        this.session = current.editorSession();
        require(this.session.authority() == ConfigScreenOpenPolicy.Authority.LOCAL_DEFAULTS,
                "Local Apply returned a screen with the wrong authority");
        require(!this.session.isDirty() && !this.root.applyButton().active,
                "Local Apply did not return a clean acknowledged editor");
        require(this.session.globalMultiplier() == this.appliedGlobal
                        && ConfigManager.snapshot().globalMultiplier == this.appliedGlobal,
                "Local Apply did not persist and redisplay the staged value");
        require(Integer.valueOf(0).equals(
                        this.session.entityCategoryMultiplier(EntityCategory.PASSIVE)),
                "Local Apply did not persist the Entity Categories child edit");

        final int staged = this.appliedGlobal < this.session.maximumMultiplier()
                ? this.appliedGlobal + 1
                : this.appliedGlobal - 1;
        require(this.session.setGlobalMultiplier(staged),
                "Could not stage the reset-cancellation draft");
        minecraft.setScreen(this.root);
        press(this.root, "Reset All Settings");
        this.phase = 11;
    }

    private void verifyResetConfirmationAndCancel(final Minecraft minecraft) {
        if (!(minecraft.screen instanceof ResetAllSettingsConfirmScreen)) {
            return;
        }
        require(hasWidgetLabel(minecraft.screen, "Reset Everything")
                        && hasWidgetLabel(minecraft.screen, "Cancel"),
                "Reset confirmation omitted its destructive or cancel action");
        press(minecraft.screen, "Cancel");
        this.phase = 12;
    }

    private void verifyResetCancelAndReopen(final Minecraft minecraft) {
        if (minecraft.screen != this.root) {
            return;
        }
        require(this.session.isDirty() && this.root.applyButton().active,
                "Cancel discarded the staged draft or disabled Apply");
        require(ConfigManager.snapshot().globalMultiplier == this.appliedGlobal,
                "Opening/cancelling Reset mutated persisted configuration");
        press(this.root, "Reset All Settings");
        this.phase = 13;
    }

    private void confirmReset(final Minecraft minecraft) {
        if (!(minecraft.screen instanceof ResetAllSettingsConfirmScreen)) {
            return;
        }
        press(minecraft.screen, "Reset Everything");
        this.phase = 14;
    }

    private void verifyResetAndConnectedAuthorityScreens(final Minecraft minecraft) {
        if (!(minecraft.screen instanceof SmartDropsConfigScreen current) || current == this.root) {
            return;
        }
        require(!current.editorSession().isDirty() && !current.applyButton().active,
                "Reset Everything did not return a clean editor");
        require(current.editorSession().globalMultiplier() == SmartDropsConfig.defaults().globalMultiplier
                        && ConfigManager.snapshot().globalMultiplier
                        == SmartDropsConfig.defaults().globalMultiplier,
                "Reset Everything did not restore authoritative defaults");
        require(current.editorSession().entityCategoryMultiplier(EntityCategory.PASSIVE) == null,
                "Reset Everything retained the Entity Categories smoke override");
        require(!current.editorSession().entityDropsEnabled(),
                "Reset Everything retained the Entity Drops enablement used by category editing");

        this.root = new SmartDropsConfigScreen(
                this.initialMenu,
                SmartDropsConfig.defaults(),
                true);
        minecraft.setScreen(this.root);
        this.phase = 15;
    }

    private void verifyConnectedOperatorScreen(final Minecraft minecraft) {
        if (minecraft.screen != this.root) {
            return;
        }
        this.session = this.root.editorSession();
        require(this.session.authority() == ConfigScreenOpenPolicy.Authority.CONNECTED_SERVER
                        && this.session.editable(),
                "Connected operator snapshot was not editable server authority");
        assertGeneralRoot(this.root, true);
        final int changed = this.session.globalMultiplier() + 1;
        require(this.session.setGlobalMultiplier(changed),
                "Connected operator could not stage an authoritative change");
        minecraft.setScreen(this.root);
        require(this.root.applyButton().active,
                "Connected operator dirty draft did not enable Apply");

        this.root = new SmartDropsConfigScreen(
                this.initialMenu,
                SmartDropsConfig.defaults(),
                false);
        minecraft.setScreen(this.root);
        this.phase = 16;
    }

    private void verifyConnectedNonOperatorScreen(final Minecraft minecraft) {
        if (minecraft.screen != this.root) {
            return;
        }
        this.session = this.root.editorSession();
        require(this.session.authority() == ConfigScreenOpenPolicy.Authority.CONNECTED_SERVER
                        && !this.session.editable(),
                "Connected non-operator snapshot did not open read-only server authority");
        assertGeneralRoot(this.root, false);
        require(!this.session.setGlobalMultiplier(this.session.globalMultiplier() + 1)
                        && !this.session.isDirty(),
                "Connected non-operator could mutate the staged server snapshot");
        for (Button button : buttons(this.root)) {
            final String label = button.getMessage().getString();
            if (ROOT_NAVIGATION_LABELS.contains(label)) {
                require(button.active, "Read-only child navigation was disabled: " + label);
            }
        }
        this.phase = 17;
    }

    private void finish(final Minecraft minecraft) throws Exception {
        final String markerName = System.getProperty(
                "smart_resource_drops.fabricClientSmokeMarker",
                "client-smoke.success");
        final Path marker = Path.of(markerName).toAbsolutePath().normalize();
        final Path parent = marker.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(
                marker,
                "Fabric 1.21.2-1.21.3 client GUI/authority smoke passed\n",
                StandardCharsets.UTF_8);
        SmartResourceDrops.LOGGER.info(
                "Fabric 1.21.2-1.21.3 client smoke passed: block-XP wording, non-empty entity categories, navigation/back, local apply/reset, and connected operator/non-operator authority");
        this.stopped = true;
        minecraft.stop();
    }

    private static void verifyAuthorityPolicy() {
        assertDecision(false, false, false,
                ConfigScreenOpenPolicy.Authority.LOCAL_DEFAULTS,
                ConfigScreenOpenPolicy.InitialPhase.READY,
                "disconnected title menu");
        assertDecision(false, true, false,
                ConfigScreenOpenPolicy.Authority.CONNECTED_SERVER,
                ConfigScreenOpenPolicy.InitialPhase.LOADING,
                "integrated server before play connection");
        assertDecision(true, true, false,
                ConfigScreenOpenPolicy.Authority.CONNECTED_SERVER,
                ConfigScreenOpenPolicy.InitialPhase.LOADING,
                "integrated server before cached snapshot");
        assertDecision(true, true, true,
                ConfigScreenOpenPolicy.Authority.CONNECTED_SERVER,
                ConfigScreenOpenPolicy.InitialPhase.READY,
                "integrated server owner snapshot");
        assertDecision(true, false, false,
                ConfigScreenOpenPolicy.Authority.CONNECTED_SERVER,
                ConfigScreenOpenPolicy.InitialPhase.LOADING,
                "dedicated server before cached snapshot");
        assertDecision(true, false, true,
                ConfigScreenOpenPolicy.Authority.CONNECTED_SERVER,
                ConfigScreenOpenPolicy.InitialPhase.READY,
                "dedicated server operator/non-operator snapshot");
    }

    private static void assertDecision(
            final boolean connected,
            final boolean integrated,
            final boolean cached,
            final ConfigScreenOpenPolicy.Authority authority,
            final ConfigScreenOpenPolicy.InitialPhase phase,
            final String route
    ) {
        final ConfigScreenOpenPolicy.Decision decision = ConfigScreenOpenPolicy.decide(
                connected,
                integrated,
                cached);
        require(decision.authority() == authority && decision.phase() == phase,
                "Unexpected authority decision for " + route + ": " + decision);
    }

    private static void assertGeneralRoot(
            final SmartDropsConfigScreen screen,
            final boolean editable
    ) {
        require(screen.editorSession().editable() == editable,
                "General screen editability did not match authority");
        require(screen.applyButton() != null && screen.resetButton() != null,
                "General screen omitted Apply or Reset");
        require(screen.resetButton().active == editable,
                "Reset availability did not match authority");
        for (String label : ROOT_NAVIGATION_LABELS) {
            require(buttons(screen).stream()
                            .anyMatch(button -> label.equals(button.getMessage().getString())),
                    "General screen omitted navigation: " + label);
        }
        assertMultiplierValueAreasCentered(screen);
    }

    private static void assertMultiplierValueAreasCentered(final Screen screen) {
        final List<AbstractWidget> allWidgets = widgets(screen);
        int checked = 0;
        for (AbstractWidget widget : allWidgets) {
            if (!(widget instanceof StringWidget valueWidget)
                    || !valueWidget.getMessage().getString().matches("\\d+x")) {
                continue;
            }
            final Button decrement = buttons(screen).stream()
                    .filter(button -> "-".equals(button.getMessage().getString()))
                    .filter(button -> button.getY() == valueWidget.getY() && button.getX() < valueWidget.getX())
                    .max(java.util.Comparator.comparingInt(Button::getX))
                    .orElseThrow(() -> new AssertionError("Multiplier value had no decrement button"));
            final Button increment = buttons(screen).stream()
                    .filter(button -> "+".equals(button.getMessage().getString()))
                    .filter(button -> button.getY() == valueWidget.getY() && button.getX() > valueWidget.getX())
                    .min(java.util.Comparator.comparingInt(Button::getX))
                    .orElseThrow(() -> new AssertionError("Multiplier value had no increment button"));
            final int valueCenterTwice = (2 * valueWidget.getX()) + valueWidget.getWidth();
            final int gapCenterTwice = decrement.getX() + decrement.getWidth() + increment.getX();
            require(valueCenterTwice == gapCenterTwice,
                    "Multiplier value area was not centered between its buttons");
            checked++;
        }
        require(checked >= 2, "General screen did not expose both centered multiplier values");
    }

    private static void assertStructuredTooltipComposition(final StructuredConfigList.Row row) {
        final String truncated = StructuredConfigList.composeHoverText(row, true).getString();
        final List<String> visibleFields = List.of(
                        row.primary(), row.secondary(), row.leftDetail(), row.rightDetail())
                .stream()
                .map(Component::getString)
                .filter(value -> !value.isEmpty())
                .toList();
        for (String field : visibleFields) {
            require(truncated.lines().filter(field::equals).count() == 1,
                    "Structured tooltip repeated visible row text: " + field);
        }
        require(StructuredConfigList.composeHoverText(row, false).getString().isEmpty(),
                "Untruncated structured row retained duplicate-only tooltip text");
        final String narration = StructuredConfigList.composeNarrationText(row).getString();
        for (String field : visibleFields) {
            require(narration.indexOf(field) == narration.lastIndexOf(field),
                    "Structured-row narration repeated visible row text: " + field);
        }
    }

    private static void assertBlockExperienceWording(final Screen screen) {
        final List<String> labels = widgets(screen).stream()
                .map(widget -> widget.getMessage().getString())
                .toList();
        require(labels.stream().anyMatch(label -> label.startsWith("Multiply Block XP:")),
                "General screen did not identify the block-only XP toggle");
        require(labels.contains("Block XP Multiplier"),
                "General screen did not identify the block-only XP multiplier");
        require(labels.stream().noneMatch(label -> label.contains("Multiply Experience")),
                "Ambiguous legacy Multiply Experience wording is still visible");
        require(Component.translatable("smart_resource_drops.gui.multiply_xp_tooltip")
                        .getString().contains("Mob XP is configured separately under Entity Drops"),
                "Block XP tooltip no longer separates mob/entity XP");
    }

    private static void assertEntityCatalog(final ConfigEditorSession session) {
        require(!session.entityCategories().isEmpty(), "Entity category model was empty");
        require(session.entityCatalog().size() > 20,
                "Target-native entity catalog was unexpectedly small: " + session.entityCatalog().size());
        assertSelected(session, "minecraft:enderman", EntityCategory.NEUTRAL);
        assertSelected(session, "minecraft:iron_golem", EntityCategory.GOLEMS);
        assertSelected(session, "minecraft:cow", EntityCategory.PASSIVE);
        assertSelected(session, "minecraft:zombie", EntityCategory.HOSTILE);
        require(session.entityInfo("minecraft:copper_golem").isEmpty(),
                "1.21.2-1.21.3 catalog unexpectedly exposed the later copper golem entity");
    }

    private static void assertSelected(
            final ConfigEditorSession session,
            final String entityId,
            final EntityCategory expected
    ) {
        final ConfigEditorSession.EntityInfo info = session.entityInfo(entityId)
                .orElseThrow(() -> new AssertionError("Missing entity catalog row " + entityId));
        require(info.selectedCategory() == expected && !info.categoryEstimated(),
                entityId + " resolved as " + info.selectedCategory()
                        + " (estimated=" + info.categoryEstimated() + "), expected " + expected);
    }

    private static void assertChild(
            final Screen screen,
            final SmartDropsConfigScreen root,
            final ConfigEditorSession session,
            final String route
    ) {
        require(screen instanceof SmartDropsSubScreen,
                route + " did not open a staged child screen");
        final SmartDropsSubScreen child = (SmartDropsSubScreen) screen;
        require(child.root == root && child.session == session,
                route + " did not retain the exact root/session");
        require(hasWidgetLabel(screen, "Back"), route + " omitted Back navigation");
    }

    private static StructuredConfigList onlyList(final Screen screen) {
        final List<StructuredConfigList> lists = screen.children().stream()
                .filter(StructuredConfigList.class::isInstance)
                .map(StructuredConfigList.class::cast)
                .toList();
        require(lists.size() == 1,
                "Expected one structured list on " + screen.getClass().getSimpleName());
        return lists.get(0);
    }

    private static StructuredConfigList.Row row(final Screen screen, final String label) {
        return onlyList(screen).rows().stream()
                .filter(row -> label.equals(row.primary().getString()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        screen.getClass().getSimpleName() + " omitted row " + label));
    }

    private static boolean isScreen(final Screen screen, final String className) {
        return screen != null && className.equals(screen.getClass().getSimpleName());
    }

    private static void press(final Screen screen, final String label) {
        buttons(screen).stream()
                .filter(button -> label.equals(button.getMessage().getString()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        screen.getClass().getSimpleName() + " omitted button " + label))
                .onPress();
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

    private static void require(final boolean condition, final String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private record Navigation(String label, String className) {
    }
}
