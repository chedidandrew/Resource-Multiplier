package com.chedidandrew.smartresourcedrops.core.entity;

import net.minecraft.world.phys.Vec3;

import java.util.Objects;

/** One-shot identity for the exact mob-death XP award currently entering ExperienceOrb.award. */
final class EntityExperienceAwardToken {
    private final Object level;
    private final Vec3 position;
    private final int amount;
    private boolean consumed;

    EntityExperienceAwardToken(Object level, Vec3 position, int amount) {
        this.level = Objects.requireNonNull(level, "level");
        this.position = Objects.requireNonNull(position, "position");
        this.amount = amount;
    }

    boolean consume(Object candidateLevel, Vec3 candidatePosition, int candidateAmount) {
        if (consumed
                || candidateLevel != level
                || amount != candidateAmount
                || !position.equals(candidatePosition)) {
            return false;
        }
        consumed = true;
        return true;
    }

    boolean consumed() {
        return consumed;
    }
}
