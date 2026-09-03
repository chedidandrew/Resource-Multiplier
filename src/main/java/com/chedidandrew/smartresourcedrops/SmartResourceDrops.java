package com.chedidandrew.smartresourcedrops;

import com.chedidandrew.smartresourcedrops.config.ConfigManager;
import com.chedidandrew.smartresourcedrops.platform.PlatformPlayerSupport;
import com.chedidandrew.smartresourcedrops.provenance.PlacementTracker;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SmartResourceDrops {
    public static final String MOD_ID = "smart_resource_drops";
    public static final String MOD_NAME = "Smart Resource Multiplier";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    private SmartResourceDrops() {
    }

    public static void initializeCommon() {
        PlatformPlayerSupport.bootstrap();
        PlacementTracker.bootstrap();
        ConfigManager.load();
        LOGGER.info("{} initialized", MOD_NAME);
    }
}
