package com.chedidandrew.smartresourcedrops.legacy;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Items;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityPiston;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Facing;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.IShearable;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ExplosionEvent;

/** Forge 1.7.10 adapter for final loot, XP, shearing, and placement provenance. */
public final class LegacyEventHandler {
    private static final int MAX_MULTIPLIED_STACKS = 4096;
    private static final long MAX_MULTIPLIED_ITEMS = 262144L;
    private static final int MAX_SHEARING_STACKS = 256;
    private static final long MAX_SHEARING_ITEMS = 1024L;
    private static final int MAX_MULTIPLIED_XP_AWARD = 2477 * 256;
    private final Map<World, Set<Long>> pendingRemovals =
            Collections.synchronizedMap(new WeakHashMap<World, Set<Long>>());
    private final Map<World, Set<Long>> explosionDrops =
            Collections.synchronizedMap(new WeakHashMap<World, Set<Long>>());
    private final Map<World, Map<UUID, EntityFallingBlock>> protectedFallingBlocks =
            Collections.synchronizedMap(new WeakHashMap<World, Map<UUID, EntityFallingBlock>>());
    private final Map<World, List<ExperienceDeath>> experienceDeaths =
            Collections.synchronizedMap(new WeakHashMap<World, List<ExperienceDeath>>());
    private final Map<World, Map<UUID, RememberedKill>> rememberedKills =
            Collections.synchronizedMap(new WeakHashMap<World, Map<UUID, RememberedKill>>());

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlace(BlockEvent.PlaceEvent event) {
        if (event.world.isRemote) return;
        PlacedBlockData data = PlacedBlockData.get(event.world);
        if (event instanceof BlockEvent.MultiPlaceEvent) {
            for (BlockSnapshot snapshot : ((BlockEvent.MultiPlaceEvent) event).getReplacedBlockSnapshots()) {
                data.mark(snapshot.x, snapshot.y, snapshot.z);
                cancelRemoval(event.world, snapshot.x, snapshot.y, snapshot.z);
            }
        } else {
            data.mark(event.x, event.y, event.z);
            cancelRemoval(event.world, event.x, event.y, event.z);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBreak(BlockEvent.BreakEvent event) {
        if (event.world.isRemote) return;
        PlacedBlockData data = PlacedBlockData.get(event.world);
        boolean placed = data.contains(event.x, event.y, event.z);
        LegacyConfig config = SmartResourceMultiplier.config();
        boolean realPlayer = isRealPlayer(event.getPlayer());
        String blockId = LegacyRules.blockId(event.block);
        event.setExpToDrop(multiplyBlockExperience(
                event.getExpToDrop(),
                config,
                realPlayer ? config.playerMining : config.automatedMining,
                config.isBlockEligible(
                        blockId,
                        placed,
                        event.block.hasTileEntity(event.blockMetadata))));
        scheduleRemoval(event.world, event.x, event.y, event.z);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onHarvestDrops(BlockEvent.HarvestDropsEvent event) {
        if (event.world.isRemote) return;
        scheduleRemoval(event.world, event.x, event.y, event.z);
        LegacyConfig config = SmartResourceMultiplier.config();
        boolean playerBreak = isRealPlayer(event.harvester);
        if (playerBreak && !config.playerMining) return;
        long packedPosition = PlacedBlockData.pack(event.x, event.y, event.z);
        Set<Long> explosionPositions = explosionDrops.get(event.world);
        boolean explosionBreak = explosionPositions != null
                && explosionPositions.remove(Long.valueOf(packedPosition));
        if (!playerBreak && explosionBreak && !config.explosions) return;
        if (!playerBreak && !explosionBreak && !config.automatedMining) return;

        boolean placed = PlacedBlockData.get(event.world).contains(event.x, event.y, event.z);
        int multiplier = config.blockMultiplier(
                LegacyRules.blockId(event.block),
                LegacyRules.blockCategory(event.block, event.blockMetadata),
                event.world.provider.dimensionId,
                placed,
                event.block.hasTileEntity(event.blockMetadata),
                playerBreak ? event.harvester.getUniqueID().toString() : null);
        multiplyStacks(event.drops, multiplier);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onExplosion(ExplosionEvent.Detonate event) {
        if (event.world.isRemote) return;
        Set<Long> positions = explosionDrops.get(event.world);
        if (positions == null) {
            positions = new LinkedHashSet<Long>();
            explosionDrops.put(event.world, positions);
        }
        for (ChunkPosition position : event.getAffectedBlocks()) {
            positions.add(Long.valueOf(PlacedBlockData.pack(
                    position.chunkPosX, position.chunkPosY, position.chunkPosZ)));
            scheduleRemoval(event.world, position.chunkPosX, position.chunkPosY, position.chunkPosZ);
        }
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinWorldEvent event) {
        if (event.world.isRemote) return;
        if (event.entity instanceof EntityXPOrb) multiplyExperienceOrb(event.world, (EntityXPOrb) event.entity);
        if (!(event.entity instanceof EntityFallingBlock)) return;

        int x = MathHelper.floor_double(event.entity.posX);
        int y = MathHelper.floor_double(event.entity.posY);
        int z = MathHelper.floor_double(event.entity.posZ);
        PlacedBlockData data = PlacedBlockData.get(event.world);
        UUID entityId = event.entity.getUniqueID();
        long origin = PlacedBlockData.pack(x, y, z);
        if (data.contains(x, y, z)) {
            if (!data.beginFalling(entityId, origin)) return;
            data.unmarkPacked(origin);
            cancelRemoval(event.world, x, y, z);
        } else if (!data.hasFalling(entityId)) return;
        Map<UUID, EntityFallingBlock> tracked = protectedFallingBlocks.get(event.world);
        if (tracked == null) {
            tracked = new LinkedHashMap<UUID, EntityFallingBlock>();
            protectedFallingBlocks.put(event.world, tracked);
        }
        tracked.put(entityId, (EntityFallingBlock) event.entity);
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) return;
        preservePistonProvenance(event.world);
        preserveFallingBlockProvenance(event.world);
        explosionDrops.remove(event.world);
        Set<Long> pending = pendingRemovals.remove(event.world);
        if (pending != null) {
            PlacedBlockData data = PlacedBlockData.get(event.world);
            for (Long packed : pending) data.unmarkPacked(packed.longValue());
        }
        pruneExperienceDeaths(event.world);
        pruneRememberedKills(event.world);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) LegacyNetwork.drainServerTasks();
    }

    private void preservePistonProvenance(World world) {
        boolean conservative = SmartResourceMultiplier.config().conservativePistonProtection;
        PlacedBlockData data = PlacedBlockData.get(world);
        Set<Long> destinations = new LinkedHashSet<Long>();
        Set<Long> movedPlacedSources = new LinkedHashSet<Long>();
        // Read every source from the same pre-transfer snapshot. Mutating data as
        // each moving-piston tile is visited breaks multi-block pushes: a middle
        // coordinate is both one block's source and another block's destination.
        for (Object object : new ArrayList<Object>(world.loadedTileEntityList)) {
            if (!(object instanceof TileEntityPiston)) continue;
            TileEntityPiston piston = (TileEntityPiston) object;
            int facing = piston.getPistonOrientation();
            if (facing < 0 || facing >= Facing.offsetsXForSide.length) continue;
            int direction = piston.isExtending() ? 1 : -1;
            int sourceX = piston.xCoord - Facing.offsetsXForSide[facing] * direction;
            int sourceY = piston.yCoord - Facing.offsetsYForSide[facing] * direction;
            int sourceZ = piston.zCoord - Facing.offsetsZForSide[facing] * direction;
            boolean sourceWasPlaced = data.contains(sourceX, sourceY, sourceZ);
            if (shouldProtectPistonDestination(sourceWasPlaced, conservative)) {
                destinations.add(Long.valueOf(PlacedBlockData.pack(
                        piston.xCoord, piston.yCoord, piston.zCoord)));
                if (sourceWasPlaced) {
                    movedPlacedSources.add(Long.valueOf(PlacedBlockData.pack(
                            sourceX, sourceY, sourceZ)));
                }
            }
        }
        for (Long source : pistonSourcesToRemove(movedPlacedSources, destinations)) {
            scheduleRemovalPacked(world, source.longValue());
        }
        for (Long destination : destinations) {
            data.markPacked(destination.longValue());
            cancelRemovalPacked(world, destination.longValue());
        }
    }

    private void preserveFallingBlockProvenance(World world) {
        Map<UUID, EntityFallingBlock> tracked = protectedFallingBlocks.get(world);
        if (tracked == null || tracked.isEmpty()) return;
        PlacedBlockData data = PlacedBlockData.get(world);
        Iterator<Map.Entry<UUID, EntityFallingBlock>> iterator = tracked.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, EntityFallingBlock> entry = iterator.next();
            EntityFallingBlock entity = entry.getValue();
            long current = PlacedBlockData.pack(
                    MathHelper.floor_double(entity.posX),
                    MathHelper.floor_double(entity.posY),
                    MathHelper.floor_double(entity.posZ));
            data.updateFalling(entry.getKey(), current);
            if (entity.isDead) {
                int x = MathHelper.floor_double(entity.posX);
                int y = MathHelper.floor_double(entity.posY);
                int z = MathHelper.floor_double(entity.posZ);
                if (world.getBlock(x, y, z) == entity.func_145805_f()) data.markPacked(current);
                data.finishFalling(entry.getKey());
                iterator.remove();
            } else if (!world.loadedEntityList.contains(entity)) {
                // Chunk unload: the UUID and latest position stay in WorldSavedData
                // and are re-associated by EntityJoinWorldEvent after reload.
                iterator.remove();
            }
        }
        if (tracked.isEmpty()) protectedFallingBlocks.remove(world);
    }

    static boolean shouldProtectPistonDestination(boolean sourceWasPlaced, boolean conservative) {
        return sourceWasPlaced || conservative;
    }

    static Set<Long> pistonSourcesToRemove(Set<Long> movedPlacedSources, Set<Long> destinations) {
        Set<Long> removals = new LinkedHashSet<Long>(movedPlacedSources);
        removals.removeAll(destinations);
        return removals;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingHurt(LivingHurtEvent event) {
        EntityLivingBase entity = event.entityLiving;
        if (entity instanceof EntityPlayer || entity.worldObj.isRemote) return;
        Entity source = attacker(event.source);
        KillAttribution attribution = attribution(source);
        if (!attribution.relevant) return;
        Map<UUID, RememberedKill> kills = rememberedKills.get(entity.worldObj);
        if (kills == null) {
            kills = new LinkedHashMap<UUID, RememberedKill>();
            rememberedKills.put(entity.worldObj, kills);
        }
        kills.put(entity.getUniqueID(), new RememberedKill(
                attribution.origin,
                attribution.playerId,
                entity.worldObj.getTotalWorldTime() + 100L));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onLivingDrops(LivingDropsEvent event) {
        EntityLivingBase entity = event.entityLiving;
        if (entity instanceof EntityPlayer || entity.worldObj.isRemote) return;
        LegacyConfig config = SmartResourceMultiplier.config();
        KillOrigin origin = resolveKillOrigin(
                entity.worldObj,
                entity.getUniqueID(),
                attacker(event.source),
                event.recentlyHit);
        forgetRememberedKill(entity.worldObj, entity.getUniqueID());
        boolean killEligible = killRequirementEligible(
                config.entityKillRequirement, event.recentlyHit, origin);
        String entityId = LegacyRules.entityId(entity);
        boolean boss = LegacyRules.isBoss(entity);

        if (!event.isCanceled() && killEligible && config.enabled && config.entityDropsEnabled) {
            int multiplier = config.entityMultiplier(
                    entityId, LegacyRules.entityCategory(entity), boss);
            multiplyEntityItems(entity.worldObj, event.drops, multiplier);
        }
        // Vanilla 1.7.10 only creates ordinary living-entity XP when its
        // recentlyHit flag is set. Do not create a proximity context for an
        // environmental death that will never own an orb.
        if (!event.recentlyHit || !entityExperienceEligible(
                config, entityId, boss, killEligible)) return;
        List<ExperienceDeath> deaths = experienceDeaths.get(entity.worldObj);
        if (deaths == null) {
            deaths = new ArrayList<ExperienceDeath>();
            experienceDeaths.put(entity.worldObj, deaths);
        }
        deaths.add(new ExperienceDeath(entity.posX, entity.posY, entity.posZ,
                entity.worldObj.getTotalWorldTime() + (boss ? 240L : 30L),
                config.mobExperienceMultiplier, boss ? 1024.0D : 9.0D));
    }

    private void multiplyExperienceOrb(World world, EntityXPOrb orb) {
        List<ExperienceDeath> deaths = experienceDeaths.get(world);
        if (deaths == null) return;
        for (int index = deaths.size() - 1; index >= 0; index--) {
            ExperienceDeath death = deaths.get(index);
            double x = orb.posX - death.x;
            double y = orb.posY - death.y;
            double z = orb.posZ - death.z;
            if (x * x + y * y + z * z <= death.radiusSquared) {
                orb.xpValue = multiplyExistingExperience(orb.xpValue, death.multiplier);
                return;
            }
        }
    }

    private void pruneExperienceDeaths(World world) {
        List<ExperienceDeath> deaths = experienceDeaths.get(world);
        if (deaths == null) return;
        long now = world.getTotalWorldTime();
        Iterator<ExperienceDeath> iterator = deaths.iterator();
        while (iterator.hasNext()) if (iterator.next().expiresAt < now) iterator.remove();
        if (deaths.isEmpty()) experienceDeaths.remove(world);
    }

    private void pruneRememberedKills(World world) {
        Map<UUID, RememberedKill> kills = rememberedKills.get(world);
        if (kills == null) return;
        long now = world.getTotalWorldTime();
        Iterator<RememberedKill> iterator = kills.values().iterator();
        while (iterator.hasNext()) if (iterator.next().expiresAt < now) iterator.remove();
        if (kills.isEmpty()) rememberedKills.remove(world);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityInteract(EntityInteractEvent event) {
        EntityPlayer player = event.entityPlayer;
        if (player.worldObj.isRemote) return;
        LegacyConfig config = SmartResourceMultiplier.config();
        boolean automated = player instanceof FakePlayer;
        if (!config.enabled
                || (automated ? !config.automatedShearingDropsEnabled : !config.manualShearingDropsEnabled)
                || !(event.target instanceof IShearable)) return;
        ItemStack shears = player.getCurrentEquippedItem();
        if (shears == null || !(shears.getItem() instanceof ItemShears)) return;
        String entityId = LegacyRules.entityId(event.target);
        // 1.7.10 has no data-pack entity tags. Sheep are known-safe standard
        // output; an exact JSON override acts as explicit certification for a
        // compatible modded IShearable. Unknown and permanent/special actions
        // stay entirely vanilla rather than risking duplicated transformations.
        if (!LegacyRules.isSafeShearingEntityId(
                entityId, config.shearingEntityMultipliers.containsKey(entityId))) return;
        int x = MathHelper.floor_double(event.target.posX);
        int y = MathHelper.floor_double(event.target.posY);
        int z = MathHelper.floor_double(event.target.posZ);
        IShearable shearable = (IShearable) event.target;
        if (!shearable.isShearable(shears, player.worldObj, x, y, z)) return;

        int fortune = EnchantmentHelper.getEnchantmentLevel(Enchantment.fortune.effectId, shears);
        ArrayList<ItemStack> drops = shearable.onSheared(shears, player.worldObj, x, y, z, fortune);
        if (drops == null) drops = new ArrayList<ItemStack>();
        multiplyStacks(
                drops,
                config.shearingMultiplier(entityId),
                MAX_SHEARING_STACKS,
                MAX_SHEARING_ITEMS);
        for (ItemStack drop : drops) {
            if (drop == null || drop.stackSize <= 0) continue;
            EntityItem item = new EntityItem(player.worldObj, event.target.posX, event.target.posY + 0.5D,
                    event.target.posZ, drop);
            item.delayBeforeCanPickup = 10;
            player.worldObj.spawnEntityInWorld(item);
        }
        shears.damageItem(1, player);
        event.setCanceled(true);
    }

    private static Entity attacker(DamageSource source) {
        return source == null ? null : source.getEntity();
    }

    private KillOrigin resolveKillOrigin(
            World world, UUID victimId, Entity immediateSource, boolean vanillaPlayerKilled) {
        if (!vanillaPlayerKilled) return KillOrigin.NONE;
        Map<UUID, RememberedKill> kills = rememberedKills.get(world);
        RememberedKill remembered = kills == null ? null : kills.get(victimId);
        if (remembered == null || remembered.expiresAt < world.getTotalWorldTime()) return KillOrigin.NONE;
        KillAttribution immediate = attribution(immediateSource);
        if (!immediate.relevant) return remembered.origin;
        return immediate.origin == remembered.origin
                        && equalUuid(immediate.playerId, remembered.playerId)
                ? immediate.origin
                : KillOrigin.NONE;
    }

    private void forgetRememberedKill(World world, UUID victimId) {
        Map<UUID, RememberedKill> kills = rememberedKills.get(world);
        if (kills == null) return;
        kills.remove(victimId);
        if (kills.isEmpty()) rememberedKills.remove(world);
    }

    private static KillAttribution attribution(Entity source) {
        if (source instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) source;
            return new KillAttribution(
                    true,
                    player instanceof FakePlayer ? KillOrigin.NONE : KillOrigin.DIRECT_PLAYER,
                    player instanceof FakePlayer ? null : player.getUniqueID());
        }
        if (source instanceof EntityTameable && ((EntityTameable) source).isTamed()) {
            Entity owner = ((EntityTameable) source).getOwner();
            if (owner instanceof EntityPlayer && !(owner instanceof FakePlayer)) {
                return new KillAttribution(true, KillOrigin.TAMED_ENTITY, owner.getUniqueID());
            }
            return new KillAttribution(true, KillOrigin.NONE, null);
        }
        return new KillAttribution(false, KillOrigin.NONE, null);
    }

    static boolean killRequirementEligible(
            LegacyConfig.EntityKillRequirement requirement,
            boolean vanillaPlayerKilled,
            KillOrigin origin) {
        if (requirement == LegacyConfig.EntityKillRequirement.ALL_STANDARD_DEATH_LOOT) return true;
        if (!vanillaPlayerKilled) return false;
        if (origin == KillOrigin.DIRECT_PLAYER) return true;
        return requirement == LegacyConfig.EntityKillRequirement.PLAYER_OR_TAMED_ENTITY
                && origin == KillOrigin.TAMED_ENTITY;
    }

    private static boolean equalUuid(UUID left, UUID right) {
        return left == null ? right == null : left.equals(right);
    }

    static boolean isRealPlayer(EntityPlayer player) {
        return player != null && !(player instanceof FakePlayer);
    }

    static boolean entityExperienceEligible(
            LegacyConfig config, String entityId, boolean boss, boolean killEligible) {
        return config.enabled
                && config.multiplyMobExperience
                && config.isEntityAllowed(entityId)
                && killEligible
                && (!boss || config.multiplyBossExperience);
    }

    private void scheduleRemoval(World world, int x, int y, int z) {
        scheduleRemovalPacked(world, PlacedBlockData.pack(x, y, z));
    }

    private void scheduleRemovalPacked(World world, long packed) {
        Set<Long> pending = pendingRemovals.get(world);
        if (pending == null) {
            pending = new LinkedHashSet<Long>();
            pendingRemovals.put(world, pending);
        }
        pending.add(Long.valueOf(packed));
    }

    private void cancelRemoval(World world, int x, int y, int z) {
        cancelRemovalPacked(world, PlacedBlockData.pack(x, y, z));
    }

    private void cancelRemovalPacked(World world, long packed) {
        Set<Long> pending = pendingRemovals.get(world);
        if (pending != null) pending.remove(Long.valueOf(packed));
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
            if (stack == null || stack.stackSize <= 0) continue;
            originals.add(stack.copy());
            sourceItems += stack.stackSize;
        }
        if (!fitsOutputBudget(originals.size(), sourceItems, multiplier, maximumStacks, maximumItems)) return;
        for (int copy = 1; copy < multiplier; copy++) {
            for (ItemStack original : originals) {
                drops.add(original.copy());
            }
        }
    }

    static void multiplyEntityItems(World world, List<EntityItem> drops, int multiplier) {
        if (multiplier <= 0) {
            Iterator<EntityItem> iterator = drops.iterator();
            while (iterator.hasNext()) {
                if (!retainEntityLootAtMultiplier(multiplier, isProtectedEntityLoot(iterator.next()))) {
                    iterator.remove();
                }
            }
            return;
        }
        if (multiplier == 1 || drops.isEmpty()) return;
        List<EntityItem> originals = new ArrayList<EntityItem>(drops.size());
        long sourceItems = 0L;
        for (EntityItem original : drops) {
            if (original == null || original.getEntityItem() == null
                    || original.getEntityItem().stackSize <= 0) continue;
            if (isProtectedEntityLoot(original)) continue;
            originals.add(original);
            sourceItems += original.getEntityItem().stackSize;
        }
        if (!fitsOutputBudget(
                originals.size(), sourceItems, multiplier, MAX_MULTIPLIED_STACKS, MAX_MULTIPLIED_ITEMS)) return;
        for (int copy = 1; copy < multiplier; copy++) {
            for (EntityItem original : originals) {
                ItemStack stack = original.getEntityItem();
                EntityItem duplicate = new EntityItem(world, original.posX, original.posY, original.posZ, stack.copy());
                duplicate.motionX = original.motionX;
                duplicate.motionY = original.motionY;
                duplicate.motionZ = original.motionZ;
                duplicate.delayBeforeCanPickup = original.delayBeforeCanPickup;
                drops.add(duplicate);
            }
        }
    }

    private static boolean isProtectedEntityLoot(EntityItem entityItem) {
        if (entityItem == null || entityItem.getEntityItem() == null) return false;
        // Legacy Forge has no data-pack item tags; preserve the modern tag's
        // default protected output without multiplying it.
        return isProtectedItemIdentity(entityItem.getEntityItem().getItem(), Items.saddle);
    }

    static boolean isProtectedItemIdentity(Object item, Object saddleItem) {
        return item != null && item == saddleItem;
    }

    static boolean retainEntityLootAtMultiplier(int multiplier, boolean protectedOutput) {
        return multiplier > 0 || protectedOutput;
    }

    static boolean fitsOutputBudget(
            int sourceStacks, long sourceItems, int multiplier, int maximumStacks, long maximumItems) {
        int safeMultiplier = Math.max(0, multiplier);
        if (safeMultiplier == 0) return true;
        if (sourceStacks < 0 || sourceItems < 0L || maximumStacks < 0 || maximumItems < 0L) return false;
        return sourceStacks <= maximumStacks / safeMultiplier
                && sourceItems <= maximumItems / safeMultiplier;
    }

    private static int safeMultiply(int value, int multiplier) {
        long result = (long) value * (long) Math.max(0, multiplier);
        return result > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
    }

    static int multiplyBlockExperience(
            int value, LegacyConfig config, boolean sourceEnabled, boolean eligible) {
        if (!config.multiplyExperience || !sourceEnabled || !eligible) return value;
        return safeMultiply(value, config.experienceMultiplier);
    }

    static int multiplyExistingExperience(int value, int multiplier) {
        if (value <= 0 || multiplier <= 1) return value;
        long result = (long) value * (long) multiplier;
        // Forge 1.7.10 exposes the spawned orb rather than the original whole
        // mob-XP award. Apply the modern fail-closed ceiling to that exact
        // observable unit and leave an oversized award entirely vanilla.
        return result > MAX_MULTIPLIED_XP_AWARD ? value : (int) result;
    }

    private static final class ExperienceDeath {
        final double x;
        final double y;
        final double z;
        final long expiresAt;
        final int multiplier;
        final double radiusSquared;

        ExperienceDeath(double x, double y, double z, long expiresAt, int multiplier, double radiusSquared) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.expiresAt = expiresAt;
            this.multiplier = multiplier;
            this.radiusSquared = radiusSquared;
        }
    }

    enum KillOrigin {
        NONE,
        DIRECT_PLAYER,
        TAMED_ENTITY
    }

    private static final class KillAttribution {
        final boolean relevant;
        final KillOrigin origin;
        final UUID playerId;

        KillAttribution(boolean relevant, KillOrigin origin, UUID playerId) {
            this.relevant = relevant;
            this.origin = origin;
            this.playerId = playerId;
        }
    }

    private static final class RememberedKill {
        final KillOrigin origin;
        final UUID playerId;
        final long expiresAt;

        RememberedKill(KillOrigin origin, UUID playerId, long expiresAt) {
            this.origin = origin;
            this.playerId = playerId;
            this.expiresAt = expiresAt;
        }
    }
}
