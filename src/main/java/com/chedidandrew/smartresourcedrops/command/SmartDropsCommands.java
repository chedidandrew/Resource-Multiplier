package com.chedidandrew.smartresourcedrops.command;


import net.minecraft.commands.arguments.IdentifierArgument;
import com.chedidandrew.smartresourcedrops.config.ConfigManager;
import com.chedidandrew.smartresourcedrops.config.ConfigValidationReport;
import com.chedidandrew.smartresourcedrops.config.ConfigValidator;
import com.chedidandrew.smartresourcedrops.config.LiveConfigRegistryView;
import com.chedidandrew.smartresourcedrops.config.SmartDropsConfig;
import com.chedidandrew.smartresourcedrops.core.Category;
import com.chedidandrew.smartresourcedrops.core.DropSource;
import com.chedidandrew.smartresourcedrops.core.MultiplierResolver;
import com.chedidandrew.smartresourcedrops.core.RuleResolutionTrace;
import com.chedidandrew.smartresourcedrops.core.SmartDropTags;
import com.chedidandrew.smartresourcedrops.core.SmartDropsStats;
import com.chedidandrew.smartresourcedrops.core.entity.EntityMultiplierResolver;
import com.chedidandrew.smartresourcedrops.core.entity.EntityRuleTrace;
import com.chedidandrew.smartresourcedrops.core.shearing.ShearingRuleResolver;
import com.chedidandrew.smartresourcedrops.core.shearing.ShearingRuleTrace;
import com.chedidandrew.smartresourcedrops.core.shearing.ShearingSource;
import com.chedidandrew.smartresourcedrops.core.shearing.ShearingTags;
import com.chedidandrew.smartresourcedrops.network.SmartDropsNetworking;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class SmartDropsCommands {
    private static final String STANDARD_SHEARING_TAG =
            ShearingTags.STANDARD_RESOURCES.location().toString();
    private static final String SPECIAL_SHEARING_TAG =
            ShearingTags.SPECIAL.location().toString();

    private SmartDropsCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(buildRoot()));
    }

    static LiteralArgumentBuilder<CommandSourceStack> buildRoot() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("smartdrops")
                .executes(context -> showStatus(context.getSource()))
                .then(Commands.literal("status")
                        .executes(context -> showStatus(context.getSource())))
                .then(Commands.literal("gui")
                        .executes(context -> message(
                                context.getSource(),
                                "Open the client screen with /smartdropsgui.")))
                .then(Commands.literal("personal")
                        .then(Commands.argument("multiplier", IntegerArgumentType.integer(0, 64))
                                .executes(context -> setPersonalMultiplier(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "multiplier"))))
                        .then(Commands.literal("inherit")
                                .executes(context -> clearPersonalMultiplier(context.getSource()))))
                .then(Commands.literal("shearing")
                        .executes(context -> showShearingStatus(context.getSource()))
                        .then(Commands.literal("status")
                                .executes(context -> showShearingStatus(context.getSource()))))
                .then(Commands.literal("validate")
                        .requires(SmartDropsCommands::isAdmin)
                        .executes(context -> validate(context.getSource(), false))
                        .then(Commands.literal("verbose")
                                .executes(context -> validate(context.getSource(), true))))
                .then(Commands.literal("inspect")
                        .executes(context -> inspect(context.getSource(), false))
                        .then(Commands.literal("verbose")
                                .executes(context -> inspect(context.getSource(), true)))
                        .then(Commands.literal("entity")
                                .executes(context -> inspectEntity(context.getSource(), false))
                                .then(Commands.literal("verbose")
                                        .executes(context -> inspectEntity(context.getSource(), true)))));

        LiteralArgumentBuilder<CommandSourceStack> admin = Commands.literal("admin")
                .requires(SmartDropsCommands::isAdmin)
                .then(toggleCommand("enabled", value -> ConfigManager.update(config -> config.enabled = value)))
                .then(toggleCommand("protection", value -> ConfigManager.update(config -> config.smartPlacementProtection = value)))
                .then(toggleCommand("xp", value -> ConfigManager.update(config -> config.multiplyExperience = value)))
                .then(toggleCommand("player-mining", value -> ConfigManager.update(config -> config.playerMining = value)))
                .then(toggleCommand("explosions", value -> ConfigManager.update(config -> config.explosions = value)))
                .then(toggleCommand("automation", value -> ConfigManager.update(config -> config.automatedMining = value)))
                .then(toggleCommand("blockentities", value -> ConfigManager.update(config -> config.protectBlockEntities = value)))
                .then(toggleCommand("piston-safe", value -> ConfigManager.update(config -> config.conservativePistonProtection = value)))
                .then(toggleCommand("player-overrides", value -> ConfigManager.update(config -> config.allowPlayerOverrides = value)))
                .then(toggleCommand("stats", value -> ConfigManager.update(config -> config.statisticsEnabled = value)))
                .then(Commands.literal("global")
                        .then(Commands.argument("multiplier", IntegerArgumentType.integer(0, 64))
                                .executes(context -> setGlobal(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "multiplier")))))
                .then(Commands.literal("xp-multiplier")
                        .then(Commands.argument("multiplier", IntegerArgumentType.integer(1, 64))
                                .executes(context -> setExperience(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "multiplier")))))
                .then(Commands.literal("maximum")
                        .then(Commands.argument("multiplier", IntegerArgumentType.integer(1, 64))
                                .executes(context -> setMaximum(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "multiplier")))))
                .then(Commands.literal("player-maximum")
                        .then(Commands.argument("multiplier", IntegerArgumentType.integer(1, 64))
                                .executes(context -> setPlayerMaximum(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "multiplier")))))
                .then(Commands.literal("shearing")
                        .then(toggleCommand("manual", value -> ConfigManager.update(
                                config -> config.manualShearingDropsEnabled = value)))
                        .then(toggleCommand("automated", value -> ConfigManager.update(
                                config -> config.automatedShearingDropsEnabled = value)))
                        .then(Commands.literal("multiplier")
                                .then(Commands.literal("inherit")
                                        .executes(context -> inheritShearingDefault(context.getSource())))
                                .then(Commands.argument("multiplier", IntegerArgumentType.integer(0, 64))
                                        .executes(context -> setShearingDefault(
                                                context.getSource(),
                                                IntegerArgumentType.getInteger(context, "multiplier")))))
                        .then(Commands.literal("entity")
                                .then(Commands.argument("entity_id", IdentifierArgument.id())
                                        .then(Commands.literal("inherit")
                                                .executes(context -> clearShearingEntityMultiplier(
                                                        context.getSource(),
                                                        IdentifierArgument.getId(
                                                                context,
                                                                "entity_id").toString())))
                                        .then(Commands.argument(
                                                        "multiplier",
                                                        IntegerArgumentType.integer(0, 64))
                                                .executes(context -> setShearingEntityMultiplier(
                                                        context.getSource(),
                                                        IdentifierArgument.getId(
                                                                context,
                                                                "entity_id").toString(),
                                                                IntegerArgumentType.getInteger(
                                                                        context,
                                                                "multiplier")))))))
                .then(Commands.literal("source")
                        .then(enumLiteral("natural-only", () -> ConfigManager.update(
                                config -> config.sourceMode = SmartDropsConfig.SourceMode.NATURAL_ONLY)))
                        .then(enumLiteral("all", () -> ConfigManager.update(
                                config -> config.sourceMode = SmartDropsConfig.SourceMode.ALL)))
                        .then(enumLiteral("player-placed-only", () -> ConfigManager.update(
                                config -> config.sourceMode = SmartDropsConfig.SourceMode.PLAYER_PLACED_ONLY))))
                .then(Commands.literal("filter-mode")
                        .then(enumLiteral("blacklist", () -> ConfigManager.update(
                                config -> config.filterMode = SmartDropsConfig.FilterMode.BLACKLIST)))
                        .then(enumLiteral("whitelist", () -> ConfigManager.update(
                                config -> config.filterMode = SmartDropsConfig.FilterMode.WHITELIST))))
                .then(Commands.literal("block")
                        .then(Commands.argument("block_id", IdentifierArgument.id())
                                .then(Commands.argument("multiplier", IntegerArgumentType.integer(0, 64))
                                        .executes(context -> setMapMultiplier(
                                                context.getSource(),
                                                IdentifierArgument.getId(context, "block_id").toString(),
                                                IntegerArgumentType.getInteger(context, "multiplier"),
                                                RuleMap.BLOCK)))
                                .then(Commands.literal("inherit")
                                        .executes(context -> clearMapMultiplier(
                                                context.getSource(),
                                                IdentifierArgument.getId(context, "block_id").toString(),
                                                RuleMap.BLOCK)))))
                .then(Commands.literal("category")
                        .then(Commands.argument("category", StringArgumentType.word())
                                .then(Commands.argument("multiplier", IntegerArgumentType.integer(0, 64))
                                        .executes(context -> setMapMultiplier(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "category"),
                                                IntegerArgumentType.getInteger(context, "multiplier"),
                                                RuleMap.CATEGORY)))
                                .then(Commands.literal("inherit")
                                        .executes(context -> clearMapMultiplier(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "category"),
                                                RuleMap.CATEGORY)))))
                .then(Commands.literal("dimension")
                        .then(Commands.argument("dimension_id", IdentifierArgument.id())
                                .then(Commands.argument("multiplier", IntegerArgumentType.integer(0, 64))
                                        .executes(context -> setMapMultiplier(
                                                context.getSource(),
                                                IdentifierArgument.getId(context, "dimension_id").toString(),
                                                IntegerArgumentType.getInteger(context, "multiplier"),
                                                RuleMap.DIMENSION)))
                                .then(Commands.literal("inherit")
                                        .executes(context -> clearMapMultiplier(
                                                context.getSource(),
                                                IdentifierArgument.getId(context, "dimension_id").toString(),
                                                RuleMap.DIMENSION)))))
                .then(buildListCommand("blacklist", ListKind.BLACKLIST))
                .then(buildListCommand("whitelist", ListKind.WHITELIST))
                .then(buildListCommand("tag-blacklist", ListKind.TAG_BLACKLIST))
                .then(buildListCommand("tag-whitelist", ListKind.TAG_WHITELIST))
                .then(buildListCommand("blockentity-allowlist", ListKind.BLOCK_ENTITY_ALLOWLIST))
                .then(Commands.literal("preset")
                        .then(presetLiteral("vanilla-plus", SmartDropsConfig.Preset.VANILLA_PLUS))
                        .then(presetLiteral("faster-survival", SmartDropsConfig.Preset.FASTER_SURVIVAL))
                        .then(presetLiteral("fast-progression", SmartDropsConfig.Preset.FAST_PROGRESSION)))
                .then(Commands.literal("reload")
                        .executes(context -> {
                            if (!ConfigManager.load()) {
                                context.getSource().sendFailure(Component.literal(
                                        "Configuration could not be safely reloaded. Check the server log."));
                                return 0;
                            }
                            return message(context.getSource(), "Configuration reloaded from disk.");
                        }))
                .then(Commands.literal("reset")
                        .executes(context -> {
                            if (!ConfigManager.reset()) {
                                return configUpdateFailure(context.getSource());
                            }
                            SmartDropsNetworking.afterAuthoritativeReset(context.getSource().getServer());
                            return message(context.getSource(), "Configuration reset to safe defaults.");
                        }))
                .then(Commands.literal("statistics")
                        .then(Commands.literal("show")
                                .executes(context -> showStatistics(context.getSource())))
                        .then(Commands.literal("reset")
                                .executes(context -> {
                                    SmartDropsStats.reset();
                                    return message(context.getSource(), "Statistics reset.");
                                })));

        return root.then(admin);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> toggleCommand(
            String name,
            ToggleSetter setter
    ) {
        return Commands.literal(name)
                .then(Commands.literal("on").executes(context -> {
                    if (!setter.set(true)) {
                        return configUpdateFailure(context.getSource());
                    }
                    return message(context.getSource(), name + " is now ON.");
                }))
                .then(Commands.literal("off").executes(context -> {
                    if (!setter.set(false)) {
                        return configUpdateFailure(context.getSource());
                    }
                    return message(context.getSource(), name + " is now OFF.");
                }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> enumLiteral(String name, ConfigAction action) {
        return Commands.literal(name).executes(context -> {
            if (!action.run()) {
                return configUpdateFailure(context.getSource());
            }
            return message(context.getSource(), "Set to " + name + ".");
        });
    }

    private static LiteralArgumentBuilder<CommandSourceStack> presetLiteral(
            String name,
            SmartDropsConfig.Preset preset
    ) {
        return Commands.literal(name).executes(context -> {
            if (!ConfigManager.update(config -> config.applyPreset(preset))) {
                return configUpdateFailure(context.getSource());
            }
            return message(context.getSource(), "Applied preset: " + name + ".");
        });
    }

    static LiteralArgumentBuilder<CommandSourceStack> buildListCommand(String name, ListKind kind) {
        return Commands.literal(name)
                .then(Commands.literal("add")
                        .then(Commands.argument("id", StringArgumentType.greedyString())
                                .executes(context -> changeList(
                                        context.getSource(),
                                        kind,
                                        StringArgumentType.getString(context, "id"),
                                        true))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("id", StringArgumentType.greedyString())
                                .executes(context -> changeList(
                                        context.getSource(),
                                        kind,
                                        StringArgumentType.getString(context, "id"),
                                        false))));
    }

    private static int setGlobal(CommandSourceStack source, int multiplier) {
        if (!ConfigManager.update(config -> config.globalMultiplier = multiplier)) {
            return configUpdateFailure(source);
        }
        return message(source, "Global multiplier set to " + ConfigManager.get().globalMultiplier + "x.");
    }

    private static int setExperience(CommandSourceStack source, int multiplier) {
        if (!ConfigManager.update(config -> config.experienceMultiplier = multiplier)) {
            return configUpdateFailure(source);
        }
        return message(
                source,
                "Experience multiplier set to " + ConfigManager.get().experienceMultiplier + "x.");
    }

    private static int setMaximum(CommandSourceStack source, int multiplier) {
        if (!ConfigManager.update(config -> config.maximumMultiplier = multiplier)) {
            return configUpdateFailure(source);
        }
        return message(source, "Maximum multiplier set to " + ConfigManager.get().maximumMultiplier + "x.");
    }

    private static int setPlayerMaximum(CommandSourceStack source, int multiplier) {
        if (!ConfigManager.update(config -> config.maxPlayerMultiplier = multiplier)) {
            return configUpdateFailure(source);
        }
        return message(
                source,
                "Maximum personal multiplier set to " + ConfigManager.get().maxPlayerMultiplier + "x.");
    }

    private static int inheritShearingDefault(final CommandSourceStack source) {
        if (!ConfigManager.update(config -> config.inheritDefaultShearingMultiplier = true)) {
            return configUpdateFailure(source);
        }
        final SmartDropsConfig config = ConfigManager.get();
        return message(
                source,
                "Default shearing multiplier now inherits the global "
                        + config.globalMultiplier + "x rule.");
    }

    private static int setShearingDefault(
            final CommandSourceStack source,
            final int multiplier
    ) {
        if (!ConfigManager.update(config -> {
            config.inheritDefaultShearingMultiplier = false;
            config.defaultShearingMultiplier = multiplier;
        })) {
            return configUpdateFailure(source);
        }
        return message(
                source,
                "Default shearing multiplier set to "
                        + ConfigManager.get().defaultShearingMultiplier + "x.");
    }

    private static int setShearingEntityMultiplier(
            final CommandSourceStack source,
            final String entityId,
            final int multiplier
    ) {
        final LiveConfigRegistryView registries = LiveConfigRegistryView.from(source);
        if (!registries.entityExists(entityId)) {
            source.sendFailure(Component.literal("Unknown entity type: " + entityId));
            return 0;
        }
        if (ShearingTags.isKnownVanillaSpecial(entityId)
                || registries.entityIdsInTag(SPECIAL_SHEARING_TAG).contains(entityId)) {
            source.sendFailure(Component.literal(
                    entityId + " is a special shearing transformation fixed at vanilla 1x."));
            return 0;
        }
        if (!registries.entityIdsInTag(STANDARD_SHEARING_TAG).contains(entityId)) {
            source.sendFailure(Component.literal(
                    entityId + " is not certified by " + STANDARD_SHEARING_TAG
                            + "; unknown shearables remain vanilla 1x."));
            return 0;
        }
        if (!ConfigManager.update(config -> config.shearingEntityMultipliers.put(entityId, multiplier))) {
            return configUpdateFailure(source);
        }
        final Integer stored = ConfigManager.get().shearingEntityMultipliers.get(entityId);
        if (stored == null) {
            source.sendFailure(Component.literal("Shearing rule limit reached; no setting was changed."));
            return 0;
        }
        return message(source, "Shearing entity " + entityId + " set to " + stored + "x.");
    }

    private static int clearShearingEntityMultiplier(
            final CommandSourceStack source,
            final String entityId
    ) {
        if (!ConfigManager.update(config -> config.shearingEntityMultipliers.remove(entityId))) {
            return configUpdateFailure(source);
        }
        final LiveConfigRegistryView registries = LiveConfigRegistryView.from(source);
        if (ShearingTags.isKnownVanillaSpecial(entityId)
                || registries.entityIdsInTag(SPECIAL_SHEARING_TAG).contains(entityId)) {
            return message(
                    source,
                    "Cleared the shearing override for " + entityId
                            + "; this special transformation remains fixed at vanilla 1x.");
        }
        if (registries.entityExists(entityId)
                && registries.entityIdsInTag(STANDARD_SHEARING_TAG).contains(entityId)) {
            return message(
                    source,
                    "Cleared the shearing override for " + entityId
                            + "; it now inherits the default shearing rule.");
        }
        return message(
                source,
                "Cleared the shearing override for " + entityId
                        + "; it remains vanilla 1x unless certified by "
                        + STANDARD_SHEARING_TAG + ".");
    }

    private static int setMapMultiplier(
            CommandSourceStack source,
            String rawKey,
            int multiplier,
            RuleMap map
    ) {
        String key = normalizeRuleKey(rawKey, map);
        if (key == null) {
            source.sendFailure(Component.literal("Invalid " + map.label + ": " + rawKey));
            return 0;
        }
        if (!ConfigManager.update(config -> map.map(config).put(key, multiplier))) {
            return configUpdateFailure(source);
        }
        final Integer stored = map.map(ConfigManager.get()).get(key);
        if (stored == null) {
            source.sendFailure(Component.literal("Rule limit reached; no setting was changed."));
            return 0;
        }
        return message(source, map.label + " " + key + " set to " + stored + "x.");
    }

    private static int clearMapMultiplier(CommandSourceStack source, String rawKey, RuleMap map) {
        String key = normalizeRuleKey(rawKey, map);
        if (key == null) {
            source.sendFailure(Component.literal("Invalid " + map.label + ": " + rawKey));
            return 0;
        }
        if (!ConfigManager.update(config -> map.map(config).remove(key))) {
            return configUpdateFailure(source);
        }
        return message(source, map.label + " " + key + " now inherits its parent rule.");
    }

    private static String normalizeRuleKey(String raw, RuleMap map) {
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > SmartDropsConfig.MAX_RULE_KEY_LENGTH) {
            return null;
        }
        if (map == RuleMap.CATEGORY) {
            return Category.parse(normalized).map(Category::key).orElse(null);
        }
        Identifier identifier = Identifier.tryParse(normalized);
        return identifier == null ? null : identifier.toString();
    }

    private static int changeList(CommandSourceStack source, ListKind kind, String rawValue, boolean add) {
        String normalized = normalizeListValue(rawValue, kind);
        if (normalized == null) {
            source.sendFailure(Component.literal("Invalid identifier: " + rawValue));
            return 0;
        }

        String value = normalized;
        if (!ConfigManager.update(config -> {
            Set<String> values = kind.values(config);
            if (add) {
                values.add(value);
            } else {
                values.remove(value);
            }
        })) {
            return configUpdateFailure(source);
        }
        if (kind.values(ConfigManager.get()).contains(value) != add) {
            source.sendFailure(Component.literal("Rule limit reached; no setting was changed."));
            return 0;
        }
        return message(source, (add ? "Added " : "Removed ") + value + (add ? " to " : " from ") + kind.label + ".");
    }

    static String normalizeListValue(String rawValue, ListKind kind) {
        if (rawValue == null) {
            return null;
        }
        String normalized = kind.tagList
                ? SmartDropTags.normalizeTagId(rawValue)
                : rawValue.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > SmartDropsConfig.MAX_RULE_KEY_LENGTH) {
            return null;
        }
        Identifier identifier = Identifier.tryParse(normalized);
        return identifier == null ? null : identifier.toString();
    }

    private static int setPersonalMultiplier(CommandSourceStack source, int multiplier) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Only a player can use a personal multiplier."));
            return 0;
        }
        SmartDropsConfig config = ConfigManager.get();
        if (!config.allowPlayerOverrides && !isAdmin(source)) {
            source.sendFailure(Component.literal("Player overrides are disabled by the server."));
            return 0;
        }
        int capped = Math.min(multiplier, config.maxPlayerMultiplier);
        String playerId = player.getUUID().toString();
        if (!ConfigManager.update(copy -> copy.playerMultipliers.put(playerId, capped))) {
            return configUpdateFailure(source);
        }
        Integer stored = ConfigManager.get().playerMultipliers.get(playerId);
        if (stored == null) {
            source.sendFailure(Component.literal("Rule limit reached; no setting was changed."));
            return 0;
        }
        return message(source, "Your personal multiplier is now " + stored + "x.");
    }

    private static int clearPersonalMultiplier(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Only a player can clear a personal multiplier."));
            return 0;
        }
        if (!ConfigManager.update(config -> config.playerMultipliers.remove(player.getUUID().toString()))) {
            return configUpdateFailure(source);
        }
        return message(source, "Your personal multiplier now inherits server rules.");
    }

    private static int showStatus(CommandSourceStack source) {
        SmartDropsConfig config = ConfigManager.get();
        String status = "Smart Resource Multiplier: "
                + (config.enabled ? "ON" : "OFF")
                + " | global=" + config.globalMultiplier + "x"
                + " | protection=" + onOff(config.smartPlacementProtection)
                + " | source=" + config.sourceMode
                + " | filter=" + config.filterMode
                + " | XP=" + (config.multiplyExperience ? config.experienceMultiplier + "x" : "OFF")
                + " | explosions=" + onOff(config.explosions)
                + " | automation=" + onOff(config.automatedMining)
                + " | entityDrops=" + onOff(config.entityDropsEnabled)
                + " | entityKill=" + config.entityKillRequirement
                + " | entityDefault=" + (config.inheritDefaultEntityMultiplier
                        ? "INHERIT(" + config.globalMultiplier + "x)"
                        : config.defaultEntityMultiplier + "x")
                + " | mobXP=" + (config.multiplyMobExperience ? config.mobExperienceMultiplier + "x" : "OFF")
                + " | bossDrops=" + onOff(config.bossDropsEnabled)
                + " | manualShearing=" + onOff(config.manualShearingDropsEnabled)
                + " | automatedShearing=" + onOff(config.automatedShearingDropsEnabled)
                + " | shearingDefault=" + (config.inheritDefaultShearingMultiplier
                        ? "INHERIT(" + config.globalMultiplier + "x)"
                        : config.defaultShearingMultiplier + "x");
        return message(source, status);
    }

    private static int showShearingStatus(final CommandSourceStack source) {
        final SmartDropsConfig config = ConfigManager.get();
        final String configuredDefault = config.inheritDefaultShearingMultiplier
                ? "INHERIT(" + config.globalMultiplier + "x)"
                : config.defaultShearingMultiplier + "x";
        return message(
                source,
                "Smart Resource Multiplier shearing: master=" + onOff(config.enabled)
                        + " | manual=" + onOff(config.manualShearingDropsEnabled)
                        + " | automated=" + onOff(config.automatedShearingDropsEnabled)
                        + " | default=" + configuredDefault
                        + " | overrides=" + config.shearingEntityMultipliers.size()
                        + " | unknown/special=vanilla 1x"
                        + " | outputBudget=1024 items/256 source or materialized stacks");
    }

    private static int showStatistics(CommandSourceStack source) {
        SmartDropsStats.Snapshot stats = SmartDropsStats.snapshot();
        return message(source,
                "Stats: evaluated=" + stats.blocksEvaluated()
                        + ", multiplied=" + stats.blocksMultiplied()
                        + ", vanillaItems=" + stats.vanillaItems()
                        + ", bonusItems=" + stats.bonusItems()
                        + ", suppressedItems=" + stats.suppressedItems()
                        + ", bonusXP=" + stats.bonusExperience()
                        + ", blockBudgetFallbacks=" + stats.blockBudgetFallbacks());
    }

    static int validate(final CommandSourceStack source, final boolean verbose) {
        final ConfigValidationReport report = ConfigValidator.validate(
                ConfigManager.validationSnapshot(),
                LiveConfigRegistryView.from(source));
        for (Component line : ConfigValidationFormatter.format(report, verbose)) {
            source.sendSuccess(() -> line, false);
        }
        return 1;
    }

    static int inspect(CommandSourceStack source, boolean verbose) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("A player target is required for block inspection."));
            source.sendSuccess(() -> Component.literal(
                    "Run /smartdrops inspect as a player while looking at a block."), false);
            return 0;
        }

        ServerLevel level = player.level();
        HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);
        if (player.level() != level) {
            source.sendFailure(Component.literal("Your dimension changed during inspection."));
            return 0;
        }
        if (hit.getType() != HitResult.Type.BLOCK) {
            return noTarget(source);
        }

        BlockPos pos = ((BlockHitResult) hit).getBlockPos().immutable();
        if (!level.isLoaded(pos)) {
            source.sendFailure(Component.literal("The targeted block is no longer loaded."));
            return 0;
        }

        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return noTarget(source);
        }
        BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        RuleResolutionTrace trace = MultiplierResolver.inspect(
                level,
                pos,
                state,
                blockEntity,
                DropSource.PLAYER,
                player);

        if (player.level() != level || !level.isLoaded(pos) || !level.getBlockState(pos).equals(state)) {
            source.sendFailure(Component.literal("The targeted block changed during inspection."));
            source.sendSuccess(() -> Component.literal(
                    "Look at the block and run /smartdrops inspect again."), false);
            return 0;
        }

        for (Component line : BlockInspectionFormatter.format(state, pos, trace, verbose)) {
            source.sendSuccess(() -> line, false);
        }
        return 1;
    }

    static int inspectEntity(final CommandSourceStack source, final boolean verbose) {
        final ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("A player target is required for entity inspection."));
            source.sendSuccess(() -> Component.literal(
                    "Run /smartdrops inspect entity as a player while looking at a living entity."), false);
            return 0;
        }

        final ServerLevel level = player.level();
        final double range = player.entityInteractionRange();
        final HitResult hit = ProjectileUtil.getHitResultOnViewVector(
                player,
                EntitySelector.CAN_BE_PICKED
                        .and(EntitySelector.NO_SPECTATORS)
                        .and(entity -> entity instanceof LivingEntity && !(entity instanceof Player)),
                range);
        if (player.level() != level) {
            source.sendFailure(Component.literal("Your dimension changed during entity inspection."));
            return 0;
        }
        if (!(hit instanceof EntityHitResult entityHit)
                || !(entityHit.getEntity() instanceof LivingEntity target)
                || target instanceof Player) {
            return noEntityTarget(source);
        }
        if (target.level() != level
                || target.isRemoved()
                || !player.isWithinEntityInteractionRange(target, 0.0)) {
            source.sendFailure(Component.literal("The targeted entity moved or disappeared during inspection."));
            source.sendSuccess(() -> Component.literal(
                    "Look at the entity and run /smartdrops inspect entity again."), false);
            return 0;
        }

        final int entityId = target.getId();
        final java.util.UUID entityUuid = target.getUUID();
        final EntityRuleTrace trace = EntityMultiplierResolver.inspect(level, target, player);
        final SmartDropsConfig shearingConfig = ConfigManager.snapshot();
        final boolean shearable = target instanceof Shearable;
        final ShearingRuleTrace manualShearing = ShearingRuleResolver.trace(
                shearingConfig,
                target.getType(),
                ShearingSource.MANUAL_PLAYER);
        final ShearingRuleTrace automatedShearing = ShearingRuleResolver.trace(
                shearingConfig,
                target.getType(),
                ShearingSource.VANILLA_DISPENSER);
        if (player.level() != level
                || target.level() != level
                || target.isRemoved()
                || target.getId() != entityId
                || !target.getUUID().equals(entityUuid)
                || !player.isWithinEntityInteractionRange(target, 0.0)) {
            source.sendFailure(Component.literal("The targeted entity changed during inspection."));
            source.sendSuccess(() -> Component.literal(
                    "Look at the entity and run /smartdrops inspect entity again."), false);
            return 0;
        }

        for (Component line : EntityInspectionFormatter.format(
                target,
                trace,
                shearable,
                manualShearing,
                automatedShearing,
                verbose)) {
            source.sendSuccess(() -> line, false);
        }
        return 1;
    }

    private static int noTarget(CommandSourceStack source) {
        source.sendFailure(Component.literal("No block is currently targeted."));
        source.sendSuccess(() -> Component.literal(
                "Look at a block within interaction range and run /smartdrops inspect again."), false);
        return 0;
    }

    private static int noEntityTarget(final CommandSourceStack source) {
        source.sendFailure(Component.literal("No living entity is currently targeted."));
        source.sendSuccess(() -> Component.literal(
                "Look at a non-player living entity within interaction range and run "
                        + "/smartdrops inspect entity again."), false);
        return 0;
    }

    private static boolean isAdmin(CommandSourceStack source) {
        return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    private static int message(CommandSourceStack source, String text) {
        source.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static int configUpdateFailure(CommandSourceStack source) {
        source.sendFailure(Component.literal(
                "Configuration was not changed because it could not be persisted. Check the server log."));
        return 0;
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }

    @FunctionalInterface
    private interface ToggleSetter {
        boolean set(boolean value);
    }

    @FunctionalInterface
    private interface ConfigAction {
        boolean run();
    }

    private enum RuleMap {
        BLOCK("block") {
            @Override
            Map<String, Integer> map(SmartDropsConfig config) {
                return config.blockMultipliers;
            }
        },
        CATEGORY("category") {
            @Override
            Map<String, Integer> map(SmartDropsConfig config) {
                return config.categoryMultipliers;
            }
        },
        DIMENSION("dimension") {
            @Override
            Map<String, Integer> map(SmartDropsConfig config) {
                return config.dimensionMultipliers;
            }
        };

        private final String label;

        RuleMap(String label) {
            this.label = label;
        }

        abstract Map<String, Integer> map(SmartDropsConfig config);
    }

    enum ListKind {
        BLACKLIST("blacklist", false) {
            @Override
            Set<String> values(SmartDropsConfig config) {
                return config.blacklist;
            }
        },
        WHITELIST("whitelist", false) {
            @Override
            Set<String> values(SmartDropsConfig config) {
                return config.whitelist;
            }
        },
        TAG_BLACKLIST("tag blacklist", true) {
            @Override
            Set<String> values(SmartDropsConfig config) {
                return config.tagBlacklist;
            }
        },
        TAG_WHITELIST("tag whitelist", true) {
            @Override
            Set<String> values(SmartDropsConfig config) {
                return config.tagWhitelist;
            }
        },
        BLOCK_ENTITY_ALLOWLIST("block entity allowlist", false) {
            @Override
            Set<String> values(SmartDropsConfig config) {
                return config.blockEntityAllowlist;
            }
        };

        private final String label;
        private final boolean tagList;

        ListKind(String label, boolean tagList) {
            this.label = label;
            this.tagList = tagList;
        }

        abstract Set<String> values(SmartDropsConfig config);
    }
}
