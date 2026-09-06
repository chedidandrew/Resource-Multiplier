package com.chedidandrew.smartresourcedrops.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Collection;
import java.util.HashSet;
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
    private final boolean legacyExteriorOverlaySuppressed;
    private List<Row> rows = List.of();
    private Component deferredTooltip;
    private int deferredTooltipX;
    private int deferredTooltipY;

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
        super(minecraft, screenWidth, height, y, y + height, DEFAULT_ROW_HEIGHT);
        this.preferredRowWidth = Math.max(1, preferredRowWidth);
        // In 1.19.2 the vanilla selection-list renderer paints opaque dirt
        // strips above and below the list after sibling widgets have rendered.
        // That legacy chrome would cover search fields positioned above this
        // list, so screens render their own background outside the viewport.
        setRenderTopAndBottom(false);
        this.legacyExteriorOverlaySuppressed = true;
        updateResponsiveBounds(screenWidth, height, y);
    }

    /** Replaces every lightweight entry, resets selection, and scrolls to top. */
    public void replaceRows(final Collection<Row> newRows) {
        Objects.requireNonNull(newRows, "newRows");
        this.rows = List.copyOf(newRows);
        replaceEntries(rows.stream().map(row -> new Entry(row)).toList());
        setScrollAmount(0.0);
        setScrollAmount(0.0);
    }

    public List<Row> rows() {
        return rows;
    }

    public int rowCount() {
        return rows.size();
    }

    public void setPreferredRowWidth(final int preferredRowWidth) {
        this.preferredRowWidth = Math.max(1, preferredRowWidth);
        updateResponsiveBounds(this.width, this.y1 - this.y0, this.y0);
    }

    public int getPreferredRowWidth() {
        return preferredRowWidth;
    }

    @Override
    public void render(
            final PoseStack graphics,
            final int mouseX,
            final int mouseY,
            final float partialTick
    ) {
        deferredTooltip = null;
        final ScissorBounds scissor = framebufferScissor(
                this.x0,
                this.y0,
                this.x1,
                this.y1,
                this.minecraft.getWindow().getGuiScale(),
                this.minecraft.getWindow().getHeight());
        RenderSystem.enableScissor(scissor.x(), scissor.y(), scissor.width(), scissor.height());
        try {
            // 1.19.2 renders a row in full when even one pixel intersects the
            // viewport. Scissoring prevents the last partial row from painting
            // its text, separator, or hover state into the footer.
            super.render(graphics, mouseX, mouseY, partialTick);
        } finally {
            RenderSystem.disableScissor();
        }
    }

    /** Converts top-origin GUI coordinates into bottom-origin framebuffer pixels. */
    static ScissorBounds framebufferScissor(
            final int left,
            final int top,
            final int right,
            final int bottom,
            final double guiScale,
            final int framebufferHeight
    ) {
        if (!(guiScale > 0.0) || !Double.isFinite(guiScale)) {
            throw new IllegalArgumentException("guiScale must be finite and positive");
        }
        final int pixelLeft = (int) Math.floor(left * guiScale);
        final int pixelRight = (int) Math.ceil(right * guiScale);
        final int pixelTop = (int) Math.floor(top * guiScale);
        final int pixelBottom = (int) Math.ceil(bottom * guiScale);
        return new ScissorBounds(
                pixelLeft,
                framebufferHeight - pixelBottom,
                Math.max(0, pixelRight - pixelLeft),
                Math.max(0, pixelBottom - pixelTop));
    }

    int viewportLeft() {
        return this.x0;
    }

    int viewportTop() {
        return this.y0;
    }

    int viewportRight() {
        return this.x1;
    }

    int viewportBottom() {
        return this.y1;
    }

    boolean legacyExteriorOverlaySuppressed() {
        return this.legacyExteriorOverlaySuppressed;
    }

    /** Draws the hovered row tooltip after every screen widget has rendered. */
    public void renderDeferredTooltip(final PoseStack graphics) {
        if (deferredTooltip == null) {
            return;
        }
        final Font font = this.minecraft.font;
        if (this.minecraft.screen != null) {
            this.minecraft.screen.renderTooltip(
                    graphics,
                    font.split(
                            deferredTooltip,
                            Math.max(1, Math.min(320, this.width - 16))),
                    deferredTooltipX,
                    deferredTooltipY);
        }
    }

    /**
     * Centers the list, retaining an eight-pixel screen margin on compact
     * windows and a small gutter for the vanilla scrollbar on larger windows.
     */
    public void updateResponsiveBounds(final int screenWidth, final int height, final int y) {
        int availableWidth = Math.max(1, screenWidth - LIST_SIDE_MARGIN * 2);
        int listWidth = Math.min(availableWidth, preferredRowWidth + 24);
        int x = (screenWidth - listWidth) / 2;
        updateSize(listWidth, height, y, y + height);
        setLeftPos(x);
    }

    /** Compatibility name retained for the screens shared with newer Minecraft versions. */
    public double scrollAmount() {
        return getScrollAmount();
    }

    @Override
    public int getRowWidth() {
        return Math.max(1, Math.min(preferredRowWidth, width - 24));
    }

    /**
     * Minecraft 1.20.1 derives the vanilla scrollbar position from the full
     * screen center, ignoring a list moved with {@link #setLeftPos(int)}.
     * Anchor it to this responsive list's actual right edge instead.
     */
    @Override
    protected int getScrollbarPosition() {
        return getRowRight() + 4;
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
        public void render(
                final PoseStack graphics,
                final int index,
                final int rowTop,
                final int rowLeft,
                final int rowWidth,
                final int rowHeight,
                final int mouseX,
                final int mouseY,
                final boolean hovered,
                final float partialTick
        ) {
            if (hovered) {
                net.minecraft.client.gui.GuiComponent.fill(
                        graphics,
                        rowLeft + 1,
                        rowTop + 1,
                        rowLeft + rowWidth - 1,
                        rowTop + rowHeight - 1,
                        HOVER_BACKGROUND);
            }
            net.minecraft.client.gui.GuiComponent.fill(
                    graphics,
                    rowLeft + 1,
                    rowTop + rowHeight - 1,
                    rowLeft + rowWidth - 1,
                    rowTop + rowHeight,
                    ROW_SEPARATOR);

            Font font = StructuredConfigList.this.minecraft.font;
            int left = rowLeft + ROW_HORIZONTAL_PADDING;
            int right = rowLeft + rowWidth - ROW_HORIZONTAL_PADDING;
            int availableWidth = Math.max(0, right - left);
            int lineY = rowTop + 2;

            ClippedText primary = clip(font, row.primary(), availableWidth);
            ClippedText secondary = clip(font, row.secondary(), availableWidth);
            net.minecraft.client.gui.GuiComponent.drawString(graphics, font, primary.text(), left, lineY, PRIMARY_COLOR);
            net.minecraft.client.gui.GuiComponent.drawString(graphics, font, secondary.text(), left, lineY + 10, SECONDARY_COLOR);

            int detailWidth = row.leftDetail().getString().isEmpty() || row.rightDetail().getString().isEmpty()
                    ? availableWidth
                    : Math.max(0, (availableWidth - DETAIL_GAP) / 2);
            ClippedText leftDetail = clip(font, row.leftDetail(), detailWidth);
            ClippedText rightDetail = clip(font, row.rightDetail(), detailWidth);
            int detailY = lineY + 20;
            net.minecraft.client.gui.GuiComponent.drawString(graphics, font, leftDetail.text(), left, detailY, DETAIL_COLOR);
            int rightDetailX = right - font.width(rightDetail.text());
            net.minecraft.client.gui.GuiComponent.drawString(graphics, font, rightDetail.text(), rightDetailX, detailY, DETAIL_COLOR);

            boolean truncated = primary.truncated()
                    || secondary.truncated()
                    || leftDetail.truncated()
                    || rightDetail.truncated();
            if (hovered && (!row.tooltip().getString().isEmpty() || truncated)) {
                final Component tooltip = hoverText(truncated);
                if (!tooltip.getString().isEmpty()) {
                    StructuredConfigList.this.deferredTooltip = tooltip;
                    StructuredConfigList.this.deferredTooltipX = mouseX;
                    StructuredConfigList.this.deferredTooltipY = mouseY;
                }
            }
        }

        @Override
        public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
            if (button != 0) {
                return false;
            }
            activate();
            return true;
        }

        @Override
        public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
            if (keyCode == 257 || keyCode == 335 || keyCode == 32) {
                activate();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public Component getNarration() {
            return Component.translatable("narrator.select", fullRowText());
        }

        private void activate() {
            StructuredConfigList.this.setSelected(this);
            row.action().run();
        }

        /**
         * Builds hover text without hiding clipped row content behind a
         * supplemental tooltip. When the row is truncated, every unclipped row
         * field is shown first and supplemental details follow on new lines.
         */
        private Component hoverText(final boolean truncated) {
            return composeHoverText(row, truncated);
        }

        private Component fullRowText() {
            return composeNarrationText(row);
        }
    }

    static Component composeHoverText(final Row row, final boolean truncated) {
        final MutableComponent text = Component.empty();
        final Set<String> seen = new HashSet<>();
        if (truncated) {
            appendUniquePart(text, seen, row.primary(), "\n");
            appendUniquePart(text, seen, row.secondary(), "\n");
            appendUniquePart(text, seen, row.leftDetail(), "\n");
            appendUniquePart(text, seen, row.rightDetail(), "\n");
        } else {
            rememberPart(seen, row.primary());
            rememberPart(seen, row.secondary());
            rememberPart(seen, row.leftDetail());
            rememberPart(seen, row.rightDetail());
        }
        appendUniquePart(text, seen, row.tooltip(), "\n");
        return text;
    }

    static Component composeNarrationText(final Row row) {
        final MutableComponent text = Component.empty();
        final Set<String> seen = new HashSet<>();
        appendUniquePart(text, seen, row.primary(), ", ");
        appendUniquePart(text, seen, row.secondary(), ", ");
        appendUniquePart(text, seen, row.leftDetail(), ", ");
        appendUniquePart(text, seen, row.rightDetail(), ", ");
        appendUniquePart(text, seen, row.tooltip(), ", ");
        return text;
    }

    private static void rememberPart(final Set<String> seen, final Component part) {
        part.getString().lines()
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .forEach(seen::add);
    }

    private static void appendUniquePart(
            final MutableComponent target,
            final Set<String> seen,
            final Component part,
            final String separator
    ) {
        for (String line : part.getString().lines().map(String::strip).filter(value -> !value.isEmpty()).toList()) {
            if (!seen.add(line)) {
                continue;
            }
            if (!target.getString().isEmpty()) {
                target.append(Component.literal(separator));
            }
            target.append(Component.literal(line));
        }
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

    record ScissorBounds(int x, int y, int width, int height) {
    }
}
