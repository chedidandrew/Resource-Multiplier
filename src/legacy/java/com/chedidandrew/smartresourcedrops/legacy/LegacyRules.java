package com.chedidandrew.smartresourcedrops.legacy;

import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.common.registry.EntityRegistry;
import java.util.Locale;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLog;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.monster.EntityGolem;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAmbientCreature;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

/** Minecraft 1.7.10 registry and category adapter. */
public final class LegacyRules {
    private LegacyRules() {}

    public static String blockId(Block block) {
        String key = GameData.getBlockRegistry().getNameForObject(block);
        if (key == null) return "minecraft:air";
        String normalized = normalizeId(key);
        if ("minecraft:mob_spawner".equals(normalized)) return "minecraft:spawner";
        if ("minecraft:portal".equals(normalized)) return "minecraft:nether_portal";
        return normalized;
    }

    public static String entityId(Entity entity) {
        EntityRegistry.EntityRegistration registration =
                EntityRegistry.instance().lookupModSpawn(entity.getClass(), true);
        if (registration != null) {
            return normalizeId(registration.getContainer().getModId() + ":" + registration.getEntityName());
        }
        if (entity instanceof EntityHorse) return horseId((EntityHorse) entity);
        String key = EntityList.getEntityString(entity);
        return key == null ? "minecraft:unknown" : normalizeVanillaEntityId(key);
    }

    private static String horseId(EntityHorse horse) {
        int type = horse.getHorseType();
        if (type == 1) return "minecraft:donkey";
        if (type == 2) return "minecraft:mule";
        if (type == 3) return "minecraft:zombie_horse";
        if (type == 4) return "minecraft:skeleton_horse";
        return "minecraft:horse";
    }

    static boolean isSpecialShearingEntityId(String entityId) {
        return "minecraft:mooshroom".equals(entityId) || "minecraft:snow_golem".equals(entityId);
    }

    static boolean isStandardShearingEntityId(String entityId) {
        // Sheep are the only vanilla 1.7.10 IShearable whose action is a
        // repeatable standard-resource harvest. Mooshroom and snow-golem
        // shearing permanently transform/equip the entity and stay vanilla.
        return "minecraft:sheep".equals(entityId);
    }

    static boolean isSafeShearingEntityId(String entityId, boolean hasExactCertification) {
        return !isSpecialShearingEntityId(entityId)
                && (isStandardShearingEntityId(entityId) || hasExactCertification);
    }

    static String normalizeVanillaEntityId(String key) {
        String normalized = normalizeId(key);
        if ("minecraft:cavespider".equals(normalized)) return "minecraft:cave_spider";
        if ("minecraft:enderdragon".equals(normalized)) return "minecraft:ender_dragon";
        if ("minecraft:entityhorse".equals(normalized)) return "minecraft:horse";
        if ("minecraft:lavaslime".equals(normalized)) return "minecraft:magma_cube";
        if ("minecraft:mushroomcow".equals(normalized)) return "minecraft:mooshroom";
        if ("minecraft:ozelot".equals(normalized)) return "minecraft:ocelot";
        if ("minecraft:pigzombie".equals(normalized)) return "minecraft:zombified_piglin";
        if ("minecraft:snowman".equals(normalized)) return "minecraft:snow_golem";
        if ("minecraft:villagergolem".equals(normalized)) return "minecraft:iron_golem";
        if ("minecraft:witherboss".equals(normalized)) return "minecraft:wither";
        return normalized;
    }

    private static String normalizeId(String key) {
        String lowered = key.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        return lowered.indexOf(':') >= 0 ? lowered : "minecraft:" + lowered;
    }

    public static String blockCategory(Block block, int metadata) {
        String id = blockId(block);
        String path = id.substring(id.indexOf(':') + 1);
        if (block instanceof BlockLog || isLogPath(path)) return "logs";
        if (block instanceof BlockLeaves || path.contains("leaves")) return "leaves";
        if (block instanceof BlockCrops || path.contains("crop") || path.contains("wheat")
                || path.contains("carrot") || path.contains("potato")) return "crops";
        if (hasOrePrefix(block, metadata, "ore") || isOrePath(path)) return "ores";
        if (isRawResourceBlock(block, metadata, path)) return "raw_resource_blocks";
        Material material = block.getMaterial();
        if (path.contains("netherrack") || path.contains("nether") || path.contains("soul_sand")
                || path.contains("glowstone")) return "nether";
        if (path.contains("end_stone") || path.contains("dragon_egg")) return "end";
        if (material == Material.rock || material == Material.iron || material == Material.anvil) return "stone";
        if (material == Material.ground || material == Material.grass || material == Material.sand
                || material == Material.clay || material == Material.snow) return "soil";
        if (block instanceof IGrowable || material == Material.plants || material == Material.vine
                || material == Material.cactus || material == Material.gourd) return "plants";
        if (material == Material.wood || material == Material.glass || material == Material.circuits
                || material == Material.cloth || material == Material.carpet) return "building_blocks";
        return "miscellaneous";
    }

    static boolean isLogPath(String path) {
        return "log".equals(path)
                || "log2".equals(path)
                || "wood".equals(path)
                || path.startsWith("log_")
                || path.endsWith("_log")
                || path.contains("_log_")
                || path.endsWith("_wood")
                || path.contains("_wood_");
    }

    static boolean isOrePath(String path) {
        return "ore".equals(path)
                || path.startsWith("ore_")
                || path.endsWith("_ore")
                || path.contains("_ore_");
    }

    private static boolean isRawResourceBlock(Block block, int metadata, String path) {
        if (path.equals("iron_block") || path.equals("gold_block") || path.equals("diamond_block")
                || path.equals("emerald_block") || path.equals("lapis_block")
                || path.equals("redstone_block") || path.equals("coal_block")
                || path.equals("quartz_block")) return true;
        return block.getMaterial() != Material.glass && hasOrePrefix(block, metadata, "block");
    }

    private static boolean hasOrePrefix(Block block, int metadata, String prefix) {
        try {
            int[] ids = OreDictionary.getOreIDs(new ItemStack(block, 1, metadata));
            for (int id : ids) if (OreDictionary.getOreName(id).toLowerCase(Locale.ROOT).startsWith(prefix)) return true;
        } catch (RuntimeException ignored) {
            // Registry-name heuristics remain deterministic for unusual blocks.
        }
        return false;
    }

    public static boolean isBoss(EntityLivingBase entity) { return entity instanceof IBossDisplayData; }

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
