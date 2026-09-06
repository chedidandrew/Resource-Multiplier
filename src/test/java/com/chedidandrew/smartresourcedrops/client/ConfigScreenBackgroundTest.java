package com.chedidandrew.smartresourcedrops.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ConfigScreenBackgroundTest {
    @Test
    void obscuresPreviousMenuWhenNoWorldIsLoaded() {
        assertTrue(ConfigScreenBackground.shouldObscureParent(false));
    }

    @Test
    void preservesTransparentInWorldPresentation() {
        assertFalse(ConfigScreenBackground.shouldObscureParent(true));
    }
}
