package com.chedidandrew.smartresourcedrops.client;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.core.entity.EntityCategory;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Test-run-only check for NeoForge resource discovery and the formerly blank category screen. */
@Mod(value = SmartResourceDrops.MOD_ID, dist = Dist.CLIENT)
public final class NeoForgeClientCategorySmokeTest {
    private static final int TIMEOUT_TICKS = 1_200;
    private int ticks;
    private EntityCategoryScreen categoryScreen;
    private ConfigEditorSession session;

    public NeoForgeClientCategorySmokeTest() {
        NeoForge.EVENT_BUS.addListener(
                ClientTickEvent.Post.class,
                this::onClientTick);
    }

    private void onClientTick(final ClientTickEvent.Post event) {
        final Minecraft minecraft = Minecraft.getInstance();
        try {
            if (++this.ticks > TIMEOUT_TICKS) {
                throw new AssertionError("Timed out waiting for the NeoForge category smoke test");
            }
            if (minecraft.gui.overlay() != null) {
                return;
            }
            if (this.categoryScreen == null) {
                if (!(minecraft.gui.screen() instanceof TitleScreen titleScreen)) {
                    return;
                }
                verifyPackagedResources();
                final Screen routed = SmartDropsConfigScreens.create(titleScreen);
                if (!(routed instanceof SmartDropsConfigScreen root)) {
                    throw new AssertionError("Title-screen config route did not open local defaults");
                }
                this.session = root.editorSession();
                this.categoryScreen = new EntityCategoryScreen(root, root, this.session);
                minecraft.gui.setScreen(this.categoryScreen);
                return;
            }
            if (minecraft.gui.screen() != this.categoryScreen) {
                return;
            }

            final StructuredConfigList list = this.categoryScreen.children().stream()
                    .filter(StructuredConfigList.class::isInstance)
                    .map(StructuredConfigList.class::cast)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Entity Categories list was not initialized"));
            if (list.rowCount() != EntityCategory.values().length) {
                throw new AssertionError(
                        "Expected " + EntityCategory.values().length
                                + " entity-category rows, found " + list.rowCount());
            }
            final String expectedBack = Component
                    .translatable("smart_resource_drops.gui.back")
                    .getString();
            final boolean hasBack = this.categoryScreen.children().stream()
                    .filter(Button.class::isInstance)
                    .map(Button.class::cast)
                    .map(Button::getMessage)
                    .map(Component::getString)
                    .anyMatch(expectedBack::equals);
            if (!hasBack) {
                throw new AssertionError("Entity Categories screen has no Back button");
            }

            assertSelected("minecraft:enderman", EntityCategory.NEUTRAL);
            assertSelected("minecraft:iron_golem", EntityCategory.GOLEMS);
            SmartResourceDrops.LOGGER.info(
                    "NeoForge client category smoke test passed: {} rows, tagged entity classifications, and Back button",
                    list.rowCount());
            minecraft.stop();
        } catch (Throwable failure) {
            SmartResourceDrops.LOGGER.error("NeoForge client category smoke test failed", failure);
            throw failure instanceof Error error
                    ? error
                    : new AssertionError("NeoForge client category smoke test failed", failure);
        }
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
