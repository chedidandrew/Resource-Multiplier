package com.chedidandrew.smartresourcedrops.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/** Small 1.19.2-compatible replacement for the newer vanilla button builder and tooltip API. */
final class LegacyButton extends Button {
    static final int VANILLA_TEXTURE_WIDTH = 200;
    private static final int TEXTURE_CAP_WIDTH = VANILLA_TEXTURE_WIDTH / 2;
    private static final int TEXTURE_ATLAS_SIZE = 256;
    private static final int BUTTON_TEXTURE_Y = 46;
    private static final int BUTTON_TEXTURE_ROW_HEIGHT = 20;

    private @Nullable Component tooltip;

    private LegacyButton(
            int x,
            int y,
            int width,
            int height,
            Component message,
            OnPress onPress,
            @Nullable Component tooltip
    ) {
        super(x, y, width, height, message, onPress, LegacyButton::renderTooltip);
        this.tooltip = tooltip;
    }

    static Builder builder(Component message, OnPress onPress) {
        return new Builder(message, onPress);
    }

    void setTooltip(@Nullable Component tooltip) {
        this.tooltip = tooltip;
    }

    void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    int left() {
        return this.x;
    }

    int top() {
        return this.y;
    }

    /**
     * Minecraft 1.19.2's vanilla renderer samples two {@code width / 2} regions from a
     * 200-pixel-wide texture. Widths above 200 therefore use negative or out-of-range texture
     * coordinates. Preserve the full widget bounds and render those wider buttons with fixed
     * vanilla caps plus a stretched center slice.
     */
    @Override
    public void renderButton(
            final PoseStack poseStack,
            final int mouseX,
            final int mouseY,
            final float partialTick
    ) {
        if (!requiresWideTexture(this.getWidth())) {
            super.renderButton(poseStack, mouseX, mouseY, partialTick);
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        final Font font = minecraft.font;
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, WIDGETS_LOCATION);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
        final int textureY = BUTTON_TEXTURE_Y
                + this.getYImage(this.isHoveredOrFocused()) * BUTTON_TEXTURE_ROW_HEIGHT;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();

        final WideTextureLayout layout = wideTextureLayout(this.getWidth());
        final TextureSlice center = layout.center();
        GuiComponent.blit(
                poseStack,
                this.x + center.destinationOffset(),
                this.y,
                center.destinationWidth(),
                this.getHeight(),
                (float) center.textureU(),
                (float) textureY,
                center.textureWidth(),
                this.getHeight(),
                TEXTURE_ATLAS_SIZE,
                TEXTURE_ATLAS_SIZE);
        renderFixedSlice(poseStack, textureY, layout.left());
        renderFixedSlice(poseStack, textureY, layout.right());

        this.renderBg(poseStack, minecraft, mouseX, mouseY);
        final int textColor = this.active ? 0xFFFFFF : 0xA0A0A0;
        GuiComponent.drawCenteredString(
                poseStack,
                font,
                this.getMessage(),
                this.x + this.getWidth() / 2,
                this.y + (this.getHeight() - 8) / 2,
                textColor | Mth.ceil(this.alpha * 255.0F) << 24);
        if (this.isHoveredOrFocused()) {
            this.renderToolTip(poseStack, mouseX, mouseY);
        }
    }

    private void renderFixedSlice(
            final PoseStack poseStack,
            final int textureY,
            final TextureSlice slice
    ) {
        this.blit(
                poseStack,
                this.x + slice.destinationOffset(),
                this.y,
                slice.textureU(),
                textureY,
                slice.destinationWidth(),
                this.getHeight());
    }

    static boolean requiresWideTexture(final int width) {
        return width > VANILLA_TEXTURE_WIDTH;
    }

    static WideTextureLayout wideTextureLayout(final int width) {
        if (!requiresWideTexture(width)) {
            throw new IllegalArgumentException("Wide texture layout requires width > 200");
        }
        return new WideTextureLayout(
                new TextureSlice(0, TEXTURE_CAP_WIDTH, 0, TEXTURE_CAP_WIDTH),
                new TextureSlice(
                        TEXTURE_CAP_WIDTH,
                        width - VANILLA_TEXTURE_WIDTH,
                        TEXTURE_CAP_WIDTH - 1,
                        2),
                new TextureSlice(
                        width - TEXTURE_CAP_WIDTH,
                        TEXTURE_CAP_WIDTH,
                        TEXTURE_CAP_WIDTH,
                        TEXTURE_CAP_WIDTH));
    }

    record TextureSlice(
            int destinationOffset,
            int destinationWidth,
            int textureU,
            int textureWidth
    ) {
    }

    record WideTextureLayout(TextureSlice left, TextureSlice center, TextureSlice right) {
    }

    private static void renderTooltip(Button button, PoseStack poseStack, int mouseX, int mouseY) {
        if (!(button instanceof LegacyButton legacy) || legacy.tooltip == null) {
            return;
        }
        Screen screen = Minecraft.getInstance().screen;
        if (screen != null) {
            screen.renderTooltip(poseStack, legacy.tooltip, mouseX, mouseY);
        }
    }

    static final class Builder {
        private final Component message;
        private final OnPress onPress;
        private int x;
        private int y;
        private int width = DEFAULT_WIDTH;
        private int height = DEFAULT_HEIGHT;
        private @Nullable Component tooltip;

        private Builder(Component message, OnPress onPress) {
            this.message = Objects.requireNonNull(message, "message");
            this.onPress = Objects.requireNonNull(onPress, "onPress");
        }

        Builder size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        Builder bounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            return size(width, height);
        }

        Builder tooltip(@Nullable Component tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        LegacyButton build() {
            return new LegacyButton(x, y, width, height, message, onPress, tooltip);
        }
    }
}
