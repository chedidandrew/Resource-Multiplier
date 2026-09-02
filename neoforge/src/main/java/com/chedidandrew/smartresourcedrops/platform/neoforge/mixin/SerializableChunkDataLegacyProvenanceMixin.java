package com.chedidandrew.smartresourcedrops.platform.neoforge.mixin;

import com.chedidandrew.smartresourcedrops.platform.neoforge.LegacyFabricProvenanceMigration;
import com.chedidandrew.smartresourcedrops.provenance.PlacedBlockData;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Makes Fabric placement markers visible to NeoForge's normal attachment decoder. */
@Mixin(SerializableChunkData.class)
abstract class SerializableChunkDataLegacyProvenanceMixin
        implements LegacyFabricProvenanceMigration.Carrier {
    @Unique
    private PlacedBlockData smart_resource_drops$legacyFabricProvenance;

    @Override
    public PlacedBlockData smart_resource_drops$getLegacyFabricProvenance() {
        return smart_resource_drops$legacyFabricProvenance;
    }

    @Override
    public void smart_resource_drops$setLegacyFabricProvenance(final PlacedBlockData data) {
        smart_resource_drops$legacyFabricProvenance = data;
    }

    @Inject(method = "parse", at = @At("RETURN"))
    private static void smart_resource_drops$readFabricAttachment(
            final LevelHeightAccessor levelHeight,
            final PalettedContainerFactory containerFactory,
            final CompoundTag chunkData,
            final CallbackInfoReturnable<SerializableChunkData> cir
    ) {
        final SerializableChunkData data = cir.getReturnValue();
        if (data != null) {
            ((LegacyFabricProvenanceMigration.Carrier) (Object) data)
                    .smart_resource_drops$setLegacyFabricProvenance(
                            LegacyFabricProvenanceMigration.decode(chunkData));
        }
    }
}
