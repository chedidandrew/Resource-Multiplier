package com.chedidandrew.smartresourcedrops.core.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.chedidandrew.smartresourcedrops.core.util.QueuedWorkDrain.TimedValue;

final class QueuedWorkDrainTest {
    @Test
    void removesDueActionsBeforeTheyCanReenterTheQueue() {
        final Map<String, TimedValue<Runnable>> queue = new LinkedHashMap<>();
        queue.put("first", new TimedValue<>(() -> {
            queue.remove("second");
            queue.put("replacement", new TimedValue<>(() -> { }, 20L));
        }, 0L));
        queue.put("second", new TimedValue<>(() -> { }, 0L));

        final List<Runnable> due = QueuedWorkDrain.removeDue(queue, 10L, 5L, false);
        assertEquals(2, due.size());
        assertEquals(0, queue.size());
        assertDoesNotThrow(() -> due.forEach(Runnable::run));
        assertEquals(List.of("replacement"), List.copyOf(queue.keySet()));
    }
}
