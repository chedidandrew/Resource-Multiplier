package com.chedidandrew.smartresourcedrops.packagedprobe;

import com.mojang.logging.LogUtils;
import java.util.concurrent.atomic.AtomicBoolean;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

/** Stops a clean server only after proving the copied production JAR owns the mod classes. */
@Mod(value = "smart_resource_drops_packaged_probe", dist = Dist.DEDICATED_SERVER)
public final class PackagedServerProbe {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicBoolean EXECUTED = new AtomicBoolean();

    public PackagedServerProbe() {
        if (Boolean.getBoolean("smart_resource_drops.packagedServerProbe")) {
            NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, this::onServerTick);
        }
    }

    private void onServerTick(final ServerTickEvent.Post event) {
        if (!EXECUTED.compareAndSet(false, true)) {
            return;
        }
        try {
            PackagedProbeSupport.verifyProductionModAndCodeSource();
            PackagedProbeSupport.verifyProductionConfigExists();
            final var commands = event.getServer().getCommands();
            final var source = event.getServer().createCommandSourceStack();
            final int status = commands.getDispatcher().execute("smartdrops status", source);
            final int validation = commands.getDispatcher().execute("smartdrops validate", source);
            if (status <= 0 || validation <= 0) {
                throw new AssertionError(
                        "Packaged commands failed: status=" + status + ", validate=" + validation);
            }
            PackagedProbeSupport.writeMarker("packaged-server.success");
            LOGGER.info(
                    "Packaged NeoForge server candidate Smart Resource Multiplier 1.3.1+mc1.21.1 passed exact version, code-source, config, status, and validation checks");
            event.getServer().halt(false);
        } catch (Throwable failure) {
            throw failure instanceof Error error
                    ? error
                    : new AssertionError("Packaged NeoForge server probe failed", failure);
        }
    }
}
