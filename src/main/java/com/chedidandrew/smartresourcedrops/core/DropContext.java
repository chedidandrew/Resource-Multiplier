package com.chedidandrew.smartresourcedrops.core;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.config.ConfigManager;
import com.chedidandrew.smartresourcedrops.config.SmartDropsConfig;
import com.chedidandrew.smartresourcedrops.core.util.BlockLootOutputBudget;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public final class DropContext {
    private static final ThreadLocal<Deque<Session>> SESSIONS = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Deque<AutomationFrame>> AUTOMATION_FRAMES = ThreadLocal.withInitial(ArrayDeque::new);

    private DropContext() {
    }

    public static void beginPlayer(
            ServerLevel level,
            Player player,
            BlockPos pos,
            BlockState state,
            BlockEntity blockEntity
    ) {
        push(new Session(
                DropSource.PLAYER,
                level,
                pos,
                state,
                MultiplierResolver.resolve(level, pos, state, blockEntity, DropSource.PLAYER, player)));
    }

    public static void beginExplosion(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            BlockEntity blockEntity,
            Entity actor
    ) {
        push(new Session(
                DropSource.EXPLOSION,
                level,
                pos,
                state,
                MultiplierResolver.resolve(level, pos, state, blockEntity, DropSource.EXPLOSION, actor)));
    }

    public static void beginAutomation(
            LevelAccessor level,
            BlockPos pos,
            BlockState state,
            BlockEntity blockEntity,
            Entity actor
    ) {
        AutomationFrame frame = AutomationFrame.NOT_OWNER;
        if (level instanceof ServerLevel serverLevel) {
            final Session active = SESSIONS.get().peek();
            if (active == null || !active.target.matches(serverLevel, pos.asLong(), state)) {
                push(new Session(
                        DropSource.AUTOMATION,
                        serverLevel,
                        pos,
                        state,
                        MultiplierResolver.resolve(serverLevel, pos, state, blockEntity, DropSource.AUTOMATION, actor)));
                frame = AutomationFrame.OWNER;
            }
        }
        AUTOMATION_FRAMES.get().push(frame);
    }

    public static void endAutomation() {
        Deque<AutomationFrame> frames = AUTOMATION_FRAMES.get();
        if (frames.isEmpty()) {
            return;
        }
        AutomationFrame frame = frames.pop();
        if (frame.owner) {
            endExpected(DropSource.AUTOMATION);
        }
        if (frames.isEmpty()) {
            AUTOMATION_FRAMES.remove();
        }
    }

    public static List<ItemStack> applyDrops(
            final List<ItemStack> original,
            final LootParams.Builder params,
            final BlockState state
    ) {
        final Session session = SESSIONS.get().peek();
        final Vec3 origin = params.getOptionalParameter(LootContextParams.ORIGIN);
        if (session == null
                || origin == null
                || !session.target.claim(params.getLevel(), BlockPos.containing(origin).asLong(), state)) {
            return original;
        }

        final RuleEngine.Decision decision = session.decision;
        if (!decision.eligible()) {
            return original;
        }

        final int multiplier = decision.multiplier();
        final BlockLootOutputBudget.Result result = BlockLootOutputBudget.multiply(original, multiplier);
        if (result.fellBackToVanilla()) {
            SmartDropsStats.recordBlockBudgetFallback(result.originalItemCount());
            BlockLootBudgetWarnings.warn(
                    params.getLevel(),
                    BlockPos.containing(origin),
                    state,
                    multiplier,
                    result);
        } else {
            SmartDropsStats.recordDrops(multiplier, result.originalItemCount());
        }
        return result.output();
    }

    public static int multiplyExperience(final int amount, final ServerLevel level, final Vec3 pos) {
        final Session session = SESSIONS.get().peek();
        final SmartDropsConfig config = ConfigManager.get();
        if (session == null
                || !session.target.matchesPosition(level, BlockPos.containing(pos).asLong())
                || !config.multiplyExperience
                || amount <= 0
                || !session.decision.eligible()) {
            return amount;
        }

        final int multiplier = SmartDropsConfig.clamp(
                config.experienceMultiplier,
                1,
                config.maximumMultiplier);
        final long result = (long) amount * multiplier;
        final int multiplied = result > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
        SmartDropsStats.recordExperience(amount, multiplied);
        return multiplied;
    }

    public static void endExpected(DropSource expected) {
        final Deque<Session> stack = SESSIONS.get();
        if (stack.isEmpty()) {
            return;
        }
        final Session session = stack.pop();
        if (session.source != expected) {
            SmartResourceDrops.LOGGER.warn(
                    "Drop context mismatch. Expected {}, found {}. Context was cleared safely.",
                    expected,
                    session.source);
            stack.clear();
        }
        if (stack.isEmpty()) {
            SESSIONS.remove();
        }
    }

    public static DropSource activeSource() {
        final Session session = SESSIONS.get().peek();
        return session == null ? null : session.source;
    }

    private static void push(final Session session) {
        SESSIONS.get().push(session);
    }

    private record AutomationFrame(boolean owner) {
        private static final AutomationFrame OWNER = new AutomationFrame(true);
        private static final AutomationFrame NOT_OWNER = new AutomationFrame(false);
    }

    private static final class Session {
        private final DropSource source;
        private final RuleEngine.Decision decision;
        private final DropTargetGuard target;

        private Session(
                final DropSource source,
                final ServerLevel level,
                final BlockPos pos,
                final BlockState state,
                final RuleEngine.Decision decision
        ) {
            this.source = source;
            this.decision = decision;
            this.target = new DropTargetGuard(level, pos.asLong(), state);
        }
    }
}
