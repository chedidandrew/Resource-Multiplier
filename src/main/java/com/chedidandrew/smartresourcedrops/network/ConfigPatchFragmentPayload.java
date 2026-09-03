package com.chedidandrew.smartresourcedrops.network;

import java.util.ArrayList;
import java.util.List;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** One canonical fragment of a client-to-server configuration patch. */
public record ConfigPatchFragmentPayload(
        int format,
        int requestId,
        long expectedRevision,
        ConfigTransferCodec.Compression compression,
        int rawBytes,
        int encodedBytes,
        int chunkIndex,
        int chunkCount,
        byte[] chunk
) implements ConfigPayload {
    public static final ResourceLocation TYPE = SmartResourceDrops.id("config_patch_fragment");

    public ConfigPatchFragmentPayload {
        chunk = chunk.clone();
        ConfigTransferCodec.validateFrame(
                format,
                requestId,
                expectedRevision,
                compression,
                rawBytes,
                encodedBytes,
                chunkIndex,
                chunkCount,
                chunk);
    }

    public static List<ConfigPatchFragmentPayload> encode(final ConfigPatchPayload payload) {
        final ConfigTransferCodec.EncodedBody body = ConfigTransferCodec.encode(payload.json());
        final ArrayList<ConfigPatchFragmentPayload> frames = new ArrayList<>(body.chunks().size());
        for (int index = 0; index < body.chunks().size(); index++) {
            frames.add(new ConfigPatchFragmentPayload(
                    ConfigTransferCodec.FORMAT,
                    payload.requestId(),
                    payload.expectedRevision(),
                    body.compression(),
                    body.rawBytes(),
                    body.encodedBytes(),
                    index,
                    body.chunks().size(),
                    body.chunks().get(index)));
        }
        return List.copyOf(frames);
    }

    public static ConfigPatchFragmentPayload read(final FriendlyByteBuf buffer) {
        final int format = buffer.readVarInt();
        final int requestId = buffer.readVarInt();
        final long expectedRevision = buffer.readVarLong();
        final ConfigTransferCodec.Compression compression =
                ConfigTransferCodec.Compression.fromOrdinal(buffer.readVarInt());
        final int rawBytes = buffer.readVarInt();
        final int encodedBytes = buffer.readVarInt();
        final int chunkIndex = buffer.readVarInt();
        final int chunkCount = buffer.readVarInt();
        final byte[] chunk = buffer.readByteArray(ConfigTransferCodec.CHUNK_BYTES);
        if (buffer.readableBytes() != 0) {
            throw new IllegalArgumentException("Trailing bytes in config patch fragment");
        }
        return new ConfigPatchFragmentPayload(
                format, requestId, expectedRevision, compression, rawBytes, encodedBytes,
                chunkIndex, chunkCount, chunk);
    }

    @Override
    public ResourceLocation id() {
        return TYPE;
    }

    @Override
    public void write(final FriendlyByteBuf buffer) {
        buffer.writeVarInt(format);
        buffer.writeVarInt(requestId);
        buffer.writeVarLong(expectedRevision);
        buffer.writeVarInt(compression.ordinal());
        buffer.writeVarInt(rawBytes);
        buffer.writeVarInt(encodedBytes);
        buffer.writeVarInt(chunkIndex);
        buffer.writeVarInt(chunkCount);
        buffer.writeByteArray(chunk);
    }

    @Override
    public byte[] chunk() {
        return chunk.clone();
    }
}
