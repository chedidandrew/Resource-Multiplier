package com.chedidandrew.smartresourcedrops.core.shearing;

import com.chedidandrew.smartresourcedrops.config.ConfigManager;
import com.chedidandrew.smartresourcedrops.config.SmartDropsConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

import java.util.Map;
import java.util.Objects;

/** Resolves shearing policy from authoritative config and live entity-type tag membership. */
public final class ShearingRuleResolver {
    private ShearingRuleResolver() {
    }

    public static ShearingRuleTrace trace(EntityType<?> type, ShearingSource source) {
        return trace(ConfigManager.get(), type, source);
    }

    public static ShearingRuleTrace trace(
            SmartDropsConfig config,
            EntityType<?> type,
            ShearingSource source
    ) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(source, "source");

        Holder<EntityType<?>> holder = BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type);
        boolean standardTagged = holder.is(ShearingTags.STANDARD_RESOURCES);
        boolean specialTagged = holder.is(ShearingTags.SPECIAL);
        Identifier identifier = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        String entityId = identifier == null ? "minecraft:unregistered" : identifier.toString();
        return trace(config, entityId, standardTagged, specialTagged, source);
    }

    static ShearingRuleTrace trace(
            SmartDropsConfig config,
            String entityId,
            boolean standardTagged,
            boolean specialTagged,
            ShearingSource source
    ) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(entityId, "entityId");
        Objects.requireNonNull(source, "source");

        boolean knownVanillaSpecial = ShearingTags.isKnownVanillaSpecial(entityId);
        ShearingClassification classification = specialTagged || knownVanillaSpecial
                ? ShearingClassification.SPECIAL
                : standardTagged
                        ? ShearingClassification.STANDARD_RESOURCE
                        : ShearingClassification.UNKNOWN;
        int maximum = Math.max(1, Math.min(
                SmartDropsConfig.ABSOLUTE_MAX_MULTIPLIER,
                config.maximumMultiplier));
        boolean sourceEnabled = switch (source) {
            case MANUAL_PLAYER -> config.manualShearingDropsEnabled;
            case VANILLA_DISPENSER -> config.automatedShearingDropsEnabled;
        };
        int configuredDefault = bounded(
                config.inheritDefaultShearingMultiplier
                        ? config.globalMultiplier
                        : config.defaultShearingMultiplier,
                maximum);
        Map<String, Integer> overrides = config.shearingEntityMultipliers;
        Integer rawOverride = overrides == null ? null : overrides.get(entityId);
        Integer exactOverride = rawOverride == null ? null : bounded(rawOverride, maximum);

        if (classification == ShearingClassification.SPECIAL) {
            return result(
                    entityId,
                    classification,
                    standardTagged,
                    specialTagged,
                    knownVanillaSpecial,
                    source,
                    config,
                    sourceEnabled,
                    configuredDefault,
                    exactOverride,
                    1,
                    ShearingRuleTrace.RuleSource.SPECIAL_SAFETY,
                    standardTagged
                            ? "special safety wins over conflicting standard-resource certification"
                            : "special shearables are fixed at vanilla 1x");
        }
        if (classification == ShearingClassification.UNKNOWN) {
            return result(
                    entityId,
                    classification,
                    false,
                    false,
                    false,
                    source,
                    config,
                    sourceEnabled,
                    configuredDefault,
                    exactOverride,
                    1,
                    ShearingRuleTrace.RuleSource.UNKNOWN_SAFETY,
                    "unknown shearables require explicit standard-resource certification");
        }
        if (!config.enabled) {
            return result(
                    entityId,
                    classification,
                    true,
                    false,
                    false,
                    source,
                    config,
                    sourceEnabled,
                    configuredDefault,
                    exactOverride,
                    1,
                    ShearingRuleTrace.RuleSource.MOD_DISABLED,
                    "Smart Resource Multiplier is disabled");
        }
        if (!sourceEnabled) {
            return result(
                    entityId,
                    classification,
                    true,
                    false,
                    false,
                    source,
                    config,
                    false,
                    configuredDefault,
                    exactOverride,
                    1,
                    ShearingRuleTrace.RuleSource.SOURCE_DISABLED,
                    source == ShearingSource.MANUAL_PLAYER
                            ? "manual shearing drops are disabled"
                            : "automated shearing drops are disabled");
        }
        if (exactOverride != null) {
            return result(
                    entityId,
                    classification,
                    true,
                    false,
                    false,
                    source,
                    config,
                    true,
                    configuredDefault,
                    exactOverride,
                    exactOverride,
                    ShearingRuleTrace.RuleSource.ENTITY_OVERRIDE,
                    "exact certified shearing-entity override");
        }
        return result(
                entityId,
                classification,
                true,
                false,
                false,
                source,
                config,
                true,
                configuredDefault,
                null,
                configuredDefault,
                config.inheritDefaultShearingMultiplier
                        ? ShearingRuleTrace.RuleSource.GLOBAL_DEFAULT
                        : ShearingRuleTrace.RuleSource.SHEARING_DEFAULT,
                config.inheritDefaultShearingMultiplier
                        ? "shearing default inherits the global multiplier"
                        : "configured shearing default multiplier");
    }

    private static ShearingRuleTrace result(
            String entityId,
            ShearingClassification classification,
            boolean standardTagged,
            boolean specialTagged,
            boolean knownVanillaSpecial,
            ShearingSource source,
            SmartDropsConfig config,
            boolean sourceEnabled,
            int configuredDefault,
            Integer exactOverride,
            int appliedMultiplier,
            ShearingRuleTrace.RuleSource selectedRule,
            String reason
    ) {
        return new ShearingRuleTrace(
                entityId,
                classification,
                standardTagged,
                specialTagged,
                knownVanillaSpecial,
                source,
                config.enabled,
                sourceEnabled,
                config.inheritDefaultShearingMultiplier,
                configuredDefault,
                exactOverride,
                appliedMultiplier,
                selectedRule,
                reason);
    }

    private static int bounded(int value, int maximum) {
        return Math.max(0, Math.min(maximum, value));
    }
}
