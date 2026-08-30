package com.chedidandrew.smartresourcedrops;

import com.chedidandrew.smartresourcedrops.command.SmartDropsCommands;
import com.chedidandrew.smartresourcedrops.config.ConfigManager;
import com.chedidandrew.smartresourcedrops.network.SmartDropsNetworking;
import com.chedidandrew.smartresourcedrops.provenance.PlacementTracker;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SmartResourceDrops implements ModInitializer {
    public static final String MOD_ID = "smart_resource_drops";
    public static final String MOD_NAME = "Resource Multiplier";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ConfigManager.load();
        PlacementTracker.bootstrap();
        SmartDropsNetworking.registerCommon();
        SmartDropsCommands.register();

        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (level instanceof ServerLevel serverLevel) {
                PlacementTracker.remove(serverLevel, pos);
            }
        });

        LOGGER.info("{} initialized", MOD_NAME);
    }
}
