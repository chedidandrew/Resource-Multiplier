package com.chedidandrew.smartresourcedrops.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LegacyButtonTextureLayoutTest {
    @Test
    void vanillaWidthDoesNotSelectWideRendering() {
        assertFalse(LegacyButton.requiresWideTexture(200));
        assertTrue(LegacyButton.requiresWideTexture(201));
        assertThrows(IllegalArgumentException.class, () -> LegacyButton.wideTextureLayout(200));
    }

    @Test
    void wideLayoutsCoverTheDestinationWithSafeTextureCoordinates() {
        assertWideLayout(201, 1);
        assertWideLayout(246, 46);
        assertWideLayout(500, 300);
    }

    private static void assertWideLayout(final int width, final int expectedCenterWidth) {
        final LegacyButton.WideTextureLayout layout = LegacyButton.wideTextureLayout(width);
        assertEquals(0, layout.left().destinationOffset());
        assertEquals(100, layout.left().destinationWidth());
        assertEquals(100, layout.center().destinationOffset());
        assertEquals(expectedCenterWidth, layout.center().destinationWidth());
        assertEquals(width - 100, layout.right().destinationOffset());
        assertEquals(100, layout.right().destinationWidth());
        assertEquals(layout.center().destinationOffset(),
                layout.left().destinationOffset() + layout.left().destinationWidth());
        assertEquals(layout.right().destinationOffset(),
                layout.center().destinationOffset() + layout.center().destinationWidth());
        assertEquals(width,
                layout.right().destinationOffset() + layout.right().destinationWidth());
        for (LegacyButton.TextureSlice slice : new LegacyButton.TextureSlice[]{
                layout.left(), layout.center(), layout.right()
        }) {
            assertTrue(slice.destinationWidth() > 0);
            assertTrue(slice.textureU() >= 0);
            assertTrue(slice.textureWidth() > 0);
            assertTrue(slice.textureU() + slice.textureWidth()
                    <= LegacyButton.VANILLA_TEXTURE_WIDTH);
        }
    }
}
