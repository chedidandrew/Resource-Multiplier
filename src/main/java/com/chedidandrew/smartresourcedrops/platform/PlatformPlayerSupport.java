package com.chedidandrew.smartresourcedrops.platform;

import java.util.Objects;
import java.util.function.Predicate;

import net.minecraft.world.entity.player.Player;

/** Loader-installed player classification used by shared gameplay policy. */
public final class PlatformPlayerSupport {
    private static volatile Predicate<Player> fakePlayerPredicate;

    private PlatformPlayerSupport() {
    }

    public static void installFakePlayerPredicate(final Predicate<Player> predicate) {
        fakePlayerPredicate = Objects.requireNonNull(predicate, "predicate");
    }

    public static boolean isFakePlayer(final Player player) {
        if (player == null) {
            return false;
        }
        final Predicate<Player> predicate = fakePlayerPredicate;
        // Fail closed: an incomplete loader bootstrap must never grant automation player privileges.
        return predicate == null || predicate.test(player);
    }

    public static void bootstrap() {
        if (fakePlayerPredicate == null) {
            throw new IllegalStateException("Fake-player detection has not been installed by the active loader");
        }
    }
}
