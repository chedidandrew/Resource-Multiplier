package com.chedidandrew.smartresourcedrops.client;

import com.chedidandrew.smartresourcedrops.config.ConfigPatch;
import com.chedidandrew.smartresourcedrops.config.ConfigRequestLifecycle;
import com.chedidandrew.smartresourcedrops.config.SmartDropsConfig;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Explicit loading/error bridge shared by Mod Menu and {@code /smartdropsgui}. */
public final class SmartDropsConfigLoadingScreen extends Screen {
    private static final int REQUEST_TIMEOUT_TICKS = 120;

    private final Screen returnScreen;
    private final Screen resultParent;
    private final Operation operation;
    private final ConfigPatch patch;
    private final long expectedRevision;
    private Button retryButton;
    private Button cancelButton;
    private State state = State.LOADING;
    private ConfigRequestLifecycle.Failure failure = ConfigRequestLifecycle.Failure.NONE;
    private boolean initialRequestStarted;
    private int requestId = -1;
    private int waitingTicks;

    public SmartDropsConfigLoadingScreen(final Screen parent) {
        this(parent, parent, Operation.REQUEST, null, 0L);
    }

    public SmartDropsConfigLoadingScreen(
            final Screen returnScreen,
            final Screen resultParent,
            final ConfigPatch patch,
            final long expectedRevision
    ) {
        this(returnScreen, resultParent, Operation.PATCH, patch, expectedRevision);
    }

    static SmartDropsConfigLoadingScreen forReset(
            final Screen returnScreen,
            final Screen resultParent,
            final long expectedRevision
    ) {
        return new SmartDropsConfigLoadingScreen(
                returnScreen,
                resultParent,
                Operation.RESET,
                null,
                expectedRevision);
    }

    private SmartDropsConfigLoadingScreen(
            final Screen returnScreen,
            final Screen resultParent,
            final Operation operation,
            final ConfigPatch patch,
            final long expectedRevision
    ) {
        super(Component.translatable("smart_resource_drops.title"));
        this.returnScreen = returnScreen;
        this.resultParent = resultParent;
        this.operation = operation;
        this.patch = patch;
        this.expectedRevision = Math.max(0L, expectedRevision);
    }

    @Override
    protected void init() {
        final int panelWidth = Math.min(316, Math.max(1, this.width - 16));
        final int buttonWidth = Math.max(1, (panelWidth - 8) / 2);
        final int left = (this.width - panelWidth) / 2;
        final int buttonY = Math.min(this.height - 24, this.height / 2 + 48);

        this.retryButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("smart_resource_drops.gui.retry"),
                        button -> this.beginRequest())
                .bounds(left, buttonY, buttonWidth, 20)
                .build());
        this.retryButton.active = this.state == State.ERROR;

        this.cancelButton = this.addRenderableWidget(Button.builder(
                        Component.translatable(this.operation == Operation.RESET
                                ? "smart_resource_drops.gui.reload_current"
                                : "smart_resource_drops.gui.cancel"),
                        button -> this.onClose())
                .bounds(left + buttonWidth + 8, buttonY, buttonWidth, 20)
                .build());
        this.cancelButton.active = this.operation != Operation.RESET || this.state == State.ERROR;

        if (!this.initialRequestStarted) {
            this.initialRequestStarted = true;
            this.beginRequest();
        }
    }

    private void beginRequest() {
        if (this.requestId >= 0) {
            ClientConfigState.cancelRequest(this.requestId);
        }
        this.requestId = -1;
        this.waitingTicks = 0;
        this.state = State.LOADING;
        this.failure = ConfigRequestLifecycle.Failure.NONE;

        final ClientConfigState.RequestStart start = switch (this.operation) {
            case REQUEST -> ClientConfigState.request(this.minecraft);
            case PATCH -> ClientConfigState.submit(
                    this.minecraft,
                    this.expectedRevision,
                    this.patch);
            case RESET -> ClientConfigState.reset(this.minecraft, this.expectedRevision);
        };
        if (start.started()) {
            this.requestId = start.requestId();
            if (this.retryButton != null) {
                this.retryButton.active = false;
            }
            return;
        }

        this.showError(switch (start.failure()) {
            case NO_CONNECTION -> ConfigRequestLifecycle.Failure.NO_CONNECTION;
            case CHANNEL_UNAVAILABLE -> ConfigRequestLifecycle.Failure.CHANNEL_UNAVAILABLE;
            case INVALID_PATCH -> ConfigRequestLifecycle.Failure.INVALID_PATCH;
            case SEND_FAILED -> ConfigRequestLifecycle.Failure.SEND_FAILED;
            case NONE -> ConfigRequestLifecycle.Failure.SEND_FAILED;
        });
    }

    @Override
    public void tick() {
        if (this.state != State.LOADING || this.requestId < 0) {
            return;
        }
        if (this.minecraft.getConnection() == null) {
            ClientConfigState.cancelRequest(this.requestId);
            this.showError(ConfigRequestLifecycle.Failure.DISCONNECTED);
            return;
        }
        if (!ClientConfigState.isCurrent(this.requestId, this.minecraft)) {
            this.showError(ConfigRequestLifecycle.Failure.DISCONNECTED);
            return;
        }

        this.waitingTicks++;
        if (this.waitingTicks >= REQUEST_TIMEOUT_TICKS) {
            ClientConfigState.failRequest(
                    this.requestId,
                    this.minecraft,
                    ConfigRequestLifecycle.Failure.TIMEOUT);
        }
    }

    boolean acceptsRequest(final int candidateRequestId) {
        return this.state == State.LOADING && this.requestId == candidateRequestId;
    }

    Screen resultParent() {
        return this.resultParent;
    }

    Screen returnScreen() {
        return this.returnScreen;
    }

    void showError(final ConfigRequestLifecycle.Failure reason) {
        this.state = State.ERROR;
        this.failure = reason;
        if (this.retryButton != null) {
            this.retryButton.active = true;
        }
        if (this.cancelButton != null) {
            this.cancelButton.active = true;
        }
    }

    void openReady(
            final SmartDropsConfig snapshot,
            final boolean editable,
            final long revision,
            final String status
    ) {
        this.minecraft.setScreen(new SmartDropsConfigScreen(
                this.resultParent,
                snapshot,
                editable,
                status,
                revision));
    }

    @Override
    public void render(
            final GuiGraphics graphics,
            final int mouseX,
            final int mouseY,
            final float partialTick
    ) {
        super.render(graphics, mouseX, mouseY, partialTick);
        final int centerX = this.width / 2;
        final int centerY = this.height / 2;

        graphics.drawCenteredString(this.font, this.title, centerX, centerY - 58, 0xFFFFFFFF);
        if (this.state == State.LOADING) {
            graphics.drawCenteredString(
                    this.font,
                    fitted(Component.translatable(this.operation == Operation.RESET
                            ? "smart_resource_drops.gui.reset_loading"
                            : "smart_resource_drops.gui.loading")),
                    centerX,
                    centerY - 20,
                    0xFFE0E0E0);
            graphics.drawCenteredString(
                    this.font,
                    fitted(Component.translatable(this.operation == Operation.RESET
                            ? "smart_resource_drops.gui.reset_loading_detail"
                            : "smart_resource_drops.gui.loading_detail")),
                    centerX,
                    centerY,
                    0xFFA0A0A0);
            return;
        }

        graphics.drawCenteredString(
                this.font,
                fitted(Component.translatable("smart_resource_drops.gui.load_failed")),
                centerX,
                centerY - 30,
                0xFFFF8080);
        graphics.drawCenteredString(
                this.font,
                fitted(Component.translatable(
                        "smart_resource_drops.gui.failure_reason",
                        Component.translatable(failureTranslationKey()))),
                centerX,
                centerY - 10,
                0xFFE0E0E0);
        graphics.drawCenteredString(
                this.font,
                fitted(Component.translatable("smart_resource_drops.gui.error_detail")),
                centerX,
                centerY + 10,
                0xFFA0A0A0);
    }

    private Component fitted(final Component text) {
        final String value = text.getString();
        final int maximumWidth = Math.max(1, this.width - 24);
        if (this.font.width(value) <= maximumWidth) {
            return text;
        }
        final String suffix = "...";
        return Component.literal(this.font.plainSubstrByWidth(
                value,
                Math.max(1, maximumWidth - this.font.width(suffix))) + suffix);
    }

    private String failureTranslationKey() {
        return switch (this.failure) {
            case NO_CONNECTION -> "smart_resource_drops.gui.reason_no_connection";
            case CHANNEL_UNAVAILABLE -> "smart_resource_drops.gui.reason_sync_unavailable";
            case INVALID_PATCH -> "smart_resource_drops.gui.reason_invalid_patch";
            case SEND_FAILED -> "smart_resource_drops.gui.reason_send_failed";
            case TIMEOUT -> "smart_resource_drops.gui.reason_timeout";
            case DISCONNECTED -> "smart_resource_drops.gui.reason_disconnected";
            case INVALID_RESPONSE -> "smart_resource_drops.gui.reason_invalid_response";
            case NONE -> "smart_resource_drops.gui.reason_unknown";
        };
    }

    @Override
    public void onClose() {
        if (this.requestId >= 0) {
            ClientConfigState.cancelRequest(this.requestId);
        }
        if (this.state == State.ERROR) {
            ClientConfigState.clearPendingCompactResult();
        }
        if (this.operation == Operation.RESET) {
            if (this.state == State.LOADING) {
                return;
            }
            if (this.minecraft.getConnection() != null) {
                this.minecraft.setScreen(new SmartDropsConfigLoadingScreen(this.resultParent));
            } else {
                this.minecraft.setScreen(this.resultParent);
            }
            return;
        }
        this.minecraft.setScreen(this.returnScreen);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return this.operation != Operation.RESET || this.state == State.ERROR;
    }

    @Override
    public void removed() {
        if (this.requestId >= 0) {
            ClientConfigState.cancelRequest(this.requestId);
        }
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public boolean isInGameUi() {
        return this.minecraft != null && this.minecraft.level != null;
    }

    enum State {
        LOADING,
        ERROR
    }

    enum Operation {
        REQUEST,
        PATCH,
        RESET
    }
}
