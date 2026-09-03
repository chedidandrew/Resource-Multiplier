package com.chedidandrew.smartresourcedrops.network;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.config.ConfigManager;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class SmartDropsNetworking {
    private static final long REQUEST_COOLDOWN_TICKS = 40L;
    private static final long PATCH_COOLDOWN_TICKS = 20L;
    private static final long PATCH_DECODE_COOLDOWN_TICKS = 20L;
    private static final long RESET_COOLDOWN_TICKS = 40L;
    private static final long TRANSFER_TIMEOUT_TICKS = 400L;
    private static final int MAX_ACTIVE_PATCH_TRANSFERS = 32;
    private static final int MAX_BUFFERED_PATCH_BYTES = 16 * 1024 * 1024;
    private static final int MAX_PENDING_PATCH_DECODES = 32;
    private static final Map<ServerPlayer, Long> LAST_REQUEST_TICK = new WeakHashMap<>();
    private static final Map<ServerPlayer, Long> LAST_PATCH_TICK = new WeakHashMap<>();
    private static final Map<ServerPlayer, Long> LAST_PATCH_DECODE_TICK = new WeakHashMap<>();
    private static final Map<ServerPlayer, Long> LAST_RESET_TICK = new WeakHashMap<>();
    private static final Map<ServerPlayer, PendingRequest> PENDING_REQUESTS = new WeakHashMap<>();
    private static final Map<ServerPlayer, PendingPatch> PENDING_PATCHES = new WeakHashMap<>();
    private static final Map<ServerPlayer, ConfigTransferAssembler<PatchMetadata>> PATCH_TRANSFERS =
            new WeakHashMap<>();
    private static final Map<ServerPlayer, PendingPatchDecode> PENDING_PATCH_DECODES =
            new WeakHashMap<>();
    private static final ThreadLocal<UUID> PUBLICATION_ACKNOWLEDGED_PLAYER = new ThreadLocal<>();
    private static volatile MinecraftServer activeServer;
    private static volatile Transport transport;

    private SmartDropsNetworking() {
    }

    public static void initialize(final Transport installedTransport) {
        transport = Objects.requireNonNull(installedTransport, "installedTransport");
        ConfigManager.setPublicationListener(SmartDropsNetworking::onConfigPublished);
    }

    public static void serverStarted(final MinecraftServer server) {
        activeServer = server;
    }

    public static void serverStopped(final MinecraftServer server) {
        if (activeServer == server) {
            activeServer = null;
        }
        synchronized (LAST_REQUEST_TICK) {
            LAST_REQUEST_TICK.clear();
            PENDING_REQUESTS.clear();
        }
        synchronized (LAST_PATCH_TICK) {
            LAST_PATCH_TICK.clear();
            PENDING_PATCHES.clear();
            PATCH_TRANSFERS.clear();
        }
        synchronized (LAST_PATCH_DECODE_TICK) {
            LAST_PATCH_DECODE_TICK.clear();
            PENDING_PATCH_DECODES.clear();
        }
        synchronized (LAST_RESET_TICK) {
            LAST_RESET_TICK.clear();
        }
    }

    public static void serverTick() {
        expirePatchTransfers();
        flushPendingPatchDecodes();
        flushPendingRequests();
        flushPendingPatches();
    }

    public static void handlePatchFragment(
            final ConfigPatchFragmentPayload payload,
            final ServerPlayer player
    ) {
        if (!canEditConfiguration(player)) {
            sendMutationResult(
                    player,
                    payload.requestId(),
                    ConfigSnapshotPayload.PatchResult.UNAUTHORIZED);
            return;
        }
        final long now = player.level().getGameTime();
        final ConfigTransferAssembler.Complete<PatchMetadata> complete;
        synchronized (LAST_PATCH_TICK) {
            ConfigTransferAssembler<PatchMetadata> assembler = PATCH_TRANSFERS.get(player);
            if (assembler == null) {
                if (PATCH_TRANSFERS.size() + pendingPatchDecodeCount()
                        >= MAX_ACTIVE_PATCH_TRANSFERS) {
                    return;
                }
                assembler = new ConfigTransferAssembler<>();
                PATCH_TRANSFERS.put(player, assembler);
            }
            if (!assembler.hasChunk(payload.chunkIndex())
                    && bufferedPatchBytes() + pendingPatchDecodeBytes()
                    + payload.chunk().length > MAX_BUFFERED_PATCH_BYTES) {
                PATCH_TRANSFERS.remove(player);
                return;
            }
            try {
                final Optional<ConfigTransferAssembler.Complete<PatchMetadata>> assembled = assembler.accept(
                        new PatchMetadata(payload.requestId(), payload.expectedRevision()),
                        payload.compression(),
                        payload.rawBytes(),
                        payload.encodedBytes(),
                        payload.chunkIndex(),
                        payload.chunkCount(),
                        payload.chunk(),
                        now);
                if (assembled.isEmpty()) {
                    return;
                }
                complete = assembled.get();
                PATCH_TRANSFERS.remove(player);
            } catch (IllegalArgumentException exception) {
                PATCH_TRANSFERS.remove(player);
                SmartResourceDrops.LOGGER.warn(
                        "Rejected malformed config patch transfer from {}: {}",
                        player.getScoreboardName(),
                        exception.getMessage());
                return;
            }
        }
        final DecodeAdmission admission = admitCompletedPatchDecode(player, complete, now);
        if (admission != DecodeAdmission.ADMIT) {
            if (admission == DecodeAdmission.REJECT) {
                sendMutationResult(
                        player,
                        payload.requestId(),
                        ConfigSnapshotPayload.PatchResult.REJECTED);
            }
            return;
        }
        decodeAndHandlePatch(player, complete);
    }

    private static void decodeAndHandlePatch(
            final ServerPlayer player,
            final ConfigTransferAssembler.Complete<PatchMetadata> complete
    ) {
        if (!canEditConfiguration(player)) {
            sendMutationResult(
                    player,
                    complete.metadata().requestId(),
                    ConfigSnapshotPayload.PatchResult.UNAUTHORIZED);
            return;
        }
        final String json;
        try {
            json = complete.decode();
        } catch (IllegalArgumentException exception) {
            SmartResourceDrops.LOGGER.warn(
                    "Rejected malformed config patch body from {}: {}",
                    player.getScoreboardName(),
                    exception.getMessage());
            return;
        }
        handlePatch(new ConfigPatchPayload(
                complete.metadata().requestId(),
                complete.metadata().expectedRevision(),
                json), player);
    }

    /**
     * Limits expensive UTF-8/inflate work without rate-limiting chunks. One
     * newest complete transfer is deferred per peer so rapid GUI edits retain
     * the existing coalescing behavior without creating an inflate loop.
     */
    private static DecodeAdmission admitCompletedPatchDecode(
            final ServerPlayer player,
            final ConfigTransferAssembler.Complete<PatchMetadata> complete,
            final long now
    ) {
        synchronized (LAST_PATCH_DECODE_TICK) {
            final Long previous = LAST_PATCH_DECODE_TICK.get(player);
            if (previous != null && now >= previous && now - previous < PATCH_DECODE_COOLDOWN_TICKS) {
                final PendingPatchDecode existing = PENDING_PATCH_DECODES.get(player);
                final int retainedBytes = pendingPatchDecodeBytes()
                        - (existing == null ? 0 : existing.complete().encodedBytes());
                if ((existing == null
                        && PATCH_TRANSFERS.size() + PENDING_PATCH_DECODES.size()
                        >= MAX_PENDING_PATCH_DECODES)
                        || retainedBytes + complete.encodedBytes() > MAX_BUFFERED_PATCH_BYTES) {
                    return DecodeAdmission.REJECT;
                }
                PENDING_PATCH_DECODES.put(
                        player,
                        new PendingPatchDecode(
                                complete,
                                previous + PATCH_DECODE_COOLDOWN_TICKS));
                return DecodeAdmission.QUEUED;
            }
            PENDING_PATCH_DECODES.remove(player);
            LAST_PATCH_DECODE_TICK.put(player, now);
            return DecodeAdmission.ADMIT;
        }
    }

    private static void flushPendingPatchDecodes() {
        final List<ReadyPatchDecode> ready = new ArrayList<>();
        synchronized (LAST_PATCH_DECODE_TICK) {
            final Iterator<Map.Entry<ServerPlayer, PendingPatchDecode>> iterator =
                    PENDING_PATCH_DECODES.entrySet().iterator();
            while (iterator.hasNext()) {
                final Map.Entry<ServerPlayer, PendingPatchDecode> entry = iterator.next();
                final ServerPlayer player = entry.getKey();
                if (player == null || player.hasDisconnected() || player.isRemoved()) {
                    LAST_PATCH_DECODE_TICK.remove(player);
                    iterator.remove();
                    continue;
                }
                final long now = player.level().getGameTime();
                final PendingPatchDecode pending = entry.getValue();
                if (now < pending.eligibleTick()) {
                    continue;
                }
                LAST_PATCH_DECODE_TICK.put(player, now);
                ready.add(new ReadyPatchDecode(player, pending.complete()));
                iterator.remove();
            }
        }
        ready.forEach(patch -> decodeAndHandlePatch(patch.player(), patch.complete()));
    }

    private static int pendingPatchDecodeBytes() {
        int total = 0;
        for (PendingPatchDecode pending : PENDING_PATCH_DECODES.values()) {
            total = Math.addExact(total, pending.complete().encodedBytes());
        }
        return total;
    }

    private static int pendingPatchDecodeCount() {
        synchronized (LAST_PATCH_DECODE_TICK) {
            return PENDING_PATCH_DECODES.size();
        }
    }

    public static void handleRequest(final ConfigRequestPayload payload, final ServerPlayer player) {
        SmartResourceDrops.LOGGER.debug(
                "Received config request #{} from {}",
                payload.requestId(),
                player.getScoreboardName());
        final long now = player.level().getGameTime();
        synchronized (LAST_REQUEST_TICK) {
            final Long previous = LAST_REQUEST_TICK.get(player);
            if (previous != null && now >= previous && now - previous < REQUEST_COOLDOWN_TICKS) {
                PENDING_REQUESTS.put(
                        player,
                        new PendingRequest(payload.requestId(), previous + REQUEST_COOLDOWN_TICKS));
                return;
            }
            LAST_REQUEST_TICK.put(player, now);
        }
        sendSnapshot(player, payload.requestId());
    }

    public static void handlePatch(final ConfigPatchPayload payload, final ServerPlayer player) {
        final boolean editableAtReceipt = canEditConfiguration(player);
        SmartResourceDrops.LOGGER.debug(
                "Received config patch #{} from {} (editable={})",
                payload.requestId(),
                player.getScoreboardName(),
                editableAtReceipt);
        if (acceptOrQueuePatch(player, payload, editableAtReceipt)) {
            applyPatch(player, payload);
        }
    }

    public static void handleReset(final ConfigResetPayload payload, final ServerPlayer player) {
        final boolean editableAtReceipt = canEditConfiguration(player);
        SmartResourceDrops.LOGGER.debug(
                "Received config reset #{} from {} at revision {} (editable={})",
                payload.requestId(),
                player.getScoreboardName(),
                payload.expectedRevision(),
                editableAtReceipt);
        if (!editableAtReceipt) {
            sendMutationResult(
                    player,
                    payload.requestId(),
                    ConfigSnapshotPayload.PatchResult.RESET_UNAUTHORIZED);
            return;
        }
        if (payload.expectedRevision() != ConfigManager.revision()) {
            sendMutationResult(
                    player,
                    payload.requestId(),
                    ConfigSnapshotPayload.PatchResult.RESET_REJECTED);
            return;
        }
        if (!acceptReset(player)) {
            sendMutationResult(
                    player,
                    payload.requestId(),
                    ConfigSnapshotPayload.PatchResult.RESET_REJECTED);
            return;
        }
        applyReset(player, payload);
    }

    private static void flushPendingRequests() {
        final List<ReadyRequest> ready = new ArrayList<>();
        synchronized (LAST_REQUEST_TICK) {
            final Iterator<Map.Entry<ServerPlayer, PendingRequest>> iterator = PENDING_REQUESTS.entrySet().iterator();
            while (iterator.hasNext()) {
                final Map.Entry<ServerPlayer, PendingRequest> entry = iterator.next();
                final ServerPlayer player = entry.getKey();
                if (player == null || player.hasDisconnected() || player.isRemoved()) {
                    iterator.remove();
                    continue;
                }

                final long now = player.level().getGameTime();
                final PendingRequest pending = entry.getValue();
                if (now < pending.eligibleTick()) {
                    continue;
                }
                LAST_REQUEST_TICK.put(player, now);
                ready.add(new ReadyRequest(player, pending.requestId()));
                iterator.remove();
            }
        }
        ready.forEach(request -> sendSnapshot(request.player(), request.requestId()));
    }

    private static void expirePatchTransfers() {
        synchronized (LAST_PATCH_TICK) {
            final Iterator<Map.Entry<ServerPlayer, ConfigTransferAssembler<PatchMetadata>>> iterator =
                    PATCH_TRANSFERS.entrySet().iterator();
            while (iterator.hasNext()) {
                final Map.Entry<ServerPlayer, ConfigTransferAssembler<PatchMetadata>> entry = iterator.next();
                final ServerPlayer player = entry.getKey();
                if (player == null || player.hasDisconnected() || player.isRemoved()) {
                    iterator.remove();
                    continue;
                }
                entry.getValue().expire(player.level().getGameTime(), TRANSFER_TIMEOUT_TICKS);
                if (!entry.getValue().active()) {
                    iterator.remove();
                }
            }
        }
    }

    private static int bufferedPatchBytes() {
        int total = 0;
        for (ConfigTransferAssembler<PatchMetadata> assembler : PATCH_TRANSFERS.values()) {
            total = Math.addExact(total, assembler.bufferedBytes());
        }
        return total;
    }

    private static boolean acceptOrQueuePatch(
            final ServerPlayer player,
            final ConfigPatchPayload payload,
            final boolean editableAtReceipt
    ) {
        final long now = player.level().getGameTime();
        synchronized (LAST_PATCH_TICK) {
            final Long previous = LAST_PATCH_TICK.get(player);
            if (previous != null && now >= previous && now - previous < PATCH_COOLDOWN_TICKS) {
                if (editableAtReceipt) {
                    PENDING_PATCHES.put(
                            player,
                            new PendingPatch(payload, previous + PATCH_COOLDOWN_TICKS));
                    SmartResourceDrops.LOGGER.debug(
                            "Queued rate-limited config patch #{} from {}",
                            payload.requestId(),
                            player.getScoreboardName());
                } else {
                    PENDING_PATCHES.remove(player);
                    sendMutationResult(
                            player,
                            payload.requestId(),
                            ConfigSnapshotPayload.PatchResult.UNAUTHORIZED);
                }
                return false;
            }
            PENDING_PATCHES.remove(player);
            LAST_PATCH_TICK.put(player, now);
            return true;
        }
    }

    private static void flushPendingPatches() {
        final List<ReadyPatch> ready = new ArrayList<>();
        synchronized (LAST_PATCH_TICK) {
            final Iterator<Map.Entry<ServerPlayer, PendingPatch>> iterator = PENDING_PATCHES.entrySet().iterator();
            while (iterator.hasNext()) {
                final Map.Entry<ServerPlayer, PendingPatch> entry = iterator.next();
                final ServerPlayer player = entry.getKey();
                if (player == null || player.hasDisconnected() || player.isRemoved()) {
                    iterator.remove();
                    continue;
                }

                final long now = player.level().getGameTime();
                final PendingPatch pending = entry.getValue();
                if (now < pending.eligibleTick()) {
                    continue;
                }
                LAST_PATCH_TICK.put(player, now);
                ready.add(new ReadyPatch(player, pending.payload()));
                iterator.remove();
            }
        }
        ready.forEach(patch -> applyPatch(patch.player(), patch.payload()));
    }

    private static void applyPatch(final ServerPlayer player, final ConfigPatchPayload payload) {
        final boolean editable = canEditConfiguration(player);
        if (!editable) {
            sendMutationResult(player, payload.requestId(), ConfigSnapshotPayload.PatchResult.UNAUTHORIZED);
            return;
        }
        final boolean applied = publishForPlayer(player, () -> ConfigManager.applyClientPatch(
                payload.json(),
                payload.expectedRevision()));
        if (applied) {
            sendSnapshot(player, payload.requestId(), ConfigSnapshotPayload.PatchResult.APPLIED);
        } else {
            sendMutationResult(player, payload.requestId(), ConfigSnapshotPayload.PatchResult.REJECTED);
        }
    }

    private static void applyReset(final ServerPlayer player, final ConfigResetPayload payload) {
        if (!canEditConfiguration(player)) {
            sendMutationResult(
                    player,
                    payload.requestId(),
                    ConfigSnapshotPayload.PatchResult.RESET_UNAUTHORIZED);
            return;
        }

        final boolean reset = publishForPlayer(
                player,
                () -> ConfigManager.reset(payload.expectedRevision()));
        if (reset) {
            clearPendingPatches();
            sendSnapshot(player, payload.requestId(), ConfigSnapshotPayload.PatchResult.RESET_APPLIED);
        } else {
            sendMutationResult(
                    player,
                    payload.requestId(),
                    ConfigSnapshotPayload.PatchResult.RESET_REJECTED);
        }
    }

    /** A full reset supersedes every operator patch queued against the previous configuration. */
    static void clearPendingPatches() {
        synchronized (LAST_PATCH_TICK) {
            PENDING_PATCHES.clear();
            PATCH_TRANSFERS.clear();
        }
        synchronized (LAST_PATCH_DECODE_TICK) {
            PENDING_PATCH_DECODES.clear();
        }
    }

    /**
     * Completes a successful command/console reset by dropping queued patches. The
     * ConfigManager publication listener has already scheduled typed editor invalidation.
     */
    public static void afterAuthoritativeReset(final MinecraftServer server) {
        clearPendingPatches();
    }

    /** Destructive resets are never queued; repeated requests are rejected during a short cooldown. */
    private static boolean acceptReset(final ServerPlayer player) {
        final long now = player.level().getGameTime();
        synchronized (LAST_RESET_TICK) {
            final Long previous = LAST_RESET_TICK.get(player);
            if (previous != null && now >= previous && now - previous < RESET_COOLDOWN_TICKS) {
                return false;
            }
            LAST_RESET_TICK.put(player, now);
            return true;
        }
    }

    /** Small explicit failure response; rejected mutations never reflect a full config snapshot. */
    private static void sendMutationResult(
            final ServerPlayer player,
            final int requestId,
            final ConfigSnapshotPayload.PatchResult result
    ) {
        if (!transport().canSend(player, ConfigMutationResultPayload.TYPE)) {
            sendSnapshot(player, requestId, result);
            return;
        }
        transport().send(player, new ConfigMutationResultPayload(
                requestId,
                ConfigManager.revision(),
                canEditConfiguration(player),
                result));
    }

    private static boolean publishForPlayer(
            final ServerPlayer player,
            final BooleanSupplier publication
    ) {
        PUBLICATION_ACKNOWLEDGED_PLAYER.set(player.getUUID());
        try {
            return publication.getAsBoolean();
        } finally {
            PUBLICATION_ACKNOWLEDGED_PLAYER.remove();
        }
    }

    private static void onConfigPublished(
            final long revision,
            final ConfigManager.PublicationKind kind
    ) {
        final MinecraftServer server = activeServer;
        if (server == null) {
            return;
        }
        final Optional<UUID> acknowledgedPlayer =
                Optional.ofNullable(PUBLICATION_ACKNOWLEDGED_PLAYER.get());
        server.execute(() -> {
            if (activeServer == server) {
                broadcastInvalidation(server, acknowledgedPlayer, revision, kind);
            }
        });
    }

    /** Refreshes clean editors and marks dirty drafts stale without acknowledging the initiator twice. */
    private static void broadcastInvalidation(
            final MinecraftServer server,
            final Optional<UUID> acknowledgedPlayer,
            final long revision,
            final ConfigManager.PublicationKind kind
    ) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (acknowledgedPlayer.map(id -> !id.equals(player.getUUID())).orElse(true)
                    && transport().canSend(player, ConfigInvalidationPayload.TYPE)) {
                transport().send(player, new ConfigInvalidationPayload(
                        revision,
                        kind == ConfigManager.PublicationKind.RESET
                                ? ConfigInvalidationPayload.ChangeKind.RESET
                                : ConfigInvalidationPayload.ChangeKind.UPDATE));
            }
        }
    }

    public static void sendSnapshot(final ServerPlayer player, final int requestId) {
        sendSnapshot(player, requestId, ConfigSnapshotPayload.PatchResult.NONE);
    }

    private static void sendSnapshot(
            final ServerPlayer player,
            final int requestId,
            final ConfigSnapshotPayload.PatchResult patchResult
    ) {
        if (!transport().canSend(player, ConfigSnapshotFragmentPayload.TYPE)) {
            return;
        }

        final ConfigManager.ClientSnapshot snapshot = ConfigManager.clientSnapshot();
        final String json = snapshot.json();
        final boolean editable = canEditConfiguration(player);
        final ConfigSnapshotPayload snapshotPayload = new ConfigSnapshotPayload(
                requestId,
                snapshot.revision(),
                json,
                editable,
                patchResult);
        final List<ConfigSnapshotFragmentPayload> fragments;
        try {
            fragments = ConfigSnapshotFragmentPayload.encode(snapshotPayload);
        } catch (IllegalArgumentException exception) {
            SmartResourceDrops.LOGGER.error(
                    "Refusing invalid Smart Resource Multiplier config snapshot for {}: {}",
                    player.getScoreboardName(),
                    exception.getMessage());
            return;
        }
        for (ConfigSnapshotFragmentPayload fragment : fragments) {
            transport().send(player, fragment);
        }
        SmartResourceDrops.LOGGER.debug(
                "Sent config snapshot #{} to {} (revision={}, editable={}, patchResult={}, chars={})",
                requestId,
                player.getScoreboardName(),
                snapshot.revision(),
                editable,
                patchResult,
                json.length());
    }

    static boolean canEditConfiguration(final ServerPlayer player) {
        return player.level().getServer().isSingleplayerOwner(player.getGameProfile())
                || player.hasPermissions(2);
    }

    private static Transport transport() {
        final Transport current = transport;
        if (current == null) {
            throw new IllegalStateException("Config networking has not been installed by the active loader");
        }
        return current;
    }

    /** Package-local bounded-state snapshot used by protocol admission tests. */
    static TransferDiagnostics transferDiagnostics() {
        synchronized (LAST_PATCH_TICK) {
            synchronized (LAST_PATCH_DECODE_TICK) {
                return new TransferDiagnostics(
                        PATCH_TRANSFERS.size(),
                        PENDING_PATCH_DECODES.size(),
                        bufferedPatchBytes() + pendingPatchDecodeBytes(),
                        PENDING_PATCH_DECODES.values().stream()
                                .map(pending -> pending.complete().metadata().requestId())
                                .sorted()
                                .toList());
            }
        }
    }

    /** Loader adapter for negotiated server-to-client play payloads. */
    public interface Transport {
        boolean canSend(ServerPlayer player, ResourceLocation type);

        void send(ServerPlayer player, ConfigPayload payload);
    }

    private record PendingRequest(int requestId, long eligibleTick) {
    }

    private record ReadyRequest(ServerPlayer player, int requestId) {
    }

    private record PendingPatch(ConfigPatchPayload payload, long eligibleTick) {
    }

    private record ReadyPatch(ServerPlayer player, ConfigPatchPayload payload) {
    }

    private record PendingPatchDecode(
            ConfigTransferAssembler.Complete<PatchMetadata> complete,
            long eligibleTick
    ) {
    }

    private record ReadyPatchDecode(
            ServerPlayer player,
            ConfigTransferAssembler.Complete<PatchMetadata> complete
    ) {
    }

    private enum DecodeAdmission {
        ADMIT,
        QUEUED,
        REJECT
    }

    record TransferDiagnostics(
            int activeTransfers,
            int pendingDecodes,
            int bufferedBytes,
            List<Integer> pendingRequestIds
    ) {
        TransferDiagnostics {
            pendingRequestIds = List.copyOf(pendingRequestIds);
        }
    }

    private record PatchMetadata(int requestId, long expectedRevision) {
    }
}
