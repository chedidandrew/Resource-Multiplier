package com.chedidandrew.smartresourcedrops.network;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ConfigSnapshotPayload(
        int requestId,
        long revision,
        String json,
        boolean editable,
        PatchResult patchResult
) implements CustomPacketPayload {
    public static final int MAX_JSON_LENGTH = 1_048_576;

    public static final Type<ConfigSnapshotPayload> TYPE =
            new Type<>(SmartResourceDrops.id("config_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigSnapshotPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.requestId());
                buffer.writeVarLong(payload.revision());
                buffer.writeUtf(payload.json(), MAX_JSON_LENGTH);
                buffer.writeBoolean(payload.editable());
                buffer.writeVarInt(payload.patchResult().ordinal());
            },
            buffer -> new ConfigSnapshotPayload(
                    buffer.readVarInt(),
                    buffer.readVarLong(),
                    buffer.readUtf(MAX_JSON_LENGTH),
                    buffer.readBoolean(),
                    PatchResult.fromOrdinal(buffer.readVarInt())));

    @Override
    public Type<ConfigSnapshotPayload> type() {
        return TYPE;
    }

    public enum PatchResult {
        NONE,
        APPLIED,
        REJECTED,
        UNAUTHORIZED,
        RESET_APPLIED,
        RESET_REJECTED,
        RESET_UNAUTHORIZED;

        static PatchResult fromOrdinal(int ordinal) {
            PatchResult[] values = values();
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : REJECTED;
        }
    }
}
