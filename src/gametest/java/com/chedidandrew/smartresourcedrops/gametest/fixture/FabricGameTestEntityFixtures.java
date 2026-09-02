package com.chedidandrew.smartresourcedrops.gametest.fixture;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

/** Fabric adapter for the shared entity GameTest fixture definitions and state. */
public final class FabricGameTestEntityFixtures implements ModInitializer {
    @Override
    public void onInitialize() {
        GameTestEntityFixtures.registerEntities((key, type) ->
                Registry.register(BuiltInRegistries.ENTITY_TYPE, key, type));
        for (EntityType<? extends Mob> type : GameTestEntityFixtures.mobTypes()) {
            FabricDefaultAttributeRegistry.register(type, Mob.createMobAttributes());
        }

        LootTableEvents.MODIFY_DROPS.register((table, context, drops) -> {
            GameTestEntityFixtures.applyFinalDropFixtures(
                    table.is(GameTestEntityFixtures.COMPONENT_RICH_LOOT),
                    table.is(GameTestEntityFixtures.NESTED_OUTER_LOOT),
                    context,
                    drops);
            if (table.is(GameTestEntityFixtures.EXCEPTION_LOOT)) {
                GameTestEntityFixtures.throwIfArmedExceptionLoot();
            }
        });
    }
}
