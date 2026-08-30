package com.chedidandrew.smartresourcedrops.config;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ConfigScreenLayoutTest {
    private static final List<int[]> PHYSICAL_RESOLUTIONS = List.of(
            new int[] {1280, 720},
            new int[] {1920, 1080},
            new int[] {2560, 1440});

    @Test
    void commonPhysicalResolutionsAndGuiScalesProduceUsableLogicalLayouts() {
        for (int[] physical : PHYSICAL_RESOLUTIONS) {
            for (int guiScale : List.of(2, 3, 4)) {
                int logicalWidth = physical[0] / guiScale;
                int logicalHeight = physical[1] / guiScale;
                ConfigScreenLayout layout = ConfigScreenLayout.calculate(logicalWidth, logicalHeight);
                String label = physical[0] + "x" + physical[1] + " at GUI scale " + guiScale
                        + " (" + logicalWidth + "x" + logicalHeight + ")";

                assertTrue(layout.contentLeft() >= 0, label);
                assertTrue(layout.contentRight() <= logicalWidth, label);
                assertTrue(layout.contentWidth() <= 500, label);
                assertTrue(Math.abs(layout.contentLeft() - (logicalWidth - layout.contentRight())) <= 1, label);

                assertSectionUsesContentColumn(layout, layout.headerBounds(), label);
                assertSectionUsesContentColumn(layout, layout.listBounds(), label);
                assertSectionUsesContentColumn(layout, layout.footerBounds(), label);
                assertTrue(layout.headerBounds().y() >= 0, label);
                assertTrue(layout.headerBounds().bottom() <= layout.listBounds().y(), label);
                assertTrue(layout.listBounds().bottom() <= layout.footerBounds().y(), label);
                assertTrue(layout.footerBounds().bottom() <= logicalHeight, label);
                assertTrue(layout.listBounds().height() > 0, label);
                assertTrue(layout.listBounds().height() >= layout.rootRowHeight(), label);

                assertTrue(layout.columnGap() > 0, label);
                assertEquals(
                        layout.contentWidth(),
                        layout.leftColumnWidth() + layout.columnGap() + layout.rightColumnWidth(),
                        label);
                assertTrue(Math.abs(layout.leftColumnWidth() - layout.rightColumnWidth()) <= 1, label);
                assertTrue(layout.leftColumnWidth() >= 120, label);
                assertTrue(layout.rightColumnWidth() >= 120, label);
                assertTrue(layout.rootRowHeight() > 0, label);
                assertTrue(layout.rootRowSpacing() > 0, label);
                assertEquals(layout.rootRowHeight() + layout.rootRowSpacing(), layout.rootRowPitch(), label);
            }
        }
    }

    @Test
    void narrowestSupportedScaledWindowRetainsPracticalMarginsAndViewport() {
        ConfigScreenLayout layout = ConfigScreenLayout.calculate(320, 180);

        assertEquals(12, layout.contentLeft());
        assertEquals(296, layout.contentWidth());
        assertEquals(12, 320 - layout.contentRight());
        assertTrue(layout.compact());
        assertTrue(layout.listBounds().height() >= 4 * layout.rootRowHeight());
    }

    @Test
    void wideScreensCapTheCenteredContentColumn() {
        ConfigScreenLayout layout = ConfigScreenLayout.calculate(1280, 720);

        assertEquals(500, layout.contentWidth());
        assertEquals(390, layout.contentLeft());
        assertEquals(390, 1280 - layout.contentRight());
        assertFalse(layout.compact());
    }

    @Test
    void rejectsNonPositiveLogicalDimensions() {
        assertThrows(IllegalArgumentException.class, () -> ConfigScreenLayout.calculate(0, 180));
        assertThrows(IllegalArgumentException.class, () -> ConfigScreenLayout.calculate(320, 0));
        assertThrows(IllegalArgumentException.class, () -> ConfigScreenLayout.calculate(-1, 180));
    }

    private static void assertSectionUsesContentColumn(
            ConfigScreenLayout layout,
            ConfigScreenLayout.Bounds bounds,
            String label) {
        assertEquals(layout.contentLeft(), bounds.x(), label);
        assertEquals(layout.contentWidth(), bounds.width(), label);
    }
}
