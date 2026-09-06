package com.chedidandrew.smartresourcedrops.gametest;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.config.ConfigManager;
import com.chedidandrew.smartresourcedrops.config.SmartDropsConfig;
import com.chedidandrew.smartresourcedrops.core.entity.EntityCategory;
import com.chedidandrew.smartresourcedrops.core.entity.EntityClassifier;
import com.chedidandrew.smartresourcedrops.provenance.PlacementTracker;
import com.chedidandrew.smartresourcedrops.provenance.ProtectedFallingBlock;
import com.chedidandrew.smartresourcedrops.provenance.ProtectedPistonMovement;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.spongepowered.asm.mixin.MixinEnvironment;

/** Focused Forge 43 integration coverage for the Minecraft 1.19.2 backport. */
@Mod.EventBusSubscriber(
        modid = SmartResourceDrops.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class Forge119GameTests {
    private static final String NAMESPACE = "smart_resource_drops_gametest";
    private static final String STRUCTURE = NAMESPACE + ":wide";
    private static final BlockPos TARGET = new BlockPos(2, 2, 2);

    private Forge119GameTests() {
    }

    @SubscribeEvent
    public static void register(final RegisterGameTestsEvent event) {
        event.register(Forge119GameTests.class);
    }

    @GameTestGenerator
    public static Collection<TestFunction> generateTests() {
        SmartResourceDrops.LOGGER.info(
                "Smart Resource Multiplier Forge 1.19.2 GameTest discovery: 5 tests");
        return List.of(
                test("dedicated_server_loads_production_mixins",
                        Forge119GameTests::dedicatedServerLoadsProductionMixins),
                test("persistent_placement_storage_marks_and_removes",
                        Forge119GameTests::persistentPlacementStorageMarksAndRemoves),
                test("registry_backed_entity_classification_works",
                        Forge119GameTests::registryBackedEntityClassificationWorks),
                test("standard_entity_death_loot_uses_configured_multiplier",
                        Forge119GameTests::standardEntityDeathLootUsesConfiguredMultiplier),
                test("automated_block_drops_use_configured_multiplier",
                        Forge119GameTests::automatedBlockDropsUseConfiguredMultiplier));
    }

    private static TestFunction test(
            final String name,
            final Consumer<GameTestHelper> body
    ) {
        return new TestFunction(
                "defaultBatch",
                NAMESPACE + ":" + name,
                STRUCTURE,
                Rotation.NONE,
                40,
                0L,
                true,
                body);
    }

    private static void dedicatedServerLoadsProductionMixins(final GameTestHelper helper) {
        check(helper, FMLEnvironment.dist == Dist.DEDICATED_SERVER,
                "GameTest did not launch on a dedicated server");
        MixinEnvironment.getCurrentEnvironment().audit();
        check(helper, ProtectedFallingBlock.class.isAssignableFrom(FallingBlockEntity.class),
                "Falling-block provenance mixin was not applied");
        check(helper, ProtectedPistonMovement.class.isAssignableFrom(PistonMovingBlockEntity.class),
                "Piston provenance mixin was not applied");
        helper.succeed();
    }

    private static void persistentPlacementStorageMarksAndRemoves(final GameTestHelper helper) {
        final BlockPos absolute = helper.absolutePos(TARGET);
        PlacementTracker.remove(helper.getLevel(), absolute);
        PlacementTracker.mark(helper.getLevel(), absolute);
        check(helper, PlacementTracker.isMarked(helper.getLevel(), absolute),
                "Forge chunk capability did not retain a placement mark");
        check(helper, PlacementTracker.remove(helper.getLevel(), absolute),
                "Forge chunk capability did not remove a placement mark");
        check(helper, !PlacementTracker.isMarked(helper.getLevel(), absolute),
                "Forge chunk capability retained a removed placement mark");
        helper.succeed();
    }

    private static void registryBackedEntityClassificationWorks(final GameTestHelper helper) {
        final var cow = helper.spawn(EntityType.COW, TARGET);
        final var classification = EntityClassifier.classify(cow);
        check(helper, classification.selectedCategory() == EntityCategory.PASSIVE,
                "Cow did not resolve to the passive entity category: "
                        + classification.selectedCategory());
        helper.succeed();
    }

    private static void standardEntityDeathLootUsesConfiguredMultiplier(
            final GameTestHelper helper
    ) {
        final boolean updated = ConfigManager.update(config -> configureThreeTimesEntities(config));
        check(helper, updated || ConfigManager.get().defaultEntityMultiplier == 3,
                "Could not stage the 3x entity integration-test configuration");
        final var cow = helper.spawn(EntityType.COW, new BlockPos(6, 2, 6));
        cow.hurt(DamageSource.GENERIC, 1_000.0F);
        helper.runAfterDelay(2L, () -> {
            final var drops = helper.getLevel().getEntitiesOfClass(
                    ItemEntity.class,
                    cow.getBoundingBox().inflate(2.0D),
                    item -> item.getItem().is(Items.BEEF) || item.getItem().is(Items.LEATHER));
            check(helper, !drops.isEmpty(), "Cow produced no standard death loot");
            check(helper, drops.stream().allMatch(item -> item.getItem().getCount() % 3 == 0),
                    "A standard cow death-loot stack was not multiplied by 3");
            helper.succeed();
        });
    }

    private static void automatedBlockDropsUseConfiguredMultiplier(final GameTestHelper helper) {
        helper.killAllEntities();
        final boolean updated = ConfigManager.update(config -> configureThreeTimesAutomation(config));
        check(helper, updated || ConfigManager.get().globalMultiplier == 3,
                "Could not stage the 3x integration-test configuration");

        helper.setBlock(TARGET, Blocks.DIRT);
        final BlockPos absolute = helper.absolutePos(TARGET);
        Block.dropResources(Blocks.DIRT.defaultBlockState(), helper.getLevel(), absolute);
        helper.runAfterDelay(2L, () -> {
            helper.assertItemEntityCountIs(Items.DIRT, TARGET, 2.0D, 3);
            helper.succeed();
        });
    }

    private static void configureThreeTimesEntities(final SmartDropsConfig config) {
        config.entityDropsEnabled = true;
        config.inheritDefaultEntityMultiplier = false;
        config.defaultEntityMultiplier = 3;
        config.entityKillRequirement =
                SmartDropsConfig.EntityKillRequirement.ALL_STANDARD_DEATH_LOOT;
        config.bossDropsEnabled = true;
        config.entityFilterMode = SmartDropsConfig.FilterMode.BLACKLIST;
        config.entityCategoryMultipliers.clear();
        config.entityMultipliers.clear();
        config.entityBlacklist.clear();
        config.entityTagBlacklist.clear();
    }

    private static void configureThreeTimesAutomation(final SmartDropsConfig config) {
        config.enabled = true;
        config.globalMultiplier = 3;
        config.sourceMode = SmartDropsConfig.SourceMode.ALL;
        config.automatedMining = true;
        config.smartPlacementProtection = false;
        config.protectBlockEntities = false;
        config.filterMode = SmartDropsConfig.FilterMode.BLACKLIST;
        config.blacklist.clear();
        config.tagBlacklist.clear();
        config.blockMultipliers.clear();
        config.categoryMultipliers.clear();
        config.dimensionMultipliers.clear();
    }

    private static void check(
            final GameTestHelper helper,
            final boolean condition,
            final String message
    ) {
        if (!condition) {
            helper.fail(message);
        }
    }
}
