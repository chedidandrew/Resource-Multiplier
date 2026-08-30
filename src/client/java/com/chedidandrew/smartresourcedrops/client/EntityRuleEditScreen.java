package com.chedidandrew.smartresourcedrops.client;

import com.chedidandrew.smartresourcedrops.core.entity.EntityCategory;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.stream.Collectors;

/** Focused editor for one entity category or exact entity-type override. */
final class EntityRuleEditScreen extends SmartDropsSubScreen {
    enum Kind {
        CATEGORY,
        ENTITY
    }

    private final Kind kind;
    private final String key;
    private MultiplierControl multiplier;
    private Button resetButton;

    EntityRuleEditScreen(
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
        final int controlY = top + (compact ? 28 : 50);
        this.multiplier = new MultiplierControl(
                this.font,
                Component.translatable(this.kind == Kind.CATEGORY
                        ? "smart_resource_drops.gui.entity_category_multiplier"
                        : "smart_resource_drops.gui.entity_override_multiplier"),
                0,
                this.session.maximumMultiplier(),
                this.configuredValue(),
                true,
                Component.translatable("smart_resource_drops.gui.inherit"),
                value -> {
                    this.setConfiguredValue(value);
                    this.refreshControls();
                });
        this.multiplier.setTooltip(Component.translatable(this.kind == Kind.CATEGORY
                ? "smart_resource_drops.gui.entity_category_multiplier_tooltip"
                : "smart_resource_drops.gui.entity_override_multiplier_tooltip"));
        this.multiplier.setPosition(left, controlY, this.contentWidth());
        for (AbstractWidget widget : this.multiplier.widgets()) {
            this.addRenderableWidget(widget);
        }

        final int buttonWidth = Math.min(240, this.contentWidth());
        this.resetButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("smart_resource_drops.gui.reset_override"),
                        button -> {
                            this.setConfiguredValue(null);
                            this.refreshControls();
                        })
                .bounds(
                        (this.width - buttonWidth) / 2,
                        Math.min(this.contentBottom() - 20, controlY + (compact ? 62 : 86)),
                        buttonWidth,
                        20)
                .build());
        this.addBackButton();
        this.refreshControls();
    }

    private void refreshControls() {
        final Integer configured = this.configuredValue();
        final boolean editable = this.session.editable() && this.session.entityDropsEnabled();
        this.multiplier.setValueSilently(configured);
        this.multiplier.setEditable(editable);
        this.resetButton.active = editable && configured != null;
    }

    private Integer configuredValue() {
        return this.kind == Kind.CATEGORY
                ? EntityCategory.parse(this.key)
                        .map(this.session::entityCategoryMultiplier)
                        .orElse(null)
                : this.session.entityMultiplier(this.key);
    }

    private void setConfiguredValue(final Integer value) {
        if (this.kind == Kind.CATEGORY) {
            EntityCategory.parse(this.key).ifPresent(category ->
                    this.session.setEntityCategoryMultiplier(category, value));
        } else {
            this.session.setEntityMultiplier(this.key, value);
        }
    }

    private ConfigEditorSession.EffectiveValue effectiveValue() {
        return this.kind == Kind.CATEGORY
                ? EntityCategory.parse(this.key)
                        .map(this.session::effectiveEntityCategoryValue)
                        .orElseGet(() -> this.session.effectiveEntityCategoryValue(
                                EntityCategory.MISCELLANEOUS))
                : this.session.effectiveEntityValue(this.key);
    }

    private ConfigEditorSession.EffectiveValue inheritedValue() {
        return this.kind == Kind.CATEGORY
                ? EntityCategory.parse(this.key)
                        .map(this.session::inheritedEntityCategoryValue)
                        .orElseGet(() -> this.session.inheritedEntityCategoryValue(
                                EntityCategory.MISCELLANEOUS))
                : this.session.inheritedEntityValue(this.key);
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
            final ConfigEditorSession.EntityInfo info = this.session.entityInfo(this.key).orElse(null);
            graphics.text(
                    this.font,
                    ConfigUiText.fitted(
                            this.font,
                            Component.translatable("smart_resource_drops.gui.registry_id", this.key),
                            this.contentWidth()),
                    left,
                    top,
                    0xFFA0A0A0);
            final String categories = this.session.entityInfo(this.key)
                    .map(entityInfo -> entityInfo.categories().stream()
                            .map(ConfigUiText::entityCategoryName)
                            .map(Component::getString)
                            .collect(Collectors.joining(", ")))
                    .orElse(ConfigUiText.entityCategoryName(
                            EntityCategory.MISCELLANEOUS).getString());
            final Component categoryLine = info != null && info.categoryEstimated()
                    ? Component.translatable(
                            "smart_resource_drops.gui.entity_category_estimated_warning",
                            categories)
                    : Component.translatable(
                            "smart_resource_drops.gui.categories_value",
                            categories);
            graphics.text(
                    this.font,
                    ConfigUiText.fitted(
                            this.font,
                            categoryLine,
                            this.contentWidth()),
                    left,
                    top + 12,
                    0xFFE0E0E0);
        } else {
            final EntityCategory category = EntityCategory.parse(this.key)
                    .orElse(EntityCategory.MISCELLANEOUS);
            graphics.text(
                    this.font,
                    Component.translatable(
                            "smart_resource_drops.gui.entity_category_count",
                            this.session.categoryEntityCount(category)),
                    left,
                    top,
                    0xFFA0A0A0);
            if (category == EntityCategory.BOSSES) {
                graphics.text(
                        this.font,
                        Component.translatable(this.session.bossDropsEnabled()
                                ? "smart_resource_drops.gui.boss_safety_enabled"
                                : "smart_resource_drops.gui.boss_safety_disabled"),
                        left,
                        top + 12,
                        this.session.bossDropsEnabled() ? 0xFFE0E0E0 : 0xFFFFA070);
            }
        }

        final int detailsY = top + (this.height < 220 ? 52 : 78);
        final Integer configured = this.configuredValue();
        final ConfigEditorSession.EffectiveValue effective = this.effectiveValue();
        graphics.text(this.font, ConfigUiText.configured(configured), left, detailsY, 0xFFE0E0E0);
        final boolean estimatedInheritedValue = this.kind == Kind.ENTITY
                && this.session.entityInfo(this.key)
                        .map(ConfigEditorSession.EntityInfo::categoryEstimated)
                        .orElse(true)
                && effective.sourceTier() != ConfigEditorSession.SourceTier.ENTITY;
        graphics.text(
                this.font,
                estimatedInheritedValue
                        ? Component.translatable(
                                "smart_resource_drops.gui.entity_effective_estimated",
                                effective.multiplier())
                        : ConfigUiText.effective(effective.multiplier()),
                left,
                detailsY + 12,
                0xFFFFFFFF);
        final Component inherited = configured == null
                ? EntityCategoryScreen.sourceName(this.inheritedValue())
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

    private static Component titleFor(
            final ConfigEditorSession session,
            final Kind kind,
            final String key
    ) {
        if (kind == Kind.CATEGORY) {
            return EntityCategory.parse(key)
                    .map(ConfigUiText::entityCategoryName)
                    .orElseGet(() -> Component.literal(key));
        }
        return Component.literal(session.entityInfo(key)
                .map(ConfigEditorSession.EntityInfo::displayName)
                .orElse(key));
    }
}
