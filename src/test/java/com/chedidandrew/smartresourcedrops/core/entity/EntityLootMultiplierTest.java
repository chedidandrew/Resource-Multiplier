package com.chedidandrew.smartresourcedrops.core.entity;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EntityLootMultiplierTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void zeroSuppressesOrdinaryLootButNeverSaddlesOrTotems() {
        List<ItemStack> output = new ArrayList<>();
        EntityLootMultiplier.emit(output::add, stack(Items.DIAMOND, 2), 0);
        EntityLootMultiplier.emit(output::add, stack(Items.SADDLE, 1), 0);
        EntityLootMultiplier.emit(output::add, stack(Items.TOTEM_OF_UNDYING, 1), 0);

        assertEquals(2, output.size());
        assertTrue(output.get(0).is(Items.SADDLE));
        assertTrue(output.get(1).is(Items.TOTEM_OF_UNDYING));
    }

    @Test
    void highMultiplierPreservesComponentsAndConsolidatesToLegalStacks() {
        ItemStack source = stack(Items.SNOWBALL, 2);
        source.setHoverName(Component.literal("preserved"));
        List<ItemStack> output = new ArrayList<>();

        EntityLootMultiplier.emit(output::add, source, 64);

        assertEquals(128, output.stream().mapToInt(ItemStack::getCount).sum());
        assertEquals(8, output.size());
        assertTrue(output.stream().allMatch(stack -> stack.getCount() <= stack.getMaxStackSize()));
        assertTrue(output.stream().allMatch(stack -> ItemStack.isSameItemSameTags(source, stack)));
        assertEquals(2, source.getCount());
    }

    @Test
    void emptyInvocationDoesNotClaimAndNestedWrappersMultiplyTheOwningInvocationOnce() {
        EntityLootOutputController controller = new EntityLootOutputController();
        List<ItemStack> output = new ArrayList<>();
        AtomicInteger warnings = new AtomicInteger();

        controller.wrap(output::add, 2, () -> true, warnings::incrementAndGet);
        assertFalse(controller.claimed());

        Consumer<ItemStack> inner = controller.wrap(
                output::add, 2, () -> true, warnings::incrementAndGet);
        Consumer<ItemStack> outer = controller.wrap(
                inner, 2, () -> true, warnings::incrementAndGet);
        outer.accept(stack(Items.DIAMOND, 1));
        outer.accept(stack(Items.DIAMOND, 3));

        assertTrue(controller.claimed());
        assertEquals(8, output.stream().mapToInt(ItemStack::getCount).sum());
        assertEquals(2, output.size());
        assertEquals(0, warnings.get());
    }

    @Test
    void outputBudgetIsCumulativeAndFallsBackToVanillaAfterOneDiagnostic() {
        EntityLootOutputController controller = new EntityLootOutputController();
        List<ItemStack> output = new ArrayList<>();
        AtomicInteger warnings = new AtomicInteger();
        Consumer<ItemStack> wrapped = controller.wrap(
                output::add, 64, () -> true, warnings::incrementAndGet);

        for (int index = 0; index < 65; index++) {
            ItemStack unstackable = stack(Items.DIAMOND_SWORD, 1);
            wrapped.accept(unstackable);
        }
        wrapped.accept(stack(Items.DIAMOND, 2));

        assertTrue(controller.budgetExceeded());
        assertEquals(EntityLootOutputController.MAX_MULTIPLIED_STACKS, controller.multipliedStacks());
        assertEquals(4_099, output.stream().mapToInt(ItemStack::getCount).sum());
        assertEquals(4_098, output.size());
        assertEquals(1, warnings.get());
    }

    @Test
    void itemBudgetCannotBeEvadedByManyOtherwiseLegalStackableOutputs() {
        EntityLootOutputController controller = new EntityLootOutputController();
        List<ItemStack> output = new ArrayList<>();
        AtomicInteger warnings = new AtomicInteger();
        Consumer<ItemStack> wrapped = controller.wrap(
                output::add, 64, () -> true, warnings::incrementAndGet);

        for (int index = 0; index < 42; index++) {
            ItemStack largeStack = stack(Items.DIAMOND, 99);
            wrapped.accept(largeStack);
        }
        wrapped.accept(stack(Items.DIAMOND, 1));

        assertTrue(controller.budgetExceeded());
        assertEquals(259_776L, controller.multipliedItems());
        assertEquals(4_059L, controller.multipliedStacks());
        assertEquals(259_876, output.stream().mapToInt(ItemStack::getCount).sum());
        assertEquals(4_061, output.size());
        assertEquals(1, warnings.get());
    }

    @Test
    void protectedOutputTagHasStableDatapackResourceLocation() {
        assertEquals(
                "smart_resource_drops:protected_entity_loot",
                EntityLootTags.PROTECTED_OUTPUTS.location().toString());
    }

    private static ItemStack stack(Item item, int count) {
        return new ItemStack(item, count);
    }
}
