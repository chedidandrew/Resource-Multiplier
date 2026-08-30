package com.chedidandrew.smartresourcedrops.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class DropTargetGuardTest {
    @Test
    void mismatchedNestedLootCannotConsumeTheExpectedTarget() {
        final Object level = new Object();
        final Object state = new Object();
        final DropTargetGuard guard = new DropTargetGuard(level, 42L, state);

        assertFalse(guard.claim(level, 43L, state));
        assertFalse(guard.claim(level, 42L, new Object()));
        assertTrue(guard.claim(level, 42L, state));
        assertFalse(guard.claim(level, 42L, state));
    }

    @Test
    void experienceRequiresTheSameLevelAndPosition() {
        final Object level = new Object();
        final DropTargetGuard guard = new DropTargetGuard(level, 7L, new Object());

        assertTrue(guard.matchesPosition(level, 7L));
        assertFalse(guard.matchesPosition(new Object(), 7L));
        assertFalse(guard.matchesPosition(level, 8L));
    }
}
