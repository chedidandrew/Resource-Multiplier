package com.chedidandrew.smartresourcedrops.gametest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;

import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Registers the loader-neutral test bodies through Forge 47's legacy GameTest API. */
@Mod.EventBusSubscriber(
        modid = SmartResourceDrops.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class NeoForgeGameTestRegistrar {
    public static final int EXPECTED_TEST_COUNT = 62;
    private static final String TEST_NAMESPACE = "smart_resource_drops_gametest";
    private static final String WIDE_STRUCTURE = TEST_NAMESPACE + ":wide";
    private static final Set<String> EXCLUDED = Set.of(
            "dedicatedServerLoadsEveryRequiredMixin",
            "dedicatedServerAuditsAllShearingMixins");
    private static final List<TestSpec> TESTS = discover();

    private NeoForgeGameTestRegistrar() {
    }

    @SubscribeEvent
    public static void register(final RegisterGameTestsEvent event) {
        event.register(NeoForgeGameTestRegistrar.class);
    }

    @GameTestGenerator
    public static Collection<TestFunction> generateTests() {
        SmartResourceDrops.LOGGER.info(
                "Smart Resource Multiplier NeoForge GameTest discovery: {} tests",
                TESTS.size());
        return TESTS.stream().map(spec -> new TestFunction(
                "defaultBatch",
                spec.id().toString(),
                WIDE_STRUCTURE,
                Rotation.NONE,
                20,
                0L,
                true,
                spec.function())).toList();
    }

    private static List<TestSpec> discover() {
        final List<TestSpec> result = new ArrayList<>();
        addTests(result, SmartResourceDropsGameTests.class);
        addTests(result, SmartResourceDropsShearingGameTests.class);
        addTests(result, SmartResourceDropsBlockBudgetGameTests.class);
        addTests(result, SmartResourceDropsEntityGameTests.class);
        addTests(result, NeoForgeMixinAuditGameTests.class);
        if (result.size() != EXPECTED_TEST_COUNT) {
            throw new IllegalStateException(
                    "NeoForge GameTest discovery drifted: expected "
                            + EXPECTED_TEST_COUNT + " tests, found " + result.size());
        }
        return List.copyOf(result);
    }

    private static void addTests(final List<TestSpec> result, final Class<?> type) {
        Arrays.stream(type.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .filter(method -> !Modifier.isStatic(method.getModifiers()))
                .filter(method -> method.getReturnType() == void.class)
                .filter(method -> Arrays.equals(
                        method.getParameterTypes(), new Class<?>[] {GameTestHelper.class}))
                .filter(method -> !EXCLUDED.contains(method.getName()))
                .sorted(Comparator.comparing(Method::getName))
                .map(method -> spec(type, method))
                .forEach(result::add);
    }

    private static TestSpec spec(final Class<?> type, final Method method) {
        final Object target;
        try {
            target = type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
        final Consumer<GameTestHelper> function = helper -> invoke(method, target, helper);
        final String path = (type.getSimpleName() + "_" + method.getName())
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .toLowerCase(Locale.ROOT);
        return new TestSpec(new ResourceLocation(TEST_NAMESPACE, path), function);
    }

    private static void invoke(
            final Method method,
            final Object target,
            final GameTestHelper helper
    ) {
        try {
            method.invoke(target, helper);
        } catch (InvocationTargetException exception) {
            final Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new RuntimeException("GameTest threw a checked exception: " + method, cause);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("Could not invoke GameTest: " + method, exception);
        }
    }

    private record TestSpec(ResourceLocation id, Consumer<GameTestHelper> function) {
    }
}
