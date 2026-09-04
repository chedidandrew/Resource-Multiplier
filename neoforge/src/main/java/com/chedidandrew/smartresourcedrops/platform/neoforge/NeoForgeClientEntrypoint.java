package com.chedidandrew.smartresourcedrops.platform.neoforge;

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

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.Commands;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Physical-client-only NeoForge bootstrap. */
@Mod(value = SmartResourceDrops.MOD_ID, dist = Dist.CLIENT)
public final class NeoForgeClientEntrypoint {
    private static final String OPEN_CONFIG_QUEUE_KEY = "smart_resource_drops:open_config_gui";

    public NeoForgeClientEntrypoint(final IEventBus modBus, final ModContainer container) {
        ClientModResources.install(NeoForgeClientEntrypoint::findResources);
        ClientNetworkBridge.install(new ClientNetworkBridge.Transport() {
            @Override
            public boolean canSend(final CustomPacketPayload.Type<?> type) {
                final var listener = Minecraft.getInstance().getConnection();
                return listener != null && listener.hasChannel(type);
            }

            @Override
            public void send(final CustomPacketPayload payload) {
                ClientPacketDistributor.sendToServer(payload);
            }
        });
        NeoForgeNetworking.installClientReceiver(NeoForgeClientEntrypoint::handleClientPayload);
        NeoForge.EVENT_BUS.addListener(
                RegisterClientCommandsEvent.class,
                NeoForgeClientEntrypoint::registerClientCommands);
        NeoForge.EVENT_BUS.addListener(
                ClientTickEvent.Post.class,
                event -> ClientCommandQueue.tick(Minecraft.getInstance()));
        NeoForge.EVENT_BUS.addListener(
                ClientPlayerNetworkEvent.LoggingOut.class,
                NeoForgeClientEntrypoint::onLoggingOut);
        container.registerExtensionPoint(
                IConfigScreenFactory.class,
                (ignoredContainer, parent) -> SmartDropsConfigScreens.create(parent));
    }

    private static void handleClientPayload(final CustomPacketPayload payload) {
        if (payload instanceof ConfigSnapshotPayload snapshot) {
            SmartResourceDrops.LOGGER.debug("Received config snapshot #{} on the client", snapshot.requestId());
            ClientConfigState.accept(snapshot, Minecraft.getInstance());
        } else if (payload instanceof ConfigInvalidationPayload invalidation) {
            ClientConfigState.acceptInvalidation(invalidation, Minecraft.getInstance());
        } else if (payload instanceof ConfigMutationResultPayload result) {
            ClientConfigState.acceptMutationResult(result, Minecraft.getInstance());
        } else {
            SmartResourceDrops.LOGGER.warn("Ignored unexpected client payload type {}", payload.type().id());
        }
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
            final var contents = file.getContents();
            if (!contents.containsFile(relativePath)) {
                return;
            }
            final String source = file.getFileName() + "!/" + relativePath;
            resources.add(new ClientModResources.Resource(
                    source,
                    () -> contents.openFile(relativePath)));
        });
        return resources;
    }
}
