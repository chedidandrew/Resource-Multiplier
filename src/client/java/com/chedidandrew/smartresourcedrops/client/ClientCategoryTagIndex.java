package com.chedidandrew.smartresourcedrops.client;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.core.Category;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves the category tag data bundled by Minecraft and installed mods for title-screen use.
 *
 * <p>Registry tag bindings are supplied by a world/server and are therefore empty on the title
 * screen. The local-default editor is available there, so relying only on {@code BlockState#is}
 * makes every category appear empty. This small resolver follows the same data-pack tag JSON
 * graph directly from installed mod resources. Live registry bindings are still authoritative
 * whenever they are available; this index is a client-side discovery fallback only.</p>
 */
final class ClientCategoryTagIndex {
    private static final String CATEGORY_NAMESPACE = SmartResourceDrops.MOD_ID;
    private static final String TAG_ROOT = "data/%s/tags/block/%s.json";

    private ClientCategoryTagIndex() {
    }

    static Map<Category, Set<String>> load() {
        final Resolver resolver = new Resolver();
        final EnumMap<Category, Set<String>> result = new EnumMap<>(Category.class);
        for (Category category : Category.values()) {
            if (category == Category.MISCELLANEOUS) {
                result.put(category, Set.of());
                continue;
            }
            result.put(category, resolver.resolve(
                    CATEGORY_NAMESPACE + ":categories/" + category.key()));
        }
        return result;
    }

    private static final class Resolver {
        private final Map<String, Set<String>> cache = new HashMap<>();
        private final Set<String> resolving = new HashSet<>();

        private Set<String> resolve(final String rawTagId) {
            final String tagId = normalizeTagId(rawTagId);
            if (tagId.isEmpty()) {
                return Set.of();
            }
            final Set<String> cached = this.cache.get(tagId);
            if (cached != null) {
                return cached;
            }
            if (!this.resolving.add(tagId)) {
                return Set.of();
            }

            final LinkedHashSet<String> blocks = new LinkedHashSet<>();
            try {
                for (ClientModResources.Resource resource : this.resources(tagId)) {
                    this.readValues(resource, blocks);
                }
            } finally {
                this.resolving.remove(tagId);
            }
            final Set<String> resolved = Set.copyOf(blocks);
            this.cache.put(tagId, resolved);
            return resolved;
        }

        private List<ClientModResources.Resource> resources(final String tagId) {
            final int separator = tagId.indexOf(':');
            final String namespace = separator < 0 ? "minecraft" : tagId.substring(0, separator);
            final String value = separator < 0 ? tagId : tagId.substring(separator + 1);
            if (namespace.isBlank() || value.isBlank() || value.contains("..")) {
                return List.of();
            }
            final String relative = TAG_ROOT.formatted(namespace, value);
            return ClientModResources.findAll(relative);
        }

        private void readValues(
                final ClientModResources.Resource resource,
                final LinkedHashSet<String> blocks
        ) {
            try (Reader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                final JsonElement parsed = JsonParser.parseReader(reader);
                if (!parsed.isJsonObject()) {
                    return;
                }
                final JsonObject object = parsed.getAsJsonObject();
                if (object.has("replace") && object.get("replace").getAsBoolean()) {
                    blocks.clear();
                }
                if (!object.has("values") || !object.get("values").isJsonArray()) {
                    return;
                }
                for (JsonElement entry : object.getAsJsonArray("values")) {
                    final String id = entryId(entry);
                    if (id.isEmpty()) {
                        continue;
                    }
                    if (id.charAt(0) == '#') {
                        blocks.addAll(this.resolve(id.substring(1)));
                    } else {
                        blocks.add(normalizeBlockId(id));
                    }
                }
            } catch (IOException | RuntimeException exception) {
                SmartResourceDrops.LOGGER.warn(
                        "Unable to read client category tag resource {}",
                        resource,
                        exception);
            }
        }

        private static String entryId(final JsonElement entry) {
            if (entry.isJsonPrimitive() && entry.getAsJsonPrimitive().isString()) {
                return entry.getAsString();
            }
            if (entry.isJsonObject()) {
                final JsonObject object = entry.getAsJsonObject();
                if (object.has("id") && object.get("id").isJsonPrimitive()) {
                    return object.get("id").getAsString();
                }
            }
            return "";
        }

        private static String normalizeTagId(final String value) {
            if (value == null) {
                return "";
            }
            final String trimmed = value.trim().toLowerCase(java.util.Locale.ROOT);
            return trimmed.indexOf(':') < 0 ? "minecraft:" + trimmed : trimmed;
        }

        private static String normalizeBlockId(final String value) {
            return normalizeTagId(value);
        }
    }
}
