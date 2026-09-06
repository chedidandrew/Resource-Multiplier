package com.chedidandrew.smartresourcedrops.client;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StructuredConfigListTextTest {
    @Test
    void truncatedHoverTextIncludesEachVisibleOrSupplementalLineOnce() {
        final StructuredConfigList.Row row = new StructuredConfigList.Row(
                Component.literal("Entity Drops"),
                Component.literal("Multiply standard death loot."),
                Component.literal("Configured: Inherit"),
                Component.literal("Configure"),
                Component.literal("Entity Drops\nMultiply standard death loot.\nConfigure\nServer controlled."),
                () -> { });

        assertEquals(
                "Entity Drops\nMultiply standard death loot.\nConfigured: Inherit\nConfigure\nServer controlled.",
                StructuredConfigList.composeHoverText(row, true).getString());
    }

    @Test
    void untruncatedHoverTextOnlyIncludesSupplementalLines() {
        final StructuredConfigList.Row row = new StructuredConfigList.Row(
                Component.literal("Entity Drops"),
                Component.literal("Multiply standard death loot."),
                Component.empty(),
                Component.literal("Configure"),
                Component.literal("Entity Drops\nMultiply standard death loot.\nConfigure\nServer controlled."),
                () -> { });

        assertEquals(
                "Server controlled.",
                StructuredConfigList.composeHoverText(row, false).getString());
    }

    @Test
    void narrationDeduplicatesCompositeTooltipFields() {
        final StructuredConfigList.Row row = new StructuredConfigList.Row(
                Component.literal("Entity Drops"),
                Component.literal("Multiply standard death loot."),
                Component.empty(),
                Component.literal("Configure"),
                Component.literal("Entity Drops\nMultiply standard death loot.\nConfigure"),
                () -> { });

        assertEquals(
                "Entity Drops, Multiply standard death loot., Configure",
                StructuredConfigList.composeNarrationText(row).getString());
    }
}
