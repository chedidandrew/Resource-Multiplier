package com.chedidandrew.smartresourcedrops.platform.fabric.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.client.ClientConfigState;
import com.chedidandrew.smartresourcedrops.client.ClientModResources;
import com.chedidandrew.smartresourcedrops.client.ClientNetworkBridge;
import com.chedidandrew.smartresourcedrops.client.SmartDropsConfigScreens;
import com.chedidandrew.smartresourcedrops.config.ConfigScreenOpenPolicy;
import com.chedidandrew.smartresourcedrops.core.client.util.ClientCommandQueue;
import com.chedidandrew.smartresourcedrops.network.ConfigInvalidationPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigMutationResultPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigSnapshotPayload;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Fabric client bootstrap for shared GUI, tag discovery, and config networking. */
public final class FabricClientEntrypoint implements ClientModInitializer {
    private static final String OPEN_CONFIG_QUEUE_KEY = "smart_resource_drops:open_config_gui";

    @Override
    public void onInitializeClient() {
        ClientModResources.install(FabricClientEntrypoint::findResources);
        ClientNetworkBridge.install(new ClientNetworkBridge.Transport() {
            @Override
            public boolean canSend(final CustomPacketPayload.Type<?> type) {
                return ClientPlayNetworking.canSend(type);
            }

            @Override
            public void send(final CustomPacketPayload payload) {
                ClientPlayNetworking.send(payload);
            }
        });

        ClientCommandQueue.initialize();
        ClientTickEvents.END_CLIENT_TICK.register(ClientCommandQueue::tick);
        ClientPlayNetworking.registerGlobalReceiver(ConfigSnapshotPayload.TYPE, (payload, context) -> {
            SmartResourceDrops.LOGGER.debug("Received config snapshot #{} on the client", payload.requestId());
            context.client().execute(() -> ClientConfigState.accept(payload, context.client()));
        });
        ClientPlayNetworking.registerGlobalReceiver(ConfigInvalidationPayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientConfigState.acceptInvalidation(payload, context.client())));
        ClientPlayNetworking.registerGlobalReceiver(ConfigMutationResultPayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientConfigState.acceptMutationResult(payload, context.client())));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientCommandQueue.clear();
            ClientConfigState.onDisconnect(client, handler.getConnection());
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) ->
                dispatcher.register(ClientCommands.literal("smartdropsgui").executes(context -> {
                    final Minecraft minecraft = Minecraft.getInstance();
                    final Object connection = minecraft.getConnection();
                    final Screen originatingScreen = minecraft.gui.screen();
                    return ClientCommandQueue.runCoalesced(OPEN_CONFIG_QUEUE_KEY, () -> {
                        final Screen currentScreen = minecraft.gui.screen();
                        if (!ConfigScreenOpenPolicy.canOpenDelayedCommand(
                                connection,
                                minecraft.getConnection(),
                                originatingScreen,
                                currentScreen)) {
                            return;
                        }
                        minecraft.gui.setScreen(SmartDropsConfigScreens.create(currentScreen));
                    }) ? 1 : 0;
                })));
    }

    private static List<ClientModResources.Resource> findResources(final String relativePath) {
        final ArrayList<ClientModResources.Resource> resources = new ArrayList<>();
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            mod.findPath(relativePath).ifPresent(path -> resources.add(resource(path)));
        }
        return resources;
    }

    private static ClientModResources.Resource resource(final Path path) {
        return new ClientModResources.Resource(path.toString(), () -> Files.newInputStream(path));
    }
}
