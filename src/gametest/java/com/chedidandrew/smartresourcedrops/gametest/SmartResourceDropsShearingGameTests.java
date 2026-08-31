package com.chedidandrew.smartresourcedrops.gametest;

import com.chedidandrew.smartresourcedrops.config.ConfigManager;
import com.chedidandrew.smartresourcedrops.config.SmartDropsConfig;
import com.chedidandrew.smartresourcedrops.core.shearing.ShearingActionContext;
import com.chedidandrew.smartresourcedrops.core.shearing.ShearingClassification;
import com.chedidandrew.smartresourcedrops.core.shearing.ShearingGameTestAccess;
import com.chedidandrew.smartresourcedrops.core.shearing.ShearingOutputBudget;
import com.chedidandrew.smartresourcedrops.core.shearing.ShearingRuleResolver;
import com.chedidandrew.smartresourcedrops.core.shearing.ShearingRuleTrace;
import com.chedidandrew.smartresourcedrops.core.shearing.ShearingSource;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import net.minecraft.world.entity.monster.skeleton.Bogged;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Dedicated-server black-box and focused invariant coverage for entity shearing multiplication. */
public final class SmartResourceDropsShearingGameTests {
    private static final BlockPos ENTITY_POS = new BlockPos(4, 2, 4);
    private static final BlockPos DISPENSER_POS = new BlockPos(3, 2, 4);

    @GameTest
    public void freshManualDefaultInheritsGlobalTwoForRealSheep(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareShearing(helper, config -> {
                config.globalMultiplier = 2;
                config.inheritDefaultShearingMultiplier = true;
            });
            final Sheep sheep = spawnSheep(helper, ENTITY_POS);
            shearWithPlayer(helper, sheep, new ItemStack(Items.SHEARS));
            assertWoolMultiple(helper, ENTITY_POS, Items.WOOL.white(), 2, 2, 6,
                    "fresh inherited 2x manual sheep");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest
    public void disabledManualSourceKeepsRealSheepVanilla(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareShearing(helper, config -> {
                config.manualShearingDropsEnabled = false;
                config.defaultShearingMultiplier = 64;
            });
            final Sheep sheep = spawnSheep(helper, ENTITY_POS);
            final ItemStack shears = shearWithPlayer(helper, sheep, new ItemStack(Items.SHEARS));
            assertItemRange(helper, ENTITY_POS, Items.WOOL.white(), 1, 3,
                    "disabled manual source");
            helper.assertTrue(sheep.isSheared(), "Disabled multiplication prevented vanilla shearing");
            helper.assertTrue(shears.getDamageValue() == 1,
                    "Disabled multiplication changed vanilla tool damage");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest
    public void zeroMultiplierSuppressesWoolButCompletesShearing(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareShearing(helper, config -> config.defaultShearingMultiplier = 0);
            final Sheep sheep = spawnSheep(helper, ENTITY_POS);
            final ItemStack shears = shearWithPlayer(helper, sheep, new ItemStack(Items.SHEARS));
            assertItemTotal(helper, ENTITY_POS, Items.WOOL.white(), 0, "0x manual sheep");
            helper.assertTrue(sheep.isSheared(), "0x did not preserve the vanilla sheared state");
            helper.assertTrue(shears.getDamageValue() == 1,
                    "0x did not preserve exactly one point of shear damage");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest
    public void oneMultiplierPreservesVanillaSheepBounds(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareShearing(helper, config -> config.defaultShearingMultiplier = 1);
            shearWithPlayer(helper, spawnSheep(helper, ENTITY_POS), new ItemStack(Items.SHEARS));
            assertItemRange(helper, ENTITY_POS, Items.WOOL.white(), 1, 3, "1x manual sheep");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest
    public void twoMultiplierScalesFinalVanillaSheepLoot(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareShearing(helper, config -> config.defaultShearingMultiplier = 2);
            shearWithPlayer(helper, spawnSheep(helper, ENTITY_POS), new ItemStack(Items.SHEARS));
            assertWoolMultiple(helper, ENTITY_POS, Items.WOOL.white(), 2, 2, 6,
                    "2x manual sheep");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest(padding = 16)
    public void sixtyFourMultiplierUsesLegalStacksForRealSheep(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareShearing(helper, config -> config.defaultShearingMultiplier = 64);
            shearWithPlayer(helper, spawnSheep(helper, ENTITY_POS), new ItemStack(Items.SHEARS));
            assertWoolMultiple(helper, ENTITY_POS, Items.WOOL.white(), 64, 64, 192,
                    "64x manual sheep");
            for (ItemEntity drop : itemDrops(helper, ENTITY_POS, Items.WOOL.white())) {
                helper.assertTrue(drop.getItem().getCount() <= drop.getItem().getMaxStackSize(),
                        "64x manual sheep emitted an illegal stack");
            }
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest
    public void coloredSheepKeepsItsWoolIdentityAndComponents(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareShearing(helper, config -> config.defaultShearingMultiplier = 3);
            final Sheep sheep = spawnSheep(helper, ENTITY_POS);
            sheep.setColor(DyeColor.BLUE);
            shearWithPlayer(helper, sheep, new ItemStack(Items.SHEARS));
            assertWoolMultiple(helper, ENTITY_POS, Items.WOOL.blue(), 3, 3, 9,
                    "3x blue sheep");
            helper.assertTrue(allItemDrops(helper, ENTITY_POS).stream()
                            .allMatch(drop -> drop.getItem().is(Items.WOOL.blue())),
                    "Colored sheep output was converted to a different wool item");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest
    public void babySheepCannotBeShearedOrDamageTheTool(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareShearing(helper, config -> config.defaultShearingMultiplier = 64);
            final Sheep sheep = spawnSheep(helper, ENTITY_POS);
            sheep.setBaby(true);
            final ItemStack shears = new ItemStack(Items.SHEARS);
            interactWithPlayer(helper, sheep, shears);
            helper.assertFalse(sheep.isSheared(), "Baby sheep incorrectly entered the shearing action");
            helper.assertTrue(shears.getDamageValue() == 0, "Baby sheep damaged the shears");
            assertItemTotal(helper, ENTITY_POS, Items.WOOL.white(), 0, "baby sheep");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest
    public void alreadyShearedSheepCannotRepeatTheAction(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareShearing(helper, config -> config.defaultShearingMultiplier = 64);
            final Sheep sheep = spawnSheep(helper, ENTITY_POS);
            sheep.setSheared(true);
            final ItemStack shears = new ItemStack(Items.SHEARS);
            interactWithPlayer(helper, sheep, shears);
            helper.assertTrue(sheep.isSheared(), "Already-sheared state was unexpectedly cleared");
            helper.assertTrue(shears.getDamageValue() == 0,
                    "An already-sheared sheep damaged the shears again");
            assertItemTotal(helper, ENTITY_POS, Items.WOOL.white(), 0, "already-sheared sheep");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest
    public void realManualShearingDamagesTheToolExactlyOnce(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareShearing(helper, config -> config.defaultShearingMultiplier = 8);
            final ItemStack shears = shearWithPlayer(
                    helper,
                    spawnSheep(helper, ENTITY_POS),
                    new ItemStack(Items.SHEARS));
            helper.assertTrue(shears.getDamageValue() == 1,
                    "8x output changed the one-action durability cost");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest
    public void nonShearsInteractionNeverStartsAQualifiedAction(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareShearing(helper, config -> config.defaultShearingMultiplier = 64);
            final Sheep sheep = spawnSheep(helper, ENTITY_POS);
            final ItemStack stick = new ItemStack(Items.STICK);
            interactWithPlayer(helper, sheep, stick);
            helper.assertFalse(sheep.isSheared(), "A non-shears interaction sheared the sheep");
            helper.assertTrue(stick.getCount() == 1, "A non-shears interaction consumed its item");
            assertItemTotal(helper, ENTITY_POS, Items.WOOL.white(), 0, "non-shears interaction");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest
    public void disabledVanillaDispenserSourceKeepsSheepVanilla(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareShearing(helper, config -> {
                config.automatedShearingDropsEnabled = false;
                config.defaultShearingMultiplier = 64;
            });
            final Sheep sheep = spawnSheep(helper, ENTITY_POS);
            final ItemStack shears = dispenseShears(helper, DISPENSER_POS, Direction.EAST);
            assertItemRange(helper, ENTITY_POS, Items.WOOL.white(), 1, 3,
                    "disabled dispenser source");
            helper.assertTrue(sheep.isSheared(), "Vanilla dispenser did not shear its target");
            helper.assertTrue(shears.getDamageValue() == 1,
                    "Vanilla dispenser did not damage shears exactly once");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest(padding = 16)
    public void enabledVanillaDispenserSourceMultipliesSheep(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareShearing(helper, config -> {
                config.automatedShearingDropsEnabled = true;
                config.defaultShearingMultiplier = 4;
            });
            final Sheep sheep = spawnSheep(helper, ENTITY_POS);
            final ItemStack shears = dispenseShears(helper, DISPENSER_POS, Direction.EAST);
            assertWoolMultiple(helper, ENTITY_POS, Items.WOOL.white(), 4, 4, 12,
                    "enabled dispenser source");
            helper.assertTrue(sheep.isSheared(), "Enabled dispenser source did not finish shearing");
            helper.assertTrue(shears.getDamageValue() == 1,
                    "Multiplied dispenser action changed durability cost");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest
    public void dispenserBeehivePathIsNeverTreatedAsEntityShearing(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareShearing(helper, config -> {
                config.automatedShearingDropsEnabled = true;
                config.defaultShearingMultiplier = 64;
            });
            final BlockPos target = DISPENSER_POS.relative(Direction.EAST);
            helper.getLevel().setBlock(
                    helper.absolutePos(target),
                    Blocks.BEEHIVE.defaultBlockState().setValue(BeehiveBlock.HONEY_LEVEL, 5),
                    Block.UPDATE_ALL);
            final ItemStack shears = dispenseShears(helper, DISPENSER_POS, Direction.EAST);
            assertItemTotal(helper, target, Items.HONEYCOMB, 3, "dispenser beehive");
            helper.assertTrue(
                    helper.getLevel().getBlockState(helper.absolutePos(target))
                            .getValue(BeehiveBlock.HONEY_LEVEL) == 0,
                    "Dispenser beehive did not reset its honey level");
            helper.assertTrue(shears.getDamageValue() == 1,
                    "Dispenser beehive changed vanilla shear durability");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest
    public void dispenserLeashRemovalStopsBeforeEntityShearing(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareShearing(helper, config -> {
                config.automatedShearingDropsEnabled = true;
                config.defaultShearingMultiplier = 64;
            });
            final Sheep sheep = spawnSheep(helper, ENTITY_POS);
            final ServerPlayer holder = GameTestPlayers.survival(helper);
            holder.setPos(sheep.getX(), sheep.getY(), sheep.getZ() - 2.0);
            sheep.setLeashedTo(holder, false);
            helper.assertTrue(sheep.isLeashed(), "Could not prepare the leashed sheep fixture");

            final ItemStack shears = dispenseShears(helper, DISPENSER_POS, Direction.EAST);
            helper.assertFalse(sheep.isLeashed(), "Dispenser did not remove the leash");
            helper.assertFalse(sheep.isSheared(), "Leash removal fell through into entity shearing");
            assertItemTotal(helper, ENTITY_POS, Items.LEAD, 1, "dispenser leash removal");
            assertItemTotal(helper, ENTITY_POS, Items.WOOL.white(), 0, "leashed sheep wool");
            helper.assertTrue(shears.getDamageValue() == 1,
                    "Leash removal did not retain one vanilla durability cost");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest
    public void mooshroomTransformationRemainsFixedVanillaOutput(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareSpecialOverride(helper, "minecraft:mooshroom");
            final MushroomCow mooshroom = helper.spawnWithNoFreeWill(EntityTypes.MOOSHROOM, ENTITY_POS);
            mooshroom.setBaby(false);
            shearWithPlayer(helper, mooshroom, new ItemStack(Items.SHEARS));
            assertItemTotal(helper, ENTITY_POS, Items.RED_MUSHROOM, 5, "special mooshroom");
            helper.assertTrue(helper.getLevel().getEntities(
                            EntityTypes.COW,
                            area(helper, ENTITY_POS, 2.5),
                            entity -> entity.isAlive()).size() == 1,
                    "Mooshroom did not perform exactly one vanilla cow conversion");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest
    public void snowGolemPumpkinRemovalRemainsFixedVanillaOutput(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareSpecialOverride(helper, "minecraft:snow_golem");
            final SnowGolem golem = helper.spawnWithNoFreeWill(EntityTypes.SNOW_GOLEM, ENTITY_POS);
            golem.setPumpkin(true);
            shearWithPlayer(helper, golem, new ItemStack(Items.SHEARS));
            helper.assertFalse(golem.hasPumpkin(), "Snow golem retained its carved pumpkin");
            assertItemTotal(helper, ENTITY_POS, Items.CARVED_PUMPKIN, 1, "special snow golem");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest
    public void boggedMushroomRemovalRemainsFixedVanillaOutput(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareSpecialOverride(helper, "minecraft:bogged");
            final Bogged bogged = helper.spawnWithNoFreeWill(EntityTypes.BOGGED, ENTITY_POS);
            bogged.setSheared(false);
            shearWithPlayer(helper, bogged, new ItemStack(Items.SHEARS));
            helper.assertFalse(bogged.readyForShearing(), "Bogged stayed ready after shearing");
            final int mushrooms = itemTotal(helper, ENTITY_POS, Items.RED_MUSHROOM)
                    + itemTotal(helper, ENTITY_POS, Items.BROWN_MUSHROOM);
            helper.assertTrue(mushrooms == 2,
                    "Special bogged produced " + mushrooms + " mushrooms instead of vanilla 2");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest
    public void copperGolemDirectEquipmentOutputRemainsSingle(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareSpecialOverride(helper, "minecraft:copper_golem");
            final CopperGolem golem = helper.spawnWithNoFreeWill(EntityTypes.COPPER_GOLEM, ENTITY_POS);
            golem.setItemSlot(CopperGolem.EQUIPMENT_SLOT_ANTENNA, new ItemStack(Items.POPPY));
            helper.assertTrue(golem.readyForShearing(), "Copper golem antenna fixture was not shearable");
            shearWithPlayer(helper, golem, new ItemStack(Items.SHEARS));
            helper.assertFalse(golem.readyForShearing(), "Copper golem retained its antenna item");
            assertItemTotal(helper, ENTITY_POS, Items.POPPY, 1, "special copper golem");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest
    public void sulfurCubeDirectEquipmentOutputRemainsSingle(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareSpecialOverride(helper, "minecraft:sulfur_cube");
            final SulfurCube cube = helper.spawnWithNoFreeWill(EntityTypes.SULFUR_CUBE, ENTITY_POS);
            cube.setBaby(false);
            cube.setItemSlot(EquipmentSlot.BODY, new ItemStack(Items.GUNPOWDER));
            helper.assertTrue(cube.readyForShearing(), "Sulfur cube body-item fixture was not shearable");
            shearWithPlayer(helper, cube, new ItemStack(Items.SHEARS));
            helper.assertFalse(cube.readyForShearing(), "Sulfur cube retained its body item");
            assertItemTotal(helper, ENTITY_POS, Items.GUNPOWDER, 1, "special sulfur cube");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest
    public void shearingConfigurationCannotAffectBlockDropPipeline(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareShearing(helper, config -> {
                config.defaultShearingMultiplier = 64;
                config.globalMultiplier = 1;
                config.sourceMode = SmartDropsConfig.SourceMode.ALL;
                config.filterMode = SmartDropsConfig.FilterMode.BLACKLIST;
                config.smartPlacementProtection = false;
                config.dimensionMultipliers.clear();
                config.categoryMultipliers.clear();
                config.blockMultipliers.clear();
                config.blacklist.clear();
                config.whitelist.clear();
                config.tagBlacklist.clear();
                config.tagWhitelist.clear();
            });
            final BlockPos blockPos = helper.absolutePos(ENTITY_POS);
            helper.getLevel().setBlock(blockPos, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            Block.dropResources(
                    Blocks.STONE.defaultBlockState(),
                    helper.getLevel(),
                    blockPos,
                    null,
                    GameTestPlayers.survival(helper),
                    new ItemStack(Items.DIAMOND_PICKAXE));
            assertItemTotal(helper, ENTITY_POS, Items.COBBLESTONE, 1,
                    "block drop while shearing default is 64x");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest
    public void resolverCertifiesSheepThroughTheProductionStandardTag(final GameTestHelper helper) {
        final ShearingRuleTrace trace = ShearingRuleResolver.trace(
                SmartDropsConfig.defaults(),
                EntityTypes.SHEEP,
                ShearingSource.MANUAL_PLAYER);
        helper.assertTrue(trace.classification() == ShearingClassification.STANDARD_RESOURCE,
                "Production shearing certification tag did not classify sheep as standard");
        helper.assertTrue(trace.standardTagged() && !trace.specialTagged(),
                "Production sheep tag membership was incomplete or conflicting");
        helper.succeed();
    }

    @GameTest
    public void resolverTreatsEveryKnownVanillaSpecialAsFixedVanilla(final GameTestHelper helper) {
        final SmartDropsConfig config = SmartDropsConfig.defaults();
        config.manualShearingDropsEnabled = true;
        config.defaultShearingMultiplier = 64;
        config.inheritDefaultShearingMultiplier = false;
        final List<EntityType<?>> specialTypes = List.of(
                EntityTypes.MOOSHROOM,
                EntityTypes.SNOW_GOLEM,
                EntityTypes.BOGGED,
                EntityTypes.COPPER_GOLEM,
                EntityTypes.SULFUR_CUBE);
        for (EntityType<?> type : specialTypes) {
            final ShearingRuleTrace trace = ShearingRuleResolver.trace(
                    config,
                    type,
                    ShearingSource.MANUAL_PLAYER);
            helper.assertTrue(trace.classification() == ShearingClassification.SPECIAL,
                    "Known vanilla special was not classified as special: " + trace.entityId());
            helper.assertTrue(trace.appliedMultiplier() == 1 && trace.fixedVanilla(),
                    "Known vanilla special escaped the fixed 1x gate: " + trace.entityId());
        }
        helper.succeed();
    }

    @GameTest
    public void resolverSpecialTagWinsAConflictingStandardTag(final GameTestHelper helper) {
        final SmartDropsConfig config = SmartDropsConfig.defaults();
        config.shearingEntityMultipliers.put("example:conflict", 64);
        final ShearingRuleTrace trace = ShearingGameTestAccess.syntheticTrace(
                config,
                "example:conflict",
                true,
                true,
                ShearingSource.MANUAL_PLAYER);
        helper.assertTrue(trace.tagConflict(), "Synthetic conflict did not retain both tag facts");
        helper.assertTrue(trace.classification() == ShearingClassification.SPECIAL,
                "Special did not win the conflicting certification tags");
        helper.assertTrue(trace.appliedMultiplier() == 1,
                "Conflicting special entity escaped fixed vanilla 1x");
        helper.succeed();
    }

    @GameTest
    public void resolverUnknownOverrideCannotBypassCertification(final GameTestHelper helper) {
        final SmartDropsConfig config = SmartDropsConfig.defaults();
        config.shearingEntityMultipliers.put("example:uncertified", 64);
        final ShearingRuleTrace trace = ShearingGameTestAccess.syntheticTrace(
                config,
                "example:uncertified",
                false,
                false,
                ShearingSource.MANUAL_PLAYER);
        helper.assertTrue(trace.classification() == ShearingClassification.UNKNOWN,
                "Uncertified fixture was not classified as unknown");
        helper.assertTrue(trace.exactOverride() == 64 && trace.appliedMultiplier() == 1,
                "Unknown exact override bypassed the certification safety gate");
        helper.succeed();
    }

    @GameTest
    public void resolverSafeExactOverrideWinsTheShearingDefault(final GameTestHelper helper) {
        final SmartDropsConfig config = SmartDropsConfig.defaults();
        config.inheritDefaultShearingMultiplier = false;
        config.defaultShearingMultiplier = 7;
        config.shearingEntityMultipliers.put("example:certified", 3);
        final ShearingRuleTrace trace = ShearingGameTestAccess.syntheticTrace(
                config,
                "example:certified",
                true,
                false,
                ShearingSource.MANUAL_PLAYER);
        helper.assertTrue(trace.appliedMultiplier() == 3,
                "Certified exact override did not win the shearing default");
        helper.assertTrue(trace.selectedRule() == ShearingRuleTrace.RuleSource.ENTITY_OVERRIDE,
                "Certified exact override reported the wrong rule source");
        helper.succeed();
    }

    @GameTest
    public void resolverManualAndAutomatedFlagsRemainIndependent(final GameTestHelper helper) {
        final SmartDropsConfig config = SmartDropsConfig.defaults();
        config.manualShearingDropsEnabled = false;
        config.automatedShearingDropsEnabled = true;
        config.inheritDefaultShearingMultiplier = false;
        config.defaultShearingMultiplier = 5;
        final ShearingRuleTrace manual = ShearingGameTestAccess.syntheticTrace(
                config,
                "example:certified",
                true,
                false,
                ShearingSource.MANUAL_PLAYER);
        final ShearingRuleTrace automated = ShearingGameTestAccess.syntheticTrace(
                config,
                "example:certified",
                true,
                false,
                ShearingSource.VANILLA_DISPENSER);
        helper.assertTrue(manual.appliedMultiplier() == 1 && !manual.sourceEnabled(),
                "Disabled manual source did not resolve to vanilla 1x");
        helper.assertTrue(automated.appliedMultiplier() == 5 && automated.sourceEnabled(),
                "Enabled automated source did not resolve its configured multiplier");
        helper.succeed();
    }

    @GameTest
    public void resolverMasterGateAndInheritedGlobalRemainExplicit(final GameTestHelper helper) {
        final SmartDropsConfig config = SmartDropsConfig.defaults();
        config.globalMultiplier = 6;
        config.inheritDefaultShearingMultiplier = true;
        final ShearingRuleTrace inherited = ShearingGameTestAccess.syntheticTrace(
                config,
                "example:certified",
                true,
                false,
                ShearingSource.MANUAL_PLAYER);
        helper.assertTrue(inherited.appliedMultiplier() == 6,
                "Shearing did not inherit the configured global multiplier");
        helper.assertTrue(inherited.selectedRule() == ShearingRuleTrace.RuleSource.GLOBAL_DEFAULT,
                "Inherited global rule reported the wrong source");

        config.enabled = false;
        final ShearingRuleTrace disabled = ShearingGameTestAccess.syntheticTrace(
                config,
                "example:certified",
                true,
                false,
                ShearingSource.MANUAL_PLAYER);
        helper.assertTrue(disabled.appliedMultiplier() == 1,
                "Master gate did not restore vanilla 1x");
        helper.assertTrue(disabled.selectedRule() == ShearingRuleTrace.RuleSource.MOD_DISABLED,
                "Master gate reported the wrong rule source");
        helper.succeed();
    }

    @GameTest
    public void outputBudgetAllowsTheExactOneThousandTwentyFourItemBoundary(final GameTestHelper helper) {
        final ShearingOutputBudget.Result result = ShearingOutputBudget.plan(
                List.of(List.of(new ItemStack(Items.DIAMOND, 16))),
                64);
        helper.assertTrue(result.fits(), "Exact 1024-item output was rejected");
        helper.assertTrue(result.multipliedItems() == 1_024L,
                "Exact item-boundary plan counted the wrong output");
        helper.assertTrue(total(result.outputBatches().getFirst()) == 1_024,
                "Exact item-boundary plan materialized the wrong count");
        helper.succeed();
    }

    @GameTest
    public void outputBudgetRejectsItemOverflowAtomically(final GameTestHelper helper) {
        final ShearingOutputBudget.Result result = ShearingOutputBudget.plan(
                List.of(List.of(new ItemStack(Items.DIAMOND, 17))),
                64);
        helper.assertFalse(result.fits(), "1088-item output escaped the item budget");
        helper.assertTrue(result.limitExceeded() == ShearingOutputBudget.LimitExceeded.ITEMS,
                "Item overflow reported the wrong safety limit");
        helper.assertTrue(result.outputBatches().isEmpty(),
                "Rejected item overflow exposed a partial output plan");
        helper.succeed();
    }

    @GameTest
    public void outputBudgetRejectsSourceStackOverflowAtomically(final GameTestHelper helper) {
        final List<ItemStack> source = new ArrayList<>();
        for (int index = 0; index < 257; index++) {
            source.add(new ItemStack(Items.DIAMOND));
        }
        final ShearingOutputBudget.Result result = ShearingOutputBudget.plan(List.of(source), 1);
        helper.assertFalse(result.fits(), "257 source stacks escaped the source-stack budget");
        helper.assertTrue(result.limitExceeded() == ShearingOutputBudget.LimitExceeded.SOURCE_STACKS,
                "Source-stack overflow reported the wrong safety limit");
        helper.assertTrue(result.outputBatches().isEmpty(),
                "Rejected source-stack overflow exposed a partial output plan");
        helper.succeed();
    }

    @GameTest
    public void outputBudgetRejectsMaterializedStackOverflowAtomically(final GameTestHelper helper) {
        final ShearingOutputBudget.Result result = ShearingOutputBudget.plan(
                List.of(List.of(new ItemStack(Items.SHEARS, 129))),
                2);
        helper.assertFalse(result.fits(), "258 unstackable outputs escaped the materialization budget");
        helper.assertTrue(
                result.limitExceeded() == ShearingOutputBudget.LimitExceeded.MATERIALIZED_STACKS,
                "Materialized-stack overflow reported the wrong safety limit");
        helper.assertTrue(result.outputBatches().isEmpty(),
                "Rejected materialization overflow exposed a partial output plan");
        helper.succeed();
    }

    @GameTest
    public void outputBufferPreservesEachVanillaConsumerBatch(final GameTestHelper helper) {
        final ShearingGameTestAccess.BufferRun run = ShearingGameTestAccess.complete(
                3,
                List.of(
                        List.of(new ItemStack(Items.WOOL.white(), 2)),
                        List.of(new ItemStack(Items.WOOL.blue()))));
        helper.assertTrue(run.fallback() == ShearingOutputBudget.LimitExceeded.NONE,
                "Safe multi-batch output unexpectedly fell back");
        helper.assertTrue(total(run.emittedBatches().get(0)) == 6,
                "First consumer batch did not receive its own multiplied output");
        helper.assertTrue(total(run.emittedBatches().get(1)) == 3,
                "Second consumer batch did not receive its own multiplied output");
        helper.assertTrue(run.emittedBatches().get(0).stream()
                        .allMatch(stack -> stack.is(Items.WOOL.white()))
                        && run.emittedBatches().get(1).stream()
                        .allMatch(stack -> stack.is(Items.WOOL.blue())),
                "Per-batch consumer identities were mixed");
        helper.succeed();
    }

    @GameTest
    public void outputBufferFallsBackTheWholeActionAcrossBatches(final GameTestHelper helper) {
        final ShearingGameTestAccess.BufferRun run = ShearingGameTestAccess.complete(
                64,
                List.of(
                        List.of(new ItemStack(Items.WOOL.white(), 10)),
                        List.of(new ItemStack(Items.WOOL.blue(), 10))));
        helper.assertTrue(run.fallback() == ShearingOutputBudget.LimitExceeded.ITEMS,
                "Cumulative multi-batch overflow did not enter the item fallback");
        helper.assertTrue(total(run.emittedBatches().get(0)) == 10
                        && total(run.emittedBatches().get(1)) == 10,
                "Cumulative overflow emitted a partial multiplier instead of vanilla originals");
        helper.succeed();
    }

    @GameTest
    public void outputBufferAbortRestoresOriginalsAfterAnActionException(final GameTestHelper helper) {
        final ShearingGameTestAccess.BufferRun run = ShearingGameTestAccess.abort(
                8,
                List.of(
                        List.of(new ItemStack(Items.WOOL.white(), 2)),
                        List.of(new ItemStack(Items.WOOL.blue()))));
        helper.assertTrue(total(run.emittedBatches().get(0)) == 2
                        && total(run.emittedBatches().get(1)) == 1,
                "Exception abort did not restore the closest vanilla 1x output");
        helper.assertFalse(run.rollbackWarning(),
                "Exception abort reported an unexpected rollback-emission failure");
        helper.succeed();
    }

    @GameTest
    public void nestedShearingContextsRestoreTheOuterIdentity(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareShearing(helper, config -> config.defaultShearingMultiplier = 2);
            final ServerPlayer player = GameTestPlayers.survival(helper);
            final Sheep outerTarget = spawnSheep(helper, new BlockPos(3, 2, 4));
            final Sheep innerTarget = spawnSheep(helper, new BlockPos(6, 2, 4));
            try (ShearingActionContext.Scope outer = ShearingActionContext.beginManual(
                    outerTarget,
                    helper.getLevel(),
                    player)) {
                helper.assertTrue(ShearingActionContext.activeTrace(outerTarget) != null,
                        "Outer shearing identity was not installed");
                try (ShearingActionContext.Scope inner = ShearingActionContext.beginManual(
                        innerTarget,
                        helper.getLevel(),
                        player)) {
                    helper.assertTrue(ShearingActionContext.activeTrace(outerTarget) == null,
                            "Outer target captured output while an inner action was active");
                    helper.assertTrue(ShearingActionContext.activeTrace(innerTarget) != null,
                            "Inner shearing identity was not installed");
                }
                helper.assertTrue(ShearingActionContext.activeTrace(outerTarget) != null,
                        "Closing the inner action did not restore the outer identity");
            }
            helper.assertTrue(ShearingActionContext.activeTrace(outerTarget) == null,
                    "Closing the outer action leaked thread-local state");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest
    public void disabledSameTargetInnerSourceMasksAndRestoresEligibleOuter(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareShearing(helper, config -> {
                config.manualShearingDropsEnabled = true;
                config.automatedShearingDropsEnabled = false;
                config.defaultShearingMultiplier = 2;
            });
            final ServerPlayer player = GameTestPlayers.survival(helper);
            final Sheep target = spawnSheep(helper, ENTITY_POS);
            final int[] emitted = {0};
            try (ShearingActionContext.Scope outer = ShearingActionContext.beginManual(
                    target,
                    helper.getLevel(),
                    player)) {
                helper.assertTrue(
                        ShearingActionContext.activeTrace(target) != null
                                && ShearingActionContext.activeTrace(target).source()
                                == ShearingSource.MANUAL_PLAYER,
                        "Eligible manual outer scope was not active");
                try (ShearingActionContext.Scope inner = ShearingActionContext.beginDispenser(
                        target,
                        helper.getLevel())) {
                    final ShearingRuleTrace innerTrace = ShearingActionContext.activeTrace(target);
                    helper.assertTrue(innerTrace != null
                                    && innerTrace.source() == ShearingSource.VANILLA_DISPENSER
                                    && !innerTrace.sourceEnabled()
                                    && innerTrace.appliedMultiplier() == 1,
                            "Disabled same-target dispenser scope did not mask the manual source");
                    final BiConsumer<ServerLevel, ItemStack> output =
                            ShearingActionContext.wrapLootConsumer(
                                    target,
                                    helper.getLevel(),
                                    (level, stack) -> emitted[0] += stack.getCount());
                    output.accept(helper.getLevel(), new ItemStack(Items.WOOL.white()));
                    inner.complete();
                    helper.assertTrue(emitted[0] == 1,
                            "Disabled inner source leaked its output into the eligible outer buffer");
                }
                helper.assertTrue(
                        ShearingActionContext.activeTrace(target) != null
                                && ShearingActionContext.activeTrace(target).source()
                                == ShearingSource.MANUAL_PLAYER,
                        "Closing the disabled inner source did not restore the manual outer scope");
                outer.complete();
            }
            helper.assertTrue(emitted[0] == 1,
                    "Closing the outer scope replayed output already owned by the disabled inner source");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest
    public void mismatchedEntityCannotCaptureAnotherShearingActionOutput(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareShearing(helper, config -> config.defaultShearingMultiplier = 2);
            final ServerPlayer player = GameTestPlayers.survival(helper);
            final Sheep activeTarget = spawnSheep(helper, new BlockPos(3, 2, 4));
            final Sheep unrelatedTarget = spawnSheep(helper, new BlockPos(6, 2, 4));
            final BiConsumer<ServerLevel, ItemStack> downstream = (level, stack) -> { };
            try (ShearingActionContext.Scope ignored = ShearingActionContext.beginManual(
                    activeTarget,
                    helper.getLevel(),
                    player)) {
                final BiConsumer<ServerLevel, ItemStack> wrapped =
                        ShearingActionContext.wrapLootConsumer(
                                unrelatedTarget,
                                helper.getLevel(),
                                downstream);
                helper.assertTrue(wrapped == downstream,
                        "Mismatched entity identity captured another action's output");
            }
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest
    public void shearingStatusCommandIsReadOnlyAndServerAuthoritative(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareShearing(helper, config -> {
                config.manualShearingDropsEnabled = true;
                config.automatedShearingDropsEnabled = false;
                config.defaultShearingMultiplier = 7;
            });
            final long revision = ConfigManager.revision();
            final CapturingCommandSource messages = new CapturingCommandSource();
            final CommandSourceStack source = helper.getLevel().getServer()
                    .createCommandSourceStack()
                    .withSource(messages);
            helper.assertTrue(executeCommand(helper, "smartdrops shearing status", source) == 1,
                    "Shearing status command failed");
            helper.assertTrue(messages.text().contains("Smart Resource Multiplier shearing")
                            && messages.text().contains("outputBudget=1024 items/256 source or materialized stacks"),
                    "Shearing status omitted its state or safety budget");
            helper.assertTrue(ConfigManager.revision() == revision,
                    "Read-only shearing status changed the authoritative revision");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest
    public void shearingAdminCommandsMutateOnlyAuthoritativeShearingFields(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareShearing(helper, config -> { });
            final CommandSourceStack source = helper.getLevel().getServer().createCommandSourceStack();
            helper.assertTrue(executeCommand(helper, "smartdrops admin shearing manual off", source) == 1,
                    "Manual shearing admin command failed");
            helper.assertTrue(executeCommand(helper, "smartdrops admin shearing automated on", source) == 1,
                    "Automated shearing admin command failed");
            helper.assertTrue(executeCommand(helper, "smartdrops admin shearing multiplier 7", source) == 1,
                    "Default shearing multiplier admin command failed");
            helper.assertTrue(executeCommand(
                            helper,
                            "smartdrops admin shearing entity minecraft:sheep 5",
                            source) == 1,
                    "Certified sheep override admin command failed");
            final SmartDropsConfig active = ConfigManager.get();
            helper.assertFalse(active.manualShearingDropsEnabled,
                    "Manual shearing admin command did not update the authoritative config");
            helper.assertTrue(active.automatedShearingDropsEnabled,
                    "Automated shearing admin command did not update the authoritative config");
            helper.assertFalse(active.inheritDefaultShearingMultiplier,
                    "Numeric shearing default did not disable inheritance");
            helper.assertTrue(active.defaultShearingMultiplier == 7
                            && active.shearingEntityMultipliers.get("minecraft:sheep") == 5,
                    "Shearing admin multipliers were not stored authoritatively");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest
    public void shearingAdminRejectsSpecialAddsButAllowsTheirRemoval(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareShearing(helper, config -> config.shearingEntityMultipliers.put(
                    "minecraft:mooshroom",
                    9));
            final CapturingCommandSource messages = new CapturingCommandSource();
            final CommandSourceStack source = helper.getLevel().getServer()
                    .createCommandSourceStack()
                    .withSource(messages);
            helper.assertTrue(executeCommand(
                            helper,
                            "smartdrops admin shearing entity minecraft:mooshroom 64",
                            source) == 0,
                    "Special mooshroom override was incorrectly accepted");
            helper.assertTrue(messages.text().contains("fixed at vanilla 1x"),
                    "Special override rejection omitted its safety explanation");
            helper.assertTrue(executeCommand(
                            helper,
                            "smartdrops admin shearing entity minecraft:mooshroom inherit",
                            source) == 1,
                    "Special override removal was incorrectly rejected");
            helper.assertFalse(ConfigManager.get().shearingEntityMultipliers.containsKey(
                            "minecraft:mooshroom"),
                    "Special override removal did not clear the stored rule");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest
    public void entityInspectionReportsShearingWithoutMutation(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareShearing(helper, config -> config.defaultShearingMultiplier = 3);
            final Sheep sheep = spawnSheep(helper, ENTITY_POS);
            final ServerPlayer player = GameTestPlayers.survival(helper);
            final ItemStack shears = new ItemStack(Items.SHEARS);
            player.setItemInHand(InteractionHand.MAIN_HAND, shears);
            player.setPos(sheep.getX(), sheep.getY(), sheep.getZ() - 2.5);
            aimAt(player, sheep.getEyePosition());
            final long revision = ConfigManager.revision();
            final float health = sheep.getHealth();
            final Vec3 position = sheep.position();
            final int itemCount = allItemDrops(helper, ENTITY_POS).size();
            final CapturingCommandSource messages = new CapturingCommandSource();

            helper.assertTrue(executeCommand(
                            helper,
                            "smartdrops inspect entity verbose",
                            player.createCommandSourceStack().withSource(messages)) == 1,
                    "Verbose sheep inspection failed");
            helper.assertTrue(messages.text().contains("Entity shearing")
                            && messages.text().contains("Effective manual multiplier")
                            && messages.text().contains("Output safety budget"),
                    "Verbose sheep inspection omitted its shearing diagnostics");
            helper.assertTrue(sheep.isAlive() && !sheep.isSheared() && sheep.getHealth() == health,
                    "Sheep inspection changed health or shear state");
            helper.assertTrue(sheep.position().equals(position), "Sheep inspection moved its target");
            helper.assertTrue(shears.getDamageValue() == 0, "Sheep inspection damaged the held shears");
            helper.assertTrue(allItemDrops(helper, ENTITY_POS).size() == itemCount,
                    "Sheep inspection spawned output");
            helper.assertTrue(ConfigManager.revision() == revision,
                    "Sheep inspection changed the authoritative config revision");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest
    public void dedicatedServerAuditsAllThreeShearingMixins(final GameTestHelper helper) {
        final FabricLoader loader = FabricLoader.getInstance();
        helper.assertTrue(loader.getEnvironmentType() == EnvType.SERVER,
                "Shearing GameTests must run on the dedicated-server environment");
        final ClassLoader classLoader = loader.getClass().getClassLoader();
        for (String resource : List.of(
                "com/chedidandrew/smartresourcedrops/mixin/PlayerShearingContextMixin.class",
                "com/chedidandrew/smartresourcedrops/mixin/ShearsDispenseItemBehaviorMixin.class",
                "com/chedidandrew/smartresourcedrops/mixin/LivingEntityShearingLootMixin.class")) {
            helper.assertTrue(classLoader.getResource(resource) != null,
                    "Dedicated server omitted required shearing mixin class " + resource);
        }
        MixinEnvironment.getCurrentEnvironment().audit();
        helper.succeed();
    }

    @GameTest
    public void resetDefaultsEnableOnlyManualShearingAndClearOverrides(final GameTestHelper helper) {
        final SmartDropsConfig defaults = SmartDropsConfig.defaults();
        helper.assertTrue(defaults.manualShearingDropsEnabled,
                "Fresh/reset defaults did not enable manual shearing");
        helper.assertFalse(defaults.automatedShearingDropsEnabled,
                "Fresh/reset defaults unexpectedly enabled automated shearing");
        helper.assertTrue(defaults.inheritDefaultShearingMultiplier,
                "Fresh/reset defaults did not inherit the global multiplier");
        helper.assertTrue(defaults.shearingEntityMultipliers.isEmpty(),
                "Fresh/reset defaults retained individual shearing overrides");
        helper.succeed();
    }

    private static void prepareSpecialOverride(
            final GameTestHelper helper,
            final String entityId
    ) {
        prepareShearing(helper, config -> {
            config.defaultShearingMultiplier = 64;
            config.shearingEntityMultipliers.put(entityId, 64);
        });
    }

    private static void prepareShearing(
            final GameTestHelper helper,
            final Consumer<SmartDropsConfig> customization
    ) {
        helper.assertTrue(ConfigManager.update(config -> {
            config.enabled = true;
            config.globalMultiplier = 1;
            config.maximumMultiplier = 64;
            config.manualShearingDropsEnabled = true;
            config.automatedShearingDropsEnabled = false;
            config.inheritDefaultShearingMultiplier = false;
            config.defaultShearingMultiplier = 1;
            config.shearingEntityMultipliers.clear();
            customization.accept(config);
        }), "Could not prepare the shearing GameTest configuration");
    }

    private static Sheep spawnSheep(final GameTestHelper helper, final BlockPos position) {
        final Sheep sheep = helper.spawnWithNoFreeWill(EntityTypes.SHEEP, position);
        sheep.setBaby(false);
        sheep.setSheared(false);
        sheep.setColor(DyeColor.WHITE);
        return sheep;
    }

    private static ItemStack shearWithPlayer(
            final GameTestHelper helper,
            final LivingEntity target,
            final ItemStack tool
    ) {
        interactWithPlayer(helper, target, tool);
        helper.assertTrue(!tool.isEmpty(), "One shearing action unexpectedly destroyed a fresh tool");
        return tool;
    }

    private static void interactWithPlayer(
            final GameTestHelper helper,
            final LivingEntity target,
            final ItemStack heldItem
    ) {
        final ServerPlayer player = GameTestPlayers.survival(helper);
        player.setPos(target.getX(), target.getY(), target.getZ() - 2.0);
        player.setItemInHand(InteractionHand.MAIN_HAND, heldItem);
        player.interactOn(target, InteractionHand.MAIN_HAND, target.position());
    }

    private static ItemStack dispenseShears(
            final GameTestHelper helper,
            final BlockPos relativeDispenserPos,
            final Direction facing
    ) {
        final BlockPos dispenserPos = helper.absolutePos(relativeDispenserPos);
        final BlockState state = Blocks.DISPENSER.defaultBlockState()
                .setValue(DispenserBlock.FACING, facing);
        helper.getLevel().setBlock(dispenserPos, state, Block.UPDATE_ALL);
        final DispenserBlockEntity blockEntity = (DispenserBlockEntity) helper.getLevel()
                .getBlockEntity(dispenserPos);
        helper.assertTrue(blockEntity != null, "Could not create the real dispenser block entity");
        final DispenseItemBehavior behavior = DispenserBlock.DISPENSER_REGISTRY.get(Items.SHEARS);
        helper.assertTrue(behavior != null, "Vanilla shears dispenser behavior was not registered");
        final ItemStack shears = new ItemStack(Items.SHEARS);
        behavior.dispense(new BlockSource(helper.getLevel(), dispenserPos, state, blockEntity), shears);
        return shears;
    }

    private static int executeCommand(
            final GameTestHelper helper,
            final String command,
            final CommandSourceStack source
    ) {
        try {
            return helper.getLevel().getServer().getCommands().getDispatcher().execute(command, source);
        } catch (CommandSyntaxException exception) {
            throw new AssertionError("Shearing command failed: " + command, exception);
        }
    }

    private static void aimAt(final ServerPlayer player, final Vec3 target) {
        final Vec3 eye = player.getEyePosition();
        final double xDelta = target.x - eye.x;
        final double zDelta = target.z - eye.z;
        final double horizontal = Math.sqrt(xDelta * xDelta + zDelta * zDelta);
        final float yaw = (float) Math.toDegrees(Math.atan2(zDelta, xDelta)) - 90.0F;
        final float pitch = (float) -Math.toDegrees(Math.atan2(target.y - eye.y, horizontal));
        player.absSnapRotationTo(yaw, pitch);
    }

    private static void assertWoolMultiple(
            final GameTestHelper helper,
            final BlockPos position,
            final Item wool,
            final int divisor,
            final int minimum,
            final int maximum,
            final String scenario
    ) {
        final int total = itemTotal(helper, position, wool);
        helper.assertTrue(total >= minimum && total <= maximum && total % divisor == 0,
                scenario + " produced " + total + " items; expected a multiple of "
                        + divisor + " in [" + minimum + ", " + maximum + "]");
    }

    private static void assertItemRange(
            final GameTestHelper helper,
            final BlockPos position,
            final Item item,
            final int minimum,
            final int maximum,
            final String scenario
    ) {
        final int total = itemTotal(helper, position, item);
        helper.assertTrue(total >= minimum && total <= maximum,
                scenario + " produced " + total + " items; expected ["
                        + minimum + ", " + maximum + "]");
    }

    private static void assertItemTotal(
            final GameTestHelper helper,
            final BlockPos position,
            final Item item,
            final int expected,
            final String scenario
    ) {
        final int actual = itemTotal(helper, position, item);
        helper.assertTrue(actual == expected,
                scenario + " produced " + actual + " " + item + " items instead of " + expected);
    }

    private static int itemTotal(
            final GameTestHelper helper,
            final BlockPos position,
            final Item item
    ) {
        return total(itemDrops(helper, position, item).stream()
                .map(ItemEntity::getItem)
                .toList());
    }

    private static int total(final List<ItemStack> stacks) {
        return stacks.stream()
                .filter(stack -> stack != null && !stack.isEmpty())
                .mapToInt(ItemStack::getCount)
                .sum();
    }

    private static List<ItemEntity> itemDrops(
            final GameTestHelper helper,
            final BlockPos position,
            final Item item
    ) {
        return allItemDrops(helper, position).stream()
                .filter(drop -> drop.getItem().is(item))
                .toList();
    }

    private static List<ItemEntity> allItemDrops(
            final GameTestHelper helper,
            final BlockPos position
    ) {
        return helper.getLevel().getEntities(
                EntityTypes.ITEM,
                area(helper, position, 2.5),
                ItemEntity::isAlive);
    }

    private static AABB area(
            final GameTestHelper helper,
            final BlockPos position,
            final double radius
    ) {
        final Vec3 center = Vec3.atCenterOf(helper.absolutePos(position));
        return new AABB(center, center).inflate(radius);
    }

    private static void restoreConfiguration(final SmartDropsConfig previous) {
        if (!ConfigManager.update(config -> copyConfiguration(config, previous))) {
            throw new AssertionError("Could not restore the shearing GameTest configuration");
        }
    }

    static void copyConfiguration(
            final SmartDropsConfig target,
            final SmartDropsConfig source
    ) {
        target.schemaVersion = source.schemaVersion;
        target.enabled = source.enabled;
        target.globalMultiplier = source.globalMultiplier;
        target.maximumMultiplier = source.maximumMultiplier;
        target.sourceMode = source.sourceMode;
        target.filterMode = source.filterMode;
        target.smartPlacementProtection = source.smartPlacementProtection;
        target.protectBlockEntities = source.protectBlockEntities;
        target.playerMining = source.playerMining;
        target.explosions = source.explosions;
        target.automatedMining = source.automatedMining;
        target.multiplyExperience = source.multiplyExperience;
        target.experienceMultiplier = source.experienceMultiplier;
        target.conservativePistonProtection = source.conservativePistonProtection;
        target.allowPlayerOverrides = source.allowPlayerOverrides;
        target.maxPlayerMultiplier = source.maxPlayerMultiplier;
        target.statisticsEnabled = source.statisticsEnabled;

        target.entityDropsEnabled = source.entityDropsEnabled;
        target.inheritDefaultEntityMultiplier = source.inheritDefaultEntityMultiplier;
        target.defaultEntityMultiplier = source.defaultEntityMultiplier;
        target.entityKillRequirement = source.entityKillRequirement;
        target.entityFilterMode = source.entityFilterMode;
        target.bossDropsEnabled = source.bossDropsEnabled;
        target.multiplyMobExperience = source.multiplyMobExperience;
        target.mobExperienceMultiplier = source.mobExperienceMultiplier;
        target.multiplyBossExperience = source.multiplyBossExperience;

        target.manualShearingDropsEnabled = source.manualShearingDropsEnabled;
        target.automatedShearingDropsEnabled = source.automatedShearingDropsEnabled;
        target.inheritDefaultShearingMultiplier = source.inheritDefaultShearingMultiplier;
        target.defaultShearingMultiplier = source.defaultShearingMultiplier;

        copy(target.dimensionMultipliers, source.dimensionMultipliers);
        copy(target.categoryMultipliers, source.categoryMultipliers);
        copy(target.blockMultipliers, source.blockMultipliers);
        copy(target.blacklist, source.blacklist);
        copy(target.whitelist, source.whitelist);
        copy(target.tagBlacklist, source.tagBlacklist);
        copy(target.tagWhitelist, source.tagWhitelist);
        copy(target.blockEntityAllowlist, source.blockEntityAllowlist);
        copy(target.playerMultipliers, source.playerMultipliers);
        copy(target.entityCategoryMultipliers, source.entityCategoryMultipliers);
        copy(target.entityMultipliers, source.entityMultipliers);
        copy(target.entityBlacklist, source.entityBlacklist);
        copy(target.entityWhitelist, source.entityWhitelist);
        copy(target.entityTagBlacklist, source.entityTagBlacklist);
        copy(target.entityTagWhitelist, source.entityTagWhitelist);
        copy(target.shearingEntityMultipliers, source.shearingEntityMultipliers);
    }

    private static <K, V> void copy(
            final java.util.Map<K, V> target,
            final java.util.Map<K, V> source
    ) {
        target.clear();
        target.putAll(source);
    }

    private static <T> void copy(
            final java.util.Set<T> target,
            final java.util.Set<T> source
    ) {
        target.clear();
        target.addAll(source);
    }

    private static final class CapturingCommandSource implements CommandSource {
        private final List<Component> messages = new ArrayList<>();

        @Override
        public void sendSystemMessage(final Component message) {
            messages.add(message.copy());
        }

        @Override
        public boolean acceptsSuccess() {
            return true;
        }

        @Override
        public boolean acceptsFailure() {
            return true;
        }

        @Override
        public boolean shouldInformAdmins() {
            return false;
        }

        private String text() {
            return String.join("\n", messages.stream().map(Component::getString).toList());
        }
    }
}
