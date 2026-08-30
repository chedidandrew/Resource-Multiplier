package com.chedidandrew.smartresourcedrops.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BoundedRateLimiterTest {
    @Test
    void suppressesRepeatedKeysButAllowsReasonsBlocksAndExpiry() {
        final BoundedRateLimiter limiter = new BoundedRateLimiter(4, 100L);

        assertTrue(limiter.tryAcquire("minecraft:stone|ITEMS", 1_000L));
        assertFalse(limiter.tryAcquire("minecraft:stone|ITEMS", 1_050L));
        assertTrue(limiter.tryAcquire("minecraft:stone|STACKS", 1_050L));
        assertTrue(limiter.tryAcquire("minecraft:dirt|ITEMS", 1_050L));
        assertTrue(limiter.tryAcquire("minecraft:stone|ITEMS", 1_100L));
    }

    @Test
    void boundsKeysAndRecoversFromAClockReset() {
        final BoundedRateLimiter limiter = new BoundedRateLimiter(2, 100L);

        assertTrue(limiter.tryAcquire("one", 500L));
        assertTrue(limiter.tryAcquire("two", 500L));
        assertTrue(limiter.tryAcquire("three", 500L));
        assertEquals(2, limiter.size());
        assertTrue(limiter.tryAcquire("one", 501L), "The evicted eldest key remained suppressed");
        assertEquals(2, limiter.size());
        assertTrue(limiter.tryAcquire("one", 10L), "A monotonic-clock reset remained suppressed");
    }
}
