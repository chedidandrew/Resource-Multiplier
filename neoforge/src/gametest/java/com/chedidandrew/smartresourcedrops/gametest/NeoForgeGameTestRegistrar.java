package com.chedidandrew.smartresourcedrops.gametest;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.gametest.framework.TestFunction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/** Registers the shared test methods through NeoForge 21.1's pre-registry GameTest event. */
@EventBusSubscriber(modid = SmartResourceDrops.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class NeoForgeGameTestRegistrar {
    public static final int EXPECTED_TEST_COUNT = 64;
    private static final String STRUCTURE = "smart_resource_drops_gametest:wide";
    private static final List<Class<?>> SHARED_TEST_CLASSES = List.of(
            SmartResourceDropsGameTests.class,
            SmartResourceDropsShearingGameTests.class,
            SmartResourceDropsBlockBudgetGameTests.class,
            SmartResourceDropsEntityGameTests.class);
    private static final List<Class<?>> NEOFORGE_TEST_CLASSES = List.of(
            NeoForgeAutomationAuthorityGameTests.class,
            NeoForgeMixinAuditGameTests.class);

    private NeoForgeGameTestRegistrar() {
    }

    @SubscribeEvent
    public static void registerTests(final RegisterGameTestsEvent event) {
        try {
            event.register(NeoForgeGameTestRegistrar.class.getDeclaredMethod("generatedSharedTests"));
        } catch (NoSuchMethodException impossible) {
            throw new IllegalStateException("NeoForge GameTest generator method disappeared", impossible);
        }

        final List<Method> nativeMethods = NEOFORGE_TEST_CLASSES.stream()
                .flatMap(type -> Arrays.stream(type.getDeclaredMethods()))
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .filter(method -> !Modifier.isStatic(method.getModifiers()))
                .filter(method -> method.isAnnotationPresent(GameTest.class))
                .sorted(Comparator
                        .comparing((Method method) -> method.getDeclaringClass().getName())
                        .thenComparing(Method::getName))
                .toList();
        nativeMethods.forEach(event::register);

        final int discovered = sharedMethods().size() + nativeMethods.size();
        if (discovered != EXPECTED_TEST_COUNT) {
            throw new IllegalStateException(
                    "Expected " + EXPECTED_TEST_COUNT + " NeoForge GameTests, discovered " + discovered);
        }
        SmartResourceDrops.LOGGER.info(
                "Registered exactly {} NeoForge 1.21.2 GameTests",
                discovered);
    }

    /**
     * NeoForge 21.1 prefixes ordinary test templates with the declaring class.
     * A generator preserves Fabric's full-ID annotations while binding every
     * shared method to one explicit binary 1.21.2 structure.
     */
    @GameTestGenerator
    public static Collection<TestFunction> generatedSharedTests() {
        final ArrayList<TestFunction> tests = new ArrayList<>();
        for (Method method : sharedMethods()) {
            final GameTest annotation = method.getAnnotation(GameTest.class);
            final String className = method.getDeclaringClass().getSimpleName().toLowerCase();
            final String testName = className + "." + method.getName().toLowerCase();
            tests.add(new TestFunction(
                    annotation.batch(),
                    testName,
                    STRUCTURE,
                    StructureUtils.getRotationForRotationSteps(annotation.rotationSteps()),
                    annotation.timeoutTicks(),
                    annotation.setupTicks(),
                    annotation.required(),
                    annotation.manualOnly(),
                    annotation.requiredSuccesses(),
                    annotation.attempts(),
                    annotation.skyAccess(),
                    helper -> invoke(method, helper)));
        }
        return List.copyOf(tests);
    }

    private static List<Method> sharedMethods() {
        return SHARED_TEST_CLASSES.stream()
                .flatMap(type -> Arrays.stream(type.getDeclaredMethods()))
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .filter(method -> !Modifier.isStatic(method.getModifiers()))
                .filter(method -> method.isAnnotationPresent(GameTest.class))
                .sorted(Comparator
                        .comparing((Method method) -> method.getDeclaringClass().getName())
                        .thenComparing(Method::getName))
                .toList();
    }

    private static void invoke(final Method method, final GameTestHelper helper) {
        try {
            final Object instance = method.getDeclaringClass().getDeclaredConstructor().newInstance();
            method.invoke(instance, helper);
        } catch (InvocationTargetException failure) {
            final Throwable cause = failure.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new RuntimeException(cause);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Could not invoke GameTest " + method, failure);
        }
    }
}
