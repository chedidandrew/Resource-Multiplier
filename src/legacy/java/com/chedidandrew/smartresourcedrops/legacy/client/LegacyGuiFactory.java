package com.chedidandrew.smartresourcedrops.legacy.client;

import cpw.mods.fml.client.IModGuiFactory;
import java.util.Collections;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

/** Enables Forge's title-screen Config button without bypassing in-world server authority. */
public final class LegacyGuiFactory implements IModGuiFactory {
    @Override
    public void initialize(Minecraft minecraftInstance) {
        // No client state is needed until Forge constructs the screen.
    }

    @Override
    public Class<? extends GuiScreen> mainConfigGuiClass() {
        return LegacyConfigScreen.class;
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return Collections.emptySet();
    }

    @Override
    public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement element) {
        return null;
    }
}
