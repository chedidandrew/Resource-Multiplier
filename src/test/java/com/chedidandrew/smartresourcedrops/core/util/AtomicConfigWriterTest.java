package com.chedidandrew.smartresourcedrops.core.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AtomicConfigWriterTest {
    @AfterEach
    void clearInterruptedStatus() {
        Thread.interrupted();
    }

    @Test
    void replacesRealTargetAndLeavesNoTemporarySibling(@TempDir final Path directory)
            throws IOException {
        final Path target = directory.resolve("smart_resource_drops.json");
        Files.writeString(target, "old");

        AtomicConfigWriter.write(target, "new");

        assertEquals("new", Files.readString(target));
        try (var files = Files.list(directory)) {
            assertEquals(List.of(target), files.toList());
        }
    }

    @Test
    void retriesTransientAccessDenialWithTheSameCompletedTempFile() throws IOException {
        Path temp = Path.of("finished-config.tmp");
        Path target = Path.of("smart_resource_drops.json");
        AtomicInteger attempts = new AtomicInteger();
        List<Path> attemptedSources = new ArrayList<>();
        List<Long> delays = new ArrayList<>();

        AtomicConfigWriter.moveWithBoundedSharingViolationRetry(
                temp,
                target,
                (source, destination) -> {
                    attemptedSources.add(source);
                    int attempt = attempts.incrementAndGet();
                    if (attempt < 3) {
                        throw new AccessDeniedException(source.toString(), destination.toString(), "locked");
                    }
                },
                delays::add);

        assertEquals(3, attempts.get());
        assertEquals(List.of(temp, temp, temp), attemptedSources);
        assertEquals(List.of(25L, 50L), delays);
    }

    @Test
    void doesNotRetryUnrelatedIoFailures() {
        AtomicInteger attempts = new AtomicInteger();
        IOException expected = new IOException("disk full");

        IOException actual = assertThrows(IOException.class, () ->
                AtomicConfigWriter.moveWithBoundedSharingViolationRetry(
                        Path.of("temp"),
                        Path.of("target"),
                        (source, destination) -> {
                            attempts.incrementAndGet();
                            throw expected;
                        },
                        ignored -> {
                            throw new AssertionError("sleep must not run");
                        }));

        assertSame(expected, actual);
        assertEquals(1, attempts.get());
    }

    @Test
    void capsRetriesAndPreservesFirstAndFinalFailures() {
        AtomicInteger attempts = new AtomicInteger();

        AccessDeniedException actual = assertThrows(AccessDeniedException.class, () ->
                AtomicConfigWriter.moveWithBoundedSharingViolationRetry(
                        Path.of("temp"),
                        Path.of("target"),
                        (source, destination) -> {
                            int attempt = attempts.incrementAndGet();
                            throw new AccessDeniedException(
                                    source.toString(),
                                    destination.toString(),
                                    "locked-" + attempt);
                        },
                        ignored -> {
                        }));

        assertEquals(5, attempts.get());
        assertTrue(actual.getReason().contains("locked-5"));
        assertEquals(1, actual.getSuppressed().length);
        assertTrue(actual.getSuppressed()[0].getMessage().contains("locked-1"));
    }

    @Test
    void interruptionStopsRetryAndRestoresInterruptFlag() {
        AtomicInteger attempts = new AtomicInteger();

        IOException actual = assertThrows(IOException.class, () ->
                AtomicConfigWriter.moveWithBoundedSharingViolationRetry(
                        Path.of("temp"),
                        Path.of("target"),
                        (source, destination) -> {
                            attempts.incrementAndGet();
                            throw new AccessDeniedException(source.toString());
                        },
                        ignored -> {
                            throw new InterruptedException("stop");
                        }));

        assertEquals(1, attempts.get());
        assertTrue(Thread.currentThread().isInterrupted());
        assertTrue(actual.getCause() instanceof InterruptedException);
        assertFalse(List.of(actual.getSuppressed()).isEmpty());
    }
}
