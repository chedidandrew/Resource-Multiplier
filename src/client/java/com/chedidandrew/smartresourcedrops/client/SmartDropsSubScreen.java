package com.chedidandrew.smartresourcedrops.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Shared navigation and sizing for screens inside one staged editor session. */
public abstract class SmartDropsSubScreen extends Screen {
    private static final int FOOTER_GAP = 4;
    private static final int BACK_BUTTON_HEIGHT = 20;

    public record UnsavedChangesIndicatorLayout(
            int x,
            int y,
            int width,
            int height,
            boolean stacked
    ) {
    }

    protected final SmartDropsConfigScreen root;
    protected final Screen backScreen;
    protected final ConfigEditorSession session;

    SmartDropsSubScreen(
            final Component title,
            final SmartDropsConfigScreen root,
            final Screen backScreen,
            final ConfigEditorSession session
    ) {
        super(title);
        this.root = root;
        this.backScreen = backScreen;
        this.session = session;
    }

    protected final int contentWidth() {
        return Math.min(500, Math.max(1, this.width - 24));
    }

    protected final int contentLeft() {
        return (this.width - this.contentWidth()) / 2;
    }

    protected final int titleY() {
        return this.height < 220 ? 5 : 14;
    }

    protected final int contentTop() {
        return this.height < 220 ? 24 : 34;
    }

    protected final int footerY() {
        return Math.max(this.contentTop() + 20, this.height - 28);
    }

    /** Bottom edge available to child content, excluding a stacked footer indicator. */
    public final int contentBottom() {
        return this.usesStackedUnsavedChangesLayout()
                ? this.footerY() - this.font.lineHeight - (FOOTER_GAP * 2)
                : this.footerY() - FOOTER_GAP;
    }

    /** Screens with paired footer actions can reserve the stacked form at every width. */
    protected boolean supportsInlineUnsavedChangesIndicator() {
        return true;
    }

    protected final Button addBackButton() {
        final int buttonWidth = Math.min(200, this.contentWidth());
        return this.addRenderableWidget(Button.builder(
                        Component.translatable("smart_resource_drops.gui.back"),
                        button -> this.onClose())
                .bounds((this.width - buttonWidth) / 2, this.footerY(), buttonWidth, 20)
                .build());
    }

    /** Testable render condition shared by every child and focused editor screen. */
    public final boolean unsavedChangesIndicatorVisible() {
        return this.session.isDirty();
    }

    /** Exact bounds used by rendering and compact-layout client regression checks. */
    public final UnsavedChangesIndicatorLayout unsavedChangesIndicatorLayout() {
        final Component text = this.unsavedChangesIndicatorText();
        final int textWidth = this.font.width(text);
        final boolean stacked = this.usesStackedUnsavedChangesLayout();
        final int x = stacked
                ? (this.width - textWidth) / 2
                : (this.width - Math.min(200, this.contentWidth())) / 2 - FOOTER_GAP - textWidth;
        final int y = stacked
                ? this.footerY() - this.font.lineHeight - FOOTER_GAP
                : this.footerY() + (BACK_BUTTON_HEIGHT - this.font.lineHeight) / 2;
        return new UnsavedChangesIndicatorLayout(
                x,
                y,
                textWidth,
                this.font.lineHeight,
                stacked);
    }

    private boolean usesStackedUnsavedChangesLayout() {
        if (!this.supportsInlineUnsavedChangesIndicator()) {
            return true;
        }
        final int backLeft = (this.width - Math.min(200, this.contentWidth())) / 2;
        return backLeft - FOOTER_GAP - this.font.width(this.unsavedChangesIndicatorText())
                < this.contentLeft();
    }

    private Component unsavedChangesIndicatorText() {
        return ConfigUiText.fitted(
                this.font,
                Component.translatable("smart_resource_drops.gui.unsaved_changes"),
                Math.max(1, this.contentWidth()));
    }

    @Override
    public void render(
            final GuiGraphics graphics,
            final int mouseX,
            final int mouseY,
            final float partialTick
    ) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(
                this.font,
                ConfigUiText.fitted(this.font, this.title, Math.max(1, this.width - 24)),
                this.width / 2,
                this.titleY(),
                0xFFFFFFFF);
        if (this.session.serverRevisionStale()) {
            graphics.drawCenteredString(
                    this.font,
                    ConfigUiText.fitted(
                            this.font,
                            Component.translatable(
                                    "smart_resource_drops.gui.server_revision_stale",
                                    this.session.latestKnownServerRevision()),
                            Math.max(1, this.width - 24)),
                    this.width / 2,
                    this.titleY() + 11,
                    0xFFFFC060);
        }
        if (this.unsavedChangesIndicatorVisible()) {
            final UnsavedChangesIndicatorLayout layout = this.unsavedChangesIndicatorLayout();
            graphics.drawString(
                    this.font,
                    this.unsavedChangesIndicatorText(),
                    layout.x(),
                    layout.y(),
                    0xFFD6B85C);
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.backScreen);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public boolean isInGameUi() {
        return this.minecraft != null && this.minecraft.level != null;
    }
}
