package com.chedidandrew.smartresourcedrops.platform.fabric;

import com.chedidandrew.smartresourcedrops.network.ConfigInvalidationPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigMutationResultPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigPatchFragmentPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigRequestPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigResetPayload;
import com.chedidandrew.smartresourcedrops.network.SmartDropsNetworking;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Fabric payload registration and lifecycle adapter for the shared config protocol. */
final class FabricNetworking {
    private FabricNetworking() {
    }

    static void register() {
        SmartDropsNetworking.initialize(new SmartDropsNetworking.Transport() {
            @Override
            public boolean canSend(
                    final ServerPlayer player,
                    final ResourceLocation type
            ) {
                return ServerPlayNetworking.canSend(player, type);
            }

            @Override
            public void send(final ServerPlayer player, final ConfigPayload payload) {
                final FriendlyByteBuf buffer = PacketByteBufs.create();
                payload.write(buffer);
                ServerPlayNetworking.send(player, payload.id(), buffer);
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(ConfigRequestPayload.TYPE,
                (server, player, handler, buffer, responseSender) -> {
                    final ConfigRequestPayload payload = ConfigRequestPayload.read(buffer);
                    server.execute(() -> SmartDropsNetworking.handleRequest(payload, player));
                });
        ServerPlayNetworking.registerGlobalReceiver(ConfigPatchFragmentPayload.TYPE,
                (server, player, handler, buffer, responseSender) -> {
                    final ConfigPatchFragmentPayload payload = ConfigPatchFragmentPayload.read(buffer);
                    server.execute(() -> SmartDropsNetworking.handlePatchFragment(payload, player));
                });
        ServerPlayNetworking.registerGlobalReceiver(ConfigResetPayload.TYPE,
                (server, player, handler, buffer, responseSender) -> {
                    final ConfigResetPayload payload = ConfigResetPayload.read(buffer);
                    server.execute(() -> SmartDropsNetworking.handleReset(payload, player));
                });
        ServerLifecycleEvents.SERVER_STARTED.register(SmartDropsNetworking::serverStarted);
        ServerLifecycleEvents.SERVER_STOPPED.register(SmartDropsNetworking::serverStopped);
        ServerTickEvents.END_SERVER_TICK.register(server -> SmartDropsNetworking.serverTick());
    }
}
