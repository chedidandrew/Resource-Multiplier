package com.chedidandrew.smartresourcedrops.core.provenance;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Bounded two-tick cache for remove-before-drop integrations. Some machines
 * clear the block before asking its loot table for drops. Remembering only
 * previously marked positions closes that provenance-laundering window.
 */
public final class RecentRemovalCache {
    private static final long MAX_AGE_TICKS = 2L;
    private static final int MAX_ENTRIES_PER_LEVEL = 4096;
    private static final Map<Level, RemovalWindow> REMOVALS = new WeakHashMap<>();

    private RecentRemovalCache() {
    }

    public static synchronized void record(final Level level, final BlockPos pos) {
        REMOVALS.computeIfAbsent(level, ignored -> new RemovalWindow())
            .record(pos.asLong(), level.getGameTime());
    }

    public static synchronized boolean wasRecentlyRemoved(final Level level, final BlockPos pos) {
        final RemovalWindow levelEntries = REMOVALS.get(level);
        if (levelEntries == null) {
            return false;
        }
        return levelEntries.contains(pos.asLong(), level.getGameTime());
    }

    /**
     * Observes the current removal window without expiring entries, advancing
     * its clock, or consuming any conservative fallback state.
     */
    public static synchronized boolean peekWasRecentlyRemoved(final Level level, final BlockPos pos) {
        final RemovalWindow levelEntries = REMOVALS.get(level);
        if (levelEntries == null) {
            return false;
        }
        return levelEntries.peekContains(pos.asLong(), level.getGameTime());
    }

    static final class RemovalWindow {
        private final LinkedHashMap<Long, Long> entries = new LinkedHashMap<>();
        private long lastTick = Long.MIN_VALUE;
        private long conservativeUntil = Long.MIN_VALUE;

        void record(final long packedPos, final long now) {
            advance(now);
            entries.remove(packedPos);
            if (entries.size() >= MAX_ENTRIES_PER_LEVEL) {
                final Iterator<Long> iterator = entries.keySet().iterator();
                if (iterator.hasNext()) {
                    iterator.next();
                    iterator.remove();
                }
                conservativeUntil = now + MAX_AGE_TICKS;
            }
            entries.put(packedPos, now);
        }

        boolean contains(final long packedPos, final long now) {
            advance(now);
            if (now <= conservativeUntil) {
                return true;
            }
            final Long removedAt = entries.get(packedPos);
            return removedAt != null && now - removedAt <= MAX_AGE_TICKS;
        }

        boolean peekContains(final long packedPos, final long now) {
            if (lastTick != Long.MIN_VALUE && now < lastTick) {
                return false;
            }
            if (now <= conservativeUntil) {
                return true;
            }
            final Long removedAt = entries.get(packedPos);
            return removedAt != null && now - removedAt <= MAX_AGE_TICKS;
        }

        private void advance(final long now) {
            if (lastTick != Long.MIN_VALUE && now < lastTick) {
                entries.clear();
                conservativeUntil = Long.MIN_VALUE;
            }
            lastTick = now;
            final Iterator<Map.Entry<Long, Long>> iterator = entries.entrySet().iterator();
            while (iterator.hasNext()) {
                final Map.Entry<Long, Long> entry = iterator.next();
                if (now - entry.getValue() <= MAX_AGE_TICKS) {
                    break;
                }
                iterator.remove();
            }
        }
    }
}
