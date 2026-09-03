package com.chedidandrew.smartresourcedrops.client;

import java.util.List;
import java.util.Random;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.network.ConfigPatchFragmentPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigPatchPayload;
import com.chedidandrew.smartresourcedrops.network.ConfigTransferCodec;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Sends individually valid but malicious fragment sequences through Forge's real channel. */
@Mod.EventBusSubscriber(modid = SmartResourceDrops.MOD_ID, value = Dist.CLIENT)
public final class NeoForgeOversizedWireClientSmokeTest {
    private static final int TIMEOUT_TICKS = 6_000;
    private static final int OP_WAIT_TICKS = 180;
    private static final int TRANSFER_EXPIRY_WAIT_TICKS = 430;
    private static final NeoForgeOversizedWireClientSmokeTest INSTANCE =
            new NeoForgeOversizedWireClientSmokeTest();

    private int ticks;
    private int connectedTicks;
    private int attackTicks;
    private Phase phase = Phase.WAIT_CONNECTION;

    private NeoForgeOversizedWireClientSmokeTest() {
    }

    @SubscribeEvent
    public static void tick(final TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END
                && Boolean.getBoolean("smart_resource_drops.oversizedWireTest")) {
            INSTANCE.onClientTick();
        }
    }

    private void onClientTick() {
        final Minecraft minecraft = Minecraft.getInstance();
        try {
            if (++ticks > TIMEOUT_TICKS) {
                throw new AssertionError("Timed out during malformed-fragment gate at " + phase);
            }
            switch (phase) {
                case WAIT_CONNECTION -> waitForConnection(minecraft);
                case WAIT_EXPIRY -> waitForExpiry(minecraft);
                case WAIT_HEALTHY_SNAPSHOT -> waitForHealthySnapshot(minecraft);
                case COMPLETE -> {
                    // Minecraft is stopping.
                }
            }
        } catch (Throwable failure) {
            SmartResourceDrops.LOGGER.error("NeoForge malformed-fragment client smoke failed", failure);
            throw failure instanceof Error error
                    ? error
                    : new AssertionError("NeoForge malformed-fragment client smoke failed", failure);
        }
    }

    private void waitForConnection(final Minecraft minecraft) {
        if (minecraft.getConnection() == null || minecraft.player == null || minecraft.level == null) {
            connectedTicks = 0;
            return;
        }
        if (!ClientNetworkBridge.canSend(ConfigPatchFragmentPayload.TYPE)) {
            throw new AssertionError("Forge config fragment channel was not negotiated");
        }
        if (++connectedTicks < OP_WAIT_TICKS) {
            return;
        }

        final List<ConfigPatchFragmentPayload> duplicateFrames = ConfigPatchFragmentPayload.encode(
                new ConfigPatchPayload(0x535240, 0L, pseudoRandomAscii(90_000)));
        if (duplicateFrames.size() < 2) {
            throw new AssertionError("Malicious duplicate fixture did not create multiple fragments");
        }
        ClientNetworkBridge.send(duplicateFrames.get(0));
        ClientNetworkBridge.send(duplicateFrames.get(0));

        final List<ConfigPatchFragmentPayload> mixedFrames = ConfigPatchFragmentPayload.encode(
                new ConfigPatchPayload(0x535241, 0L, pseudoRandomAscii(90_000)));
        ClientNetworkBridge.send(mixedFrames.get(0));
        final ConfigPatchFragmentPayload second = mixedFrames.get(1);
        ClientNetworkBridge.send(new ConfigPatchFragmentPayload(
                second.format(),
                second.requestId(),
                second.expectedRevision() + 1L,
                second.compression(),
                second.rawBytes(),
                second.encodedBytes(),
                second.chunkIndex(),
                second.chunkCount(),
                second.chunk()));

        for (int attempt = 0; attempt < 8; attempt++) {
            ClientNetworkBridge.send(new ConfigPatchFragmentPayload(
                    ConfigTransferCodec.FORMAT,
                    0x535242 + attempt,
                    0L,
                    ConfigTransferCodec.Compression.ZLIB,
                    ConfigTransferCodec.RAW_UTF8_MAX,
                    2,
                    0,
                    1,
                    new byte[] {0, 0}));
        }

        final List<ConfigPatchFragmentPayload> incomplete = ConfigPatchFragmentPayload.encode(
                new ConfigPatchPayload(0x535243, 0L, pseudoRandomAscii(90_000)));
        ClientNetworkBridge.send(incomplete.get(0));
        SmartResourceDrops.LOGGER.info(
                "Sent duplicate, mixed-metadata, repeated max-inflate, and incomplete Forge fragment attacks");
        phase = Phase.WAIT_EXPIRY;
        attackTicks = 0;
    }

    private void waitForExpiry(final Minecraft minecraft) {
        if (minecraft.getConnection() == null || minecraft.player == null) {
            throw new AssertionError("Malformed fragments disconnected the Forge client");
        }
        if (++attackTicks < TRANSFER_EXPIRY_WAIT_TICKS) {
            return;
        }
        final ClientConfigState.RequestStart request = ClientConfigState.request(minecraft);
        if (!request.started()) {
            throw new AssertionError("Healthy request could not start after fragment attacks: " + request.failure());
        }
        phase = Phase.WAIT_HEALTHY_SNAPSHOT;
    }

    private void waitForHealthySnapshot(final Minecraft minecraft) {
        if (minecraft.screen instanceof SmartDropsConfigLoadingScreen) {
            return;
        }
        if (!(minecraft.screen instanceof SmartDropsConfigScreen)) {
            return;
        }
        if (ClientConfigState.cachedSnapshot(minecraft).isEmpty()) {
            throw new AssertionError("Healthy snapshot was not accepted after fragment attacks");
        }
        SmartResourceDrops.LOGGER.info(
                "NeoForge oversized-wire client smoke passed: malformed fragments failed closed and a later fragmented snapshot succeeded");
        phase = Phase.COMPLETE;
        minecraft.stop();
    }

    private static String pseudoRandomAscii(final int length) {
        final Random random = new Random(0x5a17c0deL);
        final String alphabet = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_";
        final StringBuilder value = new StringBuilder(length);
        while (value.length() < length) {
            value.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return value.toString();
    }

    private enum Phase {
        WAIT_CONNECTION,
        WAIT_EXPIRY,
        WAIT_HEALTHY_SNAPSHOT,
        COMPLETE
    }
}
