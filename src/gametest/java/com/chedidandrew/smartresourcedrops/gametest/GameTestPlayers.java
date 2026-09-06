package com.chedidandrew.smartresourcedrops.gametest;

import com.chedidandrew.smartresourcedrops.platform.PlatformPlayerSupport;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

/** Supported mock-player construction shared by the dedicated-server GameTests. */
final class GameTestPlayers {
    private GameTestPlayers() {
    }

    static ServerPlayer survival(final GameTestHelper helper) {
        return withGameMode(helper, GameType.SURVIVAL);
    }

    @SuppressWarnings("removal")
    static ServerPlayer withGameMode(final GameTestHelper helper, final GameType gameType) {
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        PlatformPlayerSupport.installFakePlayerPredicate(candidate ->
                !"test-mock-player".equals(candidate.getScoreboardName())
                        && candidate.getClass().getName().contains("FakePlayer"));
        if (PlatformPlayerSupport.isFakePlayer(player)) {
            throw new AssertionError("The target-native GameTest player exemption was not installed");
        }
        player.setGameMode(gameType);
        return player;
    }
}
