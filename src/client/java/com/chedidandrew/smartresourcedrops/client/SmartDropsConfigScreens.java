package com.chedidandrew.smartresourcedrops.client;

import java.util.Optional;

import com.chedidandrew.smartresourcedrops.config.ConfigManager;
import com.chedidandrew.smartresourcedrops.config.ConfigScreenOpenPolicy;
import com.chedidandrew.smartresourcedrops.config.SmartDropsConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Shared entry route for Mod Menu and the direct client command. */
public final class SmartDropsConfigScreens {
    private SmartDropsConfigScreens() {
    }

    public static Screen create(final Screen parent) {
        final Minecraft minecraft = Minecraft.getInstance();
        final Optional<ClientConfigState.CachedSnapshot> cached = ClientConfigState.cachedSnapshot(minecraft);
        final ConfigScreenOpenPolicy.Decision decision = ConfigScreenOpenPolicy.decide(
                minecraft.getConnection() != null,
                minecraft.hasSingleplayerServer(),
                cached.isPresent());

        if (decision.authority() == ConfigScreenOpenPolicy.Authority.LOCAL_DEFAULTS) {
            final ConfigManager.ClientSnapshot snapshot = ConfigManager.clientSnapshot();
            final SmartDropsConfig config = ConfigManager.tryParseSnapshotJson(snapshot.json())
                    .orElseGet(() -> ConfigManager.snapshotForClient());
            return SmartDropsConfigScreen.forLocalDefaults(
                    parent,
                    config,
                    "",
                    snapshot.revision());
        }
        if (decision.phase() == ConfigScreenOpenPolicy.InitialPhase.READY && cached.isPresent()) {
            final ClientConfigState.CachedSnapshot snapshot = cached.get();
            return new SmartDropsConfigScreen(
                    parent,
                    snapshot.config(),
                    snapshot.editable(),
                    "",
                    snapshot.revision());
        }
        return new SmartDropsConfigLoadingScreen(parent);
    }
}
