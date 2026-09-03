package com.chedidandrew.smartresourcedrops.gametest.fixture;

import net.fabricmc.api.ModInitializer;

/** Fabric adapter for the shared pathological block-loot fixture state. */
public final class FabricGameTestBlockLootFixtures implements ModInitializer {
    @Override
    public void onInitialize() {
        // Runtime final-drop instrumentation is supplied by the dedicated GameTest mixin.
    }
}
