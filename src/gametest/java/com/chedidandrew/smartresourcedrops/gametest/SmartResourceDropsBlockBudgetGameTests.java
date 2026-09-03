package com.chedidandrew.smartresourcedrops.gametest;

import com.chedidandrew.smartresourcedrops.config.ConfigManager;
import com.chedidandrew.smartresourcedrops.config.SmartDropsConfig;
import com.chedidandrew.smartresourcedrops.core.SmartDropsStats;
import com.chedidandrew.smartresourcedrops.gametest.fixture.GameTestBlockLootFixtures;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Real server-pipeline checks for block output-budget fallback and context recovery. */
public final class SmartResourceDropsBlockBudgetGameTests {
    private static final int ORIGINAL_DIAMONDS = GameTestBlockLootFixtures.PATHOLOGICAL_STACKS
            * GameTestBlockLootFixtures.ITEMS_PER_STACK;

    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void pathologicalFinalLootFallsBackForEveryBlockSourceAndRecovers(
            final GameTestHelper helper
    ) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        SmartDropsStats.reset();
        GameTestBlockLootFixtures.reset();
        try {
            configure();
            final ServerPlayer player = GameTestPlayers.withGameMode(helper, GameType.SURVIVAL);
            final ItemStack tool = new ItemStack(Items.DIAMOND_SHOVEL);

            final BlockPos playerPos = helper.absolutePos(new BlockPos(1, 2, 1));
            helper.getLevel().setBlock(playerPos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
            GameTestBlockLootFixtures.arm();
            Block.dropResources(
                    Blocks.DIRT.defaultBlockState(),
                    helper.getLevel(),
                    playerPos,
                    null,
                    player,
                    tool);
            assertWorldDrops(helper, playerPos, 1, ORIGINAL_DIAMONDS, "player mining fallback");
            removeDrops(helper, playerPos);

            final BlockPos automationPos = helper.absolutePos(new BlockPos(5, 2, 1));
            helper.getLevel().setBlock(automationPos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
            GameTestBlockLootFixtures.arm();
            Block.dropResources(Blocks.DIRT.defaultBlockState(), helper.getLevel(), automationPos);
            assertWorldDrops(helper, automationPos, 1, ORIGINAL_DIAMONDS, "automation fallback");
            removeDrops(helper, automationPos);

            final BlockPos explosionPos = helper.absolutePos(new BlockPos(1, 2, 5));
            final BlockState explosionState = Blocks.DIRT.defaultBlockState();
            helper.getLevel().setBlock(explosionPos, explosionState, Block.UPDATE_ALL);
            GameTestBlockLootFixtures.arm();
            explosion(helper.getLevel(), explosionPos).finalizeExplosion(true);
            assertWorldDrops(helper, explosionPos, 1, ORIGINAL_DIAMONDS, "explosion fallback");
            removeDrops(helper, explosionPos);

            SmartDropsStats.Snapshot fallbackStats = SmartDropsStats.snapshot();
            helper.assertTrue(fallbackStats.blocksEvaluated() == 3L,
                    "Budget fallbacks were not recorded as evaluated block events");
            helper.assertTrue(fallbackStats.blocksMultiplied() == 0L,
                    "Budget fallbacks were incorrectly recorded as successful multiplication");
            helper.assertTrue(fallbackStats.bonusItems() == 0L,
                    "Budget fallbacks incorrectly recorded requested bonus output");
            helper.assertTrue(fallbackStats.blockBudgetFallbacks() == 3L,
                    "Expected three block budget fallback statistics");

            final BlockPos recoveryPos = helper.absolutePos(new BlockPos(5, 2, 5));
            helper.getLevel().setBlock(recoveryPos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
            Block.dropResources(
                    Blocks.DIRT.defaultBlockState(),
                    helper.getLevel(),
                    recoveryPos,
                    null,
                    player,
                    tool);
            assertWorldDrops(helper, recoveryPos, 64, 0, "post-fallback normal multiplication");

            SmartDropsStats.Snapshot recoveredStats = SmartDropsStats.snapshot();
            helper.assertTrue(recoveredStats.blocksMultiplied() == 1L,
                    "A normal event after fallback did not multiply");
            helper.assertTrue(recoveredStats.bonusItems() == 63L,
                    "The normal recovery event recorded incorrect bonus output");
            helper.assertTrue(recoveredStats.blockBudgetFallbacks() == 3L,
                    "The normal recovery event changed the fallback count");
        } finally {
            GameTestBlockLootFixtures.reset();
            restore(previous);
            SmartDropsStats.reset();
        }
        helper.succeed();
    }

    private static void configure() {
        if (!ConfigManager.update(config -> {
            config.enabled = true;
            config.globalMultiplier = 64;
            config.maximumMultiplier = 64;
            config.sourceMode = SmartDropsConfig.SourceMode.ALL;
            config.filterMode = SmartDropsConfig.FilterMode.BLACKLIST;
            config.playerMining = true;
            config.explosions = true;
            config.automatedMining = true;
            config.statisticsEnabled = true;
            config.dimensionMultipliers.clear();
            config.categoryMultipliers.clear();
            config.blockMultipliers.clear();
            config.blacklist.clear();
            config.whitelist.clear();
            config.tagBlacklist.clear();
            config.tagWhitelist.clear();
            config.playerMultipliers.clear();
        })) {
            throw new AssertionError("Could not prepare block budget GameTest configuration");
        }
    }

    private static void restore(final SmartDropsConfig previous) {
        if (!ConfigManager.update(config ->
                SmartResourceDropsShearingGameTests.copyConfiguration(config, previous))) {
            throw new AssertionError("Could not restore block budget GameTest configuration");
        }
    }

    private static Explosion explosion(final ServerLevel level, final BlockPos pos) {
        return new Explosion(
                level,
                null,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                4.0F,
                false,
                Explosion.BlockInteraction.DESTROY,
                List.of(pos));
    }

    private static void assertWorldDrops(
            final GameTestHelper helper,
            final BlockPos pos,
            final int expectedDirt,
            final int expectedDiamonds,
            final String scenario
    ) {
        assertListDrops(
                helper,
                dropsNear(helper, pos).stream().map(ItemEntity::getItem).toList(),
                expectedDirt,
                expectedDiamonds,
                scenario);
    }

    private static void assertListDrops(
            final GameTestHelper helper,
            final List<ItemStack> drops,
            final int expectedDirt,
            final int expectedDiamonds,
            final String scenario
    ) {
        final int actualDirt = total(drops, Items.DIRT);
        final int actualDiamonds = total(drops, Items.DIAMOND);
        helper.assertTrue(actualDirt == expectedDirt,
                scenario + " produced " + actualDirt + " dirt instead of " + expectedDirt);
        helper.assertTrue(actualDiamonds == expectedDiamonds,
                scenario + " produced " + actualDiamonds + " diamonds instead of " + expectedDiamonds);
    }

    private static int total(final List<ItemStack> drops, final Item item) {
        return drops.stream()
                .filter(stack -> stack.is(item))
                .mapToInt(ItemStack::getCount)
                .sum();
    }

    private static List<ItemEntity> dropsNear(final GameTestHelper helper, final BlockPos pos) {
        return helper.getLevel().getEntities(
                EntityType.ITEM,
                new AABB(pos).inflate(1.5D),
                ItemEntity::isAlive);
    }

    private static void removeDrops(final GameTestHelper helper, final BlockPos pos) {
        dropsNear(helper, pos).forEach(ItemEntity::kill);
    }
}
