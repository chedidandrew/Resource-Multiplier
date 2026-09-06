package com.chedidandrew.smartresourcedrops.client;

import java.util.Objects;

import com.chedidandrew.smartresourcedrops.network.ConfigPayload;
import net.minecraft.resources.ResourceLocation;

/** Loader-installed transport for the shared server-authoritative config client. */
public final class ClientNetworkBridge {
    private static volatile Transport transport;

    private ClientNetworkBridge() {
    }

    public static void install(final Transport installedTransport) {
        transport = Objects.requireNonNull(installedTransport, "installedTransport");
    }

    public static boolean canSend(final ResourceLocation type) {
        final Transport current = transport;
        return current != null && current.canSend(type);
    }

    public static void send(final ConfigPayload payload) {
        final Transport current = transport;
        if (current == null) {
            throw new IllegalStateException("Client networking has not been installed by the active loader");
        }
        current.send(payload);
    }

    public interface Transport {
        boolean canSend(ResourceLocation type);

        void send(ConfigPayload payload);
    }
}
