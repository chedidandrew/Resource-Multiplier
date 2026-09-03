package com.chedidandrew.smartresourcedrops.core.provenance;

import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;

/** Classifies successful state replacements without turning placed resources natural. */
public final class ProvenanceTransitionPolicy {
    private static final Set<Block> SOIL_FAMILY = Set.of(
        Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT,
        Blocks.PODZOL, Blocks.MYCELIUM, Blocks.DIRT_PATH, Blocks.FARMLAND,
        Blocks.MUD, Blocks.CLAY
    );

    private ProvenanceTransitionPolicy() {
    }

    public static Result classify(final BlockState oldState, final BlockState newState) {
        if (oldState == newState) {
            return Result.PRESERVE;
        }
        final Block oldBlock = oldState.getBlock();
        final Block newBlock = newState.getBlock();
        if (oldBlock == newBlock) {
            return isGeneratedGrowth(oldState) ? Result.GENERATED : Result.PRESERVE;
        }
        if (newState.isAir()) {
            return Result.REMOVE;
        }
        if (isLogTransition(oldState, newState)
            || (SOIL_FAMILY.contains(oldBlock) && SOIL_FAMILY.contains(newBlock))
            || isCopperTransition(oldBlock, newBlock)
            || isConcreteTransition(oldState, newState)
            || isCoralTransition(oldBlock, newBlock)
            || isCauldronTransition(oldBlock, newBlock)) {
            return Result.PRESERVE;
        }
        return Result.REMOVE;
    }

    private static boolean isLogTransition(final BlockState oldState, final BlockState newState) {
        if (oldState.is(BlockTags.LOGS) && newState.is(BlockTags.LOGS)) {
            return true;
        }
        final ResourceLocation oldId = BuiltInRegistries.BLOCK.getKey(oldState.getBlock());
        final ResourceLocation newId = BuiltInRegistries.BLOCK.getKey(newState.getBlock());
        if (!oldId.getNamespace().equals(newId.getNamespace())) {
            return false;
        }
        final String oldPath = oldId.getPath().replace("stripped_", "");
        final String newPath = newId.getPath().replace("stripped_", "");
        return oldPath.equals(newPath)
            && (oldPath.endsWith("_log") || oldPath.endsWith("_wood")
                || oldPath.endsWith("_stem") || oldPath.endsWith("_hyphae"));
    }

    private static boolean isGeneratedGrowth(final BlockState state) {
        final Block block = state.getBlock();
        return state.is(BlockTags.CROPS)
            || block == Blocks.NETHER_WART
            || block == Blocks.COCOA
            || block == Blocks.SWEET_BERRY_BUSH
            || block == Blocks.CAVE_VINES
            || block == Blocks.CAVE_VINES_PLANT
            || block == Blocks.KELP;
    }

    private static boolean isCopperTransition(final Block oldBlock, final Block newBlock) {
        return WeatheringCopper.getNext(oldBlock).orElse(null) == newBlock
            || WeatheringCopper.getPrevious(oldBlock).orElse(null) == newBlock
            || HoneycombItem.WAXABLES.get().get(oldBlock) == newBlock
            || HoneycombItem.WAX_OFF_BY_BLOCK.get().get(oldBlock) == newBlock;
    }

    private static boolean isConcreteTransition(final BlockState oldState, final BlockState newState) {
        final ResourceLocation oldId = BuiltInRegistries.BLOCK.getKey(oldState.getBlock());
        final ResourceLocation newId = BuiltInRegistries.BLOCK.getKey(newState.getBlock());
        return oldId.getNamespace().equals(newId.getNamespace())
            && oldId.getPath().endsWith("_concrete_powder")
            && oldId.getPath().replace("_concrete_powder", "_concrete").equals(newId.getPath());
    }

    private static boolean isCoralTransition(final Block oldBlock, final Block newBlock) {
        final ResourceLocation oldId = BuiltInRegistries.BLOCK.getKey(oldBlock);
        final ResourceLocation newId = BuiltInRegistries.BLOCK.getKey(newBlock);
        return oldId.getNamespace().equals(newId.getNamespace())
            && oldId.getPath().replace("dead_", "").equals(newId.getPath().replace("dead_", ""))
            && oldId.getPath().contains("coral");
    }

    private static boolean isCauldronTransition(final Block oldBlock, final Block newBlock) {
        final ResourceLocation oldId = BuiltInRegistries.BLOCK.getKey(oldBlock);
        final ResourceLocation newId = BuiltInRegistries.BLOCK.getKey(newBlock);
        return oldId.getNamespace().equals(newId.getNamespace())
            && oldId.getPath().endsWith("cauldron")
            && newId.getPath().endsWith("cauldron");
    }

    public enum Result {
        PRESERVE,
        GENERATED,
        REMOVE
    }
}
