package com.chedidandrew.smartresourcedrops.platform.neoforge;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.command.SmartDropsCommands;
import com.chedidandrew.smartresourcedrops.config.ConfigManager;
import com.chedidandrew.smartresourcedrops.network.SmartDropsNetworking;
import com.chedidandrew.smartresourcedrops.platform.PlatformPlayerSupport;
import com.chedidandrew.smartresourcedrops.provenance.PlacementTracker;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Physical-server-safe NeoForge bootstrap. */
@Mod(SmartResourceDrops.MOD_ID)
public final class NeoForgeEntrypoint {
    public NeoForgeEntrypoint(final IEventBus modBus) {
        ConfigManager.configureConfigDirectory(FMLPaths.CONFIGDIR.get());
        PlatformPlayerSupport.installFakePlayerPredicate(Player::isFakePlayer);

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
    }
}
