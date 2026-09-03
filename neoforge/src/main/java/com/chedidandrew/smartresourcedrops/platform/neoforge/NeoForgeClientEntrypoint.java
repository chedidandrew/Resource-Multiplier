package com.chedidandrew.smartresourcedrops.platform.neoforge;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.client.ClientConfigState;
import com.chedidandrew.smartresourcedrops.client.ClientConfigTransferState;
import com.chedidandrew.smartresourcedrops.client.ClientModResources;
import com.chedidandrew.smartresourcedrops.client.ClientNetworkBridge;
import com.chedidandrew.smartresourcedrops.client.SmartDropsConfigScreens;
import com.chedidandrew.smartresourcedrops.config.ConfigScreenOpenPolicy;
import com.chedidandrew.smartresourcedrops.core.client.util.ClientCommandQueue;
import com.chedidandrew.smartresourcedrops.network.ConfigInvalidationPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigMutationResultPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigSnapshotFragmentPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.Commands;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;

/** Physical-client-only registration invoked through DistExecutor. */
final class NeoForgeClientEntrypoint {
    private static final String OPEN_CONFIG_QUEUE_KEY = "smart_resource_drops:open_config_gui";

    private NeoForgeClientEntrypoint() {
    }

    static void register(final IEventBus modBus) {
        ClientModResources.install(NeoForgeClientEntrypoint::findResources);
        ClientNetworkBridge.install(new ClientNetworkBridge.Transport() {
            @Override
            public boolean canSend(final net.minecraft.resources.ResourceLocation type) {
                final var listener = Minecraft.getInstance().getConnection();
                return listener != null
                        && NeoForgeNetworking.CHANNEL.isRemotePresent(listener.getConnection());
            }

            @Override
            public void send(final ConfigPayload payload) {
                NeoForgeNetworking.CHANNEL.sendToServer(payload);
            }
        });

        MinecraftForge.EVENT_BUS.addListener(
                (RegisterClientCommandsEvent event) -> registerClientCommands(event));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) {
                ClientCommandQueue.tick(Minecraft.getInstance());
                ClientConfigTransferState.tick(Minecraft.getInstance());
            }
        });
        MinecraftForge.EVENT_BUS.addListener(
                (ClientPlayerNetworkEvent.LoggingOut event) -> onLoggingOut(event));
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parent) -> SmartDropsConfigScreens.create(parent)));
    }

    static void accept(final ConfigSnapshotFragmentPayload payload) {
        ClientConfigTransferState.accept(payload, Minecraft.getInstance());
    }

    static void accept(final ConfigInvalidationPayload payload) {
        ClientConfigState.acceptInvalidation(payload, Minecraft.getInstance());
    }

    static void accept(final ConfigMutationResultPayload payload) {
        ClientConfigState.acceptMutationResult(payload, Minecraft.getInstance());
    }

    private static void registerClientCommands(final RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("smartdropsgui").executes(context -> {
            final Minecraft minecraft = Minecraft.getInstance();
            final Object connection = minecraft.getConnection();
            final Screen originatingScreen = minecraft.screen;
            return ClientCommandQueue.runCoalesced(OPEN_CONFIG_QUEUE_KEY, () -> {
                final Screen currentScreen = minecraft.screen;
                if (!ConfigScreenOpenPolicy.canOpenDelayedCommand(
                        connection,
                        minecraft.getConnection(),
                        originatingScreen,
                        currentScreen)) {
                    return;
                }
                minecraft.setScreen(SmartDropsConfigScreens.create(currentScreen));
            }) ? 1 : 0;
        }));
    }

    private static void onLoggingOut(final ClientPlayerNetworkEvent.LoggingOut event) {
        final Minecraft minecraft = Minecraft.getInstance();
        final Object connection = event.getConnection() == null
                ? ClientConfigState.connectionIdentity(minecraft)
                : event.getConnection();
        ClientCommandQueue.clear();
        ClientConfigState.onDisconnect(minecraft, connection);
    }

    private static List<ClientModResources.Resource> findResources(final String relativePath) {
        final ArrayList<ClientModResources.Resource> resources = new ArrayList<>();
        ModList.get().forEachModFile(file -> {
            final var path = file.findResource(relativePath);
            if (!Files.isRegularFile(path)) {
                return;
            }
            final String source = file.getFileName() + "!/" + relativePath;
            resources.add(new ClientModResources.Resource(source, () -> {
                final var resourcePath = file.findResource(relativePath);
                if (!Files.isRegularFile(resourcePath)) {
                    throw new FileNotFoundException(source);
                }
                return Files.newInputStream(resourcePath);
            }));
        });
        return resources;
    }
}
