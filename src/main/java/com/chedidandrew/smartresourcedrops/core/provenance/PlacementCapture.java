package com.chedidandrew.smartresourcedrops.core.provenance;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Captures every block position created by one BlockItem placement transaction. */
public final class PlacementCapture {
    private static final int MAX_NESTED_PLACEMENTS = 64;
    private static final ThreadLocal<Deque<Transaction>> TRANSACTIONS =
        ThreadLocal.withInitial(ArrayDeque::new);

    private PlacementCapture() {
    }

    public static void begin(final Level level) {
        begin(level, true);
    }

    /** Defers reconciliation without treating arbitrary outer-hook writes as player placement. */
    public static void beginBoundary(final Level level) {
        begin(level, false);
    }

    private static void begin(final Level level, final boolean capturesPlacements) {
        final Deque<Transaction> transactions = TRANSACTIONS.get();
        if (transactions.size() >= MAX_NESTED_PLACEMENTS) {
            transactions.clear();
        }
        transactions.push(new Transaction(level, capturesPlacements));
    }

    /** Records the first state observed for a position in the active placement transaction. */
    public static void recordChange(
            final Level level,
            final BlockPos pos,
            final BlockState originalState,
            final boolean originallyPlaced,
            final int updateFlags
    ) {
        final Transaction transaction = TRANSACTIONS.get().peek();
        if (transaction != null && transaction.level() == level) {
            transaction.changes().compute(
                    pos.immutable(),
                    (ignored, existing) -> existing == null
                            ? new OriginalChange(
                                    originalState,
                                    originallyPlaced,
                                    updateFlags,
                                    transaction.capturesPlacements())
                            : existing.withPlacementCapture(
                                    existing.captureAsPlacement() || transaction.capturesPlacements()));
        }
    }

    public static void end(final boolean success) {
        final Deque<Transaction> transactions = TRANSACTIONS.get();
        final Transaction transaction = transactions.poll();
        if (success && transaction != null) {
            final Transaction parent = transactions.peek();
            if (parent != null && parent.level() == transaction.level()) {
                transaction.changes().forEach((pos, childChange) -> parent.changes().merge(
                        pos,
                        childChange,
                        (parentChange, ignored) -> parentChange.withPlacementCapture(
                                parentChange.captureAsPlacement() || childChange.captureAsPlacement())));
            } else {
                reconcile(transaction);
            }
        }
        if (transactions.isEmpty()) {
            TRANSACTIONS.remove();
        }
    }

    public static boolean active(final Level level) {
        final Transaction transaction = TRANSACTIONS.get().peek();
        return transaction != null && transaction.level() == level;
    }

    private static void reconcile(final Transaction transaction) {
        for (Map.Entry<BlockPos, OriginalChange> entry : transaction.changes().entrySet()) {
            final BlockPos pos = entry.getKey();
            final OriginalChange original = entry.getValue();
            final BlockState finalState = transaction.level().getBlockState(pos);

            if (original.captureAsPlacement()
                    && !finalState.isAir()
                    && finalState != original.state()) {
                PlacementProvenanceBridge.mark(transaction.level(), pos);
                continue;
            }
            if (!original.placed()
                    || (original.updateFlags() & Block.UPDATE_MOVE_BY_PISTON) != 0) {
                continue;
            }
            switch (ProvenanceTransitionPolicy.classify(original.state(), finalState)) {
                case PRESERVE -> {
                }
                case GENERATED -> PlacementProvenanceBridge.unmark(transaction.level(), pos);
                case REMOVE -> {
                    RecentRemovalCache.record(transaction.level(), pos);
                    PlacementProvenanceBridge.unmark(transaction.level(), pos);
                }
            }
        }
    }

    private record OriginalChange(
            BlockState state,
            boolean placed,
            int updateFlags,
            boolean captureAsPlacement
    ) {
        private OriginalChange withPlacementCapture(final boolean value) {
            return value == captureAsPlacement
                    ? this
                    : new OriginalChange(state, placed, updateFlags, value);
        }
    }

    private record Transaction(
            Level level,
            boolean capturesPlacements,
            Map<BlockPos, OriginalChange> changes
    ) {
        private Transaction(final Level level, final boolean capturesPlacements) {
            this(level, capturesPlacements, new LinkedHashMap<>());
        }
    }
}
