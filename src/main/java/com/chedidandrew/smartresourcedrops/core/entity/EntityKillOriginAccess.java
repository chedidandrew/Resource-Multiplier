package com.chedidandrew.smartresourcedrops.core.entity;

/** Mixin bridge that preserves whether vanilla's remembered player was direct or tamed. */
public interface EntityKillOriginAccess {
    EntityKillAttribution.Kind smartResourceDrops$rememberedKillOrigin();
}
