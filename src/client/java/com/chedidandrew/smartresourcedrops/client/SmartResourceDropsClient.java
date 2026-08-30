package com.chedidandrew.smartresourcedrops.client;


import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.config.ConfigScreenOpenPolicy;
import com.chedidandrew.smartresourcedrops.core.client.util.ClientCommandQueue;
import com.chedidandrew.smartresourcedrops.network.ConfigInvalidationPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigMutationResultPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigSnapshotPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class SmartResourceDropsClient implements ClientModInitializer {
    private static final String OPEN_CONFIG_QUEUE_KEY = "smart_resource_drops:open_config_gui";

    @Override
    public void onInitializeClient() {
        ClientCommandQueue.initialize();
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
            ClientConfigState.onDisconnect(client, handler);
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
                    })
                            ? 1
                            : 0;
                })));
    }
}
