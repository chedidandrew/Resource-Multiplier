package com.chedidandrew.smartresourcedrops.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** Version-neutral config packet contract used by the loader adapters. */
public interface ConfigPayload {
    ResourceLocation id();

    void write(FriendlyByteBuf buffer);

    static void requireFullyRead(final FriendlyByteBuf buffer, final String payloadName) {
        if (buffer.readableBytes() != 0) {
            throw new IllegalArgumentException("Trailing bytes in " + payloadName);
        }
    }
}
