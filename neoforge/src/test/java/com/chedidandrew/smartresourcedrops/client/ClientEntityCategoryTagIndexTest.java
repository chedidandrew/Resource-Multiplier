package com.chedidandrew.smartresourcedrops.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.chedidandrew.smartresourcedrops.core.entity.EntityCategory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class ClientEntityCategoryTagIndexTest {
    @Test
    void resolvesEveryCategoryWithReplaceObjectsNestedTagsAndCycleSafety() {
        final Map<String, List<String>> resources = Map.of(
                path("categories/neutral"), List.of(
                        """
                        {"replace":false,"values":["minecraft:piglin","minecraft:player"]}
                        """,
                        """
                        {"replace":true,"values":["minecraft:enderman","#test:cycle_a"]}
                        """),
                path("categories/golems"), List.of(
                        """
                        {"values":[{"id":"minecraft:iron_golem","required":false},"minecraft:snow_golem"]}
                        """),
                testPath("cycle_a"), List.of(
                        """
                        {"values":["#test:cycle_b","minecraft:zombified_piglin"]}
                        """),
                testPath("cycle_b"), List.of(
                        """
                        {"values":["#test:cycle_a","minecraft:player"]}
                        """));

        ClientModResources.install(relativePath -> resources
                .getOrDefault(relativePath, List.of())
                .stream()
                .map(json -> resource(relativePath, json))
                .toList());

        final Map<EntityCategory, Set<String>> resolved = ClientEntityCategoryTagIndex.load();

        assertEquals(Set.of(EntityCategory.values()), resolved.keySet());
        assertEquals(
                Set.of("minecraft:enderman", "minecraft:zombified_piglin"),
                resolved.get(EntityCategory.NEUTRAL));
        assertEquals(
                Set.of("minecraft:iron_golem", "minecraft:snow_golem"),
                resolved.get(EntityCategory.GOLEMS));
        assertFalse(resolved.values().stream().anyMatch(values -> values.contains("minecraft:player")));
        assertFalse(resolved.get(EntityCategory.NEUTRAL).contains("minecraft:piglin"));
        assertTrue(resolved.get(EntityCategory.PASSIVE).isEmpty());
    }

    private static ClientModResources.Resource resource(
            final String source,
            final String json
    ) {
        return new ClientModResources.Resource(
                source,
                () -> new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
    }

    private static String path(final String value) {
        return "data/smart_resource_drops/tags/entity_types/" + value + ".json";
    }

    private static String testPath(final String value) {
        return "data/test/tags/entity_types/" + value + ".json";
    }
}
