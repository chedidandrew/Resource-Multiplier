package com.chedidandrew.smartresourcedrops.config;

/**
 * Client-independent request generation tracker used by the configuration GUI.
 * Connection objects are deliberately compared by identity so a response from
 * a previous play session can never satisfy a request in a new session.
 */
public final class ConfigRequestLifecycle {
    public enum Phase {
        IDLE,
        LOADING,
        READY,
        ERROR,
        CLOSED
    }

    public enum Failure {
        NONE,
        NO_CONNECTION,
        CHANNEL_UNAVAILABLE,
        INVALID_PATCH,
        SEND_FAILED,
        TIMEOUT,
        DISCONNECTED,
        INVALID_RESPONSE
    }

    private int nextRequestId;
    private int currentRequestId = -1;
    private Object currentConnection;
    private Phase phase = Phase.IDLE;
    private Failure failure = Failure.NONE;

    public synchronized int begin(final Object connection) {
        if (connection == null) {
            throw new IllegalArgumentException("A server request requires a connection");
        }
        nextRequestId = nextRequestId == Integer.MAX_VALUE ? 1 : nextRequestId + 1;
        currentRequestId = nextRequestId;
        currentConnection = connection;
        phase = Phase.LOADING;
        failure = Failure.NONE;
        return currentRequestId;
    }

    public synchronized boolean isCurrent(final int requestId, final Object connection) {
        return phase == Phase.LOADING
                && currentRequestId == requestId
                && currentConnection == connection;
    }

    public synchronized boolean accept(final int requestId, final Object connection) {
        if (!isCurrent(requestId, connection)) {
            return false;
        }
        clearCurrent();
        phase = Phase.READY;
        failure = Failure.NONE;
        return true;
    }

    public synchronized boolean fail(
            final int requestId,
            final Object connection,
            final Failure reason
    ) {
        if (!isCurrent(requestId, connection)) {
            return false;
        }
        clearCurrent();
        phase = Phase.ERROR;
        failure = reason == null ? Failure.SEND_FAILED : reason;
        return true;
    }

    public synchronized boolean cancel(final int requestId) {
        if (phase != Phase.LOADING || currentRequestId != requestId) {
            return false;
        }
        clearCurrent();
        phase = Phase.CLOSED;
        failure = Failure.NONE;
        return true;
    }

    public synchronized boolean disconnect(final Object connection) {
        if (phase != Phase.LOADING || currentConnection != connection) {
            return false;
        }
        clearCurrent();
        phase = Phase.ERROR;
        failure = Failure.DISCONNECTED;
        return true;
    }

    public synchronized Phase phase() {
        return phase;
    }

    public synchronized Failure failure() {
        return failure;
    }

    public synchronized int currentRequestId() {
        return currentRequestId;
    }

    private void clearCurrent() {
        currentRequestId = -1;
        currentConnection = null;
    }
}
