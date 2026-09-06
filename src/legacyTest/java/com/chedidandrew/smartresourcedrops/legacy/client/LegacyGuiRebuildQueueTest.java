package com.chedidandrew.smartresourcedrops.legacy.client;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public final class LegacyGuiRebuildQueueTest {
    @Test
    public void repeatedRequestsCoalesceIntoOneDeferredRebuild() {
        LegacyGuiRebuildQueue queue = new LegacyGuiRebuildQueue();

        assertFalse(queue.consume());
        queue.request();
        queue.request();
        assertTrue(queue.consume());
        assertFalse(queue.consume());
    }
}
