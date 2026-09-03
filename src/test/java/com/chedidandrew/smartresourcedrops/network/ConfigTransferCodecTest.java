package com.chedidandrew.smartresourcedrops.network;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.junit.jupiter.api.Test;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

final class ConfigTransferCodecTest {
    @Test
    void exactUtf8BoundaryRoundTripsAndEveryFrameFitsLegacyServerboundLimit() {
        final String json = pseudoRandomAscii(ConfigTransferCodec.RAW_UTF8_MAX);
        final ConfigTransferCodec.EncodedBody body = ConfigTransferCodec.encode(json);
        assertTrue(body.chunks().size() > 1);
        assertEquals(json, ConfigTransferCodec.decode(
                body.compression(), body.rawBytes(), body.encodedBytes(), join(body.chunks())));

        final List<ConfigPatchFragmentPayload> patchFrames = ConfigPatchFragmentPayload.encode(
                new ConfigPatchPayload(1, Long.MAX_VALUE, json));
        final List<ConfigSnapshotFragmentPayload> snapshotFrames = ConfigSnapshotFragmentPayload.encode(
                new ConfigSnapshotPayload(
                        1,
                        Long.MAX_VALUE,
                        json,
                        true,
                        ConfigSnapshotPayload.PatchResult.APPLIED));
        patchFrames.forEach(frame -> assertTrue(serializedBytes(frame) <= 32_767));
        snapshotFrames.forEach(frame -> assertTrue(serializedBytes(frame) <= 32_767));
    }

    @Test
    void utf8ByteLimitIsEnforcedInsteadOfCharacterCount() {
        final String exact = "\u20ac".repeat(ConfigTransferCodec.RAW_UTF8_MAX / 3);
        assertEquals(exact, decode(ConfigTransferCodec.encode(exact)));
        final String over = exact + "\u20ac";
        assertTrue(over.length() < ConfigTransferCodec.RAW_UTF8_MAX);
        assertThrows(IllegalArgumentException.class, () -> ConfigTransferCodec.encode(over));
    }

    @Test
    void malformedCompressedBodiesFailClosed() {
        final ConfigTransferCodec.EncodedBody body = ConfigTransferCodec.encode("x".repeat(100_000));
        assertEquals(ConfigTransferCodec.Compression.ZLIB, body.compression());
        final byte[] encoded = join(body.chunks());

        final byte[] truncated = java.util.Arrays.copyOf(encoded, encoded.length - 1);
        assertThrows(IllegalArgumentException.class, () -> ConfigTransferCodec.decode(
                body.compression(), body.rawBytes(), truncated.length, truncated));

        final byte[] trailing = java.util.Arrays.copyOf(encoded, encoded.length + 1);
        trailing[trailing.length - 1] = 1;
        assertThrows(IllegalArgumentException.class, () -> ConfigTransferCodec.decode(
                body.compression(), body.rawBytes(), trailing.length, trailing));

        assertThrows(IllegalArgumentException.class, () -> ConfigTransferCodec.decode(
                body.compression(), body.rawBytes() - 1, body.encodedBytes(), encoded));
    }

    @Test
    void assemblerSupportsOutOfOrderButRejectsDuplicateAndMixedMetadata() {
        final String json = pseudoRandomAscii(90_000);
        final ConfigTransferCodec.EncodedBody body = ConfigTransferCodec.encode(json);
        final ArrayList<Integer> order = new ArrayList<>();
        for (int index = 0; index < body.chunks().size(); index++) {
            order.add(index);
        }
        Collections.reverse(order);

        final ConfigTransferAssembler<String> assembler = new ConfigTransferAssembler<>();
        Optional<ConfigTransferAssembler.Complete<String>> complete = Optional.empty();
        for (int index : order) {
            complete = assembler.accept(
                    "metadata",
                    body.compression(),
                    body.rawBytes(),
                    body.encodedBytes(),
                    index,
                    body.chunks().size(),
                    body.chunks().get(index),
                    1L);
        }
        assertTrue(complete.isPresent());
        assertEquals(json, complete.orElseThrow().decode());

        assembler.accept(
                "metadata", body.compression(), body.rawBytes(), body.encodedBytes(),
                0, body.chunks().size(), body.chunks().get(0), 2L);
        assertThrows(IllegalArgumentException.class, () -> assembler.accept(
                "metadata", body.compression(), body.rawBytes(), body.encodedBytes(),
                0, body.chunks().size(), body.chunks().get(0), 2L));
        assertFalse(assembler.active());

        assembler.accept(
                "metadata", body.compression(), body.rawBytes(), body.encodedBytes(),
                0, body.chunks().size(), body.chunks().get(0), 3L);
        assertThrows(IllegalArgumentException.class, () -> assembler.accept(
                "different", body.compression(), body.rawBytes(), body.encodedBytes(),
                1, body.chunks().size(), body.chunks().get(1), 3L));
        assertFalse(assembler.active());
    }

    @Test
    void assemblerExpiresAndRejectsNonCanonicalFramesWithoutRetainingBytes() {
        final ConfigTransferCodec.EncodedBody body = ConfigTransferCodec.encode(
                pseudoRandomAscii(90_000));
        final ConfigTransferAssembler<String> assembler = new ConfigTransferAssembler<>();
        assembler.accept(
                "metadata", body.compression(), body.rawBytes(), body.encodedBytes(),
                0, body.chunks().size(), body.chunks().get(0), 10L);
        assertTrue(assembler.bufferedBytes() > 0);
        assembler.expire(411L, 400L);
        assertFalse(assembler.active());
        assertEquals(0, assembler.bufferedBytes());

        assertThrows(IllegalArgumentException.class, () -> assembler.accept(
                "metadata", body.compression(), body.rawBytes(), body.encodedBytes(),
                -1, body.chunks().size(), body.chunks().get(0), 500L));
        assertFalse(assembler.active());
    }

    @Test
    void completedBytesAreDefensivelyCopied() {
        final ConfigTransferCodec.EncodedBody body = ConfigTransferCodec.encode("abcdef");
        final byte[] source = body.chunks().get(0).clone();
        final ConfigTransferAssembler<String> assembler = new ConfigTransferAssembler<>();
        final ConfigTransferAssembler.Complete<String> complete = assembler.accept(
                "metadata", body.compression(), body.rawBytes(), body.encodedBytes(),
                0, 1, source, 1L).orElseThrow();
        source[0] ^= 1;
        assertArrayEquals(join(body.chunks()), complete.encoded());
        final byte[] returned = complete.encoded();
        returned[0] ^= 1;
        assertArrayEquals(join(body.chunks()), complete.encoded());
    }

    private static int serializedBytes(final ConfigPayload payload) {
        final FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            payload.write(buffer);
            return buffer.readableBytes();
        } finally {
            buffer.release();
        }
    }

    private static String decode(final ConfigTransferCodec.EncodedBody body) {
        return ConfigTransferCodec.decode(
                body.compression(), body.rawBytes(), body.encodedBytes(), join(body.chunks()));
    }

    private static byte[] join(final List<byte[]> chunks) {
        final int size = chunks.stream().mapToInt(chunk -> chunk.length).sum();
        final byte[] joined = new byte[size];
        int offset = 0;
        for (byte[] chunk : chunks) {
            System.arraycopy(chunk, 0, joined, offset, chunk.length);
            offset += chunk.length;
        }
        return joined;
    }

    private static String pseudoRandomAscii(final int length) {
        final Random random = new Random(0x5a17c0deL);
        final String alphabet = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_";
        final StringBuilder value = new StringBuilder(length);
        while (value.length() < length) {
            value.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return value.toString();
    }
}
