package com.chedidandrew.smartresourcedrops.core.entity;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Immutable category evidence for one entity type. */
public record EntityClassification(
        String entityId,
        List<EntityCategory> matchedCategories,
        EntityCategory selectedCategory,
        Map<EntityCategory, Set<MatchSource>> categorySources,
        Set<String> runtimeTags,
        boolean boss,
        Set<MatchSource> bossSources,
        boolean miscellaneousFallback,
        String reason
) {
    public EntityClassification {
        Objects.requireNonNull(entityId, "entityId");
        Objects.requireNonNull(matchedCategories, "matchedCategories");
        Objects.requireNonNull(selectedCategory, "selectedCategory");
        Objects.requireNonNull(categorySources, "categorySources");
        Objects.requireNonNull(runtimeTags, "runtimeTags");
        Objects.requireNonNull(bossSources, "bossSources");
        Objects.requireNonNull(reason, "reason");

        matchedCategories = List.copyOf(matchedCategories);
        EnumMap<EntityCategory, Set<MatchSource>> immutableSources = new EnumMap<>(EntityCategory.class);
        categorySources.forEach((category, sources) -> immutableSources.put(
                category,
                Collections.unmodifiableSet(new LinkedHashSet<>(sources))));
        categorySources = Collections.unmodifiableMap(immutableSources);
        runtimeTags = Collections.unmodifiableSet(new LinkedHashSet<>(runtimeTags));
        bossSources = Collections.unmodifiableSet(new LinkedHashSet<>(bossSources));
    }

    public enum MatchSource {
        SMART_RESOURCE_DROPS_TAG,
        VANILLA_ENTITY_TYPE_TAG,
        KNOWN_VANILLA_TYPE,
        VANILLA_CLASS,
        MOB_CATEGORY,
        FALLBACK
    }
}
