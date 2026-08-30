package com.chedidandrew.smartresourcedrops.client;

import java.util.stream.Collectors;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Focused editor for one category, dimension, or exact block rule. */
final class RuleEditScreen extends SmartDropsSubScreen {
    enum Kind {
        CATEGORY,
        DIMENSION,
        BLOCK
    }

    private final Kind kind;
    private final String key;
    private MultiplierControl multiplier;
    private Button resetButton;

    RuleEditScreen(
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
        final int controlY = top + (compact ? 28 : 42);
        this.multiplier = new MultiplierControl(
                this.font,
                Component.translatable(switch (this.kind) {
                    case BLOCK -> "smart_resource_drops.gui.block_multiplier";
                    case CATEGORY -> "smart_resource_drops.gui.category_multiplier";
                    case DIMENSION -> "smart_resource_drops.gui.dimension_multiplier";
                }),
                0,
                this.session.maximumMultiplier(),
                this.configuredValue(),
                true,
                Component.translatable("smart_resource_drops.gui.inherit"),
                value -> {
                    this.setConfiguredValue(value);
                    this.refreshControls();
                });
        this.multiplier.setTooltip(Component.translatable(switch (this.kind) {
            case BLOCK -> "smart_resource_drops.gui.block_multiplier_tooltip";
            case CATEGORY -> "smart_resource_drops.gui.category_multiplier_tooltip";
            case DIMENSION -> "smart_resource_drops.gui.dimension_multiplier_tooltip";
        }));
        this.multiplier.setPosition(left, controlY, this.contentWidth());
        for (AbstractWidget widget : this.multiplier.widgets()) {
            this.addRenderableWidget(widget);
        }

        final int actionY = Math.min(this.contentBottom() - 20, controlY + (compact ? 72 : 92));
        final int gap = 8;
        final boolean hasCategoryView = this.kind == Kind.CATEGORY;
        final int actionWidth = hasCategoryView
                ? Math.max(1, (this.contentWidth() - gap) / 2)
                : Math.min(240, this.contentWidth());
        final int actionLeft = hasCategoryView
                ? left
                : (this.width - actionWidth) / 2;
        if (hasCategoryView) {
            this.addRenderableWidget(Button.builder(
                            Component.translatable("smart_resource_drops.gui.view_category_blocks"),
                            button -> this.minecraft.gui.setScreen(new BlockOverridesScreen(
                                    this.root,
                                    this,
                                    this.session,
                                    this.key)))
                    .bounds(actionLeft, actionY, actionWidth, 20)
                    .build());
        }
        this.resetButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("smart_resource_drops.gui.reset_override"),
                        button -> {
                            this.setConfiguredValue(null);
                            this.refreshControls();
                        })
                .bounds(
                        hasCategoryView ? actionLeft + actionWidth + gap : actionLeft,
                        actionY,
                        actionWidth,
                        20)
                .build());
        this.addBackButton();
        this.refreshControls();
    }

    private void refreshControls() {
        if (this.multiplier == null) {
            return;
        }
        final Integer configured = this.configuredValue();
        this.multiplier.setValueSilently(configured);
        this.multiplier.setEditable(this.session.editable());
        this.resetButton.active = this.session.editable() && configured != null;
    }

    private Integer configuredValue() {
        return switch (this.kind) {
            case BLOCK -> this.session.blockMultiplier(this.key);
            case CATEGORY -> this.session.categoryMultiplier(this.key);
            case DIMENSION -> this.session.dimensionMultiplier(this.key);
        };
    }

    private void setConfiguredValue(final Integer value) {
        switch (this.kind) {
            case BLOCK -> this.session.setBlockMultiplier(this.key, value);
            case CATEGORY -> this.session.setCategoryMultiplier(this.key, value);
            case DIMENSION -> this.session.setDimensionMultiplier(this.key, value);
        }
    }

    private ConfigEditorSession.EffectiveValue effectiveValue() {
        return switch (this.kind) {
            case BLOCK -> this.session.effectiveBlockValue(this.key);
            case CATEGORY -> this.session.effectiveCategoryValue(this.key);
            case DIMENSION -> this.session.effectiveDimensionValue(this.key);
        };
    }

    private ConfigEditorSession.EffectiveValue inheritedValue() {
        return switch (this.kind) {
            case BLOCK -> this.session.inheritedBlockValue(this.key);
            case CATEGORY -> this.session.inheritedCategoryValue(this.key);
            case DIMENSION -> this.session.inheritedDimensionValue(this.key);
        };
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
        final boolean compact = this.height < 220;
        int y = top;
        if (this.kind == Kind.BLOCK) {
            graphics.text(
                    this.font,
                    ConfigUiText.fitted(
                            this.font,
                            Component.translatable("smart_resource_drops.gui.registry_id", this.key),
                            this.contentWidth()),
                    left,
                    y,
                    0xFFA0A0A0);
            y += 12;
            final String categoryNames = this.session.blockInfo(this.key)
                    .map(info -> info.categories().stream()
                            .map(category -> ConfigUiText.categoryName(category.key()).getString())
                            .collect(Collectors.joining(", ")))
                    .orElse(ConfigUiText.categoryName("miscellaneous").getString());
            graphics.text(
                    this.font,
                    ConfigUiText.fitted(
                            this.font,
                            Component.translatable(
                                    "smart_resource_drops.gui.categories_value",
                                    categoryNames),
                            this.contentWidth()),
                    left,
                    y,
                    0xFFE0E0E0);
        } else if (this.kind == Kind.DIMENSION) {
            graphics.text(
                    this.font,
                    ConfigUiText.fitted(this.font, Component.literal(this.key), this.contentWidth()),
                    left,
                    y,
                    0xFFA0A0A0);
        } else {
            graphics.text(
                    this.font,
                    Component.translatable(
                            "smart_resource_drops.gui.category_blocks",
                            this.session.categoryBlockCount(this.key)),
                    left,
                    y,
                    0xFFA0A0A0);
        }

        final int detailsY = top + (compact ? 52 : 70);
        final Integer configured = this.configuredValue();
        final ConfigEditorSession.EffectiveValue effective = this.effectiveValue();
        graphics.text(
                this.font,
                ConfigUiText.configured(configured),
                left,
                detailsY,
                0xFFE0E0E0);
        graphics.text(
                this.font,
                ConfigUiText.effective(effective.multiplier()),
                left,
                detailsY + 12,
                0xFFFFFFFF);
        final Component inherited = configured == null
                ? this.sourceName(this.inheritedValue())
                : Component.translatable("smart_resource_drops.gui.not_inherited");
        graphics.text(
                this.font,
                ConfigUiText.fitted(
                        this.font,
                        Component.translatable("smart_resource_drops.gui.inherited_from", inherited),
                        this.contentWidth()),
                left,
                detailsY + 24,
                0xFFA0A0A0);
    }

    private Component sourceName(final ConfigEditorSession.EffectiveValue value) {
        return switch (value.sourceTier()) {
            case BLOCK -> Component.literal(value.sourceKey());
            case CATEGORY -> ConfigUiText.categoryName(value.sourceKey());
            case DIMENSION -> ConfigUiText.dimensionName(value.sourceKey());
            case ENTITY, ENTITY_CATEGORY, ENTITY_DEFAULT -> Component.literal(value.sourceKey());
            case GLOBAL -> Component.translatable("smart_resource_drops.gui.global_source");
        };
    }

    private static Component titleFor(
            final ConfigEditorSession session,
            final Kind kind,
            final String key
    ) {
        return switch (kind) {
            case BLOCK -> Component.literal(session.blockInfo(key)
                    .map(ConfigEditorSession.BlockInfo::displayName)
                    .orElse(key));
            case CATEGORY -> ConfigUiText.categoryName(key);
            case DIMENSION -> ConfigUiText.dimensionName(key);
        };
    }
}
