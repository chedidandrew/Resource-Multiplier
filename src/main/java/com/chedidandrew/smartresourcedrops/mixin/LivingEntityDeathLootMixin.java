package com.chedidandrew.smartresourcedrops.mixin;

import com.chedidandrew.smartresourcedrops.config.ConfigManager;
import com.chedidandrew.smartresourcedrops.core.entity.EntityDeathContext;
import com.chedidandrew.smartresourcedrops.core.entity.EntityKillAttribution;
import com.chedidandrew.smartresourcedrops.core.entity.EntityKillOriginAccess;
import com.chedidandrew.smartresourcedrops.core.entity.EntityMultiplierResolver;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

@Mixin(LivingEntity.class)
abstract class LivingEntityDeathLootMixin implements EntityKillOriginAccess {
    @Unique
    private EntityKillAttribution.Kind smartResourceDrops$rememberedKillOrigin =
            EntityKillAttribution.Kind.NONE;

    @WrapMethod(
            method = "dropAllDeathLoot(Lnet/minecraft/world/damagesource/DamageSource;)V")
    private void smartResourceDrops$scopeStandardDeathLoot(
            DamageSource source,
            Operation<Void> original
    ) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self.level() instanceof ServerLevel level)) {
            original.call(source);
            return;
        }
        try (EntityDeathContext.Scope ignored = EntityDeathContext.begin(self, level, source)) {
            original.call(source);
        }
    }

    @WrapOperation(
            method = "dropFromLootTable(Lnet/minecraft/world/damagesource/DamageSource;Z)V",
             at = @At(
                     value = "INVOKE",
                     target = "Lnet/minecraft/world/level/storage/loot/LootTable;getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;JLjava/util/function/Consumer;)V"),
             require = 1,
             expect = 1)
    private void smartResourceDrops$wrapOnlyStandardLootConsumer(
            LootTable table,
            LootParams params,
            long seed,
            Consumer<ItemStack> consumer,
            Operation<Void> original
    ) {
        LivingEntity self = (LivingEntity) (Object) this;
        Consumer<ItemStack> multiplied = EntityDeathContext.wrapStandardLootConsumer(self, consumer);
        original.call(table, params, seed, multiplied);
    }

    @WrapOperation(
            method = "dropExperience()V",
             at = @At(
                     value = "INVOKE",
                     target = "Lnet/minecraft/world/entity/ExperienceOrb;award(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)V"),
             require = 1,
             expect = 1)
    private void smartResourceDrops$multiplyOnlyMobDeathExperience(
            ServerLevel level,
            Vec3 position,
            int amount,
            Operation<Void> original
    ) {
        LivingEntity self = (LivingEntity) (Object) this;
        int multiplied = EntityDeathContext.multiplyExperience(self, amount);
        try (EntityDeathContext.ExperienceAwardScope ignored =
                     EntityDeathContext.expectMobExperienceAward(level, position, multiplied)) {
            original.call(level, position, multiplied);
        }
    }

    @Inject(
            method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/entity/LivingEntity;lastHurtByPlayerTime:I",
                    opcode = Opcodes.PUTFIELD,
                    shift = At.Shift.AFTER),
            require = 2,
            expect = 2)
    private void smartResourceDrops$rememberVanillaAttributionOriginWhenVanillaCreditsPlayer(
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> callback
    ) {
        // These are the two accepted-damage paths where 1.20.1 vanilla grants
        // direct-player or tamed-wolf kill credit, before a same-call death.
        smartResourceDrops$rememberVanillaAttributionOrigin(source);
    }

    @Unique
    private void smartResourceDrops$rememberVanillaAttributionOrigin(DamageSource source) {
        if (!smartResourceDrops$trackingEnabled()) {
            if (source.getEntity() instanceof Player
                    || source.getEntity() instanceof Wolf wolf && wolf.isTame()) {
                smartResourceDrops$rememberedKillOrigin = EntityKillAttribution.Kind.NONE;
            }
            return;
        }

        if (source.getEntity() instanceof Player player) {
            smartResourceDrops$rememberedKillOrigin = EntityMultiplierResolver.isRealPlayer(player)
                    ? EntityKillAttribution.Kind.DIRECT_PLAYER
                    : EntityKillAttribution.Kind.NONE;
        } else if (source.getEntity() instanceof Wolf wolf && wolf.isTame()) {
            smartResourceDrops$rememberedKillOrigin = EntityMultiplierResolver.resolvedTamedOwner(wolf) != null
                    ? EntityKillAttribution.Kind.TAMED_ENTITY
                    : EntityKillAttribution.Kind.NONE;
        }
    }

    @Override
    public EntityKillAttribution.Kind smartResourceDrops$rememberedKillOrigin() {
        return smartResourceDrops$currentCreditedPlayer() != null
                ? smartResourceDrops$rememberedKillOrigin
                : EntityKillAttribution.Kind.NONE;
    }

    @Override
    public boolean smartResourceDrops$hasRememberedPlayer() {
        return smartResourceDrops$currentCreditedPlayer() != null;
    }

    @Override
    public Player smartResourceDrops$rememberedPlayer() {
        return smartResourceDrops$currentCreditedPlayer();
    }

    @Unique
    private Player smartResourceDrops$currentCreditedPlayer() {
        final LivingEntity killCredit = ((LivingEntity) (Object) this).getKillCredit();
        return killCredit instanceof Player player ? player : null;
    }

    @Unique
    private boolean smartResourceDrops$trackingEnabled() {
        return EntityMultiplierResolver.rulesCouldApply(
                ConfigManager.get(),
                (LivingEntity) (Object) this);
    }
}
