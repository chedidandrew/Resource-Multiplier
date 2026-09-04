package com.chedidandrew.smartresourcedrops.gametest;

import com.chedidandrew.smartresourcedrops.config.ConfigManager;
import com.chedidandrew.smartresourcedrops.config.SmartDropsConfig;
import com.chedidandrew.smartresourcedrops.gametest.fixture.GameTestEntityFixtures;
import com.chedidandrew.smartresourcedrops.platform.PlatformPlayerSupport;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.UUID;

/** Loader-specific proof that a real NeoForge FakePlayer never gains player-kill authority. */
public final class NeoForgeAutomationAuthorityGameTests {
    @GameTest(structure = "smart_resource_drops_gametest:wide", maxTicks = 100)
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
            GameTestAssertions.assertTrue(helper,
                    PlatformPlayerSupport.isFakePlayer(automation),
                    "NeoForge FakePlayer was not recognized as automation");
            final BlockPos relative = new BlockPos(3, 2, 4);
            final var victim = helper.spawnWithNoFreeWill(
                    GameTestEntityFixtures.HOSTILE,
                    relative);
            GameTestAssertions.assertTrue(helper,
                    victim.hurtServer(
                            helper.getLevel(),
                            helper.getLevel().damageSources().playerAttack(automation),
                            Float.MAX_VALUE),
                    "Fixture refused lethal NeoForge FakePlayer damage");
            GameTestAssertions.assertTrue(helper, victim.isDeadOrDying(), "Fixture survived FakePlayer damage");

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
            GameTestAssertions.assertTrue(helper,
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
