package com.chedidandrew.smartresourcedrops.platform.neoforge;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.command.SmartDropsCommands;
import com.chedidandrew.smartresourcedrops.config.ConfigManager;
import com.chedidandrew.smartresourcedrops.network.SmartDropsNetworking;
import com.chedidandrew.smartresourcedrops.platform.PlatformPlayerSupport;
import com.chedidandrew.smartresourcedrops.provenance.PlacementTracker;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.DistExecutor;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** NeoForge 20.5 bootstrap, with physical-client setup gated explicitly. */
@Mod(SmartResourceDrops.MOD_ID)
public final class NeoForgeEntrypoint {
    public NeoForgeEntrypoint(final IEventBus modBus, final ModContainer container) {
        ConfigManager.configureConfigDirectory(FMLPaths.CONFIGDIR.get());
        PlatformPlayerSupport.installFakePlayerPredicate(player -> player instanceof FakePlayer);

        NeoForgePlacementStorage.register(modBus);
        PlacementTracker.installStorage(new NeoForgePlacementStorage());
        NeoForgeNetworking.register(modBus);

        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, event ->
                SmartDropsCommands.register(event.getDispatcher()));
        NeoForge.EVENT_BUS.addListener(ServerStartedEvent.class, event ->
                SmartDropsNetworking.serverStarted(event.getServer()));
        NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, event ->
                SmartDropsNetworking.serverStopped(event.getServer()));
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event ->
                SmartDropsNetworking.serverTick());

        SmartResourceDrops.initializeCommon();
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> new NeoForgeClientEntrypoint(modBus, container));
    }
}
