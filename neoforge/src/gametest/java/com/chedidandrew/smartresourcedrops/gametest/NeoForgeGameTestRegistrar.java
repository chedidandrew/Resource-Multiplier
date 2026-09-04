package com.chedidandrew.smartresourcedrops.gametest;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.mojang.serialization.MapCodec;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/** Registers all shared and loader-specific scenarios with NeoForge's 1.21.10 test registry. */
@EventBusSubscriber(modid = SmartResourceDrops.MOD_ID)
public final class NeoForgeGameTestRegistrar {
    public static final int EXPECTED_TEST_COUNT = 64;
    private static final ResourceLocation ENVIRONMENT_ID =
            ResourceLocation.fromNamespaceAndPath("smart_resource_drops_gametest", "default");
    private static final List<Class<?>> TEST_CLASSES = List.of(
            SmartResourceDropsGameTests.class,
            SmartResourceDropsShearingGameTests.class,
            SmartResourceDropsBlockBudgetGameTests.class,
            SmartResourceDropsEntityGameTests.class,
            NeoForgeAutomationAuthorityGameTests.class,
            NeoForgeMixinAuditGameTests.class);

    private NeoForgeGameTestRegistrar() {
    }

    @SubscribeEvent
    public static void registerTests(final RegisterGameTestsEvent event) {
        final Holder<TestEnvironmentDefinition> environment = event.registerEnvironment(
                ENVIRONMENT_ID,
                new TestEnvironmentDefinition.AllOf());
        final List<Method> methods = testMethods();
        if (methods.size() != EXPECTED_TEST_COUNT) {
            throw new IllegalStateException(
                    "Expected " + EXPECTED_TEST_COUNT + " NeoForge GameTests, discovered " + methods.size());
        }

        for (Method method : methods) {
            final GameTest annotation = method.getAnnotation(GameTest.class);
            final String className = method.getDeclaringClass().getSimpleName().toLowerCase();
            final ResourceLocation testId = ResourceLocation.fromNamespaceAndPath(
                    "smart_resource_drops_gametest",
                    className + "." + method.getName().toLowerCase());
            final TestData<Holder<TestEnvironmentDefinition>> data = new TestData<>(
                    environment,
                    ResourceLocation.parse(annotation.structure()),
                    annotation.maxTicks(),
                    annotation.setupTicks(),
                    annotation.required(),
                    annotation.rotation(),
                    annotation.manualOnly(),
                    annotation.maxAttempts(),
                    annotation.requiredSuccesses(),
                    annotation.skyAccess());
            event.registerTest(testId, new ReflectiveGameTestInstance(data, method));
        }

        SmartResourceDrops.LOGGER.info(
                "Registered exactly {} NeoForge 1.21.10 GameTests",
                methods.size());
    }

    private static List<Method> testMethods() {
        return TEST_CLASSES.stream()
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

    private static final class ReflectiveGameTestInstance extends GameTestInstance {
        private final Method method;

        private ReflectiveGameTestInstance(
                final TestData<Holder<TestEnvironmentDefinition>> data,
                final Method method) {
            super(data);
            this.method = method;
        }

        @Override
        public void run(final GameTestHelper helper) {
            invoke(method, helper);
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            throw new UnsupportedOperationException(
                    "Runtime-registered Smart Resource Multiplier tests are not serialized");
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("Smart Resource Multiplier Java test");
        }
    }
}
