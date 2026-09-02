package com.chedidandrew.smartresourcedrops.client;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.config.ConfigPatch;
import com.chedidandrew.smartresourcedrops.network.ConfigPatchPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigRequestPayload;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketEncoder;
import net.minecraft.network.VarInt;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Sends a deliberately malformed play payload below the normal typed encoder. */
@Mod(value = SmartResourceDrops.MOD_ID, dist = Dist.CLIENT)
public final class NeoForgeOversizedWireClientSmokeTest {
    private static final int TIMEOUT_TICKS = 6_000;
    private static final int SETTLE_TICKS = 40;
    private static final int ATTACK_REQUEST_ID = 0x53524D;
    private static final int OVERSIZED_JSON_LENGTH = ConfigPatch.MAX_JSON_LENGTH + 1;
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    private int ticks;
    private int connectedTicks;
    private volatile boolean wireWriteCompleted;
    private volatile Throwable wireWriteFailure;
    private Connection attackedConnection;
    private boolean attackScheduled;

    public NeoForgeOversizedWireClientSmokeTest() {
        if (Boolean.getBoolean("smart_resource_drops.oversizedWireTest")
                && REGISTERED.compareAndSet(false, true)) {
            NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, this::onClientTick);
        }
    }

    private void onClientTick(final ClientTickEvent.Post event) {
        final Minecraft minecraft = Minecraft.getInstance();
        try {
            if (++this.ticks > TIMEOUT_TICKS) {
                throw new AssertionError("Timed out waiting for oversized wire-payload rejection");
            }
            if (this.wireWriteFailure != null) {
                throw new AssertionError("Could not write hostile wire payload", this.wireWriteFailure);
            }

            if (!this.attackScheduled) {
                scheduleAttackWhenReady(minecraft);
                return;
            }

            if (this.wireWriteCompleted
                    && this.attackedConnection != null
                    && !this.attackedConnection.isConnected()) {
                SmartResourceDrops.LOGGER.info(
                        "NeoForge oversized-wire client smoke passed: {}-character patch reached the wire and the offending connection was rejected",
                        OVERSIZED_JSON_LENGTH);
                minecraft.stop();
            }
        } catch (Throwable failure) {
            SmartResourceDrops.LOGGER.error("NeoForge oversized-wire client smoke test failed", failure);
            throw failure instanceof Error error
                    ? error
                    : new AssertionError("NeoForge oversized-wire client smoke test failed", failure);
        }
    }

    private void scheduleAttackWhenReady(final Minecraft minecraft) {
        final var listener = minecraft.getConnection();
        if (listener == null || minecraft.player == null || minecraft.level == null) {
            this.connectedTicks = 0;
            return;
        }
        if (!listener.hasChannel(ConfigPatchPayload.TYPE)) {
            throw new AssertionError("NeoForge config-patch channel was not negotiated");
        }
        if (++this.connectedTicks < SETTLE_TICKS) {
            return;
        }

        this.attackedConnection = listener.getConnection();
        this.attackScheduled = true;
        writeHostilePayload(this.attackedConnection);
    }

    private void writeHostilePayload(final Connection connection) {
        final Channel channel = connection.channel();
        channel.eventLoop().execute(() -> {
            ByteBuf encodedProbe = null;
            ByteBuf hostilePacket = null;
            try {
                final ChannelHandlerContext encoderContext = channel.pipeline().context("encoder");
                if (encoderContext == null || !(encoderContext.handler() instanceof PacketEncoder<?> packetEncoder)) {
                    throw new IllegalStateException("Active play packet encoder was not available");
                }

                // Ask the active protocol codec for the negotiated custom-payload packet ID,
                // then deliberately bypass the typed ConfigPatchPayload encoder. This keeps
                // the test resilient to Minecraft packet-ID changes while proving the server's
                // decoder, rather than the ordinary client guard, enforces the size boundary.
                encodedProbe = channel.alloc().buffer();
                encodeProbe(packetEncoder, encodedProbe);
                final int customPayloadPacketId = VarInt.read(encodedProbe);

                hostilePacket = channel.alloc().buffer(OVERSIZED_JSON_LENGTH + 128);
                VarInt.write(hostilePacket, customPayloadPacketId);
                final FriendlyByteBuf payload = new FriendlyByteBuf(hostilePacket);
                payload.writeIdentifier(ConfigPatchPayload.TYPE.id());
                payload.writeVarInt(ATTACK_REQUEST_ID);
                payload.writeVarLong(0L);
                payload.writeVarInt(OVERSIZED_JSON_LENGTH);
                hostilePacket.writeZero(OVERSIZED_JSON_LENGTH);

                final ByteBuf submittedPacket = hostilePacket;
                hostilePacket = null;
                SmartResourceDrops.LOGGER.info(
                        "Sending hostile NeoForge config patch below the typed encoder ({} characters; maximum {})",
                        OVERSIZED_JSON_LENGTH,
                        ConfigPatchPayload.MAX_JSON_LENGTH);
                encoderContext.writeAndFlush(submittedPacket).addListener(future -> {
                    if (future.isSuccess()) {
                        this.wireWriteCompleted = true;
                    } else {
                        this.wireWriteFailure = future.cause();
                    }
                });
            } catch (Throwable failure) {
                if (hostilePacket != null) {
                    hostilePacket.release();
                }
                this.wireWriteFailure = failure;
            } finally {
                if (encodedProbe != null) {
                    encodedProbe.release();
                }
            }
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void encodeProbe(final PacketEncoder<?> packetEncoder, final ByteBuf output) {
        final StreamCodec codec = packetEncoder.getProtocolInfo().codec();
        codec.encode(
                output,
                new ServerboundCustomPayloadPacket(new ConfigRequestPayload(ATTACK_REQUEST_ID)));
    }
}
