package com.chedidandrew.smartresourcedrops.client;

import com.chedidandrew.smartresourcedrops.core.entity.EntityCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Search-first, lazy-catalog exact entity-type override browser. */
final class EntityOverridesScreen extends SmartDropsSubScreen {
    static final int RESULT_LIMIT = 200;

    private final EntityCategory categoryFilter;
    private EditBox search;
    private StructuredConfigList list;
    private int totalMatches;
    private String preservedQuery = "";
    private double preservedScroll;

    EntityOverridesScreen(
            final SmartDropsConfigScreen root,
            final Screen backScreen,
            final ConfigEditorSession session
    ) {
        this(root, backScreen, session, null);
    }

    EntityOverridesScreen(
            final SmartDropsConfigScreen root,
            final Screen backScreen,
            final ConfigEditorSession session,
            final EntityCategory categoryFilter
    ) {
        super(
                Component.translatable("smart_resource_drops.gui.entity_overrides"),
                root,
                backScreen,
                session);
        this.categoryFilter = categoryFilter;
    }

    @Override
    protected void init() {
        if (this.search != null) {
            this.preservedQuery = this.search.getValue();
        }
        if (this.list != null) {
            this.preservedScroll = this.list.scrollAmount();
        }
        final int left = this.contentLeft();
        final int top = this.contentTop();
        this.search = this.addRenderableWidget(new EditBox(
                this.font,
                left,
                top,
                this.contentWidth(),
                20,
                Component.translatable("smart_resource_drops.gui.entities_search")));
        this.search.setHint(Component.translatable("smart_resource_drops.gui.entities_search"));
        this.search.setMaxLength(128);

        final int explanationHeight = this.height < 220 ? 11 : 22;
        final int listY = top + 24 + explanationHeight;
        final int listHeight = Math.max(1, this.contentBottom() - listY);
        this.list = this.addRenderableWidget(new StructuredConfigList(
                this.minecraft,
                this.width,
                listHeight,
                listY,
                this.contentWidth()));
        this.search.setResponder(query -> this.refreshRows());
        this.search.setValue(this.preservedQuery);
        this.addBackButton();
        this.refreshRows();
        this.list.setScrollAmount(this.preservedScroll);
        this.setInitialFocus(this.search);
    }

    private void refreshRows() {
        if (this.search == null || this.list == null) {
            return;
        }
        final String query = this.search.getValue().trim().toLowerCase(Locale.ROOT);
        final List<ConfigEditorSession.EntityInfo> matches;
        if (this.categoryFilter == null) {
            matches = this.session.searchEntities(query);
            this.totalMatches = matches.size();
        } else {
            final ArrayList<ConfigEditorSession.EntityInfo> filtered = new ArrayList<>();
            int count = 0;
            for (ConfigEditorSession.EntityInfo info : this.session.categoryEntities(this.categoryFilter)) {
                if (!query.isEmpty() && !info.searchText().contains(query)) {
                    continue;
                }
                count++;
                if (filtered.size() < RESULT_LIMIT) {
                    filtered.add(info);
                }
            }
            this.totalMatches = count;
            matches = filtered;
        }

        final List<StructuredConfigList.Row> rows = new ArrayList<>();
        for (ConfigEditorSession.EntityInfo info : matches.stream().limit(RESULT_LIMIT).toList()) {
            final Integer configured = this.session.entityMultiplier(info.id());
            final ConfigEditorSession.EffectiveValue effective =
                    this.session.effectiveEntityValue(info.id());
            final boolean estimatedInheritedValue = info.categoryEstimated()
                    && effective.sourceTier() != ConfigEditorSession.SourceTier.ENTITY;
            final Component category = info.categoryEstimated()
                    ? Component.translatable(
                            "smart_resource_drops.gui.entity_category_estimated",
                            ConfigUiText.entityCategoryName(info.selectedCategory()))
                    : Component.translatable(
                            "smart_resource_drops.gui.entity_category",
                            ConfigUiText.entityCategoryName(info.selectedCategory()));
            final Component effectiveValue = estimatedInheritedValue
                    ? Component.translatable(
                            "smart_resource_drops.gui.entity_effective_estimated",
                            effective.multiplier())
                    : ConfigUiText.effective(effective.multiplier());
            final Component action = Component.translatable(this.session.editable()
                    ? "smart_resource_drops.gui.configure"
                    : "smart_resource_drops.gui.view_details");
            rows.add(new StructuredConfigList.Row(
                    Component.literal(info.displayName()),
                    Component.literal(info.id()),
                    category,
                    Component.empty()
                            .append(ConfigUiText.configured(configured))
                            .append("  ")
                            .append(effectiveValue)
                            .append("  ")
                            .append(action),
                    info.categoryEstimated()
                            ? Component.empty()
                                    .append(Component.literal(info.displayName() + "\n" + info.id() + "\n"))
                                    .append(Component.translatable(
                                            "smart_resource_drops.gui.entity_category_estimated_tooltip"))
                            : Component.literal(info.displayName() + "\n" + info.id()),
                    () -> this.minecraft.setScreen(new EntityRuleEditScreen(
                            this.root,
                            this,
                            this.session,
                            EntityRuleEditScreen.Kind.ENTITY,
                            info.id()))));
        }
        this.list.replaceRows(rows);
    }

    @Override
    public void render(
            final GuiGraphics graphics,
            final int mouseX,
            final int mouseY,
            final float partialTick
    ) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (this.search == null) {
            return;
        }
        final int y = this.contentTop() + 24;
        final Component hint;
        if (this.categoryFilter != null) {
            hint = Component.translatable(
                    "smart_resource_drops.gui.entity_category_result_count",
                    ConfigUiText.entityCategoryName(this.categoryFilter),
                    this.totalMatches);
        } else if (this.search.getValue().isBlank()) {
            hint = Component.translatable("smart_resource_drops.gui.entities_override_hint");
        } else if (this.totalMatches > RESULT_LIMIT) {
            hint = Component.translatable(
                    "smart_resource_drops.gui.entities_result_limit",
                    RESULT_LIMIT);
        } else {
            hint = Component.translatable(
                    "smart_resource_drops.gui.entities_result_count",
                    this.totalMatches);
        }
        graphics.drawCenteredString(
                this.font,
                ConfigUiText.fitted(this.font, hint, this.contentWidth()),
                this.width / 2,
                y,
                0xFFA0A0A0);
        if (this.totalMatches == 0) {
            final int centerY = Math.min(this.contentBottom() - 24, y + 42);
            graphics.drawCenteredString(
                    this.font,
                    Component.translatable("smart_resource_drops.gui.entities_empty"),
                    this.width / 2,
                    centerY,
                    0xFFE0E0E0);
            graphics.drawCenteredString(
                    this.font,
                    Component.translatable("smart_resource_drops.gui.entities_empty_help"),
                    this.width / 2,
                    centerY + 12,
                    0xFFA0A0A0);
        }
    }
}
