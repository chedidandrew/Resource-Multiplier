package com.chedidandrew.smartresourcedrops.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Simple bounded multiline text widget for Minecraft 1.19.2. */
final class LegacyMultiLineTextWidget extends AbstractWidget {
    private final Font font;

    LegacyMultiLineTextWidget(int x, int y, int width, int height, Component message, Font font) {
        super(x, y, width, height, message);
        this.font = font;
        this.active = false;
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        int lineY = y;
        for (FormattedCharSequence line : font.split(getMessage(), Math.max(1, getWidth()))) {
            if (lineY + font.lineHeight > y + getHeight()) {
                break;
            }
            GuiComponent.drawString(poseStack, font, line, x, lineY, 0xFFFFFFFF);
            lineY += font.lineHeight;
        }
    }

    @Override
    public void updateNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
