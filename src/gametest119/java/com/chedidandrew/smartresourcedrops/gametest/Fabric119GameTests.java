package com.chedidandrew.smartresourcedrops.gametest;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.config.ConfigManager;
import com.chedidandrew.smartresourcedrops.config.SmartDropsConfig;
import com.chedidandrew.smartresourcedrops.core.entity.EntityCategory;
import com.chedidandrew.smartresourcedrops.core.entity.EntityClassifier;
import com.chedidandrew.smartresourcedrops.provenance.PlacementTracker;
import com.chedidandrew.smartresourcedrops.provenance.ProtectedFallingBlock;
import com.chedidandrew.smartresourcedrops.provenance.ProtectedPistonMovement;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import org.spongepowered.asm.mixin.MixinEnvironment;

/** Focused target-native integration coverage for the Minecraft 1.19.2 backport. */
public final class Fabric119GameTests implements FabricGameTest, ModInitializer {
    private static final BlockPos TARGET = new BlockPos(2, 2, 2);

    @Override
    public void onInitialize() {
        SmartResourceDrops.LOGGER.info(
                "Smart Resource Multiplier Fabric 1.19.2 GameTest discovery: 5 tests");
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void dedicatedServerLoadsProductionMixins(final GameTestHelper helper) {
        check(helper, FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER,
                "GameTest did not launch on a dedicated server");
        check(helper, !FabricLoader.getInstance().isModLoaded("modmenu"),
                "Optional Mod Menu leaked onto the server runtime");
        MixinEnvironment.getCurrentEnvironment().audit();
        check(helper, ProtectedFallingBlock.class.isAssignableFrom(FallingBlockEntity.class),
                "Falling-block provenance mixin was not applied");
        check(helper, ProtectedPistonMovement.class.isAssignableFrom(PistonMovingBlockEntity.class),
                "Piston provenance mixin was not applied");
        helper.succeed();
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void persistentPlacementStorageMarksAndRemoves(final GameTestHelper helper) {
        final BlockPos absolute = helper.absolutePos(TARGET);
        PlacementTracker.remove(helper.getLevel(), absolute);
        PlacementTracker.mark(helper.getLevel(), absolute);
        check(helper, PlacementTracker.isMarked(helper.getLevel(), absolute),
                "Fabric SavedData did not retain a placement mark");
        check(helper, PlacementTracker.remove(helper.getLevel(), absolute),
                "Fabric SavedData did not remove a placement mark");
        check(helper, !PlacementTracker.isMarked(helper.getLevel(), absolute),
                "Fabric SavedData retained a removed placement mark");
        helper.succeed();
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void registryBackedEntityClassificationWorks(final GameTestHelper helper) {
        final var cow = helper.spawn(EntityType.COW, TARGET);
        final var classification = EntityClassifier.classify(cow);
        check(helper, classification.selectedCategory() == EntityCategory.PASSIVE,
                "Cow did not resolve to the passive entity category: "
                        + classification.selectedCategory());
        helper.succeed();
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 40)
    public void standardEntityDeathLootUsesConfiguredMultiplier(final GameTestHelper helper) {
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

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 40)
    public void automatedBlockDropsUseConfiguredMultiplier(final GameTestHelper helper) {
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
