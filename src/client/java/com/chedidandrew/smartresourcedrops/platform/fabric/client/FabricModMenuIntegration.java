package com.chedidandrew.smartresourcedrops.platform.fabric.client;

import com.chedidandrew.smartresourcedrops.client.SmartDropsConfigScreens;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Optional Fabric Mod Menu bridge. */
public final class FabricModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return SmartDropsConfigScreens::create;
    }
}
