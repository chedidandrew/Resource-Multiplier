package com.chedidandrew.smartresourcedrops.network;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.config.ConfigManager;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

public final class SmartDropsNetworking {
    private static final long REQUEST_COOLDOWN_TICKS = 40L;
    private static final long PATCH_COOLDOWN_TICKS = 20L;
    private static final long RESET_COOLDOWN_TICKS = 40L;
    private static final Map<ServerPlayer, Long> LAST_REQUEST_TICK = new WeakHashMap<>();
    private static final Map<ServerPlayer, Long> LAST_PATCH_TICK = new WeakHashMap<>();
    private static final Map<ServerPlayer, Long> LAST_RESET_TICK = new WeakHashMap<>();
    private static final Map<ServerPlayer, PendingRequest> PENDING_REQUESTS = new WeakHashMap<>();
    private static final Map<ServerPlayer, PendingPatch> PENDING_PATCHES = new WeakHashMap<>();
    private static final ThreadLocal<UUID> PUBLICATION_ACKNOWLEDGED_PLAYER = new ThreadLocal<>();
    private static volatile MinecraftServer activeServer;

    private SmartDropsNetworking() {
    }

    public static void registerCommon() {
        PayloadTypeRegistry.serverboundPlay().register(ConfigRequestPayload.TYPE, ConfigRequestPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ConfigPatchPayload.TYPE, ConfigPatchPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ConfigResetPayload.TYPE, ConfigResetPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ConfigSnapshotPayload.TYPE, ConfigSnapshotPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(
                ConfigInvalidationPayload.TYPE,
                ConfigInvalidationPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(
                ConfigMutationResultPayload.TYPE,
                ConfigMutationResultPayload.CODEC);

        ConfigManager.setPublicationListener(SmartDropsNetworking::onConfigPublished);
        ServerLifecycleEvents.SERVER_STARTED.register(server -> activeServer = server);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            if (activeServer == server) {
                activeServer = null;
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(ConfigRequestPayload.TYPE, (payload, context) -> {
            final ServerPlayer player = context.player();
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
        });
        ServerPlayNetworking.registerGlobalReceiver(ConfigPatchPayload.TYPE, (payload, context) -> {
            final ServerPlayer player = context.player();
            final boolean editableAtReceipt = canEditConfiguration(player);
            SmartResourceDrops.LOGGER.debug(
                    "Received config patch #{} from {} (editable={})",
                    payload.requestId(),
                    player.getScoreboardName(),
                    editableAtReceipt);
            if (!acceptOrQueuePatch(player, payload, editableAtReceipt)) {
                return;
            }
            applyPatch(player, payload);
        });
        ServerPlayNetworking.registerGlobalReceiver(ConfigResetPayload.TYPE, (payload, context) -> {
            final ServerPlayer player = context.player();
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
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            flushPendingRequests();
            flushPendingPatches();
        });
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
        if (!ServerPlayNetworking.canSend(player, ConfigMutationResultPayload.TYPE)) {
            sendSnapshot(player, requestId, result);
            return;
        }
        ServerPlayNetworking.send(player, new ConfigMutationResultPayload(
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
                    && ServerPlayNetworking.canSend(player, ConfigInvalidationPayload.TYPE)) {
                ServerPlayNetworking.send(player, new ConfigInvalidationPayload(
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
        if (!ServerPlayNetworking.canSend(player, ConfigSnapshotPayload.TYPE)) {
            return;
        }

        final ConfigManager.ClientSnapshot snapshot = ConfigManager.clientSnapshot();
        final String json = snapshot.json();
        if (json.length() > ConfigSnapshotPayload.MAX_JSON_LENGTH) {
            SmartResourceDrops.LOGGER.error(
                "Refusing oversized Smart Resource Multiplier config snapshot for {} ({} characters; maximum {})",
                player.getScoreboardName(),
                json.length(),
                ConfigSnapshotPayload.MAX_JSON_LENGTH);
            return;
        }

        final boolean editable = canEditConfiguration(player);
        ServerPlayNetworking.send(player, new ConfigSnapshotPayload(
                requestId,
                snapshot.revision(),
                json,
                editable,
                patchResult));
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
        return player.level().getServer().isSingleplayerOwner(player.nameAndId())
                || player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    private record PendingRequest(int requestId, long eligibleTick) {
    }

    private record ReadyRequest(ServerPlayer player, int requestId) {
    }

    private record PendingPatch(ConfigPatchPayload payload, long eligibleTick) {
    }

    private record ReadyPatch(ServerPlayer player, ConfigPatchPayload payload) {
    }
}
