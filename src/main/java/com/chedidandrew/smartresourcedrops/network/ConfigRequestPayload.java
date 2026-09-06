package com.chedidandrew.smartresourcedrops.network;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record ConfigRequestPayload(int requestId) implements ConfigPayload {
    public static final ResourceLocation TYPE = SmartResourceDrops.id("config_request");

    public static ConfigRequestPayload read(final FriendlyByteBuf buffer) {
        final ConfigRequestPayload payload = new ConfigRequestPayload(buffer.readVarInt());
        ConfigPayload.requireFullyRead(buffer, "config request");
        return payload;
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
