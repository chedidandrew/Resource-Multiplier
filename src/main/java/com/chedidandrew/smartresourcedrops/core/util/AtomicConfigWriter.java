package com.chedidandrew.smartresourcedrops.core.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Atomic UTF-8 config replacement with bounded handling for transient Windows file sharing. */
public final class AtomicConfigWriter {
    private static final int MAX_MOVE_ATTEMPTS = 5;

    @FunctionalInterface
    interface MoveOperation {
        void move(Path source, Path target) throws IOException;
    }

    @FunctionalInterface
    interface RetrySleeper {
        void sleep(long millis) throws InterruptedException;
    }

    private AtomicConfigWriter() {
    }

    public static void write(final Path target, final String content) throws IOException {
        final Path absolute = target.toAbsolutePath();
        final Path parent = absolute.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        final Path temp = Files.createTempFile(
                parent,
                absolute.getFileName() + ".",
                ".tmp");
        try {
            Files.writeString(temp, content, StandardCharsets.UTF_8);
            moveWithBoundedSharingViolationRetry(temp, absolute);
        } catch (IOException failure) {
            try {
                Files.deleteIfExists(temp);
            } catch (IOException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
    }

    private static void moveWithBoundedSharingViolationRetry(
            final Path temp,
            final Path target
    ) throws IOException {
        moveWithBoundedSharingViolationRetry(temp, target, AtomicConfigWriter::moveOnce, Thread::sleep);
    }

    static void moveWithBoundedSharingViolationRetry(
            final Path temp,
            final Path target,
            final MoveOperation moveOperation,
            final RetrySleeper sleeper
    ) throws IOException {
        FileSystemException firstSharingFailure = null;
        for (int attempt = 0; attempt < MAX_MOVE_ATTEMPTS; attempt++) {
            try {
                moveOperation.move(temp, target);
                return;
            } catch (FileSystemException sharingFailure) {
                if (firstSharingFailure == null) {
                    firstSharingFailure = sharingFailure;
                }
                if (attempt + 1 == MAX_MOVE_ATTEMPTS) {
                    if (sharingFailure != firstSharingFailure) {
                        sharingFailure.addSuppressed(firstSharingFailure);
                    }
                    throw sharingFailure;
                }
                try {
                    sleeper.sleep(25L * (attempt + 1));
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    final IOException failure = new IOException(
                            "Interrupted while retrying atomic configuration replacement",
                            interrupted);
                    failure.addSuppressed(sharingFailure);
                    if (firstSharingFailure != sharingFailure) {
                        failure.addSuppressed(firstSharingFailure);
                    }
                    throw failure;
                }
            }
        }
        throw new IOException("Configuration replacement failed without an I/O cause");
    }

    private static void moveOnce(final Path temp, final Path target) throws IOException {
        try {
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
