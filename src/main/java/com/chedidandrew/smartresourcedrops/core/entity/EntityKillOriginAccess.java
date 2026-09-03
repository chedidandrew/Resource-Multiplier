package com.chedidandrew.smartresourcedrops.core.entity;

import net.minecraft.world.entity.player.Player;

/** Mixin bridge that preserves whether vanilla's remembered player was direct or tamed. */
public interface EntityKillOriginAccess {
    EntityKillAttribution.Kind smartResourceDrops$rememberedKillOrigin();

    boolean smartResourceDrops$hasRememberedPlayerKill();

    Player smartResourceDrops$rememberedPlayer();
}
