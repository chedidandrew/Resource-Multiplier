package com.chedidandrew.smartresourcedrops.config;

import java.util.Set;

/**
 * Narrow read-only registry view used by the validation engine.
 *
 * <p>Calls are made only for configured identifiers and tags.</p>
 */
public interface ConfigRegistryView {
    boolean blockExists(String identifier);

    boolean entityExists(String identifier);

    boolean dimensionExists(String identifier);

    boolean blockTagBound(String identifier);

    boolean entityTagBound(String identifier);

    /**
     * Returns the currently bound members of an entity-type tag.
     *
     * <p>This is deliberately a live, read-only view used by validation. It only
     * contains registry entries that resolved during data-pack loading; callers
     * must not infer unresolved JSON members from an empty result.</p>
     */
    Set<String> entityIdsInTag(String identifier);

    BlockEntityCapability blockEntityCapability(String identifier);

    enum BlockEntityCapability {
        YES,
        NO,
        UNKNOWN
    }
}
