package com.chedidandrew.smartresourcedrops.network;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.config.ConfigPatch;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** One bounded delta containing only settings explicitly edited by an operator. */
public record ConfigPatchPayload(int requestId, long expectedRevision, String json) implements ConfigPayload {
    public static final int MAX_JSON_LENGTH = ConfigPatch.MAX_JSON_LENGTH;
    public static final ResourceLocation TYPE = SmartResourceDrops.id("config_patch");

    public ConfigPatchPayload {
        if (requestId <= 0 || expectedRevision < 0L || json == null) {
            throw new IllegalArgumentException("Invalid logical config patch");
        }
    }

    public static ConfigPatchPayload read(final FriendlyByteBuf buffer) {
        return new ConfigPatchPayload(
                buffer.readVarInt(),
                buffer.readVarLong(),
                buffer.readUtf(MAX_JSON_LENGTH));
    }

    @Override
    public ResourceLocation id() {
        return TYPE;
    }

    @Override
    public void write(final FriendlyByteBuf buffer) {
        buffer.writeVarInt(requestId);
        buffer.writeVarLong(expectedRevision);
        buffer.writeUtf(json, MAX_JSON_LENGTH);
    }
}
