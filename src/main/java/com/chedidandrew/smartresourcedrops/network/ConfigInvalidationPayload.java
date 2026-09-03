package com.chedidandrew.smartresourcedrops.network;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** Tells open editors that a newer authoritative configuration has been published. */
public record ConfigInvalidationPayload(long revision, ChangeKind changeKind) implements ConfigPayload {
    public static final ResourceLocation TYPE = SmartResourceDrops.id("config_invalidation");

    public ConfigInvalidationPayload {
        if (revision <= 0L || changeKind == null) {
            throw new IllegalArgumentException("Invalid config invalidation");
        }
    }

    public static ConfigInvalidationPayload read(final FriendlyByteBuf buffer) {
        final long revision = buffer.readVarLong();
        final ChangeKind changeKind = ChangeKind.fromOrdinal(buffer.readVarInt());
        ConfigPayload.requireFullyRead(buffer, "config invalidation");
        return new ConfigInvalidationPayload(revision, changeKind);
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

        static ChangeKind fromOrdinal(final int ordinal) {
            final ChangeKind[] values = values();
            if (ordinal < 0 || ordinal >= values.length) {
                throw new IllegalArgumentException("Unknown config invalidation kind");
            }
            return values[ordinal];
        }
    }
}
