package com.chedidandrew.smartresourcedrops.network;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Compact rejection/authorization response. Successful mutations still receive a complete
 * authoritative snapshot; failures do not reflect a potentially large configuration payload.
 */
public record ConfigMutationResultPayload(
        int requestId,
        long revision,
        boolean editable,
        ConfigSnapshotPayload.PatchResult result
) implements CustomPacketPayload {
    public static final Type<ConfigMutationResultPayload> TYPE =
            new Type<>(SmartResourceDrops.id("config_mutation_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigMutationResultPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.requestId());
                buffer.writeVarLong(payload.revision());
                buffer.writeBoolean(payload.editable());
                buffer.writeVarInt(payload.result().ordinal());
            },
            buffer -> new ConfigMutationResultPayload(
                    buffer.readVarInt(),
                    buffer.readVarLong(),
                    buffer.readBoolean(),
                    ConfigSnapshotPayload.PatchResult.fromOrdinal(buffer.readVarInt())));

    @Override
    public Type<ConfigMutationResultPayload> type() {
        return TYPE;
    }
}
