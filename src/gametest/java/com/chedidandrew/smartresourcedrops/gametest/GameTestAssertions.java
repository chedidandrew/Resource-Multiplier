package com.chedidandrew.smartresourcedrops.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;

/** Keeps assertion call sites readable across the Component-only GameTest API. */
final class GameTestAssertions {
    private GameTestAssertions() {
    }

    static void assertTrue(
            final GameTestHelper helper,
            final boolean condition,
            final String message
    ) {
        GameTestAssertions.assertTrue(helper, condition, Component.literal(message));
    }

    static void assertTrue(
            final GameTestHelper helper,
            final boolean condition,
            final Component message
    ) {
        helper.assertTrue(condition, message);
    }

    static void assertFalse(
            final GameTestHelper helper,
            final boolean condition,
            final String message
    ) {
        GameTestAssertions.assertFalse(helper, condition, Component.literal(message));
    }

    static void assertFalse(
            final GameTestHelper helper,
            final boolean condition,
            final Component message
    ) {
        helper.assertFalse(condition, message);
    }
}
