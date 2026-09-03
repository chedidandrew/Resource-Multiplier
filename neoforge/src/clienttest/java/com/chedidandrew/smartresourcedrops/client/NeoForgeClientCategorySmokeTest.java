package com.chedidandrew.smartresourcedrops.client;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.core.Category;
import com.chedidandrew.smartresourcedrops.core.entity.EntityCategory;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Test-run-only check for NeoForge resource discovery and the formerly blank category screen. */
@Mod.EventBusSubscriber(modid = SmartResourceDrops.MOD_ID, value = Dist.CLIENT)
public final class NeoForgeClientCategorySmokeTest {
    private static final int TIMEOUT_TICKS = 2_400;
    private static final NeoForgeClientCategorySmokeTest INSTANCE = new NeoForgeClientCategorySmokeTest();
    private int ticks;
    private int phase;
    private SmartDropsConfigScreen rootScreen;
    private EntityDropsScreen entityDropsScreen;
    private EntityCategoryScreen categoryScreen;
    private ConfigEditorSession session;

    private NeoForgeClientCategorySmokeTest() {
    }

    @SubscribeEvent
    public static void tick(final TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END
                && Boolean.getBoolean("smart_resource_drops.clientCategoryTest")) {
            INSTANCE.onClientTick();
        }
    }

    private void onClientTick() {
        final Minecraft minecraft = Minecraft.getInstance();
        try {
            if (minecraft.getOverlay() != null) {
                return;
            }
            if (++this.ticks > TIMEOUT_TICKS) {
                throw new AssertionError("Timed out waiting for the NeoForge category smoke test");
            }
            if (this.rootScreen == null) {
                final Screen initialMenu = minecraft.screen;
                if (!(initialMenu instanceof TitleScreen)
                        && !(initialMenu instanceof AccessibilityOnboardingScreen)) {
                    return;
                }
                minecraft.getWindow().setWindowed(320, 180);
                minecraft.resizeDisplay();
                verifyPackagedResources();
                final Screen routed = SmartDropsConfigScreens.create(initialMenu);
                if (!(routed instanceof SmartDropsConfigScreen root)) {
                    throw new AssertionError("Initial-menu config route did not open local defaults");
                }
                this.rootScreen = root;
                this.session = root.editorSession();
                minecraft.setScreen(root);
                // Forge 47/GLFW clamps the physical Linux test window to 854x480.
                // Resize the real screen itself so the layout is still exercised at
                // the same compact 320x180 logical dimensions on every platform.
                root.resize(minecraft, 320, 180);
                return;
            }
            switch (this.phase) {
                case 0 -> this.verifyRootAndOpenEntityDrops(minecraft);
                case 1 -> this.enableEntityDrops(minecraft);
                case 2 -> this.openEntityCategories(minecraft);
                case 3 -> this.verifyCategoriesAndOpenPassive(minecraft);
                case 4 -> this.editPassiveCategory(minecraft);
                case 5 -> this.returnFromDirtyCategory(minecraft);
                case 6 -> this.returnFromEntityDrops(minecraft);
                case 7 -> this.applyFromRoot(minecraft);
                case 8 -> this.verifyAppliedAndFinish(minecraft);
                default -> throw new AssertionError("Unexpected category smoke phase " + this.phase);
            }
        } catch (Throwable failure) {
            SmartResourceDrops.LOGGER.error("NeoForge client category smoke test failed", failure);
            throw failure instanceof Error error
                    ? error
                    : new AssertionError("NeoForge client category smoke test failed", failure);
        }
    }

    private void verifyRootAndOpenEntityDrops(final Minecraft minecraft) {
        if (minecraft.screen != this.rootScreen) {
            return;
        }
        if (this.rootScreen.width != 320 || this.rootScreen.height != 180) {
            throw new AssertionError(
                    "Compact physical GUI did not initialize at 320x180: "
                            + this.rootScreen.width + "x" + this.rootScreen.height);
        }
        final List<String> labels = widgets(this.rootScreen).stream()
                .map(widget -> widget.getMessage().getString())
                .toList();
        if (labels.stream().noneMatch(label -> label.startsWith("Multiply Block XP:"))
                || !labels.contains("Block XP Multiplier")
                || labels.stream().anyMatch(label -> label.contains("Multiply Experience"))) {
            throw new AssertionError("General screen did not label block-only XP unambiguously");
        }
        assertSelected("minecraft:enderman", EntityCategory.NEUTRAL);
        assertSelected("minecraft:iron_golem", EntityCategory.GOLEMS);
        assertSelected("minecraft:cow", EntityCategory.PASSIVE);
        assertSelected("minecraft:zombie", EntityCategory.HOSTILE);
        if (this.session.entityInfo("minecraft:copper_golem").isPresent()) {
            throw new AssertionError("1.20.1 catalog unexpectedly exposed copper_golem");
        }
        press(this.rootScreen, "Entity Drops");
        this.phase = 1;
    }

    private void enableEntityDrops(final Minecraft minecraft) {
        if (!(minecraft.screen instanceof EntityDropsScreen current)) {
            return;
        }
        this.entityDropsScreen = current;
        if (!this.session.entityDropsEnabled()) {
            row(current, "Entity Drops").action().run();
        }
        this.phase = 2;
    }

    private void openEntityCategories(final Minecraft minecraft) {
        if (!(minecraft.screen instanceof EntityDropsScreen current)) {
            return;
        }
        if (!this.session.entityDropsEnabled()) {
            throw new AssertionError("Physical Entity Drops toggle did not enable entity editing");
        }
        this.entityDropsScreen = current;
        row(current, "Entity Categories").action().run();
        this.phase = 3;
    }

    private void verifyCategoriesAndOpenPassive(final Minecraft minecraft) {
        if (!(minecraft.screen instanceof EntityCategoryScreen current)) {
            return;
        }
        this.categoryScreen = current;
        final StructuredConfigList list = onlyList(current);
        if (list.rowCount() != EntityCategory.values().length) {
            throw new AssertionError(
                    "Expected " + EntityCategory.values().length
                            + " entity-category rows, found " + list.rowCount());
        }
        if (!hasButton(current, "Back")) {
            throw new AssertionError("Entity Categories screen has no Back button");
        }
        if (this.session.entityCategoryMultiplier(EntityCategory.PASSIVE) != null) {
            throw new AssertionError("Fresh defaults unexpectedly configured Passive");
        }
        row(current, ConfigUiText.entityCategoryName(EntityCategory.PASSIVE).getString())
                .action().run();
        this.phase = 4;
    }

    private void editPassiveCategory(final Minecraft minecraft) {
        if (!(minecraft.screen instanceof EntityRuleEditScreen)) {
            return;
        }
        press(minecraft.screen, "+");
        if (!Integer.valueOf(0).equals(
                this.session.entityCategoryMultiplier(EntityCategory.PASSIVE))) {
            throw new AssertionError("Passive category editor did not stage an explicit 0x value");
        }
        press(minecraft.screen, "Back");
        this.phase = 5;
    }

    private void returnFromDirtyCategory(final Minecraft minecraft) {
        if (minecraft.screen != this.categoryScreen) {
            return;
        }
        if (!this.categoryScreen.unsavedChangesIndicatorVisible()) {
            throw new AssertionError("Entity category child did not show unsaved changes");
        }
        press(this.categoryScreen, "Back");
        this.phase = 6;
    }

    private void returnFromEntityDrops(final Minecraft minecraft) {
        if (!(minecraft.screen instanceof EntityDropsScreen)) {
            return;
        }
        press(minecraft.screen, "Back");
        this.phase = 7;
    }

    private void applyFromRoot(final Minecraft minecraft) {
        if (minecraft.screen != this.rootScreen) {
            return;
        }
        if (!this.session.isDirty() || !this.rootScreen.applyButton().active) {
            throw new AssertionError("Nested Entity Categories edit did not dirty the root editor");
        }
        press(this.rootScreen, "Apply Changes");
        this.phase = 8;
    }

    private void verifyAppliedAndFinish(final Minecraft minecraft) throws Exception {
        if (!(minecraft.screen instanceof SmartDropsConfigScreen current)) {
            return;
        }
        if (current.editorSession().isDirty() || current.applyButton().active
                || !Integer.valueOf(0).equals(current.editorSession()
                        .entityCategoryMultiplier(EntityCategory.PASSIVE))) {
            throw new AssertionError("Root Apply did not acknowledge the Passive category edit");
        }
        final String configuredDirectory = System.getProperty(
                "smart_resource_drops.clientCategoryTestDirectory");
        if (configuredDirectory == null || configuredDirectory.isBlank()) {
            throw new AssertionError("Missing NeoForge category smoke directory property");
        }
        Files.writeString(
                Path.of(configuredDirectory).resolve("client-category.success"),
                "pass\n",
                StandardCharsets.UTF_8);
        SmartResourceDrops.LOGGER.info(
                "NeoForge client category smoke test passed at 320x180: Root -> Entity Drops -> Entity Categories, nine target-native rows, Passive edit, Back/dirty propagation, and root Apply");
        minecraft.stop();
    }

    private static StructuredConfigList onlyList(final Screen screen) {
        return screen.children().stream()
                .filter(StructuredConfigList.class::isInstance)
                .map(StructuredConfigList.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        screen.getClass().getSimpleName() + " omitted its structured list"));
    }

    private static StructuredConfigList.Row row(final Screen screen, final String label) {
        return onlyList(screen).rows().stream()
                .filter(candidate -> label.equals(candidate.primary().getString()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        screen.getClass().getSimpleName() + " omitted row " + label));
    }

    private static void press(final Screen screen, final String label) {
        screen.children().stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .filter(button -> label.equals(button.getMessage().getString()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        screen.getClass().getSimpleName() + " omitted button " + label))
                .onPress();
    }

    private static boolean hasButton(final Screen screen, final String label) {
        return screen.children().stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .anyMatch(button -> label.equals(button.getMessage().getString()));
    }

    private static List<AbstractWidget> widgets(final Screen screen) {
        return screen.children().stream()
                .filter(AbstractWidget.class::isInstance)
                .map(AbstractWidget.class::cast)
                .toList();
    }

    private static void verifyPackagedResources() throws Exception {
        if (!ClientCategoryTagIndex.load().getOrDefault(Category.ORES, Set.of())
                .contains("minecraft:diamond_ore")) {
            throw new AssertionError("Title-screen block tag fallback did not resolve diamond ore");
        }
        if (!ClientShearingTagIndex.load().standardResources().contains("minecraft:sheep")) {
            throw new AssertionError("Title-screen shearing tag fallback did not resolve sheep");
        }
        final Map<EntityCategory, Set<String>> resolved = ClientEntityCategoryTagIndex.load();
        if (resolved.size() != EntityCategory.values().length) {
            throw new AssertionError("Not all entity-category tags were resolved");
        }
        for (EntityCategory category : EntityCategory.values()) {
            final String path = "data/smart_resource_drops/tags/entity_types/categories/"
                    + category.key() + ".json";
            final List<ClientModResources.Resource> resources = ClientModResources.findAll(path);
            if (resources.isEmpty()) {
                throw new AssertionError("No installed resource found for " + path);
            }
            for (ClientModResources.Resource resource : resources) {
                try (InputStream ignored = resource.open()) {
                    // Opening every discovered resource catches broken dev-folder and JAR locators.
                }
            }
        }
        if (!resolved.getOrDefault(EntityCategory.NEUTRAL, Set.of())
                .contains("minecraft:enderman")) {
            throw new AssertionError("Enderman is missing from the Neutral category tag");
        }
        if (!resolved.getOrDefault(EntityCategory.GOLEMS, Set.of())
                .contains("minecraft:iron_golem")) {
            throw new AssertionError("Iron Golem is missing from the Golems category tag");
        }
    }

    private void assertSelected(
            final String entityId,
            final EntityCategory expected
    ) {
        final ConfigEditorSession.EntityInfo info = this.session
                .entityInfo(entityId)
                .orElseThrow(() -> new AssertionError("Missing entity catalog entry " + entityId));
        if (info.selectedCategory() != expected || info.categoryEstimated()) {
            throw new AssertionError(
                    entityId + " resolved as " + info.selectedCategory()
                            + " (estimated=" + info.categoryEstimated() + "), expected " + expected);
        }
    }
}
