package com.chedidandrew.smartresourcedrops.gametest;

import com.chedidandrew.smartresourcedrops.config.ConfigManager;
import com.chedidandrew.smartresourcedrops.config.SmartDropsConfig;
import com.chedidandrew.smartresourcedrops.core.Category;
import com.chedidandrew.smartresourcedrops.core.DropSource;
import com.chedidandrew.smartresourcedrops.core.MultiplierResolver;
import com.chedidandrew.smartresourcedrops.core.RuleEngine;
import com.chedidandrew.smartresourcedrops.core.RuleResolutionTrace;
import com.chedidandrew.smartresourcedrops.core.SmartDropsStats;
import com.chedidandrew.smartresourcedrops.provenance.PlacementTracker;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.nio.file.Files;

public final class SmartResourceDropsGameTests {
    @GameTest(structure = "smart_resource_drops_gametest:wide")
    public void levelMixinPreservesTransformsAndClearsUnrelatedReplacements(final GameTestHelper helper) {
        assertPreserved(helper, new BlockPos(1, 2, 1), Blocks.DIRT, Blocks.FARMLAND);
        assertPreserved(helper, new BlockPos(2, 2, 1), Blocks.OAK_LOG, Blocks.STRIPPED_OAK_LOG);
        assertPreserved(
            helper,
            new BlockPos(3, 2, 1),
            Blocks.COPPER_BLOCK,
            Blocks.EXPOSED_COPPER);
        assertPreserved(helper, new BlockPos(4, 2, 1), Blocks.WHITE_CONCRETE_POWDER, Blocks.WHITE_CONCRETE);

        final BlockPos unrelated = helper.absolutePos(new BlockPos(5, 2, 1));
        helper.getLevel().setBlock(unrelated, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        PlacementTracker.mark(helper.getLevel(), unrelated);
        helper.getLevel().setBlock(unrelated, Blocks.DIAMOND_ORE.defaultBlockState(), Block.UPDATE_ALL);
        GameTestAssertions.assertFalse(helper, PlacementTracker.isMarked(helper.getLevel(), unrelated), "Unrelated replacement inherited provenance");
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
    public void blockItemPlacementMarksBothDoorBlocks(final GameTestHelper helper) {
        final BlockPos support = new BlockPos(1, 1, 1);
        final BlockPos lower = support.above();
        helper.setBlock(support, Blocks.STONE);
        final ServerPlayer player = GameTestPlayers.survival(helper);
        final ItemStack door = new ItemStack(Items.OAK_DOOR);
        // NeoForge's 1.21.1 placement hook reads the stack from the real hand.
        player.setItemInHand(InteractionHand.MAIN_HAND, door);
        helper.placeAt(player, door, support, Direction.UP);

        final BlockPos lowerAbsolute = helper.absolutePos(lower);
        helper.assertBlockPresent(Blocks.OAK_DOOR, lower);
        helper.assertBlockPresent(Blocks.OAK_DOOR, lower.above());
        GameTestAssertions.assertTrue(helper, PlacementTracker.isMarked(helper.getLevel(), lowerAbsolute), "Door lower half was not marked");
        GameTestAssertions.assertTrue(helper, PlacementTracker.isMarked(helper.getLevel(), lowerAbsolute.above()), "Door upper half was not marked");

        final BlockPos failedLower = new BlockPos(5, 4, 5);
        final ItemStack unsupportedDoor = new ItemStack(Items.OAK_DOOR);
        player.setItemInHand(InteractionHand.MAIN_HAND, unsupportedDoor);
        helper.placeAt(player, unsupportedDoor, failedLower.below(), Direction.UP);
        final BlockPos failedAbsolute = helper.absolutePos(failedLower);
        helper.assertBlockNotPresent(Blocks.OAK_DOOR, failedLower);
        GameTestAssertions.assertFalse(helper,
                PlacementTracker.isMarked(helper.getLevel(), failedAbsolute),
                "Failed placement left a provenance marker");
        GameTestAssertions.assertFalse(helper,
                PlacementTracker.isMarked(helper.getLevel(), failedAbsolute.above()),
                "Failed multi-block placement left a provenance marker");
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
    public void realLootPipelineProtectsPlacedBlocksAndAggregatesHighMultipliers(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            configureLootTest();
            final ServerPlayer player = GameTestPlayers.survival(helper);
            final ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);

            final BlockPos natural = helper.absolutePos(new BlockPos(1, 2, 3));
            helper.getLevel().setBlock(natural, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            Block.dropResources(
                    Blocks.STONE.defaultBlockState(), helper.getLevel(), natural, null, player, tool);
            assertDropped(helper, natural, 2, 1, "Natural stone at 2x");

            final BlockPos placed = helper.absolutePos(new BlockPos(4, 2, 3));
            helper.getLevel().setBlock(placed, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            PlacementTracker.mark(helper.getLevel(), placed);
            Block.dropResources(
                    Blocks.STONE.defaultBlockState(), helper.getLevel(), placed, null, player, tool);
            assertDropped(helper, placed, 1, 1, "Player-placed stone");

            removeDrops(helper, natural);
            ConfigManager.update(config -> config.globalMultiplier = 0);
            Block.dropResources(
                    Blocks.STONE.defaultBlockState(), helper.getLevel(), natural, null, player, tool);
            assertDropped(helper, natural, 0, 0, "Natural stone at 0x");

            ConfigManager.update(config -> config.globalMultiplier = 64);
            Block.dropResources(
                    Blocks.STONE.defaultBlockState(), helper.getLevel(), natural, null, player, tool);
            assertDropped(helper, natural, 64, 1, "Natural stone at 64x");
        } finally {
            restoreLootTestConfig(previous);
        }
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
    public void repeatedBlockInspectionIsReadOnlyAndMatchesGameplayResolution(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            GameTestAssertions.assertTrue(helper, ConfigManager.update(config -> {
                config.enabled = true;
                config.globalMultiplier = 2;
                config.maximumMultiplier = 64;
                config.sourceMode = SmartDropsConfig.SourceMode.ALL;
                config.filterMode = SmartDropsConfig.FilterMode.BLACKLIST;
                config.smartPlacementProtection = true;
                config.protectBlockEntities = true;
                config.playerMining = true;
                config.explosions = true;
                config.automatedMining = false;
                config.allowPlayerOverrides = false;
                config.statisticsEnabled = true;
                config.dimensionMultipliers.clear();
                config.categoryMultipliers.clear();
                config.categoryMultipliers.put(Category.STONE.key(), 3);
                config.blockMultipliers.clear();
                config.blacklist.clear();
                config.whitelist.clear();
                config.tagBlacklist.clear();
                config.tagWhitelist.clear();
                config.blockEntityAllowlist.clear();
                config.playerMultipliers.clear();
            }), "Could not prepare inspection GameTest configuration");

            final ServerPlayer player = GameTestPlayers.survival(helper);
            final BlockPos stonePos = helper.absolutePos(new BlockPos(1, 2, 6));
            final BlockPos chestPos = helper.absolutePos(new BlockPos(4, 2, 6));
            helper.getLevel().setBlock(stonePos, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            helper.getLevel().setBlock(chestPos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
            PlacementTracker.mark(helper.getLevel(), chestPos);

            final BlockState stoneState = helper.getLevel().getBlockState(stonePos);
            final BlockState chestState = helper.getLevel().getBlockState(chestPos);
            final BlockEntity chestEntity = helper.getLevel().getBlockEntity(chestPos);
            GameTestAssertions.assertTrue(helper, chestEntity instanceof Container, "Real chest did not create a container block entity");
            final Container chestInventory = (Container) chestEntity;
            chestInventory.setItem(0, new ItemStack(Items.DIAMOND));
            final ItemStack chestContents = chestInventory.getItem(0).copy();

            final long revisionBefore = ConfigManager.revision();
            final SmartDropsStats.Snapshot statisticsBefore = SmartDropsStats.snapshot();
            final boolean stoneMarkedBefore = PlacementTracker.isMarked(helper.getLevel(), stonePos);
            final boolean chestMarkedBefore = PlacementTracker.isMarked(helper.getLevel(), chestPos);

            final RuleResolutionTrace stoneTrace = MultiplierResolver.inspect(
                    helper.getLevel(), stonePos, stoneState, null, DropSource.PLAYER, player);
            final RuleResolutionTrace chestTrace = MultiplierResolver.inspect(
                    helper.getLevel(), chestPos, chestState, chestEntity, DropSource.PLAYER, player);
            for (int attempt = 0; attempt < 3; attempt++) {
                GameTestAssertions.assertTrue(helper, stoneTrace.equals(MultiplierResolver.inspect(
                                helper.getLevel(), stonePos, stoneState, null, DropSource.PLAYER, player)),
                        "Repeated stone inspection changed its trace");
                GameTestAssertions.assertTrue(helper, chestTrace.equals(MultiplierResolver.inspect(
                                helper.getLevel(), chestPos, chestState, chestEntity, DropSource.PLAYER, player)),
                        "Repeated chest inspection changed its trace");
            }

            GameTestAssertions.assertTrue(helper, stoneTrace.matchedCategories().contains(Category.STONE),
                    "Real minecraft:stone tag did not resolve the Stone category");
            GameTestAssertions.assertTrue(helper, stoneTrace.categoryRuleCategory() == Category.STONE
                            && stoneTrace.configuredMultiplier() == 3,
                    "Stone category override was not selected by the inspection trace");
            GameTestAssertions.assertTrue(helper, chestTrace.hasBlockEntity() && chestTrace.blockEntityProtected(),
                    "Real chest was not diagnosed as a protected block entity");
            GameTestAssertions.assertTrue(helper, chestTrace.playerPlaced(),
                    "Tracked chest provenance was not visible to inspection");

            assertInspectionUnchanged(
                    helper,
                    revisionBefore,
                    statisticsBefore,
                    stonePos,
                    stoneState,
                    stoneMarkedBefore,
                    chestPos,
                    chestState,
                    chestMarkedBefore,
                    chestEntity,
                    chestInventory,
                    chestContents);

            final RuleEngine.Decision stoneGameplay = MultiplierResolver.resolve(
                    helper.getLevel(), stonePos, stoneState, null, DropSource.PLAYER, player);
            final RuleEngine.Decision chestGameplay = MultiplierResolver.resolve(
                    helper.getLevel(), chestPos, chestState, chestEntity, DropSource.PLAYER, player);
            GameTestAssertions.assertTrue(helper, stoneTrace.decision().equals(stoneGameplay),
                    "Stone inspection decision diverged from normal gameplay resolution");
            GameTestAssertions.assertTrue(helper, chestTrace.decision().equals(chestGameplay),
                    "Chest inspection decision diverged from normal gameplay resolution");

            assertInspectionUnchanged(
                    helper,
                    revisionBefore,
                    statisticsBefore,
                    stonePos,
                    stoneState,
                    stoneMarkedBefore,
                    chestPos,
                    chestState,
                    chestMarkedBefore,
                    chestEntity,
                    chestInventory,
                    chestContents);
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
    public void inspectCommandHandlesLookTargetSkyAndConsole(final GameTestHelper helper) {
        final ServerPlayer player = GameTestPlayers.survival(helper);
        final BlockPos targetPos = helper.absolutePos(new BlockPos(2, 2, 5));
        helper.getLevel().setBlock(targetPos, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);

        player.setPos(
                targetPos.getX() + 0.5,
                targetPos.getY(),
                targetPos.getZ() - 3.0);
        final Vec3 eye = player.getEyePosition();
        final Vec3 target = Vec3.atCenterOf(targetPos);
        final double xDelta = target.x - eye.x;
        final double zDelta = target.z - eye.z;
        final double horizontal = Math.sqrt(xDelta * xDelta + zDelta * zDelta);
        final float yaw = (float) Math.toDegrees(Math.atan2(target.z - eye.z, target.x - eye.x)) - 90.0F;
        final float pitch = (float) -Math.toDegrees(Math.atan2(target.y - eye.y, horizontal));
        player.absSnapRotationTo(yaw, pitch);

        final HitResult targetHit = player.pick(player.blockInteractionRange(), 1.0F, false);
        GameTestAssertions.assertTrue(helper, targetHit.getType() == HitResult.Type.BLOCK,
                "The server-side inspection raycast did not acquire the looked-at block");
        GameTestAssertions.assertTrue(helper, ((BlockHitResult) targetHit).getBlockPos().equals(targetPos),
                "The server-side inspection raycast selected the wrong block");
        final CapturingCommandSource targetMessages = new CapturingCommandSource();
        GameTestAssertions.assertTrue(helper, executeCommand(
                        helper,
                        "smartdrops inspect",
                        player.createCommandSourceStack().withSource(targetMessages)) == 1,
                "The looked-at block inspection command did not succeed");
        GameTestAssertions.assertTrue(helper, targetMessages.text().contains("Smart Resource Multiplier Inspection")
                        && targetMessages.text().contains("minecraft:stone"),
                "The successful command did not emit the expected inspection components");

        player.setPos(targetPos.getX() + 0.5, targetPos.getY() + 10.0, targetPos.getZ() + 0.5);
        player.absSnapRotationTo(player.getYRot(), -90.0F);
        GameTestAssertions.assertTrue(helper,
                player.pick(player.blockInteractionRange(), 1.0F, false).getType() == HitResult.Type.MISS,
                "The no-target command check unexpectedly hit a block");
        final CapturingCommandSource noTargetMessages = new CapturingCommandSource();
        GameTestAssertions.assertTrue(helper, executeCommand(
                        helper,
                        "smartdrops inspect verbose",
                        player.createCommandSourceStack().withSource(noTargetMessages)) == 0,
                "Looking into the sky did not return the clear no-target failure result");
        GameTestAssertions.assertTrue(helper, noTargetMessages.text().contains("No block is currently targeted.")
                        && noTargetMessages.text().contains("within interaction range"),
                "The no-target command did not emit clear recovery guidance");

        final CapturingCommandSource consoleMessages = new CapturingCommandSource();
        GameTestAssertions.assertTrue(helper, executeCommand(
                        helper,
                        "smartdrops inspect",
                        helper.getLevel().getServer().createCommandSourceStack().withSource(consoleMessages)) == 0,
                "Console inspection did not return the player-required failure result");
        GameTestAssertions.assertTrue(helper, consoleMessages.text().contains("A player target is required")
                        && consoleMessages.text().contains("Run /smartdrops inspect as a player"),
                "Console inspection did not emit clear player-required guidance");
        GameTestAssertions.assertTrue(helper, helper.getLevel().getBlockState(targetPos).is(Blocks.STONE),
                "Inspection command altered the targeted block");
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
    public void validationCommandIsOperatorOnlyBoundedAndReadOnlyForConsoleAndPlayers(
            final GameTestHelper helper
    ) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        final String privatePlayerId = "00000000-0000-0000-0000-000000000456";
        final BlockPos tracked = helper.absolutePos(new BlockPos(6, 2, 6));
        try {
            helper.getLevel().setBlock(tracked, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            PlacementTracker.mark(helper.getLevel(), tracked);
            GameTestAssertions.assertTrue(helper, ConfigManager.update(config -> {
                config.blockMultipliers.clear();
                for (int index = 0; index < 20; index++) {
                    config.blockMultipliers.put("missing:block_" + index, 2);
                }
                config.blockEntityAllowlist.clear();
                config.blockEntityAllowlist.add("missing:block_entity");
                config.allowPlayerOverrides = true;
                config.playerMultipliers.clear();
                config.playerMultipliers.put(privatePlayerId, 3);
            }), "Could not prepare validation command configuration");

            final long revisionBefore = ConfigManager.revision();
            final String jsonBefore = readConfigJson();
            final SmartDropsStats.Snapshot statisticsBefore = SmartDropsStats.snapshot();
            final BlockState stateBefore = helper.getLevel().getBlockState(tracked);
            final boolean provenanceBefore = PlacementTracker.isMarked(helper.getLevel(), tracked);

            final CapturingCommandSource consoleMessages = new CapturingCommandSource();
            GameTestAssertions.assertTrue(helper, executeCommand(
                            helper,
                            "smartdrops validate",
                            helper.getLevel().getServer().createCommandSourceStack().withSource(consoleMessages)) == 1,
                    "Console validation command did not succeed");
            GameTestAssertions.assertTrue(helper, consoleMessages.text().contains("Smart Resource Multiplier Validation")
                            && consoleMessages.text().contains("additional issue(s) omitted")
                            && consoleMessages.messageCount() <= 24,
                    "Compact validation output was missing or unbounded");
            GameTestAssertions.assertFalse(helper, consoleMessages.text().contains(privatePlayerId),
                    "Validation output exposed a stored player UUID");

            final ServerPlayer operator = GameTestPlayers.withGameMode(helper, GameType.CREATIVE);
            final CapturingCommandSource verboseMessages = new CapturingCommandSource();
            GameTestAssertions.assertTrue(helper, executeCommand(
                            helper,
                            "smartdrops validate verbose",
                            operator.createCommandSourceStack()
                                    .withPermission(4)
                                    .withSource(verboseMessages)) == 1,
                    "Operator verbose validation command did not succeed");
            GameTestAssertions.assertTrue(helper, verboseMessages.text().contains("missing:block_19")
                            && verboseMessages.text().contains("BLOCK_ENTITY_ALLOWLIST_ENTRY_UNRESOLVED")
                            && !verboseMessages.text().contains("BLOCK_ENTITY_ALLOWLIST_ENTRY_NOT_BLOCK_ENTITY")
                            && verboseMessages.text().contains("No configuration or world data was changed."),
                    "Verbose validation omitted expected bounded details or its read-only statement");

            final ServerPlayer normalPlayer = GameTestPlayers.withGameMode(helper, GameType.SURVIVAL);
            GameTestAssertions.assertTrue(helper, commandIsRejected(
                            helper,
                            "smartdrops validate",
                            normalPlayer.createCommandSourceStack().withPermission(0)),
                    "Non-operator validation was not rejected by the server command tree");

            GameTestAssertions.assertTrue(helper, ConfigManager.revision() == revisionBefore,
                    "Validation changed the authoritative config revision");
            GameTestAssertions.assertTrue(helper, readConfigJson().equals(jsonBefore),
                    "Validation rewrote the configuration file");
            GameTestAssertions.assertTrue(helper, ConfigManager.get().blockMultipliers.containsKey("missing:block_19"),
                    "Validation removed an unresolved configured block ID");
            GameTestAssertions.assertTrue(helper, ConfigManager.get().blockEntityAllowlist.contains("missing:block_entity"),
                    "Validation removed an unresolved block-entity allowlist ID");
            GameTestAssertions.assertTrue(helper, SmartDropsStats.snapshot().equals(statisticsBefore),
                    "Validation changed runtime statistics");
            GameTestAssertions.assertTrue(helper, helper.getLevel().getBlockState(tracked).equals(stateBefore),
                    "Validation changed world block state");
            GameTestAssertions.assertTrue(helper, PlacementTracker.isMarked(helper.getLevel(), tracked) == provenanceBefore,
                    "Validation changed placement provenance");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide")
    public void configurationResetPreservesPlacedBlockProvenance(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        final BlockPos placed = helper.absolutePos(new BlockPos(2, 2, 5));
        try {
            helper.getLevel().setBlock(placed, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            PlacementTracker.mark(helper.getLevel(), placed);
            GameTestAssertions.assertTrue(helper,
                    PlacementTracker.isMarked(helper.getLevel(), placed),
                    "The pre-reset player-placed marker was not recorded");

            GameTestAssertions.assertTrue(helper,
                    ConfigManager.update(config -> {
                        config.globalMultiplier = 7;
                        config.categoryMultipliers.put("ores", 5);
                        config.blockMultipliers.put("minecraft:diamond_ore", 8);
                        config.manualShearingDropsEnabled = false;
                        config.automatedShearingDropsEnabled = true;
                        config.inheritDefaultShearingMultiplier = false;
                        config.defaultShearingMultiplier = 9;
                        config.shearingEntityMultipliers.put("minecraft:sheep", 11);
                    }),
                    "Could not prepare the non-default configuration for the reset test");
            GameTestAssertions.assertTrue(helper, ConfigManager.reset(), "The authoritative configuration reset failed");

            GameTestAssertions.assertTrue(helper,
                    PlacementTracker.isMarked(helper.getLevel(), placed),
                    "Reset All Settings erased player-placed block provenance");
            GameTestAssertions.assertTrue(helper,
                    ConfigManager.get().globalMultiplier == SmartDropsConfig.defaults().globalMultiplier,
                    "The real reset path did not restore the default global multiplier");
            GameTestAssertions.assertTrue(helper,
                    ConfigManager.get().categoryMultipliers.isEmpty()
                            && ConfigManager.get().blockMultipliers.isEmpty(),
                    "The real reset path retained multiplier overrides");
            GameTestAssertions.assertTrue(helper,
                    ConfigManager.get().manualShearingDropsEnabled
                            && !ConfigManager.get().automatedShearingDropsEnabled
                            && ConfigManager.get().inheritDefaultShearingMultiplier
                            && ConfigManager.get().shearingEntityMultipliers.isEmpty(),
                    "The real reset path did not restore fresh-install shearing defaults");
        } finally {
            restoreConfiguration(previous);
        }
        helper.succeed();
    }

    private static void configureLootTest() {
        ConfigManager.update(config -> {
            config.enabled = true;
            config.globalMultiplier = 2;
            config.maximumMultiplier = 64;
            config.sourceMode = SmartDropsConfig.SourceMode.NATURAL_ONLY;
            config.filterMode = SmartDropsConfig.FilterMode.WHITELIST;
            config.smartPlacementProtection = true;
            config.playerMining = true;
            config.allowPlayerOverrides = false;
            config.dimensionMultipliers.clear();
            config.categoryMultipliers.clear();
            config.blockMultipliers.clear();
            config.whitelist.clear();
            config.whitelist.add("minecraft:stone");
            config.tagWhitelist.clear();
        });
    }

    private static int executeCommand(
            final GameTestHelper helper,
            final String command,
            final CommandSourceStack source
    ) {
        try {
            return helper.getLevel().getServer().getCommands().getDispatcher().execute(command, source);
        } catch (CommandSyntaxException exception) {
            throw new AssertionError("Inspection command failed to execute: " + command, exception);
        }
    }

    private static boolean commandIsRejected(
            final GameTestHelper helper,
            final String command,
            final CommandSourceStack source
    ) {
        try {
            helper.getLevel().getServer().getCommands().getDispatcher().execute(command, source);
            return false;
        } catch (CommandSyntaxException expected) {
            return true;
        }
    }

    private static String readConfigJson() {
        try {
            return Files.readString(ConfigManager.path());
        } catch (IOException exception) {
            throw new AssertionError("Could not read the validation test configuration", exception);
        }
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

        private int messageCount() {
            return messages.size();
        }
    }

    private static void restoreLootTestConfig(final SmartDropsConfig previous) {
        restoreConfiguration(previous);
    }

    private static void restoreConfiguration(final SmartDropsConfig previous) {
        ConfigManager.update(config ->
                SmartResourceDropsShearingGameTests.copyConfiguration(config, previous));
    }

    private static void assertDropped(
            final GameTestHelper helper,
            final BlockPos absolutePos,
            final int expectedItems,
            final int expectedEntities,
            final String scenario
    ) {
        final List<ItemEntity> drops = dropsNear(helper, absolutePos);
        final int total = drops.stream().mapToInt(entity -> entity.getItem().getCount()).sum();
        GameTestAssertions.assertTrue(helper, total == expectedItems,
                scenario + " produced " + total + " items instead of " + expectedItems);
        GameTestAssertions.assertTrue(helper, drops.size() == expectedEntities,
                scenario + " spawned " + drops.size() + " item entities instead of " + expectedEntities);
    }

    private static void assertInspectionUnchanged(
            final GameTestHelper helper,
            final long expectedRevision,
            final SmartDropsStats.Snapshot expectedStatistics,
            final BlockPos stonePos,
            final BlockState stoneState,
            final boolean stoneMarked,
            final BlockPos chestPos,
            final BlockState chestState,
            final boolean chestMarked,
            final BlockEntity chestEntity,
            final Container chestInventory,
            final ItemStack chestContents
    ) {
        GameTestAssertions.assertTrue(helper, ConfigManager.revision() == expectedRevision,
                "Inspection changed the authoritative config revision");
        GameTestAssertions.assertTrue(helper, SmartDropsStats.snapshot().equals(expectedStatistics),
                "Inspection changed block-drop statistics");
        GameTestAssertions.assertTrue(helper, helper.getLevel().getBlockState(stonePos).equals(stoneState),
                "Inspection changed the stone block state");
        GameTestAssertions.assertTrue(helper, helper.getLevel().getBlockState(chestPos).equals(chestState),
                "Inspection changed the chest block state");
        GameTestAssertions.assertTrue(helper, helper.getLevel().getBlockEntity(chestPos) == chestEntity,
                "Inspection replaced or removed the chest block entity");
        GameTestAssertions.assertTrue(helper, PlacementTracker.isMarked(helper.getLevel(), stonePos) == stoneMarked,
                "Inspection changed natural stone provenance");
        GameTestAssertions.assertTrue(helper, PlacementTracker.isMarked(helper.getLevel(), chestPos) == chestMarked,
                "Inspection changed tracked chest provenance");
        GameTestAssertions.assertTrue(helper, ItemStack.matches(chestContents, chestInventory.getItem(0)),
                "Inspection read or modified protected chest inventory data");
    }

    private static void removeDrops(final GameTestHelper helper, final BlockPos absolutePos) {
        dropsNear(helper, absolutePos).forEach(entity -> entity.kill(helper.getLevel()));
    }

    private static List<ItemEntity> dropsNear(final GameTestHelper helper, final BlockPos absolutePos) {
        return helper.getLevel().getEntities(
                EntityType.ITEM,
                new AABB(absolutePos).inflate(1.0),
                ItemEntity::isAlive);
    }

    private static void assertPreserved(
            final GameTestHelper helper,
            final BlockPos relativePos,
            final Block oldBlock,
            final Block newBlock
    ) {
        final BlockPos absolutePos = helper.absolutePos(relativePos);
        helper.getLevel().setBlock(absolutePos, oldBlock.defaultBlockState(), Block.UPDATE_ALL);
        PlacementTracker.mark(helper.getLevel(), absolutePos);
        helper.getLevel().setBlock(absolutePos, newBlock.defaultBlockState(), Block.UPDATE_ALL);
        GameTestAssertions.assertTrue(helper,
            PlacementTracker.isMarked(helper.getLevel(), absolutePos),
            oldBlock + " -> " + newBlock + " lost provenance");
    }
}
