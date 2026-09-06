package com.chedidandrew.smartresourcedrops.legacy.client;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class LegacyClientGuiQueueTest {
    @Test
    public void coalescesRequestsForTheNextClientTick() {
        LegacyClientGuiQueue.consumeOpenRequest();

        LegacyClientGuiQueue.requestOpen();
        LegacyClientGuiQueue.requestOpen();

        assertTrue(LegacyClientGuiQueue.consumeOpenRequest());
        assertFalse(LegacyClientGuiQueue.consumeOpenRequest());
    }
}
