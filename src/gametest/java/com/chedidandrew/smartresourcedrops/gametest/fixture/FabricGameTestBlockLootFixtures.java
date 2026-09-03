package com.chedidandrew.smartresourcedrops.gametest.fixture;

import net.fabricmc.api.ModInitializer;
/** Fabric entrypoint retained for the target-native test fixture module. */
public final class FabricGameTestBlockLootFixtures implements ModInitializer {
    @Override
    public void onInitialize() {
        // Fabric API 0.116 has no final-drop event. The GameTest-only LootTable
        // mixin applies the fixture after vanilla generation instead.
    }
}
