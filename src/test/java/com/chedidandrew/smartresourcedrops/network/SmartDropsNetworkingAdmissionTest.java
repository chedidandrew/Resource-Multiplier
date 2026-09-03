package com.chedidandrew.smartresourcedrops.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

final class SmartDropsNetworkingAdmissionTest {
    private static final int CHUNK_BYTES = ConfigTransferCodec.CHUNK_BYTES;

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @BeforeEach
    void installSilentTransport() {
        SmartDropsNetworking.serverStopped(null);
        SmartDropsNetworking.initialize(new SmartDropsNetworking.Transport() {
            @Override
            public boolean canSend(final ServerPlayer player, final ResourceLocation type) {
                return false;
            }

            @Override
            public void send(final ServerPlayer player, final ConfigPayload payload) {
                throw new AssertionError("Silent admission transport must never send");
            }
        });
    }

    @AfterEach
    void clearNetworkState() {
        SmartDropsNetworking.serverStopped(null);
    }

    @Test
    void unauthorizedFragmentAllocatesNoTransferState() {
        final Peer peer = peer(false, 10L);
        SmartDropsNetworking.handlePatchFragment(incompleteFrame(1, 0), peer.player());
        assertEquals(new SmartDropsNetworking.TransferDiagnostics(0, 0, 0, List.of()),
                SmartDropsNetworking.transferDiagnostics());
    }

    @Test
    void combinedSlotCeilingRejectsThirtyThirdPeer() {
        final Peer deferred = peer(true, 10L);
        SmartDropsNetworking.handlePatchFragment(malformedCompressedFrame(1000), deferred.player());
        SmartDropsNetworking.handlePatchFragment(malformedCompressedFrame(1001), deferred.player());
        final List<Peer> peers = new ArrayList<>();
        for (int index = 0; index < 32; index++) {
            final Peer peer = peer(true, 10L);
            peers.add(peer);
            SmartDropsNetworking.handlePatchFragment(
                    incompleteFrame(index + 1, 0),
                    peer.player());
        }
        final SmartDropsNetworking.TransferDiagnostics state =
                SmartDropsNetworking.transferDiagnostics();
        assertEquals(31, state.activeTransfers());
        assertEquals(1, state.pendingDecodes());
        assertEquals(31 * CHUNK_BYTES + 1, state.bufferedBytes());
        assertEquals(List.of(1001), state.pendingRequestIds());
    }

    @Test
    void aggregateByteCeilingDropsTheTransferThatWouldCrossIt() {
        final Peer deferred = peer(true, 10L);
        SmartDropsNetworking.handlePatchFragment(malformedCompressedFrame(1000), deferred.player());
        for (int chunkIndex = 0; chunkIndex < ConfigTransferCodec.MAX_CHUNKS; chunkIndex++) {
            SmartDropsNetworking.handlePatchFragment(maxFrame(1001, chunkIndex), deferred.player());
        }
        assertEquals(ConfigTransferCodec.RAW_UTF8_MAX,
                SmartDropsNetworking.transferDiagnostics().bufferedBytes());

        final List<Peer> peers = new ArrayList<>();
        for (int peerIndex = 0; peerIndex < 15; peerIndex++) {
            final Peer peer = peer(true, 10L);
            peers.add(peer);
            for (int chunkIndex = 0; chunkIndex < 34; chunkIndex++) {
                SmartDropsNetworking.handlePatchFragment(
                        maxFrame(peerIndex + 1, chunkIndex),
                        peer.player());
            }
        }
        final int retainedBeforeRejection = ConfigTransferCodec.RAW_UTF8_MAX
                + 15 * 34 * CHUNK_BYTES;
        assertEquals(retainedBeforeRejection,
                SmartDropsNetworking.transferDiagnostics().bufferedBytes());

        final Peer rejected = peer(true, 10L);
        SmartDropsNetworking.handlePatchFragment(maxFrame(100, 0), rejected.player());
        SmartDropsNetworking.handlePatchFragment(maxFrame(100, 1), rejected.player());
        SmartDropsNetworking.handlePatchFragment(maxFrame(100, 2), rejected.player());

        final SmartDropsNetworking.TransferDiagnostics state =
                SmartDropsNetworking.transferDiagnostics();
        assertEquals(15, state.activeTransfers());
        assertEquals(1, state.pendingDecodes());
        assertEquals(List.of(1001), state.pendingRequestIds());
        assertEquals(retainedBeforeRejection, state.bufferedBytes());
        assertTrue(state.bufferedBytes() <= 16 * 1024 * 1024);
    }

    @Test
    void deferredDecodeIsBoundedReplacesNewestAndCleansOnDisconnectAndStop() {
        final Peer peer = peer(true, 10L);
        SmartDropsNetworking.handlePatchFragment(malformedCompressedFrame(1), peer.player());
        SmartDropsNetworking.handlePatchFragment(malformedCompressedFrame(2), peer.player());
        SmartDropsNetworking.handlePatchFragment(malformedCompressedFrame(3), peer.player());

        SmartDropsNetworking.TransferDiagnostics state =
                SmartDropsNetworking.transferDiagnostics();
        assertEquals(0, state.activeTransfers());
        assertEquals(1, state.pendingDecodes());
        assertEquals(1, state.bufferedBytes());
        assertEquals(List.of(3), state.pendingRequestIds());

        peer.disconnected().set(true);
        SmartDropsNetworking.serverTick();
        assertEquals(new SmartDropsNetworking.TransferDiagnostics(0, 0, 0, List.of()),
                SmartDropsNetworking.transferDiagnostics());

        final Peer active = peer(true, 20L);
        SmartDropsNetworking.handlePatchFragment(incompleteFrame(10, 0), active.player());
        assertEquals(1, SmartDropsNetworking.transferDiagnostics().activeTransfers());
        SmartDropsNetworking.serverStopped(null);
        assertEquals(new SmartDropsNetworking.TransferDiagnostics(0, 0, 0, List.of()),
                SmartDropsNetworking.transferDiagnostics());
    }

    @Test
    void demotionBeforeDeferredDecodeDropsWorkWithoutInflating() {
        final Peer peer = peer(true, 10L);
        SmartDropsNetworking.handlePatchFragment(malformedCompressedFrame(1), peer.player());
        SmartDropsNetworking.handlePatchFragment(malformedCompressedFrame(2), peer.player());
        assertEquals(1, SmartDropsNetworking.transferDiagnostics().pendingDecodes());

        peer.authorized().set(false);
        peer.tick().set(30L);
        SmartDropsNetworking.serverTick();
        assertEquals(new SmartDropsNetworking.TransferDiagnostics(0, 0, 0, List.of()),
                SmartDropsNetworking.transferDiagnostics());
    }

    private static ConfigPatchFragmentPayload incompleteFrame(
            final int requestId,
            final int chunkIndex
    ) {
        return new ConfigPatchFragmentPayload(
                ConfigTransferCodec.FORMAT,
                requestId,
                0L,
                ConfigTransferCodec.Compression.NONE,
                CHUNK_BYTES + 1,
                CHUNK_BYTES + 1,
                chunkIndex,
                2,
                new byte[chunkIndex == 0 ? CHUNK_BYTES : 1]);
    }

    private static ConfigPatchFragmentPayload maxFrame(
            final int requestId,
            final int chunkIndex
    ) {
        return new ConfigPatchFragmentPayload(
                ConfigTransferCodec.FORMAT,
                requestId,
                0L,
                ConfigTransferCodec.Compression.NONE,
                ConfigTransferCodec.RAW_UTF8_MAX,
                ConfigTransferCodec.RAW_UTF8_MAX,
                chunkIndex,
                ConfigTransferCodec.MAX_CHUNKS,
                new byte[chunkIndex == ConfigTransferCodec.MAX_CHUNKS - 1 ? 4096 : CHUNK_BYTES]);
    }

    private static ConfigPatchFragmentPayload malformedCompressedFrame(final int requestId) {
        return new ConfigPatchFragmentPayload(
                ConfigTransferCodec.FORMAT,
                requestId,
                0L,
                ConfigTransferCodec.Compression.ZLIB,
                ConfigTransferCodec.RAW_UTF8_MAX,
                1,
                0,
                1,
                new byte[] {0});
    }

    private static Peer peer(final boolean initiallyAuthorized, final long initialTick) {
        final AtomicBoolean authorized = new AtomicBoolean(initiallyAuthorized);
        final AtomicBoolean disconnected = new AtomicBoolean();
        final AtomicLong tick = new AtomicLong(initialTick);
        final MinecraftServer server = mock(MinecraftServer.class);
        final ServerLevel level = mock(ServerLevel.class);
        final ServerPlayer player = mock(ServerPlayer.class);
        when(level.getServer()).thenReturn(server);
        when(level.getGameTime()).thenAnswer(invocation -> tick.get());
        when(player.level()).thenReturn(level);
        when(player.hasPermissions(2)).thenAnswer(invocation -> authorized.get());
        when(player.hasDisconnected()).thenAnswer(invocation -> disconnected.get());
        when(player.isRemoved()).thenAnswer(invocation -> disconnected.get());
        when(player.getScoreboardName()).thenReturn("wire-admission-test");
        return new Peer(player, authorized, disconnected, tick);
    }

    private record Peer(
            ServerPlayer player,
            AtomicBoolean authorized,
            AtomicBoolean disconnected,
            AtomicLong tick
    ) {
    }
}
