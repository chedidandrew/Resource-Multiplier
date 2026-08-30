package com.chedidandrew.smartresourcedrops.config;

/**
 * Pure responsive layout calculation shared by the hierarchical configuration screens.
 *
 * <p>All measurements are Minecraft logical pixels. The three section bounds intentionally use
 * the same centered content column so individual screens only need to decide what belongs in each
 * section, rather than repeat resolution-specific arithmetic.
 */
public record ConfigScreenLayout(
        int contentLeft,
        int contentWidth,
        Bounds headerBounds,
        Bounds listBounds,
        Bounds footerBounds,
        int columnGap,
        int leftColumnWidth,
        int rightColumnWidth,
        int rootRowHeight,
        int rootRowSpacing,
        boolean compact
) {
    private static final int MAX_CONTENT_WIDTH = 500;
    private static final int HORIZONTAL_MARGIN = 12;
    private static final int COMPACT_HEIGHT_THRESHOLD = 270;

    /** Calculates a layout for a screen's logical (already GUI-scaled) dimensions. */
    public static ConfigScreenLayout calculate(final int width, final int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Screen dimensions must be positive");
        }

        final boolean compact = height < COMPACT_HEIGHT_THRESHOLD;
        final int availableWidth = Math.max(1, width - HORIZONTAL_MARGIN * 2);
        final int contentWidth = Math.min(MAX_CONTENT_WIDTH, availableWidth);
        final int contentLeft = (width - contentWidth) / 2;

        final int edgeMargin = compact ? 6 : 12;
        final int sectionGap = compact ? 4 : 8;
        final int headerHeight = compact ? 22 : 28;
        final int footerHeight = compact ? 24 : 28;

        final Bounds header = new Bounds(contentLeft, edgeMargin, contentWidth, headerHeight);
        final int footerY = Math.max(header.bottom() + sectionGap * 2 + 1, height - edgeMargin - footerHeight);
        final Bounds footer = new Bounds(contentLeft, footerY, contentWidth, footerHeight);
        final int listY = header.bottom() + sectionGap;
        final int listHeight = Math.max(1, footer.y() - sectionGap - listY);
        final Bounds list = new Bounds(contentLeft, listY, contentWidth, listHeight);

        final int columnGap = 8;
        final int columnsWidth = Math.max(0, contentWidth - columnGap);
        final int leftColumnWidth = columnsWidth / 2;
        final int rightColumnWidth = columnsWidth - leftColumnWidth;

        return new ConfigScreenLayout(
                contentLeft,
                contentWidth,
                header,
                list,
                footer,
                columnGap,
                leftColumnWidth,
                rightColumnWidth,
                20,
                compact ? 4 : 6,
                compact);
    }

    public int contentRight() {
        return contentLeft + contentWidth;
    }

    public int rootRowPitch() {
        return rootRowHeight + rootRowSpacing;
    }

    /** Immutable on-screen rectangle, expressed in logical pixels. */
    public record Bounds(int x, int y, int width, int height) {
        public Bounds {
            if (width < 0 || height < 0) {
                throw new IllegalArgumentException("Bounds dimensions cannot be negative");
            }
        }

        public int right() {
            return x + width;
        }

        public int bottom() {
            return y + height;
        }
    }
}
