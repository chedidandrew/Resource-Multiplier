package com.chedidandrew.smartresourcedrops.core;

import com.chedidandrew.smartresourcedrops.config.ConfigManager;
import com.chedidandrew.smartresourcedrops.config.SmartDropsConfig;
import com.chedidandrew.smartresourcedrops.provenance.PlacementTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashSet;
import java.util.Set;

public final class MultiplierResolver {
    private MultiplierResolver() {
    }

    public static RuleEngine.Decision resolve(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            BlockEntity blockEntity,
            DropSource source,
            Entity actor
    ) {
        return trace(level, pos, state, blockEntity, source, actor, false).decision();
    }

    /**
     * Produces a read-only explanation of the real resolver without advancing
     * provenance caches or entering a drop/statistics context.
     */
    public static RuleResolutionTrace inspect(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            BlockEntity blockEntity,
            DropSource source,
            Entity actor
    ) {
        return trace(level, pos, state, blockEntity, source, actor, true);
    }

    private static RuleResolutionTrace trace(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            BlockEntity blockEntity,
            DropSource source,
            Entity actor,
            boolean readOnly
    ) {
        SmartDropsConfig config = ConfigManager.get();
        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        String dimensionId = level.dimension().identifier().toString();
        LinkedHashSet<Category> categories = SmartDropTags.categoriesFor(state);
        boolean playerPlaced = readOnly
                ? PlacementTracker.peekPlaced(level, pos)
                : PlacementTracker.isPlaced(level, pos);
        boolean hasBlockEntity = blockEntity != null || state.hasBlockEntity();
        Set<String> filterTags = SmartDropTags.allMatchingFilterTags(
                state,
                config.tagBlacklist,
                config.tagWhitelist);
        String playerId = actor instanceof Player player ? player.getUUID().toString() : null;

        return RuleEngine.trace(config, new RuleEngine.RuleInput(
                blockId,
                dimensionId,
                categories,
                playerPlaced,
                hasBlockEntity,
                filterTags,
                source,
                playerId));
    }
}
