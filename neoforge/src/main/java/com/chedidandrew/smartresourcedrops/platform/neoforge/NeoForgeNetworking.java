package com.chedidandrew.smartresourcedrops.platform.neoforge;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.network.ConfigInvalidationPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigMutationResultPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigPatchPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigRequestPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigResetPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigSnapshotPayload;
import com.chedidandrew.smartresourcedrops.network.SmartDropsNetworking;

import java.util.Objects;
import java.util.function.Consumer;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

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
                    final ResourceLocation type
            ) {
                return NetworkRegistry.getInstance().isConnected(player.connection, type);
            }

            @Override
            public void send(final ServerPlayer player, final ConfigPayload payload) {
                PacketDistributor.PLAYER.with(player).send(payload);
            }
        });
        modBus.addListener(RegisterPayloadHandlerEvent.class, NeoForgeNetworking::registerPayloads);
    }

    static void installClientReceiver(final Consumer<CustomPacketPayload> receiver) {
        clientReceiver = Objects.requireNonNull(receiver, "receiver");
    }

    private static void registerPayloads(final RegisterPayloadHandlerEvent event) {
        final IPayloadRegistrar registrar = event.registrar(SmartResourceDrops.MOD_ID)
                .versioned(PROTOCOL_VERSION)
                .optional();
        registrar.play(
                ConfigRequestPayload.TYPE,
                ConfigRequestPayload::read,
                builder -> builder.server((payload, context) -> context.workHandler().execute(() ->
                        SmartDropsNetworking.handleRequest(
                                payload,
                                (ServerPlayer) context.player().orElseThrow()))));
        registrar.play(
                ConfigPatchPayload.TYPE,
                ConfigPatchPayload::read,
                builder -> builder.server((payload, context) -> context.workHandler().execute(() ->
                        SmartDropsNetworking.handlePatch(
                                payload,
                                (ServerPlayer) context.player().orElseThrow()))));
        registrar.play(
                ConfigResetPayload.TYPE,
                ConfigResetPayload::read,
                builder -> builder.server((payload, context) -> context.workHandler().execute(() ->
                        SmartDropsNetworking.handleReset(
                                payload,
                                (ServerPlayer) context.player().orElseThrow()))));
        registrar.play(
                ConfigSnapshotPayload.TYPE,
                ConfigSnapshotPayload::read,
                builder -> builder.client((payload, context) -> context.workHandler().execute(() ->
                        clientReceiver.accept(payload))));
        registrar.play(
                ConfigInvalidationPayload.TYPE,
                ConfigInvalidationPayload::read,
                builder -> builder.client((payload, context) -> context.workHandler().execute(() ->
                        clientReceiver.accept(payload))));
        registrar.play(
                ConfigMutationResultPayload.TYPE,
                ConfigMutationResultPayload::read,
                builder -> builder.client((payload, context) -> context.workHandler().execute(() ->
                        clientReceiver.accept(payload))));
    }
}
