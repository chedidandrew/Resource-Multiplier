package com.chedidandrew.smartresourcedrops.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;

/** String-message assertion adapters for Minecraft 1.21.5's component-based GameTest API. */
final class GameTestAssertions {
    private GameTestAssertions() {
    }

    static void assertTrue(final GameTestHelper helper, final boolean condition, final String message) {
        helper.assertTrue(condition, Component.literal(message));
    }

    static void assertFalse(final GameTestHelper helper, final boolean condition, final String message) {
        helper.assertFalse(condition, Component.literal(message));
    }
}
