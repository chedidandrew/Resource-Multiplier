package com.chedidandrew.smartresourcedrops.gametest.fixture;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

/** NeoForge registration and final-loot adapters for the shared entity GameTest fixtures. */
@EventBusSubscriber(modid = SmartResourceDrops.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class NeoForgeGameTestEntityFixtures {
    private NeoForgeGameTestEntityFixtures() {
    }

    @SubscribeEvent
    public static void register(final RegisterEvent event) {
        event.register(Registries.ENTITY_TYPE, helper ->
                GameTestEntityFixtures.registerEntities(
                        (key, type) -> helper.register(key, type)));
        event.register(
                NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,
                SmartResourceDrops.id("gametest_entity_final_loot"),
                () -> FinalLootModifier.CODEC);
        event.register(
                Registries.LOOT_FUNCTION_TYPE,
                SmartResourceDrops.id("gametest_exception_trigger"),
                () -> ExceptionTrigger.TYPE);
    }

    @SubscribeEvent
    public static void attributes(final EntityAttributeCreationEvent event) {
        final AttributeSupplier attributes = Mob.createMobAttributes().build();
        for (EntityType<? extends Mob> type : GameTestEntityFixtures.mobTypes()) {
            event.put(type, attributes);
        }
    }

    @EventBusSubscriber(modid = SmartResourceDrops.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
    public static final class GameBusEvents {
        private GameBusEvents() {
        }

        @SubscribeEvent
        public static void lootTableLoad(final LootTableLoadEvent event) {
            if (!event.getKey().equals(GameTestEntityFixtures.EXCEPTION_LOOT)) {
                return;
            }
            event.getTable().addPool(LootPool.lootPool()
                    .name("smart_resource_drops_gametest_exception_trigger")
                    .setRolls(ConstantValue.exactly(1.0F))
                    .add(LootItem.lootTableItem(Items.STONE))
                    .apply(() -> ExceptionTrigger.INSTANCE)
                    .build());
        }
    }

    public static final class FinalLootModifier extends LootModifier {
        public static final MapCodec<FinalLootModifier> CODEC =
                RecordCodecBuilder.mapCodec(instance -> codecStart(instance)
                        .apply(instance, FinalLootModifier::new));

        public FinalLootModifier(final LootItemCondition[] conditions) {
            super(conditions);
        }

        @Override
        protected ObjectArrayList<ItemStack> doApply(
                final ObjectArrayList<ItemStack> loot,
                final LootContext context
        ) {
            final ResourceLocation id = context.getQueriedLootTableId();
            GameTestEntityFixtures.applyFinalDropFixtures(
                    id.equals(GameTestEntityFixtures.COMPONENT_RICH_LOOT.location()),
                    id.equals(GameTestEntityFixtures.NESTED_OUTER_LOOT.location()),
                    context,
                    loot);
            return loot;
        }

        @Override
        public MapCodec<? extends IGlobalLootModifier> codec() {
            return CODEC;
        }
    }

    private static final class ExceptionTrigger implements LootItemFunction {
        private static final ExceptionTrigger INSTANCE = new ExceptionTrigger();
        private static final MapCodec<ExceptionTrigger> CODEC = MapCodec.unit(INSTANCE);
        private static final LootItemFunctionType<ExceptionTrigger> TYPE =
                new LootItemFunctionType<>(CODEC);

        @Override
        public ItemStack apply(final ItemStack stack, final LootContext context) {
            GameTestEntityFixtures.throwIfArmedExceptionLoot();
            return ItemStack.EMPTY;
        }

        @Override
        public LootItemFunctionType<? extends LootItemFunction> getType() {
            return TYPE;
        }
    }
}
