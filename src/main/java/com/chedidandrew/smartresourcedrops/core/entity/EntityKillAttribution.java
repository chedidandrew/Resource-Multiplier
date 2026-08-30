package com.chedidandrew.smartresourcedrops.core.entity;

import java.util.Objects;
import java.util.UUID;

/** The non-blocking, death-time player attribution relevant to entity rules. */
public record EntityKillAttribution(Kind kind, UUID playerId, boolean vanillaPlayerKilled) {
    public EntityKillAttribution {
        Objects.requireNonNull(kind, "kind");
        if (kind == Kind.NONE && playerId != null) {
            throw new IllegalArgumentException("An unattributed death cannot have a player id");
        }
        if (kind != Kind.NONE && playerId == null) {
            throw new IllegalArgumentException("A player-attributed death must have a player id");
        }
    }

    public static EntityKillAttribution none(boolean vanillaPlayerKilled) {
        return new EntityKillAttribution(Kind.NONE, null, vanillaPlayerKilled);
    }

    public static EntityKillAttribution direct(UUID playerId, boolean vanillaPlayerKilled) {
        return new EntityKillAttribution(Kind.DIRECT_PLAYER, playerId, vanillaPlayerKilled);
    }

    public static EntityKillAttribution tamed(UUID playerId, boolean vanillaPlayerKilled) {
        return new EntityKillAttribution(Kind.TAMED_ENTITY, playerId, vanillaPlayerKilled);
    }

    public enum Kind {
        NONE,
        DIRECT_PLAYER,
        TAMED_ENTITY;

        public static Kind parsePersisted(String value) {
            if (value == null || value.isBlank()) {
                return NONE;
            }
            try {
                return valueOf(value);
            } catch (IllegalArgumentException exception) {
                return NONE;
            }
        }
    }
}
