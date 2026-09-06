package com.chedidandrew.smartresourcedrops.legacy.client;

/** Pure legacy-screen geometry kept separate so it can be tested without loading LWJGL. */
final class LegacyConfigLayout {
    private LegacyConfigLayout() {}

    static int actionRowY(int screenHeight) {
        return screenHeight - 24;
    }

    static int verticalGap(int screenHeight, int normal) {
        return screenHeight < 270 ? 20 : normal;
    }
}
