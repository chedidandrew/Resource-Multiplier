package com.chedidandrew.smartresourcedrops.core.entity;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EntityExperienceHardeningTest {
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void xpBudgetMultipliesSafeAwardsAndLeavesPathologicalAwardsVanilla() {
        EntityExperienceBudget.Result safe = EntityExperienceBudget.multiply(100, 64);
        assertEquals(6_400, safe.amount());
        assertFalse(safe.budgetExceeded());

        EntityExperienceBudget.Result bounded = EntityExperienceBudget.multiply(10_000, 64);
        assertEquals(10_000, bounded.amount());
        assertTrue(bounded.budgetExceeded());

        EntityExperienceBudget.Result alreadyLarge = EntityExperienceBudget.multiply(
                EntityExperienceBudget.MAX_MULTIPLIED_XP_AWARD + 1,
                2);
        assertEquals(EntityExperienceBudget.MAX_MULTIPLIED_XP_AWARD + 1, alreadyLarge.amount());
        assertTrue(alreadyLarge.budgetExceeded());
    }

    @Test
    void awardTokenIgnoresUnrelatedNestedAwardsAndConsumesItsExactAwardOnce() {
        Object level = new Object();
        Vec3 position = new Vec3(1.25, 2.5, 3.75);
        EntityExperienceAwardToken token = new EntityExperienceAwardToken(level, position, 42);

        assertFalse(token.consume(level, position, 7));
        assertFalse(token.consume(new Object(), position, 42));
        assertFalse(token.consume(level, position.add(0.0, 1.0, 0.0), 42));
        assertTrue(token.consume(level, position, 42));
        assertTrue(token.consumed());
        assertFalse(token.consume(level, position, 42));
    }

    @Test
    void immediatePlayerMustMatchLiveVanillaAttribution() {
        EntityKillAttribution valid = EntityMultiplierResolver.validateImmediatePlayer(
                PLAYER,
                true,
                PLAYER,
                EntityKillAttribution.Kind.DIRECT_PLAYER);
        assertEquals(EntityKillAttribution.Kind.DIRECT_PLAYER, valid.kind());

        EntityKillAttribution wrongPlayer = EntityMultiplierResolver.validateImmediatePlayer(
                PLAYER,
                true,
                OTHER_PLAYER,
                EntityKillAttribution.Kind.DIRECT_PLAYER);
        assertEquals(EntityKillAttribution.Kind.NONE, wrongPlayer.kind());

        EntityKillAttribution tamedMemory = EntityMultiplierResolver.validateImmediatePlayer(
                PLAYER,
                true,
                PLAYER,
                EntityKillAttribution.Kind.TAMED_ENTITY);
        assertEquals(EntityKillAttribution.Kind.DIRECT_PLAYER, tamedMemory.kind());
    }

    @Test
    void persistedOriginParsingFailsClosed() {
        assertEquals(
                EntityKillAttribution.Kind.DIRECT_PLAYER,
                EntityKillAttribution.Kind.parsePersisted("DIRECT_PLAYER"));
        assertEquals(
                EntityKillAttribution.Kind.TAMED_ENTITY,
                EntityKillAttribution.Kind.parsePersisted("TAMED_ENTITY"));
        assertEquals(EntityKillAttribution.Kind.NONE, EntityKillAttribution.Kind.parsePersisted("bad"));
        assertEquals(EntityKillAttribution.Kind.NONE, EntityKillAttribution.Kind.parsePersisted(""));
    }

    @Test
    void knownBossFallbackIsInstanceFreeAndConservative() {
        assertTrue(EntityClassifier.isKnownBossType("minecraft:ender_dragon"));
        assertTrue(EntityClassifier.isKnownBossType("minecraft:wither"));
        assertTrue(EntityClassifier.isKnownBossType("minecraft:warden"));
        assertTrue(EntityClassifier.isKnownBossType("minecraft:elder_guardian"));
        assertTrue(EntityClassifier.isKnownBossType("minecraft:ravager"));
        assertTrue(EntityClassifier.isKnownBossType("minecraft:evoker"));
        assertFalse(EntityClassifier.isKnownBossType("minecraft:zombie"));
    }
}
