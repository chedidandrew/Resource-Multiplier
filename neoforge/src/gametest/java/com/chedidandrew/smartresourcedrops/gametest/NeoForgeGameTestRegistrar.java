package com.chedidandrew.smartresourcedrops.gametest;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Registers the loader-neutral Fabric GameTest bodies with NeoForge's 26.2 registry API. */
@EventBusSubscriber(modid = SmartResourceDrops.MOD_ID)
public final class NeoForgeGameTestRegistrar {
    private static final String TEST_NAMESPACE = "smart_resource_drops_gametest";
    private static final Identifier EMPTY_STRUCTURE = id("empty");
    private static final Set<String> EXCLUDED = Set.of(
            "dedicatedServerLoadsEveryRequiredMixin",
            "dedicatedServerAuditsAllThreeShearingMixins");
    private static final Set<String> PAD_16 = Set.of(
            "sixtyFourMultiplierUsesLegalStacksForRealSheep",
            "enabledVanillaDispenserSourceMultipliesSheep");
    private static final List<TestSpec> TESTS = discover();

    private NeoForgeGameTestRegistrar() {}

    @SubscribeEvent
    public static void registerFunctions(final RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, helper -> {
            for (TestSpec spec : TESTS) {
                helper.register(spec.id(), spec.function());
            }
        });
    }

    @SubscribeEvent
    public static void registerTests(final RegisterGameTestsEvent event) {
        final Holder<TestEnvironmentDefinition<?>> environment =
                event.registerEnvironment(id("environment"));
        for (TestSpec spec : TESTS) {
            final ResourceKey<Consumer<GameTestHelper>> functionKey =
                    ResourceKey.create(Registries.TEST_FUNCTION, spec.id());
            final TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                    environment, EMPTY_STRUCTURE, 20, 0, true, Rotation.NONE,
                    false, 1, 1, false, spec.padding());
            event.registerTest(spec.id(), new FunctionGameTestInstance(functionKey, data));
        }
    }

    private static List<TestSpec> discover() {
        final List<TestSpec> result = new ArrayList<>();
        addTests(result, SmartResourceDropsGameTests.class);
        addTests(result, SmartResourceDropsShearingGameTests.class);
        addTests(result, SmartResourceDropsBlockBudgetGameTests.class);
        addTests(result, SmartResourceDropsEntityGameTests.class);
        addTests(result, NeoForgeMixinAuditGameTests.class);
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
        final int padding = type == SmartResourceDropsEntityGameTests.class
                ? 32
                : PAD_16.contains(method.getName()) ? 16 : 1;
        return new TestSpec(id(path), function, padding);
    }

    private static void invoke(
            final Method method,
            final Object target,
            final GameTestHelper helper) {
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

    private static Identifier id(final String path) {
        return Identifier.fromNamespaceAndPath(TEST_NAMESPACE, path);
    }

    private record TestSpec(
            Identifier id,
            Consumer<GameTestHelper> function,
            int padding) {}
}
