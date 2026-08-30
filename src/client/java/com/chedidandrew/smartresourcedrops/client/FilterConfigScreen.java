package com.chedidandrew.smartresourcedrops.client;

import java.util.ArrayList;
import java.util.List;

import com.chedidandrew.smartresourcedrops.config.SmartDropsConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** Focused exact-block filter editor with read-only visibility for configured tag rules. */
final class FilterConfigScreen extends SmartDropsSubScreen {
    private static final int RESULT_LIMIT = 200;

    private EditBox search;
    private Button modeButton;
    private StructuredConfigList list;
    private int shownRows;
    private int totalRows;
    private String preservedQuery = "";
    private double preservedScroll;

    FilterConfigScreen(
            final SmartDropsConfigScreen root,
            final ConfigEditorSession session
    ) {
        super(
                Component.translatable("smart_resource_drops.gui.filters"),
                root,
                root,
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
        final int modeWidth = Math.min(150, Math.max(92, this.contentWidth() / 3));
        this.modeButton = this.addRenderableWidget(Button.builder(
                        this.modeLabel(),
                        button -> {
                            this.session.setFilterMode(this.session.filterMode()
                                    == SmartDropsConfig.FilterMode.BLACKLIST
                                    ? SmartDropsConfig.FilterMode.WHITELIST
                                    : SmartDropsConfig.FilterMode.BLACKLIST);
                            this.refreshMode();
                            this.refreshRows();
                        })
                .tooltip(Tooltip.create(this.modeTooltip()))
                .bounds(left, top, modeWidth, 20)
                .build());
        this.modeButton.active = this.session.editable();

        final int searchY = top + 26;
        this.search = this.addRenderableWidget(new EditBox(
                this.font,
                left,
                searchY,
                this.contentWidth(),
                20,
                Component.translatable("smart_resource_drops.gui.filters_search")));
        this.search.setHint(Component.translatable("smart_resource_drops.gui.filters_search"));
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
    }

    private void refreshRows() {
        if (this.list == null || this.search == null) {
            return;
        }
        final String query = this.search.getValue();
        final ConfigEditorSession.FilterEntryState activeState = this.activeState();
        final boolean emptyQuery = query.isBlank();
        final List<StructuredConfigList.Row> rows = new ArrayList<>();

        int matchingRows = 0;
        for (ConfigEditorSession.BlockInfo info : this.session.searchFilterBlocks(query)) {
            final ConfigEditorSession.FilterEntryState current = this.session.filterState(info.id());
            if (emptyQuery && current != activeState) {
                continue;
            }
            matchingRows++;
            if (rows.size() >= RESULT_LIMIT) {
                continue;
            }
            final boolean listed = current == activeState;
            rows.add(new StructuredConfigList.Row(
                    Component.literal(info.displayName()),
                    Component.literal(info.id()),
                    Component.literal(listed ? this.modeName() : "Not configured"),
                    this.actionLabel(listed),
                    Component.literal(info.displayName() + "\n" + info.id()),
                    () -> {
                        if (this.session.editable()) {
                            this.session.setFilterState(
                                    info.id(),
                                    listed
                                            ? ConfigEditorSession.FilterEntryState.NONE
                                            : activeState);
                            this.refreshRows();
                        }
                    }));
        }

        for (String tag : this.session.searchFilterTags(query)) {
            final ConfigEditorSession.FilterEntryState state = this.session.tagFilterState(tag);
            if (emptyQuery && state != activeState) {
                continue;
            }
            matchingRows++;
            if (rows.size() >= RESULT_LIMIT) {
                continue;
            }
            rows.add(new StructuredConfigList.Row(
                    Component.literal("#" + tag),
                    Component.translatable("smart_resource_drops.gui.filter_tag_read_only"),
                    Component.literal(state == ConfigEditorSession.FilterEntryState.NONE
                            ? "Not configured"
                            : state == ConfigEditorSession.FilterEntryState.BLACKLIST
                                    ? "Blacklist"
                                    : "Whitelist"),
                    Component.translatable("smart_resource_drops.gui.filter_tag_read_only"),
                    Component.literal("#" + tag),
                    () -> {
                        // The existing ConfigPatch protocol intentionally has no tag-filter edits.
                    }));
        }

        this.totalRows = matchingRows;
        this.shownRows = Math.min(RESULT_LIMIT, this.totalRows);
        this.list.replaceRows(rows);
    }

    private ConfigEditorSession.FilterEntryState activeState() {
        return this.session.filterMode() == SmartDropsConfig.FilterMode.BLACKLIST
                ? ConfigEditorSession.FilterEntryState.BLACKLIST
                : ConfigEditorSession.FilterEntryState.WHITELIST;
    }

    private Component actionLabel(final boolean listed) {
        if (!this.session.editable()) {
            return Component.translatable(
                    "smart_resource_drops.gui.read_only_value",
                    listed
                            ? Component.translatable("smart_resource_drops.gui.filter_remove")
                            : Component.translatable("smart_resource_drops.gui.filter_add"));
        }
        return Component.translatable(listed
                ? "smart_resource_drops.gui.filter_remove"
                : "smart_resource_drops.gui.filter_add");
    }

    private Component modeLabel() {
        return Component.empty()
                .append(Component.translatable("smart_resource_drops.gui.filter_mode"))
                .append(": ")
                .append(Component.literal(this.modeName()));
    }

    private String modeName() {
        return Component.translatable(this.session.filterMode() == SmartDropsConfig.FilterMode.BLACKLIST
                ? "smart_resource_drops.gui.filter_mode_blacklist"
                : "smart_resource_drops.gui.filter_mode_whitelist").getString();
    }

    private Component modeTooltip() {
        return Component.translatable(this.session.filterMode() == SmartDropsConfig.FilterMode.BLACKLIST
                ? "smart_resource_drops.gui.filter_blacklist_tooltip"
                : "smart_resource_drops.gui.filter_whitelist_tooltip");
    }

    @Override
    public void extractRenderState(
            final GuiGraphicsExtractor graphics,
            final int mouseX,
            final int mouseY,
            final float partialTick
    ) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        final Component explanation = Component.translatable(
                this.session.filterMode() == SmartDropsConfig.FilterMode.BLACKLIST
                        ? "smart_resource_drops.gui.filter_blacklist_explanation"
                        : "smart_resource_drops.gui.filter_whitelist_explanation");
        final int left = this.contentLeft();
        final int modeWidth = Math.min(150, Math.max(92, this.contentWidth() / 3));
        graphics.text(
                this.font,
                ConfigUiText.fitted(
                        this.font,
                        explanation,
                        Math.max(1, this.contentWidth() - modeWidth - 8)),
                left + modeWidth + 8,
                this.contentTop() + 6,
                0xFFA0A0A0);

        if (this.totalRows > this.shownRows) {
            graphics.centeredText(
                    this.font,
                    Component.translatable(
                            "smart_resource_drops.gui.blocks_result_limit",
                            this.shownRows),
                    this.width / 2,
                    this.contentTop() + 48,
                    0xFFFFFF80);
        } else if (this.totalRows == 0) {
            graphics.centeredText(
                    this.font,
                    Component.translatable("smart_resource_drops.gui.filter_none"),
                    this.width / 2,
                    Math.min(this.contentBottom() - 16, this.contentTop() + 88),
                    0xFFA0A0A0);
        }
    }
}
