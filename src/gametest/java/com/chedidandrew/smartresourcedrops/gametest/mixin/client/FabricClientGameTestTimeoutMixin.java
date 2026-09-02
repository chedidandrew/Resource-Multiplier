package com.chedidandrew.smartresourcedrops.gametest.mixin.client;

import net.fabricmc.fabric.impl.client.gametest.util.ClientGameTestImpl;
import net.minecraft.SharedConstants;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Gives Fabric's client-test world builder enough time on cold software-rendered CI runners.
 *
 * <p>Fabric API 0.141.6+1.21.11 hard-codes a one-minute limit inside
 * {@link ClientGameTestImpl#waitForWorldLoad}. That can expire under Xvfb before an otherwise
 * healthy integrated server finishes its first world load. This mixin belongs only to the
 * unpublished GameTest mod and does not change the production mod or its runtime behavior.
 */
@Mixin(value = ClientGameTestImpl.class, remap = false)
abstract class FabricClientGameTestTimeoutMixin {
    private static final int WORLD_LOAD_TIMEOUT_TICKS = 5 * SharedConstants.TICKS_PER_MINUTE;

    @ModifyConstant(
            method = "waitForWorldLoad",
            constant = @Constant(intValue = SharedConstants.TICKS_PER_MINUTE),
            remap = false
    )
    private static int smartResourceDrops$extendWorldLoadTimeout(final int originalTimeout) {
        return WORLD_LOAD_TIMEOUT_TICKS;
    }
}
