package com.chedidandrew.smartresourcedrops.command;

import com.chedidandrew.smartresourcedrops.config.SmartDropsConfig;
import com.chedidandrew.smartresourcedrops.core.entity.EntityCategory;
import com.chedidandrew.smartresourcedrops.core.entity.EntityClassification;
import com.chedidandrew.smartresourcedrops.core.entity.EntityRuleTrace;
import com.chedidandrew.smartresourcedrops.core.shearing.ShearingClassification;
import com.chedidandrew.smartresourcedrops.core.shearing.ShearingOutputBudget;
import com.chedidandrew.smartresourcedrops.core.shearing.ShearingRuleTrace;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;

/** Server-safe, read-only rendering for an entity rule-resolution trace. */
final class EntityInspectionFormatter {
    private static final int MAX_VISIBLE_ID = 96;
    private static final int MAX_VISIBLE_TAGS = 8;
    private static final int MAX_VISIBLE_TAG_LENGTH = 64;

    private EntityInspectionFormatter() {
    }

    static List<Component> format(
            final LivingEntity entity,
            final EntityRuleTrace trace,
            final boolean shearable,
            final ShearingRuleTrace manualShearing,
            final ShearingRuleTrace automatedShearing,
            final boolean verbose
    ) {
        return verbose
                ? verbose(entity, trace, shearable, manualShearing, automatedShearing)
                : compact(entity, trace, shearable, manualShearing, automatedShearing);
    }

    private static List<Component> compact(
            final LivingEntity entity,
            final EntityRuleTrace trace,
            final boolean shearable,
            final ShearingRuleTrace manualShearing,
            final ShearingRuleTrace automatedShearing
    ) {
        final List<Component> lines = new ArrayList<>();
        lines.add(title());
        lines.add(valueLine("Entity", entity.getName().copy().withStyle(ChatFormatting.WHITE)));
        lines.add(valueLine("ID", copyableId(trace.entityId())));
        lines.add(valueLine("Classification", plain(categoryName(trace.selectedCategory()))));
        lines.add(valueLine("Boss", yesNo(trace.boss())));
        lines.add(valueLine("Standard death loot", itemResult(trace)));
        lines.add(valueLine("Kill requirement", plain(killRequirement(trace.killRequirement()))));
        lines.add(valueLine("Invoking player qualifies", yesNo(trace.invokingPlayerWouldQualify())));
        lines.add(valueLine("Entity filter", status(
                trace.filterEligible() ? "Allowed" : "Blocked",
                trace.filterEligible())));
        lines.add(valueLine("Configured multiplier", multiplier(trace.configuredMultiplier())));
        lines.add(valueLine("Resolved from", plain(ruleSource(trace))));
        lines.add(valueLine("Effective multiplier", effectiveItemMultiplier(trace)));
        lines.add(valueLine("Mob XP", experienceResult(trace)));
        if (shearingRelevant(shearable, manualShearing)) {
            lines.add(valueLine("Shearable", yesNo(shearable)));
            lines.add(valueLine(
                    "Shearing classification",
                    plain(shearingClassification(manualShearing))));
            lines.add(valueLine(
                    "Effective manual shearing",
                    shearingMultiplier(manualShearing)));
            lines.add(valueLine(
                    "Effective automated shearing",
                    shearingMultiplier(automatedShearing)));
        }
        return List.copyOf(lines);
    }

    private static List<Component> verbose(
            final LivingEntity entity,
            final EntityRuleTrace trace,
            final boolean shearable,
            final ShearingRuleTrace manualShearing,
            final ShearingRuleTrace automatedShearing
    ) {
        final List<Component> lines = new ArrayList<>();
        lines.add(title());

        lines.add(section("Entity"));
        lines.add(indentedValue("Name", entity.getName().copy().withStyle(ChatFormatting.WHITE)));
        lines.add(indentedValue("ID", copyableId(trace.entityId())));
        lines.add(indentedValue("Permanent exclusion", status(
                trace.permanentlyExcluded() ? trace.permanentExclusionReason() : "No",
                !trace.permanentlyExcluded())));

        lines.add(section("Classification"));
        lines.add(indentedValue("Matched categories", plain(categoryList(trace.matchedCategories()))));
        lines.add(indentedValue("Selected category", plain(categoryName(trace.selectedCategory()))));
        lines.add(indentedValue("Classification reason", plain(trace.classificationReason())));
        lines.add(indentedValue("Miscellaneous fallback", yesNo(trace.miscellaneousFallback())));
        lines.add(indentedValue("Boss", yesNo(trace.boss())));
        lines.add(indentedValue("Boss classification source", plain(sourceList(trace.bossSources()))));
        lines.add(indentedValue("Runtime entity-type tags", plain(tagList(trace.runtimeTags()))));

        lines.add(section("Filtering and attribution"));
        lines.add(indentedValue("Filter mode", plain(titleCase(trace.filterMode().name()))));
        lines.add(indentedValue("Exact blacklist match", yesNo(trace.exactBlacklisted())));
        lines.add(indentedValue("Blacklist tag matches", plain(tagList(trace.matchingBlacklistTags()))));
        lines.add(indentedValue("Exact whitelist match", yesNo(trace.exactWhitelisted())));
        lines.add(indentedValue("Whitelist tag matches", plain(tagList(trace.matchingWhitelistTags()))));
        lines.add(indentedValue("Filter result", status(
                trace.filterEligible() ? "Allowed" : "Blocked",
                trace.filterEligible())));
        lines.add(indentedValue("Kill requirement", plain(killRequirement(trace.killRequirement()))));
        lines.add(indentedValue("Inspection attribution", plain(titleCase(trace.attribution().name()))));
        lines.add(indentedValue("Hypothetical vanilla player-kill credit", yesNo(trace.vanillaPlayerKilled())));
        lines.add(indentedValue("Invoking player would qualify", yesNo(trace.invokingPlayerWouldQualify())));

        lines.add(section("Item multiplier resolution"));
        lines.add(indentedValue("Entity override", optionalMultiplier(trace.entityOverride())));
        lines.add(indentedValue("Category override", optionalMultiplier(trace.categoryOverride())));
        lines.add(indentedValue(
                "Default entity multiplier",
                trace.inheritDefaultEntityMultiplier()
                        ? Component.literal("Inherit global").withStyle(ChatFormatting.YELLOW)
                        : multiplier(trace.entityDefaultMultiplier())));
        lines.add(indentedValue("Global multiplier", multiplier(trace.globalMultiplier())));
        lines.add(indentedValue("Maximum multiplier", multiplier(trace.maximumMultiplier())));
        lines.add(indentedValue("Selected rule", plain(ruleSource(trace))));
        lines.add(indentedValue("Configured multiplier", multiplier(trace.configuredMultiplier())));
        lines.add(indentedValue("Boss drops", enabled(trace.bossDropsEnabled())));
        lines.add(indentedValue("Standard death loot", itemResult(trace)));
        lines.add(indentedValue("Effective multiplier", effectiveItemMultiplier(trace)));
        lines.add(indentedValue("Item rule reason", plain(titleCase(trace.itemReason().name()))));

        lines.add(section("Mob experience"));
        lines.add(indentedValue("Mob XP multiplication", enabled(trace.multiplyMobExperience())));
        lines.add(indentedValue("Mob XP multiplier", multiplier(trace.mobExperienceMultiplier())));
        lines.add(indentedValue("Boss XP multiplication", enabled(trace.multiplyBossExperience())));
        lines.add(indentedValue("Effective mob XP", experienceResult(trace)));
        lines.add(indentedValue("XP rule reason", plain(titleCase(trace.experienceReason().name()))));

        if (shearingRelevant(shearable, manualShearing)) {
            lines.add(section("Entity shearing"));
            lines.add(indentedValue("Implements Shearable", yesNo(shearable)));
            lines.add(indentedValue(
                    "Eligible classification",
                    plain(shearingClassification(manualShearing))));
            lines.add(indentedValue("Standard-resource tag", yesNo(manualShearing.standardTagged())));
            lines.add(indentedValue("Special safety tag", yesNo(manualShearing.specialTagged())));
            lines.add(indentedValue("Tag conflict", yesNo(manualShearing.tagConflict())));
            lines.add(indentedValue("Manual shearing", enabled(manualShearing.sourceEnabled())));
            lines.add(indentedValue("Automated shearing", enabled(automatedShearing.sourceEnabled())));
            lines.add(indentedValue(
                    "Configured shearing multiplier",
                    configuredShearingMultiplier(manualShearing)));
            lines.add(indentedValue(
                    "Manual selected rule",
                    plain(shearingRuleSource(manualShearing))));
            lines.add(indentedValue(
                    "Effective manual multiplier",
                    shearingMultiplier(manualShearing)));
            lines.add(indentedValue(
                    "Effective automated multiplier",
                    shearingMultiplier(automatedShearing)));
            lines.add(indentedValue("Resolution reason", plain(manualShearing.reason())));
            lines.add(indentedValue(
                    "Output safety budget",
                    plain("Enforced at action time: "
                            + ShearingOutputBudget.MAX_MULTIPLIED_ITEMS
                            + " items / "
                            + ShearingOutputBudget.MAX_SOURCE_OR_MATERIALIZED_STACKS
                            + " source or materialized stacks")));
            lines.add(Component.literal(
                            "  Standard helper output will be multiplied only when a supported player or "
                                    + "vanilla dispenser path uses it.")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        lines.add(Component.literal(
                        "Tip: Include this output when reporting entity or mod compatibility issues.")
                .withStyle(ChatFormatting.DARK_GRAY));
        return List.copyOf(lines);
    }

    private static Component title() {
        return Component.literal("Resource Multiplier Entity Inspection")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
    }

    private static Component section(final String text) {
        return Component.literal(text).withStyle(ChatFormatting.AQUA);
    }

    private static MutableComponent valueLine(final String label, final Component value) {
        return Component.literal(label + ": ").withStyle(ChatFormatting.GRAY).append(value);
    }

    private static MutableComponent indentedValue(final String label, final Component value) {
        return Component.literal("  " + label + ": ").withStyle(ChatFormatting.GRAY).append(value);
    }

    private static Component copyableId(final String id) {
        final String visible = BlockInspectionFormatter.truncate(id, MAX_VISIBLE_ID);
        return Component.literal(visible).withStyle(style -> style
                .withColor(ChatFormatting.DARK_GRAY)
                .withClickEvent(new ClickEvent.CopyToClipboard(id))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal(
                        "Click to copy entity ID\n" + id))));
    }

    private static Component itemResult(final EntityRuleTrace trace) {
        if (!trace.itemEligible()) {
            return Component.literal("Vanilla (" + titleCase(trace.itemReason().name()) + ")")
                    .withStyle(ChatFormatting.YELLOW);
        }
        if (trace.appliedMultiplier() == 0) {
            return Component.literal("Eligible non-protected loot suppressed")
                    .withStyle(ChatFormatting.RED);
        }
        return Component.literal("Multiplier active").withStyle(ChatFormatting.GREEN);
    }

    private static Component effectiveItemMultiplier(final EntityRuleTrace trace) {
        if (!trace.itemEligible() || trace.appliedMultiplier() == 1) {
            return Component.literal("Vanilla 1x").withStyle(ChatFormatting.YELLOW);
        }
        if (trace.appliedMultiplier() == 0) {
            return Component.literal("Eligible non-protected loot suppressed (0x)")
                    .withStyle(ChatFormatting.RED);
        }
        return multiplier(trace.appliedMultiplier());
    }

    private static Component experienceResult(final EntityRuleTrace trace) {
        if (!trace.experienceEligible()) {
            return Component.literal("Vanilla").withStyle(ChatFormatting.YELLOW);
        }
        return multiplier(trace.appliedExperienceMultiplier());
    }

    private static boolean shearingRelevant(
            final boolean shearable,
            final ShearingRuleTrace trace
    ) {
        return shearable
                || trace.standardTagged()
                || trace.specialTagged()
                || trace.exactOverride() != null;
    }

    private static String shearingClassification(final ShearingRuleTrace trace) {
        return switch (trace.classification()) {
            case STANDARD_RESOURCE -> "Standard Resource";
            case SPECIAL -> "Special Transformation";
            case UNKNOWN -> "Uncertified / Unknown";
        };
    }

    private static Component configuredShearingMultiplier(final ShearingRuleTrace trace) {
        if (trace.classification() != ShearingClassification.STANDARD_RESOURCE) {
            return Component.literal("Unreachable; fixed vanilla 1x")
                    .withStyle(ChatFormatting.YELLOW);
        }
        if (trace.exactOverride() != null) {
            return multiplier(trace.exactOverride());
        }
        if (trace.inheritsGlobalDefault()) {
            return Component.literal("Inherit global -> "
                            + trace.configuredDefaultMultiplier() + "x")
                    .withStyle(ChatFormatting.YELLOW);
        }
        return multiplier(trace.configuredDefaultMultiplier());
    }

    private static Component shearingMultiplier(final ShearingRuleTrace trace) {
        if (trace.fixedVanilla()
                || trace.selectedRule() == ShearingRuleTrace.RuleSource.MOD_DISABLED
                || trace.selectedRule() == ShearingRuleTrace.RuleSource.SOURCE_DISABLED
                || trace.appliedMultiplier() == 1) {
            return Component.literal(trace.fixedVanilla() ? "Fixed vanilla 1x" : "Vanilla 1x")
                    .withStyle(ChatFormatting.YELLOW);
        }
        if (trace.appliedMultiplier() == 0) {
            return Component.literal("Qualifying standard output suppressed (0x)")
                    .withStyle(ChatFormatting.RED);
        }
        return multiplier(trace.appliedMultiplier());
    }

    private static String shearingRuleSource(final ShearingRuleTrace trace) {
        return switch (trace.selectedRule()) {
            case SPECIAL_SAFETY -> "Special safety gate";
            case UNKNOWN_SAFETY -> "Unknown / uncertified safety gate";
            case MOD_DISABLED -> "Mod disabled";
            case SOURCE_DISABLED -> "Source disabled";
            case ENTITY_OVERRIDE -> "Individual shearing entity override";
            case GLOBAL_DEFAULT -> "Global multiplier inherited by shearing";
            case SHEARING_DEFAULT -> "Default shearing multiplier";
        };
    }

    private static String ruleSource(final EntityRuleTrace trace) {
        return switch (trace.selectedRule()) {
            case GLOBAL -> "Global multiplier";
            case ENTITY_DEFAULT -> "Default entity multiplier";
            case CATEGORY_OVERRIDE -> categoryName(trace.selectedCategory()) + " category override";
            case ENTITY_OVERRIDE -> "Individual entity override";
        };
    }

    private static String killRequirement(final SmartDropsConfig.EntityKillRequirement requirement) {
        return switch (requirement) {
            case PLAYER_KILLS_ONLY -> "Player Kills Only";
            case PLAYER_OR_TAMED_ENTITY -> "Player or Tamed Entity";
            case ALL_STANDARD_DEATH_LOOT -> "All Standard Death Loot";
        };
    }

    private static String categoryList(final List<EntityCategory> categories) {
        if (categories.isEmpty()) {
            return "None";
        }
        final StringJoiner joiner = new StringJoiner(", ");
        categories.forEach(category -> joiner.add(categoryName(category)));
        return joiner.toString();
    }

    private static String categoryName(final EntityCategory category) {
        return category == null ? "None" : titleCase(category.key());
    }

    private static String sourceList(final Set<EntityClassification.MatchSource> sources) {
        if (sources.isEmpty()) {
            return "None";
        }
        final StringJoiner joiner = new StringJoiner(", ");
        sources.forEach(source -> joiner.add(titleCase(source.name())));
        return joiner.toString();
    }

    private static String tagList(final Set<String> tags) {
        if (tags.isEmpty()) {
            return "None";
        }
        final StringJoiner joiner = new StringJoiner(", ");
        int count = 0;
        for (String tag : tags) {
            if (count == MAX_VISIBLE_TAGS) {
                break;
            }
            joiner.add(BlockInspectionFormatter.truncate(tag, MAX_VISIBLE_TAG_LENGTH));
            count++;
        }
        if (tags.size() > count) {
            joiner.add("+" + (tags.size() - count) + " more");
        }
        return joiner.toString();
    }

    private static Component optionalMultiplier(final Integer value) {
        return value == null
                ? Component.literal("None").withStyle(ChatFormatting.DARK_GRAY)
                : multiplier(value);
    }

    private static Component multiplier(final int value) {
        final ChatFormatting color = value == 0
                ? ChatFormatting.RED
                : value == 1 ? ChatFormatting.YELLOW : ChatFormatting.GREEN;
        return Component.literal(value + "x").withStyle(color);
    }

    private static Component enabled(final boolean value) {
        return Component.literal(value ? "Enabled" : "Disabled")
                .withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    private static Component yesNo(final boolean value) {
        return Component.literal(value ? "Yes" : "No")
                .withStyle(value ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY);
    }

    private static Component status(final String text, final boolean positive) {
        return Component.literal(text).withStyle(positive ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    private static Component plain(final String text) {
        return Component.literal(text).withStyle(ChatFormatting.WHITE);
    }

    private static String titleCase(final String value) {
        final String normalized = value.toLowerCase(Locale.ROOT).replace('-', '_');
        final StringJoiner joiner = new StringJoiner(" ");
        for (String part : normalized.split("_")) {
            if (!part.isEmpty()) {
                joiner.add(Character.toUpperCase(part.charAt(0)) + part.substring(1));
            }
        }
        return joiner.toString();
    }
}
