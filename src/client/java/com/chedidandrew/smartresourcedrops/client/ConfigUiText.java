package com.chedidandrew.smartresourcedrops.client;

import java.util.Locale;

import com.chedidandrew.smartresourcedrops.core.entity.EntityCategory;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

final class ConfigUiText {
    private ConfigUiText() {
    }

    static Component categoryName(final String key) {
        return Component.literal(switch (key) {
            case "raw_resource_blocks" -> "Raw Resources";
            case "logs" -> "Logs / Wood";
            default -> titleCase(key);
        });
    }

    static Component dimensionName(final String id) {
        return Component.literal(switch (id) {
            case "minecraft:overworld" -> "Overworld";
            case "minecraft:the_nether" -> "Nether";
            case "minecraft:the_end" -> "End";
            default -> {
                final int separator = id.indexOf(':');
                yield titleCase(separator >= 0 ? id.substring(separator + 1) : id);
            }
        });
    }

    static Component entityCategoryName(final EntityCategory category) {
        return Component.translatable("smart_resource_drops.gui.entity_category." + category.key());
    }

    static Component configured(final Integer value) {
        return Component.translatable(
                "smart_resource_drops.gui.configured",
                value == null
                        ? Component.translatable("smart_resource_drops.gui.inherit")
                        : Component.literal(value + "x"));
    }

    static Component effective(final int value) {
        return Component.translatable("smart_resource_drops.gui.effective", value);
    }

    static Component onOff(final boolean value) {
        return Component.translatable(value
                ? "smart_resource_drops.gui.on"
                : "smart_resource_drops.gui.off");
    }

    static Component fitted(final Font font, final Component component, final int maximumWidth) {
        final String value = component.getString();
        if (font.width(value) <= maximumWidth) {
            return component;
        }
        final String suffix = "...";
        return Component.literal(font.plainSubstrByWidth(
                value,
                Math.max(1, maximumWidth - font.width(suffix))) + suffix);
    }

    static String titleCase(final String raw) {
        final String normalized = raw.toLowerCase(Locale.ROOT).replace('-', '_').replace('/', '_');
        final StringBuilder result = new StringBuilder();
        for (String part : normalized.split("_")) {
            if (part.isEmpty()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return result.toString();
    }
}
