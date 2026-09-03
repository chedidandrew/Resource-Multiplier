package com.chedidandrew.smartresourcedrops.platform.neoforge;

import net.minecraft.world.entity.Shearable;
import net.minecraftforge.common.IForgeShearable;

/** Assigns dual-interface targets to exactly one shearing multiplication path. */
public final class NeoForgeShearingRoute {
    private NeoForgeShearingRoute() {
    }

    public static boolean ownsForgeOnlyPath(final IForgeShearable shearable) {
        return !(shearable instanceof Shearable);
    }
}
