package com.chedidandrew.smartresourcedrops.legacy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityPiston;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.IShearable;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Forge event adapter for final loot, XP, shearing, and placement provenance. */
public final class LegacyEventHandler {
    private static final int MAX_MULTIPLIED_STACKS = 4096;
    private static final long MAX_MULTIPLIED_ITEMS = 262144L;
    private static final int MAX_SHEARING_STACKS = 256;
    private static final long MAX_SHEARING_ITEMS = 1024L;
    private final Map<World, Set<BlockPos>> pendingRemovals =
            Collections.synchronizedMap(new WeakHashMap<World, Set<BlockPos>>());
    private final Map<World, Set<BlockPos>> explosionDrops =
            Collections.synchronizedMap(new WeakHashMap<World, Set<BlockPos>>());
    private final Map<World, Map<Integer, BlockPos>> protectedFallingBlocks =
            Collections.synchronizedMap(new WeakHashMap<World, Map<Integer, BlockPos>>());
    private final Map<World, Map<java.util.UUID, Long>> qualifyingExperienceDeaths =
            Collections.synchronizedMap(new WeakHashMap<World, Map<java.util.UUID, Long>>());

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlace(BlockEvent.PlaceEvent event) {
        if (event.getWorld().isRemote) return;
        World world = (World) event.getWorld();
        if (event instanceof BlockEvent.MultiPlaceEvent) {
            for (BlockSnapshot snapshot : ((BlockEvent.MultiPlaceEvent) event).getReplacedBlockSnapshots()) {
                markPlacement(world, snapshot.getPos());
            }
        } else {
            markPlacement(world, event.getPos());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBreak(BlockEvent.BreakEvent event) {
        if (event.getWorld().isRemote) return;
        World world = (World) event.getWorld();
        boolean placed = PlacedBlockData.get(world).contains(event.getPos());
        LegacyConfig config = SmartResourceMultiplier.config();
        boolean realPlayer = isRealPlayer(event.getPlayer());
        String blockId = LegacyRules.blockId(event.getState());
        boolean eligible = config.isBlockEligible(
                blockId,
                placed,
                event.getState().getBlock().hasTileEntity(event.getState()));
        event.setExpToDrop(multiplyBlockExperience(
                event.getExpToDrop(),
                config,
                realPlayer ? config.playerMining : config.automatedMining,
                eligible));
        scheduleRemoval(world, event.getPos());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onHarvestDrops(BlockEvent.HarvestDropsEvent event) {
        if (event.getWorld().isRemote) return;
        World world = (World) event.getWorld();
        LegacyConfig config = SmartResourceMultiplier.config();
        boolean playerBreak = isRealPlayer(event.getHarvester());
        scheduleRemoval(world, event.getPos());
        if (playerBreak && !config.playerMining) return;
        Set<BlockPos> explosionPositions = explosionDrops.get(world);
        boolean explosionBreak = explosionPositions != null && explosionPositions.remove(event.getPos());
        if (!playerBreak && explosionBreak && !config.explosions) return;
        if (!playerBreak && !explosionBreak && !config.automatedMining) return;

        boolean placed = PlacedBlockData.get(world).contains(event.getPos());
        String playerId = playerBreak ? event.getHarvester().getUniqueID().toString() : null;
        int multiplier = config.blockMultiplier(
                LegacyRules.blockId(event.getState()),
                LegacyRules.blockCategory(event.getState()),
                world.provider.getDimension(),
                placed,
                event.getState().getBlock().hasTileEntity(event.getState()),
                playerId);
        multiplyStacks(event.getDrops(), multiplier);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getWorld().isRemote) return;
        Set<BlockPos> positions = explosionDrops.get(event.getWorld());
        if (positions == null) {
            positions = new LinkedHashSet<BlockPos>();
            explosionDrops.put(event.getWorld(), positions);
        }
        for (BlockPos pos : event.getAffectedBlocks()) {
            positions.add(pos.toImmutable());
            scheduleRemoval(event.getWorld(), pos);
        }
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote || !(event.getEntity() instanceof EntityFallingBlock)) return;
        BlockPos origin = new BlockPos(event.getEntity().posX, event.getEntity().posY, event.getEntity().posZ);
        PlacedBlockData data = PlacedBlockData.get(event.getWorld());
        if (!data.contains(origin)) return;
        Map<Integer, BlockPos> tracked = protectedFallingBlocks.get(event.getWorld());
        if (tracked == null) {
            tracked = new LinkedHashMap<Integer, BlockPos>();
            protectedFallingBlocks.put(event.getWorld(), tracked);
        }
        tracked.put(Integer.valueOf(event.getEntity().getEntityId()), origin);
        scheduleRemoval(event.getWorld(), origin);
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) return;
        preservePistonProvenance(event.world);
        preserveFallingBlockProvenance(event.world);
        explosionDrops.remove(event.world);
        Set<BlockPos> pending = pendingRemovals.remove(event.world);
        if (pending != null && !pending.isEmpty()) {
            PlacedBlockData data = PlacedBlockData.get(event.world);
            for (BlockPos pos : pending) data.unmark(pos);
        }
        Map<java.util.UUID, Long> deaths = qualifyingExperienceDeaths.get(event.world);
        if (deaths != null) {
            long now = event.world.getTotalWorldTime();
            Iterator<Map.Entry<java.util.UUID, Long>> deathIterator = deaths.entrySet().iterator();
            while (deathIterator.hasNext()) {
                if (deathIterator.next().getValue().longValue() < now) deathIterator.remove();
            }
            if (deaths.isEmpty()) qualifyingExperienceDeaths.remove(event.world);
        }
    }

    private void preservePistonProvenance(World world) {
        if (!SmartResourceMultiplier.config().conservativePistonProtection) return;
        PlacedBlockData data = PlacedBlockData.get(world);
        List<TileEntity> tiles = new ArrayList<TileEntity>(world.loadedTileEntityList);
        for (TileEntity tile : tiles) {
            if (!(tile instanceof TileEntityPiston)) continue;
            TileEntityPiston piston = (TileEntityPiston) tile;
            EnumFacing movement = piston.isExtending() ? piston.getFacing() : piston.getFacing().getOpposite();
            BlockPos destination = piston.getPos();
            BlockPos source = destination.offset(movement.getOpposite());
            if (data.contains(source)) {
                data.mark(destination);
                scheduleRemoval(world, source);
            }
        }
    }

    private void preserveFallingBlockProvenance(World world) {
        Map<Integer, BlockPos> tracked = protectedFallingBlocks.get(world);
        if (tracked == null || tracked.isEmpty()) return;
        Set<Integer> seen = new LinkedHashSet<Integer>();
        for (Entity entity : new ArrayList<Entity>(world.loadedEntityList)) {
            if (!(entity instanceof EntityFallingBlock)) continue;
            Integer id = Integer.valueOf(entity.getEntityId());
            if (!tracked.containsKey(id)) continue;
            seen.add(id);
            BlockPos current = new BlockPos(entity.posX, entity.posY, entity.posZ);
            tracked.put(id, current);
            if (entity.isDead) PlacedBlockData.get(world).mark(current);
        }
        Iterator<Map.Entry<Integer, BlockPos>> iterator = tracked.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, BlockPos> entry = iterator.next();
            if (!seen.contains(entry.getKey())) {
                PlacedBlockData.get(world).mark(entry.getValue());
                iterator.remove();
            }
        }
        if (tracked.isEmpty()) protectedFallingBlocks.remove(world);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingDrops(LivingDropsEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (entity instanceof EntityPlayer || entity.world.isRemote
                || !qualifies(event, SmartResourceMultiplier.config())) return;
        LegacyConfig config = SmartResourceMultiplier.config();
        int multiplier = config.entityMultiplier(
                LegacyRules.entityId(entity), LegacyRules.entityCategory(entity), LegacyRules.isBoss(entity));
        multiplyEntityItems(entity.world, event.getDrops(), multiplier);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingDeath(LivingDeathEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (entity instanceof EntityPlayer || entity.world.isRemote) return;
        LegacyConfig config = SmartResourceMultiplier.config();
        if (!qualifies(event.getSource().getTrueSource(), config)) return;
        Map<java.util.UUID, Long> deaths = qualifyingExperienceDeaths.get(entity.world);
        if (deaths == null) {
            deaths = new LinkedHashMap<java.util.UUID, Long>();
            qualifyingExperienceDeaths.put(entity.world, deaths);
        }
        deaths.put(entity.getUniqueID(), Long.valueOf(entity.world.getTotalWorldTime() + 30L));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingExperience(LivingExperienceDropEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        LegacyConfig config = SmartResourceMultiplier.config();
        if (entity instanceof EntityPlayer || entity.world.isRemote
                || !config.enabled || !config.multiplyMobExperience) return;
        boolean boss = LegacyRules.isBoss(entity);
        if (boss && !config.multiplyBossExperience) return;
        if (config.entityKillRequirement != LegacyConfig.EntityKillRequirement.ALL_STANDARD_DEATH_LOOT
                && !isRealPlayer(event.getAttackingPlayer())) {
            Map<java.util.UUID, Long> deaths = qualifyingExperienceDeaths.get(entity.world);
            if (deaths == null || !deaths.containsKey(entity.getUniqueID())) return;
            deaths.remove(entity.getUniqueID());
        }
        event.setDroppedExperience(multiplyExistingExperience(
                event.getDroppedExperience(), config.mobExperienceMultiplier));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityInteract(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getWorld().isRemote) return;
        LegacyConfig config = SmartResourceMultiplier.config();
        boolean automated = event.getEntityPlayer() instanceof FakePlayer;
        if (!config.enabled
                || (automated ? !config.automatedShearingDropsEnabled : !config.manualShearingDropsEnabled)) return;
        Entity target = event.getTarget();
        ItemStack shears = event.getItemStack();
        if (shears.isEmpty() || !(shears.getItem() instanceof ItemShears) || !(target instanceof IShearable)) return;
        IShearable shearable = (IShearable) target;
        BlockPos pos = new BlockPos(target);
        if (!shearable.isShearable(shears, event.getWorld(), pos)) return;

        int fortune = EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, shears);
        List<ItemStack> drops = shearable.onSheared(shears, event.getWorld(), pos, fortune);
        int multiplier = config.shearingMultiplier(LegacyRules.entityId(target));
        multiplyStacks(drops, multiplier, MAX_SHEARING_STACKS, MAX_SHEARING_ITEMS);
        for (ItemStack drop : drops) {
            EntityItem item = new EntityItem(event.getWorld(), target.posX, target.posY + 0.5D, target.posZ, drop);
            item.setDefaultPickupDelay();
            event.getWorld().spawnEntity(item);
        }
        shears.damageItem(1, event.getEntityPlayer());
        event.setCancellationResult(EnumActionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static boolean qualifies(LivingDropsEvent event, LegacyConfig config) {
        if (!config.enabled || !config.entityDropsEnabled) return false;
        return qualifies(event.getSource().getTrueSource(), config);
    }

    private static boolean qualifies(Entity source, LegacyConfig config) {
        if (config.entityKillRequirement == LegacyConfig.EntityKillRequirement.ALL_STANDARD_DEATH_LOOT) return true;
        if (source instanceof EntityPlayer && !(source instanceof FakePlayer)) return true;
        if (config.entityKillRequirement == LegacyConfig.EntityKillRequirement.PLAYER_OR_TAMED_ENTITY
                && source instanceof EntityTameable) {
            EntityTameable tameable = (EntityTameable) source;
            return tameable.isTamed() && tameable.getOwner() instanceof EntityPlayer
                    && !(tameable.getOwner() instanceof FakePlayer);
        }
        return false;
    }

    private void markPlacement(World world, BlockPos pos) {
        PlacedBlockData.get(world).mark(pos);
        Set<BlockPos> pending = pendingRemovals.get(world);
        if (pending != null) pending.remove(pos);
    }

    private static boolean isRealPlayer(Entity entity) {
        return entity instanceof EntityPlayer && !(entity instanceof FakePlayer);
    }

    private void scheduleRemoval(World world, BlockPos pos) {
        Set<BlockPos> pending = pendingRemovals.get(world);
        if (pending == null) {
            pending = new LinkedHashSet<BlockPos>();
            pendingRemovals.put(world, pending);
        }
        pending.add(pos.toImmutable());
    }

    static void multiplyStacks(List<ItemStack> drops, int multiplier) {
        multiplyStacks(drops, multiplier, MAX_MULTIPLIED_STACKS, MAX_MULTIPLIED_ITEMS);
    }

    private static void multiplyStacks(
            List<ItemStack> drops, int multiplier, int maximumStacks, long maximumItems) {
        if (multiplier <= 0) {
            drops.clear();
            return;
        }
        if (multiplier == 1 || drops.isEmpty()) return;
        List<ItemStack> originals = new ArrayList<ItemStack>(drops.size());
        long sourceItems = 0L;
        for (ItemStack stack : drops) {
            if (stack.isEmpty()) continue;
            originals.add(stack.copy());
            sourceItems += Math.max(0, stack.getCount());
        }
        if (!fitsOutputBudget(originals.size(), sourceItems, multiplier, maximumStacks, maximumItems)) return;
        for (int copy = 1; copy < multiplier; copy++) {
            for (ItemStack original : originals) {
                drops.add(original.copy());
            }
        }
    }

    private static void multiplyEntityItems(World world, List<EntityItem> drops, int multiplier) {
        if (multiplier <= 0) {
            Iterator<EntityItem> iterator = drops.iterator();
            while (iterator.hasNext()) {
                if (!isProtectedEntityDrop(iterator.next())) iterator.remove();
            }
            return;
        }
        if (multiplier == 1 || drops.isEmpty()) return;
        List<EntityItem> originals = new ArrayList<EntityItem>(drops.size());
        long sourceItems = 0L;
        for (EntityItem original : drops) {
            if (original == null || original.getItem().isEmpty() || isProtectedEntityDrop(original)) continue;
            originals.add(original);
            sourceItems += Math.max(0, original.getItem().getCount());
        }
        if (!fitsOutputBudget(
                originals.size(), sourceItems, multiplier, MAX_MULTIPLIED_STACKS, MAX_MULTIPLIED_ITEMS)) return;
        for (int copy = 1; copy < multiplier; copy++) {
            for (EntityItem original : originals) {
                EntityItem duplicate = new EntityItem(world, original.posX, original.posY, original.posZ,
                        original.getItem().copy());
                duplicate.motionX = original.motionX;
                duplicate.motionY = original.motionY;
                duplicate.motionZ = original.motionZ;
                duplicate.setDefaultPickupDelay();
                drops.add(duplicate);
            }
        }
    }

    private static boolean isProtectedEntityDrop(EntityItem entityItem) {
        if (entityItem == null || entityItem.getItem().isEmpty()) return false;
        return entityItem.getItem().getItem() == Items.SADDLE
                || entityItem.getItem().getItem() == Items.TOTEM_OF_UNDYING;
    }

    static boolean fitsOutputBudget(
            int sourceStacks, long sourceItems, int multiplier, int maximumStacks, long maximumItems) {
        int safeMultiplier = Math.max(0, multiplier);
        if (safeMultiplier == 0) return true;
        if (sourceStacks < 0 || sourceItems < 0L || maximumStacks < 0 || maximumItems < 0L) return false;
        return sourceStacks <= maximumStacks / safeMultiplier
                && sourceItems <= maximumItems / safeMultiplier;
    }

    static int multiplyBlockExperience(
            int value, LegacyConfig config, boolean sourceEnabled, boolean eligible) {
        if (!config.multiplyExperience || !sourceEnabled || !eligible) return value;
        return safeMultiply(value, config.experienceMultiplier);
    }

    static int multiplyExistingExperience(int value, int multiplier) {
        return safeMultiply(value, multiplier);
    }

    private static int safeMultiply(int value, int multiplier) {
        long result = (long) value * (long) Math.max(0, multiplier);
        return result > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
    }
}
