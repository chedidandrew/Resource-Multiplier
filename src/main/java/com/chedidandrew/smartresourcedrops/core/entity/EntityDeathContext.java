package com.chedidandrew.smartresourcedrops.core.entity;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.config.ConfigManager;
import com.chedidandrew.smartresourcedrops.config.SmartDropsConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import java.util.function.Consumer;

/** Nested, server-thread-scoped state for exactly one vanilla standard-death-loot call. */
public final class EntityDeathContext {
    private static final ThreadLocal<Deque<Session>> SESSIONS = new ThreadLocal<>();
    private static final ThreadLocal<Deque<EntityExperienceAwardToken>> MOB_EXPERIENCE_AWARDS =
            new ThreadLocal<>();

    private EntityDeathContext() {
    }

    public static Scope begin(LivingEntity entity, ServerLevel level, DamageSource source) {
        SmartDropsConfig config = ConfigManager.get();
        if (!EntityMultiplierResolver.rulesCouldApply(config, entity)) {
            return Scope.NOOP;
        }
        Deque<Session> stack = SESSIONS.get();
        Session active = stack == null ? null : stack.peek();
        if (active != null && active.entity == entity) {
            return Scope.NOOP;
        }
        Session session = new Session(entity, level, source, EntityMultiplierResolver.trace(level, entity, source));
        if (stack == null) {
            stack = new ArrayDeque<>();
            SESSIONS.set(stack);
        }
        stack.push(session);
        return new Scope(session);
    }

    public static Consumer<ItemStack> wrapStandardLootConsumer(
            LivingEntity entity,
            Consumer<ItemStack> downstream
    ) {
        Session session = activeSession(entity);
        if (session == null) {
            return downstream;
        }
        EntityRuleTrace trace = session.trace;
        if (!trace.itemEligible() || trace.appliedMultiplier() == 1) {
            return downstream;
        }
        return session.lootOutput.wrap(
                downstream,
                trace.appliedMultiplier(),
                () -> activeSession(entity) == session,
                () -> SmartResourceDrops.LOGGER.warn(
                        "Entity loot multiplication budget exceeded for {} {} at {} in {}; "
                                + "remaining standard loot is being left vanilla (limit: {} items / {} stacks)",
                        session.entityType,
                        session.entityUuid,
                        session.deathPosition,
                        session.dimension.identifier(),
                        EntityLootOutputController.MAX_MULTIPLIED_ITEMS,
                        EntityLootOutputController.MAX_MULTIPLIED_STACKS));
    }

    public static int multiplyExperience(LivingEntity entity, int amount) {
        Session session = activeSession(entity);
        if (session == null || session.experienceClaimed) {
            return amount;
        }
        session.experienceClaimed = true;
        EntityRuleTrace trace = session.trace;
        if (amount <= 0 || !trace.experienceEligible()) {
            return amount;
        }
        EntityExperienceBudget.Result result = EntityExperienceBudget.multiply(
                amount,
                trace.appliedExperienceMultiplier());
        if (result.budgetExceeded() && !session.experienceBudgetWarned) {
            session.experienceBudgetWarned = true;
            SmartResourceDrops.LOGGER.warn(
                    "Entity XP multiplication budget exceeded for {} {} at {} in {}; "
                            + "the original {} XP award is being left vanilla (limit: {})",
                    session.entityType,
                    session.entityUuid,
                    session.deathPosition,
                    session.dimension.identifier(),
                    amount,
                    EntityExperienceBudget.MAX_MULTIPLIED_XP_AWARD);
        }
        return result.amount();
    }

    public static @Nullable EntityRuleTrace activeTrace(LivingEntity entity) {
        Session session = activeSession(entity);
        return session == null ? null : session.trace;
    }

    public static ExperienceAwardScope expectMobExperienceAward(
            ServerLevel level,
            Vec3 position,
            int amount
    ) {
        EntityExperienceAwardToken token = new EntityExperienceAwardToken(level, position, amount);
        Deque<EntityExperienceAwardToken> stack = MOB_EXPERIENCE_AWARDS.get();
        if (stack == null) {
            stack = new ArrayDeque<>();
            MOB_EXPERIENCE_AWARDS.set(stack);
        }
        stack.push(token);
        return new ExperienceAwardScope(token);
    }

    public static boolean consumeExpectedMobExperienceAward(
            ServerLevel level,
            Vec3 position,
            int amount
    ) {
        Deque<EntityExperienceAwardToken> stack = MOB_EXPERIENCE_AWARDS.get();
        if (stack == null) {
            MOB_EXPERIENCE_AWARDS.remove();
            return false;
        }
        EntityExperienceAwardToken token = stack.peek();
        return token != null && token.consume(level, position, amount);
    }

    private static @Nullable Session activeSession(LivingEntity entity) {
        Deque<Session> stack = SESSIONS.get();
        if (stack == null) {
            SESSIONS.remove();
            return null;
        }
        Session session = stack.peek();
        return session != null && session.entity == entity ? session : null;
    }

    public static final class Scope implements AutoCloseable {
        private static final Scope NOOP = new Scope(null);
        private final @Nullable Session session;
        private boolean closed;

        private Scope(@Nullable Session session) {
            this.session = session;
        }

        @Override
        public void close() {
            if (closed || session == null) {
                return;
            }
            closed = true;
            Deque<Session> stack = SESSIONS.get();
            if (stack == null) {
                return;
            }
            if (stack.isEmpty()) {
                SESSIONS.remove();
                return;
            }
            Session removed = stack.pop();
            if (removed != session) {
                SmartResourceDrops.LOGGER.warn("Entity death context mismatch; clearing scoped state safely");
                stack.clear();
            }
            if (stack.isEmpty()) {
                SESSIONS.remove();
            }
        }
    }

    public static final class ExperienceAwardScope implements AutoCloseable {
        private final EntityExperienceAwardToken token;
        private boolean closed;

        private ExperienceAwardScope(EntityExperienceAwardToken token) {
            this.token = token;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            Deque<EntityExperienceAwardToken> stack = MOB_EXPERIENCE_AWARDS.get();
            if (stack == null) {
                MOB_EXPERIENCE_AWARDS.remove();
                return;
            }
            EntityExperienceAwardToken removed = stack.poll();
            if (removed != token) {
                SmartResourceDrops.LOGGER.warn(
                        "Entity XP award token mismatch; clearing scoped state safely");
                stack.clear();
            }
            if (stack.isEmpty()) {
                MOB_EXPERIENCE_AWARDS.remove();
            }
        }
    }

    private static final class Session {
        private final LivingEntity entity;
        private final UUID entityUuid;
        private final String entityType;
        private final net.minecraft.resources.ResourceKey<Level> dimension;
        private final Vec3 deathPosition;
        private final DamageSource source;
        private final @Nullable UUID creditedPlayer;
        private final @Nullable UUID tamedOwner;
        private final boolean boss;
        private final EntityRuleTrace trace;
        private final EntityLootOutputController lootOutput = new EntityLootOutputController();
        private boolean experienceClaimed;
        private boolean experienceBudgetWarned;

        private Session(
                LivingEntity entity,
                ServerLevel level,
                DamageSource source,
                EntityRuleTrace trace
        ) {
            this.entity = entity;
            this.entityUuid = entity.getUUID();
            this.entityType = trace.entityId();
            this.dimension = level.dimension();
            this.deathPosition = entity.position();
            this.source = source;
            this.creditedPlayer = trace.attribution() == EntityKillAttribution.Kind.DIRECT_PLAYER
                    ? trace.attributedPlayerId()
                    : null;
            this.tamedOwner = trace.attribution() == EntityKillAttribution.Kind.TAMED_ENTITY
                    ? trace.attributedPlayerId()
                    : null;
            this.boss = trace.boss();
            this.trace = trace;
        }
    }
}
