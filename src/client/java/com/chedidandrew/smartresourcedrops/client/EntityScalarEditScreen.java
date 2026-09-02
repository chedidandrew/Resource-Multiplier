package com.chedidandrew.smartresourcedrops.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Focused reusable multiplier editor for entity defaults and qualifying mob XP. */
final class EntityScalarEditScreen extends SmartDropsSubScreen {
    enum Kind {
        DEFAULT_MULTIPLIER,
        MOB_EXPERIENCE_MULTIPLIER
    }

    private final Kind kind;
    private MultiplierControl multiplier;
    private Button resetButton;

    EntityScalarEditScreen(
            final SmartDropsConfigScreen root,
            final Screen backScreen,
            final ConfigEditorSession session,
            final Kind kind
    ) {
        super(
                Component.translatable(kind == Kind.DEFAULT_MULTIPLIER
                        ? "smart_resource_drops.gui.entity_default_multiplier"
                        : "smart_resource_drops.gui.mob_xp_multiplier"),
                root,
                backScreen,
                session);
        this.kind = kind;
    }

    @Override
    protected void init() {
        final int left = this.contentLeft();
        final int top = this.contentTop();
        final boolean defaultMultiplier = this.kind == Kind.DEFAULT_MULTIPLIER;
        final Integer initialValue = defaultMultiplier
                ? this.session.defaultEntityMultiplier()
                : Integer.valueOf(this.session.mobExperienceMultiplier());
        this.multiplier = new MultiplierControl(
                this.font,
                Component.translatable(defaultMultiplier
                        ? "smart_resource_drops.gui.entity_default_multiplier"
                        : "smart_resource_drops.gui.mob_xp_multiplier"),
                defaultMultiplier ? 0 : 1,
                this.session.maximumMultiplier(),
                initialValue,
                defaultMultiplier,
                Component.translatable("smart_resource_drops.gui.inherit"),
                value -> {
                    if (defaultMultiplier) {
                        this.session.setDefaultEntityMultiplier(value);
                    } else if (value != null) {
                        this.session.setMobExperienceMultiplier(value);
                    }
                    this.refreshControls();
                });
        this.multiplier.setTooltip(Component.translatable(defaultMultiplier
                ? "smart_resource_drops.gui.entity_default_multiplier_tooltip"
                : "smart_resource_drops.gui.mob_xp_multiplier_tooltip"));
        this.multiplier.setPosition(left, top + 38, this.contentWidth());
        for (AbstractWidget widget : this.multiplier.widgets()) {
            this.addRenderableWidget(widget);
        }

        final int buttonWidth = Math.min(240, this.contentWidth());
        this.resetButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("smart_resource_drops.gui.reset_override"),
                        button -> {
                            if (defaultMultiplier) {
                                this.session.setDefaultEntityMultiplier(null);
                            } else {
                                this.session.setMobExperienceMultiplier(2);
                            }
                            this.refreshControls();
                        })
                .bounds((this.width - buttonWidth) / 2, top + 86, buttonWidth, 20)
                .build());
        this.addBackButton();
        this.refreshControls();
    }

    private void refreshControls() {
        if (this.multiplier == null) {
            return;
        }
        final boolean defaultMultiplier = this.kind == Kind.DEFAULT_MULTIPLIER;
        final boolean dependencyEnabled = defaultMultiplier
                ? this.session.entityDropsEnabled()
                : this.session.multiplyMobExperience();
        final boolean editable = this.session.editable() && dependencyEnabled;
        final Integer currentValue = defaultMultiplier
                ? this.session.defaultEntityMultiplier()
                : Integer.valueOf(this.session.mobExperienceMultiplier());
        this.multiplier.setValueSilently(currentValue);
        this.multiplier.setEditable(editable);
        this.resetButton.active = editable && (defaultMultiplier
                ? this.session.defaultEntityMultiplier() != null
                : this.session.mobExperienceMultiplier() != 2);
    }

    @Override
    public void render(
            final GuiGraphics graphics,
            final int mouseX,
            final int mouseY,
            final float partialTick
    ) {
        super.render(graphics, mouseX, mouseY, partialTick);
        final boolean defaultMultiplier = this.kind == Kind.DEFAULT_MULTIPLIER;
        final boolean dependencyEnabled = defaultMultiplier
                ? this.session.entityDropsEnabled()
                : this.session.multiplyMobExperience();
        final Component explanation = Component.translatable(defaultMultiplier
                ? "smart_resource_drops.gui.entity_default_multiplier_tooltip"
                : "smart_resource_drops.gui.mob_xp_multiplier_tooltip");
        graphics.drawCenteredString(
                this.font,
                ConfigUiText.fitted(this.font, explanation, this.contentWidth()),
                this.width / 2,
                this.contentTop() + 8,
                0xFFA0A0A0);
        if (!dependencyEnabled) {
            graphics.drawCenteredString(
                    this.font,
                    Component.translatable("smart_resource_drops.gui.control_inactive"),
                    this.width / 2,
                    this.contentTop() + 66,
                    0xFFB08080);
        }
    }
}
