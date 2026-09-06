package com.chedidandrew.smartresourcedrops.legacy.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.chedidandrew.smartresourcedrops.legacy.SmartResourceMultiplier;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.common.Mod;
import org.junit.Test;

public final class LegacyGuiFactoryTest {
    @Test
    public void modMetadataEnablesTheForgeConfigButton() {
        Mod metadata = SmartResourceMultiplier.class.getAnnotation(Mod.class);
        assertNotNull(metadata);
        assertEquals(SmartResourceMultiplier.GUI_FACTORY, metadata.guiFactory());

        LegacyGuiFactory factory = new LegacyGuiFactory();
        assertTrue(factory.hasConfigGui());
        assertTrue(factory.runtimeGuiCategories().isEmpty());
        GuiScreen screen = factory.createConfigGui(null);
        assertTrue(screen instanceof LegacyConfigScreen);
        assertTrue(((LegacyConfigScreen) screen).isReadyForLocalEditing());
    }

    @Test
    public void compactMainPageDoesNotOverlapTheActionRow() {
        int minimumSupportedHeight = 235;
        int lastMainButtonBottom = 50
                + 6 * LegacyConfigScreen.verticalGap(minimumSupportedHeight, 27)
                + 20;
        int lastDensePageButtonBottom = 48
                + 7 * LegacyConfigScreen.verticalGap(minimumSupportedHeight, 26)
                + 20;
        int actionRowY = LegacyConfigScreen.actionRowY(minimumSupportedHeight);

        assertTrue(lastMainButtonBottom < actionRowY);
        assertTrue(lastDensePageButtonBottom < actionRowY);
        assertTrue(actionRowY + 20 <= minimumSupportedHeight);
        assertEquals(20, LegacyConfigScreen.verticalGap(269, 27));
        assertEquals(27, LegacyConfigScreen.verticalGap(270, 27));
    }
}
