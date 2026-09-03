package com.chedidandrew.smartresourcedrops.platform.neoforge;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.command.SmartDropsCommands;
import com.chedidandrew.smartresourcedrops.config.ConfigManager;
import com.chedidandrew.smartresourcedrops.network.SmartDropsNetworking;
import com.chedidandrew.smartresourcedrops.platform.PlatformPlayerSupport;
import com.chedidandrew.smartresourcedrops.provenance.PlacementTracker;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.NetworkConstants;

/** Physical-server-safe legacy NeoForge/Forge bootstrap for Minecraft 1.20.1. */
@Mod(SmartResourceDrops.MOD_ID)
public final class NeoForgeEntrypoint {
    public NeoForgeEntrypoint() {
        final IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerExtensionPoint(
                IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(
                        () -> NetworkConstants.IGNORESERVERONLY,
                        (remoteVersion, isServer) -> true));
        ConfigManager.configureConfigDirectory(FMLPaths.CONFIGDIR.get());
        PlatformPlayerSupport.installFakePlayerPredicate(player -> player instanceof FakePlayer);

        NeoForgePlacementStorage.register(modBus);
        PlacementTracker.installStorage(new NeoForgePlacementStorage());
        NeoForgeNetworking.register();

        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent event) ->
                SmartDropsCommands.register(event.getDispatcher()));
        MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent event) ->
                SmartDropsNetworking.serverStarted(event.getServer()));
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent event) ->
                SmartDropsNetworking.serverStopped(event.getServer()));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) {
                SmartDropsNetworking.serverTick();
            }
        });

        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> NeoForgeClientEntrypoint.register(modBus));
        SmartResourceDrops.initializeCommon();
    }
}
