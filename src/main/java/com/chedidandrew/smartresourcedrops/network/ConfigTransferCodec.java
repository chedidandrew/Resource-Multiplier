package com.chedidandrew.smartresourcedrops.network;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/** Strict bounded UTF-8, compression, and canonical framing for legacy play payload limits. */
public final class ConfigTransferCodec {
    public static final int FORMAT = 1;
    public static final int RAW_UTF8_MAX = 1_048_576;
    public static final int CHUNK_BYTES = 30_720;
    public static final int MAX_CHUNKS = 35;

    private ConfigTransferCodec() {
    }

    public static EncodedBody encode(final String json) {
        final byte[] raw = encodeUtf8(json);
        if (raw.length < 1 || raw.length > RAW_UTF8_MAX) {
            throw new IllegalArgumentException(
                    "Config JSON UTF-8 length must be 1.." + RAW_UTF8_MAX + " bytes");
        }
        final byte[] compressed = deflate(raw);
        final Compression compression;
        final byte[] encoded;
        if (compressed.length < raw.length) {
            compression = Compression.ZLIB;
            encoded = compressed;
        } else {
            compression = Compression.NONE;
            encoded = raw;
        }
        final int count = canonicalChunkCount(encoded.length);
        final ArrayList<byte[]> chunks = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            final int start = index * CHUNK_BYTES;
            final int length = canonicalChunkLength(encoded.length, index, count);
            chunks.add(java.util.Arrays.copyOfRange(encoded, start, start + length));
        }
        return new EncodedBody(
                compression,
                raw.length,
                encoded.length,
                List.copyOf(chunks));
    }

    public static String decode(
            final Compression compression,
            final int rawBytes,
            final int encodedBytes,
            final byte[] encoded
    ) {
        validateLengths(compression, rawBytes, encodedBytes);
        if (encoded.length != encodedBytes) {
            throw new IllegalArgumentException("Encoded config length does not match frame metadata");
        }
        final byte[] raw = compression == Compression.NONE
                ? encoded.clone()
                : inflate(encoded, rawBytes);
        if (raw.length != rawBytes) {
            throw new IllegalArgumentException("Decoded config length does not match frame metadata");
        }
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(raw))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException("Config transfer is not strict UTF-8", exception);
        }
    }

    public static void validateFrame(
            final int format,
            final int requestId,
            final long revision,
            final Compression compression,
            final int rawBytes,
            final int encodedBytes,
            final int chunkIndex,
            final int chunkCount,
            final byte[] chunk
    ) {
        if (format != FORMAT || requestId <= 0 || revision < 0L) {
            throw new IllegalArgumentException("Invalid config transfer identity");
        }
        validateLengths(compression, rawBytes, encodedBytes);
        final int canonicalCount = canonicalChunkCount(encodedBytes);
        if (chunkCount != canonicalCount || chunkCount > MAX_CHUNKS
                || chunkIndex < 0 || chunkIndex >= chunkCount) {
            throw new IllegalArgumentException("Non-canonical config transfer chunk metadata");
        }
        if (chunk.length != canonicalChunkLength(encodedBytes, chunkIndex, chunkCount)) {
            throw new IllegalArgumentException("Non-canonical config transfer chunk length");
        }
    }

    public static int canonicalChunkCount(final int encodedBytes) {
        if (encodedBytes < 1 || encodedBytes > RAW_UTF8_MAX) {
            throw new IllegalArgumentException("Invalid encoded config length");
        }
        final int count = Math.floorDiv(encodedBytes - 1, CHUNK_BYTES) + 1;
        if (count > MAX_CHUNKS) {
            throw new IllegalArgumentException("Config transfer exceeds the maximum chunk count");
        }
        return count;
    }

    public static int canonicalChunkLength(
            final int encodedBytes,
            final int chunkIndex,
            final int chunkCount
    ) {
        if (chunkCount != canonicalChunkCount(encodedBytes)
                || chunkIndex < 0 || chunkIndex >= chunkCount) {
            throw new IllegalArgumentException("Invalid config transfer chunk index");
        }
        return Math.min(CHUNK_BYTES, encodedBytes - chunkIndex * CHUNK_BYTES);
    }

    private static void validateLengths(
            final Compression compression,
            final int rawBytes,
            final int encodedBytes
    ) {
        if (compression == null || rawBytes < 1 || rawBytes > RAW_UTF8_MAX
                || encodedBytes < 1 || encodedBytes > RAW_UTF8_MAX) {
            throw new IllegalArgumentException("Invalid config transfer lengths");
        }
        if (compression == Compression.NONE && encodedBytes != rawBytes) {
            throw new IllegalArgumentException("Uncompressed transfer lengths must match");
        }
        if (compression == Compression.ZLIB && encodedBytes >= rawBytes) {
            throw new IllegalArgumentException("Compressed transfer must be smaller than raw UTF-8");
        }
    }

    private static byte[] encodeUtf8(final String value) {
        try {
            final ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(CharBuffer.wrap(value));
            final byte[] bytes = new byte[encoded.remaining()];
            encoded.get(bytes);
            return bytes;
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException("Config JSON contains malformed Unicode", exception);
        }
    }

    private static byte[] deflate(final byte[] raw) {
        final Deflater deflater = new Deflater(Deflater.BEST_SPEED);
        try {
            deflater.setInput(raw);
            deflater.finish();
            final ByteArrayOutputStream output = new ByteArrayOutputStream(raw.length);
            final byte[] buffer = new byte[8192];
            while (!deflater.finished()) {
                final int count = deflater.deflate(buffer);
                if (count <= 0) {
                    throw new IllegalArgumentException("Config compression did not make progress");
                }
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        } finally {
            deflater.end();
        }
    }

    private static byte[] inflate(final byte[] encoded, final int rawBytes) {
        final Inflater inflater = new Inflater();
        try {
            inflater.setInput(encoded);
            final byte[] raw = new byte[rawBytes];
            int offset = 0;
            while (offset < raw.length) {
                final int count = inflater.inflate(raw, offset, raw.length - offset);
                if (count == 0) {
                    break;
                }
                offset += count;
            }
            if (offset != raw.length || !inflater.finished() || inflater.getRemaining() != 0) {
                throw new IllegalArgumentException("Malformed or non-canonical compressed config transfer");
            }
            return raw;
        } catch (DataFormatException exception) {
            throw new IllegalArgumentException("Malformed compressed config transfer", exception);
        } finally {
            inflater.end();
        }
    }

    public enum Compression {
        NONE,
        ZLIB;

        public static Compression fromOrdinal(final int ordinal) {
            final Compression[] values = values();
            if (ordinal < 0 || ordinal >= values.length) {
                throw new IllegalArgumentException("Unknown config transfer compression");
            }
            return values[ordinal];
        }
    }

    public record EncodedBody(
            Compression compression,
            int rawBytes,
            int encodedBytes,
            List<byte[]> chunks
    ) {
        public EncodedBody {
            chunks = List.copyOf(chunks);
            if (chunks.size() != canonicalChunkCount(encodedBytes)) {
                throw new IllegalArgumentException("Encoded body has non-canonical chunks");
            }
            for (int index = 0; index < chunks.size(); index++) {
                final byte[] chunk = chunks.get(index);
                if (chunk.length != canonicalChunkLength(encodedBytes, index, chunks.size())) {
                    throw new IllegalArgumentException("Encoded body has non-canonical chunk length");
                }
            }
        }
    }
}
