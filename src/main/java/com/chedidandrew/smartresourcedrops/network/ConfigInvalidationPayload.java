package com.chedidandrew.smartresourcedrops.network;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Tells open editors that a newer authoritative configuration has been published. */
public record ConfigInvalidationPayload(long revision, ChangeKind changeKind) implements CustomPacketPayload {
    public static final Type<ConfigInvalidationPayload> TYPE =
            new Type<>(SmartResourceDrops.id("config_invalidation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigInvalidationPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarLong(payload.revision());
                buffer.writeVarInt(payload.changeKind().ordinal());
            },
            buffer -> new ConfigInvalidationPayload(
                    buffer.readVarLong(),
                    ChangeKind.fromOrdinal(buffer.readVarInt())));

    @Override
    public Type<ConfigInvalidationPayload> type() {
        return TYPE;
    }

    public enum ChangeKind {
        UPDATE,
        RESET;

        private static ChangeKind fromOrdinal(final int ordinal) {
            final ChangeKind[] values = values();
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : RESET;
        }
    }
}
