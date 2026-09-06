package com.chedidandrew.smartresourcedrops.client;

import com.chedidandrew.smartresourcedrops.config.SmartDropsConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Advanced staged options that already have an exact ConfigPatch representation. */
final class AdvancedConfigScreen extends SmartDropsSubScreen {
    private static final Component PREVIEW = Component.literal("Preview >");

    private StructuredConfigList optionList;
    private double preservedScroll;

    AdvancedConfigScreen(
            final SmartDropsConfigScreen root,
            final ConfigEditorSession session
    ) {
        this(root, root, session);
    }

    AdvancedConfigScreen(
            final SmartDropsConfigScreen root,
            final Screen backScreen,
            final ConfigEditorSession session
    ) {
        super(
                Component.translatable("smart_resource_drops.gui.advanced"),
                root,
                backScreen,
                session);
    }

    @Override
    protected void init() {
        int listHeight = Math.max(20, contentBottom() - contentTop() - 4);
        optionList = addRenderableWidget(new StructuredConfigList(
                minecraft,
                width,
                listHeight,
                contentTop(),
                Math.min(476, contentWidth())));
        refreshRows();
        optionList.setScrollAmount(preservedScroll);
        addBackButton();
    }

    private void refreshRows() {
        if (optionList == null) {
            return;
        }

        double previousScroll = optionList.scrollAmount();
        List<StructuredConfigList.Row> rows = new ArrayList<>();
        addBooleanRow(
                rows,
                Component.translatable("smart_resource_drops.gui.enabled"),
                Component.translatable("smart_resource_drops.gui.enabled_tooltip"),
                session.enabled(),
                session::setEnabled);
        addBooleanRow(
                rows,
                Component.translatable("smart_resource_drops.gui.player_mining"),
                Component.translatable("smart_resource_drops.gui.player_mining_tooltip"),
                session.playerMining(),
                session::setPlayerMining);
        addBooleanRow(
                rows,
                Component.translatable("smart_resource_drops.gui.explosions"),
                Component.translatable("smart_resource_drops.gui.explosions_tooltip"),
                session.explosions(),
                session::setExplosions);
        addBooleanRow(
                rows,
                Component.translatable("smart_resource_drops.gui.automated_mining"),
                Component.translatable("smart_resource_drops.gui.automated_mining_tooltip"),
                session.automatedMining(),
                session::setAutomatedMining);
        addBooleanRow(
                rows,
                Component.translatable("smart_resource_drops.gui.protect_block_entities"),
                Component.translatable("smart_resource_drops.gui.protect_block_entities_tooltip"),
                session.protectBlockEntities(),
                session::setProtectBlockEntities);
        addBooleanRow(
                rows,
                Component.translatable("smart_resource_drops.gui.piston_safety"),
                Component.translatable("smart_resource_drops.gui.piston_safety_tooltip"),
                session.conservativePistonProtection(),
                session::setConservativePistonProtection);
        addBooleanRow(
                rows,
                Component.translatable("smart_resource_drops.gui.player_overrides"),
                Component.translatable("smart_resource_drops.gui.player_overrides_tooltip"),
                session.allowPlayerOverrides(),
                session::setAllowPlayerOverrides);
        addBooleanRow(
                rows,
                Component.translatable("smart_resource_drops.gui.statistics"),
                Component.translatable("smart_resource_drops.gui.statistics_tooltip"),
                session.statisticsEnabled(),
                session::setStatisticsEnabled);

        addPresetRow(rows, SmartDropsConfig.Preset.VANILLA_PLUS);
        addPresetRow(rows, SmartDropsConfig.Preset.FASTER_SURVIVAL);
        addPresetRow(rows, SmartDropsConfig.Preset.FAST_PROGRESSION);

        optionList.replaceRows(rows);
        optionList.setScrollAmount(previousScroll);
    }

    private void addBooleanRow(
            final List<StructuredConfigList.Row> rows,
            final Component label,
            final Component tooltip,
            final boolean currentValue,
            final BooleanSetter setter
    ) {
        Component displayedValue = ConfigUiText.onOff(currentValue);
        Component rightDetail = session.editable()
                ? displayedValue
                : Component.translatable("smart_resource_drops.gui.read_only_value", displayedValue);
        rows.add(new StructuredConfigList.Row(
                label,
                Component.empty(),
                Component.empty(),
                rightDetail,
                tooltip,
                () -> {
                    if (!session.editable()) {
                        return;
                    }
                    preservedScroll = optionList.scrollAmount();
                    setter.set(!currentValue);
                    // Reinitializing the same screen refreshes every immutable row
                    // without leaving keyboard focus attached to a replaced entry.
                    minecraft.setScreen(this);
                }));
    }

    private void addPresetRow(
            final List<StructuredConfigList.Row> rows,
            final SmartDropsConfig.Preset preset
    ) {
        Component name = PresetPreviewScreen.presetName(preset);
        Component summary = PresetPreviewScreen.presetSummary(preset);
        Component tooltip = Component.empty()
                .append(name)
                .append(Component.literal("\n"))
                .append(summary);
        rows.add(new StructuredConfigList.Row(
                Component.empty(),
                name,
                summary,
                PREVIEW,
                tooltip,
                () -> minecraft.setScreen(new PresetPreviewScreen(
                        root,
                        this,
                        session,
                        preset))));
    }

    @FunctionalInterface
    private interface BooleanSetter {
        boolean set(boolean value);
    }
}
