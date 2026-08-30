package com.chedidandrew.smartresourcedrops.client;

import com.chedidandrew.smartresourcedrops.config.ConfigManager;
import com.chedidandrew.smartresourcedrops.config.ConfigPatch;
import com.chedidandrew.smartresourcedrops.config.ConfigScreenLayout;
import com.chedidandrew.smartresourcedrops.config.ConfigScreenOpenPolicy;
import com.chedidandrew.smartresourcedrops.config.SmartDropsConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Compact General page and root of the staged hierarchical configuration flow. */
public final class SmartDropsConfigScreen extends Screen {
    private final ConfigEditorSession session;
    private ConfigScreenLayout layout;
    private MultiplierControl globalMultiplier;
    private MultiplierControl experienceMultiplier;
    private Button protectionButton;
    private Button sourceButton;
    private Button experienceButton;
    private Button resetButton;
    private Button applyButton;
    private Button doneButton;
    private int generalLabelY;
    private int configurationLabelY;
    private int authorityY;
    private boolean compactLayout;
    private boolean showConfigurationLabel;

    public SmartDropsConfigScreen(
            final Screen parent,
            final SmartDropsConfig snapshot,
            final boolean editable
    ) {
        this(parent, snapshot, editable, "", ConfigScreenOpenPolicy.Authority.CONNECTED_SERVER, 0L);
    }

    SmartDropsConfigScreen(
            final Screen parent,
            final SmartDropsConfig snapshot,
            final boolean editable,
            final String status
    ) {
        this(parent, snapshot, editable, status, 0L);
    }

    SmartDropsConfigScreen(
            final Screen parent,
            final SmartDropsConfig snapshot,
            final boolean editable,
            final String status,
            final long revision
    ) {
        this(parent, snapshot, editable, status, ConfigScreenOpenPolicy.Authority.CONNECTED_SERVER, revision);
    }

    static SmartDropsConfigScreen forLocalDefaults(
            final Screen parent,
            final SmartDropsConfig snapshot,
            final String status
    ) {
        return forLocalDefaults(parent, snapshot, status, ConfigManager.revision());
    }

    static SmartDropsConfigScreen forLocalDefaults(
            final Screen parent,
            final SmartDropsConfig snapshot,
            final String status,
            final long revision
    ) {
        return new SmartDropsConfigScreen(
                parent,
                snapshot,
                true,
                status,
                ConfigScreenOpenPolicy.Authority.LOCAL_DEFAULTS,
                revision);
    }

    private SmartDropsConfigScreen(
            final Screen parent,
            final SmartDropsConfig snapshot,
            final boolean editable,
            final String status,
            final ConfigScreenOpenPolicy.Authority authority,
            final long revision
    ) {
        this(new ConfigEditorSession(parent, snapshot, editable, status, authority, revision));
    }

    private SmartDropsConfigScreen(final ConfigEditorSession session) {
        super(Component.translatable("smart_resource_drops.title"));
        this.session = session;
    }

    /** Test/inspection seam; child screens receive this exact same instance. */
    public ConfigEditorSession editorSession() {
        return this.session;
    }

    /** Test/inspection seam for staged clean/dirty behavior. */
    public Button applyButton() {
        return this.applyButton;
    }

    /** Test/inspection seam for reset availability and confirmation behavior. */
    public Button resetButton() {
        return this.resetButton;
    }

    @Override
    protected void init() {
        this.layout = ConfigScreenLayout.calculate(this.width, this.height);
        final int left = this.layout.contentLeft();
        final int contentWidth = this.layout.contentWidth();
        // The extra reset row needs slightly more vertical room than the original footer.
        // Keep the compact two-column General controls until a full layout can fit cleanly.
        final boolean compact = this.layout.compact() || this.height < 300;
        this.compactLayout = compact;
        final boolean shortWindow = this.height < 220;
        final boolean minimalWindow = this.height < 200;
        this.showConfigurationLabel = !minimalWindow;
        this.authorityY = shortWindow ? 15 : compact ? 18 : 29;
        this.generalLabelY = shortWindow ? 25 : compact ? 31 : 43;
        final int firstRowY = shortWindow ? 34 : this.generalLabelY + 11;
        final int rowPitch = shortWindow ? 22 : compact ? 24 : this.layout.rootRowPitch();

        this.globalMultiplier = new MultiplierControl(
                this.font,
                Component.translatable("smart_resource_drops.gui.global_multiplier"),
                0,
                this.session.maximumMultiplier(),
                this.session.globalMultiplier(),
                value -> {
                    this.session.setGlobalMultiplier(value);
                    this.refreshControlState();
                });
        this.globalMultiplier.setTooltip(Component.translatable(
                "smart_resource_drops.gui.global_multiplier_tooltip"));
        this.globalMultiplier.setPosition(left, firstRowY, contentWidth);
        this.addControl(this.globalMultiplier);

        if (compact) {
            this.addCompactCommonControls(left, firstRowY + rowPitch, rowPitch);
            this.configurationLabelY = firstRowY + rowPitch * 3 + 1;
        } else {
            this.addFullCommonControls(left, contentWidth, firstRowY + rowPitch, rowPitch);
            this.configurationLabelY = firstRowY + rowPitch * 5 + 2;
        }
        this.addNavigation(
                left,
                contentWidth,
                minimalWindow ? firstRowY + rowPitch * 3 : this.configurationLabelY + 10,
                minimalWindow ? 13 : shortWindow ? 16 : compact ? 18 : 20);
        this.addFooter(left);
        this.refreshControlState();
    }

    private void addCompactCommonControls(final int left, final int y, final int rowPitch) {
        final int gap = this.layout.columnGap();
        final int leftWidth = this.layout.leftColumnWidth();
        final int rightX = left + leftWidth + gap;
        this.protectionButton = this.addRenderableWidget(Button.builder(
                        this.protectionLabel(),
                        button -> {
                            this.session.setSmartPlacementProtection(
                                    !this.session.smartPlacementProtection());
                            this.refreshControlState();
                        })
                .tooltip(Tooltip.create(Component.translatable(
                        "smart_resource_drops.gui.placement_protection_tooltip")))
                .bounds(left, y, leftWidth, 20)
                .build());
        this.sourceButton = this.addRenderableWidget(Button.builder(
                        this.sourceLabel(),
                        button -> {
                            this.session.setSourceMode(nextSource(this.session.sourceMode()));
                            this.refreshControlState();
                        })
                .tooltip(Tooltip.create(this.sourceTooltip()))
                .bounds(rightX, y, this.layout.rightColumnWidth(), 20)
                .build());
        this.experienceButton = this.addRenderableWidget(Button.builder(
                        this.experienceLabel(),
                        button -> {
                            this.session.setMultiplyExperience(!this.session.multiplyExperience());
                            this.refreshControlState();
                        })
                .tooltip(Tooltip.create(Component.translatable(
                        "smart_resource_drops.gui.multiply_xp_tooltip")))
                .bounds(left, y + rowPitch, leftWidth, 20)
                .build());
        this.experienceMultiplier = this.createExperienceControl(
                rightX,
                y + rowPitch,
                this.layout.rightColumnWidth());
    }

    private void addFullCommonControls(
            final int left,
            final int contentWidth,
            final int y,
            final int rowPitch
    ) {
        this.protectionButton = this.addRenderableWidget(Button.builder(
                        this.protectionLabel(),
                        button -> {
                            this.session.setSmartPlacementProtection(
                                    !this.session.smartPlacementProtection());
                            this.refreshControlState();
                        })
                .tooltip(Tooltip.create(Component.translatable(
                        "smart_resource_drops.gui.placement_protection_tooltip")))
                .bounds(left, y, contentWidth, 20)
                .build());
        this.sourceButton = this.addRenderableWidget(Button.builder(
                        this.sourceLabel(),
                        button -> {
                            this.session.setSourceMode(nextSource(this.session.sourceMode()));
                            this.refreshControlState();
                        })
                .tooltip(Tooltip.create(this.sourceTooltip()))
                .bounds(left, y + rowPitch, contentWidth, 20)
                .build());
        this.experienceButton = this.addRenderableWidget(Button.builder(
                        this.experienceLabel(),
                        button -> {
                            this.session.setMultiplyExperience(!this.session.multiplyExperience());
                            this.refreshControlState();
                        })
                .tooltip(Tooltip.create(Component.translatable(
                        "smart_resource_drops.gui.multiply_xp_tooltip")))
                .bounds(left, y + rowPitch * 2, contentWidth, 20)
                .build());
        this.experienceMultiplier = this.createExperienceControl(
                left,
                y + rowPitch * 3,
                contentWidth);
    }

    private MultiplierControl createExperienceControl(final int x, final int y, final int width) {
        final MultiplierControl control = new MultiplierControl(
                this.font,
                Component.translatable("smart_resource_drops.gui.xp_multiplier"),
                1,
                this.session.maximumMultiplier(),
                this.session.experienceMultiplier(),
                value -> {
                    this.session.setExperienceMultiplier(value);
                    this.refreshControlState();
                });
        control.setTooltip(Component.translatable("smart_resource_drops.gui.xp_multiplier_tooltip"));
        control.setPosition(x, y, width);
        this.addControl(control);
        return control;
    }

    private void addNavigation(
            final int left,
            final int contentWidth,
            final int y,
            final int buttonHeight
    ) {
        final int gap = 6;
        final int columnWidth = Math.max(1, (contentWidth - gap * 2) / 3);
        final int second = left + columnWidth + gap;
        final int third = second + columnWidth + gap;
        final int secondRow = y + buttonHeight + 2;
        this.addRenderableWidget(Button.builder(
                        Component.translatable("smart_resource_drops.gui.categories"),
                        button -> this.minecraft.gui.setScreen(new RuleListScreen(
                                this, this.session, RuleListScreen.Kind.CATEGORY)))
                .bounds(left, y, columnWidth, buttonHeight).build());
        this.addRenderableWidget(Button.builder(
                        Component.translatable("smart_resource_drops.gui.block_overrides"),
                        button -> this.minecraft.gui.setScreen(new BlockOverridesScreen(this, this.session)))
                .bounds(second, y, columnWidth, buttonHeight).build());
        this.addRenderableWidget(Button.builder(
                        Component.translatable("smart_resource_drops.gui.dimensions"),
                        button -> this.minecraft.gui.setScreen(new RuleListScreen(
                                this, this.session, RuleListScreen.Kind.DIMENSION)))
                .bounds(third, y, columnWidth, buttonHeight).build());
        this.addRenderableWidget(Button.builder(
                        Component.translatable("smart_resource_drops.gui.filters"),
                        button -> this.minecraft.gui.setScreen(new FilterConfigScreen(this, this.session)))
                .bounds(left, secondRow, columnWidth, buttonHeight).build());
        this.addRenderableWidget(Button.builder(
                        Component.translatable("smart_resource_drops.gui.advanced"),
                        button -> this.minecraft.gui.setScreen(new AdvancedConfigScreen(this, this.session)))
                .bounds(second, secondRow, columnWidth, buttonHeight).build());
        this.addRenderableWidget(Button.builder(
                        Component.translatable("smart_resource_drops.gui.entity_drops"),
                        button -> this.minecraft.gui.setScreen(new EntityDropsScreen(this, this.session)))
                .bounds(third, secondRow, columnWidth, buttonHeight).build());
    }

    private void addFooter(final int left) {
        final int y = this.layout.footerBounds().y()
                + Math.max(0, (this.layout.footerBounds().height() - 20) / 2);
        final int leftWidth = this.layout.leftColumnWidth();
        final int resetY = y - (this.height < 220 ? 22 : 24);
        this.resetButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("smart_resource_drops.gui.reset_all"),
                        button -> this.openResetConfirmation())
                .tooltip(Tooltip.create(Component.translatable(
                        "smart_resource_drops.gui.reset_all_tooltip")))
                .bounds(left, resetY, this.layout.contentWidth(), 20)
                .build());
        this.applyButton = this.addRenderableWidget(Button.builder(
                        Component.translatable(this.session.authority()
                                == ConfigScreenOpenPolicy.Authority.LOCAL_DEFAULTS
                                ? "smart_resource_drops.gui.apply_local"
                                : "smart_resource_drops.gui.apply"),
                        button -> this.applyChanges())
                .bounds(left, y, leftWidth, 20).build());
        this.doneButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("smart_resource_drops.gui.done"),
                        button -> this.exitFlow())
                .bounds(
                        left + leftWidth + this.layout.columnGap(),
                        y,
                        this.layout.rightColumnWidth(),
                        20)
                .build());
    }

    private void addControl(final MultiplierControl control) {
        for (AbstractWidget widget : control.widgets()) {
            this.addRenderableWidget(widget);
        }
    }

    private void refreshControlState() {
        if (this.protectionButton == null) {
            return;
        }
        this.protectionButton.setMessage(this.protectionLabel());
        this.sourceButton.setMessage(this.sourceLabel());
        this.sourceButton.setTooltip(Tooltip.create(this.sourceTooltip()));
        this.experienceButton.setMessage(this.experienceLabel());
        final boolean editable = this.session.editable();
        this.globalMultiplier.setEditable(editable);
        this.globalMultiplier.setValueSilently(this.session.globalMultiplier());
        this.protectionButton.active = editable;
        this.sourceButton.active = editable;
        this.experienceButton.active = editable;
        this.experienceMultiplier.setEditable(editable && this.session.multiplyExperience());
        this.experienceMultiplier.setValueSilently(this.session.experienceMultiplier());
        this.resetButton.active = editable;
        this.resetButton.setTooltip(Tooltip.create(Component.translatable(editable
                ? "smart_resource_drops.gui.reset_all_tooltip"
                : "smart_resource_drops.gui.reset_no_permission")));
        this.applyButton.active = editable && this.session.isDirty();
        this.doneButton.setMessage(Component.translatable(this.session.isDirty()
                ? "smart_resource_drops.gui.discard_changes"
                : "smart_resource_drops.gui.done"));
    }

    private void applyChanges() {
        if (!this.session.editable()) {
            this.session.setStatus(Component.translatable(
                    "smart_resource_drops.gui.read_only").getString());
            return;
        }
        if (this.session.authority() == ConfigScreenOpenPolicy.Authority.LOCAL_DEFAULTS
                && (this.minecraft.getConnection() != null || this.minecraft.hasSingleplayerServer())) {
            this.session.setStatus(Component.translatable(
                    "smart_resource_drops.gui.session_changed").getString());
            return;
        }
        if (this.session.authority() == ConfigScreenOpenPolicy.Authority.CONNECTED_SERVER
                && this.minecraft.getConnection() == null) {
            this.session.setStatus(Component.translatable(
                    "smart_resource_drops.gui.not_connected").getString());
            return;
        }
        if (!this.session.belongsToCurrentConnection(this.minecraft)) {
            this.session.setStatus(Component.translatable(
                    "smart_resource_drops.gui.session_changed").getString());
            return;
        }
        final ConfigPatch patch = this.session.buildPatch();
        if (patch.isEmpty()) {
            this.session.setStatus(Component.translatable(
                    "smart_resource_drops.gui.no_changes").getString());
            this.refreshControlState();
            return;
        }
        if (this.session.authority() == ConfigScreenOpenPolicy.Authority.LOCAL_DEFAULTS) {
            if (ConfigManager.applyLocalPatch(patch, this.session.revision())) {
                this.minecraft.gui.setScreen(forLocalDefaults(
                        this.session.originalParent(),
                        ConfigManager.snapshotForClient(),
                        Component.translatable(
                                "smart_resource_drops.gui.local_applied").getString()));
            } else {
                this.session.setStatus(Component.translatable(
                        "smart_resource_drops.gui.local_rejected").getString());
            }
            return;
        }
        this.minecraft.gui.setScreen(new SmartDropsConfigLoadingScreen(
                this,
                this.session.originalParent(),
                patch,
                this.session.revision()));
    }

    private void openResetConfirmation() {
        if (!this.session.editable()) {
            this.session.setStatus(Component.translatable(
                    "smart_resource_drops.gui.reset_no_permission").getString());
            this.refreshControlState();
            return;
        }
        if (!this.session.belongsToCurrentConnection(this.minecraft)) {
            this.session.setStatus(Component.translatable(
                    "smart_resource_drops.gui.session_changed").getString());
            this.refreshControlState();
            return;
        }
        this.minecraft.gui.setScreen(new ResetAllSettingsConfirmScreen(this));
    }

    /** Shared callback for the Cancel button, Escape, and the destructive confirmation. */
    void handleResetConfirmation(final boolean confirmed) {
        if (!confirmed) {
            this.minecraft.gui.setScreen(this);
            return;
        }
        if (!this.session.editable()) {
            this.session.setStatus(Component.translatable(
                    "smart_resource_drops.gui.reset_no_permission").getString());
            this.minecraft.gui.setScreen(this);
            return;
        }

        if (this.session.authority() == ConfigScreenOpenPolicy.Authority.LOCAL_DEFAULTS) {
            if (this.minecraft.getConnection() != null || this.minecraft.hasSingleplayerServer()) {
                this.session.setStatus(Component.translatable(
                        "smart_resource_drops.gui.session_changed").getString());
                this.minecraft.gui.setScreen(this);
                return;
            }
            ClientConfigState.invalidatePendingMutations();
            if (ConfigManager.reset(this.session.revision())) {
                this.minecraft.gui.setScreen(forLocalDefaults(
                        this.session.originalParent(),
                        ConfigManager.snapshotForClient(),
                        Component.translatable(
                                "smart_resource_drops.gui.reset_local_applied").getString(),
                        ConfigManager.revision()));
            } else {
                this.session.setStatus(Component.translatable(
                        "smart_resource_drops.gui.reset_local_rejected").getString());
                this.minecraft.gui.setScreen(this);
            }
            return;
        }

        if (this.minecraft.getConnection() == null) {
            this.session.setStatus(Component.translatable(
                    "smart_resource_drops.gui.not_connected").getString());
            this.minecraft.gui.setScreen(this);
            return;
        }
        if (!this.session.belongsToCurrentConnection(this.minecraft)) {
            this.session.setStatus(Component.translatable(
                    "smart_resource_drops.gui.session_changed").getString());
            this.minecraft.gui.setScreen(this);
            return;
        }
        ClientConfigState.invalidatePendingMutations();
        this.minecraft.gui.setScreen(SmartDropsConfigLoadingScreen.forReset(
                this,
                this.session.originalParent(),
                this.session.revision()));
    }

    private Component protectionLabel() {
        return Component.empty()
                .append(Component.translatable(
                        "smart_resource_drops.gui.placement_protection_label"))
                .append(": ")
                .append(ConfigUiText.onOff(this.session.smartPlacementProtection()));
    }

    private Component sourceLabel() {
        return Component.empty()
                .append(Component.translatable("smart_resource_drops.gui.multiplier_source"))
                .append(": ")
                .append(Component.translatable(sourceTranslationKey(this.session.sourceMode())));
    }

    private Component sourceTooltip() {
        return Component.translatable(switch (this.session.sourceMode()) {
            case NATURAL_ONLY -> "smart_resource_drops.gui.source_natural_tooltip";
            case ALL -> "smart_resource_drops.gui.source_all_tooltip";
            case PLAYER_PLACED_ONLY -> "smart_resource_drops.gui.source_placed_tooltip";
        });
    }

    private Component experienceLabel() {
        return Component.empty()
                .append(Component.translatable("smart_resource_drops.gui.multiply_xp"))
                .append(": ")
                .append(ConfigUiText.onOff(this.session.multiplyExperience()));
    }

    private static SmartDropsConfig.SourceMode nextSource(final SmartDropsConfig.SourceMode source) {
        return switch (source) {
            case NATURAL_ONLY -> SmartDropsConfig.SourceMode.ALL;
            case ALL -> SmartDropsConfig.SourceMode.PLAYER_PLACED_ONLY;
            case PLAYER_PLACED_ONLY -> SmartDropsConfig.SourceMode.NATURAL_ONLY;
        };
    }

    private static String sourceTranslationKey(final SmartDropsConfig.SourceMode source) {
        return switch (source) {
            case NATURAL_ONLY -> "smart_resource_drops.gui.source_natural";
            case ALL -> "smart_resource_drops.gui.source_all";
            case PLAYER_PLACED_ONLY -> "smart_resource_drops.gui.source_placed";
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
        graphics.centeredText(
                this.font,
                this.title,
                this.width / 2,
                this.compactLayout ? 5 : 14,
                0xFFFFFFFF);
        graphics.text(
                this.font,
                Component.translatable("smart_resource_drops.gui.general"),
                this.layout.contentLeft(),
                this.generalLabelY,
                0xFFFFFFFF);
        if (this.showConfigurationLabel) {
            graphics.text(
                    this.font,
                    Component.translatable("smart_resource_drops.gui.configuration"),
                    this.layout.contentLeft(),
                    this.configurationLabelY,
                    0xFFFFFFFF);
        }
        graphics.centeredText(
                this.font,
                ConfigUiText.fitted(this.font, this.authorityLine(), this.layout.contentWidth()),
                this.width / 2,
                this.authorityY,
                this.session.editable() ? 0xFFA0A0A0 : 0xFFFFA0A0);
    }

    private Component authorityLine() {
        if (!this.session.status().isBlank()) {
            return Component.literal(this.session.status());
        }
        if (this.session.authority() == ConfigScreenOpenPolicy.Authority.LOCAL_DEFAULTS) {
            return Component.empty()
                    .append(Component.translatable("smart_resource_drops.gui.local_heading"))
                    .append(" - ")
                    .append(Component.translatable("smart_resource_drops.gui.local_detail"));
        }
        return Component.empty()
                .append(Component.translatable("smart_resource_drops.gui.server_heading"))
                .append(" - ")
                .append(Component.translatable(this.session.editable()
                        ? "smart_resource_drops.gui.server_detail"
                        : "smart_resource_drops.gui.server_read_only_detail"));
    }

    @Override
    public void onClose() {
        this.exitFlow();
    }

    private void exitFlow() {
        if (this.session.authority() == ConfigScreenOpenPolicy.Authority.CONNECTED_SERVER) {
            ClientConfigState.invalidateCachedSnapshot();
        }
        this.minecraft.gui.setScreen(this.session.originalParent());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return this.minecraft != null && this.minecraft.level != null;
    }
}
