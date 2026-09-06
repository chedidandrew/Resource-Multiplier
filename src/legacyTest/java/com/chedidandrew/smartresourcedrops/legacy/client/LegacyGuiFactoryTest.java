package com.chedidandrew.smartresourcedrops.legacy.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.chedidandrew.smartresourcedrops.legacy.SmartResourceMultiplier;
import cpw.mods.fml.common.Mod;
import org.junit.Test;

public final class LegacyGuiFactoryTest {
    @Test
    public void modMetadataEnablesAnEditableForgeConfigScreen() throws Exception {
        Mod metadata = SmartResourceMultiplier.class.getAnnotation(Mod.class);
        assertNotNull(metadata);
        assertEquals(SmartResourceMultiplier.GUI_FACTORY, metadata.guiFactory());

        LegacyGuiFactory factory = new LegacyGuiFactory();
        assertEquals(LegacyConfigScreen.class, factory.mainConfigGuiClass());
        assertTrue(factory.runtimeGuiCategories().isEmpty());
        assertNull(factory.getHandlerFor(null));
        assertNotNull(factory.mainConfigGuiClass()
                .getConstructor(net.minecraft.client.gui.GuiScreen.class));
    }

    @Test
    public void compactPagesDoNotOverlapTheActionRow() {
        int minimumSupportedHeight = 235;
        int lastMainButtonBottom = 50
                + 6 * LegacyConfigLayout.verticalGap(minimumSupportedHeight, 27)
                + 20;
        int lastDensePageButtonBottom = 48
                + 7 * LegacyConfigLayout.verticalGap(minimumSupportedHeight, 26)
                + 20;
        int actionRowY = LegacyConfigLayout.actionRowY(minimumSupportedHeight);

        assertTrue(lastMainButtonBottom < actionRowY);
        assertTrue(lastDensePageButtonBottom < actionRowY);
        assertTrue(actionRowY + 20 <= minimumSupportedHeight);
        assertEquals(20, LegacyConfigLayout.verticalGap(269, 27));
        assertEquals(27, LegacyConfigLayout.verticalGap(270, 27));
    }
}
