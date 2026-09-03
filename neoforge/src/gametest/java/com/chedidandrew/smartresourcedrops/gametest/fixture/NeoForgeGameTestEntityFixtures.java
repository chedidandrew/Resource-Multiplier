package com.chedidandrew.smartresourcedrops.gametest.fixture;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.mojang.serialization.Codec;
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
import net.minecraft.world.level.storage.loot.Serializer;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

/** Forge 47 registration and final-loot adapters for shared entity fixtures. */
@Mod.EventBusSubscriber(
        modid = SmartResourceDrops.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class NeoForgeGameTestEntityFixtures {
    private NeoForgeGameTestEntityFixtures() {
    }

    @SubscribeEvent
    public static void register(final RegisterEvent event) {
        event.register(Registries.ENTITY_TYPE, helper ->
                GameTestEntityFixtures.registerEntities(
                        (key, type) -> helper.register(key, type)));
        event.register(
                ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,
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

    public static final class FinalLootModifier extends LootModifier {
        public static final Codec<FinalLootModifier> CODEC =
                RecordCodecBuilder.create(instance -> codecStart(instance)
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
                    id.equals(GameTestEntityFixtures.COMPONENT_RICH_LOOT),
                    id.equals(GameTestEntityFixtures.NESTED_OUTER_LOOT),
                    context,
                    loot);
            return loot;
        }

        @Override
        public Codec<? extends IGlobalLootModifier> codec() {
            return CODEC;
        }
    }

    @Mod.EventBusSubscriber(
            modid = SmartResourceDrops.MOD_ID,
            bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ForgeBusEvents {
        private ForgeBusEvents() {
        }

        @SubscribeEvent
        public static void lootTableLoad(final LootTableLoadEvent event) {
            if (!event.getName().equals(GameTestEntityFixtures.EXCEPTION_LOOT)) {
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

    private static final class ExceptionTrigger implements LootItemFunction {
        private static final ExceptionTrigger INSTANCE = new ExceptionTrigger();
        private static final LootItemFunctionType TYPE = new LootItemFunctionType(
                new Serializer<ExceptionTrigger>() {
                    @Override
                    public void serialize(
                            final JsonObject json,
                            final ExceptionTrigger value,
                            final JsonSerializationContext context
                    ) {
                    }

                    @Override
                    public ExceptionTrigger deserialize(
                            final JsonObject json,
                            final JsonDeserializationContext context
                    ) {
                        return INSTANCE;
                    }
                });

        @Override
        public ItemStack apply(final ItemStack stack, final LootContext context) {
            GameTestEntityFixtures.throwIfArmedExceptionLoot();
            return ItemStack.EMPTY;
        }

        @Override
        public LootItemFunctionType getType() {
            return TYPE;
        }
    }
}
