package com.chedidandrew.smartresourcedrops.client;

import java.util.Optional;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.config.ConfigRequestLifecycle;
import com.chedidandrew.smartresourcedrops.network.ConfigSnapshotFragmentPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigSnapshotPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigTransferAssembler;

import net.minecraft.client.Minecraft;

/** Bounded client-side assembly for authoritative snapshot fragments. */
public final class ClientConfigTransferState {
    private static final long TRANSFER_TIMEOUT_TICKS = 400L;

    private static ConfigTransferAssembler<SnapshotMetadata> assembler = new ConfigTransferAssembler<>();
    private static Object connection;
    private static int requestId = -1;
    private static long tick;

    private ClientConfigTransferState() {
    }

    public static void begin(final int nextRequestId, final Object nextConnection) {
        assembler.clear();
        requestId = nextRequestId;
        connection = nextConnection;
    }

    public static void accept(
            final ConfigSnapshotFragmentPayload payload,
            final Minecraft minecraft
    ) {
        final Object currentConnection = ClientConfigState.connectionIdentity(minecraft);
        if (currentConnection == null
                || currentConnection != connection
                || payload.requestId() != requestId
                || !ClientConfigState.isCurrent(payload.requestId(), minecraft)) {
            SmartResourceDrops.LOGGER.debug(
                    "Ignored stale config snapshot fragment #{}",
                    payload.requestId());
            return;
        }

        final Optional<ConfigTransferAssembler.Complete<SnapshotMetadata>> complete;
        try {
            complete = assembler.accept(
                    new SnapshotMetadata(
                            payload.requestId(),
                            payload.revision(),
                            payload.editable(),
                            payload.patchResult()),
                    payload.compression(),
                    payload.rawBytes(),
                    payload.encodedBytes(),
                    payload.chunkIndex(),
                    payload.chunkCount(),
                    payload.chunk(),
                    tick);
        } catch (IllegalArgumentException exception) {
            rejectCurrent(minecraft, payload.requestId(), exception.getMessage());
            return;
        }
        if (complete.isEmpty()) {
            return;
        }

        final ConfigTransferAssembler.Complete<SnapshotMetadata> assembled = complete.get();
        final String json;
        try {
            json = assembled.decode();
        } catch (IllegalArgumentException exception) {
            rejectCurrent(minecraft, payload.requestId(), exception.getMessage());
            return;
        }
        final SnapshotMetadata metadata = assembled.metadata();
        clear();
        SmartResourceDrops.LOGGER.debug(
                "Received complete config snapshot #{} on the client",
                metadata.requestId());
        ClientConfigState.accept(new ConfigSnapshotPayload(
                metadata.requestId(),
                metadata.revision(),
                json,
                metadata.editable(),
                metadata.patchResult()), minecraft);
    }

    public static void tick(final Minecraft minecraft) {
        tick++;
        final boolean wasActive = assembler.active();
        assembler.expire(tick, TRANSFER_TIMEOUT_TICKS);
        if (wasActive && !assembler.active() && requestId >= 0) {
            final int expiredRequest = requestId;
            clear();
            ClientConfigState.failRequest(
                    expiredRequest,
                    minecraft,
                    ConfigRequestLifecycle.Failure.INVALID_RESPONSE);
        }
    }

    public static void clear() {
        assembler.clear();
        connection = null;
        requestId = -1;
    }

    private static void rejectCurrent(
            final Minecraft minecraft,
            final int rejectedRequestId,
            final String reason
    ) {
        SmartResourceDrops.LOGGER.warn(
                "Rejected malformed config snapshot transfer #{}: {}",
                rejectedRequestId,
                reason);
        clear();
        ClientConfigState.failRequest(
                rejectedRequestId,
                minecraft,
                ConfigRequestLifecycle.Failure.INVALID_RESPONSE);
    }

    private record SnapshotMetadata(
            int requestId,
            long revision,
            boolean editable,
            ConfigSnapshotPayload.PatchResult patchResult
    ) {
    }
}
