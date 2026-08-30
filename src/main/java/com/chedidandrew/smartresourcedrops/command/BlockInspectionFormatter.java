package com.chedidandrew.smartresourcedrops.command;

import com.chedidandrew.smartresourcedrops.config.SmartDropsConfig;
import com.chedidandrew.smartresourcedrops.core.Category;
import com.chedidandrew.smartresourcedrops.core.DropSource;
import com.chedidandrew.smartresourcedrops.core.RuleEngine;
import com.chedidandrew.smartresourcedrops.core.RuleResolutionTrace;
import com.chedidandrew.smartresourcedrops.core.util.LootOutputBudget;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/** Server-safe chat rendering for an immutable inspection trace. */
final class BlockInspectionFormatter {
    private static final int MAX_VISIBLE_ID = 96;
    private static final int MAX_VISIBLE_STATE = 160;
    private static final int MAX_HOVER_STATE = 1_024;
    private static final int MAX_VISIBLE_TAGS = 8;
    private static final int MAX_VISIBLE_TAG_LENGTH = 64;

    private BlockInspectionFormatter() {
    }

    static List<Component> format(
            BlockState state,
            BlockPos pos,
            RuleResolutionTrace trace,
            boolean verbose
    ) {
        return verbose ? verbose(state, pos, trace) : compact(state, pos, trace);
    }

    private static List<Component> compact(
            BlockState state,
            BlockPos pos,
            RuleResolutionTrace trace
    ) {
        List<Component> lines = new ArrayList<>();
        lines.add(title());
        lines.add(valueLine("Block", state.getBlock().getName().copy().withStyle(ChatFormatting.WHITE)));
        lines.add(valueLine("ID", copyableId(trace.blockId())));
        lines.add(valueLine("Position", plain(position(pos))));
        lines.add(valueLine("Dimension", technical(trace.dimensionId())));
        lines.add(valueLine("Categories", plain(categoryList(trace.matchedCategories()))));
        lines.add(valueLine("Provenance", status(
                provenanceText(trace), trace.provenanceEligible())));
        lines.add(valueLine("Block entity", status(
                blockEntitySummary(trace), !trace.blockEntityProtected())));
        lines.add(valueLine("Filter", status(filterResult(trace), trace.filterEligible())));
        lines.add(valueLine("Configured multiplier", multiplier(trace.configuredMultiplier())));
        lines.add(valueLine("Effective player-mining result", effectiveResult(trace)));
        lines.add(valueLine("Resolved from", plain(ruleSource(trace))));
        lines.add(valueLine("Eligibility", status(
                trace.eligible() ? "Eligible" : "Not eligible", trace.eligible())));
        if (!trace.eligible()) {
            lines.add(valueLine("Reason", status(reasonText(trace), false)));
        }
        lines.add(valueLine("Player mining", enabled(trace.playerMiningEnabled())));
        lines.add(valueLine("Smart placement protection", enabled(trace.smartPlacementProtectionEnabled())));
        return List.copyOf(lines);
    }

    private static List<Component> verbose(
            BlockState state,
            BlockPos pos,
            RuleResolutionTrace trace
    ) {
        List<Component> lines = new ArrayList<>();
        lines.add(title());

        lines.add(section("Block"));
        lines.add(indentedValue("Name", state.getBlock().getName().copy().withStyle(ChatFormatting.WHITE)));
        lines.add(indentedValue("ID", copyableId(trace.blockId())));
        lines.add(indentedValue("State", stateValue(state)));
        lines.add(indentedValue("Position", plain(position(pos))));
        lines.add(indentedValue("Dimension", technical(trace.dimensionId())));

        lines.add(section("Classification"));
        lines.add(indentedValue("Matched categories", plain(categoryList(trace.matchedCategories()))));
        lines.add(indentedValue("Resolution order", plain(categoryList(trace.matchedCategories()))));
        lines.add(indentedValue("Selected category", plain(categoryName(trace.selectedCategory()))));
        lines.add(indentedValue("Block entity", yesNo(trace.hasBlockEntity())));
        lines.add(indentedValue("Protection enabled", enabled(trace.blockEntityProtectionEnabled())));
        lines.add(indentedValue("Allowlisted", yesNo(trace.blockEntityAllowlisted())));
        lines.add(indentedValue("Result", status(
                blockEntityResult(trace), !trace.blockEntityProtected())));

        lines.add(section("Provenance"));
        lines.add(indentedValue("Status", status(provenanceText(trace), trace.provenanceEligible())));
        lines.add(indentedValue("Smart placement protection", enabled(trace.smartPlacementProtectionEnabled())));
        lines.add(indentedValue("Source mode", plain(sourceMode(trace.sourceMode()))));
        lines.add(indentedValue("Eligible by provenance", yesNo(trace.provenanceEligible())));

        lines.add(section("Filtering"));
        lines.add(indentedValue("Mode", plain(titleCase(trace.filterMode().name()))));
        lines.add(indentedValue("Exact block blacklist match", yesNo(trace.exactBlacklisted())));
        lines.add(indentedValue("Matching blacklist tags", plain(tagList(trace.matchingBlacklistTags()))));
        lines.add(indentedValue("Exact block whitelist match", yesNo(trace.exactWhitelisted())));
        lines.add(indentedValue("Matching whitelist tags", plain(tagList(trace.matchingWhitelistTags()))));
        lines.add(indentedValue("Result", status(filterResult(trace), trace.filterEligible())));

        lines.add(section("Multiplier resolution"));
        lines.add(indentedValue("Player overrides enabled", enabled(trace.playerOverridesEnabled())));
        lines.add(indentedValue("Stored player override", optionalMultiplier(trace.storedPlayerOverride())));
        lines.add(indentedValue("Effective player override", optionalMultiplier(trace.effectivePlayerOverride())));
        if (trace.storedPlayerOverride() != null) {
            lines.add(indentedValue("Maximum personal multiplier", multiplier(trace.maxPlayerMultiplier())));
        }
        lines.add(indentedValue("Block override", optionalMultiplier(trace.blockOverride())));
        lines.add(indentedValue("Category candidates", plain(categoryCandidates(trace))));
        lines.add(indentedValue("Dimension override", optionalMultiplier(trace.dimensionOverride())));
        lines.add(indentedValue("Global multiplier", multiplier(trace.globalMultiplier())));
        lines.add(indentedValue("Maximum multiplier", multiplier(trace.maximumMultiplier())));
        lines.add(indentedValue("Selected rule", plain(ruleSource(trace))));
        if (trace.selectedRuleValue() != trace.configuredMultiplier()) {
            lines.add(indentedValue("Selected value before cap", multiplier(trace.selectedRuleValue())));
        }
        lines.add(indentedValue("Configured multiplier", multiplier(trace.configuredMultiplier())));
        lines.add(indentedValue("Effective player-mining result", effectiveResult(trace)));
        lines.add(indentedValue(
                "Output safety budget",
                plain("Enforced at drop time ("
                        + LootOutputBudget.MAX_MULTIPLIED_ITEMS
                        + " items / "
                        + LootOutputBudget.MAX_MULTIPLIED_STACKS
                        + " stacks)")));
        lines.add(indentedValue("Final eligibility", status(
                trace.eligible() ? "Eligible" : "Not eligible", trace.eligible())));
        if (!trace.eligible()) {
            lines.add(indentedValue("Reason", status(reasonText(trace), false)));
        }

        lines.add(section("Sources"));
        lines.add(indentedValue("Player mining", enabled(trace.playerMiningEnabled())));
        lines.add(indentedValue("Explosions", enabled(trace.explosionsEnabled())));
        lines.add(indentedValue("Automated mining", enabled(trace.automationEnabled())));
        lines.add(Component.literal("Tip: Include this inspection output when reporting mod compatibility issues.")
                .withStyle(ChatFormatting.DARK_GRAY));
        return List.copyOf(lines);
    }

    private static Component title() {
        return Component.literal("Resource Multiplier Inspection")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
    }

    private static Component section(String text) {
        return Component.literal(text).withStyle(ChatFormatting.AQUA);
    }

    private static MutableComponent valueLine(String label, Component value) {
        return Component.literal(label + ": ")
                .withStyle(ChatFormatting.GRAY)
                .append(value);
    }

    private static MutableComponent indentedValue(String label, Component value) {
        return Component.literal("  " + label + ": ")
                .withStyle(ChatFormatting.GRAY)
                .append(value);
    }

    private static Component copyableId(String id) {
        String visible = truncate(id, MAX_VISIBLE_ID);
        return Component.literal(visible).withStyle(style -> style
                .withColor(ChatFormatting.DARK_GRAY)
                .withClickEvent(new ClickEvent.CopyToClipboard(id))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal(
                        "Click to copy block ID\n" + id))));
    }

    private static Component stateValue(BlockState state) {
        String full = describeState(state);
        String visible = truncate(full, MAX_VISIBLE_STATE);
        MutableComponent value = Component.literal(visible).withStyle(ChatFormatting.WHITE);
        if (!visible.equals(full)) {
            String hover = truncate(full, MAX_HOVER_STATE);
            return value.withStyle(style -> style.withHoverEvent(
                    new HoverEvent.ShowText(Component.literal(hover))));
        }
        return value;
    }

    static String describeState(BlockState state) {
        List<String> values = state.getValues().map(Object::toString).toList();
        if (values.isEmpty()) {
            return "Default";
        }
        return String.join(", ", values);
    }

    private static Component effectiveResult(RuleResolutionTrace trace) {
        if (!trace.eligible() || trace.appliedMultiplier() == 1) {
            return Component.literal("Vanilla 1x").withStyle(
                    trace.eligible() ? ChatFormatting.YELLOW : ChatFormatting.RED);
        }
        if (trace.appliedMultiplier() == 0) {
            return Component.literal("Drops suppressed (0x)").withStyle(ChatFormatting.RED);
        }
        return Component.literal(trace.appliedMultiplier() + "x").withStyle(ChatFormatting.GREEN);
    }

    private static Component multiplier(int value) {
        ChatFormatting color = value == 0
                ? ChatFormatting.RED
                : value == 1 ? ChatFormatting.YELLOW : ChatFormatting.GREEN;
        return Component.literal(value + "x").withStyle(color);
    }

    private static Component optionalMultiplier(Integer value) {
        return value == null
                ? Component.literal("None").withStyle(ChatFormatting.DARK_GRAY)
                : multiplier(value);
    }

    private static Component enabled(boolean value) {
        return Component.literal(value ? "Enabled" : "Disabled")
                .withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    private static Component yesNo(boolean value) {
        return Component.literal(value ? "Yes" : "No")
                .withStyle(value ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY);
    }

    private static Component status(String text, boolean positive) {
        return Component.literal(text).withStyle(positive ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    private static Component plain(String text) {
        return Component.literal(text).withStyle(ChatFormatting.WHITE);
    }

    private static Component technical(String text) {
        return Component.literal(text).withStyle(ChatFormatting.DARK_GRAY);
    }

    private static String position(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    private static String provenanceText(RuleResolutionTrace trace) {
        return trace.playerPlaced() ? "Player placed / tracked" : "Natural / untracked";
    }

    private static String blockEntitySummary(RuleResolutionTrace trace) {
        if (!trace.hasBlockEntity()) {
            return "No";
        }
        return trace.blockEntityProtected() ? "Yes (protected)" : "Yes (allowed)";
    }

    private static String blockEntityResult(RuleResolutionTrace trace) {
        if (!trace.hasBlockEntity()) {
            return "Not applicable";
        }
        return trace.blockEntityProtected() ? "Protected" : "Allowed";
    }

    private static String filterResult(RuleResolutionTrace trace) {
        if (trace.filterMode() == SmartDropsConfig.FilterMode.BLACKLIST) {
            if (trace.filterEligible()) {
                return "Allowed";
            }
            if (trace.exactBlacklisted() && !trace.matchingBlacklistTags().isEmpty()) {
                return "Blocked by exact block and tag blacklist";
            }
            return trace.exactBlacklisted()
                    ? "Blocked by exact block blacklist"
                    : "Blocked by tag blacklist";
        }
        if (!trace.filterEligible()) {
            return "Not present in whitelist";
        }
        if (trace.exactWhitelisted() && !trace.matchingWhitelistTags().isEmpty()) {
            return "Allowed by exact block and tag whitelist";
        }
        return trace.exactWhitelisted() ? "Allowed by exact block whitelist" : "Allowed by tag whitelist";
    }

    private static String reasonText(RuleResolutionTrace trace) {
        return switch (trace.reason()) {
            case MOD_DISABLED -> "Resource Multiplier is disabled";
            case SOURCE_DISABLED -> sourceName(trace.source()) + " is disabled";
            case BLOCK_ENTITY_PROTECTED -> "Block entity protection is enabled and this block is not allowlisted";
            case FILTERED -> filterResult(trace);
            case PLAYER_PLACED_PROTECTED ->
                    "Tracked block protected in Natural Blocks Only mode";
            case NATURAL_BLOCK_EXCLUDED ->
                    "Untracked block excluded in Player-Placed Blocks Only mode";
            case GLOBAL_RULE, DIMENSION_RULE, CATEGORY_RULE, BLOCK_RULE, PLAYER_OVERRIDE ->
                    "Eligible through " + ruleSource(trace);
        };
    }

    private static String ruleSource(RuleResolutionTrace trace) {
        return switch (trace.selectedRule()) {
            case GLOBAL -> "Global multiplier";
            case DIMENSION_OVERRIDE -> "Dimension override";
            case CATEGORY_OVERRIDE -> categoryName(trace.categoryRuleCategory()) + " category override";
            case BLOCK_OVERRIDE -> "Individual block override";
            case PLAYER_OVERRIDE -> "Player override";
        };
    }

    private static String categoryCandidates(RuleResolutionTrace trace) {
        StringJoiner joiner = new StringJoiner(", ");
        Map<Category, Integer> overrides = trace.categoryOverrides();
        for (Category category : trace.matchedCategories()) {
            Integer value = overrides.get(category);
            joiner.add(categoryName(category) + "=" + (value == null ? "None" : value + "x"));
        }
        return joiner.toString();
    }

    private static String categoryList(List<Category> categories) {
        StringJoiner joiner = new StringJoiner(", ");
        categories.forEach(category -> joiner.add(categoryName(category)));
        return joiner.toString();
    }

    private static String categoryName(Category category) {
        return category == null ? "None" : titleCase(category.key());
    }

    private static String tagList(Set<String> tags) {
        if (tags.isEmpty()) {
            return "None";
        }
        StringJoiner joiner = new StringJoiner(", ");
        int count = 0;
        for (String tag : tags) {
            if (count == MAX_VISIBLE_TAGS) {
                break;
            }
            joiner.add(truncate(tag, MAX_VISIBLE_TAG_LENGTH));
            count++;
        }
        if (tags.size() > count) {
            joiner.add("+" + (tags.size() - count) + " more");
        }
        return joiner.toString();
    }

    private static String sourceMode(SmartDropsConfig.SourceMode mode) {
        return switch (mode) {
            case NATURAL_ONLY -> "Natural Blocks Only";
            case ALL -> "All Blocks";
            case PLAYER_PLACED_ONLY -> "Player-Placed Blocks Only";
        };
    }

    private static String sourceName(DropSource source) {
        return switch (source) {
            case PLAYER -> "Player mining";
            case EXPLOSION -> "Explosions";
            case AUTOMATION -> "Automated mining";
        };
    }

    private static String titleCase(String value) {
        String normalized = value.toLowerCase(Locale.ROOT).replace('-', '_');
        StringJoiner joiner = new StringJoiner(" ");
        for (String part : normalized.split("_")) {
            if (!part.isEmpty()) {
                joiner.add(Character.toUpperCase(part.charAt(0)) + part.substring(1));
            }
        }
        return joiner.toString();
    }

    static String truncate(String value, int maximumLength) {
        if (value.length() <= maximumLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maximumLength - 1)) + "…";
    }
}
