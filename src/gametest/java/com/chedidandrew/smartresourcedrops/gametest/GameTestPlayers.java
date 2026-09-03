package com.chedidandrew.smartresourcedrops.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

/** Supported mock-player construction shared by the dedicated-server GameTests. */
final class GameTestPlayers {
    private GameTestPlayers() {
    }

    static ServerPlayer survival(final GameTestHelper helper) {
        return withGameMode(helper, GameType.SURVIVAL);
    }

    static ServerPlayer withGameMode(final GameTestHelper helper, final GameType gameType) {
        final ServerPlayer player = new ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "srm-test-player")) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return gameType == GameType.CREATIVE;
            }
        };
        // Vanilla 1.20.1's GameTest helper hands Forge a Connection whose Netty
        // channel is null. Forge inspects the pipeline while placing the player,
        // so use an in-memory channel to exercise the real login/player path.
        final Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player);
        player.setGameMode(gameType);
        return player;
    }
}
