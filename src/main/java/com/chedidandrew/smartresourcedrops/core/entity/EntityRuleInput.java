package com.chedidandrew.smartresourcedrops.core.entity;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** Minecraft-independent facts supplied to the authoritative entity rule engine. */
public record EntityRuleInput(
        String entityId,
        EntityClassification classification,
        Set<String> matchedFilterTags,
        EntityKillAttribution attribution,
        boolean permanentlyExcluded,
        String permanentExclusionReason,
        boolean invokingPlayerWouldQualify
) {
    public EntityRuleInput {
        Objects.requireNonNull(entityId, "entityId");
        Objects.requireNonNull(classification, "classification");
        Objects.requireNonNull(matchedFilterTags, "matchedFilterTags");
        Objects.requireNonNull(attribution, "attribution");
        Objects.requireNonNull(permanentExclusionReason, "permanentExclusionReason");

        entityId = entityId.toLowerCase(Locale.ROOT);
        matchedFilterTags = Collections.unmodifiableSet(new LinkedHashSet<>(matchedFilterTags));
    }
}
