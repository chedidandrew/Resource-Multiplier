package com.chedidandrew.smartresourcedrops.core.entity;

/** Prevents SRD from turning a bounded vanilla XP award into a pathological orb loop. */
final class EntityExperienceBudget {
    static final int MAX_MULTIPLIED_XP_AWARD = 2477 * 256;

    private EntityExperienceBudget() {
    }

    static Result multiply(int amount, int multiplier) {
        if (amount <= 0 || multiplier <= 1) {
            return new Result(amount, false);
        }
        long multiplied = (long) amount * multiplier;
        if (multiplied > MAX_MULTIPLIED_XP_AWARD) {
            return new Result(amount, true);
        }
        return new Result((int) multiplied, false);
    }

    record Result(int amount, boolean budgetExceeded) {
    }
}
