package com.chedidandrew.smartresourcedrops.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntFunction;

/**
 * A compact, reusable {@code [-] value [+]} control made entirely from
 * vanilla widgets. A nullable value represents inheritance; integer zero is
 * always treated as an ordinary value.
 */
public final class MultiplierControl implements LayoutElement {
    private static final int HEIGHT = Button.DEFAULT_HEIGHT;
    private static final int BUTTON_WIDTH = 20;
    private static final int VALUE_WIDTH = 48;
    private static final int GAP = 2;
    private static final int LABEL_GAP = 8;
    private static final int CONTROL_WIDTH = BUTTON_WIDTH + GAP + VALUE_WIDTH + GAP + BUTTON_WIDTH;

    private final Font font;
    private final int minimum;
    private final int maximum;
    private final boolean allowInherit;
    private final Component inheritLabel;
    private final IntFunction<Component> valueFormatter;
    private final Consumer<Integer> onChanged;
    private final StringWidget labelWidget;
    private final Button decrementButton;
    private final StringWidget valueWidget;
    private final Button incrementButton;
    private final List<AbstractWidget> widgets;

    private @Nullable Integer value;
    private boolean editable = true;
    private int x;
    private int y;
    private int width = CONTROL_WIDTH;

    /**
     * Creates a non-null multiplier control whose values are rendered as
     * {@code 2x}, {@code 3x}, and so on.
     */
    public MultiplierControl(
            final Font font,
            final Component label,
            final int minimum,
            final int maximum,
            final int initialValue,
            final Consumer<Integer> onChanged
    ) {
        this(
                font,
                label,
                minimum,
                maximum,
                initialValue,
                false,
                Component.literal("Inherit"),
                value -> Component.literal(value + "x"),
                onChanged);
    }

    /**
     * Creates a control that may optionally use {@code null} for inheritance.
     * Decrementing the minimum value enters the inherit state; incrementing
     * from inherit returns to the minimum value.
     */
    public MultiplierControl(
            final Font font,
            final Component label,
            final int minimum,
            final int maximum,
            final @Nullable Integer initialValue,
            final boolean allowInherit,
            final Component inheritLabel,
            final Consumer<Integer> onChanged
    ) {
        this(
                font,
                label,
                minimum,
                maximum,
                initialValue,
                allowInherit,
                inheritLabel,
                value -> Component.literal(value + "x"),
                onChanged);
    }

    /**
     * Creates a control with a caller-provided formatter for concrete values.
     * The change consumer receives {@code null} only when inheritance is
     * enabled.
     */
    public MultiplierControl(
            final Font font,
            final Component label,
            final int minimum,
            final int maximum,
            final @Nullable Integer initialValue,
            final boolean allowInherit,
            final Component inheritLabel,
            final IntFunction<Component> valueFormatter,
            final Consumer<Integer> onChanged
    ) {
        this.font = Objects.requireNonNull(font, "font");
        if (minimum > maximum) {
            throw new IllegalArgumentException("minimum must not exceed maximum");
        }
        if (!allowInherit && initialValue == null) {
            throw new IllegalArgumentException("a non-nullable control requires an initial value");
        }

        this.minimum = minimum;
        this.maximum = maximum;
        this.allowInherit = allowInherit;
        this.inheritLabel = Objects.requireNonNull(inheritLabel, "inheritLabel");
        this.valueFormatter = Objects.requireNonNull(valueFormatter, "valueFormatter");
        this.onChanged = Objects.requireNonNull(onChanged, "onChanged");
        this.value = normalize(initialValue);

        this.labelWidget = new StringWidget(Objects.requireNonNull(label, "label"), font);
        this.decrementButton = Button.builder(Component.literal("-"), button -> step(-1))
                .size(BUTTON_WIDTH, HEIGHT)
                .build();
        this.valueWidget = new StringWidget(VALUE_WIDTH, HEIGHT, Component.empty(), font)
                .setMaxWidth(VALUE_WIDTH);
        this.incrementButton = Button.builder(Component.literal("+"), button -> step(1))
                .size(BUTTON_WIDTH, HEIGHT)
                .build();
        this.widgets = List.of(labelWidget, decrementButton, valueWidget, incrementButton);

        refreshLabels();
        repositionWidgets();
    }

    public @Nullable Integer getValue() {
        return value;
    }

    /**
     * Sets a value and invokes the change callback only when the normalized
     * value is different from the current value.
     *
     * @return whether the value actually changed
     */
    public boolean setValue(final @Nullable Integer newValue) {
        return setValueInternal(newValue, true);
    }

    /** Sets a value without invoking the change callback. */
    public void setValueSilently(final @Nullable Integer newValue) {
        setValueInternal(newValue, false);
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(final boolean editable) {
        this.editable = editable;
        refreshButtonStates();
    }

    /** Refreshes the formatted value and both boundary button states. */
    public void refreshLabels() {
        valueWidget.setMessage(value == null ? inheritLabel : valueFormatter.apply(value));
        refreshButtonStates();
        repositionWidgets();
    }

    /** Applies one tooltip to the label, value, and both adjustment buttons. */
    public void setTooltip(final @Nullable Component message) {
        Tooltip tooltip = message == null ? null : Tooltip.create(message);
        for (AbstractWidget widget : widgets) {
            widget.setTooltip(tooltip);
        }
    }

    /** Returns the vanilla widgets in keyboard traversal order. */
    public List<AbstractWidget> widgets() {
        return widgets;
    }

    /** Repositions the entire control and changes its available width. */
    public void setPosition(final int x, final int y, final int width) {
        this.x = x;
        this.y = y;
        this.width = Math.max(CONTROL_WIDTH, width);
        repositionWidgets();
    }

    public void setWidth(final int width) {
        this.width = Math.max(CONTROL_WIDTH, width);
        repositionWidgets();
    }

    @Override
    public void setX(final int x) {
        this.x = x;
        repositionWidgets();
    }

    @Override
    public void setY(final int y) {
        this.y = y;
        repositionWidgets();
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void visitWidgets(final Consumer<AbstractWidget> widgetVisitor) {
        widgets.forEach(widgetVisitor);
    }

    private void step(final int direction) {
        if (!editable) {
            return;
        }

        Integer next;
        if (value == null) {
            next = direction > 0 ? minimum : null;
        } else if (direction < 0 && allowInherit && value == minimum) {
            next = null;
        } else {
            long candidate = (long)value + direction;
            next = (int)Math.max(minimum, Math.min(maximum, candidate));
        }
        setValueInternal(next, true);
    }

    private boolean setValueInternal(final @Nullable Integer newValue, final boolean notify) {
        Integer normalized = normalize(newValue);
        if (Objects.equals(value, normalized)) {
            return false;
        }

        value = normalized;
        refreshLabels();
        if (notify) {
            onChanged.accept(value);
        }
        return true;
    }

    private @Nullable Integer normalize(final @Nullable Integer candidate) {
        if (candidate == null) {
            if (!allowInherit) {
                throw new IllegalArgumentException("inherit is not enabled for this control");
            }
            return null;
        }
        return Math.max(minimum, Math.min(maximum, candidate));
    }

    private void refreshButtonStates() {
        decrementButton.active = editable && (value != null) && (allowInherit || value > minimum);
        incrementButton.active = editable && (value == null || value < maximum);
    }

    private void repositionWidgets() {
        boolean hasLabel = !labelWidget.getMessage().getString().isEmpty();
        int controlX = hasLabel ? x + width - CONTROL_WIDTH : x + (width - CONTROL_WIDTH) / 2;
        int labelWidth = Math.max(0, controlX - x - LABEL_GAP);

        labelWidget.visible = hasLabel && labelWidth > 0;
        labelWidget.setMaxWidth(labelWidth);
        labelWidget.setPosition(x, y + (HEIGHT - labelWidget.getHeight()) / 2);

        decrementButton.setPosition(controlX, y);
        int valueAreaX = controlX + BUTTON_WIDTH + GAP;
        int renderedValueWidth = Math.min(VALUE_WIDTH, font.width(valueWidget.getMessage()));
        valueWidget.setPosition(valueAreaX + (VALUE_WIDTH - renderedValueWidth) / 2, y);
        incrementButton.setPosition(valueAreaX + VALUE_WIDTH + GAP, y);
    }
}
