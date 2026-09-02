package com.chedidandrew.smartresourcedrops.core.util;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BlockLootOutputBudgetTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void zeroIsMutableAndOnePreservesExactInputIdentity() {
        final ItemStack sourceStack = stack(1, 64);
        final List<ItemStack> source = new ArrayList<>(List.of(sourceStack));

        final BlockLootOutputBudget.Result suppressed = BlockLootOutputBudget.multiply(source, 0);
        assertEquals(BlockLootOutputBudget.Outcome.SUPPRESSED, suppressed.outcome());
        assertNotSame(source, suppressed.output());
        assertDoesNotThrow(() -> suppressed.output().add(stack(1, 64)));
        assertEquals(1, source.size());
        assertEquals(1, sourceStack.getCount());

        final BlockLootOutputBudget.Result identity = BlockLootOutputBudget.multiply(source, 1);
        assertEquals(BlockLootOutputBudget.Outcome.IDENTITY, identity.outcome());
        assertSame(source, identity.output());
        assertSame(sourceStack, identity.output().getFirst());
    }

    @Test
    void exactSharedItemAndStackLimitsFit() {
        final ItemStack sourceStack = stack(4_096, 64);
        final List<ItemStack> source = List.of(sourceStack);

        final BlockLootOutputBudget.Result result = BlockLootOutputBudget.multiply(source, 64);

        assertEquals(BlockLootOutputBudget.Outcome.MULTIPLIED, result.outcome());
        assertEquals(LootOutputBudget.MAX_MULTIPLIED_ITEMS, result.multipliedItemEstimate());
        assertEquals(LootOutputBudget.MAX_MULTIPLIED_STACKS, result.legalStackEstimate());
        assertEquals(4_096, result.output().size());
        assertTrue(result.output().stream().allMatch(stack -> stack.getCount() == 64));
        assertEquals(4_096, sourceStack.getCount(), "The oversized fixture input was modified");
    }

    @Test
    void ordinaryLegalLootAtSixtyFourTimesUsesOnlyLegalStacks() {
        final ItemStack sourceStack = stack(64, 64);

        final BlockLootOutputBudget.Result result = BlockLootOutputBudget.multiply(List.of(sourceStack), 64);

        assertEquals(BlockLootOutputBudget.Outcome.MULTIPLIED, result.outcome());
        assertEquals(4_096, result.multipliedItemEstimate());
        assertEquals(64, result.output().size());
        assertTrue(result.output().stream().allMatch(stack ->
                stack.getCount() == 64 && stack.getCount() <= stack.getMaxStackSize()));
        assertEquals(64, sourceStack.getCount());
    }

    @Test
    void oneItemPastLimitFallsBackToExactOriginalList() {
        final ItemStack sourceStack = stack(4_097, 64);
        final List<ItemStack> source = new ArrayList<>(List.of(sourceStack));

        final BlockLootOutputBudget.Result result = assertDoesNotThrow(
                () -> BlockLootOutputBudget.multiply(source, 64));

        assertEquals(BlockLootOutputBudget.Outcome.FALLBACK_ITEM_LIMIT, result.outcome());
        assertTrue(result.fellBackToVanilla());
        assertSame(source, result.output());
        assertSame(sourceStack, result.output().getFirst());
        assertEquals(4_097, sourceStack.getCount());
    }

    @Test
    void exactUnstackableLimitFitsAndOnePastFallsBackWithoutPartialOutput() {
        final ItemStack atLimit = stack(2_048, 1);
        final BlockLootOutputBudget.Result fitting = BlockLootOutputBudget.multiply(List.of(atLimit), 2);
        assertEquals(BlockLootOutputBudget.Outcome.MULTIPLIED, fitting.outcome());
        assertEquals(4_096, fitting.output().size());
        assertTrue(fitting.output().stream().allMatch(stack -> stack.getCount() == 1));

        final ItemStack overLimit = stack(2_049, 1);
        final List<ItemStack> original = new ArrayList<>(List.of(overLimit));
        final BlockLootOutputBudget.Result fallback = BlockLootOutputBudget.multiply(original, 2);
        assertEquals(BlockLootOutputBudget.Outcome.FALLBACK_STACK_LIMIT, fallback.outcome());
        assertSame(original, fallback.output());
        assertEquals(BlockLootOutputBudget.EstimateKind.LOWER_BOUND, fallback.stackEstimateKind());
        assertTrue(fallback.legalStackEstimate() > LootOutputBudget.MAX_MULTIPLIED_STACKS);
    }

    @Test
    void identicalPartialEntriesAreGroupedBeforeTheStackLimitDecision() {
        final List<ItemStack> source = new ArrayList<>();
        for (int index = 0; index < 5_000; index++) {
            source.add(stack(1, 64));
        }

        final BlockLootOutputBudget.Result result = BlockLootOutputBudget.multiply(source, 2);

        assertEquals(BlockLootOutputBudget.Outcome.MULTIPLIED, result.outcome());
        assertEquals(10_000, result.multipliedItemEstimate());
        assertEquals(157, result.legalStackEstimate());
        assertEquals(157, result.output().size());
    }

    @Test
    void componentDifferencesNeverMergeAndInputsRemainUntouched() {
        final ItemStack first = stack(32, 64);
        final ItemStack second = stack(32, 64);
        first.set(DataComponents.CUSTOM_NAME, Component.literal("first"));
        second.set(DataComponents.CUSTOM_NAME, Component.literal("second"));

        final BlockLootOutputBudget.Result result = BlockLootOutputBudget.multiply(
                List.of(first, second),
                2);

        assertEquals(BlockLootOutputBudget.Outcome.MULTIPLIED, result.outcome());
        assertEquals(2, result.output().size());
        assertEquals(List.of("first", "second"), result.output().stream()
                .map(stack -> stack.get(DataComponents.CUSTOM_NAME).getString())
                .toList());
        assertEquals(32, first.getCount());
        assertEquals(32, second.getCount());
        assertFalse(ItemStack.isSameItemSameComponents(result.output().get(0), result.output().get(1)));
    }

    @Test
    void equalComponentRichEntriesUseTheSameHashGroup() {
        final ItemStack first = stack(20, 64);
        final ItemStack second = stack(20, 64);
        first.set(DataComponents.CUSTOM_NAME, Component.literal("same-rich-stack"));
        second.set(DataComponents.CUSTOM_NAME, Component.literal("same-rich-stack"));

        final BlockLootOutputBudget.Result result = BlockLootOutputBudget.multiply(
                List.of(first, second),
                2);

        assertEquals(BlockLootOutputBudget.Outcome.MULTIPLIED, result.outcome());
        assertEquals(List.of(64, 16), result.output().stream().map(ItemStack::getCount).toList());
        assertTrue(result.output().stream().allMatch(stack ->
                ItemStack.isSameItemSameComponents(first, stack)));
    }

    @Test
    void tooManyDistinctComponentGroupsFallsBackBeforeMaterialization() {
        final List<ItemStack> source = new ArrayList<>();
        for (int index = 0; index <= LootOutputBudget.MAX_MULTIPLIED_STACKS; index++) {
            final ItemStack stack = stack(1, 64);
            stack.set(DataComponents.CUSTOM_NAME, Component.literal("distinct-" + index));
            source.add(stack);
        }

        final BlockLootOutputBudget.Result result = BlockLootOutputBudget.multiply(source, 2);

        assertEquals(BlockLootOutputBudget.Outcome.FALLBACK_STACK_LIMIT, result.outcome());
        assertSame(source, result.output());
        assertEquals(BlockLootOutputBudget.EstimateKind.LOWER_BOUND, result.stackEstimateKind());
        assertTrue(result.legalStackEstimate() > LootOutputBudget.MAX_MULTIPLIED_STACKS);
    }

    @Test
    void pathologicalCountsAndInvalidEntriesDoNotThrowOrMutate() {
        final ItemStack pathological = stack(Integer.MAX_VALUE, 64);
        final List<ItemStack> pathologicalSource = new ArrayList<>(List.of(pathological));
        final BlockLootOutputBudget.Result fallback = assertDoesNotThrow(
                () -> BlockLootOutputBudget.multiply(pathologicalSource, 64));
        assertEquals(BlockLootOutputBudget.Outcome.FALLBACK_ITEM_LIMIT, fallback.outcome());
        assertSame(pathologicalSource, fallback.output());
        assertEquals(Integer.MAX_VALUE, pathological.getCount());

        final ItemStack negative = stack(1, 64);
        negative.setCount(-1);
        final ItemStack valid = stack(3, 64);
        final List<ItemStack> mixed = Arrays.asList(null, ItemStack.EMPTY, negative, valid);
        final BlockLootOutputBudget.Result multiplied = assertDoesNotThrow(
                () -> BlockLootOutputBudget.multiply(mixed, 2));
        assertEquals(6, multiplied.multipliedItemEstimate());
        assertEquals(1, multiplied.output().size());
        assertEquals(6, multiplied.output().getFirst().getCount());
        assertEquals(3, valid.getCount());
    }

    @Test
    void multiEntryPlanningSaturatesRealLongOverflow() {
        final ItemStack first = stack(Integer.MAX_VALUE, 64);
        final ItemStack second = stack(Integer.MAX_VALUE, 64);
        final ItemStack third = stack(Integer.MAX_VALUE, 64);
        final List<ItemStack> source = List.of(first, second, third);

        final BlockLootOutputBudget.Result result = assertDoesNotThrow(
                () -> BlockLootOutputBudget.multiply(source, Integer.MAX_VALUE));

        assertEquals(BlockLootOutputBudget.Outcome.FALLBACK_ITEM_LIMIT, result.outcome());
        assertEquals(Long.MAX_VALUE, result.multipliedItemEstimate());
        assertSame(source, result.output());
        assertEquals(Integer.MAX_VALUE, first.getCount());
        assertEquals(Integer.MAX_VALUE, second.getCount());
        assertEquals(Integer.MAX_VALUE, third.getCount());
    }

    private static ItemStack stack(final int count, final int maximumStackSize) {
        final ItemStack stack = new ItemStack(Items.DIAMOND, count);
        stack.set(DataComponents.MAX_STACK_SIZE, maximumStackSize);
        return stack;
    }
}
