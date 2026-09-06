package com.chedidandrew.smartresourcedrops.core.entity;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.npc.AbstractVillager;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Deterministic multi-signal classifier; enum declaration order defines selection priority. */
public final class EntityClassifier {
    private static final Set<String> KNOWN_BOSSES = Set.of(
            "minecraft:ender_dragon",
            "minecraft:wither",
            "minecraft:warden",
            "minecraft:elder_guardian",
            "minecraft:ravager",
            "minecraft:evoker");

    private EntityClassifier() {
    }

    public static EntityClassification classify(LivingEntity entity) {
        EntityType<?> type = entity.getType();
        Holder<EntityType<?>> typeHolder = BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type);
        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
        EnumMap<EntityCategory, Set<EntityClassification.MatchSource>> evidence =
                new EnumMap<>(EntityCategory.class);

        for (EntityCategory category : EntityCategory.values()) {
            if (typeHolder.is(EntityDropTags.categoryTag(category))) {
                add(evidence, category, EntityClassification.MatchSource.SMART_RESOURCE_DROPS_TAG);
            }
        }

        if (isKnownBossType(entityId)) {
            add(evidence, EntityCategory.BOSSES, EntityClassification.MatchSource.KNOWN_VANILLA_TYPE);
        }
        if (entity instanceof AbstractVillager || entity instanceof Npc) {
            add(evidence, EntityCategory.VILLAGERS_NPCS, EntityClassification.MatchSource.VANILLA_CLASS);
        }
        if (entity instanceof AbstractGolem) {
            add(evidence, EntityCategory.GOLEMS, EntityClassification.MatchSource.VANILLA_CLASS);
        }
        if (entity instanceof NeutralMob) {
            add(evidence, EntityCategory.NEUTRAL, EntityClassification.MatchSource.VANILLA_CLASS);
        }
        if (entity instanceof Animal) {
            add(evidence, EntityCategory.PASSIVE, EntityClassification.MatchSource.VANILLA_CLASS);
        }
        if (entity instanceof Monster) {
            add(evidence, EntityCategory.HOSTILE, EntityClassification.MatchSource.VANILLA_CLASS);
        }
        if (entity instanceof WaterAnimal) {
            add(evidence, EntityCategory.AQUATIC, EntityClassification.MatchSource.VANILLA_CLASS);
        }
        if (entity instanceof AmbientCreature) {
            add(evidence, EntityCategory.AMBIENT, EntityClassification.MatchSource.VANILLA_CLASS);
        }

        addMobCategoryEvidence(evidence, type.getCategory());

        boolean fallback = evidence.isEmpty();
        if (fallback) {
            add(evidence, EntityCategory.MISCELLANEOUS, EntityClassification.MatchSource.FALLBACK);
        }

        List<EntityCategory> matches = new ArrayList<>();
        for (EntityCategory category : EntityCategory.values()) {
            if (evidence.containsKey(category)) {
                matches.add(category);
            }
        }
        EntityCategory selected = selectCategory(matches, evidence);
        Set<EntityClassification.MatchSource> bossSources = evidence.getOrDefault(
                EntityCategory.BOSSES,
                Set.of());
        String reason = fallback
                ? "No recognized category tag or compatible entity class"
                : selected.key() + " via " + evidence.get(selected);
        return new EntityClassification(
                entityId,
                matches,
                selected,
                evidence,
                EntityDropTags.runtimeTags(type),
                evidence.containsKey(EntityCategory.BOSSES),
                bossSources,
                fallback,
                reason);
    }

    /** Instance-free conservative fallback shared by server classification and client catalogs. */
    public static boolean isKnownBossType(String entityId) {
        return KNOWN_BOSSES.contains(entityId);
    }

    private static EntityCategory selectCategory(
            List<EntityCategory> matches,
            Map<EntityCategory, Set<EntityClassification.MatchSource>> evidence
    ) {
        if (evidence.containsKey(EntityCategory.BOSSES)) {
            return EntityCategory.BOSSES;
        }
        for (EntityCategory category : EntityCategory.values()) {
            Set<EntityClassification.MatchSource> sources = evidence.get(category);
            if (sources != null && sources.contains(EntityClassification.MatchSource.SMART_RESOURCE_DROPS_TAG)) {
                return category;
            }
        }
        return matches.get(0);
    }

    private static void addMobCategoryEvidence(
            Map<EntityCategory, Set<EntityClassification.MatchSource>> evidence,
            MobCategory category
    ) {
        EntityCategory matched = switch (category) {
            case MONSTER -> EntityCategory.HOSTILE;
            case CREATURE -> EntityCategory.PASSIVE;
            case AMBIENT -> EntityCategory.AMBIENT;
            case AXOLOTLS, UNDERGROUND_WATER_CREATURE, WATER_CREATURE, WATER_AMBIENT ->
                    EntityCategory.AQUATIC;
            case MISC -> null;
        };
        if (matched != null) {
            add(evidence, matched, EntityClassification.MatchSource.MOB_CATEGORY);
        }
    }

    private static void add(
            Map<EntityCategory, Set<EntityClassification.MatchSource>> evidence,
            EntityCategory category,
            EntityClassification.MatchSource source
    ) {
        evidence.computeIfAbsent(category, ignored -> EnumSet.noneOf(EntityClassification.MatchSource.class))
                .add(source);
    }
}
