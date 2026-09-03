package com.chedidandrew.smartresourcedrops.packagedprobe;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Physical-client reflection probe that cannot compile against production source outputs. */
@Mod(value = "smart_resource_drops_packaged_probe", dist = Dist.CLIENT)
public final class PackagedClientProbe {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();
    private int ticks;
    private Screen productionScreen;
    private int navigationPhase;

    public PackagedClientProbe() {
        if (Boolean.getBoolean("smart_resource_drops.packagedClientProbe")
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
            if (++this.ticks > 2_400) {
                throw new AssertionError("Timed out waiting for packaged NeoForge client probe");
            }
            if (this.productionScreen == null) {
                if (!(minecraft.screen instanceof TitleScreen)
                        && !(minecraft.screen instanceof AccessibilityOnboardingScreen)) {
                    return;
                }
                PackagedProbeSupport.verifyProductionModAndCodeSource();
                PackagedProbeSupport.verifyProductionConfigExists();
                verifyCategoryResources();
                final Class<?> screens = Class.forName(
                        "com.chedidandrew.smartresourcedrops.client.SmartDropsConfigScreens");
                final Method create = screens.getMethod("create", Screen.class);
                this.productionScreen = (Screen) create.invoke(null, minecraft.screen);
                if (!this.productionScreen.getClass().getName().equals(
                        "com.chedidandrew.smartresourcedrops.client.SmartDropsConfigScreen")) {
                    throw new AssertionError(
                            "Packaged title route returned " + this.productionScreen.getClass().getName());
                }
                minecraft.setScreen(this.productionScreen);
                return;
            }
            if (this.navigationPhase == 0) {
                requireScreen(minecraft.screen, "SmartDropsConfigScreen");
                press(minecraft.screen, "Entity Drops");
                this.navigationPhase = 1;
                return;
            }
            if (this.navigationPhase == 1) {
                requireScreen(minecraft.screen, "EntityDropsScreen");
                runStructuredRow(minecraft.screen, "Entity Categories");
                this.navigationPhase = 2;
                return;
            }
            requireScreen(minecraft.screen, "EntityCategoryScreen");
            verifyCategoryScreenRows(minecraft.screen);
            PackagedProbeSupport.writeMarker("packaged-client.success");
            minecraft.stop();
        } catch (Throwable failure) {
            throw failure instanceof Error error
                    ? error
                    : new AssertionError("Packaged NeoForge client probe failed", failure);
        }
    }

    private static void verifyCategoryResources() throws Exception {
        final Class<?> index = Class.forName(
                "com.chedidandrew.smartresourcedrops.client.ClientEntityCategoryTagIndex");
        final Method load = index.getDeclaredMethod("load");
        load.setAccessible(true);
        final Object loaded = load.invoke(null);
        if (!(loaded instanceof Map<?, ?> categories) || categories.size() != 9) {
            throw new AssertionError("Packaged Entity Categories did not resolve all nine rows");
        }
        final Map<String, Set<String>> byCategory = new HashMap<>();
        final Set<String> allEntities = new HashSet<>();
        for (Map.Entry<?, ?> entry : categories.entrySet()) {
            if (!(entry.getValue() instanceof Set<?> rawValues)) {
                throw new AssertionError("Packaged Entity Categories returned a non-set row");
            }
            final Set<String> values = new HashSet<>();
            for (Object rawValue : rawValues) {
                if (!(rawValue instanceof String value)) {
                    throw new AssertionError("Packaged Entity Categories returned a non-string entity ID");
                }
                values.add(value);
                allEntities.add(value);
            }
            byCategory.put(String.valueOf(entry.getKey()), values);
        }
        requireMember(byCategory, "NEUTRAL", "minecraft:enderman");
        requireMember(byCategory, "GOLEMS", "minecraft:iron_golem");
        requireMember(byCategory, "PASSIVE", "minecraft:cow");
        requireMember(byCategory, "HOSTILE", "minecraft:zombie");
        if (allEntities.size() <= 20 || allEntities.contains("minecraft:copper_golem")) {
            throw new AssertionError(
                    "Packaged target-native entity catalog was empty, incomplete, or exposed copper_golem");
        }
    }

    private static void requireMember(
            final Map<String, Set<String>> categories,
            final String category,
            final String entityId
    ) {
        if (!categories.getOrDefault(category, Set.of()).contains(entityId)) {
            throw new AssertionError(
                    "Packaged Entity Categories omitted " + entityId + " from " + category);
        }
    }

    private static void requireScreen(final Screen screen, final String expectedSimpleName) {
        if (screen == null || !expectedSimpleName.equals(screen.getClass().getSimpleName())) {
            throw new AssertionError(
                    "Expected " + expectedSimpleName + ", found "
                            + (screen == null ? "null" : screen.getClass().getName()));
        }
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

    private static void runStructuredRow(final Screen screen, final String label) throws Exception {
        final Object list = screen.children().stream()
                .filter(child -> "StructuredConfigList".equals(child.getClass().getSimpleName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        screen.getClass().getSimpleName() + " omitted its structured list"));
        final Method rowsMethod = list.getClass().getDeclaredMethod("rows");
        rowsMethod.setAccessible(true);
        final Object rawRows = rowsMethod.invoke(list);
        if (!(rawRows instanceof Iterable<?> rows)) {
            throw new AssertionError("StructuredConfigList rows were not iterable");
        }
        for (Object row : rows) {
            final Method primaryMethod = row.getClass().getDeclaredMethod("primary");
            primaryMethod.setAccessible(true);
            final Object primary = primaryMethod.invoke(row);
            if (primary instanceof net.minecraft.network.chat.Component component
                    && label.equals(component.getString())) {
                final Method actionMethod = row.getClass().getDeclaredMethod("action");
                actionMethod.setAccessible(true);
                final Object action = actionMethod.invoke(row);
                if (!(action instanceof Runnable runnable)) {
                    throw new AssertionError("Structured row " + label + " had no runnable action");
                }
                runnable.run();
                return;
            }
        }
        throw new AssertionError(screen.getClass().getSimpleName() + " omitted row " + label);
    }

    private static void verifyCategoryScreenRows(final Screen screen) throws Exception {
        final Object list = screen.children().stream()
                .filter(child -> "StructuredConfigList".equals(child.getClass().getSimpleName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Packaged Entity Categories screen omitted its structured list"));
        final Method rowCount = list.getClass().getDeclaredMethod("rowCount");
        rowCount.setAccessible(true);
        final Object count = rowCount.invoke(list);
        if (!(count instanceof Integer rows) || rows != 9) {
            throw new AssertionError(
                    "Packaged Entity Categories screen displayed " + count + " rows instead of 9");
        }
    }
}
