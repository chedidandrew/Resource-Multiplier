package com.chedidandrew.smartresourcedrops.legacy.client;

import java.util.concurrent.atomic.AtomicBoolean;

/** Coalesces chat-command GUI requests until GuiChat has finished closing itself. */
final class LegacyClientGuiQueue {
    private static final AtomicBoolean OPEN_REQUESTED = new AtomicBoolean();

    private LegacyClientGuiQueue() {}

    static void requestOpen() {
        OPEN_REQUESTED.set(true);
    }

    static boolean consumeOpenRequest() {
        return OPEN_REQUESTED.getAndSet(false);
    }
}
