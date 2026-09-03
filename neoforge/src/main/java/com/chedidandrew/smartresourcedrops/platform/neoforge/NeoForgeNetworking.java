package com.chedidandrew.smartresourcedrops.platform.neoforge;

import com.chedidandrew.smartresourcedrops.network.ConfigInvalidationPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigMutationResultPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigPatchPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigRequestPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigResetPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigSnapshotPayload;
import com.chedidandrew.smartresourcedrops.network.SmartDropsNetworking;

import java.util.Objects;
import java.util.function.Consumer;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** NeoForge registration and transport for the shared config protocol. */
final class NeoForgeNetworking {
    private static final String PROTOCOL_VERSION = "1";
    private static volatile Consumer<CustomPacketPayload> clientReceiver = payload -> {
        // Dedicated servers register the wire format too, but never receive a
        // clientbound packet. Keeping the default receiver client-class-free
        // prevents physical-server linkage to net.minecraft.client classes.
    };

    private NeoForgeNetworking() {
    }

    static void register(final IEventBus modBus) {
        SmartDropsNetworking.initialize(new SmartDropsNetworking.Transport() {
            @Override
            public boolean canSend(
                    final ServerPlayer player,
                    final CustomPacketPayload.Type<?> type
            ) {
                return player.connection.hasChannel(type);
            }

            @Override
            public void send(final ServerPlayer player, final CustomPacketPayload payload) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        });
        modBus.addListener(RegisterPayloadHandlersEvent.class, NeoForgeNetworking::registerPayloads);
    }

    static void installClientReceiver(final Consumer<CustomPacketPayload> receiver) {
        clientReceiver = Objects.requireNonNull(receiver, "receiver");
    }

    private static void registerPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION).optional();
        registrar.playToServer(
                ConfigRequestPayload.TYPE,
                ConfigRequestPayload.CODEC,
                (payload, context) -> SmartDropsNetworking.handleRequest(
                        payload,
                        (ServerPlayer) context.player()));
        registrar.playToServer(
                ConfigPatchPayload.TYPE,
                ConfigPatchPayload.CODEC,
                (payload, context) -> SmartDropsNetworking.handlePatch(
                        payload,
                        (ServerPlayer) context.player()));
        registrar.playToServer(
                ConfigResetPayload.TYPE,
                ConfigResetPayload.CODEC,
                (payload, context) -> SmartDropsNetworking.handleReset(
                        payload,
                        (ServerPlayer) context.player()));
        registrar.playToClient(
                ConfigSnapshotPayload.TYPE,
                ConfigSnapshotPayload.CODEC,
                (payload, context) -> clientReceiver.accept(payload));
        registrar.playToClient(
                ConfigInvalidationPayload.TYPE,
                ConfigInvalidationPayload.CODEC,
                (payload, context) -> clientReceiver.accept(payload));
        registrar.playToClient(
                ConfigMutationResultPayload.TYPE,
                ConfigMutationResultPayload.CODEC,
                (payload, context) -> clientReceiver.accept(payload));
    }
}
