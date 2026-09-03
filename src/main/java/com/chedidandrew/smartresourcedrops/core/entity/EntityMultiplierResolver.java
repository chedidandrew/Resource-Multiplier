package com.chedidandrew.smartresourcedrops.core.entity;

import com.chedidandrew.smartresourcedrops.config.ConfigManager;
import com.chedidandrew.smartresourcedrops.config.SmartDropsConfig;
import com.chedidandrew.smartresourcedrops.platform.PlatformPlayerSupport;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.UUID;

/** Builds rule-engine input from authoritative server entities. */
public final class EntityMultiplierResolver {
    private EntityMultiplierResolver() {
    }

    public static EntityRuleEngine.Decision resolve(
            ServerLevel level,
            LivingEntity entity,
            DamageSource source
    ) {
        return trace(level, entity, source).decision();
    }

    public static EntityRuleTrace trace(
            ServerLevel level,
            LivingEntity entity,
            DamageSource source
    ) {
        return trace(ConfigManager.get(), entity, resolveAttribution(entity, source), false);
    }

    /** Read-only hypothetical direct-player inspection; it does not touch death or combat state. */
    public static EntityRuleTrace inspect(
            ServerLevel level,
            LivingEntity entity,
            @Nullable ServerPlayer viewer
    ) {
        boolean realViewer = viewer != null && !PlatformPlayerSupport.isFakePlayer(viewer);
        EntityKillAttribution attribution = realViewer
                ? EntityKillAttribution.direct(viewer.getUUID(), true)
                : EntityKillAttribution.none(false);
        return trace(ConfigManager.get(), entity, attribution, realViewer);
    }

    public static EntityRuleTrace inspect(ServerLevel level, LivingEntity entity) {
        return inspect(level, entity, null);
    }

    public static EntityKillAttribution resolveAttribution(LivingEntity entity, DamageSource source) {
        EntityKillOriginAccess originAccess = (EntityKillOriginAccess) entity;
        boolean vanillaPlayerKilled = originAccess.smartResourceDrops$hasRememberedPlayer();
        Player rememberedPlayer = vanillaPlayerKilled ? originAccess.smartResourceDrops$rememberedPlayer() : null;
        UUID rememberedPlayerId = isRealPlayer(rememberedPlayer) ? rememberedPlayer.getUUID() : null;
        EntityKillAttribution.Kind rememberedOrigin = entity instanceof EntityKillOriginAccess access
                ? access.smartResourceDrops$rememberedKillOrigin()
                : EntityKillAttribution.Kind.NONE;
        Entity immediate = source.getEntity();
        if (isRealPlayer(immediate)) {
            Player player = (Player) immediate;
            return validateImmediatePlayer(
                    player.getUUID(),
                    vanillaPlayerKilled,
                    rememberedPlayerId,
                    rememberedOrigin);
        }

        Player tamedOwner = resolvedTamedOwner(immediate);
        if (tamedOwner != null) {
            return EntityKillAttribution.tamed(tamedOwner.getUUID(), vanillaPlayerKilled);
        }

        if (!vanillaPlayerKilled || rememberedPlayerId == null) {
            return EntityKillAttribution.none(vanillaPlayerKilled);
        }
        return switch (rememberedOrigin) {
            case DIRECT_PLAYER -> EntityKillAttribution.direct(rememberedPlayerId, true);
            case TAMED_ENTITY -> EntityKillAttribution.tamed(rememberedPlayerId, true);
            case NONE -> EntityKillAttribution.none(true);
        };
    }

    static EntityKillAttribution validateImmediatePlayer(
            UUID immediatePlayerId,
            boolean vanillaPlayerKilled,
            @Nullable UUID rememberedPlayerId,
            EntityKillAttribution.Kind rememberedOrigin
    ) {
        if (!vanillaPlayerKilled
                || rememberedOrigin != EntityKillAttribution.Kind.DIRECT_PLAYER
                || !immediatePlayerId.equals(rememberedPlayerId)) {
            return EntityKillAttribution.none(vanillaPlayerKilled);
        }
        return EntityKillAttribution.direct(immediatePlayerId, true);
    }

    public static boolean rulesCouldApply(SmartDropsConfig config, LivingEntity entity) {
        return config.enabled
                && (config.entityDropsEnabled || config.multiplyMobExperience)
                && !(entity instanceof Player)
                && !(entity instanceof ArmorStand);
    }

    public static boolean isRealPlayer(@Nullable Entity entity) {
        return entity instanceof Player player && !PlatformPlayerSupport.isFakePlayer(player);
    }

    public static @Nullable Player resolvedTamedOwner(@Nullable Entity entity) {
        if (!(entity instanceof OwnableEntity ownable)) {
            return null;
        }
        if (entity instanceof TamableAnimal tamable && !tamable.isTame()) {
            return null;
        }
        LivingEntity rootOwner = ownable.getOwner();
        Set<LivingEntity> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        while (rootOwner instanceof OwnableEntity nested && visited.add(rootOwner)) {
            LivingEntity next = nested.getOwner();
            if (next == null) {
                break;
            }
            rootOwner = next;
        }
        return isRealPlayer(rootOwner) ? (Player) rootOwner : null;
    }

    private static EntityRuleTrace trace(
            SmartDropsConfig config,
            LivingEntity entity,
            EntityKillAttribution attribution,
            boolean invokingPlayerWouldQualify
    ) {
        EntityClassification classification = EntityClassifier.classify(entity);
        boolean permanentlyExcluded = entity instanceof Player
                || entity instanceof ArmorStand;
        String exclusionReason = entity instanceof Player
                ? "players are never entity-drop targets"
                : entity instanceof ArmorStand
                        ? "armor stands are never entity-drop targets"
                        : "none";
        return EntityRuleEngine.trace(config, new EntityRuleInput(
                classification.entityId(),
                classification,
                classification.runtimeTags(),
                attribution,
                permanentlyExcluded,
                exclusionReason,
                invokingPlayerWouldQualify));
    }
}
