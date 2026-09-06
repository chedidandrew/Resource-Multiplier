package com.chedidandrew.smartresourcedrops.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Minimal drawing facade that keeps the shared screen code readable on Minecraft 1.19.2. */
final class LegacyGuiGraphics {
    private final PoseStack poseStack;

    LegacyGuiGraphics(PoseStack poseStack) {
        this.poseStack = poseStack;
    }

    void drawString(Font font, Component text, int x, int y, int color) {
        GuiComponent.drawString(poseStack, font, text, x, y, color);
    }

    void drawString(Font font, String text, int x, int y, int color) {
        GuiComponent.drawString(poseStack, font, text, x, y, color);
    }

    void drawString(Font font, FormattedCharSequence text, int x, int y, int color) {
        GuiComponent.drawString(poseStack, font, text, x, y, color);
    }

    void drawCenteredString(Font font, Component text, int x, int y, int color) {
        GuiComponent.drawCenteredString(poseStack, font, text, x, y, color);
    }

    void drawCenteredString(Font font, String text, int x, int y, int color) {
        GuiComponent.drawCenteredString(poseStack, font, text, x, y, color);
    }

    void fill(int left, int top, int right, int bottom, int color) {
        GuiComponent.fill(poseStack, left, top, right, bottom, color);
    }
}
