package com.chedidandrew.smartresourcedrops.core.provenance;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Captures every block position created by one BlockItem placement transaction. */
public final class PlacementCapture {
    private static final int MAX_NESTED_PLACEMENTS = 64;
    private static final ThreadLocal<Deque<Transaction>> TRANSACTIONS =
        ThreadLocal.withInitial(ArrayDeque::new);

    private PlacementCapture() {
    }

    public static void begin(final Level level) {
        final Deque<Transaction> transactions = TRANSACTIONS.get();
        if (transactions.size() >= MAX_NESTED_PLACEMENTS) {
            transactions.clear();
        }
        transactions.push(new Transaction(level));
    }

    public static void recordCandidate(final Level level, final BlockPos pos) {
        final Transaction transaction = TRANSACTIONS.get().peek();
        if (transaction != null && transaction.level() == level) {
            transaction.positions().add(pos.immutable());
        }
    }

    public static void end(final boolean success) {
        final Deque<Transaction> transactions = TRANSACTIONS.get();
        final Transaction transaction = transactions.poll();
        if (transactions.isEmpty()) {
            TRANSACTIONS.remove();
        }
        if (success && transaction != null) {
            for (BlockPos pos : transaction.positions()) {
                PlacementProvenanceBridge.mark(transaction.level(), pos);
            }
        }
    }

    public static boolean active(final Level level) {
        final Transaction transaction = TRANSACTIONS.get().peek();
        return transaction != null && transaction.level() == level;
    }

    private record Transaction(Level level, Set<BlockPos> positions) {
        private Transaction(final Level level) {
            this(level, new LinkedHashSet<>());
        }
    }
}
