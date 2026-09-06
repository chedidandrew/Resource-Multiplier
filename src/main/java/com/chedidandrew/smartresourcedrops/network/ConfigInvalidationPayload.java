package com.chedidandrew.smartresourcedrops.network;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** Tells open editors that a newer authoritative configuration has been published. */
public record ConfigInvalidationPayload(long revision, ChangeKind changeKind) implements ConfigPayload {
    public static final ResourceLocation TYPE = SmartResourceDrops.id("config_invalidation");

    public static ConfigInvalidationPayload read(final FriendlyByteBuf buffer) {
        final ConfigInvalidationPayload payload = new ConfigInvalidationPayload(
                buffer.readVarLong(),
                ChangeKind.fromOrdinal(buffer.readVarInt()));
        ConfigPayload.requireFullyRead(buffer, "config invalidation");
        return payload;
    }

    @Override
    public ResourceLocation id() {
        return TYPE;
    }

    @Override
    public void write(final FriendlyByteBuf buffer) {
        buffer.writeVarLong(revision);
        buffer.writeVarInt(changeKind.ordinal());
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
