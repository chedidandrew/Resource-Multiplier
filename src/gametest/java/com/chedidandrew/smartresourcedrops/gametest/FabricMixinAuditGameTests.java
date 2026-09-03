package com.chedidandrew.smartresourcedrops.gametest;

import com.chedidandrew.smartresourcedrops.provenance.ProtectedFallingBlock;
import com.chedidandrew.smartresourcedrops.provenance.ProtectedPistonMovement;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import org.spongepowered.asm.mixin.MixinEnvironment;

/** Fabric-only dedicated-server classpath and applied-mixin audits. */
public final class FabricMixinAuditGameTests {
    @GameTest(template = "smart_resource_drops_gametest:wide")
    public void dedicatedServerLoadsEveryRequiredMixin(final GameTestHelper helper) {
        final FabricLoader loader = FabricLoader.getInstance();
        helper.assertTrue(
                loader.getEnvironmentType() == EnvType.SERVER,
                "GameTest must run on a dedicated server");
        helper.assertFalse(
                loader.isModLoaded("modmenu"),
                "Optional Mod Menu leaked onto the server runtime");
        helper.assertTrue(
                loader.getClass().getClassLoader()
                        .getResource("com/terraformersmc/modmenu/api/ModMenuApi.class") == null,
                "Mod Menu API class leaked onto the server runtime");
        MixinEnvironment.getCurrentEnvironment().audit();
        helper.assertTrue(
                ProtectedFallingBlock.class.isAssignableFrom(FallingBlockEntity.class),
                "Falling-block provenance mixin was not applied");
        helper.assertTrue(
                ProtectedPistonMovement.class.isAssignableFrom(PistonMovingBlockEntity.class),
                "Piston provenance mixin was not applied");
        helper.succeed();
    }

    @GameTest(template = "smart_resource_drops_gametest:wide")
    public void dedicatedServerAuditsAllThreeShearingMixins(final GameTestHelper helper) {
        final FabricLoader loader = FabricLoader.getInstance();
        helper.assertTrue(
                loader.getEnvironmentType() == EnvType.SERVER,
                "Shearing GameTests must run on the dedicated-server environment");
        final ClassLoader classLoader = loader.getClass().getClassLoader();
        for (String resource : List.of(
                "com/chedidandrew/smartresourcedrops/mixin/PlayerShearingContextMixin.class",
                "com/chedidandrew/smartresourcedrops/mixin/ShearsDispenseItemBehaviorMixin.class",
                "com/chedidandrew/smartresourcedrops/mixin/SheepShearingLootMixin.class")) {
            helper.assertTrue(
                    classLoader.getResource(resource) != null,
                    "Dedicated server omitted required shearing mixin class " + resource);
        }
        MixinEnvironment.getCurrentEnvironment().audit();
        helper.succeed();
    }
}
