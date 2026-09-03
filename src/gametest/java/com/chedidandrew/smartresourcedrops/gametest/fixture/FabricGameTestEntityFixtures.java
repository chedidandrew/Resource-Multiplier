package com.chedidandrew.smartresourcedrops.gametest.fixture;

import com.chedidandrew.smartresourcedrops.platform.PlatformPlayerSupport;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

/** Fabric adapter for the shared entity GameTest fixture definitions and state. */
public final class FabricGameTestEntityFixtures implements ModInitializer {
    @Override
    public void onInitialize() {
        // Fabric's old helper implements its synthetic interactive player as a
        // FakePlayer. Exempt only the fixed GameTest profile; every other
        // Fabric FakePlayer remains automation and is denied player authority.
        PlatformPlayerSupport.installFakePlayerPredicate(player ->
                player instanceof FakePlayer
                        && !"test-mock-player".equals(player.getScoreboardName()));
        GameTestEntityFixtures.registerEntities((key, type) ->
                Registry.register(BuiltInRegistries.ENTITY_TYPE, key, type));
        for (EntityType<? extends Mob> type : GameTestEntityFixtures.mobTypes()) {
            FabricDefaultAttributeRegistry.register(type, Mob.createMobAttributes());
        }
    }
}
