package com.chedidandrew.smartresourcedrops.network;

import java.util.Objects;
import java.util.Optional;

/** One-transfer-at-a-time bounded assembler; callers own peer/global admission limits. */
public final class ConfigTransferAssembler<M> {
    private Transfer<M> transfer;

    public Optional<Complete<M>> accept(
            final M metadata,
            final ConfigTransferCodec.Compression compression,
            final int rawBytes,
            final int encodedBytes,
            final int chunkIndex,
            final int chunkCount,
            final byte[] chunk,
            final long tick
    ) {
        Objects.requireNonNull(metadata, "metadata");
        Objects.requireNonNull(compression, "compression");
        Objects.requireNonNull(chunk, "chunk");
        final int canonicalCount;
        try {
            canonicalCount = ConfigTransferCodec.canonicalChunkCount(encodedBytes);
        } catch (IllegalArgumentException exception) {
            transfer = null;
            throw exception;
        }
        if (rawBytes < 1 || rawBytes > ConfigTransferCodec.RAW_UTF8_MAX
                || chunkCount != canonicalCount
                || chunkIndex < 0 || chunkIndex >= chunkCount
                || chunk.length != ConfigTransferCodec.canonicalChunkLength(
                        encodedBytes, chunkIndex, chunkCount)
                || (compression == ConfigTransferCodec.Compression.NONE && rawBytes != encodedBytes)
                || (compression == ConfigTransferCodec.Compression.ZLIB && encodedBytes >= rawBytes)) {
            transfer = null;
            throw new IllegalArgumentException("Invalid config transfer frame");
        }
        if (transfer == null) {
            transfer = new Transfer<>(
                    metadata,
                    compression,
                    rawBytes,
                    encodedBytes,
                    chunkCount,
                    tick);
        } else if (!transfer.matches(
                metadata, compression, rawBytes, encodedBytes, chunkCount)) {
            transfer = null;
            throw new IllegalArgumentException("Config transfer metadata changed mid-stream");
        }
        if (transfer.chunks[chunkIndex] != null) {
            transfer = null;
            throw new IllegalArgumentException("Duplicate config transfer chunk");
        }
        transfer.chunks[chunkIndex] = chunk.clone();
        transfer.received++;
        transfer.bufferedBytes += chunk.length;
        transfer.lastTick = tick;
        if (transfer.received != transfer.chunks.length) {
            return Optional.empty();
        }

        final Transfer<M> completed = transfer;
        transfer = null;
        if (completed.bufferedBytes != completed.encodedBytes) {
            throw new IllegalArgumentException("Assembled config transfer length mismatch");
        }
        final byte[] encoded = new byte[completed.encodedBytes];
        int offset = 0;
        for (byte[] part : completed.chunks) {
            System.arraycopy(part, 0, encoded, offset, part.length);
            offset += part.length;
        }
        return Optional.of(new Complete<>(
                completed.metadata,
                completed.compression,
                completed.rawBytes,
                completed.encodedBytes,
                encoded));
    }

    public boolean active() {
        return transfer != null;
    }

    public int bufferedBytes() {
        return transfer == null ? 0 : transfer.bufferedBytes;
    }

    public boolean hasChunk(final int index) {
        return transfer != null && index >= 0 && index < transfer.chunks.length
                && transfer.chunks[index] != null;
    }

    public void expire(final long tick, final long timeoutTicks) {
        if (transfer != null && (tick < transfer.lastTick || tick - transfer.lastTick > timeoutTicks)) {
            transfer = null;
        }
    }

    public void clear() {
        transfer = null;
    }

    public record Complete<M>(
            M metadata,
            ConfigTransferCodec.Compression compression,
            int rawBytes,
            int encodedBytes,
            byte[] encoded
    ) {
        public Complete {
            encoded = encoded.clone();
        }

        @Override
        public byte[] encoded() {
            return encoded.clone();
        }

        public String decode() {
            return ConfigTransferCodec.decode(compression, rawBytes, encodedBytes, encoded);
        }
    }

    private static final class Transfer<M> {
        private final M metadata;
        private final ConfigTransferCodec.Compression compression;
        private final int rawBytes;
        private final int encodedBytes;
        private final byte[][] chunks;
        private int received;
        private int bufferedBytes;
        private long lastTick;

        private Transfer(
                final M metadata,
                final ConfigTransferCodec.Compression compression,
                final int rawBytes,
                final int encodedBytes,
                final int chunkCount,
                final long tick
        ) {
            this.metadata = metadata;
            this.compression = compression;
            this.rawBytes = rawBytes;
            this.encodedBytes = encodedBytes;
            this.chunks = new byte[chunkCount][];
            this.lastTick = tick;
        }

        private boolean matches(
                final M expectedMetadata,
                final ConfigTransferCodec.Compression expectedCompression,
                final int expectedRawBytes,
                final int expectedEncodedBytes,
                final int expectedChunkCount
        ) {
            return metadata.equals(expectedMetadata)
                    && compression == expectedCompression
                    && rawBytes == expectedRawBytes
                    && encodedBytes == expectedEncodedBytes
                    && chunks.length == expectedChunkCount;
        }
    }
}
