package com.chedidandrew.smartresourcedrops.core.shearing;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ShearingOutputBufferTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void delaysAndMultipliesEachConsumerBatch() {
        List<String> emissions = new ArrayList<>();
        ShearingOutputBuffer<String> buffer = new ShearingOutputBuffer<>(
                2,
                ignored -> {
                    throw new AssertionError("unexpected fallback");
                },
                ignored -> {
                    throw new AssertionError("unexpected rollback failure");
                });
        BiConsumer<String, ItemStack> first = buffer.openBatch(
                "first-level",
                (level, stack) -> emissions.add("first:" + level + ":" + stack.getCount()));
        BiConsumer<String, ItemStack> second = buffer.openBatch(
                "second-level",
                (level, stack) -> emissions.add("second:" + level + ":" + stack.getCount()));

        first.accept("first-level", stack(Items.DIAMOND, 2));
        second.accept("second-level", stack(Items.EMERALD, 3));
        assertTrue(emissions.isEmpty());

        buffer.complete();

        assertEquals(List.of("first:first-level:4", "second:second-level:6"), emissions);
        buffer.complete();
        assertEquals(2, emissions.size(), "completion was not exactly once");
    }

    @Test
    void cumulativeOverflowFallsBackForTheCompleteAction() {
        List<Integer> emissions = new ArrayList<>();
        List<ShearingOutputBudget.LimitExceeded> fallbacks = new ArrayList<>();
        ShearingOutputBuffer<String> buffer = new ShearingOutputBuffer<>(
                64,
                result -> fallbacks.add(result.limitExceeded()),
                ignored -> {
                });
        BiConsumer<String, ItemStack> first = buffer.openBatch(
                "level",
                (level, stack) -> emissions.add(stack.getCount()));
        BiConsumer<String, ItemStack> second = buffer.openBatch(
                "level",
                (level, stack) -> emissions.add(stack.getCount()));

        first.accept("level", stack(Items.DIAMOND, 9));
        assertTrue(emissions.isEmpty());
        second.accept("level", stack(Items.EMERALD, 8));

        assertEquals(List.of(9, 8), emissions);
        assertEquals(List.of(ShearingOutputBudget.LimitExceeded.ITEMS), fallbacks);
        buffer.complete();
        assertEquals(List.of(9, 8), emissions);
    }

    @Test
    void abortEmitsOriginalExactlyOnce() {
        List<Integer> emissions = new ArrayList<>();
        ShearingOutputBuffer<String> buffer = new ShearingOutputBuffer<>(
                4,
                ignored -> {
                },
                ignored -> {
                });
        BiConsumer<String, ItemStack> consumer = buffer.openBatch(
                "level",
                (level, stack) -> emissions.add(stack.getCount()));
        consumer.accept("level", stack(Items.DIAMOND, 3));

        buffer.abort();
        buffer.abort();
        buffer.complete();

        assertEquals(List.of(3), emissions);
    }

    @Test
    void failedCommitNeverRetriesWithOriginalOutput() {
        AtomicInteger calls = new AtomicInteger();
        ShearingOutputBuffer<String> buffer = new ShearingOutputBuffer<>(
                2,
                ignored -> {
                },
                ignored -> {
                });
        BiConsumer<String, ItemStack> consumer = buffer.openBatch("level", (level, stack) -> {
            calls.incrementAndGet();
            throw new IllegalStateException("consumer failed");
        });
        consumer.accept("level", stack(Items.DIAMOND, 1));

        assertThrows(IllegalStateException.class, buffer::complete);
        buffer.abort();

        assertEquals(1, calls.get());
    }

    @Test
    void rollbackConsumerFailureIsReportedButDoesNotEscape() {
        AtomicInteger rollbackFailures = new AtomicInteger();
        ShearingOutputBuffer<String> buffer = new ShearingOutputBuffer<>(
                2,
                ignored -> {
                },
                ignored -> rollbackFailures.incrementAndGet());
        BiConsumer<String, ItemStack> consumer = buffer.openBatch("level", (level, stack) -> {
            throw new IllegalStateException("rollback consumer failed");
        });
        consumer.accept("level", stack(Items.DIAMOND, 1));

        assertDoesNotThrow(buffer::abort);
        assertEquals(1, rollbackFailures.get());
    }

    private static ItemStack stack(Item item, int count) {
        return new ItemStack(item, count);
    }
}
