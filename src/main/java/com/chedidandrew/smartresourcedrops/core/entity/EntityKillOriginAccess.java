package com.chedidandrew.smartresourcedrops.core.entity;

import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/** Mixin bridge that preserves whether vanilla's remembered player was direct or tamed. */
public interface EntityKillOriginAccess {
    EntityKillAttribution.Kind smartResourceDrops$rememberedKillOrigin();

    boolean smartResourceDrops$hasRememberedPlayer();

    @Nullable Player smartResourceDrops$rememberedPlayer();
}
