package com.chedidandrew.smartresourcedrops.gametest;

import com.chedidandrew.smartresourcedrops.config.ConfigManager;
import com.chedidandrew.smartresourcedrops.config.SmartDropsConfig;
import com.chedidandrew.smartresourcedrops.gametest.fixture.GameTestEntityFixtures;
import com.chedidandrew.smartresourcedrops.platform.PlatformPlayerSupport;
import com.chedidandrew.smartresourcedrops.provenance.PlacementTracker;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Loader-specific proof that a real NeoForge FakePlayer never gains player-kill authority. */
@PrefixGameTestTemplate(false)
public final class NeoForgeAutomationAuthorityGameTests {
    @GameTest(templateNamespace = "smart_resource_drops_gametest", template = "wide")
    public void neoForgePlayerBreakUsesConfiguredBlockAndXpMultipliers(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            helper.assertTrue(ConfigManager.update(config -> {
                config.enabled = true;
                config.globalMultiplier = 4;
                config.maximumMultiplier = 64;
                config.sourceMode = SmartDropsConfig.SourceMode.NATURAL_ONLY;
                config.filterMode = SmartDropsConfig.FilterMode.BLACKLIST;
                config.smartPlacementProtection = true;
                config.protectBlockEntities = true;
                config.playerMining = true;
                config.multiplyExperience = true;
                config.experienceMultiplier = 4;
                config.allowPlayerOverrides = false;
                config.dimensionMultipliers.clear();
                config.categoryMultipliers.clear();
                config.blockMultipliers.clear();
                config.blacklist.clear();
                config.tagBlacklist.clear();
                config.playerMultipliers.clear();
            }), "Could not configure NeoForge player-break multiplier test");

            final BlockPos relative = new BlockPos(5, 2, 4);
            final BlockPos absolute = helper.absolutePos(relative);
            helper.getLevel().setBlock(absolute, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            PlacementTracker.remove(helper.getLevel(), absolute);

            final ServerPlayer player = GameTestPlayers.survival(helper);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
            helper.assertTrue(
                    player.gameMode.destroyBlock(absolute),
                    "NeoForge ServerPlayerGameMode refused to destroy the fixture block");
            helper.assertBlockNotPresent(Blocks.STONE, relative);

            final Vec3 center = Vec3.atCenterOf(absolute);
            final int drops = helper.getLevel().getEntities(
                            EntityType.ITEM,
                            new AABB(center, center).inflate(1.5),
                            ItemEntity::isAlive)
                    .stream()
                    .map(ItemEntity::getItem)
                    .filter(stack -> stack.is(Items.COBBLESTONE))
                    .mapToInt(ItemStack::getCount)
                    .sum();
            helper.assertTrue(
                    drops == 4,
                    "NeoForge player break produced " + drops
                            + " cobblestone instead of the configured 4x result");

            final BlockPos sculkRelative = new BlockPos(7, 2, 4);
            final BlockPos sculkAbsolute = helper.absolutePos(sculkRelative);
            helper.getLevel().setBlock(sculkAbsolute, Blocks.SCULK.defaultBlockState(), Block.UPDATE_ALL);
            PlacementTracker.remove(helper.getLevel(), sculkAbsolute);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_HOE));
            helper.assertTrue(
                    player.gameMode.destroyBlock(sculkAbsolute),
                    "NeoForge ServerPlayerGameMode refused to destroy the XP fixture block");
            helper.assertBlockNotPresent(Blocks.SCULK, sculkRelative);

            final Vec3 sculkCenter = Vec3.atCenterOf(sculkAbsolute);
            final int experience = helper.getLevel().getEntities(
                            EntityType.EXPERIENCE_ORB,
                            new AABB(sculkCenter, sculkCenter).inflate(1.5),
                            ExperienceOrb::isAlive)
                    .stream()
                    .mapToInt(ExperienceOrb::getValue)
                    .sum();
            helper.assertTrue(
                    experience == 4,
                    "NeoForge player break produced " + experience
                            + " block XP instead of the configured 4x result");
        } finally {
            if (!ConfigManager.update(config ->
                    SmartResourceDropsShearingGameTests.copyConfiguration(config, previous))) {
                throw new AssertionError("Could not restore NeoForge player-break test config");
            }
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "smart_resource_drops_gametest", template = "wide")
    public void neoForgeFakePlayerDeathRemainsVanillaOneX(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            if (!ConfigManager.update(config -> {
                config.entityDropsEnabled = true;
                config.entityKillRequirement =
                        SmartDropsConfig.EntityKillRequirement.PLAYER_KILLS_ONLY;
                config.entityMultipliers.clear();
                config.entityMultipliers.put(
                        EntityType.getKey(GameTestEntityFixtures.HOSTILE).toString(),
                        3);
            })) {
                throw new AssertionError("Could not configure NeoForge automation authority test");
            }

            final FakePlayer automation = FakePlayerFactory.get(
                    helper.getLevel(),
                    new GameProfile(
                            UUID.fromString("00000000-0000-0000-0000-00000000ae21"),
                            "smartdrops-neoforge-automation"));
            helper.assertTrue(
                    PlatformPlayerSupport.isFakePlayer(automation),
                    "NeoForge FakePlayer was not recognized as automation");
            final BlockPos relative = new BlockPos(3, 2, 4);
            final var victim = helper.spawnWithNoFreeWill(
                    GameTestEntityFixtures.HOSTILE,
                    relative);
            helper.assertTrue(
                    victim.hurt(
                            helper.getLevel().damageSources().playerAttack(automation),
                            Float.MAX_VALUE),
                    "Fixture refused lethal NeoForge FakePlayer damage");
            helper.assertTrue(victim.isDeadOrDying(), "Fixture survived FakePlayer damage");

            final Vec3 center = Vec3.atCenterOf(helper.absolutePos(relative));
            final int drops = helper.getLevel().getEntities(
                            EntityType.ITEM,
                            new AABB(center, center).inflate(1.5),
                            ItemEntity::isAlive)
                    .stream()
                    .map(ItemEntity::getItem)
                    .filter(stack -> stack.is(Items.ROTTEN_FLESH))
                    .mapToInt(ItemStack::getCount)
                    .sum();
            helper.assertTrue(
                    drops == 1,
                    "NeoForge FakePlayer produced " + drops
                            + " rotten flesh instead of the vanilla 1x result");
        } finally {
            if (!ConfigManager.update(config ->
                    SmartResourceDropsShearingGameTests.copyConfiguration(config, previous))) {
                throw new AssertionError("Could not restore NeoForge automation authority config");
            }
        }
        helper.succeed();
    }
}
