package com.chedidandrew.smartresourcedrops.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/** Centered text widget used by the multiplier control on pre-1.20 clients. */
final class LegacyTextWidget extends AbstractWidget {
    private final Font font;
    private @Nullable Component tooltip;

    LegacyTextWidget(Component message, Font font) {
        this(font.width(message), font.lineHeight, message, font);
    }

    LegacyTextWidget(int width, int height, Component message, Font font) {
        super(0, 0, width, height, message);
        this.font = font;
    }

    void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    void setTooltip(@Nullable Component tooltip) {
        this.tooltip = tooltip;
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        GuiComponent.drawCenteredString(
                poseStack,
                font,
                getMessage(),
                x + getWidth() / 2,
                y + (getHeight() - font.lineHeight) / 2,
                active ? 0xFFFFFFFF : 0xFFA0A0A0);
    }

    @Override
    public void renderToolTip(PoseStack poseStack, int mouseX, int mouseY) {
        Screen screen = Minecraft.getInstance().screen;
        if (tooltip != null && screen != null) {
            screen.renderTooltip(poseStack, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public void updateNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
