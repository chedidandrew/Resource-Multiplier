package com.chedidandrew.smartresourcedrops.config;

/** Selects the immutable authority target used for one configuration screen. */
public final class ConfigScreenOpenPolicy {
    public enum Authority {
        LOCAL_DEFAULTS,
        CONNECTED_SERVER
    }

    public enum InitialPhase {
        READY,
        LOADING
    }

    public record Decision(Authority authority, InitialPhase phase) {
    }

    private ConfigScreenOpenPolicy() {
    }

    public static Decision decide(
            final boolean hasPlayConnection,
            final boolean hasIntegratedServer,
            final boolean hasCachedServerSnapshot
    ) {
        if (!hasPlayConnection && !hasIntegratedServer) {
            return new Decision(Authority.LOCAL_DEFAULTS, InitialPhase.READY);
        }
        return new Decision(
                Authority.CONNECTED_SERVER,
                hasPlayConnection && hasCachedServerSnapshot ? InitialPhase.READY : InitialPhase.LOADING);
    }

    /** Prevents a delayed command callback from hijacking another connection or screen. */
    public static boolean canOpenDelayedCommand(
            final Object expectedConnection,
            final Object activeConnection,
            final Object originatingScreen,
            final Object currentScreen
    ) {
        return expectedConnection != null
                && expectedConnection == activeConnection
                && (currentScreen == null || currentScreen == originatingScreen);
    }
}
