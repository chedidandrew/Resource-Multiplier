package com.chedidandrew.smartresourcedrops.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ConfigScreenOpenPolicyTest {
    @Test
    void titleScreenUsesReadyLocalDefaults() {
        ConfigScreenOpenPolicy.Decision decision = ConfigScreenOpenPolicy.decide(false, false, false);

        assertEquals(ConfigScreenOpenPolicy.Authority.LOCAL_DEFAULTS, decision.authority());
        assertEquals(ConfigScreenOpenPolicy.InitialPhase.READY, decision.phase());
    }

    @Test
    void connectedServerWithoutCacheStartsLoading() {
        ConfigScreenOpenPolicy.Decision decision = ConfigScreenOpenPolicy.decide(true, false, false);

        assertEquals(ConfigScreenOpenPolicy.Authority.CONNECTED_SERVER, decision.authority());
        assertEquals(ConfigScreenOpenPolicy.InitialPhase.LOADING, decision.phase());
    }

    @Test
    void cachedSnapshotStartsReadyButRemainsServerAuthoritative() {
        ConfigScreenOpenPolicy.Decision decision = ConfigScreenOpenPolicy.decide(true, false, true);

        assertEquals(ConfigScreenOpenPolicy.Authority.CONNECTED_SERVER, decision.authority());
        assertEquals(ConfigScreenOpenPolicy.InitialPhase.READY, decision.phase());
    }

    @Test
    void startingIntegratedServerNeverFallsBackToLocalDefaults() {
        ConfigScreenOpenPolicy.Decision decision = ConfigScreenOpenPolicy.decide(false, true, true);

        assertEquals(ConfigScreenOpenPolicy.Authority.CONNECTED_SERVER, decision.authority());
        assertEquals(ConfigScreenOpenPolicy.InitialPhase.LOADING, decision.phase());
    }

    @Test
    void delayedCommandOnlyOpensForTheSameConnectionAndUnchangedScreenFlow() {
        final Object connection = new Object();
        final Object commandScreen = new Object();

        assertTrue(ConfigScreenOpenPolicy.canOpenDelayedCommand(
                connection, connection, commandScreen, null));
        assertTrue(ConfigScreenOpenPolicy.canOpenDelayedCommand(
                connection, connection, commandScreen, commandScreen));
        assertFalse(ConfigScreenOpenPolicy.canOpenDelayedCommand(
                connection, new Object(), commandScreen, null));
        assertFalse(ConfigScreenOpenPolicy.canOpenDelayedCommand(
                connection, connection, commandScreen, new Object()));
        assertFalse(ConfigScreenOpenPolicy.canOpenDelayedCommand(
                null, null, commandScreen, null));
    }
}
