package com.chedidandrew.smartresourcedrops.core.provenance;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class RecentRemovalCacheTest {
    @Test
    void entriesExpireAfterTwoTicks() {
        final RecentRemovalCache.RemovalWindow window = new RecentRemovalCache.RemovalWindow();
        window.record(10L, 100L);

        assertTrue(window.contains(10L, 100L));
        assertTrue(window.contains(10L, 102L));
        assertFalse(window.contains(10L, 103L));
    }

    @Test
    void overflowFailsClosedForTheRemovalWindow() {
        final RecentRemovalCache.RemovalWindow window = new RecentRemovalCache.RemovalWindow();
        for (long pos = 0; pos <= 4096; pos++) {
            window.record(pos, 200L);
        }

        assertTrue(window.contains(Long.MAX_VALUE, 200L));
        assertTrue(window.contains(Long.MAX_VALUE, 202L));
        assertFalse(window.contains(Long.MAX_VALUE, 203L));
    }

    @Test
    void clockRollbackClearsStaleEntries() {
        final RecentRemovalCache.RemovalWindow window = new RecentRemovalCache.RemovalWindow();
        window.record(1L, 50L);

        assertFalse(window.contains(1L, 49L));
    }

    @Test
    void peekDoesNotAdvanceOrExpireTheRemovalWindow() {
        final RecentRemovalCache.RemovalWindow window = new RecentRemovalCache.RemovalWindow();
        window.record(10L, 100L);

        assertFalse(window.peekContains(10L, 103L));
        assertTrue(window.contains(10L, 102L),
                "A read-only inspection must not advance the cache clock or purge the marker");
    }
}
