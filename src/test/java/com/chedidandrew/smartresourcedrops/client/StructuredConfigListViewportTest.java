package com.chedidandrew.smartresourcedrops.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StructuredConfigListViewportTest {
    @Test
    void convertsGuiViewportToBottomOriginFramebufferScissor() {
        assertEquals(
                new StructuredConfigList.ScissorBounds(68, 28, 504, 272),
                StructuredConfigList.framebufferScissor(68, 60, 572, 332, 1.0, 360));
    }

    @Test
    void fractionalScaleRoundsOutwardWithoutLosingEdgePixels() {
        assertEquals(
                new StructuredConfigList.ScissorBounds(15, 78, 152, 92),
                StructuredConfigList.framebufferScissor(10, 20, 111, 81, 1.5, 200));
    }

    @Test
    void integerHighDpiScalePreservesTheWholeViewport() {
        assertEquals(
                new StructuredConfigList.ScissorBounds(24, 28, 592, 432),
                StructuredConfigList.framebufferScissor(12, 20, 308, 236, 2.0, 500));
    }

    @Test
    void reversedOrCollapsedViewportNeverProducesNegativeExtent() {
        assertEquals(
                new StructuredConfigList.ScissorBounds(20, 90, 0, 0),
                StructuredConfigList.framebufferScissor(20, 20, 10, 10, 1.0, 100));
    }

    @Test
    void rejectsInvalidGuiScale() {
        assertThrows(
                IllegalArgumentException.class,
                () -> StructuredConfigList.framebufferScissor(0, 0, 10, 10, 0.0, 100));
    }
}
