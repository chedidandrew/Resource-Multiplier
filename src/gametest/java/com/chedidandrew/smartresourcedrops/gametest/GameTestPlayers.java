package com.chedidandrew.smartresourcedrops.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

/** Supported mock-player construction shared by the dedicated-server GameTests. */
final class GameTestPlayers {
    private GameTestPlayers() {
    }

    static ServerPlayer survival(final GameTestHelper helper) {
        return (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
    }
}
