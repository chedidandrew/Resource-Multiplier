package com.chedidandrew.smartresourcedrops.legacy;

import java.util.Locale;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLog;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityGolem;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAmbientCreature;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;

/** Version-specific registry/category adapter kept out of the policy class. */
public final class LegacyRules {
    private LegacyRules() {}

    public static String blockId(IBlockState state) {
        ResourceLocation key = state.getBlock().getRegistryName();
        return key == null ? "minecraft:air" : key.toString().toLowerCase(Locale.ROOT);
    }

    public static String entityId(Entity entity) {
        ResourceLocation key = EntityList.getKey(entity);
        return key == null ? "minecraft:unknown" : key.toString().toLowerCase(Locale.ROOT);
    }

    public static String blockCategory(IBlockState state) {
        Block block = state.getBlock();
        String id = blockId(state);
        String path = id.indexOf(':') >= 0 ? id.substring(id.indexOf(':') + 1) : id;
        if (block instanceof BlockLog || path.contains("log") || path.contains("wood")) return "logs";
        if (block instanceof BlockLeaves || path.contains("leaves")) return "leaves";
        if (block instanceof BlockCrops || path.contains("crop") || path.contains("wheat")
                || path.contains("carrot") || path.contains("potato") || path.contains("beetroot")) return "crops";
        if (hasOrePrefix(state, "ore")) return "ores";
        if (path.contains("ore") && !path.contains("storage")) return "ores";
        if (isRawResourceBlock(state, path)) return "raw_resource_blocks";
        Material material = state.getMaterial();
        if (path.contains("netherrack") || path.contains("nether") || path.contains("soul_sand")
                || path.contains("glowstone") || path.contains("magma")) return "nether";
        if (path.contains("end_stone") || path.contains("purpur") || path.contains("chorus")) return "end";
        if (material == Material.ROCK || material == Material.IRON || material == Material.ANVIL) return "stone";
        if (material == Material.GROUND || material == Material.GRASS || material == Material.SAND
                || material == Material.CLAY || material == Material.SNOW) return "soil";
        if (block instanceof IGrowable || material == Material.PLANTS || material == Material.VINE
                || material == Material.CACTUS || material == Material.GOURD) return "plants";
        if (material == Material.WOOD || material == Material.GLASS || material == Material.CIRCUITS
                || material == Material.CLOTH || material == Material.CARPET) return "building_blocks";
        return "miscellaneous";
    }

    private static boolean isRawResourceBlock(IBlockState state, String path) {
        if (path.equals("iron_block") || path.equals("gold_block") || path.equals("diamond_block")
                || path.equals("emerald_block") || path.equals("lapis_block")
                || path.equals("redstone_block") || path.equals("coal_block")
                || path.equals("quartz_block")) return true;
        return state.getMaterial() != Material.GLASS && hasOrePrefix(state, "block");
    }

    private static boolean hasOrePrefix(IBlockState state, String prefix) {
        try {
            int meta = state.getBlock().getMetaFromState(state);
            int[] ids = OreDictionary.getOreIDs(new ItemStack(state.getBlock(), 1, meta));
            for (int id : ids) {
                String name = OreDictionary.getOreName(id).toLowerCase(Locale.ROOT);
                if (name.startsWith(prefix)) return true;
            }
        } catch (RuntimeException ignored) {
            // Registry-name heuristics below remain deterministic for unusual blocks.
        }
        return false;
    }

    public static boolean isBoss(EntityLivingBase entity) {
        return !entity.isNonBoss();
    }

    public static String entityCategory(EntityLivingBase entity) {
        if (isBoss(entity)) return "bosses";
        if (entity instanceof IMob) return "hostile";
        if (entity instanceof EntityVillager) return "villagers_npcs";
        if (entity instanceof EntityGolem) return "golems";
        if (entity instanceof EntityWaterMob) return "aquatic";
        if (entity instanceof EntityAmbientCreature) return "ambient";
        if (entity instanceof EntityTameable) return "neutral";
        if (entity instanceof EntityAnimal) return "passive";
        return "miscellaneous";
    }
}
