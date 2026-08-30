package com.chedidandrew.smartresourcedrops.network;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** One server-authoritative request to replace the complete configuration with mod defaults. */
public record ConfigResetPayload(int requestId, long expectedRevision) implements CustomPacketPayload {
    public static final Type<ConfigResetPayload> TYPE =
            new Type<>(SmartResourceDrops.id("config_reset"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigResetPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.requestId());
                buffer.writeVarLong(payload.expectedRevision());
            },
            buffer -> new ConfigResetPayload(buffer.readVarInt(), buffer.readVarLong()));

    @Override
    public Type<ConfigResetPayload> type() {
        return TYPE;
    }
}
