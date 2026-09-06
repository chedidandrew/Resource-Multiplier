package com.chedidandrew.smartresourcedrops.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Keeps transparent in-world config screens from retaining a previous menu frame. */
final class ConfigScreenBackground {
    private ConfigScreenBackground() {
    }

    static boolean shouldObscureParent(final boolean worldLoaded) {
        return !worldLoaded;
    }

    static void renderIfNeeded(
            final Screen screen,
            final PoseStack poseStack,
            final Minecraft minecraft
    ) {
        if (minecraft != null && shouldObscureParent(minecraft.level != null)) {
            screen.renderBackground(poseStack);
        }
    }
}
