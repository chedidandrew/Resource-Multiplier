package com.chedidandrew.smartresourcedrops.core.entity;

import com.chedidandrew.smartresourcedrops.platform.PlatformPlayerSupport;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;
import static org.mockito.Mockito.when;

final class EntityMultiplierResolverTest {
    @BeforeAll
    static void installRealPlayerClassifier() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        PlatformPlayerSupport.installFakePlayerPredicate(player -> false);
    }

    @Test
    void resolvesTheRealPlayerAtTheRootOfNestedOwnableEntities() {
        Player player = mock(Player.class);
        LivingEntity intermediary = ownableEntity();
        LivingEntity attacker = ownableEntity();
        when(((OwnableEntity) intermediary).getOwner()).thenReturn(player);
        when(((OwnableEntity) attacker).getOwner()).thenReturn(intermediary);

        assertSame(player, EntityMultiplierResolver.resolvedTamedOwner((Entity) attacker));
    }

    @Test
    void cyclicOwnerGraphsFailClosed() {
        LivingEntity first = ownableEntity();
        LivingEntity second = ownableEntity();
        when(((OwnableEntity) first).getOwner()).thenReturn(second);
        when(((OwnableEntity) second).getOwner()).thenReturn(first);

        assertNull(EntityMultiplierResolver.resolvedTamedOwner((Entity) first));
    }

    private static LivingEntity ownableEntity() {
        return mock(LivingEntity.class, withSettings().extraInterfaces(OwnableEntity.class));
    }
}
