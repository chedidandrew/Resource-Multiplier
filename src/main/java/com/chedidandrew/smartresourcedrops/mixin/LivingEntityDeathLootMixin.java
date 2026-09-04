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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

@Mixin(LivingEntity.class)
abstract class LivingEntityDeathLootMixin implements EntityKillOriginAccess {
    @Shadow protected int lastHurtByPlayerMemoryTime;
    @Shadow public abstract Player getLastHurtByPlayer();

    @Unique
    private EntityKillAttribution.Kind smartResourceDrops$rememberedKillOrigin =
            EntityKillAttribution.Kind.NONE;

    @WrapMethod(method = "dropAllDeathLoot(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;)V")
    private void smartResourceDrops$scopeStandardDeathLoot(
            final ServerLevel level,
            final DamageSource source,
            final Operation<Void> original
    ) {
        final LivingEntity self = (LivingEntity) (Object) this;
        try (EntityDeathContext.Scope ignored = EntityDeathContext.begin(self, level, source)) {
            original.call(level, source);
        }
    }

    @WrapOperation(
            method = "dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;Z)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/storage/loot/LootTable;getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;JLjava/util/function/Consumer;)V"),
            require = 1,
            expect = 1)
    private void smartResourceDrops$wrapOnlyStandardLootConsumer(
            final LootTable table,
            final LootParams params,
            final long seed,
            final Consumer<ItemStack> consumer,
            final Operation<Void> original
    ) {
        final LivingEntity self = (LivingEntity) (Object) this;
        original.call(table, params, seed,
                EntityDeathContext.wrapStandardLootConsumer(self, consumer));
    }

    @WrapOperation(
            method = "dropExperience(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;award(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)V"),
            require = 1,
            expect = 1)
    private void smartResourceDrops$multiplyOnlyMobDeathExperience(
            final ServerLevel level,
            final Vec3 position,
            final int amount,
            final Operation<Void> original
    ) {
        final LivingEntity self = (LivingEntity) (Object) this;
        final int multiplied = EntityDeathContext.multiplyExperience(self, amount);
        try (EntityDeathContext.ExperienceAwardScope ignored =
                     EntityDeathContext.expectMobExperienceAward(level, position, multiplied)) {
            original.call(level, position, multiplied);
        }
    }

    @Inject(
            method = "setLastHurtByPlayer(Lnet/minecraft/world/entity/player/Player;I)V",
            at = @At("TAIL"),
            require = 1,
            expect = 1)
    private void smartResourceDrops$rememberDirectPlayer(
            final Player player,
            final int memoryTime,
            final CallbackInfo callback
    ) {
        smartResourceDrops$rememberedKillOrigin = smartResourceDrops$trackingEnabled()
                && EntityMultiplierResolver.isRealPlayer(player)
                ? EntityKillAttribution.Kind.DIRECT_PLAYER
                : EntityKillAttribution.Kind.NONE;
    }

    @Inject(
            method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At("RETURN"),
            require = 1,
            expect = 1)
    private void smartResourceDrops$rememberSuccessfulDamageOrigin(
            final ServerLevel level,
            final DamageSource source,
            final float amount,
            final CallbackInfoReturnable<Boolean> callback
    ) {
        if (!callback.getReturnValueZ()) {
            return;
        }
        if (source.getEntity() instanceof Player player) {
            smartResourceDrops$rememberedKillOrigin = smartResourceDrops$trackingEnabled()
                    && EntityMultiplierResolver.isRealPlayer(player)
                    ? EntityKillAttribution.Kind.DIRECT_PLAYER
                    : EntityKillAttribution.Kind.NONE;
        } else if (source.getEntity() instanceof Wolf wolf && wolf.isTame()) {
            smartResourceDrops$rememberedKillOrigin = smartResourceDrops$trackingEnabled()
                    && EntityMultiplierResolver.resolvedTamedOwner(wolf) != null
                    ? EntityKillAttribution.Kind.TAMED_ENTITY
                    : EntityKillAttribution.Kind.NONE;
        }
    }

    @Override
    public EntityKillAttribution.Kind smartResourceDrops$rememberedKillOrigin() {
        return lastHurtByPlayerMemoryTime > 0
                ? smartResourceDrops$rememberedKillOrigin
                : EntityKillAttribution.Kind.NONE;
    }

    @Override
    public boolean smartResourceDrops$hasRememberedPlayerKill() {
        return lastHurtByPlayerMemoryTime > 0 && getLastHurtByPlayer() != null;
    }

    @Override
    public Player smartResourceDrops$rememberedPlayer() {
        return lastHurtByPlayerMemoryTime > 0 ? getLastHurtByPlayer() : null;
    }

    @Unique
    private boolean smartResourceDrops$trackingEnabled() {
        return EntityMultiplierResolver.rulesCouldApply(
                ConfigManager.get(), (LivingEntity) (Object) this);
    }
}
