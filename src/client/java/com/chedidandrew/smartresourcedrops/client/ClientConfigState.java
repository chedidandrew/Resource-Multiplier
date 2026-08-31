package com.chedidandrew.smartresourcedrops.client;

import java.util.Optional;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.config.ConfigManager;
import com.chedidandrew.smartresourcedrops.config.ConfigPatch;
import com.chedidandrew.smartresourcedrops.config.ConfigRequestLifecycle;
import com.chedidandrew.smartresourcedrops.config.SmartDropsConfig;
import com.chedidandrew.smartresourcedrops.core.client.util.ClientCommandQueue;
import com.chedidandrew.smartresourcedrops.network.ConfigPatchPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigRequestPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigResetPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigSnapshotPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigInvalidationPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigMutationResultPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ClientConfigState {
    private static final String CONFIG_REQUEST_QUEUE_KEY = "smart_resource_drops:config_request";
    private static final ConfigRequestLifecycle REQUESTS = new ConfigRequestLifecycle();

    private static Object cachedConnection;
    private static String cachedSnapshotJson;
    private static boolean cachedEditable;
    private static long cachedRevision;
    private static ConfigSnapshotPayload.PatchResult pendingCompactResult =
            ConfigSnapshotPayload.PatchResult.NONE;

    private ClientConfigState() {
    }

    public static RequestStart request(final Minecraft minecraft) {
        final Object connection = minecraft.getConnection();
        if (connection == null) {
            return RequestStart.failed(StartFailure.NO_CONNECTION);
        }
        if (!ClientPlayNetworking.canSend(ConfigRequestPayload.TYPE)) {
            return RequestStart.failed(StartFailure.CHANNEL_UNAVAILABLE);
        }

        final int requestId = REQUESTS.begin(connection);
        final boolean queued = ClientCommandQueue.runCoalesced(CONFIG_REQUEST_QUEUE_KEY, () -> {
            if (!REQUESTS.isCurrent(requestId, minecraft.getConnection())) {
                SmartResourceDrops.LOGGER.debug("Skipped stale queued config request #{}", requestId);
                return;
            }
            try {
                ClientPlayNetworking.send(new ConfigRequestPayload(requestId));
                SmartResourceDrops.LOGGER.debug("Sent config request #{}", requestId);
            } catch (RuntimeException exception) {
                SmartResourceDrops.LOGGER.debug("Could not send config request #{}", requestId, exception);
                minecraft.execute(() -> failRequest(
                        requestId,
                        minecraft,
                        ConfigRequestLifecycle.Failure.SEND_FAILED));
            }
        });
        if (!queued) {
            REQUESTS.fail(requestId, connection, ConfigRequestLifecycle.Failure.SEND_FAILED);
            return RequestStart.failed(StartFailure.SEND_FAILED);
        }
        showLoadingOverlay(minecraft);
        SmartResourceDrops.LOGGER.debug("Queued config request #{}", requestId);
        return RequestStart.started(requestId);
    }

    public static RequestStart submit(
            final Minecraft minecraft,
            final long expectedRevision,
            final ConfigPatch patch
    ) {
        final Object connection = minecraft.getConnection();
        if (connection == null) {
            return RequestStart.failed(StartFailure.NO_CONNECTION);
        }
        if (!ClientPlayNetworking.canSend(ConfigPatchPayload.TYPE)) {
            return RequestStart.failed(StartFailure.CHANNEL_UNAVAILABLE);
        }

        final String json;
        try {
            json = ConfigManager.encodeClientPatch(patch);
        } catch (IllegalArgumentException exception) {
            return RequestStart.failed(StartFailure.INVALID_PATCH);
        }
        if (json.length() > ConfigPatchPayload.MAX_JSON_LENGTH) {
            return RequestStart.failed(StartFailure.INVALID_PATCH);
        }

        final int requestId = REQUESTS.begin(connection);
        try {
            ClientCommandQueue.cancelCoalesced(CONFIG_REQUEST_QUEUE_KEY);
            ClientPlayNetworking.send(new ConfigPatchPayload(requestId, expectedRevision, json));
            SmartResourceDrops.LOGGER.debug("Sent config patch #{} ({} chars)", requestId, json.length());
        } catch (RuntimeException exception) {
            REQUESTS.fail(requestId, connection, ConfigRequestLifecycle.Failure.SEND_FAILED);
            SmartResourceDrops.LOGGER.debug("Could not send config patch #{}", requestId, exception);
            return RequestStart.failed(StartFailure.SEND_FAILED);
        }
        showLoadingOverlay(minecraft);
        return RequestStart.started(requestId);
    }

    /** Sends one revision-guarded, server-authoritative reset transaction. */
    public static RequestStart reset(final Minecraft minecraft, final long expectedRevision) {
        final Object connection = minecraft.getConnection();
        if (connection == null) {
            return RequestStart.failed(StartFailure.NO_CONNECTION);
        }
        if (!ClientPlayNetworking.canSend(ConfigResetPayload.TYPE)) {
            return RequestStart.failed(StartFailure.CHANNEL_UNAVAILABLE);
        }

        // A reset supersedes the old snapshot generation and any delayed config request.
        final int requestId = REQUESTS.begin(connection);
        ClientCommandQueue.cancelCoalesced(CONFIG_REQUEST_QUEUE_KEY);
        invalidateCachedSnapshot();
        try {
            ClientPlayNetworking.send(new ConfigResetPayload(requestId, expectedRevision));
            SmartResourceDrops.LOGGER.debug(
                    "Sent config reset #{} against revision {}",
                    requestId,
                    expectedRevision);
        } catch (RuntimeException exception) {
            REQUESTS.fail(requestId, connection, ConfigRequestLifecycle.Failure.SEND_FAILED);
            SmartResourceDrops.LOGGER.debug("Could not send config reset #{}", requestId, exception);
            return RequestStart.failed(StartFailure.SEND_FAILED);
        }
        showLoadingOverlay(minecraft);
        return RequestStart.started(requestId);
    }

    public static void accept(final ConfigSnapshotPayload payload, final Minecraft minecraft) {
        final Object connection = minecraft.getConnection();
        if (!REQUESTS.isCurrent(payload.requestId(), connection)) {
            SmartResourceDrops.LOGGER.debug(
                    "Ignored stale config snapshot #{} (current request #{})",
                    payload.requestId(),
                    REQUESTS.currentRequestId());
            return;
        }

        final Screen current = minecraft.gui.screen();
        if (!(current instanceof SmartDropsConfigLoadingScreen loading)
                || !loading.acceptsRequest(payload.requestId())) {
            SmartResourceDrops.LOGGER.debug(
                    "Ignored config snapshot #{} because its loading screen is no longer current",
                    payload.requestId());
            cancelRequest(payload.requestId());
            return;
        }

        final Optional<SmartDropsConfig> decoded = ConfigManager.tryParseSnapshotJson(payload.json());
        if (decoded.isEmpty()) {
            failRequest(payload.requestId(), minecraft, ConfigRequestLifecycle.Failure.INVALID_RESPONSE);
            return;
        }
        if (!REQUESTS.accept(payload.requestId(), connection)) {
            return;
        }

        cachedConnection = connection;
        cachedSnapshotJson = payload.json();
        cachedEditable = payload.editable();
        cachedRevision = payload.revision();
        final ConfigSnapshotPayload.PatchResult effectiveResult =
                payload.patchResult() == ConfigSnapshotPayload.PatchResult.NONE
                        ? pendingCompactResult
                        : payload.patchResult();
        pendingCompactResult = ConfigSnapshotPayload.PatchResult.NONE;
        final String status = statusFor(effectiveResult);
        SmartResourceDrops.LOGGER.debug(
                "Accepted config snapshot #{}; GUI transition LOADING -> READY (editable={})",
                payload.requestId(),
                payload.editable());
        loading.openReady(decoded.get(), payload.editable(), payload.revision(), status);
    }

    public static Optional<CachedSnapshot> cachedSnapshot(final Minecraft minecraft) {
        final Object connection = minecraft.getConnection();
        if (connection == null || connection != cachedConnection || cachedSnapshotJson == null) {
            return Optional.empty();
        }
        final Optional<SmartDropsConfig> decoded = ConfigManager.tryParseSnapshotJson(cachedSnapshotJson);
        if (decoded.isEmpty()) {
            invalidateCachedSnapshot();
            return Optional.empty();
        }
        return Optional.of(new CachedSnapshot(decoded.get(), cachedEditable, cachedRevision));
    }

    /** Handles ordinary revision advances without discarding dirty drafts; resets remain destructive. */
    public static void acceptInvalidation(
            final ConfigInvalidationPayload payload,
            final Minecraft minecraft
    ) {
        if (minecraft.getConnection() == null || payload.revision() <= 0L) {
            return;
        }
        final Screen current = minecraft.gui.screen();
        final Screen resultParent;
        final long visibleRevision;
        final ConfigEditorSession visibleSession;
        final SmartDropsConfigScreen visibleRoot;
        if (current instanceof SmartDropsConfigScreen root) {
            resultParent = root.editorSession().originalParent();
            visibleRevision = root.editorSession().revision();
            visibleSession = root.editorSession();
            visibleRoot = root;
        } else if (current instanceof SmartDropsSubScreen child) {
            resultParent = child.session.originalParent();
            visibleRevision = child.session.revision();
            visibleSession = child.session;
            visibleRoot = child.root;
        } else if (current instanceof ResetAllSettingsConfirmScreen confirmation) {
            resultParent = confirmation.rootScreen().editorSession().originalParent();
            visibleRevision = confirmation.rootScreen().editorSession().revision();
            visibleSession = confirmation.rootScreen().editorSession();
            visibleRoot = confirmation.rootScreen();
        } else if (current instanceof SmartDropsConfigLoadingScreen loading) {
            resultParent = loading.resultParent();
            visibleRevision = -1L;
            visibleSession = null;
            visibleRoot = null;
        } else {
            if (payload.revision() > cachedRevision) {
                invalidateCachedSnapshot();
            }
            return;
        }
        if (visibleRevision >= payload.revision()) {
            return;
        }

        invalidateCachedSnapshot();
        if (payload.changeKind() == ConfigInvalidationPayload.ChangeKind.UPDATE
                && visibleSession != null
                && visibleSession.isDirty()) {
            visibleSession.markServerRevisionAdvanced(payload.revision());
            visibleSession.setStatus(Component.translatable(
                    "smart_resource_drops.gui.server_changed_draft_kept").getString());
            if (current instanceof ResetAllSettingsConfirmScreen && visibleRoot != null) {
                minecraft.gui.setScreen(visibleRoot);
            }
            SmartResourceDrops.LOGGER.debug(
                    "Kept dirty config draft after server revision advanced to {}",
                    payload.revision());
            return;
        }

        invalidatePendingMutations();
        minecraft.gui.setScreen(new SmartDropsConfigLoadingScreen(resultParent));
        SmartResourceDrops.LOGGER.debug(
                "Reloading clean config editor after server {} advanced revision to {}",
                payload.changeKind(),
                payload.revision());
    }

    /** Handles compact mutation failures without reflecting a full server config to packet spam. */
    public static void acceptMutationResult(
            final ConfigMutationResultPayload payload,
            final Minecraft minecraft
    ) {
        final Object connection = minecraft.getConnection();
        if (!REQUESTS.isCurrent(payload.requestId(), connection)) {
            return;
        }
        final Screen current = minecraft.gui.screen();
        if (!(current instanceof SmartDropsConfigLoadingScreen loading)
                || !loading.acceptsRequest(payload.requestId())
                || !REQUESTS.accept(payload.requestId(), connection)) {
            return;
        }

        invalidateCachedSnapshot();
        final boolean unauthorized = payload.result() == ConfigSnapshotPayload.PatchResult.UNAUTHORIZED
                || payload.result() == ConfigSnapshotPayload.PatchResult.RESET_UNAUTHORIZED;
        if (unauthorized) {
            pendingCompactResult = payload.result();
            minecraft.gui.setScreen(new SmartDropsConfigLoadingScreen(loading.resultParent()));
            return;
        }

        final Screen returnScreen = loading.returnScreen();
        if (returnScreen instanceof SmartDropsConfigScreen root) {
            root.editorSession().markServerRevisionAdvanced(payload.revision());
            root.editorSession().setStatus(statusFor(payload.result()));
            minecraft.gui.setScreen(root);
            return;
        }

        pendingCompactResult = payload.result();
        minecraft.gui.setScreen(new SmartDropsConfigLoadingScreen(loading.resultParent()));
    }

    public static boolean isCurrent(final int requestId, final Minecraft minecraft) {
        return REQUESTS.isCurrent(requestId, minecraft.getConnection());
    }

    /**
     * Invalidates every queued Smart Resource Multiplier client mutation/request generation.
     * This is deliberately called only after the user confirms a full reset.
     */
    public static void invalidatePendingMutations() {
        final int requestId = REQUESTS.currentRequestId();
        if (requestId >= 0) {
            REQUESTS.cancel(requestId);
        }
        ClientCommandQueue.clear();
        pendingCompactResult = ConfigSnapshotPayload.PatchResult.NONE;
        invalidateCachedSnapshot();
    }

    public static void cancelRequest(final int requestId) {
        if (REQUESTS.cancel(requestId)) {
            ClientCommandQueue.cancelCoalesced(CONFIG_REQUEST_QUEUE_KEY);
            pendingCompactResult = ConfigSnapshotPayload.PatchResult.NONE;
            SmartResourceDrops.LOGGER.debug("Cancelled config request #{}", requestId);
        }
    }

    public static void failRequest(
            final int requestId,
            final Minecraft minecraft,
            final ConfigRequestLifecycle.Failure reason
    ) {
        if (!REQUESTS.fail(requestId, minecraft.getConnection(), reason)) {
            return;
        }
        ClientCommandQueue.cancelCoalesced(CONFIG_REQUEST_QUEUE_KEY);
        final Screen current = minecraft.gui.screen();
        if (current instanceof SmartDropsConfigLoadingScreen loading
                && loading.acceptsRequest(requestId)) {
            loading.showError(reason);
        }
        SmartResourceDrops.LOGGER.debug(
                "Config GUI transition LOADING -> ERROR for request #{} ({})",
                requestId,
                reason);
    }

    public static void onDisconnect(final Minecraft minecraft, final Object disconnectedConnection) {
        final int requestId = REQUESTS.currentRequestId();
        final boolean wasLoading = REQUESTS.disconnect(disconnectedConnection);
        ClientCommandQueue.clear();
        pendingCompactResult = ConfigSnapshotPayload.PatchResult.NONE;
        invalidateCachedSnapshot();
        if (wasLoading) {
            final Screen current = minecraft.gui.screen();
            if (current instanceof SmartDropsConfigLoadingScreen loading
                    && loading.acceptsRequest(requestId)) {
                loading.showError(ConfigRequestLifecycle.Failure.DISCONNECTED);
            }
            SmartResourceDrops.LOGGER.debug(
                    "Config GUI transition LOADING -> ERROR for request #{} (DISCONNECTED)",
                    requestId);
        }
    }

    private static void showLoadingOverlay(final Minecraft minecraft) {
        if (minecraft.player != null) {
            minecraft.player.sendOverlayMessage(Component.translatable("smart_resource_drops.gui.loading"));
        }
    }

    private static String statusFor(final ConfigSnapshotPayload.PatchResult result) {
        return switch (result) {
            case NONE -> "";
            case APPLIED -> Component.translatable("smart_resource_drops.gui.patch_applied").getString();
            case REJECTED -> Component.translatable("smart_resource_drops.gui.patch_rejected_draft_kept")
                    .getString();
            case UNAUTHORIZED -> Component.translatable("smart_resource_drops.gui.patch_unauthorized").getString();
            case RESET_APPLIED -> Component.translatable(
                    "smart_resource_drops.gui.reset_server_applied").getString();
            case RESET_REJECTED -> Component.translatable(
                    "smart_resource_drops.gui.reset_server_rejected_draft_kept").getString();
            case RESET_UNAUTHORIZED -> Component.translatable(
                    "smart_resource_drops.gui.reset_server_unauthorized").getString();
        };
    }

    static void invalidateCachedSnapshot() {
        cachedConnection = null;
        cachedSnapshotJson = null;
        cachedEditable = false;
        cachedRevision = 0L;
    }

    static void clearPendingCompactResult() {
        pendingCompactResult = ConfigSnapshotPayload.PatchResult.NONE;
    }

    public enum StartFailure {
        NONE,
        NO_CONNECTION,
        CHANNEL_UNAVAILABLE,
        INVALID_PATCH,
        SEND_FAILED
    }

    public record RequestStart(int requestId, StartFailure failure) {
        static RequestStart started(final int requestId) {
            return new RequestStart(requestId, StartFailure.NONE);
        }

        static RequestStart failed(final StartFailure failure) {
            return new RequestStart(-1, failure);
        }

        public boolean started() {
            return requestId >= 0;
        }
    }

    public record CachedSnapshot(SmartDropsConfig config, boolean editable, long revision) {
    }
}
