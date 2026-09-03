package com.chedidandrew.smartresourcedrops.network;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record ConfigRequestPayload(int requestId) implements ConfigPayload {
    public static final ResourceLocation TYPE = SmartResourceDrops.id("config_request");

    public ConfigRequestPayload {
        if (requestId <= 0) {
            throw new IllegalArgumentException("Config request ID must be positive");
        }
    }

    public static ConfigRequestPayload read(final FriendlyByteBuf buffer) {
        final int requestId = buffer.readVarInt();
        ConfigPayload.requireFullyRead(buffer, "config request");
        return new ConfigRequestPayload(requestId);
    }

    @Override
    public ResourceLocation id() {
        return TYPE;
    }

    @Override
    public void write(final FriendlyByteBuf buffer) {
        buffer.writeVarInt(requestId);
    }
}
