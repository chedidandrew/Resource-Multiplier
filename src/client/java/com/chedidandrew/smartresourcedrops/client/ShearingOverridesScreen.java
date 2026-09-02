package com.chedidandrew.smartresourcedrops.client;

import com.chedidandrew.smartresourcedrops.core.shearing.ShearingClassification;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Search-first exact override browser for certified standard-resource shearables. */
final class ShearingOverridesScreen extends SmartDropsSubScreen {
    static final int RESULT_LIMIT = 200;

    private EditBox search;
    private StructuredConfigList list;
    private int totalMatches;
    private String preservedQuery = "";
    private double preservedScroll;

    ShearingOverridesScreen(
            final SmartDropsConfigScreen root,
            final Screen backScreen,
            final ConfigEditorSession session
    ) {
        super(
                Component.translatable("smart_resource_drops.gui.shearing_overrides"),
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
        this.search = this.addRenderableWidget(new EditBox(
                this.font,
                left,
                top,
                this.contentWidth(),
                20,
                Component.translatable("smart_resource_drops.gui.shearing_search")));
        this.search.setHint(Component.translatable("smart_resource_drops.gui.shearing_search"));
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
        final List<ConfigEditorSession.ShearingInfo> matches =
                this.session.searchShearingEntities(query);
        this.totalMatches = matches.size();

        final List<StructuredConfigList.Row> rows = new ArrayList<>();
        for (ConfigEditorSession.ShearingInfo info : matches.stream()
                .limit(RESULT_LIMIT)
                .toList()) {
            final Integer configured = this.session.shearingEntityMultiplier(info.id());
            final boolean standard = info.classification()
                    == ShearingClassification.STANDARD_RESOURCE;
            final Component effective = standard
                    ? ConfigUiText.effective(this.session.effectiveShearingMultiplier(info.id()))
                    : Component.translatable("smart_resource_drops.gui.shearing_fixed_vanilla");
            final Component action = standard
                    ? Component.translatable(this.session.editable()
                            ? "smart_resource_drops.gui.configure"
                            : "smart_resource_drops.gui.view_details")
                    : Component.translatable("smart_resource_drops.gui.shearing_override_unavailable");
            rows.add(new StructuredConfigList.Row(
                    Component.literal(info.displayName()),
                    Component.literal(info.id()),
                    Component.translatable(
                            "smart_resource_drops.gui.shearing_classification_value",
                            ShearingRuleEditScreen.classificationName(info.classification())),
                    Component.empty()
                            .append(ConfigUiText.configured(configured))
                            .append("  ")
                            .append(effective)
                            .append("  ")
                            .append(action),
                    tooltipFor(info),
                    standard
                            ? () -> this.minecraft.setScreen(new ShearingRuleEditScreen(
                                    this.root,
                                    this,
                                    this.session,
                                    ShearingRuleEditScreen.Kind.ENTITY,
                                    info.id()))
                            : () -> { }));
        }
        this.list.replaceRows(rows);
    }

    private static Component tooltipFor(final ConfigEditorSession.ShearingInfo info) {
        final Component base = Component.literal(info.displayName() + "\n" + info.id() + "\n")
                .append(Component.translatable(
                        "smart_resource_drops.gui.shearing_classification_value",
                        ShearingRuleEditScreen.classificationName(info.classification())));
        final Component explanation = switch (info.classification()) {
            case STANDARD_RESOURCE -> Component.translatable(
                    "smart_resource_drops.gui.shearing_standard_tooltip");
            case SPECIAL -> Component.translatable(
                    "smart_resource_drops.gui.shearing_special_tooltip");
            case UNKNOWN -> Component.translatable(
                    "smart_resource_drops.gui.shearing_unknown_tooltip",
                    ClientShearingTagIndex.STANDARD_RESOURCES_ID);
        };
        final var tooltip = base.copy().append("\n").append(explanation);
        if (info.tagConflict()) {
            tooltip.append("\n").append(Component.translatable(
                    "smart_resource_drops.gui.shearing_tag_conflict_tooltip"));
        }
        return tooltip;
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
        if (this.search.getValue().isBlank()) {
            hint = Component.translatable("smart_resource_drops.gui.shearing_override_hint");
        } else if (this.totalMatches > RESULT_LIMIT) {
            hint = Component.translatable(
                    "smart_resource_drops.gui.shearing_result_limit",
                    RESULT_LIMIT);
        } else {
            hint = Component.translatable(
                    "smart_resource_drops.gui.shearing_result_count",
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
            final boolean emptyQuery = this.search.getValue().isBlank();
            graphics.drawCenteredString(
                    this.font,
                    Component.translatable(emptyQuery
                            ? "smart_resource_drops.gui.shearing_empty"
                            : "smart_resource_drops.gui.shearing_no_matches"),
                    this.width / 2,
                    centerY,
                    0xFFE0E0E0);
            graphics.drawCenteredString(
                    this.font,
                    Component.translatable(emptyQuery
                            ? "smart_resource_drops.gui.shearing_empty_help"
                            : "smart_resource_drops.gui.shearing_no_matches_help"),
                    this.width / 2,
                    centerY + 12,
                    0xFFA0A0A0);
        }
    }
}
