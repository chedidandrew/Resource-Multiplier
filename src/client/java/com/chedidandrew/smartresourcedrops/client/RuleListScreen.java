package com.chedidandrew.smartresourcedrops.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.chedidandrew.smartresourcedrops.core.Category;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/** Scrollable category or dimension overview. */
final class RuleListScreen extends SmartDropsSubScreen {
    enum Kind {
        CATEGORY,
        DIMENSION
    }

    private final Kind kind;
    private EditBox search;
    private StructuredConfigList list;
    private String preservedQuery = "";
    private double preservedScroll;

    RuleListScreen(
            final SmartDropsConfigScreen root,
            final ConfigEditorSession session,
            final Kind kind
    ) {
        super(
                Component.translatable(kind == Kind.CATEGORY
                        ? "smart_resource_drops.gui.categories"
                        : "smart_resource_drops.gui.dimensions"),
                root,
                root,
                session);
        this.kind = kind;
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
                Component.translatable(kind == Kind.CATEGORY
                        ? "smart_resource_drops.gui.categories_search"
                        : "smart_resource_drops.gui.dimensions_search")));
        this.search.setHint(Component.translatable(kind == Kind.CATEGORY
                ? "smart_resource_drops.gui.categories_search"
                : "smart_resource_drops.gui.dimensions_search"));
        this.search.setMaxLength(128);

        final int listY = top + 26;
        final int listHeight = Math.max(1, this.contentBottom() - listY);
        this.list = this.addRenderableWidget(new StructuredConfigList(
                this.minecraft,
                this.width,
                listHeight,
                listY,
                this.contentWidth()));
        this.search.setResponder(value -> this.refreshRows());
        this.search.setValue(this.preservedQuery);
        this.addBackButton();
        this.refreshRows();
        this.list.setScrollAmount(this.preservedScroll);
        this.setInitialFocus(this.search);
    }

    private void refreshRows() {
        if (this.list == null || this.search == null) {
            return;
        }
        final String query = this.search.getValue().trim().toLowerCase(Locale.ROOT);
        final List<StructuredConfigList.Row> rows = new ArrayList<>();
        if (this.kind == Kind.CATEGORY) {
            for (Category category : this.session.categories()) {
                final String key = category.key();
                final Component name = ConfigUiText.categoryName(key);
                if (!query.isEmpty()
                        && !key.contains(query)
                        && !name.getString().toLowerCase(Locale.ROOT).contains(query)) {
                    continue;
                }
                final Integer configured = this.session.categoryMultiplier(key);
                final ConfigEditorSession.EffectiveValue effective = this.session.effectiveCategoryValue(key);
                final Component action = Component.translatable(this.session.editable()
                        ? "smart_resource_drops.gui.configure"
                        : "smart_resource_drops.gui.view_details");
                rows.add(new StructuredConfigList.Row(
                        name,
                        ConfigUiText.configured(configured),
                        ConfigUiText.effective(effective.multiplier()),
                        action,
                        Component.literal(key),
                        () -> this.minecraft.gui.setScreen(new RuleEditScreen(
                                this.root,
                                this,
                                this.session,
                                RuleEditScreen.Kind.CATEGORY,
                                key))));
            }
        } else {
            for (String id : this.session.dimensionIds()) {
                final Component name = ConfigUiText.dimensionName(id);
                if (!query.isEmpty()
                        && !id.contains(query)
                        && !name.getString().toLowerCase(Locale.ROOT).contains(query)) {
                    continue;
                }
                final Integer configured = this.session.dimensionMultiplier(id);
                final ConfigEditorSession.EffectiveValue effective = this.session.effectiveDimensionValue(id);
                final Component action = Component.translatable(this.session.editable()
                        ? "smart_resource_drops.gui.configure"
                        : "smart_resource_drops.gui.view_details");
                rows.add(new StructuredConfigList.Row(
                        name,
                        Component.literal(id),
                        ConfigUiText.configured(configured),
                        Component.empty().append(ConfigUiText.effective(effective.multiplier()))
                                .append("  ")
                                .append(action),
                        Component.literal(id),
                        () -> this.minecraft.gui.setScreen(new RuleEditScreen(
                                this.root,
                                this,
                                this.session,
                                RuleEditScreen.Kind.DIMENSION,
                                id))));
            }
        }
        this.list.replaceRows(rows);
    }
}
