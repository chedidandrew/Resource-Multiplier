package com.chedidandrew.smartresourcedrops.optionaltest;

import java.util.List;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Loader-independent identifiers used without loading the production mod. */
final class OptionalChannelIds {
    static final String PRODUCTION_MOD_ID = "smart_resource_drops";
    static final String PROBE_MOD_ID = "smart_resource_drops_optional_channel_probe";

    private OptionalChannelIds() {
    }

    static List<CustomPacketPayload.Type<?>> clientToServer() {
        return List.of(
                type("config_request"),
                type("config_patch"),
                type("config_reset"));
    }

    static List<CustomPacketPayload.Type<?>> serverToClient() {
        return List.of(
                type("config_snapshot"),
                type("config_invalidation"),
                type("config_mutation_result"));
    }

    private static CustomPacketPayload.Type<?> type(final String path) {
        return new CustomPacketPayload.Type<>(
                Identifier.fromNamespaceAndPath(PRODUCTION_MOD_ID, path));
    }
}
