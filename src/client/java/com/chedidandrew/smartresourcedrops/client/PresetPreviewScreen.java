package com.chedidandrew.smartresourcedrops.client;

import com.chedidandrew.smartresourcedrops.config.SmartDropsConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.FittingMultiLineTextWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Objects;

/** Focused confirmation screen for staging one of the built-in presets. */
final class PresetPreviewScreen extends SmartDropsSubScreen {
    private final SmartDropsConfig.Preset preset;

    private Button stageButton;
    private Button backButton;

    PresetPreviewScreen(
            final SmartDropsConfigScreen root,
            final Screen backScreen,
            final ConfigEditorSession session,
            final SmartDropsConfig.Preset preset
    ) {
        super(
                Component.translatable("smart_resource_drops.gui.preset_preview"),
                root,
                backScreen,
                session);
        this.preset = Objects.requireNonNull(preset, "preset");
        if (preset == SmartDropsConfig.Preset.CUSTOM) {
            throw new IllegalArgumentException("CUSTOM is not a stageable built-in preset");
        }
    }

    @Override
    protected void init() {
        Component warning = Component.translatable("smart_resource_drops.gui.preset_warning")
                .withStyle(ChatFormatting.YELLOW);
        Component preview = Component.empty()
                .append(presetName(preset).copy().withStyle(ChatFormatting.BOLD))
                .append(Component.literal("\n\n"))
                .append(presetSummary(preset))
                .append(Component.literal("\n\n"))
                .append(warning);

        int textHeight = Math.max(20, contentBottom() - contentTop() - 4);
        addRenderableWidget(new FittingMultiLineTextWidget(
                contentLeft(),
                contentTop(),
                contentWidth(),
                textHeight,
                preview,
                font));

        int gap = 8;
        int buttonWidth = Math.min(200, Math.max(1, (contentWidth() - gap) / 2));
        int buttonsWidth = buttonWidth * 2 + gap;
        int buttonLeft = (width - buttonsWidth) / 2;
        stageButton = addRenderableWidget(Button.builder(
                        Component.translatable("smart_resource_drops.gui.stage_preset"),
                        button -> stagePreset())
                .bounds(buttonLeft, footerY(), buttonWidth, 20)
                .tooltip(Tooltip.create(warning))
                .build());
        stageButton.active = session.editable();
        backButton = addRenderableWidget(Button.builder(
                        Component.translatable("smart_resource_drops.gui.back"),
                        button -> onClose())
                .bounds(buttonLeft + buttonWidth + gap, footerY(), buttonWidth, 20)
                .build());
    }

    @Override
    protected boolean supportsInlineUnsavedChangesIndicator() {
        return false;
    }

    @Override
    protected void setInitialFocus() {
        if (stageButton != null && stageButton.active) {
            setInitialFocus(stageButton);
        } else if (backButton != null) {
            setInitialFocus(backButton);
        }
    }

    private void stagePreset() {
        if (!session.editable()) {
            return;
        }
        session.applyPreset(preset);
        minecraft.setScreen(backScreen);
    }

    static Component presetName(final SmartDropsConfig.Preset preset) {
        return Component.translatable(switch (preset) {
            case VANILLA_PLUS -> "smart_resource_drops.gui.preset_vanilla_plus";
            case FASTER_SURVIVAL -> "smart_resource_drops.gui.preset_faster_survival";
            case FAST_PROGRESSION -> "smart_resource_drops.gui.preset_fast_progression";
            case CUSTOM -> "smart_resource_drops.gui.presets";
        });
    }

    static Component presetSummary(final SmartDropsConfig.Preset preset) {
        return Component.translatable(switch (preset) {
            case VANILLA_PLUS -> "smart_resource_drops.gui.preset_vanilla_plus_summary";
            case FASTER_SURVIVAL -> "smart_resource_drops.gui.preset_faster_survival_summary";
            case FAST_PROGRESSION -> "smart_resource_drops.gui.preset_fast_progression_summary";
            case CUSTOM -> "smart_resource_drops.gui.presets";
        });
    }
}
