package com.chedidandrew.smartresourcedrops.client;

import com.chedidandrew.smartresourcedrops.core.entity.EntityCategory;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Scrollable overview of the separate entity-category multiplier hierarchy. */
final class EntityCategoryScreen extends SmartDropsSubScreen {
    private StructuredConfigList list;
    private double preservedScroll;

    EntityCategoryScreen(
            final SmartDropsConfigScreen root,
            final Screen backScreen,
            final ConfigEditorSession session
    ) {
        super(
                Component.translatable("smart_resource_drops.gui.entity_categories"),
                root,
                backScreen,
                session);
    }

    @Override
    protected void init() {
        if (this.list != null) {
            this.preservedScroll = this.list.scrollAmount();
        }
        final int listHeight = Math.max(20, this.contentBottom() - this.contentTop() - 4);
        this.list = this.addRenderableWidget(new StructuredConfigList(
                this.minecraft,
                this.width,
                listHeight,
                this.contentTop(),
                this.contentWidth()));
        this.refreshRows();
        this.list.setScrollAmount(this.preservedScroll);
        this.addBackButton();
    }

    private void refreshRows() {
        final List<StructuredConfigList.Row> rows = new ArrayList<>();
        for (EntityCategory category : this.session.entityCategories()) {
            final Integer configured = this.session.entityCategoryMultiplier(category);
            final ConfigEditorSession.EffectiveValue inherited =
                    this.session.inheritedEntityCategoryValue(category);
            final ConfigEditorSession.EffectiveValue effective =
                    this.session.effectiveEntityCategoryValue(category);
            final long estimatedCount = this.session.estimatedCategoryEntityCount(category);
            final Component safety = category == EntityCategory.BOSSES
                    ? Component.translatable(this.session.bossDropsEnabled()
                            ? "smart_resource_drops.gui.boss_safety_enabled"
                            : "smart_resource_drops.gui.boss_safety_disabled")
                    : Component.translatable(this.session.entityDropsEnabled()
                            ? "smart_resource_drops.gui.entity_rules_active"
                            : "smart_resource_drops.gui.entity_rules_inactive");
            final Component inheritedName = sourceName(inherited);
            final Component tooltip = Component.empty()
                    .append(Component.translatable(
                            "smart_resource_drops.gui.inherited_from",
                            inheritedName))
                    .append(". ")
                    .append(ConfigUiText.effective(effective.multiplier()))
                    .append(estimatedCount > 0
                            ? Component.empty()
                                    .append("\n")
                                    .append(Component.translatable(
                                            "smart_resource_drops.gui.entity_category_estimated_count",
                                            estimatedCount))
                            : Component.empty());
            rows.add(new StructuredConfigList.Row(
                    ConfigUiText.entityCategoryName(category),
                    ConfigUiText.configured(configured),
                    Component.empty()
                            .append(Component.translatable(
                                    "smart_resource_drops.gui.inherited_from",
                                    inheritedName))
                            .append("  ")
                            .append(ConfigUiText.effective(effective.multiplier())),
                    safety,
                    tooltip,
                    () -> this.minecraft.gui.setScreen(new EntityRuleEditScreen(
                            this.root,
                            this,
                            this.session,
                            EntityRuleEditScreen.Kind.CATEGORY,
                            category.key()))));
        }
        this.list.replaceRows(rows);
    }

    static Component sourceName(final ConfigEditorSession.EffectiveValue value) {
        return switch (value.sourceTier()) {
            case ENTITY_DEFAULT -> Component.translatable(
                    "smart_resource_drops.gui.entity_default_source");
            case ENTITY_CATEGORY -> EntityCategory.parse(value.sourceKey())
                    .map(ConfigUiText::entityCategoryName)
                    .orElseGet(() -> Component.literal(value.sourceKey()));
            case ENTITY -> Component.literal(value.sourceKey());
            case GLOBAL -> Component.translatable("smart_resource_drops.gui.global_source");
            case BLOCK, CATEGORY, DIMENSION -> Component.literal(value.sourceKey());
        };
    }
}
