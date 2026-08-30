package com.chedidandrew.smartresourcedrops.core.shearing;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.config.ConfigManager;
import com.chedidandrew.smartresourcedrops.core.util.BoundedRateLimiter;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Nested server-thread state for a supported shearing action.
 * Output is identity-matched to the top scope and retained until the one real shear call succeeds.
 */
public final class ShearingActionContext {
    private static final ThreadLocal<Deque<Session>> SESSIONS = new ThreadLocal<>();
    private static final BoundedRateLimiter WARNINGS = new BoundedRateLimiter(
            256,
            Duration.ofMinutes(5L).toNanos());

    private ShearingActionContext() {
    }

    public static Scope beginManual(
            LivingEntity target,
            ServerLevel level,
            Player responsiblePlayer
    ) {
        Objects.requireNonNull(responsiblePlayer, "responsiblePlayer");
        boolean trustedManualSource = responsiblePlayer instanceof ServerPlayer
                && !(responsiblePlayer instanceof FakePlayer);
        return begin(
                target,
                level,
                ShearingSource.MANUAL_PLAYER,
                responsiblePlayer.getUUID(),
                trustedManualSource);
    }

    public static Scope beginDispenser(LivingEntity target, ServerLevel level) {
        return begin(target, level, ShearingSource.VANILLA_DISPENSER, null, true);
    }

    public static BiConsumer<ServerLevel, ItemStack> wrapLootConsumer(
            LivingEntity target,
            ServerLevel level,
            BiConsumer<ServerLevel, ItemStack> downstream
    ) {
        Objects.requireNonNull(downstream, "downstream");
        Session session = activeSession(target, level);
        if (session == null || session.output == null) {
            return downstream;
        }

        BiConsumer<ServerLevel, ItemStack> buffered = session.output.openBatch(level, downstream);
        if (buffered == downstream) {
            return downstream;
        }
        return (outputLevel, stack) -> {
            if (activeSession(target, level) == session && outputLevel == level) {
                buffered.accept(outputLevel, stack);
            } else {
                downstream.accept(outputLevel, stack);
            }
        };
    }

    public static @Nullable ShearingRuleTrace activeTrace(LivingEntity target) {
        Deque<Session> stack = SESSIONS.get();
        if (stack == null) {
            SESSIONS.remove();
            return null;
        }
        Session session = stack.peek();
        return session != null && session.target == target ? session.trace : null;
    }

    private static Scope begin(
            LivingEntity target,
            ServerLevel level,
            ShearingSource source,
            @Nullable UUID responsiblePlayer,
            boolean captureAllowed
    ) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(level, "level");
        if (!(target instanceof Shearable) || target.level() != level) {
            return Scope.NOOP;
        }
        ShearingRuleTrace trace = ShearingRuleResolver.trace(ConfigManager.get(), target.getType(), source);
        boolean captureOutput = captureAllowed
                && trace.multiplicationEligible()
                && trace.appliedMultiplier() != 1;

        // Even a disabled, fixed-1x, or untrusted nested action needs a frame. Otherwise a
        // re-entrant action on the same entity could be captured by an eligible outer source.
        Session session = new Session(
                target,
                level,
                source,
                responsiblePlayer,
                trace,
                captureOutput);
        Deque<Session> stack = SESSIONS.get();
        if (stack == null) {
            stack = new ArrayDeque<>();
            SESSIONS.set(stack);
        }
        stack.push(session);
        return new Scope(session);
    }

    private static @Nullable Session activeSession(LivingEntity target, ServerLevel level) {
        Deque<Session> stack = SESSIONS.get();
        if (stack == null) {
            SESSIONS.remove();
            return null;
        }
        Session session = stack.peek();
        return session != null && session.target == target && session.level == level
                ? session
                : null;
    }

    public static final class Scope implements AutoCloseable {
        private static final Scope NOOP = new Scope(null);
        private final @Nullable Session session;
        private boolean closed;

        private Scope(@Nullable Session session) {
            this.session = session;
        }

        public void complete() {
            if (closed || session == null) {
                return;
            }
            if (session.output == null) {
                return;
            }
            if (!isTop(session)) {
                session.output.abort();
                return;
            }
            session.output.complete();
        }

        public void abort() {
            if (closed || session == null || session.output == null) {
                return;
            }
            session.output.abort();
        }

        @Override
        public void close() {
            if (closed || session == null) {
                return;
            }
            closed = true;
            if (session.output != null && !session.output.finished()) {
                session.output.abort();
            }

            Deque<Session> stack = SESSIONS.get();
            if (stack == null) {
                SESSIONS.remove();
                return;
            }
            Session removed = stack.poll();
            if (removed != session) {
                SmartResourceDrops.LOGGER.warn(
                        "Shearing action context mismatch; clearing scoped state safely");
                stack.clear();
            }
            if (stack.isEmpty()) {
                SESSIONS.remove();
            }
        }

        private static boolean isTop(Session session) {
            Deque<Session> stack = SESSIONS.get();
            return stack != null && stack.peek() == session;
        }
    }

    private static final class Session {
        private final LivingEntity target;
        private final ServerLevel level;
        private final UUID targetUuid;
        private final String entityId;
        private final ResourceKey<Level> dimension;
        private final Vec3 position;
        private final ShearingSource source;
        private final @Nullable UUID responsiblePlayer;
        private final ShearingRuleTrace trace;
        private final @Nullable ShearingOutputBuffer<ServerLevel> output;

        private Session(
                LivingEntity target,
                ServerLevel level,
                ShearingSource source,
                @Nullable UUID responsiblePlayer,
                ShearingRuleTrace trace,
                boolean captureOutput
        ) {
            this.target = target;
            this.level = level;
            this.targetUuid = target.getUUID();
            this.entityId = trace.entityId();
            this.dimension = level.dimension();
            this.position = target.position();
            this.source = source;
            this.responsiblePlayer = responsiblePlayer;
            this.trace = trace;
            this.output = captureOutput
                    ? new ShearingOutputBuffer<>(
                            trace.appliedMultiplier(),
                            this::warnBudgetFallback,
                            this::warnRollbackFailure)
                    : null;
        }

        private void warnBudgetFallback(ShearingOutputBudget.Result result) {
            String key = entityId + "|" + source + "|" + result.limitExceeded();
            if (!WARNINGS.tryAcquire(key, System.nanoTime())) {
                return;
            }
            SmartResourceDrops.LOGGER.warn(
                    "Shearing output budget exceeded for {} {} at {} in {} from {}; "
                            + "the complete action is falling back to vanilla 1x "
                            + "(reason: {}; original: {} items / {} source stacks; "
                            + "proposed: {} items / {} materialized stacks; limits: {} items / {} stacks)",
                    entityId,
                    targetUuid,
                    position,
                    dimension.identifier(),
                    source,
                    result.limitExceeded(),
                    result.originalItems(),
                    result.sourceStacks(),
                    result.multipliedItems(),
                    result.materializedStacks(),
                    ShearingOutputBudget.MAX_MULTIPLIED_ITEMS,
                    ShearingOutputBudget.MAX_SOURCE_OR_MATERIALIZED_STACKS);
        }

        private void warnRollbackFailure(Throwable failure) {
            String key = entityId + "|" + source + "|ROLLBACK";
            if (!WARNINGS.tryAcquire(key, System.nanoTime())) {
                return;
            }
            SmartResourceDrops.LOGGER.warn(
                    "Could not fully emit buffered vanilla shearing output while propagating an action failure "
                            + "for {} {} at {} in {} from {}; scoped state was still cleared",
                    entityId,
                    targetUuid,
                    position,
                    dimension.identifier(),
                    source,
                    failure);
        }
    }
}
