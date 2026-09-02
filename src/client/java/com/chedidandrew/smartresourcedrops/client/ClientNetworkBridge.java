package com.chedidandrew.smartresourcedrops.client;

import java.util.Objects;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Loader-installed transport for the shared server-authoritative config client. */
public final class ClientNetworkBridge {
    private static volatile Transport transport;

    private ClientNetworkBridge() {
    }

    public static void install(final Transport installedTransport) {
        transport = Objects.requireNonNull(installedTransport, "installedTransport");
    }

    public static boolean canSend(final CustomPacketPayload.Type<?> type) {
        final Transport current = transport;
        return current != null && current.canSend(type);
    }

    public static void send(final CustomPacketPayload payload) {
        final Transport current = transport;
        if (current == null) {
            throw new IllegalStateException("Client networking has not been installed by the active loader");
        }
        current.send(payload);
    }

    public interface Transport {
        boolean canSend(CustomPacketPayload.Type<?> type);

        void send(CustomPacketPayload payload);
    }
}
