package com.chedidandrew.smartresourcedrops.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Independent staged controls for standard entity shearing output. */
final class ShearingDropsScreen extends SmartDropsSubScreen {
    private StructuredConfigList optionList;
    private double preservedScroll;

    ShearingDropsScreen(
            final SmartDropsConfigScreen root,
            final Screen backScreen,
            final ConfigEditorSession session
    ) {
        super(
                Component.translatable("smart_resource_drops.gui.shearing_drops"),
                root,
                backScreen,
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
                Component.translatable("smart_resource_drops.gui.manual_shearing"),
                Component.translatable("smart_resource_drops.gui.manual_shearing_tooltip"),
                this.session.manualShearingDropsEnabled(),
                this.session::setManualShearingDropsEnabled);
        this.addBooleanRow(
                rows,
                Component.translatable("smart_resource_drops.gui.automated_shearing"),
                Component.translatable("smart_resource_drops.gui.automated_shearing_tooltip"),
                this.session.automatedShearingDropsEnabled(),
                this.session::setAutomatedShearingDropsEnabled);

        final Integer configuredDefault = this.session.defaultShearingMultiplier();
        final Component defaultValue = configuredDefault == null
                ? Component.translatable(
                        "smart_resource_drops.gui.shearing_default_inherit",
                        this.session.effectiveDefaultShearingMultiplier())
                : Component.literal(configuredDefault + "x");
        rows.add(new StructuredConfigList.Row(
                Component.translatable("smart_resource_drops.gui.shearing_default_multiplier"),
                Component.translatable("smart_resource_drops.gui.shearing_default_multiplier_tooltip"),
                Component.empty(),
                this.readOnlyValue(defaultValue),
                Component.translatable("smart_resource_drops.gui.shearing_default_multiplier_tooltip"),
                () -> this.minecraft.setScreen(new ShearingRuleEditScreen(
                        this.root,
                        this,
                        this.session,
                        ShearingRuleEditScreen.Kind.DEFAULT,
                        null))));
        rows.add(new StructuredConfigList.Row(
                Component.translatable("smart_resource_drops.gui.shearing_overrides"),
                Component.translatable("smart_resource_drops.gui.shearing_overrides_tooltip"),
                Component.empty(),
                Component.translatable("smart_resource_drops.gui.view_details"),
                Component.translatable("smart_resource_drops.gui.shearing_overrides_tooltip"),
                () -> this.minecraft.setScreen(new ShearingOverridesScreen(
                        this.root,
                        this,
                        this.session))));
        rows.add(new StructuredConfigList.Row(
                Component.translatable("smart_resource_drops.gui.shearing_safety"),
                Component.translatable("smart_resource_drops.gui.shearing_safety_unknown"),
                Component.translatable("smart_resource_drops.gui.shearing_safety_special"),
                Component.translatable("smart_resource_drops.gui.shearing_safety_budget"),
                Component.translatable("smart_resource_drops.gui.shearing_safety_tooltip"),
                () -> { }));
        this.optionList.replaceRows(rows);
    }

    private void addBooleanRow(
            final List<StructuredConfigList.Row> rows,
            final Component label,
            final Component explanation,
            final boolean value,
            final BooleanSetter setter
    ) {
        rows.add(new StructuredConfigList.Row(
                label,
                explanation,
                Component.empty(),
                this.readOnlyValue(ConfigUiText.onOff(value)),
                Component.empty().append(label).append("\n").append(explanation),
                () -> {
                    if (this.session.editable()) {
                        setter.set(!value);
                        this.rebuildPreservingScroll();
                    }
                }));
    }

    private Component readOnlyValue(final Component value) {
        return this.session.editable()
                ? value
                : Component.translatable("smart_resource_drops.gui.read_only_value", value);
    }

    private void rebuildPreservingScroll() {
        this.preservedScroll = this.optionList.scrollAmount();
        this.minecraft.setScreen(this);
    }

    @FunctionalInterface
    private interface BooleanSetter {
        boolean set(boolean value);
    }
}
