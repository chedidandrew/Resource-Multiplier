package com.chedidandrew.smartresourcedrops.legacy;

import java.io.File;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

@Mod(
        modid = SmartResourceMultiplier.MOD_ID,
        name = SmartResourceMultiplier.MOD_NAME,
        version = SmartResourceMultiplier.VERSION,
        acceptableRemoteVersions = "*",
        dependencies = "required-after:Forge@[10.13.4.1614,)",
        guiFactory = SmartResourceMultiplier.GUI_FACTORY
)
public final class SmartResourceMultiplier {
    public static final String MOD_ID = "smart_resource_drops";
    public static final String MOD_NAME = "Smart Resource Multiplier";
    public static final String VERSION = "1.3.2+mc1.7.10";
    public static final String GUI_FACTORY =
            "com.chedidandrew.smartresourcedrops.legacy.client.LegacyGuiFactory";

    @SidedProxy(
            clientSide = "com.chedidandrew.smartresourcedrops.legacy.client.ClientProxy",
            serverSide = "com.chedidandrew.smartresourcedrops.legacy.CommonProxy"
    )
    public static CommonProxy proxy;

    private static volatile LegacyConfig config = LegacyConfig.defaults();
    private static File configFile;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        configFile = new File(event.getModConfigurationDirectory(), "smart_resource_drops.json");
        config = LegacyConfig.load(configFile);
        LegacyNetwork.initServerPackets();
        proxy.preInit();
        LegacyEventHandler handler = new LegacyEventHandler();
        MinecraftForge.EVENT_BUS.register(handler);
        FMLCommonHandler.instance().bus().register(handler);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new SmartDropsCommand());
    }

    public static LegacyConfig config() {
        return config;
    }

    public static synchronized boolean replaceConfig(LegacyConfig replacement) {
        if (replacement == null) return false;
        replacement.sanitize();
        if (!replacement.save(configFile)) return false;
        config = replacement;
        return true;
    }

    public static synchronized void reloadConfig() {
        config = LegacyConfig.load(configFile);
    }
}
