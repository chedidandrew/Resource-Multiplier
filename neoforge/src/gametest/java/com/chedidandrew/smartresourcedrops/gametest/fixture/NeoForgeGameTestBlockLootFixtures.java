package com.chedidandrew.smartresourcedrops.gametest.fixture;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

/** Forge 47 adapter for the shared pathological block-loot fixture state. */
@Mod.EventBusSubscriber(
        modid = SmartResourceDrops.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class NeoForgeGameTestBlockLootFixtures {
    private NeoForgeGameTestBlockLootFixtures() {
    }

    @SubscribeEvent
    public static void register(final RegisterEvent event) {
        event.register(
                ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,
                SmartResourceDrops.id("gametest_pathological_block_loot"),
                () -> PathologicalBlockLootModifier.CODEC);
    }

    public static final class PathologicalBlockLootModifier extends LootModifier {
        public static final Codec<PathologicalBlockLootModifier> CODEC =
                RecordCodecBuilder.create(instance -> codecStart(instance)
                        .apply(instance, PathologicalBlockLootModifier::new));

        public PathologicalBlockLootModifier(final LootItemCondition[] conditions) {
            super(conditions);
        }

        @Override
        protected ObjectArrayList<ItemStack> doApply(
                final ObjectArrayList<ItemStack> generatedLoot,
                final LootContext context
        ) {
            GameTestBlockLootFixtures.appendPathologicalDropsIfArmed(
                    context.getQueriedLootTableId().equals(GameTestBlockLootFixtures.DIRT_LOOT),
                    generatedLoot);
            return generatedLoot;
        }

        @Override
        public Codec<? extends IGlobalLootModifier> codec() {
            return CODEC;
        }
    }
}
