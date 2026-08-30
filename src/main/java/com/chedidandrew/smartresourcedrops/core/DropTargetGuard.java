package com.chedidandrew.smartresourcedrops.core;

/** One-shot identity guard preventing nested loot callbacks from consuming another block's decision. */
final class DropTargetGuard {
    private final Object level;
    private final long packedPos;
    private final Object state;
    private boolean claimed;

    DropTargetGuard(final Object level, final long packedPos, final Object state) {
        this.level = level;
        this.packedPos = packedPos;
        this.state = state;
    }

    boolean matches(final Object candidateLevel, final long candidatePos, final Object candidateState) {
        return level == candidateLevel && packedPos == candidatePos && state == candidateState;
    }

    boolean claim(final Object candidateLevel, final long candidatePos, final Object candidateState) {
        if (claimed || !matches(candidateLevel, candidatePos, candidateState)) {
            return false;
        }
        claimed = true;
        return true;
    }

    boolean matchesPosition(final Object candidateLevel, final long candidatePos) {
        return level == candidateLevel && packedPos == candidatePos;
    }
}
