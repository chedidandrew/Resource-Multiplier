package com.chedidandrew.smartresourcedrops.network;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.config.ConfigPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** One bounded delta containing only settings explicitly edited by an operator. */
public record ConfigPatchPayload(int requestId, long expectedRevision, String json) implements CustomPacketPayload {
    public static final int MAX_JSON_LENGTH = ConfigPatch.MAX_JSON_LENGTH;
    public static final Type<ConfigPatchPayload> TYPE =
            new Type<>(SmartResourceDrops.id("config_patch"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigPatchPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.requestId());
                buffer.writeVarLong(payload.expectedRevision());
                buffer.writeUtf(payload.json(), MAX_JSON_LENGTH);
            },
            buffer -> new ConfigPatchPayload(
                    buffer.readVarInt(),
                    buffer.readVarLong(),
                    buffer.readUtf(MAX_JSON_LENGTH)));

    @Override
    public Type<ConfigPatchPayload> type() {
        return TYPE;
    }
}
