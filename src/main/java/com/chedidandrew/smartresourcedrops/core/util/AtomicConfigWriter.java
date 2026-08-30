package com.chedidandrew.smartresourcedrops.core.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Crash-safe UTF-8 config writer. */
public final class AtomicConfigWriter {
    private AtomicConfigWriter() {
    }

    public static void write(final Path target, final String content) throws IOException {
        final Path absolute = target.toAbsolutePath();
        final Path parent = absolute.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        final Path temp = absolute.resolveSibling(absolute.getFileName() + ".tmp");
        Files.writeString(temp, content, StandardCharsets.UTF_8);
        try {
            Files.move(temp, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
