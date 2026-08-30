package com.chedidandrew.smartresourcedrops.core.provenance;

import static com.chedidandrew.smartresourcedrops.core.provenance.ProvenanceTransitionPolicy.Result.PRESERVE;
import static com.chedidandrew.smartresourcedrops.core.provenance.ProvenanceTransitionPolicy.Result.REMOVE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

final class ProvenanceTransitionPolicyTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void preservesResourceIdentityAcrossVanillaTransforms() {
        assertEquals(PRESERVE, classify(Blocks.DIRT, Blocks.FARMLAND));
        assertEquals(PRESERVE, classify(Blocks.DIRT, Blocks.DIRT_PATH));
        assertEquals(PRESERVE, classify(Blocks.OAK_LOG, Blocks.STRIPPED_OAK_LOG));
        assertEquals(PRESERVE, classify(
            Blocks.COPPER_BLOCK.weathering().unaffected(),
            Blocks.COPPER_BLOCK.weathering().exposed()));
        assertEquals(PRESERVE, classify(Blocks.CONCRETE_POWDER.white(), Blocks.CONCRETE.white()));
    }

    @Test
    void unrelatedReplacementDoesNotInheritProvenance() {
        assertEquals(REMOVE, classify(Blocks.STONE, Blocks.DIAMOND_ORE));
    }

    private static ProvenanceTransitionPolicy.Result classify(final Block oldBlock, final Block newBlock) {
        return ProvenanceTransitionPolicy.classify(oldBlock.defaultBlockState(), newBlock.defaultBlockState());
    }
}
