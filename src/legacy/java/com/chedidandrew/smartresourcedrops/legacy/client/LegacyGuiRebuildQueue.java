package com.chedidandrew.smartresourcedrops.legacy.client;

/** One-shot UI rebuild request used to avoid mutating buttonList during mouse dispatch. */
final class LegacyGuiRebuildQueue {
    private boolean pending;

    void request() {
        pending = true;
    }

    boolean consume() {
        boolean requested = pending;
        pending = false;
        return requested;
    }
}
