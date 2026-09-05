package com.chedidandrew.smartresourcedrops.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A centered, responsive selection list for structured configuration rows.
 * Rows are lightweight list entries rather than child button widgets, so even
 * large registries only render the entries currently visible in the viewport.
 */
public final class StructuredConfigList extends ObjectSelectionList<StructuredConfigList.Entry> {
    public static final int DEFAULT_ROW_HEIGHT = 36;
    public static final int DEFAULT_PREFERRED_ROW_WIDTH = 480;

    private static final int LIST_SIDE_MARGIN = 8;
    private static final int ROW_HORIZONTAL_PADDING = 6;
    private static final int DETAIL_GAP = 8;
    private static final String ELLIPSIS = "\u2026";

    private static final int HOVER_BACKGROUND = 0x20FFFFFF;
    private static final int ROW_SEPARATOR = 0x30000000;
    private static final int PRIMARY_COLOR = 0xFFFFFFFF;
    private static final int SECONDARY_COLOR = 0xFFA0A0A0;
    private static final int DETAIL_COLOR = 0xFFC0C0C0;

    private int preferredRowWidth;
    private List<Row> rows = List.of();

    /**
     * @param screenWidth the full screen width; the list centers itself within it
     */
    public StructuredConfigList(
            final Minecraft minecraft,
            final int screenWidth,
            final int height,
            final int y
    ) {
        this(minecraft, screenWidth, height, y, DEFAULT_PREFERRED_ROW_WIDTH);
    }

    /**
     * @param screenWidth the full screen width; the list centers itself within it
     * @param preferredRowWidth maximum content width before the row is centered
     */
    public StructuredConfigList(
            final Minecraft minecraft,
            final int screenWidth,
            final int height,
            final int y,
            final int preferredRowWidth
    ) {
        super(minecraft, screenWidth, height, y, DEFAULT_ROW_HEIGHT);
        this.preferredRowWidth = Math.max(1, preferredRowWidth);
        updateResponsiveBounds(screenWidth, height, y);
    }

    /** Replaces every lightweight entry, resets selection, and scrolls to top. */
    public void replaceRows(final Collection<Row> newRows) {
        Objects.requireNonNull(newRows, "newRows");
        this.rows = List.copyOf(newRows);
        replaceEntries(rows.stream().map(row -> new Entry(row)).toList());
        setScrollAmount(0.0);
        refreshScrollAmount();
    }

    public List<Row> rows() {
        return rows;
    }

    public int rowCount() {
        return rows.size();
    }

    public void setPreferredRowWidth(final int preferredRowWidth) {
        this.preferredRowWidth = Math.max(1, preferredRowWidth);
        updateSizeAndPosition(getWidth(), getHeight(), getX(), getY());
    }

    public int getPreferredRowWidth() {
        return preferredRowWidth;
    }

    /**
     * Centers the list, retaining an eight-pixel screen margin on compact
     * windows and a small gutter for the vanilla scrollbar on larger windows.
     */
    public void updateResponsiveBounds(final int screenWidth, final int height, final int y) {
        int availableWidth = Math.max(1, screenWidth - LIST_SIDE_MARGIN * 2);
        int listWidth = Math.min(availableWidth, preferredRowWidth + 24);
        int x = (screenWidth - listWidth) / 2;
        updateSizeAndPosition(listWidth, height, x, y);
    }

    @Override
    public int getRowWidth() {
        return Math.max(1, Math.min(preferredRowWidth, getWidth() - 24));
    }

    /**
     * Immutable data for one structured row. Empty components are allowed;
     * {@code tooltip} contains supplemental hover details.
     */
    public record Row(
            Component primary,
            Component secondary,
            Component leftDetail,
            Component rightDetail,
            Component tooltip,
            Runnable action
    ) {
        public Row {
            primary = Objects.requireNonNull(primary, "primary");
            secondary = Objects.requireNonNull(secondary, "secondary");
            leftDetail = Objects.requireNonNull(leftDetail, "leftDetail");
            rightDetail = Objects.requireNonNull(rightDetail, "rightDetail");
            tooltip = Objects.requireNonNull(tooltip, "tooltip");
            action = Objects.requireNonNull(action, "action");
        }

        public Row(
                final Component primary,
                final Component secondary,
                final Component leftDetail,
                final Component rightDetail,
                final Runnable action
        ) {
            this(primary, secondary, leftDetail, rightDetail, Component.empty(), action);
        }
    }

    public final class Entry extends ObjectSelectionList.Entry<Entry> {
        private final Row row;

        private Entry(final Row row) {
            this.row = row;
        }

        public Row row() {
            return row;
        }

        @Override
        public void renderContent(
                final GuiGraphics graphics,
                final int mouseX,
                final int mouseY,
                final boolean hovered,
                final float partialTick
        ) {
            if (hovered) {
                graphics.fill(
                        getX() + 1,
                        getY() + 1,
                        getX() + getWidth() - 1,
                        getY() + getHeight() - 1,
                        HOVER_BACKGROUND);
            }
            graphics.fill(
                    getX() + 1,
                    getY() + getHeight() - 1,
                    getX() + getWidth() - 1,
                    getY() + getHeight(),
                    ROW_SEPARATOR);

            Font font = StructuredConfigList.this.minecraft.font;
            int left = getContentX() + ROW_HORIZONTAL_PADDING;
            int right = getContentRight() - ROW_HORIZONTAL_PADDING;
            int availableWidth = Math.max(0, right - left);
            int lineY = getContentY();

            ClippedText primary = clip(font, row.primary(), availableWidth);
            ClippedText secondary = clip(font, row.secondary(), availableWidth);
            graphics.drawString(font, primary.text(), left, lineY, PRIMARY_COLOR);
            graphics.drawString(font, secondary.text(), left, lineY + 10, SECONDARY_COLOR);

            int detailWidth = row.leftDetail().getString().isEmpty() || row.rightDetail().getString().isEmpty()
                    ? availableWidth
                    : Math.max(0, (availableWidth - DETAIL_GAP) / 2);
            ClippedText leftDetail = clip(font, row.leftDetail(), detailWidth);
            ClippedText rightDetail = clip(font, row.rightDetail(), detailWidth);
            int detailY = lineY + 20;
            graphics.drawString(font, leftDetail.text(), left, detailY, DETAIL_COLOR);
            int rightDetailX = right - font.width(rightDetail.text());
            graphics.drawString(font, rightDetail.text(), rightDetailX, detailY, DETAIL_COLOR);

            boolean truncated = primary.truncated()
                    || secondary.truncated()
                    || leftDetail.truncated()
                    || rightDetail.truncated();
            if (hovered && (!row.tooltip().getString().isEmpty() || truncated)) {
                Component tooltip = composeHoverText(row, truncated);
                if (!tooltip.getString().isEmpty()) {
                    graphics.setTooltipForNextFrame(
                            Tooltip.splitTooltip(StructuredConfigList.this.minecraft, tooltip),
                            mouseX,
                            mouseY);
                }
            }
        }

        @Override
        public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
            if (event.button() != 0) {
                return false;
            }
            activate();
            return true;
        }

        @Override
        public boolean keyPressed(final KeyEvent event) {
            if (event.isSelection()) {
                activate();
                return true;
            }
            return super.keyPressed(event);
        }

        @Override
        public Component getNarration() {
            return Component.translatable("narrator.select", fullRowText());
        }

        private void activate() {
            StructuredConfigList.this.setSelected(this);
            row.action().run();
        }

        private Component fullRowText() {
            MutableComponent text = Component.empty();
            appendNarrationPart(text, row.primary());
            appendNarrationPart(text, row.secondary());
            appendNarrationPart(text, row.leftDetail());
            appendNarrationPart(text, row.rightDetail());
            appendNarrationPart(text, uniqueSupplementalText(row));
            return text;
        }
    }

    /**
     * Builds hover text without hiding clipped row content or repeating the
     * title/description lines that many rows also carry in their supplemental
     * tooltip. Package visibility lets the physical client suites lock this
     * composition contract without exposing it as public API.
     */
    static Component composeHoverText(final Row row, final boolean truncated) {
        Objects.requireNonNull(row, "row");
        final MutableComponent text = Component.empty();
        if (truncated) {
            appendTooltipPart(text, row.primary());
            appendTooltipPart(text, row.secondary());
            appendTooltipPart(text, row.leftDetail());
            appendTooltipPart(text, row.rightDetail());
        }
        appendTooltipPart(text, uniqueSupplementalText(row));
        return text;
    }

    private static Component uniqueSupplementalText(final Row row) {
        final String supplemental = row.tooltip().getString();
        if (supplemental.isBlank()) {
            return Component.empty();
        }

        final Set<String> representedLines = new LinkedHashSet<>();
        collectTooltipLines(representedLines, row.primary());
        collectTooltipLines(representedLines, row.secondary());
        collectTooltipLines(representedLines, row.leftDetail());
        collectTooltipLines(representedLines, row.rightDetail());

        final List<String> sourceLines = supplemental.lines()
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .toList();
        final List<String> uniqueLines = sourceLines.stream()
                .filter(representedLines::add)
                .toList();
        if (uniqueLines.size() == sourceLines.size()) {
            return row.tooltip();
        }
        final MutableComponent unique = Component.empty();
        for (String line : uniqueLines) {
            appendTooltipPart(unique, Component.literal(line));
        }
        return unique;
    }

    private static void collectTooltipLines(
            final Set<String> target,
            final Component component
    ) {
        component.getString().lines()
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .forEach(target::add);
    }

    private static void appendTooltipPart(final MutableComponent target, final Component part) {
        if (part.getString().isEmpty()) {
            return;
        }
        if (!target.getString().isEmpty()) {
            target.append(Component.literal("\n"));
        }
        target.append(part);
    }

    private static void appendNarrationPart(final MutableComponent target, final Component part) {
        if (part.getString().isEmpty()) {
            return;
        }
        if (!target.getString().isEmpty()) {
            target.append(Component.literal(", "));
        }
        target.append(part);
    }

    private static ClippedText clip(final Font font, final Component component, final int maximumWidth) {
        String fullText = component.getString();
        if (fullText.isEmpty() || maximumWidth <= 0) {
            return new ClippedText("", !fullText.isEmpty());
        }
        if (font.width(component) <= maximumWidth) {
            return new ClippedText(fullText, false);
        }

        int ellipsisWidth = font.width(ELLIPSIS);
        if (maximumWidth <= ellipsisWidth) {
            return new ClippedText(font.plainSubstrByWidth(ELLIPSIS, maximumWidth), true);
        }
        return new ClippedText(
                font.plainSubstrByWidth(fullText, maximumWidth - ellipsisWidth) + ELLIPSIS,
                true);
    }

    private record ClippedText(String text, boolean truncated) {
    }
}
