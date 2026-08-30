package com.chedidandrew.smartresourcedrops.network;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ConfigRequestPayload(int requestId) implements CustomPacketPayload {
    public static final Type<ConfigRequestPayload> TYPE =
            new Type<>(SmartResourceDrops.id("config_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigRequestPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeVarInt(payload.requestId()),
            buffer -> new ConfigRequestPayload(buffer.readVarInt()));

    @Override
    public Type<ConfigRequestPayload> type() {
        return TYPE;
    }
}
