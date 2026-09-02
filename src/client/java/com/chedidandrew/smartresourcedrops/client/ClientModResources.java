package com.chedidandrew.smartresourcedrops.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

/** Loader-neutral view of exact resources contributed by installed mod files. */
public final class ClientModResources {
    private static volatile Locator locator;

    private ClientModResources() {
    }

    public static void install(final Locator installedLocator) {
        locator = Objects.requireNonNull(installedLocator, "installedLocator");
    }

    static List<Resource> findAll(final String relativePath) {
        final Locator current = locator;
        if (current == null) {
            throw new IllegalStateException("Installed-mod resource discovery has not been configured");
        }
        return List.copyOf(current.findAll(relativePath));
    }

    @FunctionalInterface
    public interface Locator {
        List<Resource> findAll(String relativePath);
    }

    @FunctionalInterface
    public interface StreamOpener {
        InputStream open() throws IOException;
    }

    public record Resource(String source, StreamOpener opener) {
        public Resource {
            source = Objects.requireNonNullElse(source, "unknown resource");
            opener = Objects.requireNonNull(opener, "opener");
        }

        public InputStream open() throws IOException {
            return opener.open();
        }

        @Override
        public String toString() {
            return source;
        }
    }
}
