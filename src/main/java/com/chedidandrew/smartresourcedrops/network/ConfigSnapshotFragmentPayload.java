package com.chedidandrew.smartresourcedrops.network;

import java.util.ArrayList;
import java.util.List;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** One canonical fragment of a server-to-client authoritative configuration snapshot. */
public record ConfigSnapshotFragmentPayload(
        int format,
        int requestId,
        long revision,
        boolean editable,
        ConfigSnapshotPayload.PatchResult patchResult,
        ConfigTransferCodec.Compression compression,
        int rawBytes,
        int encodedBytes,
        int chunkIndex,
        int chunkCount,
        byte[] chunk
) implements ConfigPayload {
    public static final ResourceLocation TYPE = SmartResourceDrops.id("config_snapshot_fragment");

    public ConfigSnapshotFragmentPayload {
        chunk = chunk.clone();
        if (patchResult == null) {
            throw new IllegalArgumentException("Missing config snapshot result");
        }
        ConfigTransferCodec.validateFrame(
                format,
                requestId,
                revision,
                compression,
                rawBytes,
                encodedBytes,
                chunkIndex,
                chunkCount,
                chunk);
    }

    public static List<ConfigSnapshotFragmentPayload> encode(final ConfigSnapshotPayload payload) {
        final ConfigTransferCodec.EncodedBody body = ConfigTransferCodec.encode(payload.json());
        final ArrayList<ConfigSnapshotFragmentPayload> frames = new ArrayList<>(body.chunks().size());
        for (int index = 0; index < body.chunks().size(); index++) {
            frames.add(new ConfigSnapshotFragmentPayload(
                    ConfigTransferCodec.FORMAT,
                    payload.requestId(),
                    payload.revision(),
                    payload.editable(),
                    payload.patchResult(),
                    body.compression(),
                    body.rawBytes(),
                    body.encodedBytes(),
                    index,
                    body.chunks().size(),
                    body.chunks().get(index)));
        }
        return List.copyOf(frames);
    }

    public static ConfigSnapshotFragmentPayload read(final FriendlyByteBuf buffer) {
        final int format = buffer.readVarInt();
        final int requestId = buffer.readVarInt();
        final long revision = buffer.readVarLong();
        final boolean editable = buffer.readBoolean();
        final ConfigSnapshotPayload.PatchResult result =
                ConfigSnapshotPayload.PatchResult.fromOrdinal(buffer.readVarInt());
        final ConfigTransferCodec.Compression compression =
                ConfigTransferCodec.Compression.fromOrdinal(buffer.readVarInt());
        final int rawBytes = buffer.readVarInt();
        final int encodedBytes = buffer.readVarInt();
        final int chunkIndex = buffer.readVarInt();
        final int chunkCount = buffer.readVarInt();
        final byte[] chunk = buffer.readByteArray(ConfigTransferCodec.CHUNK_BYTES);
        if (buffer.readableBytes() != 0) {
            throw new IllegalArgumentException("Trailing bytes in config snapshot fragment");
        }
        return new ConfigSnapshotFragmentPayload(
                format, requestId, revision, editable, result, compression,
                rawBytes, encodedBytes, chunkIndex, chunkCount, chunk);
    }

    @Override
    public ResourceLocation id() {
        return TYPE;
    }

    @Override
    public void write(final FriendlyByteBuf buffer) {
        buffer.writeVarInt(format);
        buffer.writeVarInt(requestId);
        buffer.writeVarLong(revision);
        buffer.writeBoolean(editable);
        buffer.writeVarInt(patchResult.ordinal());
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
