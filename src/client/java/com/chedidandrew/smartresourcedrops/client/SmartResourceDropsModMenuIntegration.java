package com.chedidandrew.smartresourcedrops.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Optional Mod Menu integration.
 *
 * <p>This class is only discovered by Mod Menu through the custom "modmenu"
 * entrypoint. Smart Resource Multiplier does not require Mod Menu to run.</p>
 */
public final class SmartResourceDropsModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return SmartDropsConfigScreens::create;
    }
}
