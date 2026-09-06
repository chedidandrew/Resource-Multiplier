package com.chedidandrew.smartresourcedrops.optionaltest;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

/** Loader-independent identifiers used without loading the production mod. */
final class OptionalChannelIds {
    static final String PRODUCTION_MOD_ID = "smart_resource_drops";
    static final String PROBE_MOD_ID = "smart_resource_drops_optional_channel_probe";

    private OptionalChannelIds() {
    }

    static List<ResourceLocation> clientToServer() {
        return List.of(
                type("config_request"),
                type("config_patch"),
                type("config_reset"));
    }

    static List<ResourceLocation> serverToClient() {
        return List.of(
                type("config_snapshot"),
                type("config_invalidation"),
                type("config_mutation_result"));
    }

    private static ResourceLocation type(final String path) {
        return new ResourceLocation(PRODUCTION_MOD_ID, path);
    }
}
