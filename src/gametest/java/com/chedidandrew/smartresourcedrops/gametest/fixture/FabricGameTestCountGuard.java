package com.chedidandrew.smartresourcedrops.gametest.fixture;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.gametest.SmartResourceDropsBlockBudgetGameTests;
import com.chedidandrew.smartresourcedrops.gametest.SmartResourceDropsEntityGameTests;
import com.chedidandrew.smartresourcedrops.gametest.SmartResourceDropsGameTests;
import com.chedidandrew.smartresourcedrops.gametest.SmartResourceDropsShearingGameTests;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.api.ModInitializer;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fails before launch if Fabric's canonical GameTest discovery set drifts. */
public final class FabricGameTestCountGuard implements ModInitializer {
    public static final int EXPECTED_TEST_COUNT = 62;
    private static final List<Class<?>> TEST_CLASSES = List.of(
            SmartResourceDropsGameTests.class,
            SmartResourceDropsShearingGameTests.class,
            SmartResourceDropsBlockBudgetGameTests.class,
            SmartResourceDropsEntityGameTests.class);

    @Override
    public void onInitialize() {
        final long count = TEST_CLASSES.stream()
                .flatMap(type -> Arrays.stream(type.getDeclaredMethods()))
                .filter(FabricGameTestCountGuard::isGameTest)
                .count();
        if (count != EXPECTED_TEST_COUNT) {
            throw new IllegalStateException(
                    "Fabric GameTest discovery drifted: expected "
                            + EXPECTED_TEST_COUNT + " tests, found " + count);
        }
        SmartResourceDrops.LOGGER.info(
                "Smart Resource Multiplier Fabric GameTest discovery: {} tests",
                count);
    }

    private static boolean isGameTest(final Method method) {
        return Modifier.isPublic(method.getModifiers())
                && !Modifier.isStatic(method.getModifiers())
                && method.getReturnType() == void.class
                && Arrays.equals(method.getParameterTypes(), new Class<?>[] {GameTestHelper.class})
                && method.isAnnotationPresent(GameTest.class);
    }
}
