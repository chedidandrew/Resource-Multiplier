package com.chedidandrew.smartresourcedrops.platform.neoforge;

import java.util.function.Supplier;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.network.ConfigInvalidationPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigMutationResultPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigPatchFragmentPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigRequestPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigResetPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigSnapshotFragmentPayload;
import com.chedidandrew.smartresourcedrops.network.SmartDropsNetworking;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Legacy Forge SimpleChannel adapter for the shared server-authoritative protocol. */
final class NeoForgeNetworking {
    private static final String PROTOCOL_VERSION = "2";
    static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(SmartResourceDrops.id("config"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION))
            .serverAcceptedVersions(NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION))
            .simpleChannel();

    private NeoForgeNetworking() {
    }

    static void register() {
        SmartDropsNetworking.initialize(new SmartDropsNetworking.Transport() {
            @Override
            public boolean canSend(final ServerPlayer player, final net.minecraft.resources.ResourceLocation type) {
                return CHANNEL.isRemotePresent(player.connection.connection);
            }

            @Override
            public void send(final ServerPlayer player, final ConfigPayload payload) {
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
            }
        });

        int discriminator = 0;
        CHANNEL.messageBuilder(ConfigRequestPayload.class, discriminator++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ConfigRequestPayload::write)
                .decoder(ConfigRequestPayload::read)
                .consumerMainThread((payload, context) -> handleServer(
                        context,
                        player -> SmartDropsNetworking.handleRequest(payload, player)))
                .add();
        CHANNEL.messageBuilder(ConfigPatchFragmentPayload.class, discriminator++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ConfigPatchFragmentPayload::write)
                .decoder(ConfigPatchFragmentPayload::read)
                .consumerMainThread((payload, context) -> handleServer(
                        context,
                        player -> SmartDropsNetworking.handlePatchFragment(payload, player)))
                .add();
        CHANNEL.messageBuilder(ConfigResetPayload.class, discriminator++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ConfigResetPayload::write)
                .decoder(ConfigResetPayload::read)
                .consumerMainThread((payload, context) -> handleServer(
                        context,
                        player -> SmartDropsNetworking.handleReset(payload, player)))
                .add();

        CHANNEL.messageBuilder(ConfigSnapshotFragmentPayload.class, discriminator++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ConfigSnapshotFragmentPayload::write)
                .decoder(ConfigSnapshotFragmentPayload::read)
                .consumerMainThread((payload, context) -> {
                    NeoForgeClientEntrypoint.accept(payload);
                    context.get().setPacketHandled(true);
                })
                .add();
        CHANNEL.messageBuilder(ConfigInvalidationPayload.class, discriminator++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ConfigInvalidationPayload::write)
                .decoder(ConfigInvalidationPayload::read)
                .consumerMainThread((payload, context) -> {
                    NeoForgeClientEntrypoint.accept(payload);
                    context.get().setPacketHandled(true);
                })
                .add();
        CHANNEL.messageBuilder(ConfigMutationResultPayload.class, discriminator, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ConfigMutationResultPayload::write)
                .decoder(ConfigMutationResultPayload::read)
                .consumerMainThread((payload, context) -> {
                    NeoForgeClientEntrypoint.accept(payload);
                    context.get().setPacketHandled(true);
                })
                .add();
    }

    private static void handleServer(
            final Supplier<NetworkEvent.Context> contextSupplier,
            final java.util.function.Consumer<ServerPlayer> handler
    ) {
        final NetworkEvent.Context context = contextSupplier.get();
        final ServerPlayer player = context.getSender();
        if (player != null) {
            handler.accept(player);
        }
        context.setPacketHandled(true);
    }
}
