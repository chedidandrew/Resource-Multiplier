package com.chedidandrew.smartresourcedrops.legacy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

public final class LegacyNetworkTest {
    @Test
    public void configMessageRoundTripsBeyondOldForgeStringLimit() {
        StringBuilder json = new StringBuilder();
        for (int index = 0; index < 20000; index++) json.append((char) ('a' + index % 26));
        LegacyNetwork.ConfigMessage original =
                new LegacyNetwork.ConfigMessage(LegacyNetwork.APPLY, true, json.toString());
        ByteBuf buffer = Unpooled.buffer();
        original.toBytes(buffer);

        LegacyNetwork.ConfigMessage decoded = new LegacyNetwork.ConfigMessage();
        decoded.fromBytes(buffer);
        assertEquals(LegacyNetwork.APPLY, decoded.action);
        assertEquals(true, decoded.editable);
        assertEquals(json.toString(), decoded.json);
    }

    @Test
    public void configMessageRejectsInvalidDeclaredLength() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(LegacyNetwork.APPLY);
        buffer.writeBoolean(true);
        buffer.writeInt(262145);
        try {
            new LegacyNetwork.ConfigMessage().fromBytes(buffer);
            fail("Oversized packet should be rejected");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    @Test
    public void configMessageRejectsTrailingPayloadBytes() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(LegacyNetwork.REQUEST);
        buffer.writeBoolean(false);
        buffer.writeInt(0);
        buffer.writeByte(99);
        try {
            new LegacyNetwork.ConfigMessage().fromBytes(buffer);
            fail("Trailing packet data should be rejected");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    @Test
    public void serverOnlyAcceptsRequestAndApplyActionsFromClients() {
        assertTrue(LegacyNetwork.ConfigMessage.isServerRequestAction(LegacyNetwork.REQUEST));
        assertTrue(LegacyNetwork.ConfigMessage.isServerRequestAction(LegacyNetwork.APPLY));
        assertFalse(LegacyNetwork.ConfigMessage.isServerRequestAction(LegacyNetwork.SNAPSHOT));
        assertFalse(LegacyNetwork.ConfigMessage.isServerRequestAction(LegacyNetwork.REJECTED));
        assertFalse(LegacyNetwork.ConfigMessage.isServerRequestAction(255));
    }

    @Test
    public void onlyOnePendingConfigTaskPerPlayerIsAccepted() {
        final AtomicInteger executions = new AtomicInteger();
        UUID player = UUID.randomUUID();
        LegacyServerTaskQueue queue = new LegacyServerTaskQueue(128);
        assertTrue(queue.offer(player, new Runnable() {
            @Override public void run() { executions.incrementAndGet(); }
        }));
        assertFalse(queue.offer(player, new Runnable() {
            @Override public void run() { executions.incrementAndGet(); }
        }));

        queue.drain();
        assertEquals(1, executions.get());
        assertTrue(queue.offer(player, new Runnable() {
            @Override public void run() { executions.incrementAndGet(); }
        }));
        queue.drain();
        assertEquals(2, executions.get());
    }

    @Test
    public void oneFailedTaskDoesNotStarveOthersOrLeakPlayerSlot() {
        final AtomicInteger executions = new AtomicInteger();
        final UUID failingPlayer = UUID.randomUUID();
        LegacyServerTaskQueue queue = new LegacyServerTaskQueue(2);
        assertTrue(queue.offer(failingPlayer, new Runnable() {
            @Override public void run() { throw new IllegalStateException("expected test failure"); }
        }));
        assertTrue(queue.offer(UUID.randomUUID(), new Runnable() {
            @Override public void run() { executions.incrementAndGet(); }
        }));

        queue.drain();
        assertEquals(1, executions.get());
        assertTrue(queue.offer(failingPlayer, new Runnable() {
            @Override public void run() { executions.incrementAndGet(); }
        }));
        queue.drain();
        assertEquals(2, executions.get());
    }
}
