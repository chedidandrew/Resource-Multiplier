package com.chedidandrew.smartresourcedrops.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.chedidandrew.smartresourcedrops.core.Category;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Search-first exact block override browser. */
final class BlockOverridesScreen extends SmartDropsSubScreen {
    static final int RESULT_LIMIT = 200;

    private final String categoryFilter;
    private EditBox search;
    private StructuredConfigList list;
    private int totalMatches;
    private String preservedQuery = "";
    private double preservedScroll;

    BlockOverridesScreen(
            final SmartDropsConfigScreen root,
            final ConfigEditorSession session
    ) {
        this(root, root, session, null);
    }

    BlockOverridesScreen(
            final SmartDropsConfigScreen root,
            final Screen backScreen,
            final ConfigEditorSession session,
            final String categoryFilter
    ) {
        super(
                Component.translatable("smart_resource_drops.gui.block_overrides"),
                root,
                backScreen,
                session);
        this.categoryFilter = Category.parse(categoryFilter).map(Category::key).orElse(null);
    }

    EditBox searchBox() {
        return this.search;
    }

    StructuredConfigList resultList() {
        return this.list;
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
                Component.translatable("smart_resource_drops.gui.blocks_search")));
        this.search.setHint(Component.translatable("smart_resource_drops.gui.blocks_search"));
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
        final List<ConfigEditorSession.BlockInfo> matches;
        if (this.categoryFilter == null) {
            matches = this.session.searchBlocks(query);
            this.totalMatches = matches.size();
        } else {
            final List<ConfigEditorSession.BlockInfo> categoryBlocks =
                    this.session.categoryBlocks(this.categoryFilter);
            final ArrayList<ConfigEditorSession.BlockInfo> limitedMatches = new ArrayList<>();
            int count = 0;
            for (ConfigEditorSession.BlockInfo info : categoryBlocks) {
                if (!query.isEmpty() && !info.searchText().contains(query)) {
                    continue;
                }
                count++;
                if (limitedMatches.size() < RESULT_LIMIT) {
                    limitedMatches.add(info);
                }
            }
            this.totalMatches = count;
            matches = limitedMatches;
        }
        final List<StructuredConfigList.Row> rows = new ArrayList<>();
        for (ConfigEditorSession.BlockInfo info : matches.stream().limit(RESULT_LIMIT).toList()) {
            final ConfigEditorSession.EffectiveValue effective =
                    this.session.effectiveBlockValue(info.id());
            final String category = info.categories().isEmpty()
                    ? Category.MISCELLANEOUS.key()
                    : info.categories().getFirst().key();
            final Component action = Component.translatable(this.session.editable()
                    ? "smart_resource_drops.gui.configure"
                    : "smart_resource_drops.gui.view_details");
            rows.add(new StructuredConfigList.Row(
                    Component.literal(info.displayName()),
                    Component.literal(info.id()),
                    Component.translatable(
                            "smart_resource_drops.gui.block_category",
                            ConfigUiText.categoryName(category)),
                    Component.empty()
                            .append(ConfigUiText.effective(effective.multiplier()))
                            .append("  ")
                            .append(action),
                    Component.literal(info.displayName() + "\n" + info.id()),
                    () -> this.minecraft.gui.setScreen(new RuleEditScreen(
                            this.root,
                            this,
                            this.session,
                            RuleEditScreen.Kind.BLOCK,
                            info.id()))));
        }
        this.list.replaceRows(rows);
    }

    @Override
    public void extractRenderState(
            final GuiGraphicsExtractor graphics,
            final int mouseX,
            final int mouseY,
            final float partialTick
    ) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (this.search == null) {
            return;
        }
        final int y = this.contentTop() + 24;
        final Component hint;
        if (this.categoryFilter != null) {
            hint = Component.empty()
                    .append(ConfigUiText.categoryName(this.categoryFilter))
                    .append(" - ")
                    .append(Component.literal(this.totalMatches + " blocks"));
        } else if (this.search.getValue().isBlank()) {
            hint = Component.translatable("smart_resource_drops.gui.blocks_override_hint");
        } else if (this.totalMatches > RESULT_LIMIT) {
            hint = Component.translatable(
                    "smart_resource_drops.gui.blocks_result_limit",
                    RESULT_LIMIT);
        } else {
            hint = Component.literal(this.totalMatches + " results");
        }
        graphics.centeredText(
                this.font,
                ConfigUiText.fitted(this.font, hint, this.contentWidth()),
                this.width / 2,
                y,
                0xFFA0A0A0);

        if (this.totalMatches == 0) {
            final int centerY = Math.min(this.contentBottom() - 24, y + 42);
            graphics.centeredText(
                    this.font,
                    Component.translatable("smart_resource_drops.gui.blocks_empty"),
                    this.width / 2,
                    centerY,
                    0xFFE0E0E0);
            graphics.centeredText(
                    this.font,
                    Component.translatable("smart_resource_drops.gui.blocks_empty_help"),
                    this.width / 2,
                    centerY + 12,
                    0xFFA0A0A0);
        }
    }
}
