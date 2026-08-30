package com.chedidandrew.smartresourcedrops.client;

import com.chedidandrew.smartresourcedrops.core.shearing.ShearingClassification;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Focused editor for the shearing default or one certified exact entity override. */
final class ShearingRuleEditScreen extends SmartDropsSubScreen {
    enum Kind {
        DEFAULT,
        ENTITY
    }

    private final Kind kind;
    private final String key;
    private MultiplierControl multiplier;

    ShearingRuleEditScreen(
            final SmartDropsConfigScreen root,
            final Screen backScreen,
            final ConfigEditorSession session,
            final Kind kind,
            final String key
    ) {
        super(titleFor(session, kind, key), root, backScreen, session);
        this.kind = kind;
        this.key = key;
    }

    @Override
    protected void init() {
        final int left = this.contentLeft();
        final int top = this.contentTop();
        final boolean compact = this.height < 220;
        final int controlY = top + (compact ? 30 : 46);
        this.multiplier = new MultiplierControl(
                this.font,
                Component.translatable(this.kind == Kind.DEFAULT
                        ? "smart_resource_drops.gui.shearing_default_multiplier"
                        : "smart_resource_drops.gui.shearing_override_multiplier"),
                0,
                this.session.maximumMultiplier(),
                this.configuredValue(),
                true,
                Component.translatable("smart_resource_drops.gui.inherit"),
                value -> {
                    this.setConfiguredValue(value);
                    this.refreshControls();
                });
        this.multiplier.setTooltip(Component.translatable(this.kind == Kind.DEFAULT
                ? "smart_resource_drops.gui.shearing_default_multiplier_tooltip"
                : "smart_resource_drops.gui.shearing_override_multiplier_tooltip"));
        this.multiplier.setPosition(left, controlY, this.contentWidth());
        for (AbstractWidget widget : this.multiplier.widgets()) {
            this.addRenderableWidget(widget);
        }

        this.addBackButton();
        this.refreshControls();
    }

    private void refreshControls() {
        final Integer configured = this.configuredValue();
        final boolean safeEntity = this.kind == Kind.DEFAULT
                || this.session.shearingEntityEditable(this.key);
        final boolean editable = this.session.editable() && safeEntity;
        this.multiplier.setValueSilently(configured);
        this.multiplier.setEditable(editable);
    }

    private Integer configuredValue() {
        return this.kind == Kind.DEFAULT
                ? this.session.defaultShearingMultiplier()
                : this.session.shearingEntityMultiplier(this.key);
    }

    private void setConfiguredValue(final Integer value) {
        if (this.kind == Kind.DEFAULT) {
            this.session.setDefaultShearingMultiplier(value);
        } else {
            this.session.setShearingEntityMultiplier(this.key, value);
        }
    }

    private int effectiveValue() {
        return this.kind == Kind.DEFAULT
                ? this.session.effectiveDefaultShearingMultiplier()
                : this.session.effectiveShearingMultiplier(this.key);
    }

    @Override
    public void extractRenderState(
            final GuiGraphicsExtractor graphics,
            final int mouseX,
            final int mouseY,
            final float partialTick
    ) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        final int left = this.contentLeft();
        final int top = this.contentTop();
        if (this.kind == Kind.ENTITY) {
            graphics.text(
                    this.font,
                    ConfigUiText.fitted(
                            this.font,
                            Component.translatable("smart_resource_drops.gui.registry_id", this.key),
                            this.contentWidth()),
                    left,
                    top,
                    0xFFA0A0A0);
            graphics.text(
                    this.font,
                    Component.translatable(
                            "smart_resource_drops.gui.shearing_classification_value",
                            classificationName(this.session.shearingClassification(this.key))),
                    left,
                    top + 12,
                    0xFFE0E0E0);
        } else {
            graphics.centeredText(
                    this.font,
                    ConfigUiText.fitted(
                            this.font,
                            Component.translatable(
                                    "smart_resource_drops.gui.shearing_default_multiplier_tooltip"),
                            this.contentWidth()),
                    this.width / 2,
                    top + 8,
                    0xFFA0A0A0);
        }

        final int detailsY = top + (this.height < 220 ? 52 : 76);
        final Integer configured = this.configuredValue();
        graphics.text(this.font, ConfigUiText.configured(configured), left, detailsY, 0xFFE0E0E0);
        graphics.text(
                this.font,
                this.kind == Kind.ENTITY && !this.session.shearingEntityEditable(this.key)
                        ? Component.translatable("smart_resource_drops.gui.shearing_fixed_vanilla")
                        : ConfigUiText.effective(this.effectiveValue()),
                left,
                detailsY + 12,
                0xFFFFFFFF);
        if (configured == null && (this.kind == Kind.DEFAULT
                || this.session.shearingEntityEditable(this.key))) {
            final Component inherited = this.kind == Kind.DEFAULT
                    ? Component.translatable("smart_resource_drops.gui.global_source")
                    : Component.translatable("smart_resource_drops.gui.shearing_default_source");
            graphics.text(
                    this.font,
                    Component.translatable("smart_resource_drops.gui.inherited_from", inherited),
                    left,
                    detailsY + 24,
                    0xFFA0A0A0);
        }
    }

    static Component classificationName(final ShearingClassification classification) {
        return Component.translatable(switch (classification) {
            case STANDARD_RESOURCE ->
                    "smart_resource_drops.gui.shearing_classification_standard";
            case SPECIAL -> "smart_resource_drops.gui.shearing_classification_special";
            case UNKNOWN -> "smart_resource_drops.gui.shearing_classification_unknown";
        });
    }

    private static Component titleFor(
            final ConfigEditorSession session,
            final Kind kind,
            final String key
    ) {
        if (kind == Kind.DEFAULT) {
            return Component.translatable("smart_resource_drops.gui.shearing_default_multiplier");
        }
        return Component.literal(session.shearingInfo(key)
                .map(ConfigEditorSession.ShearingInfo::displayName)
                .orElse(key == null ? "" : key));
    }
}
