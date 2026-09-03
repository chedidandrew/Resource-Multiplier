package com.chedidandrew.smartresourcedrops.network;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record ConfigSnapshotPayload(
        int requestId,
        long revision,
        String json,
        boolean editable,
        PatchResult patchResult
) implements ConfigPayload {
    public static final int MAX_JSON_LENGTH = 1_048_576;

    public static final ResourceLocation TYPE = SmartResourceDrops.id("config_snapshot");

    public ConfigSnapshotPayload {
        if (requestId <= 0 || revision < 0L || json == null || patchResult == null) {
            throw new IllegalArgumentException("Invalid logical config snapshot");
        }
    }

    public static ConfigSnapshotPayload read(final FriendlyByteBuf buffer) {
        return new ConfigSnapshotPayload(
                buffer.readVarInt(),
                buffer.readVarLong(),
                buffer.readUtf(MAX_JSON_LENGTH),
                buffer.readBoolean(),
                PatchResult.fromOrdinal(buffer.readVarInt()));
    }

    @Override
    public ResourceLocation id() {
        return TYPE;
    }

    @Override
    public void write(final FriendlyByteBuf buffer) {
        buffer.writeVarInt(requestId);
        buffer.writeVarLong(revision);
        buffer.writeUtf(json, MAX_JSON_LENGTH);
        buffer.writeBoolean(editable);
        buffer.writeVarInt(patchResult.ordinal());
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
            if (ordinal < 0 || ordinal >= values.length) {
                throw new IllegalArgumentException("Unknown config mutation result");
            }
            return values[ordinal];
        }
    }
}
