package com.chedidandrew.smartresourcedrops.config;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.LinkedHashSet;
import java.util.Set;

/** Per-execution adapter over the server's current registry and tag bindings. */
public final class LiveConfigRegistryView implements ConfigRegistryView {
    private final Registry<Block> blocks;
    private final Registry<EntityType<?>> entityTypes;
    private final MinecraftServer server;

    private LiveConfigRegistryView(
            final Registry<Block> blocks,
            final Registry<EntityType<?>> entityTypes,
            final MinecraftServer server
    ) {
        this.blocks = blocks;
        this.entityTypes = entityTypes;
        this.server = server;
    }

    public static LiveConfigRegistryView from(final CommandSourceStack source) {
        return new LiveConfigRegistryView(
                source.registryAccess().lookupOrThrow(Registries.BLOCK),
                source.registryAccess().lookupOrThrow(Registries.ENTITY_TYPE),
                source.getServer());
    }

    @Override
    public boolean blockExists(final String identifier) {
        final ResourceLocation id = ResourceLocation.tryParse(identifier);
        return id != null && blocks.containsKey(id);
    }

    @Override
    public boolean entityExists(final String identifier) {
        final ResourceLocation id = ResourceLocation.tryParse(identifier);
        return id != null && entityTypes.containsKey(id);
    }

    @Override
    public boolean dimensionExists(final String identifier) {
        final ResourceLocation id = ResourceLocation.tryParse(identifier);
        return id != null && server.getLevel(ResourceKey.create(Registries.DIMENSION, id)) != null;
    }

    @Override
    public boolean blockTagBound(final String identifier) {
        final ResourceLocation id = ResourceLocation.tryParse(identifier);
        return id != null && blocks.get(TagKey.create(Registries.BLOCK, id)).isPresent();
    }

    @Override
    public boolean entityTagBound(final String identifier) {
        final ResourceLocation id = ResourceLocation.tryParse(identifier);
        return id != null && entityTypes.get(TagKey.create(Registries.ENTITY_TYPE, id)).isPresent();
    }

    @Override
    public Set<String> entityIdsInTag(final String identifier) {
        final ResourceLocation id = ResourceLocation.tryParse(identifier);
        if (id == null) {
            return Set.of();
        }
        final LinkedHashSet<String> members = new LinkedHashSet<>();
        entityTypes.get(TagKey.create(Registries.ENTITY_TYPE, id)).ifPresent(tag -> tag.stream()
                .map(holder -> EntityType.getKey(holder.value()))
                .filter(java.util.Objects::nonNull)
                .map(ResourceLocation::toString)
                .sorted()
                .forEach(members::add));
        return Set.copyOf(members);
    }

    @Override
    public BlockEntityCapability blockEntityCapability(final String identifier) {
        final ResourceLocation id = ResourceLocation.tryParse(identifier);
        if (id == null) {
            return BlockEntityCapability.UNKNOWN;
        }
        if (!blocks.containsKey(id)) {
            return BlockEntityCapability.UNKNOWN;
        }
        final Block block = blocks.getValue(id);
        if (block == null) {
            return BlockEntityCapability.UNKNOWN;
        }
        return block.defaultBlockState().hasBlockEntity()
                ? BlockEntityCapability.YES
                : BlockEntityCapability.NO;
    }
}
