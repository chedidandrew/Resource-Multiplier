package com.chedidandrew.smartresourcedrops.client;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.core.entity.EntityCategory;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Test-run-only check for NeoForge resource discovery and the formerly blank category screen. */
@Mod(value = SmartResourceDrops.MOD_ID, dist = Dist.CLIENT)
public final class NeoForgeClientCategorySmokeTest {
    private static final int TIMEOUT_TICKS = 2_400;
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();
    private int ticks;
    private int phase;
    private SmartDropsConfigScreen root;
    private EntityCategoryScreen categoryScreen;
    private ConfigEditorSession session;

    public NeoForgeClientCategorySmokeTest() {
        if (Boolean.getBoolean("smart_resource_drops.clientCategoryTest")
                && REGISTERED.compareAndSet(false, true)) {
            NeoForge.EVENT_BUS.addListener(
                    ClientTickEvent.Post.class,
                    this::onClientTick);
        }
    }

    private void onClientTick(final ClientTickEvent.Post event) {
        final Minecraft minecraft = Minecraft.getInstance();
        try {
            if (minecraft.getOverlay() != null) {
                return;
            }
            if (++this.ticks > TIMEOUT_TICKS) {
                throw new AssertionError("Timed out waiting for the NeoForge category smoke test");
            }
            switch (this.phase) {
                case 0 -> openRoot(minecraft);
                case 1 -> openEntityDrops(minecraft);
                case 2 -> openCategories(minecraft);
                case 3 -> verifyRowsAndOpenEditor(minecraft);
                case 4 -> verifyEditAndDirtyPropagation(minecraft);
                default -> throw new AssertionError("Unexpected category smoke phase " + this.phase);
            }
        } catch (Throwable failure) {
            SmartResourceDrops.LOGGER.error("NeoForge client category smoke test failed", failure);
            throw failure instanceof Error error
                    ? error
                    : new AssertionError("NeoForge client category smoke test failed", failure);
        }
    }

    private void openRoot(final Minecraft minecraft) throws Exception {
        final Screen initialMenu = minecraft.screen;
        if (!(initialMenu instanceof TitleScreen)
                && !(initialMenu instanceof AccessibilityOnboardingScreen)) {
            return;
        }
        verifyPackagedResources();
        final Screen routed = SmartDropsConfigScreens.create(initialMenu);
        if (!(routed instanceof SmartDropsConfigScreen openedRoot)) {
            throw new AssertionError("Initial-menu config route did not open local defaults");
        }
        this.root = openedRoot;
        this.session = openedRoot.editorSession();
        minecraft.setScreen(openedRoot);
        assertMultiplierValuesCentered(openedRoot);
        assertStructuredTooltipComposition();
        this.phase = 1;
    }

    private void openEntityDrops(final Minecraft minecraft) {
        if (minecraft.screen != this.root) {
            return;
        }
        press(buttonWithLabel(this.root, "Entity Drops"));
        this.phase = 2;
    }

    private void openCategories(final Minecraft minecraft) {
        if (!(minecraft.screen instanceof EntityDropsScreen entityDrops)) {
            return;
        }
        final StructuredConfigList list = onlyList(entityDrops);
        if (!this.session.entityDropsEnabled()) {
            rowWithPrimary(list, "Entity Drops").action().run();
            if (!this.session.entityDropsEnabled()) {
                throw new AssertionError("Entity Drops could not be enabled for physical category editing");
            }
        }
        rowWithPrimary(onlyList(entityDrops), "Entity Categories").action().run();
        this.phase = 3;
    }

    private void verifyRowsAndOpenEditor(final Minecraft minecraft) {
        if (!(minecraft.screen instanceof EntityCategoryScreen openedCategories)) {
            return;
        }
        this.categoryScreen = openedCategories;
        final StructuredConfigList list = onlyList(openedCategories);
        if (list.rowCount() != EntityCategory.values().length) {
            throw new AssertionError(
                    "Expected " + EntityCategory.values().length
                            + " entity-category rows, found " + list.rowCount());
        }
        buttonWithLabel(openedCategories, Component
                .translatable("smart_resource_drops.gui.back")
                .getString());
        assertSelected("minecraft:enderman", EntityCategory.NEUTRAL);
        assertSelected("minecraft:iron_golem", EntityCategory.GOLEMS);
        assertSelected("minecraft:cow", EntityCategory.PASSIVE);
        rowWithPrimary(
                list,
                ConfigUiText.entityCategoryName(EntityCategory.PASSIVE).getString())
                .action()
                .run();
        this.phase = 4;
    }

    private void verifyEditAndDirtyPropagation(final Minecraft minecraft) throws Exception {
        if (!(minecraft.screen instanceof EntityRuleEditScreen editor)) {
            return;
        }
        final Button plus = buttonWithLabel(editor, "+");
        if (!plus.active) {
            throw new AssertionError("Entity Categories editor was not physically editable");
        }
        press(plus);
        if (!Integer.valueOf(0).equals(
                this.session.entityCategoryMultiplier(EntityCategory.PASSIVE))) {
            throw new AssertionError("Passive category editor did not stage explicit 0x");
        }
        press(buttonWithLabel(editor, "Back"));
        if (minecraft.screen != this.categoryScreen
                || !this.categoryScreen.unsavedChangesIndicatorVisible()) {
            throw new AssertionError("Entity Categories edit did not propagate dirty state to its list");
        }
        press(buttonWithLabel(this.categoryScreen, "Back"));
        if (!(minecraft.screen instanceof EntityDropsScreen entityDrops)
                || !entityDrops.unsavedChangesIndicatorVisible()) {
            throw new AssertionError("Entity Categories dirty state did not propagate to Entity Drops");
        }
        press(buttonWithLabel(entityDrops, "Back"));
        if (minecraft.screen != this.root || !this.session.isDirty() || !this.root.applyButton().active) {
            throw new AssertionError("Entity Categories dirty state did not reach root Apply");
        }
        SmartResourceDrops.LOGGER.info(
                "NeoForge client category smoke test passed: {} rows, tagged classifications, production navigation, physical edit, Back, dirty propagation, and root Apply",
                EntityCategory.values().length);
        final String testDirectory = System.getProperty(
                "smart_resource_drops.clientCategoryTestDirectory");
        if (testDirectory == null || testDirectory.isBlank()) {
            throw new AssertionError("Missing NeoForge category test directory property");
        }
        Files.writeString(
                Path.of(testDirectory).resolve("client-category.success"),
                "pass\n",
                StandardCharsets.UTF_8);
        minecraft.stop();
    }

    private static void verifyPackagedResources() throws Exception {
        final Map<EntityCategory, Set<String>> resolved = ClientEntityCategoryTagIndex.load();
        if (resolved.size() != EntityCategory.values().length) {
            throw new AssertionError("Not all entity-category tags were resolved");
        }
        for (EntityCategory category : EntityCategory.values()) {
            final String path = "data/smart_resource_drops/tags/entity_type/categories/"
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
        if (!resolved.getOrDefault(EntityCategory.GOLEMS, Set.of())
                .contains("minecraft:copper_golem")) {
            throw new AssertionError("Copper Golem is missing from the Golems category tag");
        }
        if (resolved.values().stream().mapToInt(Set::size).sum() <= 20) {
            throw new AssertionError("Target-native entity catalog was empty or incomplete");
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

    private static StructuredConfigList onlyList(final Screen screen) {
        final List<StructuredConfigList> lists = screen.children().stream()
                .filter(StructuredConfigList.class::isInstance)
                .map(StructuredConfigList.class::cast)
                .toList();
        if (lists.size() != 1) {
            throw new AssertionError(
                    "Expected one structured list on " + screen.getClass().getSimpleName());
        }
        return lists.getFirst();
    }

    private static StructuredConfigList.Row rowWithPrimary(
            final StructuredConfigList list,
            final String primary
    ) {
        return list.rows().stream()
                .filter(row -> primary.equals(row.primary().getString()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing structured row " + primary));
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

    private static void assertMultiplierValuesCentered(final Screen screen) {
        final List<Button> buttons = screen.children().stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .toList();
        final List<StringWidget> values = screen.children().stream()
                .filter(AbstractWidget.class::isInstance)
                .map(AbstractWidget.class::cast)
                .filter(StringWidget.class::isInstance)
                .map(StringWidget.class::cast)
                .filter(widget -> widget.getMessage().getString().matches("(?:Inherit|\\d+x)"))
                .toList();
        if (values.size() != 2) {
            throw new AssertionError(
                    "Root screen did not expose both multiplier value widgets: " + values.size());
        }
        for (StringWidget value : values) {
            final Button decrement = buttons.stream()
                    .filter(button -> button.getY() == value.getY())
                    .filter(button -> "-".equals(button.getMessage().getString()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Multiplier value omitted its decrement button"));
            final Button increment = buttons.stream()
                    .filter(button -> button.getY() == value.getY())
                    .filter(button -> "+".equals(button.getMessage().getString()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Multiplier value omitted its increment button"));
            final int expectedX = (decrement.getX() + decrement.getWidth() + increment.getX()
                    - Minecraft.getInstance().font.width(value.getMessage())) / 2;
            if (value.getX() != expectedX) {
                throw new AssertionError(
                        "Multiplier value was not centered between its buttons: "
                                + value.getX() + " != " + expectedX);
            }
        }
    }

    private static void assertStructuredTooltipComposition() {
        final Component title = Component.literal("Entity Overrides");
        final Component description = Component.literal("Search registered living entity types");
        final Component action = Component.literal("View Details >");
        final StructuredConfigList.Row row = new StructuredConfigList.Row(
                title,
                description,
                Component.empty(),
                action,
                Component.empty()
                        .append(title)
                        .append("\n")
                        .append(description)
                        .append("\nAuthoritative category details"),
                () -> { });
        final String hover = StructuredConfigList.composeHoverText(row, true).getString();
        assertOccurrenceCount(hover, title.getString(), 1);
        assertOccurrenceCount(hover, description.getString(), 1);
        assertOccurrenceCount(hover, action.getString(), 1);
        assertOccurrenceCount(hover, "Authoritative category details", 1);
        final String compactHover = StructuredConfigList.composeHoverText(row, false).getString();
        if (!"Authoritative category details".equals(compactHover)) {
            throw new AssertionError(
                    "Untruncated tooltip did not remove visible duplicate lines: " + compactHover);
        }
    }

    private static void assertOccurrenceCount(
            final String text,
            final String needle,
            final int expected
    ) {
        int count = 0;
        int offset = 0;
        while ((offset = text.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        if (count != expected) {
            throw new AssertionError(
                    "Structured tooltip occurrence mismatch for " + needle + ": " + text);
        }
    }

    private static void press(final Button button) {
        button.onPress(new KeyEvent(257, 0, 0));
    }
}
