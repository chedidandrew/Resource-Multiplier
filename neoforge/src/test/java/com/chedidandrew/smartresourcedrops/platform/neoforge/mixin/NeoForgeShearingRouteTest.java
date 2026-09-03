package com.chedidandrew.smartresourcedrops.platform.neoforge.mixin;

import com.chedidandrew.smartresourcedrops.platform.neoforge.NeoForgeShearingRoute;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.IForgeShearable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NeoForgeShearingRouteTest {
    @Test
    void dualVanillaAndForgeShearableIsOwnedOnlyByGenericSpawnCapture() {
        assertFalse(NeoForgeShearingRoute.ownsForgeOnlyPath(new DualShearable()));
    }

    @Test
    void forgeOnlyShearableKeepsLegacyListTransformation() {
        assertTrue(NeoForgeShearingRoute.ownsForgeOnlyPath(new ForgeOnlyShearable()));
    }

    private static class ForgeOnlyShearable implements IForgeShearable {
        @Override
        public boolean isShearable(
                final ItemStack item,
                final Level level,
                final BlockPos pos
        ) {
            return true;
        }

        @Override
        public List<ItemStack> onSheared(
                final Player player,
                final ItemStack item,
                final Level level,
                final BlockPos pos,
                final int fortune
        ) {
            return List.of();
        }
    }

    private static final class DualShearable extends ForgeOnlyShearable implements Shearable {
        @Override
        public void shear(final SoundSource source) {
        }

        @Override
        public boolean readyForShearing() {
            return true;
        }
    }
}
