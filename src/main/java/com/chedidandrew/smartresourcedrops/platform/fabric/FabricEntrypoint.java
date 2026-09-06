package com.chedidandrew.smartresourcedrops.platform.fabric;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.command.SmartDropsCommands;
import com.chedidandrew.smartresourcedrops.config.ConfigManager;
import com.chedidandrew.smartresourcedrops.platform.PlatformPlayerSupport;
import com.chedidandrew.smartresourcedrops.provenance.PlacementTracker;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;

/** Fabric loader bootstrap; gameplay and configuration policy remain loader-neutral. */
public final class FabricEntrypoint implements ModInitializer {
    @Override
    public void onInitialize() {
        ConfigManager.configureConfigDirectory(FabricLoader.getInstance().getConfigDir());
        // Fabric 1.19.2 predates Fabric API's common FakePlayer class. Keep the
        // same conservative automation policy for the established fake-player
        // implementations used by Carpet and other automation mods.
        PlatformPlayerSupport.installFakePlayerPredicate(FabricEntrypoint::isFakePlayer);
        PlacementTracker.installStorage(new FabricPlacementStorage());
        FabricNetworking.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                SmartDropsCommands.register(dispatcher));

        // Fabric exposes an actual post-break callback, so retain the defensive final cleanup.
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (level instanceof ServerLevel serverLevel) {
                PlacementTracker.remove(serverLevel, pos);
            }
        });
        SmartResourceDrops.initializeCommon();
    }

    private static boolean isFakePlayer(final Player player) {
        Class<?> type = player.getClass();
        while (type != null) {
            final String name = type.getName().toLowerCase(Locale.ROOT);
            if (name.contains("fakeplayer") || name.contains("playermpfake")) {
                return true;
            }
            type = type.getSuperclass();
        }
        return false;
    }
}
