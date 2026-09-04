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
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.gametest.framework.GameTestHelper;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Bogged;
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Dedicated-server black-box and focused invariant coverage for entity shearing multiplication. */
public final class SmartResourceDropsShearingGameTests {
    private static final BlockPos ENTITY_POS = new BlockPos(4, 2, 4);
    private static final BlockPos DISPENSER_POS = new BlockPos(3, 2, 4);

    @GameTest(structure = "smart_resource_drops_gametest:wide")
    public void freshManualDefaultInheritsGlobalTwoForRealSheep(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareShearing(helper, config -> {
                config.globalMultiplier = 2;
                config.inheritDefaultShearingMultiplier = true;
            });
            final Sheep sheep = spawnSheep(helper, ENTITY_POS);
            shearWithPlayer(helper, sheep, new ItemStack(Items.SHEARS));
            assertWoolMultiple(helper, ENTITY_POS, Items.WHITE_WOOL, 2, 2, 6,
                    "fresh inherited 2x manual sheep");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
    public void disabledManualSourceKeepsRealSheepVanilla(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareShearing(helper, config -> {
                config.manualShearingDropsEnabled = false;
                config.defaultShearingMultiplier = 64;
            });
            final Sheep sheep = spawnSheep(helper, ENTITY_POS);
            final ItemStack shears = shearWithPlayer(helper, sheep, new ItemStack(Items.SHEARS));
            assertItemRange(helper, ENTITY_POS, Items.WHITE_WOOL, 1, 3,
                    "disabled manual source");
            GameTestAssertions.assertTrue(helper, sheep.isSheared(), "Disabled multiplication prevented vanilla shearing");
            GameTestAssertions.assertTrue(helper, shears.getDamageValue() == 1,
                    "Disabled multiplication changed vanilla tool damage");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
    public void zeroMultiplierSuppressesWoolButCompletesShearing(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareShearing(helper, config -> config.defaultShearingMultiplier = 0);
            final Sheep sheep = spawnSheep(helper, ENTITY_POS);
            final ItemStack shears = shearWithPlayer(helper, sheep, new ItemStack(Items.SHEARS));
            assertItemTotal(helper, ENTITY_POS, Items.WHITE_WOOL, 0, "0x manual sheep");
            GameTestAssertions.assertTrue(helper, sheep.isSheared(), "0x did not preserve the vanilla sheared state");
            GameTestAssertions.assertTrue(helper, shears.getDamageValue() == 1,
                    "0x did not preserve exactly one point of shear damage");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
    public void oneMultiplierPreservesVanillaSheepBounds(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareShearing(helper, config -> config.defaultShearingMultiplier = 1);
            shearWithPlayer(helper, spawnSheep(helper, ENTITY_POS), new ItemStack(Items.SHEARS));
            assertItemRange(helper, ENTITY_POS, Items.WHITE_WOOL, 1, 3, "1x manual sheep");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
    public void twoMultiplierScalesFinalVanillaSheepLoot(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareShearing(helper, config -> config.defaultShearingMultiplier = 2);
            shearWithPlayer(helper, spawnSheep(helper, ENTITY_POS), new ItemStack(Items.SHEARS));
            assertWoolMultiple(helper, ENTITY_POS, Items.WHITE_WOOL, 2, 2, 6,
                    "2x manual sheep");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
    public void sixtyFourMultiplierUsesLegalStacksForRealSheep(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareShearing(helper, config -> config.defaultShearingMultiplier = 64);
            shearWithPlayer(helper, spawnSheep(helper, ENTITY_POS), new ItemStack(Items.SHEARS));
            assertWoolMultiple(helper, ENTITY_POS, Items.WHITE_WOOL, 64, 64, 192,
                    "64x manual sheep");
            for (ItemEntity drop : itemDrops(helper, ENTITY_POS, Items.WHITE_WOOL)) {
                GameTestAssertions.assertTrue(helper, drop.getItem().getCount() <= drop.getItem().getMaxStackSize(),
                        "64x manual sheep emitted an illegal stack");
            }
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
    public void coloredSheepKeepsItsWoolIdentityAndComponents(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareShearing(helper, config -> config.defaultShearingMultiplier = 3);
            final Sheep sheep = spawnSheep(helper, ENTITY_POS);
            sheep.setColor(DyeColor.BLUE);
            shearWithPlayer(helper, sheep, new ItemStack(Items.SHEARS));
            assertWoolMultiple(helper, ENTITY_POS, Items.BLUE_WOOL, 3, 3, 9,
                    "3x blue sheep");
            GameTestAssertions.assertTrue(helper, allItemDrops(helper, ENTITY_POS).stream()
                            .allMatch(drop -> drop.getItem().is(Items.BLUE_WOOL)),
                    "Colored sheep output was converted to a different wool item");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
    public void babySheepCannotBeShearedOrDamageTheTool(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareShearing(helper, config -> config.defaultShearingMultiplier = 64);
            final Sheep sheep = spawnSheep(helper, ENTITY_POS);
            sheep.setBaby(true);
            final ItemStack shears = new ItemStack(Items.SHEARS);
            interactWithPlayer(helper, sheep, shears);
            GameTestAssertions.assertFalse(helper, sheep.isSheared(), "Baby sheep incorrectly entered the shearing action");
            GameTestAssertions.assertTrue(helper, shears.getDamageValue() == 0, "Baby sheep damaged the shears");
            assertItemTotal(helper, ENTITY_POS, Items.WHITE_WOOL, 0, "baby sheep");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
    public void alreadyShearedSheepCannotRepeatTheAction(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareShearing(helper, config -> config.defaultShearingMultiplier = 64);
            final Sheep sheep = spawnSheep(helper, ENTITY_POS);
            sheep.setSheared(true);
            final ItemStack shears = new ItemStack(Items.SHEARS);
            interactWithPlayer(helper, sheep, shears);
            GameTestAssertions.assertTrue(helper, sheep.isSheared(), "Already-sheared state was unexpectedly cleared");
            GameTestAssertions.assertTrue(helper, shears.getDamageValue() == 0,
                    "An already-sheared sheep damaged the shears again");
            assertItemTotal(helper, ENTITY_POS, Items.WHITE_WOOL, 0, "already-sheared sheep");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
    public void realManualShearingDamagesTheToolExactlyOnce(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareShearing(helper, config -> config.defaultShearingMultiplier = 8);
            final ItemStack shears = shearWithPlayer(
                    helper,
                    spawnSheep(helper, ENTITY_POS),
                    new ItemStack(Items.SHEARS));
            GameTestAssertions.assertTrue(helper, shears.getDamageValue() == 1,
                    "8x output changed the one-action durability cost");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
    public void nonShearsInteractionNeverStartsAQualifiedAction(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareShearing(helper, config -> config.defaultShearingMultiplier = 64);
            final Sheep sheep = spawnSheep(helper, ENTITY_POS);
            final ItemStack stick = new ItemStack(Items.STICK);
            interactWithPlayer(helper, sheep, stick);
            GameTestAssertions.assertFalse(helper, sheep.isSheared(), "A non-shears interaction sheared the sheep");
            GameTestAssertions.assertTrue(helper, stick.getCount() == 1, "A non-shears interaction consumed its item");
            assertItemTotal(helper, ENTITY_POS, Items.WHITE_WOOL, 0, "non-shears interaction");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
    public void disabledVanillaDispenserSourceKeepsSheepVanilla(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareShearing(helper, config -> {
                config.automatedShearingDropsEnabled = false;
                config.defaultShearingMultiplier = 64;
            });
            final Sheep sheep = spawnSheep(helper, ENTITY_POS);
            final ItemStack shears = dispenseShears(helper, DISPENSER_POS, Direction.EAST);
            assertItemRange(helper, ENTITY_POS, Items.WHITE_WOOL, 1, 3,
                    "disabled dispenser source");
            GameTestAssertions.assertTrue(helper, sheep.isSheared(), "Vanilla dispenser did not shear its target");
            GameTestAssertions.assertTrue(helper, shears.getDamageValue() == 1,
                    "Vanilla dispenser did not damage shears exactly once");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
    public void enabledVanillaDispenserSourceMultipliesSheep(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareShearing(helper, config -> {
                config.automatedShearingDropsEnabled = true;
                config.defaultShearingMultiplier = 4;
            });
            final Sheep sheep = spawnSheep(helper, ENTITY_POS);
            final ItemStack shears = dispenseShears(helper, DISPENSER_POS, Direction.EAST);
            assertWoolMultiple(helper, ENTITY_POS, Items.WHITE_WOOL, 4, 4, 12,
                    "enabled dispenser source");
            GameTestAssertions.assertTrue(helper, sheep.isSheared(), "Enabled dispenser source did not finish shearing");
            GameTestAssertions.assertTrue(helper, shears.getDamageValue() == 1,
                    "Multiplied dispenser action changed durability cost");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
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
            GameTestAssertions.assertTrue(helper,
                    helper.getLevel().getBlockState(helper.absolutePos(target))
                            .getValue(BeehiveBlock.HONEY_LEVEL) == 0,
                    "Dispenser beehive did not reset its honey level");
            GameTestAssertions.assertTrue(helper, shears.getDamageValue() == 1,
                    "Dispenser beehive changed vanilla shear durability");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
    public void dispenserCutsTheLeashBeforeShearingTheSheep(final GameTestHelper helper) {
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
            GameTestAssertions.assertTrue(helper, sheep.isLeashed(), "Could not prepare the leashed sheep fixture");

            final ItemStack leashCuttingShears = dispenseShears(helper, DISPENSER_POS, Direction.EAST);
            GameTestAssertions.assertFalse(helper, sheep.isLeashed(),
                    "The first 1.21.9+ dispenser action did not cut the sheep's leash");
            GameTestAssertions.assertFalse(helper, sheep.isSheared(),
                    "The leash-cutting action incorrectly continued into sheep shearing");
            assertItemTotal(helper, ENTITY_POS, Items.LEAD, 1, "leash-cutting action");
            assertItemTotal(helper, ENTITY_POS, Items.WHITE_WOOL, 0, "leash-cutting action");
            GameTestAssertions.assertTrue(helper, leashCuttingShears.getDamageValue() == 1,
                    "Leash cutting did not retain one vanilla durability cost");

            final ItemStack shearingShears = dispenseShears(helper, DISPENSER_POS, Direction.EAST);
            GameTestAssertions.assertTrue(helper, sheep.isSheared(),
                    "The second dispenser action did not shear the now-unleashed sheep");
            final int wool = itemTotal(helper, ENTITY_POS, Items.WHITE_WOOL);
            GameTestAssertions.assertTrue(helper, wool >= 64 && wool <= 192 && wool % 64 == 0,
                    "Leashed sheep output did not preserve 1-3 vanilla wool emissions at 64x");
            GameTestAssertions.assertTrue(helper, shearingShears.getDamageValue() == 1,
                    "Sheep shearing did not retain one vanilla durability cost");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
    public void mooshroomTransformationRemainsFixedVanillaOutput(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareSpecialOverride(helper, "minecraft:mooshroom");
            final MushroomCow mooshroom = helper.spawnWithNoFreeWill(EntityType.MOOSHROOM, ENTITY_POS);
            mooshroom.setBaby(false);
            shearWithPlayer(helper, mooshroom, new ItemStack(Items.SHEARS));
            assertItemTotal(helper, ENTITY_POS, Items.RED_MUSHROOM, 5, "special mooshroom");
            GameTestAssertions.assertTrue(helper, helper.getLevel().getEntities(
                            EntityType.COW,
                            area(helper, ENTITY_POS, 2.5),
                            entity -> entity.isAlive()).size() == 1,
                    "Mooshroom did not perform exactly one vanilla cow conversion");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
    public void snowGolemPumpkinRemovalRemainsFixedVanillaOutput(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareSpecialOverride(helper, "minecraft:snow_golem");
            final SnowGolem golem = helper.spawnWithNoFreeWill(EntityType.SNOW_GOLEM, ENTITY_POS);
            golem.setPumpkin(true);
            shearWithPlayer(helper, golem, new ItemStack(Items.SHEARS));
            GameTestAssertions.assertFalse(helper, golem.hasPumpkin(), "Snow golem retained its carved pumpkin");
            assertItemTotal(helper, ENTITY_POS, Items.CARVED_PUMPKIN, 1, "special snow golem");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
    public void boggedMushroomRemovalRemainsFixedVanillaOutput(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareSpecialOverride(helper, "minecraft:bogged");
            final Bogged bogged = helper.spawnWithNoFreeWill(EntityType.BOGGED, ENTITY_POS);
            bogged.setSheared(false);
            shearWithPlayer(helper, bogged, new ItemStack(Items.SHEARS));
            GameTestAssertions.assertFalse(helper, bogged.readyForShearing(), "Bogged stayed ready after shearing");
            final int mushrooms = itemTotal(helper, ENTITY_POS, Items.RED_MUSHROOM)
                    + itemTotal(helper, ENTITY_POS, Items.BROWN_MUSHROOM);
            GameTestAssertions.assertTrue(helper, mushrooms == 2,
                    "Special bogged produced " + mushrooms + " mushrooms instead of vanilla 2");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
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

    @GameTest(structure = "smart_resource_drops_gametest:wide")
    public void resolverCertifiesSheepThroughTheProductionStandardTag(final GameTestHelper helper) {
        final ShearingRuleTrace trace = ShearingRuleResolver.trace(
                SmartDropsConfig.defaults(),
                EntityType.SHEEP,
                ShearingSource.MANUAL_PLAYER);
        GameTestAssertions.assertTrue(helper, trace.classification() == ShearingClassification.STANDARD_RESOURCE,
                "Production shearing certification tag did not classify sheep as standard");
        GameTestAssertions.assertTrue(helper, trace.standardTagged() && !trace.specialTagged(),
                "Production sheep tag membership was incomplete or conflicting");
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
    public void resolverTreatsEveryKnownVanillaSpecialAsFixedVanilla(final GameTestHelper helper) {
        final SmartDropsConfig config = SmartDropsConfig.defaults();
        config.manualShearingDropsEnabled = true;
        config.defaultShearingMultiplier = 64;
        config.inheritDefaultShearingMultiplier = false;
        final List<EntityType<?>> specialTypes = List.of(
                EntityType.MOOSHROOM,
                EntityType.SNOW_GOLEM,
                EntityType.BOGGED);
        for (EntityType<?> type : specialTypes) {
            final ShearingRuleTrace trace = ShearingRuleResolver.trace(
                    config,
                    type,
                    ShearingSource.MANUAL_PLAYER);
            GameTestAssertions.assertTrue(helper, trace.classification() == ShearingClassification.SPECIAL,
                    "Known vanilla special was not classified as special: " + trace.entityId());
            GameTestAssertions.assertTrue(helper, trace.appliedMultiplier() == 1 && trace.fixedVanilla(),
                    "Known vanilla special escaped the fixed 1x gate: " + trace.entityId());
        }
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
    public void resolverSpecialTagWinsAConflictingStandardTag(final GameTestHelper helper) {
        final SmartDropsConfig config = SmartDropsConfig.defaults();
        config.shearingEntityMultipliers.put("example:conflict", 64);
        final ShearingRuleTrace trace = ShearingGameTestAccess.syntheticTrace(
                config,
                "example:conflict",
                true,
                true,
                ShearingSource.MANUAL_PLAYER);
        GameTestAssertions.assertTrue(helper, trace.tagConflict(), "Synthetic conflict did not retain both tag facts");
        GameTestAssertions.assertTrue(helper, trace.classification() == ShearingClassification.SPECIAL,
                "Special did not win the conflicting certification tags");
        GameTestAssertions.assertTrue(helper, trace.appliedMultiplier() == 1,
                "Conflicting special entity escaped fixed vanilla 1x");
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
    public void resolverUnknownOverrideCannotBypassCertification(final GameTestHelper helper) {
        final SmartDropsConfig config = SmartDropsConfig.defaults();
        config.shearingEntityMultipliers.put("example:uncertified", 64);
        final ShearingRuleTrace trace = ShearingGameTestAccess.syntheticTrace(
                config,
                "example:uncertified",
                false,
                false,
                ShearingSource.MANUAL_PLAYER);
        GameTestAssertions.assertTrue(helper, trace.classification() == ShearingClassification.UNKNOWN,
                "Uncertified fixture was not classified as unknown");
        GameTestAssertions.assertTrue(helper, trace.exactOverride() == 64 && trace.appliedMultiplier() == 1,
                "Unknown exact override bypassed the certification safety gate");
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
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
        GameTestAssertions.assertTrue(helper, trace.appliedMultiplier() == 3,
                "Certified exact override did not win the shearing default");
        GameTestAssertions.assertTrue(helper, trace.selectedRule() == ShearingRuleTrace.RuleSource.ENTITY_OVERRIDE,
                "Certified exact override reported the wrong rule source");
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
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
        GameTestAssertions.assertTrue(helper, manual.appliedMultiplier() == 1 && !manual.sourceEnabled(),
                "Disabled manual source did not resolve to vanilla 1x");
        GameTestAssertions.assertTrue(helper, automated.appliedMultiplier() == 5 && automated.sourceEnabled(),
                "Enabled automated source did not resolve its configured multiplier");
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
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
        GameTestAssertions.assertTrue(helper, inherited.appliedMultiplier() == 6,
                "Shearing did not inherit the configured global multiplier");
        GameTestAssertions.assertTrue(helper, inherited.selectedRule() == ShearingRuleTrace.RuleSource.GLOBAL_DEFAULT,
                "Inherited global rule reported the wrong source");

        config.enabled = false;
        final ShearingRuleTrace disabled = ShearingGameTestAccess.syntheticTrace(
                config,
                "example:certified",
                true,
                false,
                ShearingSource.MANUAL_PLAYER);
        GameTestAssertions.assertTrue(helper, disabled.appliedMultiplier() == 1,
                "Master gate did not restore vanilla 1x");
        GameTestAssertions.assertTrue(helper, disabled.selectedRule() == ShearingRuleTrace.RuleSource.MOD_DISABLED,
                "Master gate reported the wrong rule source");
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
    public void outputBudgetAllowsTheExactOneThousandTwentyFourItemBoundary(final GameTestHelper helper) {
        final ShearingOutputBudget.Result result = ShearingOutputBudget.plan(
                List.of(List.of(new ItemStack(Items.DIAMOND, 16))),
                64);
        GameTestAssertions.assertTrue(helper, result.fits(), "Exact 1024-item output was rejected");
        GameTestAssertions.assertTrue(helper, result.multipliedItems() == 1_024L,
                "Exact item-boundary plan counted the wrong output");
        GameTestAssertions.assertTrue(helper, total(result.outputBatches().getFirst()) == 1_024,
                "Exact item-boundary plan materialized the wrong count");
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
    public void outputBudgetRejectsItemOverflowAtomically(final GameTestHelper helper) {
        final ShearingOutputBudget.Result result = ShearingOutputBudget.plan(
                List.of(List.of(new ItemStack(Items.DIAMOND, 17))),
                64);
        GameTestAssertions.assertFalse(helper, result.fits(), "1088-item output escaped the item budget");
        GameTestAssertions.assertTrue(helper, result.limitExceeded() == ShearingOutputBudget.LimitExceeded.ITEMS,
                "Item overflow reported the wrong safety limit");
        GameTestAssertions.assertTrue(helper, result.outputBatches().isEmpty(),
                "Rejected item overflow exposed a partial output plan");
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
    public void outputBudgetRejectsSourceStackOverflowAtomically(final GameTestHelper helper) {
        final List<ItemStack> source = new ArrayList<>();
        for (int index = 0; index < 257; index++) {
            source.add(new ItemStack(Items.DIAMOND));
        }
        final ShearingOutputBudget.Result result = ShearingOutputBudget.plan(List.of(source), 1);
        GameTestAssertions.assertFalse(helper, result.fits(), "257 source stacks escaped the source-stack budget");
        GameTestAssertions.assertTrue(helper, result.limitExceeded() == ShearingOutputBudget.LimitExceeded.SOURCE_STACKS,
                "Source-stack overflow reported the wrong safety limit");
        GameTestAssertions.assertTrue(helper, result.outputBatches().isEmpty(),
                "Rejected source-stack overflow exposed a partial output plan");
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
    public void outputBudgetRejectsMaterializedStackOverflowAtomically(final GameTestHelper helper) {
        final ShearingOutputBudget.Result result = ShearingOutputBudget.plan(
                List.of(List.of(new ItemStack(Items.SHEARS, 129))),
                2);
        GameTestAssertions.assertFalse(helper, result.fits(), "258 unstackable outputs escaped the materialization budget");
        GameTestAssertions.assertTrue(helper,
                result.limitExceeded() == ShearingOutputBudget.LimitExceeded.MATERIALIZED_STACKS,
                "Materialized-stack overflow reported the wrong safety limit");
        GameTestAssertions.assertTrue(helper, result.outputBatches().isEmpty(),
                "Rejected materialization overflow exposed a partial output plan");
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
    public void outputBufferPreservesEachVanillaConsumerBatch(final GameTestHelper helper) {
        final ShearingGameTestAccess.BufferRun run = ShearingGameTestAccess.complete(
                3,
                List.of(
                        List.of(new ItemStack(Items.WHITE_WOOL, 2)),
                        List.of(new ItemStack(Items.BLUE_WOOL))));
        GameTestAssertions.assertTrue(helper, run.fallback() == ShearingOutputBudget.LimitExceeded.NONE,
                "Safe multi-batch output unexpectedly fell back");
        GameTestAssertions.assertTrue(helper, total(run.emittedBatches().get(0)) == 6,
                "First consumer batch did not receive its own multiplied output");
        GameTestAssertions.assertTrue(helper, total(run.emittedBatches().get(1)) == 3,
                "Second consumer batch did not receive its own multiplied output");
        GameTestAssertions.assertTrue(helper, run.emittedBatches().get(0).stream()
                        .allMatch(stack -> stack.is(Items.WHITE_WOOL))
                        && run.emittedBatches().get(1).stream()
                        .allMatch(stack -> stack.is(Items.BLUE_WOOL)),
                "Per-batch consumer identities were mixed");
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
    public void outputBufferFallsBackTheWholeActionAcrossBatches(final GameTestHelper helper) {
        final ShearingGameTestAccess.BufferRun run = ShearingGameTestAccess.complete(
                64,
                List.of(
                        List.of(new ItemStack(Items.WHITE_WOOL, 10)),
                        List.of(new ItemStack(Items.BLUE_WOOL, 10))));
        GameTestAssertions.assertTrue(helper, run.fallback() == ShearingOutputBudget.LimitExceeded.ITEMS,
                "Cumulative multi-batch overflow did not enter the item fallback");
        GameTestAssertions.assertTrue(helper, total(run.emittedBatches().get(0)) == 10
                        && total(run.emittedBatches().get(1)) == 10,
                "Cumulative overflow emitted a partial multiplier instead of vanilla originals");
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
    public void outputBufferAbortRestoresOriginalsAfterAnActionException(final GameTestHelper helper) {
        final ShearingGameTestAccess.BufferRun run = ShearingGameTestAccess.abort(
                8,
                List.of(
                        List.of(new ItemStack(Items.WHITE_WOOL, 2)),
                        List.of(new ItemStack(Items.BLUE_WOOL))));
        GameTestAssertions.assertTrue(helper, total(run.emittedBatches().get(0)) == 2
                        && total(run.emittedBatches().get(1)) == 1,
                "Exception abort did not restore the closest vanilla 1x output");
        GameTestAssertions.assertFalse(helper, run.rollbackWarning(),
                "Exception abort reported an unexpected rollback-emission failure");
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
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
                GameTestAssertions.assertTrue(helper, ShearingActionContext.activeTrace(outerTarget) != null,
                        "Outer shearing identity was not installed");
                try (ShearingActionContext.Scope inner = ShearingActionContext.beginManual(
                        innerTarget,
                        helper.getLevel(),
                        player)) {
                    GameTestAssertions.assertTrue(helper, ShearingActionContext.activeTrace(outerTarget) == null,
                            "Outer target captured output while an inner action was active");
                    GameTestAssertions.assertTrue(helper, ShearingActionContext.activeTrace(innerTarget) != null,
                            "Inner shearing identity was not installed");
                }
                GameTestAssertions.assertTrue(helper, ShearingActionContext.activeTrace(outerTarget) != null,
                        "Closing the inner action did not restore the outer identity");
            }
            GameTestAssertions.assertTrue(helper, ShearingActionContext.activeTrace(outerTarget) == null,
                    "Closing the outer action leaked thread-local state");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
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
                GameTestAssertions.assertTrue(helper,
                        ShearingActionContext.activeTrace(target) != null
                                && ShearingActionContext.activeTrace(target).source()
                                == ShearingSource.MANUAL_PLAYER,
                        "Eligible manual outer scope was not active");
                try (ShearingActionContext.Scope inner = ShearingActionContext.beginDispenser(
                        target,
                        helper.getLevel())) {
                    final ShearingRuleTrace innerTrace = ShearingActionContext.activeTrace(target);
                    GameTestAssertions.assertTrue(helper, innerTrace != null
                                    && innerTrace.source() == ShearingSource.VANILLA_DISPENSER
                                    && !innerTrace.sourceEnabled()
                                    && innerTrace.appliedMultiplier() == 1,
                            "Disabled same-target dispenser scope did not mask the manual source");
                    final BiConsumer<ServerLevel, ItemStack> output =
                            ShearingActionContext.wrapLootConsumer(
                                    target,
                                    helper.getLevel(),
                                    (level, stack) -> emitted[0] += stack.getCount());
                    output.accept(helper.getLevel(), new ItemStack(Items.WHITE_WOOL));
                    inner.complete();
                    GameTestAssertions.assertTrue(helper, emitted[0] == 1,
                            "Disabled inner source leaked its output into the eligible outer buffer");
                }
                GameTestAssertions.assertTrue(helper,
                        ShearingActionContext.activeTrace(target) != null
                                && ShearingActionContext.activeTrace(target).source()
                                == ShearingSource.MANUAL_PLAYER,
                        "Closing the disabled inner source did not restore the manual outer scope");
                outer.complete();
            }
            GameTestAssertions.assertTrue(helper, emitted[0] == 1,
                    "Closing the outer scope replayed output already owned by the disabled inner source");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
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
                GameTestAssertions.assertTrue(helper, wrapped == downstream,
                        "Mismatched entity identity captured another action's output");
            }
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
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
            GameTestAssertions.assertTrue(helper, executeCommand(helper, "smartdrops shearing status", source) == 1,
                    "Shearing status command failed");
            GameTestAssertions.assertTrue(helper, messages.text().contains("Smart Resource Multiplier shearing")
                            && messages.text().contains("outputBudget=1024 items/256 source or materialized stacks"),
                    "Shearing status omitted its state or safety budget");
            GameTestAssertions.assertTrue(helper, ConfigManager.revision() == revision,
                    "Read-only shearing status changed the authoritative revision");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
    public void shearingAdminCommandsMutateOnlyAuthoritativeShearingFields(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            prepareShearing(helper, config -> { });
            final CommandSourceStack source = helper.getLevel().getServer().createCommandSourceStack();
            GameTestAssertions.assertTrue(helper, executeCommand(helper, "smartdrops admin shearing manual off", source) == 1,
                    "Manual shearing admin command failed");
            GameTestAssertions.assertTrue(helper, executeCommand(helper, "smartdrops admin shearing automated on", source) == 1,
                    "Automated shearing admin command failed");
            GameTestAssertions.assertTrue(helper, executeCommand(helper, "smartdrops admin shearing multiplier 7", source) == 1,
                    "Default shearing multiplier admin command failed");
            GameTestAssertions.assertTrue(helper, executeCommand(
                            helper,
                            "smartdrops admin shearing entity minecraft:sheep 5",
                            source) == 1,
                    "Certified sheep override admin command failed");
            final SmartDropsConfig active = ConfigManager.get();
            GameTestAssertions.assertFalse(helper, active.manualShearingDropsEnabled,
                    "Manual shearing admin command did not update the authoritative config");
            GameTestAssertions.assertTrue(helper, active.automatedShearingDropsEnabled,
                    "Automated shearing admin command did not update the authoritative config");
            GameTestAssertions.assertFalse(helper, active.inheritDefaultShearingMultiplier,
                    "Numeric shearing default did not disable inheritance");
            GameTestAssertions.assertTrue(helper, active.defaultShearingMultiplier == 7
                            && active.shearingEntityMultipliers.get("minecraft:sheep") == 5,
                    "Shearing admin multipliers were not stored authoritatively");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
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
            GameTestAssertions.assertTrue(helper, executeCommand(
                            helper,
                            "smartdrops admin shearing entity minecraft:mooshroom 64",
                            source) == 0,
                    "Special mooshroom override was incorrectly accepted");
            GameTestAssertions.assertTrue(helper, messages.text().contains("fixed at vanilla 1x"),
                    "Special override rejection omitted its safety explanation");
            GameTestAssertions.assertTrue(helper, executeCommand(
                            helper,
                            "smartdrops admin shearing entity minecraft:mooshroom inherit",
                            source) == 1,
                    "Special override removal was incorrectly rejected");
            GameTestAssertions.assertFalse(helper, ConfigManager.get().shearingEntityMultipliers.containsKey(
                            "minecraft:mooshroom"),
                    "Special override removal did not clear the stored rule");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
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

            GameTestAssertions.assertTrue(helper, executeCommand(
                            helper,
                            "smartdrops inspect entity verbose",
                            player.createCommandSourceStack().withSource(messages)) == 1,
                    "Verbose sheep inspection failed");
            GameTestAssertions.assertTrue(helper, messages.text().contains("Entity shearing")
                            && messages.text().contains("Effective manual multiplier")
                            && messages.text().contains("Output safety budget"),
                    "Verbose sheep inspection omitted its shearing diagnostics");
            GameTestAssertions.assertTrue(helper, sheep.isAlive() && !sheep.isSheared() && sheep.getHealth() == health,
                    "Sheep inspection changed health or shear state");
            GameTestAssertions.assertTrue(helper, sheep.position().equals(position), "Sheep inspection moved its target");
            GameTestAssertions.assertTrue(helper, shears.getDamageValue() == 0, "Sheep inspection damaged the held shears");
            GameTestAssertions.assertTrue(helper, allItemDrops(helper, ENTITY_POS).size() == itemCount,
                    "Sheep inspection spawned output");
            GameTestAssertions.assertTrue(helper, ConfigManager.revision() == revision,
                    "Sheep inspection changed the authoritative config revision");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
    public void resetDefaultsEnableOnlyManualShearingAndClearOverrides(final GameTestHelper helper) {
        final SmartDropsConfig defaults = SmartDropsConfig.defaults();
        GameTestAssertions.assertTrue(helper, defaults.manualShearingDropsEnabled,
                "Fresh/reset defaults did not enable manual shearing");
        GameTestAssertions.assertFalse(helper, defaults.automatedShearingDropsEnabled,
                "Fresh/reset defaults unexpectedly enabled automated shearing");
        GameTestAssertions.assertTrue(helper, defaults.inheritDefaultShearingMultiplier,
                "Fresh/reset defaults did not inherit the global multiplier");
        GameTestAssertions.assertTrue(helper, defaults.shearingEntityMultipliers.isEmpty(),
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
        GameTestAssertions.assertTrue(helper, ConfigManager.update(config -> {
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
        final Sheep sheep = helper.spawnWithNoFreeWill(EntityType.SHEEP, position);
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
        GameTestAssertions.assertTrue(helper, !tool.isEmpty(), "One shearing action unexpectedly destroyed a fresh tool");
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
        player.interactOn(target, InteractionHand.MAIN_HAND);
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
        GameTestAssertions.assertTrue(helper, blockEntity != null, "Could not create the real dispenser block entity");
        final DispenseItemBehavior behavior = DispenserBlock.DISPENSER_REGISTRY.get(Items.SHEARS);
        GameTestAssertions.assertTrue(helper, behavior != null, "Vanilla shears dispenser behavior was not registered");
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
        GameTestAssertions.assertTrue(helper, total >= minimum && total <= maximum && total % divisor == 0,
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
        GameTestAssertions.assertTrue(helper, total >= minimum && total <= maximum,
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
        GameTestAssertions.assertTrue(helper, actual == expected,
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
                EntityType.ITEM,
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
