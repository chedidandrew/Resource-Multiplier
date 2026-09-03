package com.chedidandrew.smartresourcedrops.gametest.fixture;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** Loader-neutral entity definitions and state used by the dedicated-server GameTests. */
public final class GameTestEntityFixtures {
    public static final String MOD_ID = "smart_resource_drops_gametest";
    public static final String COMPONENT_MARKER = "smartdrops-component-fixture";

    public static EntityType<FixtureAnimal> PASSIVE;
    public static EntityType<FixtureMonster> HOSTILE;
    public static EntityType<FixtureNeutral> NEUTRAL;
    public static EntityType<FixtureAquatic> AQUATIC;
    public static EntityType<FixturePathfinder> CATEGORY_ONLY;
    public static EntityType<FixturePathfinder> UNCLASSIFIED;
    public static EntityType<FixtureMonster> BOSS;
    public static EntityType<FixtureMonster> EQUIPMENT;
    public static EntityType<FixturePickupMonster> CARRYING;
    public static EntityType<FixtureInventoryMonster> INVENTORY;
    public static EntityType<FixtureMonster> COMPONENT_RICH;
    public static EntityType<FixtureDirectOutputMonster> DIRECT_OUTPUT;
    public static EntityType<FixtureMonster> NESTED_OUTER;
    public static EntityType<FixtureMonster> EXCEPTION;
    public static EntityType<FixtureDuplicateHookMonster> DUPLICATE_HOOK;
    public static EntityType<FixtureMonster> LOOTING_FINAL;
    public static EntityType<FixtureMonster> COOKED_FINAL;
    public static EntityType<FixtureMonster> EMPTY;
    public static EntityType<FixtureMonster> UNSTACKABLE;

    public static final ResourceLocation COMPONENT_RICH_LOOT = lootTable("component_rich");
    public static final ResourceLocation NESTED_OUTER_LOOT = lootTable("nested_outer");
    public static final ResourceLocation EXCEPTION_LOOT = lootTable("exception");
    public static final AtomicInteger COMPONENT_MODIFIER_INVOCATIONS = new AtomicInteger();

    private static LivingEntity nestedTarget;
    private static boolean throwOnNextExceptionLoot;

    private GameTestEntityFixtures() {
    }

    @FunctionalInterface
    public interface EntityRegistrar {
        void register(ResourceKey<EntityType<?>> key, EntityType<?> value);
    }

    public static void registerEntities(final EntityRegistrar registrar) {
        if (PASSIVE != null) {
            throw new IllegalStateException("GameTest entity fixtures are already registered");
        }
        PASSIVE = register(registrar, "passive", MobCategory.CREATURE, FixtureAnimal::new);
        HOSTILE = register(registrar, "hostile", MobCategory.MONSTER, FixtureMonster::new);
        NEUTRAL = register(registrar, "neutral", MobCategory.CREATURE, FixtureNeutral::new);
        AQUATIC = register(registrar, "aquatic", MobCategory.WATER_CREATURE, FixtureAquatic::new);
        CATEGORY_ONLY = register(registrar, "category_only", MobCategory.AMBIENT, FixturePathfinder::new);
        UNCLASSIFIED = register(registrar, "unclassified", MobCategory.MISC, FixturePathfinder::new);
        BOSS = register(registrar, "boss", MobCategory.MONSTER, FixtureMonster::new);
        EQUIPMENT = register(registrar, "equipment", MobCategory.MONSTER, FixtureMonster::new);
        CARRYING = register(registrar, "carrying", MobCategory.MONSTER, FixturePickupMonster::new);
        INVENTORY = register(registrar, "inventory", MobCategory.MONSTER, FixtureInventoryMonster::new);
        COMPONENT_RICH = register(registrar, "component_rich", MobCategory.MONSTER, FixtureMonster::new);
        DIRECT_OUTPUT = register(registrar, "direct_output", MobCategory.MONSTER, FixtureDirectOutputMonster::new);
        NESTED_OUTER = register(registrar, "nested_outer", MobCategory.MONSTER, FixtureMonster::new);
        EXCEPTION = register(registrar, "exception", MobCategory.MONSTER, FixtureMonster::new);
        DUPLICATE_HOOK = register(registrar, "duplicate_hook", MobCategory.MONSTER, FixtureDuplicateHookMonster::new);
        LOOTING_FINAL = register(registrar, "looting_final", MobCategory.MONSTER, FixtureMonster::new);
        COOKED_FINAL = register(registrar, "cooked_final", MobCategory.MONSTER, FixtureMonster::new);
        EMPTY = register(registrar, "empty", MobCategory.MONSTER, FixtureMonster::new);
        UNSTACKABLE = register(registrar, "unstackable", MobCategory.MONSTER, FixtureMonster::new);
    }

    public static List<EntityType<? extends Mob>> mobTypes() {
        return List.of(
                PASSIVE,
                HOSTILE,
                NEUTRAL,
                AQUATIC,
                CATEGORY_ONLY,
                UNCLASSIFIED,
                BOSS,
                EQUIPMENT,
                CARRYING,
                INVENTORY,
                COMPONENT_RICH,
                DIRECT_OUTPUT,
                NESTED_OUTER,
                EXCEPTION,
                DUPLICATE_HOOK,
                LOOTING_FINAL,
                COOKED_FINAL,
                EMPTY,
                UNSTACKABLE);
    }

    public static void applyFinalDropFixtures(
            final boolean componentRich,
            final boolean nestedOuter,
            final LootContext context,
            final List<ItemStack> drops
    ) {
        applyFinalDropFixtures(
                componentRich,
                nestedOuter,
                context.getParamOrNull(LootContextParams.DAMAGE_SOURCE),
                drops);
    }

    public static void applyFinalDropFixtures(
            final boolean componentRich,
            final boolean nestedOuter,
            final LootParams params,
            final List<ItemStack> drops
    ) {
        applyFinalDropFixtures(
                componentRich,
                nestedOuter,
                params.getParamOrNull(LootContextParams.DAMAGE_SOURCE),
                drops);
    }

    private static void applyFinalDropFixtures(
            final boolean componentRich,
            final boolean nestedOuter,
            final DamageSource source,
            final List<ItemStack> drops
    ) {
        if (componentRich) {
            COMPONENT_MODIFIER_INVOCATIONS.incrementAndGet();
            final ItemStack stack = new ItemStack(Items.DIAMOND);
            final CompoundTag marker = new CompoundTag();
            marker.putString("fixture", COMPONENT_MARKER);
            stack.setTag(marker);
            // On 1.20.1 both the custom name and arbitrary fixture marker live
            // in the same item tag, so attach the marker before adding display.Name.
            stack.setHoverName(Component.literal(COMPONENT_MARKER));
            drops.add(stack);
        }

        if (nestedOuter && nestedTarget != null) {
            final LivingEntity target = nestedTarget;
            nestedTarget = null;
            if (source != null) {
                target.hurt(source, Float.MAX_VALUE);
            }
        }
    }

    public static void throwIfArmedExceptionLoot() {
        if (throwOnNextExceptionLoot) {
            throwOnNextExceptionLoot = false;
            throw new FixtureLootException();
        }
    }

    public static void armNestedLoot(final LivingEntity target) {
        nestedTarget = target;
    }

    public static void throwOnNextExceptionLoot() {
        throwOnNextExceptionLoot = true;
    }

    public static void resetTransientState() {
        nestedTarget = null;
        throwOnNextExceptionLoot = false;
        COMPONENT_MODIFIER_INVOCATIONS.set(0);
    }

    private static ResourceLocation lootTable(final String entityPath) {
        return new ResourceLocation(MOD_ID, "entities/" + entityPath);
    }

    private static <T extends Mob> EntityType<T> register(
            final EntityRegistrar registrar,
            final String path,
            final MobCategory category,
            final EntityType.EntityFactory<T> factory) {
        final ResourceLocation id = new ResourceLocation(MOD_ID, path);
        final ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
        final EntityType<T> type = EntityType.Builder.of(factory, category)
                .sized(0.6F, 1.8F)
                .clientTrackingRange(8)
                .build(id.toString());
        registrar.register(key, type);
        return type;
    }

    public static class FixtureAnimal extends Animal {
        public FixtureAnimal(final EntityType<? extends FixtureAnimal> type, final Level level) {
            super(type, level);
        }

        @Override
        public boolean isFood(final ItemStack stack) {
            return false;
        }

        @Override
        public AgeableMob getBreedOffspring(final ServerLevel level, final AgeableMob mate) {
            return null;
        }
    }

    public static final class FixtureNeutral extends FixtureAnimal implements NeutralMob {
        private int remainingPersistentAngerTime;
        private UUID persistentAngerTarget;

        public FixtureNeutral(final EntityType<? extends FixtureNeutral> type, final Level level) {
            super(type, level);
        }

        @Override
        public int getRemainingPersistentAngerTime() {
            return remainingPersistentAngerTime;
        }

        @Override
        public void setRemainingPersistentAngerTime(final int time) {
            remainingPersistentAngerTime = time;
        }

        @Override
        public UUID getPersistentAngerTarget() {
            return persistentAngerTarget;
        }

        @Override
        public void setPersistentAngerTarget(final UUID target) {
            persistentAngerTarget = target;
        }

        @Override
        public void startPersistentAngerTimer() {
            setRemainingPersistentAngerTime(400);
        }
    }

    public static class FixtureMonster extends Monster {
        public FixtureMonster(final EntityType<? extends FixtureMonster> type, final Level level) {
            super(type, level);
        }

        @Override
        public int getExperienceReward() {
            // 7x and its 3x result (17 + 3 + 1) use distinct vanilla orb values, so the
            // fixture can total getValue() without undercounting randomly count-merged orbs.
            return 7;
        }
    }

    /** Exposes the real Mob pickup path without making any production API test-only. */
    public static final class FixturePickupMonster extends Monster {
        public FixturePickupMonster(
                final EntityType<? extends FixturePickupMonster> type,
                final Level level) {
            super(type, level);
        }

        public void fixturePickUp(final ServerLevel level, final ItemEntity itemEntity) {
            setCanPickUpLoot(true);
            pickUpItem(itemEntity);
        }

        @Override
        public int getExperienceReward() {
            return 5;
        }
    }

    public static final class FixtureAquatic extends WaterAnimal {
        public FixtureAquatic(final EntityType<? extends FixtureAquatic> type, final Level level) {
            super(type, level);
        }
    }

    public static final class FixturePathfinder extends PathfinderMob {
        public FixturePathfinder(final EntityType<? extends FixturePathfinder> type, final Level level) {
            super(type, level);
        }
    }

    public static final class FixtureInventoryMonster extends Monster {
        private final SimpleContainer inventory = new SimpleContainer(3);

        public FixtureInventoryMonster(
                final EntityType<? extends FixtureInventoryMonster> type,
                final Level level) {
            super(type, level);
        }

        public SimpleContainer fixtureInventory() {
            return inventory;
        }

        @Override
        public int getExperienceReward() {
            return 5;
        }

        @Override
        protected void dropEquipment() {
            super.dropEquipment();
            for (ItemStack stack : inventory.removeAllItems()) {
                if (!stack.isEmpty()) {
                    spawnAtLocation(stack);
                }
            }
        }
    }

    public static final class FixtureDirectOutputMonster extends Monster {
        public FixtureDirectOutputMonster(
                final EntityType<? extends FixtureDirectOutputMonster> type,
                final Level level) {
            super(type, level);
        }

        @Override
        public int getExperienceReward() {
            return 5;
        }

        @Override
        protected void dropCustomDeathLoot(
                final DamageSource source,
                final int lootingLevel,
                final boolean recentlyHitByPlayer) {
            super.dropCustomDeathLoot(source, lootingLevel, recentlyHitByPlayer);
            spawnAtLocation(new ItemStack(Items.EMERALD));
        }
    }

    /** Calls the standard death-table method twice inside one real death scope. */
    public static final class FixtureDuplicateHookMonster extends Monster {
        public FixtureDuplicateHookMonster(
                final EntityType<? extends FixtureDuplicateHookMonster> type,
                final Level level) {
            super(type, level);
        }

        @Override
        public int getExperienceReward() {
            return 5;
        }

        @Override
        protected void dropFromLootTable(
                final DamageSource source,
                final boolean recentlyHitByPlayer) {
            super.dropFromLootTable(source, recentlyHitByPlayer);
            super.dropFromLootTable(source, recentlyHitByPlayer);
        }
    }

    public static final class FixtureLootException extends RuntimeException {
        private FixtureLootException() {
            super("intentional GameTest loot failure");
        }
    }
}
