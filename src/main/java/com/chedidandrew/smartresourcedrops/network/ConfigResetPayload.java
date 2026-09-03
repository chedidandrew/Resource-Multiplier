package com.chedidandrew.smartresourcedrops.network;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** One server-authoritative request to replace the complete configuration with mod defaults. */
public record ConfigResetPayload(int requestId, long expectedRevision) implements ConfigPayload {
    public static final ResourceLocation TYPE = SmartResourceDrops.id("config_reset");

    public ConfigResetPayload {
        if (requestId <= 0 || expectedRevision < 0L) {
            throw new IllegalArgumentException("Invalid config reset identity");
        }
    }

    public static ConfigResetPayload read(final FriendlyByteBuf buffer) {
        final int requestId = buffer.readVarInt();
        final long expectedRevision = buffer.readVarLong();
        ConfigPayload.requireFullyRead(buffer, "config reset");
        return new ConfigResetPayload(requestId, expectedRevision);
    }

    @Override
    public ResourceLocation id() {
        return TYPE;
    }

    @Override
    public void write(final FriendlyByteBuf buffer) {
        buffer.writeVarInt(requestId);
        buffer.writeVarLong(expectedRevision);
    }
}
