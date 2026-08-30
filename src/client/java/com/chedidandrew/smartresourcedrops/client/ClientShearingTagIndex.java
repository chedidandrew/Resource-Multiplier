package com.chedidandrew.smartresourcedrops.client;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.core.shearing.ShearingTags;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Title-screen fallback for the datapack-extensible shearing certification tags. */
final class ClientShearingTagIndex {
    static final String STANDARD_RESOURCES_ID =
            ShearingTags.STANDARD_RESOURCES.location().toString();
    static final String SPECIAL_ID = ShearingTags.SPECIAL.location().toString();

    private static final String TAG_ROOT = "data/%s/tags/entity_type/%s.json";

    private ClientShearingTagIndex() {
    }

    static Entries load() {
        final Resolver resolver = new Resolver(List.copyOf(FabricLoader.getInstance().getAllMods()));
        return new Entries(
                resolver.resolve(STANDARD_RESOURCES_ID),
                resolver.resolve(SPECIAL_ID));
    }

    record Entries(Set<String> standardResources, Set<String> special) {
        Entries {
            standardResources = Set.copyOf(standardResources);
            special = Set.copyOf(special);
        }

        static Entries empty() {
            return new Entries(Set.of(), Set.of());
        }
    }

    private static final class Resolver {
        private final List<ModContainer> mods;
        private final Map<String, Set<String>> cache = new HashMap<>();
        private final Set<String> resolving = new HashSet<>();

        private Resolver(final List<ModContainer> mods) {
            this.mods = List.copyOf(mods);
        }

        private Set<String> resolve(final String rawTagId) {
            final String tagId = normalizeId(rawTagId);
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
            final LinkedHashSet<String> types = new LinkedHashSet<>();
            try {
                for (Path resource : resources(tagId)) {
                    readValues(resource, types);
                }
            } finally {
                this.resolving.remove(tagId);
            }
            final Set<String> result = Set.copyOf(types);
            this.cache.put(tagId, result);
            return result;
        }

        private List<Path> resources(final String tagId) {
            final int separator = tagId.indexOf(':');
            final String namespace = separator < 0 ? "minecraft" : tagId.substring(0, separator);
            final String value = separator < 0 ? tagId : tagId.substring(separator + 1);
            if (namespace.isBlank() || value.isBlank() || value.contains("..")) {
                return List.of();
            }
            final String relative = TAG_ROOT.formatted(namespace, value);
            final ArrayList<Path> paths = new ArrayList<>();
            for (ModContainer mod : this.mods) {
                mod.findPath(relative).ifPresent(paths::add);
            }
            return paths;
        }

        private void readValues(final Path resource, final LinkedHashSet<String> types) {
            try (Reader reader = Files.newBufferedReader(resource, StandardCharsets.UTF_8)) {
                final JsonElement parsed = JsonParser.parseReader(reader);
                if (!parsed.isJsonObject()) {
                    return;
                }
                final JsonObject object = parsed.getAsJsonObject();
                if (object.has("replace") && object.get("replace").getAsBoolean()) {
                    types.clear();
                }
                if (!object.has("values") || !object.get("values").isJsonArray()) {
                    return;
                }
                for (JsonElement entry : object.getAsJsonArray("values")) {
                    final String id = entryId(entry);
                    if (id.isEmpty()) {
                        continue;
                    }
                    if (id.startsWith("#")) {
                        types.addAll(resolve(id.substring(1)));
                    } else {
                        final String normalized = normalizeId(id);
                        if (!normalized.isEmpty() && !"minecraft:player".equals(normalized)) {
                            types.add(normalized);
                        }
                    }
                }
            } catch (IOException | RuntimeException exception) {
                SmartResourceDrops.LOGGER.warn(
                        "Unable to read client shearing tag resource {}",
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

        private static String normalizeId(final String value) {
            if (value == null) {
                return "";
            }
            final String normalized = value.trim().toLowerCase(Locale.ROOT);
            if (normalized.isEmpty()) {
                return "";
            }
            return normalized.indexOf(':') < 0 ? "minecraft:" + normalized : normalized;
        }
    }
}
