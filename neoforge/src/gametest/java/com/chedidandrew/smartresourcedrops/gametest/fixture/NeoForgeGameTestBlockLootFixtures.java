package com.chedidandrew.smartresourcedrops.gametest.fixture;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

/** NeoForge adapter for the shared pathological block-loot fixture state. */
@EventBusSubscriber(modid = SmartResourceDrops.MOD_ID)
public final class NeoForgeGameTestBlockLootFixtures {
    private NeoForgeGameTestBlockLootFixtures() {
    }

    @SubscribeEvent
    public static void register(final RegisterEvent event) {
        event.register(
                NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,
                SmartResourceDrops.id("gametest_pathological_block_loot"),
                () -> PathologicalBlockLootModifier.CODEC);
    }

    public static final class PathologicalBlockLootModifier extends LootModifier {
        public static final MapCodec<PathologicalBlockLootModifier> CODEC =
                RecordCodecBuilder.mapCodec(instance -> codecStart(instance)
                        .apply(instance, PathologicalBlockLootModifier::new));

        public PathologicalBlockLootModifier(
                final LootItemCondition[] conditions,
                final int priority
        ) {
            super(conditions, priority);
        }

        @Override
        protected ObjectArrayList<ItemStack> doApply(
                final ObjectArrayList<ItemStack> generatedLoot,
                final LootContext context
        ) {
            GameTestBlockLootFixtures.appendPathologicalDropsIfArmed(
                    context.getQueriedLootTableId().equals(
                            GameTestBlockLootFixtures.DIRT_LOOT.identifier()),
                    generatedLoot);
            return generatedLoot;
        }

        @Override
        public MapCodec<? extends IGlobalLootModifier> codec() {
            return CODEC;
        }
    }
}
