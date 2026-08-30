package com.chedidandrew.smartresourcedrops.core.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Removes due work before callers execute it, making queue actions safe to re-enter the queue. */
public final class QueuedWorkDrain {
    private QueuedWorkDrain() {
    }

    public static <K, V> List<V> removeDue(
            final Map<K, TimedValue<V>> queue,
            final long now,
            final long quietPeriod,
            final boolean force
    ) {
        final List<V> due = new ArrayList<>();
        final Iterator<TimedValue<V>> iterator = queue.values().iterator();
        while (iterator.hasNext()) {
            final TimedValue<V> pending = iterator.next();
            if (force || now - pending.queuedAt() >= quietPeriod) {
                due.add(pending.value());
                iterator.remove();
            }
        }
        return due;
    }

    public record TimedValue<V>(V value, long queuedAt) {
    }
}
