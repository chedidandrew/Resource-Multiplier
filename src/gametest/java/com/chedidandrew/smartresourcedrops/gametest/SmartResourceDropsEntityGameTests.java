package com.chedidandrew.smartresourcedrops.gametest;

import com.chedidandrew.smartresourcedrops.config.ConfigManager;
import com.chedidandrew.smartresourcedrops.config.SmartDropsConfig;
import com.chedidandrew.smartresourcedrops.core.entity.EntityCategory;
import com.chedidandrew.smartresourcedrops.core.entity.EntityClassification;
import com.chedidandrew.smartresourcedrops.core.entity.EntityDeathContext;
import com.chedidandrew.smartresourcedrops.core.entity.EntityMultiplierResolver;
import com.chedidandrew.smartresourcedrops.core.entity.EntityRuleTrace;
import com.chedidandrew.smartresourcedrops.gametest.fixture.GameTestEntityFixtures;
import com.chedidandrew.smartresourcedrops.gametest.fixture.GameTestEntityFixtures.FixtureInventoryMonster;
import com.chedidandrew.smartresourcedrops.gametest.fixture.GameTestEntityFixtures.FixtureLootException;
import com.chedidandrew.smartresourcedrops.gametest.fixture.GameTestEntityFixtures.FixturePickupMonster;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Black-box coverage for the server-authoritative entity death-loot and XP hooks. */
public final class SmartResourceDropsEntityGameTests {
    @GameTest(padding = 32)
    public void entityFeatureGateAndBoundaryMultipliersUseFinalStandardLoot(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            final ServerPlayer player = GameTestPlayers.survival(helper);

            configureEntityTest(config -> {
                config.entityDropsEnabled = false;
                exactMultiplier(config, GameTestEntityFixtures.HOSTILE, 64);
            });
            killByPlayer(helper, spawn(helper, GameTestEntityFixtures.HOSTILE, 1), player);
            assertItemTotal(helper, 1, Items.ROTTEN_FLESH, 1, "disabled entity feature");

            configureEntityTest(config -> exactMultiplier(config, GameTestEntityFixtures.HOSTILE, 0));
            killByPlayer(helper, spawn(helper, GameTestEntityFixtures.HOSTILE, 4), player);
            assertItemTotal(helper, 4, Items.ROTTEN_FLESH, 0, "0x standard death loot");

            configureEntityTest(config -> exactMultiplier(config, GameTestEntityFixtures.HOSTILE, 1));
            killByPlayer(helper, spawn(helper, GameTestEntityFixtures.HOSTILE, 7), player);
            assertItemTotal(helper, 7, Items.ROTTEN_FLESH, 1, "1x standard death loot");

            configureEntityTest(config -> exactMultiplier(config, GameTestEntityFixtures.HOSTILE, 64));
            killByPlayer(helper, spawn(helper, GameTestEntityFixtures.HOSTILE, 10), player);
            assertItemTotal(helper, 10, Items.ROTTEN_FLESH, 64, "64x standard death loot");
            assertLegalStacks(helper, 10, Items.ROTTEN_FLESH);
        } finally {
            restoreEntityConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest(padding = 32)
    public void finalFabricLootModifierIsMultipliedOnceWithComponentsIntact(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        GameTestEntityFixtures.resetTransientState();
        try {
            final ServerPlayer player = GameTestPlayers.survival(helper);

            configureEntityTest(config -> exactMultiplier(config, GameTestEntityFixtures.COMPONENT_RICH, 1));
            killByPlayer(helper, spawn(helper, GameTestEntityFixtures.COMPONENT_RICH, 2), player);
            assertComponentRichDrop(helper, 2, 1, "component-rich 1x identity");
            helper.assertTrue(
                    GameTestEntityFixtures.COMPONENT_MODIFIER_INVOCATIONS.get() == 1,
                    "Fabric final-drop modifier ran more than once at 1x");

            GameTestEntityFixtures.resetTransientState();
            configureEntityTest(config -> exactMultiplier(config, GameTestEntityFixtures.COMPONENT_RICH, 3));
            killByPlayer(helper, spawn(helper, GameTestEntityFixtures.COMPONENT_RICH, 6), player);
            assertComponentRichDrop(helper, 6, 3, "component-rich Fabric-modified loot");
            helper.assertTrue(
                    GameTestEntityFixtures.COMPONENT_MODIFIER_INVOCATIONS.get() == 1,
                    "Fabric final-drop modifier ran more than once");
        } finally {
            GameTestEntityFixtures.resetTransientState();
            restoreEntityConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest(padding = 32)
    public void equipmentCarriedInventoryAndDirectOutputsAreNeverMultiplied(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            final ServerPlayer player = GameTestPlayers.survival(helper);

            configureEntityTest(config -> exactMultiplier(config, GameTestEntityFixtures.EQUIPMENT, 3));
            final Mob equipped = spawn(helper, GameTestEntityFixtures.EQUIPMENT, 1);
            equipped.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            equipped.setGuaranteedDrop(EquipmentSlot.HEAD);
            killByPlayer(helper, equipped, player);
            assertItemTotal(helper, 1, Items.CLAY_BALL, 3, "equipment fixture standard loot");
            assertItemTotal(helper, 1, Items.IRON_HELMET, 1, "equipped helmet");

            configureEntityTest(config -> exactMultiplier(config, GameTestEntityFixtures.CARRYING, 3));
            final FixturePickupMonster carrying = spawn(helper, GameTestEntityFixtures.CARRYING, 5);
            final ItemEntity pickup = helper.spawnItem(Items.DIAMOND_SWORD, new BlockPos(5, 2, 4));
            carrying.fixturePickUp(helper.getLevel(), pickup);
            helper.assertTrue(pickup.isRemoved() && carrying.getMainHandItem().is(Items.DIAMOND_SWORD),
                    "Fixture did not exercise Minecraft's real mob pickup path");
            killByPlayer(helper, carrying, player);
            assertItemTotal(helper, 5, Items.CLAY_BALL, 3, "carrying fixture standard loot");
            assertItemTotal(helper, 5, Items.DIAMOND_SWORD, 1, "picked-up or carried item");

            configureEntityTest(config -> exactMultiplier(config, GameTestEntityFixtures.INVENTORY, 3));
            final FixtureInventoryMonster inventory = spawn(helper, GameTestEntityFixtures.INVENTORY, 9);
            inventory.fixtureInventory().setItem(0, new ItemStack(Items.DIAMOND, 2));
            killByPlayer(helper, inventory, player);
            assertItemTotal(helper, 9, Items.CLAY_BALL, 3, "inventory fixture standard loot");
            assertItemTotal(helper, 9, Items.DIAMOND, 2, "entity inventory contents");

            configureEntityTest(config -> exactMultiplier(config, GameTestEntityFixtures.DIRECT_OUTPUT, 3));
            killByPlayer(helper, spawn(helper, GameTestEntityFixtures.DIRECT_OUTPUT, 13), player);
            assertItemTotal(helper, 13, Items.CLAY_BALL, 3, "direct-output fixture standard loot");
            assertItemTotal(helper, 13, Items.EMERALD, 1, "direct custom ItemEntity output");
        } finally {
            restoreEntityConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest(padding = 32)
    public void bossGateProtectsSaddlesTotemsAndExperienceSeparately(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            clearExperienceOrbs(helper);
            final ServerPlayer player = GameTestPlayers.survival(helper);

            configureEntityTest(config -> {
                exactMultiplier(config, GameTestEntityFixtures.BOSS, 3);
                config.bossDropsEnabled = false;
                config.multiplyMobExperience = true;
                config.mobExperienceMultiplier = 3;
                config.multiplyBossExperience = false;
            });
            killByPlayer(helper, spawn(helper, GameTestEntityFixtures.BOSS, 2), player);
            assertBossOutputs(helper, 2, 1, "boss drops disabled");
            assertExperienceTotalAndClear(helper, 2, 7, "boss XP disabled");

            configureEntityTest(config -> {
                exactMultiplier(config, GameTestEntityFixtures.BOSS, 3);
                config.bossDropsEnabled = true;
                config.multiplyMobExperience = true;
                config.mobExperienceMultiplier = 3;
                config.multiplyBossExperience = true;
            });
            killByPlayer(helper, spawn(helper, GameTestEntityFixtures.BOSS, 10), player);
            assertBossOutputs(helper, 10, 3, "boss drops enabled");
            assertExperienceTotalAndClear(helper, 10, 21, "boss XP enabled");
        } finally {
            clearExperienceOrbs(helper);
            restoreEntityConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest(padding = 32)
    public void vanillaPlayerProjectileEnvironmentalAndTamedAttributionAreRespected(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            final ServerPlayer player = GameTestPlayers.survival(helper);
            configureEntityTest(config -> {
                config.entityKillRequirement = SmartDropsConfig.EntityKillRequirement.PLAYER_KILLS_ONLY;
                exactMultiplier(config, GameTestEntityFixtures.HOSTILE, 3);
            });

            killByPlayer(helper, spawn(helper, GameTestEntityFixtures.HOSTILE, 1), player);
            assertItemTotal(helper, 1, Items.ROTTEN_FLESH, 3, "direct player kill");

            final Mob projectileVictim = spawn(helper, GameTestEntityFixtures.HOSTILE, 4);
            final Arrow arrow = helper.spawn(EntityTypes.ARROW, new BlockPos(4, 3, 4));
            final DamageSource arrowSource = helper.getLevel().damageSources().arrow(arrow, player);
            kill(helper, projectileVictim, arrowSource);
            assertItemTotal(helper, 4, Items.ROTTEN_FLESH, 3, "player projectile kill");

            final Mob creditedVictim = spawn(helper, GameTestEntityFixtures.HOSTILE, 7);
            helper.assertTrue(
                    creditedVictim.hurtServer(
                            helper.getLevel(),
                            helper.getLevel().damageSources().playerAttack(player),
                            1.0F),
                    "Could not establish vanilla last-player credit");
            kill(helper, creditedVictim, helper.getLevel().damageSources().genericKill());
            assertItemTotal(helper, 7, Items.ROTTEN_FLESH, 3, "environmental death after player credit");

            kill(helper, spawn(helper, GameTestEntityFixtures.HOSTILE, 10),
                    helper.getLevel().damageSources().genericKill());
            assertItemTotal(helper, 10, Items.ROTTEN_FLESH, 1, "environmental death without player credit");

            final Mob playerCreditedAfterUntamedWolf = spawn(
                    helper,
                    GameTestEntityFixtures.HOSTILE,
                    25);
            helper.assertTrue(
                    playerCreditedAfterUntamedWolf.hurtServer(
                            helper.getLevel(),
                            helper.getLevel().damageSources().playerAttack(player),
                            1.0F),
                    "Could not establish player credit before untamed-wolf damage");
            playerCreditedAfterUntamedWolf.invulnerableTime = 0;
            final Wolf untamedWolf = helper.spawnWithNoFreeWill(
                    EntityTypes.WOLF,
                    new BlockPos(25, 2, 2));
            helper.assertTrue(
                    playerCreditedAfterUntamedWolf.hurtServer(
                            helper.getLevel(),
                            helper.getLevel().damageSources().mobAttack(untamedWolf),
                            1.0F),
                    "Untamed-wolf fixture damage was rejected");
            kill(
                    helper,
                    playerCreditedAfterUntamedWolf,
                    helper.getLevel().damageSources().genericKill());
            assertItemTotal(
                    helper,
                    25,
                    Items.ROTTEN_FLESH,
                    3,
                    "vanilla player credit retained after untamed-wolf damage");

            final Wolf wolf = helper.spawnWithNoFreeWill(EntityTypes.WOLF, new BlockPos(13, 2, 2));
            wolf.tame(player);
            final Mob tamedVictimPlayerOnly = spawn(helper, GameTestEntityFixtures.HOSTILE, 13);
            kill(helper, tamedVictimPlayerOnly, helper.getLevel().damageSources().mobAttack(wolf));
            assertItemTotal(helper, 13, Items.ROTTEN_FLESH, 1, "tamed kill in player-only mode");

            configureEntityTest(config -> {
                config.entityKillRequirement = SmartDropsConfig.EntityKillRequirement.PLAYER_OR_TAMED_ENTITY;
                exactMultiplier(config, GameTestEntityFixtures.HOSTILE, 3);
            });
            final Mob tamedVictim = spawn(helper, GameTestEntityFixtures.HOSTILE, 16);
            kill(helper, tamedVictim, helper.getLevel().damageSources().mobAttack(wolf));
            assertItemTotal(helper, 16, Items.ROTTEN_FLESH, 3, "tamed kill in owner-enabled mode");

            final Wolf offlineOwner = helper.spawnWithNoFreeWill(EntityTypes.WOLF, new BlockPos(19, 2, 2));
            offlineOwner.setTame(true, false);
            offlineOwner.setOwnerReference(EntityReference.of(UUID.fromString(
                    "00000000-0000-0000-0000-000000000123")));
            final Mob offlineOwnerVictim = spawn(helper, GameTestEntityFixtures.HOSTILE, 19);
            kill(helper, offlineOwnerVictim, helper.getLevel().damageSources().mobAttack(offlineOwner));
            assertItemTotal(helper, 19, Items.ROTTEN_FLESH, 1, "offline tamed owner");

            configureEntityTest(config -> {
                config.entityKillRequirement = SmartDropsConfig.EntityKillRequirement.ALL_STANDARD_DEATH_LOOT;
                exactMultiplier(config, GameTestEntityFixtures.HOSTILE, 3);
            });
            kill(helper, spawn(helper, GameTestEntityFixtures.HOSTILE, 22),
                    helper.getLevel().damageSources().genericKill());
            assertItemTotal(helper, 22, Items.ROTTEN_FLESH, 3, "all-deaths mode");
        } finally {
            restoreEntityConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest(padding = 32)
    public void mobLootGameRuleAndExperienceContextsRemainScoped(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        final boolean previousMobDrops = helper.getLevel().getGameRules().get(GameRules.MOB_DROPS);
        try {
            clearExperienceOrbs(helper);
            final ServerPlayer player = GameTestPlayers.survival(helper);
            configureEntityTest(config -> {
                exactMultiplier(config, GameTestEntityFixtures.HOSTILE, 3);
                config.multiplyMobExperience = false;
                config.mobExperienceMultiplier = 3;
            });
            killByPlayer(helper, spawn(helper, GameTestEntityFixtures.HOSTILE, 2), player);
            assertItemTotal(helper, 2, Items.ROTTEN_FLESH, 3,
                    "drops enabled while ordinary mob XP disabled");
            assertExperienceTotalAndClear(helper, 2, 7, "ordinary mob XP disabled");

            configureEntityTest(config -> {
                exactMultiplier(config, GameTestEntityFixtures.HOSTILE, 3);
                config.multiplyMobExperience = true;
                config.mobExperienceMultiplier = 3;
            });
            killByPlayer(helper, spawn(helper, GameTestEntityFixtures.HOSTILE, 8), player);
            assertItemTotal(helper, 8, Items.ROTTEN_FLESH, 3,
                    "drops enabled while ordinary mob XP enabled");
            assertExperienceTotalAndClear(helper, 8, 21, "ordinary mob XP enabled");

            configureEntityTest(config -> {
                exactMultiplier(config, GameTestEntityFixtures.HOSTILE, 3);
                config.entityDropsEnabled = false;
                config.multiplyMobExperience = true;
                config.mobExperienceMultiplier = 3;
            });
            killByPlayer(helper, spawn(helper, GameTestEntityFixtures.HOSTILE, 11), player);
            assertItemTotal(helper, 11, Items.ROTTEN_FLESH, 1,
                    "entity drops disabled independently of XP");
            assertExperienceTotalAndClear(helper, 11, 21,
                    "XP enabled independently of entity drops");

            configureEntityTest(config -> {
                exactMultiplier(config, GameTestEntityFixtures.HOSTILE, 0);
                config.multiplyMobExperience = false;
                config.mobExperienceMultiplier = 3;
            });
            killByPlayer(helper, spawn(helper, GameTestEntityFixtures.HOSTILE, 14), player);
            assertItemTotal(helper, 14, Items.ROTTEN_FLESH, 0,
                    "0x item rule with ordinary XP disabled");
            assertExperienceTotalAndClear(helper, 14, 7,
                    "ordinary XP remains vanilla at a 0x item rule");

            configureEntityTest(config -> {
                exactMultiplier(config, GameTestEntityFixtures.HOSTILE, 0);
                config.multiplyMobExperience = true;
                config.mobExperienceMultiplier = 3;
            });
            killByPlayer(helper, spawn(helper, GameTestEntityFixtures.HOSTILE, 17), player);
            assertItemTotal(helper, 17, Items.ROTTEN_FLESH, 0,
                    "0x item rule with ordinary XP enabled");
            assertExperienceTotalAndClear(helper, 17, 21,
                    "ordinary XP multiplier independent of 0x item rule");

            ExperienceOrb.award(helper.getLevel(), absoluteCenter(helper, 25), 7);
            assertExperienceTotalAndClear(helper, 25, 7, "unrelated nearby XP");

            helper.getLevel().getGameRules().set(
                    GameRules.MOB_DROPS,
                    false,
                    helper.getLevel().getServer());
            killByPlayer(helper, spawn(helper, GameTestEntityFixtures.HOSTILE, 20), player);
            assertItemTotal(helper, 20, Items.ROTTEN_FLESH, 0, "doMobLoot=false");
        } finally {
            clearExperienceOrbs(helper);
            helper.getLevel().getGameRules().set(
                    GameRules.MOB_DROPS,
                    previousMobDrops,
                    helper.getLevel().getServer());
            restoreEntityConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest(padding = 32)
    public void nestedAndExceptionalLootGenerationDoesNotLeakContext(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        GameTestEntityFixtures.resetTransientState();
        try {
            configureEntityTest(config -> {
                exactMultiplier(config, GameTestEntityFixtures.NESTED_OUTER, 2);
                exactMultiplier(config, GameTestEntityFixtures.HOSTILE, 3);
                exactMultiplier(config, GameTestEntityFixtures.EXCEPTION, 4);
            });
            final ServerPlayer player = GameTestPlayers.survival(helper);

            final Mob nestedTarget = spawn(helper, GameTestEntityFixtures.HOSTILE, 5);
            GameTestEntityFixtures.armNestedLoot(nestedTarget);
            killByPlayer(helper, spawn(helper, GameTestEntityFixtures.NESTED_OUTER, 2), player);
            assertItemTotal(helper, 2, Items.COAL, 2, "outer nested loot context");
            assertItemTotal(helper, 5, Items.ROTTEN_FLESH, 3, "inner nested loot context");

            final Mob exceptionFixture = spawn(helper, GameTestEntityFixtures.EXCEPTION, 9);
            GameTestEntityFixtures.throwOnNextExceptionLoot();
            try {
                killByPlayer(helper, exceptionFixture, player);
                throw new AssertionError("Intentional fixture loot exception was not propagated");
            } catch (FixtureLootException expected) {
                // The next call proves production context cleanup after the exceptional path.
            }
            final Mob postException = spawn(helper, GameTestEntityFixtures.EXCEPTION, 9);
            killByPlayer(helper, postException, player);
            assertItemTotal(helper, 9, Items.REDSTONE, 4, "post-exception loot context");

            killByPlayer(helper, spawn(helper, GameTestEntityFixtures.HOSTILE, 13), player);
            assertItemTotal(helper, 13, Items.ROTTEN_FLESH, 3, "independent context after exception");
        } finally {
            GameTestEntityFixtures.resetTransientState();
            restoreEntityConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest(padding = 32)
    public void duplicateStandardLootHookClaimsTheMultiplierExactlyOnce(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            configureEntityTest(config -> exactMultiplier(config, GameTestEntityFixtures.DUPLICATE_HOOK, 2));
            final ServerPlayer player = GameTestPlayers.survival(helper);
            killByPlayer(helper, spawn(helper, GameTestEntityFixtures.DUPLICATE_HOOK, 4), player);
            assertItemTotal(
                    helper,
                    4,
                    Items.LAPIS_LAZULI,
                    3,
                    "same standard loot path invoked twice in one death session");

            configureEntityTest(config -> exactMultiplier(config, GameTestEntityFixtures.HOSTILE, 2));
            final Mob wrapperFixture = spawn(helper, GameTestEntityFixtures.HOSTILE, 9);
            final List<ItemStack> sameList = new ArrayList<>();
            try (EntityDeathContext.Scope ignored = EntityDeathContext.begin(
                    wrapperFixture,
                    helper.getLevel(),
                    helper.getLevel().damageSources().genericKill())) {
                final java.util.function.Consumer<ItemStack> inner =
                        EntityDeathContext.wrapStandardLootConsumer(wrapperFixture, sameList::add);
                final java.util.function.Consumer<ItemStack> outer =
                        EntityDeathContext.wrapStandardLootConsumer(wrapperFixture, inner);
                outer.accept(new ItemStack(Items.ROTTEN_FLESH));
            }
            helper.assertTrue(
                    sameList.stream().mapToInt(ItemStack::getCount).sum() == 2,
                    "Two production wrappers around the same list multiplied 2x loot more than once");

            final List<ItemStack> playerDrops = new ArrayList<>();
            try (EntityDeathContext.Scope ignored = EntityDeathContext.begin(
                    player,
                    helper.getLevel(),
                    helper.getLevel().damageSources().genericKill())) {
                EntityDeathContext.wrapStandardLootConsumer(player, playerDrops::add)
                        .accept(new ItemStack(Items.DIAMOND));
            }
            helper.assertTrue(playerDrops.size() == 1 && playerDrops.getFirst().getCount() == 1,
                    "Runtime player-death context did not preserve vanilla output");
        } finally {
            restoreEntityConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest(padding = 32)
    public void finalLootingCookedEmptyBabyAndUnstackableResultsKeepVanillaShape(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        GameTestEntityFixtures.resetTransientState();
        try {
            final ServerPlayer player = GameTestPlayers.survival(helper);

            configureEntityTest(config -> exactMultiplier(config, GameTestEntityFixtures.LOOTING_FINAL, 3));
            final ItemStack lootingSword = new ItemStack(Items.DIAMOND_SWORD);
            lootingSword.enchant(
                    helper.getLevel().registryAccess().lookupOrThrow(
                            net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING),
                    3);
            player.setItemSlot(EquipmentSlot.MAINHAND, lootingSword);
            killByPlayer(helper, spawn(helper, GameTestEntityFixtures.LOOTING_FINAL, 1), player);
            assertItemTotal(helper, 1, Items.GOLD_NUGGET, 21,
                    "mapped Looting III final count multiplied without reroll");
            assertItemTotal(helper, 1, Items.GOLD_INGOT, 3,
                    "mapped killed_by_player conditional output");

            kill(helper, spawn(helper, GameTestEntityFixtures.LOOTING_FINAL, 21),
                    helper.getLevel().damageSources().genericKill());
            assertItemTotal(helper, 21, Items.GOLD_NUGGET, 12,
                    "non-player damage source uses the unenchanted final count");
            assertItemTotal(helper, 21, Items.GOLD_INGOT, 0,
                    "killed_by_player condition rejects environmental death");

            configureEntityTest(config -> exactMultiplier(config, GameTestEntityFixtures.COOKED_FINAL, 2));
            final Mob cooked = spawn(helper, GameTestEntityFixtures.COOKED_FINAL, 5);
            cooked.igniteForSeconds(10.0F);
            killByPlayer(helper, cooked, player);
            assertItemTotal(helper, 5, Items.COOKED_BEEF, 2, "final cooked death-table item");
            assertItemTotal(helper, 5, Items.BEEF, 0, "raw item after final furnace-smelt function");

            configureEntityTest(config -> exactMultiplier(config, GameTestEntityFixtures.EMPTY, 3));
            killByPlayer(helper, spawn(helper, GameTestEntityFixtures.EMPTY, 9), player);
            helper.assertTrue(allItemDrops(helper, 9).isEmpty(), "Empty entity loot table was recreated");

            configureEntityTest(config -> exactMultiplier(config, GameTestEntityFixtures.UNSTACKABLE, 3));
            killByPlayer(helper, spawn(helper, GameTestEntityFixtures.UNSTACKABLE, 13), player);
            assertItemTotal(helper, 13, Items.IRON_SWORD, 3, "unstackable final loot");
            final List<ItemEntity> swords = itemDrops(helper, 13, Items.IRON_SWORD);
            helper.assertTrue(
                    swords.size() == 3 && swords.stream().allMatch(drop -> drop.getItem().getCount() == 1),
                    "Unstackable item was not emitted as three legal individual stacks");

            configureEntityTest(config -> exactMultiplier(config, EntityTypes.COW, 3));
            final Mob baby = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(17, 2, 4));
            baby.setBaby(true);
            killByPlayer(helper, baby, player);
            assertItemTotal(helper, 17, Items.BEEF, 0, "baby mob with no standard drops");
            assertItemTotal(helper, 17, Items.LEATHER, 0, "baby mob leather with no standard drops");
        } finally {
            GameTestEntityFixtures.resetTransientState();
            restoreEntityConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest(padding = 32)
    public void runtimeRulePrecedenceAndWhitelistModesAreComplete(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            final ServerPlayer player = GameTestPlayers.survival(helper);
            final Mob hostile = spawn(helper, GameTestEntityFixtures.HOSTILE, 4);
            final String hostileId = EntityType.getKey(GameTestEntityFixtures.HOSTILE).toString();

            configureEntityTest(config -> {
                config.globalMultiplier = 2;
                config.inheritDefaultEntityMultiplier = false;
                config.defaultEntityMultiplier = 3;
                config.entityCategoryMultipliers.put(EntityCategory.HOSTILE.key(), 4);
                config.entityMultipliers.put(hostileId, 5);
            });
            EntityRuleTrace trace = EntityMultiplierResolver.inspect(helper.getLevel(), hostile, player);
            helper.assertTrue(trace.appliedMultiplier() == 5
                            && trace.selectedRule() == EntityRuleTrace.RuleSource.ENTITY_OVERRIDE,
                    "Exact entity override did not win");

            configureEntityTest(config -> {
                config.globalMultiplier = 2;
                config.inheritDefaultEntityMultiplier = false;
                config.defaultEntityMultiplier = 3;
                config.entityCategoryMultipliers.put(EntityCategory.HOSTILE.key(), 4);
            });
            trace = EntityMultiplierResolver.inspect(helper.getLevel(), hostile, player);
            helper.assertTrue(trace.appliedMultiplier() == 4
                            && trace.selectedRule() == EntityRuleTrace.RuleSource.CATEGORY_OVERRIDE,
                    "Entity category override did not win over the default/global rule");

            configureEntityTest(config -> {
                config.globalMultiplier = 2;
                config.inheritDefaultEntityMultiplier = false;
                config.defaultEntityMultiplier = 3;
            });
            trace = EntityMultiplierResolver.inspect(helper.getLevel(), hostile, player);
            helper.assertTrue(trace.appliedMultiplier() == 3
                            && trace.selectedRule() == EntityRuleTrace.RuleSource.ENTITY_DEFAULT,
                    "Default entity multiplier did not win over the global rule");

            configureEntityTest(config -> {
                config.globalMultiplier = 2;
                config.inheritDefaultEntityMultiplier = true;
            });
            trace = EntityMultiplierResolver.inspect(helper.getLevel(), hostile, player);
            helper.assertTrue(trace.appliedMultiplier() == 2
                            && trace.selectedRule() == EntityRuleTrace.RuleSource.GLOBAL,
                    "Inherited entity default did not fall back to the global rule");

            configureEntityTest(config -> {
                config.entityFilterMode = SmartDropsConfig.FilterMode.WHITELIST;
                config.entityWhitelist.add(hostileId);
            });
            trace = EntityMultiplierResolver.inspect(helper.getLevel(), hostile, player);
            helper.assertTrue(trace.filterEligible() && trace.exactWhitelisted(),
                    "Exact entity whitelist match did not allow the fixture");

            configureEntityTest(config -> {
                config.entityFilterMode = SmartDropsConfig.FilterMode.WHITELIST;
                config.entityTagWhitelist.add("smart_resource_drops_gametest:filter_fixture");
            });
            trace = EntityMultiplierResolver.inspect(helper.getLevel(), hostile, player);
            helper.assertTrue(trace.filterEligible()
                            && trace.matchingWhitelistTags().contains(
                                    "smart_resource_drops_gametest:filter_fixture"),
                    "Entity-type tag whitelist match did not allow the fixture");

            configureEntityTest(config -> config.entityFilterMode = SmartDropsConfig.FilterMode.WHITELIST);
            trace = EntityMultiplierResolver.inspect(helper.getLevel(), hostile, player);
            helper.assertFalse(trace.filterEligible(), "Whitelist mode allowed an unmatched entity");

            final EntityRuleTrace playerTrace = EntityMultiplierResolver.inspect(
                    helper.getLevel(),
                    player,
                    player);
            helper.assertTrue(playerTrace.permanentlyExcluded() && !playerTrace.itemEligible(),
                    "Player entity was not permanently excluded from entity drops");
        } finally {
            restoreEntityConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest(padding = 32)
    public void moddedClassificationPriorityFallbackAndInspectionAreDeterministic(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            configureEntityTest(config -> {
                config.entityKillRequirement = SmartDropsConfig.EntityKillRequirement.PLAYER_KILLS_ONLY;
                config.bossDropsEnabled = true;
                config.entityCategoryMultipliers.put(EntityCategory.PASSIVE.key(), 2);
                config.entityCategoryMultipliers.put(EntityCategory.HOSTILE.key(), 3);
                config.entityCategoryMultipliers.put(EntityCategory.NEUTRAL.key(), 4);
                config.entityCategoryMultipliers.put(EntityCategory.AQUATIC.key(), 5);
                config.entityCategoryMultipliers.put(EntityCategory.BOSSES.key(), 6);
                config.entityCategoryMultipliers.put(EntityCategory.MISCELLANEOUS.key(), 1);
            });
            final ServerPlayer player = GameTestPlayers.survival(helper);
            final Mob passive = spawn(helper, GameTestEntityFixtures.PASSIVE, 1);
            final Mob hostile = spawn(helper, GameTestEntityFixtures.HOSTILE, 4);
            final Mob neutral = spawn(helper, GameTestEntityFixtures.NEUTRAL, 7);
            final Mob aquatic = spawn(helper, GameTestEntityFixtures.AQUATIC, 10);
            final Mob categoryOnly = spawn(helper, GameTestEntityFixtures.CATEGORY_ONLY, 13);
            final Mob unclassified = spawn(helper, GameTestEntityFixtures.UNCLASSIFIED, 16);
            final Mob boss = spawn(helper, GameTestEntityFixtures.BOSS, 19);

            final EntityRuleTrace passiveTrace =
                    assertInspection(helper, player, passive, EntityCategory.PASSIVE, 2, false, false);
            final EntityRuleTrace hostileTrace =
                    assertInspection(helper, player, hostile, EntityCategory.HOSTILE, 3, false, false);
            final EntityRuleTrace neutralTrace =
                    assertInspection(helper, player, neutral, EntityCategory.NEUTRAL, 4, false, false);
            final EntityRuleTrace aquaticTrace =
                    assertInspection(helper, player, aquatic, EntityCategory.AQUATIC, 5, false, false);
            assertClassFallback(helper, passiveTrace, EntityCategory.PASSIVE);
            assertClassFallback(helper, hostileTrace, EntityCategory.HOSTILE);
            assertClassFallback(helper, neutralTrace, EntityCategory.NEUTRAL);
            assertClassFallback(helper, aquaticTrace, EntityCategory.AQUATIC);
            final EntityRuleTrace multiCategory = assertInspection(
                    helper,
                    player,
                    categoryOnly,
                    EntityCategory.PASSIVE,
                    2,
                    false,
                    false);
            helper.assertTrue(
                    multiCategory.matchedCategories().contains(EntityCategory.HOSTILE)
                            && multiCategory.matchedCategories().contains(EntityCategory.AMBIENT),
                    "Multi-category fixture did not retain all classification evidence");
            assertInspection(helper, player, unclassified, EntityCategory.MISCELLANEOUS, 1, false, true);
            assertInspection(helper, player, boss, EntityCategory.BOSSES, 6, true, false);

            killByPlayer(helper, passive, player);
            assertItemTotal(helper, 1, Items.CLAY_BALL, 2,
                    "untagged Animal class fallback at category 2x");
            killByPlayer(helper, categoryOnly, player);
            assertItemTotal(helper, 13, Items.STICK, 2,
                    "multi-category priority selected passive 2x during real death");

            configureEntityTest(config -> {
                config.entityKillRequirement = SmartDropsConfig.EntityKillRequirement.PLAYER_KILLS_ONLY;
                config.entityTagBlacklist.add("smart_resource_drops_gametest:filter_fixture");
            });
            final EntityRuleTrace filtered = EntityMultiplierResolver.inspect(helper.getLevel(), hostile, player);
            helper.assertFalse(filtered.filterEligible(), "Runtime entity-type tag blacklist was ignored");
            helper.assertTrue(
                    filtered.matchingBlacklistTags().contains("smart_resource_drops_gametest:filter_fixture"),
                    "Inspection omitted the matching runtime entity-type tag");
        } finally {
            restoreEntityConfiguration(previous);
        }
        helper.succeed();
    }

    @GameTest(padding = 32)
    public void entityInspectCommandTargetsVerboseMissAndConsoleWithoutMutation(final GameTestHelper helper) {
        final SmartDropsConfig previous = ConfigManager.snapshot();
        try {
            configureEntityTest(config -> exactMultiplier(config, GameTestEntityFixtures.HOSTILE, 3));
            final ServerPlayer player = GameTestPlayers.survival(helper);
            final Mob target = helper.spawnWithNoFreeWill(
                    GameTestEntityFixtures.HOSTILE,
                    new BlockPos(4, 2, 6));
            player.setPos(target.getX(), target.getY(), target.getZ() - 2.5);
            aimAt(player, target.getEyePosition());

            final float health = target.getHealth();
            final Vec3 position = target.position();
            final long revision = ConfigManager.revision();
            final int itemsBefore = helper.getLevel().getEntities(EntityTypes.ITEM, target.getBoundingBox().inflate(3.0),
                    ItemEntity::isAlive).size();
            final int xpBefore = helper.getLevel().getEntities(
                    EntityTypes.EXPERIENCE_ORB,
                    target.getBoundingBox().inflate(3.0),
                    ExperienceOrb::isAlive).size();

            final CapturingCommandSource compact = new CapturingCommandSource();
            helper.assertTrue(
                    executeCommand(
                            helper,
                            "smartdrops inspect entity",
                            player.createCommandSourceStack().withSource(compact)) == 1,
                    "Looked-at entity inspection did not succeed");
            helper.assertTrue(
                    compact.text().contains("Smart Resource Multiplier Entity Inspection")
                            && compact.text().contains("smart_resource_drops_gametest:hostile"),
                    "Compact entity inspection omitted its heading or entity ID");

            final CapturingCommandSource verbose = new CapturingCommandSource();
            helper.assertTrue(
                    executeCommand(
                            helper,
                            "smartdrops inspect entity verbose",
                            player.createCommandSourceStack().withSource(verbose)) == 1,
                    "Verbose looked-at entity inspection did not succeed");
            helper.assertTrue(
                    verbose.text().contains("Classification reason")
                            && verbose.text().contains("Runtime entity-type tags")
                            && verbose.text().contains("Item rule reason"),
                    "Verbose entity inspection omitted diagnostic sections");

            helper.assertTrue(target.isAlive() && target.getHealth() == health,
                    "Entity inspection damaged or killed its target");
            helper.assertTrue(target.position().equals(position), "Entity inspection moved its target");
            helper.assertTrue(ConfigManager.revision() == revision,
                    "Entity inspection changed the authoritative config revision");
            helper.assertTrue(
                    helper.getLevel().getEntities(EntityTypes.ITEM, target.getBoundingBox().inflate(3.0),
                            ItemEntity::isAlive).size() == itemsBefore,
                    "Entity inspection spawned item output");
            helper.assertTrue(
                    helper.getLevel().getEntities(
                            EntityTypes.EXPERIENCE_ORB,
                            target.getBoundingBox().inflate(3.0),
                            ExperienceOrb::isAlive).size() == xpBefore,
                    "Entity inspection spawned experience output");

            player.setPos(target.getX(), target.getY() + 8.0, target.getZ());
            player.absSnapRotationTo(player.getYRot(), -90.0F);
            final CapturingCommandSource miss = new CapturingCommandSource();
            helper.assertTrue(
                    executeCommand(
                            helper,
                            "smartdrops inspect entity verbose",
                            player.createCommandSourceStack().withSource(miss)) == 0,
                    "Entity inspection without a target did not fail cleanly");
            helper.assertTrue(
                    miss.text().contains("No living entity is currently targeted")
                            && miss.text().contains("within interaction range"),
                    "No-target entity inspection omitted recovery guidance");

            final CapturingCommandSource console = new CapturingCommandSource();
            helper.assertTrue(
                    executeCommand(
                            helper,
                            "smartdrops inspect entity",
                            helper.getLevel().getServer().createCommandSourceStack().withSource(console)) == 0,
                    "Console entity inspection did not require a player");
            helper.assertTrue(
                    console.text().contains("A player target is required")
                            && console.text().contains("/smartdrops inspect entity as a player"),
                    "Console entity inspection omitted player guidance");
        } finally {
            restoreEntityConfiguration(previous);
        }
        helper.succeed();
    }

    private static EntityRuleTrace assertInspection(
            final GameTestHelper helper,
            final ServerPlayer player,
            final Mob entity,
            final EntityCategory expectedCategory,
            final int expectedMultiplier,
            final boolean expectedBoss,
            final boolean expectedFallback) {
        final float health = entity.getHealth();
        final Vec3 position = entity.position();
        final EntityRuleTrace first = EntityMultiplierResolver.inspect(helper.getLevel(), entity, player);
        final EntityRuleTrace second = EntityMultiplierResolver.inspect(helper.getLevel(), entity, player);
        helper.assertTrue(first.equals(second), "Repeated entity inspection produced different traces");
        helper.assertTrue(first.selectedCategory() == expectedCategory,
                "Expected " + expectedCategory + " but selected " + first.selectedCategory());
        helper.assertTrue(first.appliedMultiplier() == expectedMultiplier,
                "Expected " + expectedMultiplier + "x but resolved " + first.appliedMultiplier() + "x");
        helper.assertTrue(first.boss() == expectedBoss, "Boss classification did not match the fixture");
        helper.assertTrue(first.miscellaneousFallback() == expectedFallback,
                "Miscellaneous fallback did not match the fixture");
        helper.assertTrue(entity.isAlive() && entity.getHealth() == health && entity.position().equals(position),
                "Read-only inspection mutated the fixture entity");
        return first;
    }

    private static void assertClassFallback(
            final GameTestHelper helper,
            final EntityRuleTrace trace,
            final EntityCategory category) {
        helper.assertTrue(
                trace.categorySources().getOrDefault(category, java.util.Set.of())
                        .contains(EntityClassification.MatchSource.VANILLA_CLASS),
                category + " fixture was not classified through the vanilla class fallback");
        helper.assertFalse(
                trace.categorySources().getOrDefault(category, java.util.Set.of())
                        .contains(EntityClassification.MatchSource.SMART_RESOURCE_DROPS_TAG),
                category + " fixture was still masked by a project-owned category tag");
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

    private static int executeCommand(
            final GameTestHelper helper,
            final String command,
            final CommandSourceStack source) {
        try {
            return helper.getLevel().getServer().getCommands().getDispatcher().execute(command, source);
        } catch (CommandSyntaxException exception) {
            throw new AssertionError("Entity inspection command failed: " + command, exception);
        }
    }

    private static void configureEntityTest(final java.util.function.Consumer<SmartDropsConfig> customization) {
        if (!ConfigManager.update(config -> {
            config.entityDropsEnabled = true;
            config.inheritDefaultEntityMultiplier = false;
            config.defaultEntityMultiplier = 1;
            config.entityKillRequirement = SmartDropsConfig.EntityKillRequirement.ALL_STANDARD_DEATH_LOOT;
            config.entityFilterMode = SmartDropsConfig.FilterMode.BLACKLIST;
            config.bossDropsEnabled = false;
            config.multiplyMobExperience = false;
            config.mobExperienceMultiplier = 2;
            config.multiplyBossExperience = false;
            config.entityCategoryMultipliers.clear();
            config.entityMultipliers.clear();
            config.entityBlacklist.clear();
            config.entityWhitelist.clear();
            config.entityTagBlacklist.clear();
            config.entityTagWhitelist.clear();
            customization.accept(config);
        })) {
            throw new AssertionError("Could not prepare entity GameTest configuration");
        }
    }

    private static void exactMultiplier(
            final SmartDropsConfig config,
            final EntityType<?> type,
            final int multiplier) {
        config.entityMultipliers.put(EntityType.getKey(type).toString(), multiplier);
    }

    private static <T extends Mob> T spawn(
            final GameTestHelper helper,
            final EntityType<T> type,
            final int x) {
        return helper.spawnWithNoFreeWill(type, new BlockPos(x, 2, 4));
    }

    private static void killByPlayer(
            final GameTestHelper helper,
            final Mob victim,
            final ServerPlayer player) {
        kill(helper, victim, helper.getLevel().damageSources().playerAttack(player));
    }

    private static void kill(
            final GameTestHelper helper,
            final Mob victim,
            final DamageSource source) {
        helper.assertTrue(
                victim.hurtServer(helper.getLevel(), source, Float.MAX_VALUE),
                "Fixture refused lethal damage: " + EntityType.getKey(victim.getType()));
        helper.assertTrue(victim.isDeadOrDying(), "Fixture survived lethal damage");
    }

    private static void assertBossOutputs(
            final GameTestHelper helper,
            final int x,
            final int expectedOrdinaryLoot,
            final String scenario) {
        assertItemTotal(helper, x, Items.GOLD_INGOT, expectedOrdinaryLoot, scenario + " ordinary loot");
        assertItemTotal(helper, x, Items.SADDLE, 1, scenario + " protected saddle");
        assertItemTotal(helper, x, Items.TOTEM_OF_UNDYING, 1, scenario + " protected totem");
    }

    private static void assertComponentRichDrop(
            final GameTestHelper helper,
            final int x,
            final int expected,
            final String scenario) {
        assertItemTotal(helper, x, Items.DIAMOND, expected, scenario);
        for (ItemEntity drop : itemDrops(helper, x, Items.DIAMOND)) {
            final ItemStack stack = drop.getItem();
            helper.assertTrue(
                    stack.get(DataComponents.CUSTOM_NAME) != null
                            && GameTestEntityFixtures.COMPONENT_MARKER.equals(
                                    stack.get(DataComponents.CUSTOM_NAME).getString()),
                    scenario + " lost its custom name component");
            helper.assertTrue(
                    stack.get(DataComponents.CUSTOM_DATA) != null
                            && GameTestEntityFixtures.COMPONENT_MARKER.equals(
                                    stack.get(DataComponents.CUSTOM_DATA).copyTag()
                                            .getString("fixture").orElse("")),
                    scenario + " lost its custom data component");
        }
    }

    private static void assertItemTotal(
            final GameTestHelper helper,
            final int x,
            final Item item,
            final int expected,
            final String scenario) {
        final int actual = itemDrops(helper, x, item).stream()
                .map(ItemEntity::getItem)
                .mapToInt(ItemStack::getCount)
                .sum();
        helper.assertTrue(
                actual == expected,
                scenario + " produced " + actual + " " + item + " items instead of " + expected);
    }

    private static void assertLegalStacks(final GameTestHelper helper, final int x, final Item item) {
        for (ItemEntity drop : itemDrops(helper, x, item)) {
            helper.assertTrue(
                    drop.getItem().getCount() <= drop.getItem().getMaxStackSize(),
                    "Multiplier emitted an illegal stack: " + drop.getItem());
        }
    }

    private static List<ItemEntity> itemDrops(
            final GameTestHelper helper,
            final int x,
            final Item item) {
        return allItemDrops(helper, x).stream()
                .filter(entity -> entity.getItem().getItem() == item)
                .toList();
    }

    private static List<ItemEntity> allItemDrops(final GameTestHelper helper, final int x) {
        final Vec3 center = absoluteCenter(helper, x);
        return helper.getLevel().getEntities(
                EntityTypes.ITEM,
                new AABB(center, center).inflate(1.5),
                ItemEntity::isAlive);
    }

    private static void assertExperienceTotalAndClear(
            final GameTestHelper helper,
            final int x,
            final int expected,
            final String scenario) {
        final Vec3 center = absoluteCenter(helper, x);
        final int actual = helper.getLevel().getEntities(
                        EntityTypes.EXPERIENCE_ORB,
                        new AABB(center, center).inflate(2.0),
                        ExperienceOrb::isAlive)
                .stream()
                .mapToInt(ExperienceOrb::getValue)
                .sum();
        helper.assertTrue(
                actual == expected,
                scenario + " produced " + actual + " XP instead of " + expected);
        clearExperienceOrbs(helper);
    }

    private static void clearExperienceOrbs(final GameTestHelper helper) {
        helper.killAllEntitiesOfClass(ExperienceOrb.class);
    }

    private static Vec3 absoluteCenter(final GameTestHelper helper, final int x) {
        return Vec3.atCenterOf(helper.absolutePos(new BlockPos(x, 2, 4)));
    }

    private static void restoreEntityConfiguration(final SmartDropsConfig previous) {
        if (!ConfigManager.update(config ->
                SmartResourceDropsShearingGameTests.copyConfiguration(config, previous))) {
            throw new AssertionError("Could not restore entity GameTest configuration");
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
    }
}
