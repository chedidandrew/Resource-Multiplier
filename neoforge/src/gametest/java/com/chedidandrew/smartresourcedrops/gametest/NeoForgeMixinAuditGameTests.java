package com.chedidandrew.smartresourcedrops.gametest;

import com.chedidandrew.smartresourcedrops.provenance.ProtectedFallingBlock;
import com.chedidandrew.smartresourcedrops.provenance.ProtectedPistonMovement;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import org.spongepowered.asm.mixin.MixinEnvironment;

/** NeoForge-native replacements for the Fabric loader and mixin audit GameTests. */
public final class NeoForgeMixinAuditGameTests {
    private static final List<String> SHEARING_MIXIN_RESOURCES = List.of(
            "com/chedidandrew/smartresourcedrops/mixin/PlayerShearingContextMixin.class",
            "com/chedidandrew/smartresourcedrops/mixin/ShearsDispenseItemBehaviorMixin.class",
            "com/chedidandrew/smartresourcedrops/mixin/LivingEntityShearingLootMixin.class",
            "com/chedidandrew/smartresourcedrops/platform/neoforge/mixin/NeoForgeShearsDispenseItemBehaviorMixin.class");

    @GameTest(structure = "smart_resource_drops_gametest:wide", maxTicks = 100)
    public void dedicatedServerLoadsEveryRequiredNeoForgeMixin(final GameTestHelper helper) {
        GameTestAssertions.assertTrue(helper,
                FMLEnvironment.getDist() == Dist.DEDICATED_SERVER,
                "GameTest must run on a dedicated-server NeoForge distribution");
        GameTestAssertions.assertFalse(helper,
                ModList.get().isLoaded("modmenu"),
                "Optional Mod Menu leaked onto the NeoForge server runtime");
        final ClassLoader classLoader = NeoForgeMixinAuditGameTests.class.getClassLoader();
        GameTestAssertions.assertTrue(helper,
                classLoader.getResource("com/terraformersmc/modmenu/api/ModMenuApi.class") == null,
                "Mod Menu API class leaked onto the NeoForge server runtime");

        MixinEnvironment.getCurrentEnvironment().audit();
        GameTestAssertions.assertTrue(helper,
                ProtectedFallingBlock.class.isAssignableFrom(FallingBlockEntity.class),
                "Falling-block provenance mixin was not applied");
        GameTestAssertions.assertTrue(helper,
                ProtectedPistonMovement.class.isAssignableFrom(PistonMovingBlockEntity.class),
                "Piston provenance mixin was not applied");
        helper.succeed();
    }

    @GameTest(structure = "smart_resource_drops_gametest:wide", maxTicks = 100)
    public void dedicatedServerAuditsAllNeoForgeShearingMixins(final GameTestHelper helper) {
        GameTestAssertions.assertTrue(helper,
                FMLEnvironment.getDist() == Dist.DEDICATED_SERVER,
                "Shearing GameTests must run on a dedicated-server NeoForge distribution");
        final ClassLoader classLoader = NeoForgeMixinAuditGameTests.class.getClassLoader();
        for (String resource : SHEARING_MIXIN_RESOURCES) {
            GameTestAssertions.assertTrue(helper,
                    classLoader.getResource(resource) != null,
                    "Dedicated server omitted required shearing mixin class " + resource);
        }
        MixinEnvironment.getCurrentEnvironment().audit();
        helper.succeed();
    }
}
