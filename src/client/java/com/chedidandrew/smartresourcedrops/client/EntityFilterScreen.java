package com.chedidandrew.smartresourcedrops.client;

import com.chedidandrew.smartresourcedrops.config.SmartDropsConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Separate exact-entity and entity-type-tag filter editor. */
final class EntityFilterScreen extends SmartDropsSubScreen {
    private static final int RESULT_LIMIT = 200;

    private EditBox search;
    private Button modeButton;
    private StructuredConfigList list;
    private int shownRows;
    private int totalRows;
    private String preservedQuery = "";
    private double preservedScroll;

    EntityFilterScreen(
            final SmartDropsConfigScreen root,
            final Screen backScreen,
            final ConfigEditorSession session
    ) {
        super(
                Component.translatable("smart_resource_drops.gui.entity_filters"),
                root,
                backScreen,
                session);
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
        final int modeWidth = Math.min(170, Math.max(104, this.contentWidth() / 3));
        this.modeButton = this.addRenderableWidget(Button.builder(
                        this.modeLabel(),
                        button -> {
                            this.session.setEntityFilterMode(this.session.entityFilterMode()
                                    == SmartDropsConfig.FilterMode.BLACKLIST
                                    ? SmartDropsConfig.FilterMode.WHITELIST
                                    : SmartDropsConfig.FilterMode.BLACKLIST);
                            this.refreshMode();
                            this.refreshRows();
                        })
                .tooltip(Tooltip.create(this.modeTooltip()))
                .bounds(left, top, modeWidth, 20)
                .build());
        this.modeButton.active = this.canEditRules();

        final int searchY = top + 26;
        this.search = this.addRenderableWidget(new EditBox(
                this.font,
                left,
                searchY,
                this.contentWidth(),
                20,
                Component.translatable("smart_resource_drops.gui.entity_filters_search")));
        this.search.setHint(Component.translatable("smart_resource_drops.gui.entity_filters_search"));
        this.search.setMaxLength(128);

        final int listY = searchY + 25;
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

    private void refreshMode() {
        this.modeButton.setMessage(this.modeLabel());
        this.modeButton.setTooltip(Tooltip.create(this.modeTooltip()));
        this.modeButton.active = this.canEditRules();
    }

    private void refreshRows() {
        if (this.list == null || this.search == null) {
            return;
        }
        final String query = this.search.getValue();
        final List<StructuredConfigList.Row> rows = new ArrayList<>();
        int matches = 0;

        for (ConfigEditorSession.EntityInfo info : this.session.searchEntityFilterTargets(query)) {
            final ConfigEditorSession.FilterEntryState state =
                    this.session.entityFilterState(info.id());
            matches++;
            if (rows.size() >= RESULT_LIMIT) {
                continue;
            }
            rows.add(new StructuredConfigList.Row(
                    Component.literal(info.displayName()),
                    Component.literal(info.id()),
                    Component.translatable(
                            info.categoryEstimated()
                                    ? "smart_resource_drops.gui.entity_category_estimated"
                                    : "smart_resource_drops.gui.entity_category",
                            ConfigUiText.entityCategoryName(info.selectedCategory())),
                    this.actionLabel(state),
                    info.categoryEstimated()
                            ? Component.empty()
                                    .append(Component.literal(info.displayName() + "\n" + info.id() + "\n"))
                                    .append(Component.translatable(
                                            "smart_resource_drops.gui.entity_category_estimated_tooltip"))
                            : Component.literal(info.displayName() + "\n" + info.id()),
                    () -> {
                        if (this.canEditRules()) {
                            this.session.setEntityFilterState(
                                    info.id(),
                                    state == ConfigEditorSession.FilterEntryState.NONE
                                            ? this.activeState()
                                            : ConfigEditorSession.FilterEntryState.NONE);
                            this.refreshRows();
                        }
                    }));
        }

        for (String tag : this.session.searchEntityFilterTags(query)) {
            final ConfigEditorSession.FilterEntryState state =
                    this.session.entityTagFilterState(tag);
            matches++;
            if (rows.size() >= RESULT_LIMIT) {
                continue;
            }
            rows.add(new StructuredConfigList.Row(
                    Component.literal("#" + tag),
                    Component.translatable("smart_resource_drops.gui.entity_type_tag"),
                    stateName(state),
                    this.actionLabel(state),
                    Component.literal("#" + tag),
                    () -> {
                        if (this.canEditRules()) {
                            this.session.setEntityTagFilterState(
                                    tag,
                                    state == ConfigEditorSession.FilterEntryState.NONE
                                            ? this.activeState()
                                            : ConfigEditorSession.FilterEntryState.NONE);
                            this.refreshRows();
                        }
                    }));
        }

        this.totalRows = matches;
        this.shownRows = Math.min(RESULT_LIMIT, matches);
        this.list.replaceRows(rows);
    }

    private boolean canEditRules() {
        return this.session.editable()
                && (this.session.entityDropsEnabled() || this.session.multiplyMobExperience());
    }

    private ConfigEditorSession.FilterEntryState activeState() {
        return this.session.entityFilterMode() == SmartDropsConfig.FilterMode.BLACKLIST
                ? ConfigEditorSession.FilterEntryState.BLACKLIST
                : ConfigEditorSession.FilterEntryState.WHITELIST;
    }

    private Component actionLabel(final ConfigEditorSession.FilterEntryState state) {
        final Component action = Component.translatable(state == ConfigEditorSession.FilterEntryState.NONE
                ? "smart_resource_drops.gui.filter_add"
                : "smart_resource_drops.gui.filter_remove");
        return this.canEditRules()
                ? action
                : Component.translatable("smart_resource_drops.gui.read_only_value", action);
    }

    private Component modeLabel() {
        return Component.empty()
                .append(Component.translatable("smart_resource_drops.gui.entity_filter_mode"))
                .append(": ")
                .append(Component.literal(this.modeName()));
    }

    private String modeName() {
        return Component.translatable(this.session.entityFilterMode()
                == SmartDropsConfig.FilterMode.BLACKLIST
                ? "smart_resource_drops.gui.filter_mode_blacklist"
                : "smart_resource_drops.gui.filter_mode_whitelist").getString();
    }

    private Component modeTooltip() {
        return Component.translatable(this.session.entityFilterMode()
                == SmartDropsConfig.FilterMode.BLACKLIST
                ? "smart_resource_drops.gui.entity_filter_blacklist_tooltip"
                : "smart_resource_drops.gui.entity_filter_whitelist_tooltip");
    }

    private static Component stateName(final ConfigEditorSession.FilterEntryState state) {
        return Component.translatable(switch (state) {
            case NONE -> "smart_resource_drops.gui.filter_not_configured";
            case WHITELIST -> "smart_resource_drops.gui.filter_mode_whitelist";
            case BLACKLIST -> "smart_resource_drops.gui.filter_mode_blacklist";
        });
    }

    @Override
    public void render(
            final GuiGraphics graphics,
            final int mouseX,
            final int mouseY,
            final float partialTick
    ) {
        super.render(graphics, mouseX, mouseY, partialTick);
        final Component explanation = Component.translatable(this.session.entityFilterMode()
                == SmartDropsConfig.FilterMode.BLACKLIST
                ? "smart_resource_drops.gui.entity_filter_blacklist_explanation"
                : "smart_resource_drops.gui.entity_filter_whitelist_explanation");
        final int left = this.contentLeft();
        final int modeWidth = Math.min(170, Math.max(104, this.contentWidth() / 3));
        graphics.drawString(
                this.font,
                ConfigUiText.fitted(
                        this.font,
                        explanation,
                        Math.max(1, this.contentWidth() - modeWidth - 8)),
                left + modeWidth + 8,
                this.contentTop() + 6,
                0xFFA0A0A0);
        if (!this.session.entityDropsEnabled() && !this.session.multiplyMobExperience()) {
            graphics.drawCenteredString(
                    this.font,
                    Component.translatable("smart_resource_drops.gui.control_inactive"),
                    this.width / 2,
                    this.contentTop() + 48,
                    0xFFB08080);
        } else if (this.totalRows > this.shownRows) {
            graphics.drawCenteredString(
                    this.font,
                    Component.translatable(
                            "smart_resource_drops.gui.entities_result_limit",
                            this.shownRows),
                    this.width / 2,
                    this.contentTop() + 48,
                    0xFFFFFF80);
        } else if (this.totalRows == 0) {
            graphics.drawCenteredString(
                    this.font,
                    Component.translatable("smart_resource_drops.gui.entity_filter_none"),
                    this.width / 2,
                    Math.min(this.contentBottom() - 16, this.contentTop() + 88),
                    0xFFA0A0A0);
        }
    }
}
