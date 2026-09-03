package com.chedidandrew.smartresourcedrops.packagedprobe;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import net.neoforged.fml.ModList;

final class PackagedProbeSupport {
    private static final String EXPECTED_VERSION = "1.3.1+mc1.21.1";

    private PackagedProbeSupport() {
    }

    static void verifyProductionModAndCodeSource() throws Exception {
        final var container = ModList.get()
                .getModContainerById("smart_resource_drops")
                .orElseThrow(() -> new AssertionError("Production mod was not discovered from mods/"));
        final String actualVersion = container.getModInfo().getVersion().toString();
        if (!EXPECTED_VERSION.equals(actualVersion)) {
            throw new AssertionError(
                    "Expected production version " + EXPECTED_VERSION + ", found " + actualVersion);
        }

        final String configuredJar = System.getProperty("smart_resource_drops.packagedCandidateJar");
        if (configuredJar == null || configuredJar.isBlank()) {
            throw new AssertionError("Missing packaged candidate JAR property");
        }
        final Path expectedJar = Path.of(configuredJar).toAbsolutePath().normalize();
        // ModLauncher exposes transformed classes through its union filesystem, so
        // ProtectionDomain#getCodeSource may resolve to the union root instead of
        // the physical JAR. The owning ModFile is the authoritative source selected
        // by FML for this exact mod container.
        final Path actualJar = container.getModInfo()
                .getOwningFile()
                .getFile()
                .getFilePath()
                .toAbsolutePath()
                .normalize();
        if (!Files.isSameFile(expectedJar, actualJar)) {
            throw new AssertionError(
                    "Production class loaded from " + actualJar + " instead of copied candidate " + expectedJar);
        }
        final Class<?> entrypoint = Class.forName(
                "com.chedidandrew.smartresourcedrops.platform.neoforge.NeoForgeEntrypoint");
        if (!"smart_resource_drops".equals(entrypoint.getModule().getName())) {
            throw new AssertionError(
                    "Production entrypoint was not transformed into the smart_resource_drops module: "
                            + entrypoint.getModule());
        }
    }

    static void writeMarker(final String markerName) throws Exception {
        final String configuredDirectory = System.getProperty("smart_resource_drops.packagedTestDirectory");
        if (configuredDirectory == null || configuredDirectory.isBlank()) {
            throw new AssertionError("Missing packaged test directory property");
        }
        Files.writeString(
                Path.of(configuredDirectory).resolve(markerName),
                "pass\n",
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
    }

    static void verifyProductionConfigExists() {
        final String configuredDirectory = System.getProperty("smart_resource_drops.packagedTestDirectory");
        if (configuredDirectory == null || configuredDirectory.isBlank()) {
            throw new AssertionError("Missing packaged test directory property");
        }
        final Path config = Path.of(configuredDirectory)
                .resolve("config")
                .resolve("smart_resource_drops.json");
        if (!Files.isRegularFile(config)) {
            throw new AssertionError("Production config was not created by the packaged candidate: " + config);
        }
    }
}
