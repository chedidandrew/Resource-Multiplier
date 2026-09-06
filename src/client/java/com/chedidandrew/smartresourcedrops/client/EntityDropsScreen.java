package com.chedidandrew.smartresourcedrops.client;

import com.chedidandrew.smartresourcedrops.config.SmartDropsConfig;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Focused entry point for staged entity death-loot, mob-XP, and independent shearing settings. */
final class EntityDropsScreen extends SmartDropsSubScreen {
    private StructuredConfigList optionList;
    private double preservedScroll;

    EntityDropsScreen(
            final SmartDropsConfigScreen root,
            final ConfigEditorSession session
    ) {
        super(
                Component.translatable("smart_resource_drops.gui.entity_drops_title"),
                root,
                root,
                session);
    }

    @Override
    protected void init() {
        if (this.optionList != null) {
            this.preservedScroll = this.optionList.scrollAmount();
        }
        final int listHeight = Math.max(20, this.contentBottom() - this.contentTop() - 4);
        this.optionList = this.addRenderableWidget(new StructuredConfigList(
                this.minecraft,
                this.width,
                listHeight,
                this.contentTop(),
                Math.min(476, this.contentWidth())));
        this.refreshRows();
        this.optionList.setScrollAmount(this.preservedScroll);
        this.addBackButton();
    }

    private void refreshRows() {
        if (this.optionList == null) {
            return;
        }
        final List<StructuredConfigList.Row> rows = new ArrayList<>();
        this.addBooleanRow(
                rows,
                Component.translatable("smart_resource_drops.gui.entity_drops_enabled"),
                Component.translatable("smart_resource_drops.gui.entity_drops_scope"),
                this.session.entityDropsEnabled(),
                true,
                this.session::setEntityDropsEnabled);

        final Integer configuredDefault = this.session.defaultEntityMultiplier();
        final Component defaultValue = configuredDefault == null
                ? Component.translatable(
                        "smart_resource_drops.gui.entity_default_inherit",
                        this.session.effectiveDefaultEntityMultiplier())
                : Component.literal(configuredDefault + "x");
        this.addNavigationRow(
                rows,
                Component.translatable("smart_resource_drops.gui.entity_default_multiplier"),
                Component.translatable("smart_resource_drops.gui.entity_default_multiplier_tooltip"),
                defaultValue,
                this.session.entityDropsEnabled(),
                () -> this.minecraft.setScreen(new EntityScalarEditScreen(
                        this.root,
                        this,
                        this.session,
                        EntityScalarEditScreen.Kind.DEFAULT_MULTIPLIER)));

        this.addNavigationRow(
                rows,
                Component.translatable("smart_resource_drops.gui.entity_kill_requirement"),
                Component.translatable("smart_resource_drops.gui.entity_kill_requirement_tooltip"),
                killRequirementName(this.session.entityKillRequirement()),
                this.session.entityDropsEnabled() || this.session.multiplyMobExperience(),
                () -> {
                    if (this.session.editable()
                            && (this.session.entityDropsEnabled()
                            || this.session.multiplyMobExperience())) {
                        this.session.setEntityKillRequirement(nextKillRequirement(
                                this.session.entityKillRequirement()));
                        this.rebuildPreservingScroll();
                    }
                });

        this.addBooleanRow(
                rows,
                Component.translatable("smart_resource_drops.gui.multiply_mob_xp"),
                Component.translatable("smart_resource_drops.gui.multiply_mob_xp_tooltip"),
                this.session.multiplyMobExperience(),
                true,
                this.session::setMultiplyMobExperience);
        this.addNavigationRow(
                rows,
                Component.translatable("smart_resource_drops.gui.mob_xp_multiplier"),
                Component.translatable("smart_resource_drops.gui.mob_xp_multiplier_tooltip"),
                Component.literal(this.session.mobExperienceMultiplier() + "x"),
                this.session.multiplyMobExperience(),
                () -> this.minecraft.setScreen(new EntityScalarEditScreen(
                        this.root,
                        this,
                        this.session,
                        EntityScalarEditScreen.Kind.MOB_EXPERIENCE_MULTIPLIER)));

        this.addBooleanRow(
                rows,
                Component.translatable("smart_resource_drops.gui.boss_drops"),
                Component.translatable("smart_resource_drops.gui.boss_drops_tooltip"),
                this.session.bossDropsEnabled(),
                this.session.entityDropsEnabled(),
                this.session::setBossDropsEnabled);
        this.addBooleanRow(
                rows,
                Component.translatable("smart_resource_drops.gui.multiply_boss_xp"),
                Component.translatable("smart_resource_drops.gui.multiply_boss_xp_tooltip"),
                this.session.multiplyBossExperience(),
                this.session.multiplyMobExperience(),
                this.session::setMultiplyBossExperience);

        this.addNavigationRow(
                rows,
                Component.translatable("smart_resource_drops.gui.entity_categories"),
                Component.translatable("smart_resource_drops.gui.entity_categories_tooltip"),
                Component.translatable("smart_resource_drops.gui.view_details"),
                true,
                () -> this.minecraft.setScreen(new EntityCategoryScreen(
                        this.root,
                        this,
                        this.session)));
        this.addNavigationRow(
                rows,
                Component.translatable("smart_resource_drops.gui.entity_overrides"),
                Component.translatable("smart_resource_drops.gui.entity_overrides_tooltip"),
                Component.translatable("smart_resource_drops.gui.view_details"),
                true,
                () -> this.minecraft.setScreen(new EntityOverridesScreen(
                        this.root,
                        this,
                        this.session)));
        this.addNavigationRow(
                rows,
                Component.translatable("smart_resource_drops.gui.entity_filters"),
                Component.translatable("smart_resource_drops.gui.entity_filters_tooltip"),
                Component.translatable("smart_resource_drops.gui.view_details"),
                true,
                () -> this.minecraft.setScreen(new EntityFilterScreen(
                        this.root,
                        this,
                        this.session)));
        this.addNavigationRow(
                rows,
                Component.translatable("smart_resource_drops.gui.shearing_drops"),
                Component.translatable("smart_resource_drops.gui.shearing_drops_tooltip"),
                Component.translatable("smart_resource_drops.gui.configure"),
                true,
                () -> this.minecraft.setScreen(new ShearingDropsScreen(
                        this.root,
                        this,
                        this.session)));

        this.optionList.replaceRows(rows);
    }

    private void addBooleanRow(
            final List<StructuredConfigList.Row> rows,
            final Component label,
            final Component explanation,
            final boolean value,
            final boolean dependencyEnabled,
            final BooleanSetter setter
    ) {
        final Component displayed = this.dependentValue(ConfigUiText.onOff(value), dependencyEnabled);
        rows.add(new StructuredConfigList.Row(
                label,
                explanation,
                Component.empty(),
                displayed,
                Component.empty().append(label).append("\n").append(explanation),
                () -> {
                    if (this.session.editable() && dependencyEnabled) {
                        setter.set(!value);
                        this.rebuildPreservingScroll();
                    }
                }));
    }

    private void addNavigationRow(
            final List<StructuredConfigList.Row> rows,
            final Component label,
            final Component explanation,
            final Component value,
            final boolean dependencyEnabled,
            final Runnable action
    ) {
        rows.add(new StructuredConfigList.Row(
                label,
                explanation,
                Component.empty(),
                this.dependentValue(value, dependencyEnabled),
                Component.empty().append(label).append("\n").append(explanation),
                action));
    }

    private Component dependentValue(final Component value, final boolean dependencyEnabled) {
        if (!dependencyEnabled) {
            return Component.translatable("smart_resource_drops.gui.inactive_value", value);
        }
        return this.session.editable()
                ? value
                : Component.translatable("smart_resource_drops.gui.read_only_value", value);
    }

    private void rebuildPreservingScroll() {
        this.preservedScroll = this.optionList.scrollAmount();
        this.minecraft.setScreen(this);
    }

    private static SmartDropsConfig.EntityKillRequirement nextKillRequirement(
            final SmartDropsConfig.EntityKillRequirement current
    ) {
        final SmartDropsConfig.EntityKillRequirement[] values =
                SmartDropsConfig.EntityKillRequirement.values();
        return values[(current.ordinal() + 1) % values.length];
    }

    static Component killRequirementName(final SmartDropsConfig.EntityKillRequirement value) {
        return Component.translatable(switch (value) {
            case PLAYER_KILLS_ONLY -> "smart_resource_drops.gui.kill_player_only";
            case PLAYER_OR_TAMED_ENTITY -> "smart_resource_drops.gui.kill_player_or_tamed";
            case ALL_STANDARD_DEATH_LOOT -> "smart_resource_drops.gui.kill_all_standard";
        });
    }

    @FunctionalInterface
    private interface BooleanSetter {
        boolean set(boolean value);
    }
}
