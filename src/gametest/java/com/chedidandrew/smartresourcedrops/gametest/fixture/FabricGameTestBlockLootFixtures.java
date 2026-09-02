package com.chedidandrew.smartresourcedrops.gametest.fixture;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;

/** Fabric adapter for the shared pathological block-loot fixture state. */
public final class FabricGameTestBlockLootFixtures implements ModInitializer {
    @Override
    public void onInitialize() {
        LootTableEvents.MODIFY_DROPS.register((table, context, drops) ->
                GameTestBlockLootFixtures.appendPathologicalDropsIfArmed(
                        table.is(GameTestBlockLootFixtures.DIRT_LOOT),
                        drops));
    }
}
