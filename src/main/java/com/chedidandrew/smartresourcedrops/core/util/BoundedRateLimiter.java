package com.chedidandrew.smartresourcedrops.core.util;

import java.util.LinkedHashMap;
import java.util.Map;

/** Small access-ordered key gate for suppressing repeated diagnostic messages. */
public final class BoundedRateLimiter {
    private final int maximumKeys;
    private final long intervalNanos;
    private final LinkedHashMap<String, Long> lastAccepted = new LinkedHashMap<>(16, 0.75F, true);

    public BoundedRateLimiter(final int maximumKeys, final long intervalNanos) {
        if (maximumKeys < 1 || intervalNanos < 1L) {
            throw new IllegalArgumentException("Rate-limit bounds must be positive");
        }
        this.maximumKeys = maximumKeys;
        this.intervalNanos = intervalNanos;
    }

    public synchronized boolean tryAcquire(final String key, final long nowNanos) {
        final Long previous = lastAccepted.get(key);
        if (previous != null) {
            final long elapsed = nowNanos - previous;
            if (elapsed >= 0L && elapsed < intervalNanos) {
                return false;
            }
        }

        lastAccepted.put(key, nowNanos);
        while (lastAccepted.size() > maximumKeys) {
            final Map.Entry<String, Long> eldest = lastAccepted.entrySet().iterator().next();
            lastAccepted.remove(eldest.getKey());
        }
        return true;
    }

    synchronized int size() {
        return lastAccepted.size();
    }
}
