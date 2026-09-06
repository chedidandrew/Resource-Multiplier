package com.chedidandrew.smartresourcedrops.legacy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
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
}
