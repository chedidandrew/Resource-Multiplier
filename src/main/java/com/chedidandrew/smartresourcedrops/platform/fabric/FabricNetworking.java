package com.chedidandrew.smartresourcedrops.platform.fabric;

import com.chedidandrew.smartresourcedrops.network.ConfigInvalidationPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigMutationResultPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigPatchPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigRequestPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigResetPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigSnapshotPayload;
import com.chedidandrew.smartresourcedrops.network.SmartDropsNetworking;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** Fabric payload registration and lifecycle adapter for the shared config protocol. */
final class FabricNetworking {
    private FabricNetworking() {
    }

    static void register() {
        PayloadTypeRegistry.playC2S().register(ConfigRequestPayload.TYPE, ConfigRequestPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ConfigPatchPayload.TYPE, ConfigPatchPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ConfigResetPayload.TYPE, ConfigResetPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ConfigSnapshotPayload.TYPE, ConfigSnapshotPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(
                ConfigInvalidationPayload.TYPE,
                ConfigInvalidationPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(
                ConfigMutationResultPayload.TYPE,
                ConfigMutationResultPayload.CODEC);

        SmartDropsNetworking.initialize(new SmartDropsNetworking.Transport() {
            @Override
            public boolean canSend(
                    final ServerPlayer player,
                    final CustomPacketPayload.Type<?> type
            ) {
                return ServerPlayNetworking.canSend(player, type);
            }

            @Override
            public void send(final ServerPlayer player, final CustomPacketPayload payload) {
                ServerPlayNetworking.send(player, payload);
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(ConfigRequestPayload.TYPE, (payload, context) ->
                SmartDropsNetworking.handleRequest(payload, context.player()));
        ServerPlayNetworking.registerGlobalReceiver(ConfigPatchPayload.TYPE, (payload, context) ->
                SmartDropsNetworking.handlePatch(payload, context.player()));
        ServerPlayNetworking.registerGlobalReceiver(ConfigResetPayload.TYPE, (payload, context) ->
                SmartDropsNetworking.handleReset(payload, context.player()));
        ServerLifecycleEvents.SERVER_STARTED.register(SmartDropsNetworking::serverStarted);
        ServerLifecycleEvents.SERVER_STOPPED.register(SmartDropsNetworking::serverStopped);
        ServerTickEvents.END_SERVER_TICK.register(server -> SmartDropsNetworking.serverTick());
    }
}
