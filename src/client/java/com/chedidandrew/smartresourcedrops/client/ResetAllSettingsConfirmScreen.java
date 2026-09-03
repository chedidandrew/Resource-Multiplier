package com.chedidandrew.smartresourcedrops.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;

/** Vanilla confirmation gate for the one destructive configuration action. */
public final class ResetAllSettingsConfirmScreen extends ConfirmScreen {
    private final SmartDropsConfigScreen root;

    public ResetAllSettingsConfirmScreen(final SmartDropsConfigScreen root) {
        super(
                root::handleResetConfirmation,
                Component.translatable("smart_resource_drops.gui.reset_confirm_title"),
                Component.translatable("smart_resource_drops.gui.reset_confirm_body"),
                Component.translatable("smart_resource_drops.gui.reset_everything")
                        .withStyle(ChatFormatting.RED),
                Component.translatable("smart_resource_drops.gui.reset_cancel"));
        this.root = root;
    }

    SmartDropsConfigScreen rootScreen() {
        return this.root;
    }

    /** Any non-button close path is Cancel and therefore never mutates configuration. */
    @Override
    public void onClose() {
        this.root.handleResetConfirmation(false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public boolean isInGameUi() {
        return this.minecraft != null && this.minecraft.level != null;
    }
}
