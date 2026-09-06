package com.chedidandrew.smartresourcedrops.legacy;

import java.util.Collections;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Bounded, per-player-coalesced handoff from legacy Netty threads to the server tick. */
final class LegacyServerTaskQueue {
    private static final Logger LOGGER = LogManager.getLogger("SmartResourceMultiplier");
    private final int maximumPendingTasks;
    private final Queue<Runnable> tasks = new ConcurrentLinkedQueue<Runnable>();
    private final Set<UUID> pendingPlayers = Collections.newSetFromMap(
            new ConcurrentHashMap<UUID, Boolean>());
    private final AtomicInteger pendingTasks = new AtomicInteger();

    LegacyServerTaskQueue(int maximumPendingTasks) {
        if (maximumPendingTasks < 1) throw new IllegalArgumentException("maximumPendingTasks");
        this.maximumPendingTasks = maximumPendingTasks;
    }

    boolean offer(final UUID playerId, final Runnable task) {
        if (playerId == null || task == null || !pendingPlayers.add(playerId)) return false;
        if (pendingTasks.incrementAndGet() > maximumPendingTasks) {
            pendingTasks.decrementAndGet();
            pendingPlayers.remove(playerId);
            return false;
        }
        tasks.add(new Runnable() {
            @Override
            public void run() {
                try {
                    task.run();
                } finally {
                    pendingTasks.decrementAndGet();
                    pendingPlayers.remove(playerId);
                }
            }
        });
        return true;
    }

    void drain() {
        Runnable task;
        while ((task = tasks.poll()) != null) {
            try {
                task.run();
            } catch (RuntimeException exception) {
                // A malformed request must not abort the server tick or starve
                // unrelated players' already-queued configuration work.
                LOGGER.error("Discarding failed Smart Resource Multiplier server task", exception);
            }
        }
    }
}
