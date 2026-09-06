package com.chedidandrew.smartresourcedrops.network;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Compact rejection/authorization response. Successful mutations still receive a complete
 * authoritative snapshot; failures do not reflect a potentially large configuration payload.
 */
public record ConfigMutationResultPayload(
        int requestId,
        long revision,
        boolean editable,
        ConfigSnapshotPayload.PatchResult result
) implements ConfigPayload {
    public static final ResourceLocation TYPE = SmartResourceDrops.id("config_mutation_result");

    public static ConfigMutationResultPayload read(final FriendlyByteBuf buffer) {
        final ConfigMutationResultPayload payload = new ConfigMutationResultPayload(
                buffer.readVarInt(),
                buffer.readVarLong(),
                buffer.readBoolean(),
                ConfigSnapshotPayload.PatchResult.fromOrdinal(buffer.readVarInt()));
        ConfigPayload.requireFullyRead(buffer, "config mutation result");
        return payload;
    }

    @Override
    public ResourceLocation id() {
        return TYPE;
    }

    @Override
    public void write(final FriendlyByteBuf buffer) {
        buffer.writeVarInt(requestId);
        buffer.writeVarLong(revision);
        buffer.writeBoolean(editable);
        buffer.writeVarInt(result.ordinal());
    }
}
