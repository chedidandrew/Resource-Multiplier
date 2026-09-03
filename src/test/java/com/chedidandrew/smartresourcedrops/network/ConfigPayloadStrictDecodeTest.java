package com.chedidandrew.smartresourcedrops.network;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

final class ConfigPayloadStrictDecodeTest {
    @Test
    void compactPayloadsRejectTrailingBytes() {
        assertTrailingRejected(new ConfigRequestPayload(1), ConfigRequestPayload::read);
        assertTrailingRejected(new ConfigResetPayload(1, 0L), ConfigResetPayload::read);
        assertTrailingRejected(
                new ConfigInvalidationPayload(1L, ConfigInvalidationPayload.ChangeKind.UPDATE),
                ConfigInvalidationPayload::read);
        assertTrailingRejected(
                new ConfigMutationResultPayload(
                        1, 0L, true, ConfigSnapshotPayload.PatchResult.REJECTED),
                ConfigMutationResultPayload::read);
    }

    @Test
    void compactAndFragmentPayloadsRejectUnknownEnums() {
        final FriendlyByteBuf invalidation = buffer();
        try {
            invalidation.writeVarLong(1L);
            invalidation.writeVarInt(99);
            assertThrows(IllegalArgumentException.class, () -> ConfigInvalidationPayload.read(invalidation));
        } finally {
            invalidation.release();
        }

        final FriendlyByteBuf mutation = buffer();
        try {
            mutation.writeVarInt(1);
            mutation.writeVarLong(1L);
            mutation.writeBoolean(true);
            mutation.writeVarInt(99);
            assertThrows(IllegalArgumentException.class, () -> ConfigMutationResultPayload.read(mutation));
        } finally {
            mutation.release();
        }

        final FriendlyByteBuf snapshot = buffer();
        try {
            snapshot.writeVarInt(ConfigTransferCodec.FORMAT);
            snapshot.writeVarInt(1);
            snapshot.writeVarLong(1L);
            snapshot.writeBoolean(true);
            snapshot.writeVarInt(99);
            snapshot.writeVarInt(ConfigTransferCodec.Compression.NONE.ordinal());
            snapshot.writeVarInt(1);
            snapshot.writeVarInt(1);
            snapshot.writeVarInt(0);
            snapshot.writeVarInt(1);
            snapshot.writeByteArray(new byte[] {'{'});
            assertThrows(IllegalArgumentException.class, () -> ConfigSnapshotFragmentPayload.read(snapshot));
        } finally {
            snapshot.release();
        }
    }

    @Test
    void fragmentPayloadsRejectTrailingBytes() {
        final ConfigPatchFragmentPayload patch = ConfigPatchFragmentPayload.encode(
                new ConfigPatchPayload(1, 0L, "{}"))
                .get(0);
        assertTrailingRejected(patch, ConfigPatchFragmentPayload::read);

        final ConfigSnapshotFragmentPayload snapshot = ConfigSnapshotFragmentPayload.encode(
                new ConfigSnapshotPayload(
                        1,
                        1L,
                        "{}",
                        true,
                        ConfigSnapshotPayload.PatchResult.NONE))
                .get(0);
        assertTrailingRejected(snapshot, ConfigSnapshotFragmentPayload::read);
    }

    private static <T> void assertTrailingRejected(
            final ConfigPayload payload,
            final Decoder<T> decoder
    ) {
        final FriendlyByteBuf buffer = buffer();
        try {
            payload.write(buffer);
            buffer.writeByte(0x5a);
            assertThrows(IllegalArgumentException.class, () -> decoder.read(buffer));
        } finally {
            buffer.release();
        }
    }

    private static FriendlyByteBuf buffer() {
        return new FriendlyByteBuf(Unpooled.buffer());
    }

    @FunctionalInterface
    private interface Decoder<T> {
        T read(FriendlyByteBuf buffer);
    }
}
