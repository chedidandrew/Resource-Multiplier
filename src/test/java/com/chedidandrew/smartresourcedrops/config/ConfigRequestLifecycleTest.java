package com.chedidandrew.smartresourcedrops.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ConfigRequestLifecycleTest {
    @Test
    void connectedRequestLoadsAndMatchingResponseBecomesReady() {
        ConfigRequestLifecycle lifecycle = new ConfigRequestLifecycle();
        Object connection = new Object();

        int request = lifecycle.begin(connection);

        assertEquals(ConfigRequestLifecycle.Phase.LOADING, lifecycle.phase());
        assertTrue(lifecycle.isCurrent(request, connection));
        assertTrue(lifecycle.accept(request, connection));
        assertEquals(ConfigRequestLifecycle.Phase.READY, lifecycle.phase());
        assertEquals(-1, lifecycle.currentRequestId());
    }

    @Test
    void retrySupersedesOldResponseAndAcceptsCurrentResponse() {
        ConfigRequestLifecycle lifecycle = new ConfigRequestLifecycle();
        Object connection = new Object();
        int first = lifecycle.begin(connection);
        int retry = lifecycle.begin(connection);

        assertNotEquals(first, retry);
        assertFalse(lifecycle.accept(first, connection));
        assertEquals(ConfigRequestLifecycle.Phase.LOADING, lifecycle.phase());
        assertTrue(lifecycle.accept(retry, connection));
        assertEquals(ConfigRequestLifecycle.Phase.READY, lifecycle.phase());
    }

    @Test
    void responseFromPreviousConnectionIsIgnored() {
        ConfigRequestLifecycle lifecycle = new ConfigRequestLifecycle();
        Object oldConnection = new Object();
        Object newConnection = new Object();
        int request = lifecycle.begin(oldConnection);

        assertFalse(lifecycle.accept(request, newConnection));
        assertTrue(lifecycle.isCurrent(request, oldConnection));
    }

    @Test
    void closingBeforeResponsePreventsReopen() {
        ConfigRequestLifecycle lifecycle = new ConfigRequestLifecycle();
        Object connection = new Object();
        int request = lifecycle.begin(connection);

        assertTrue(lifecycle.cancel(request));
        assertEquals(ConfigRequestLifecycle.Phase.CLOSED, lifecycle.phase());
        assertFalse(lifecycle.accept(request, connection));
    }

    @Test
    void invalidResponseTransitionsToExplicitError() {
        ConfigRequestLifecycle lifecycle = new ConfigRequestLifecycle();
        Object connection = new Object();
        int request = lifecycle.begin(connection);

        assertTrue(lifecycle.fail(
                request,
                connection,
                ConfigRequestLifecycle.Failure.INVALID_RESPONSE));
        assertEquals(ConfigRequestLifecycle.Phase.ERROR, lifecycle.phase());
        assertEquals(ConfigRequestLifecycle.Failure.INVALID_RESPONSE, lifecycle.failure());
        assertFalse(lifecycle.accept(request, connection));
    }

    @Test
    void disconnectWhileLoadingInvalidatesRequest() {
        ConfigRequestLifecycle lifecycle = new ConfigRequestLifecycle();
        Object connection = new Object();
        int request = lifecycle.begin(connection);

        assertTrue(lifecycle.disconnect(connection));
        assertEquals(ConfigRequestLifecycle.Phase.ERROR, lifecycle.phase());
        assertEquals(ConfigRequestLifecycle.Failure.DISCONNECTED, lifecycle.failure());
        assertFalse(lifecycle.accept(request, connection));
    }
}
