package com.chedidandrew.smartresourcedrops.core.client.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.chedidandrew.smartresourcedrops.SmartResourceDrops;
import com.chedidandrew.smartresourcedrops.core.util.QueuedWorkDrain;
import com.chedidandrew.smartresourcedrops.core.util.QueuedWorkDrain.TimedValue;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

/** Bounded render-thread queue that rejects whole batches instead of silently evicting edits. */
public final class ClientCommandQueue {
    private static final long QUIET_PERIOD_NANOS = 200_000_000L;
    private static final int MAX_PENDING = 128;
    private static final Map<String, TimedValue<String>> COMMANDS = new LinkedHashMap<>();
    private static final Map<String, TimedValue<Runnable>> ACTIONS = new LinkedHashMap<>();
    private ClientCommandQueue() {
    }

    public static void initialize() {
        // The active loader invokes tick(Minecraft) from its client post-tick event.
    }

    public static void tick(final Minecraft client) {
        drain(client, false);
    }

    public static boolean send(final String command) {
        if (command == null || command.isBlank()) {
            return false;
        }
        return trySendBatch(List.of(command));
    }

    public static boolean trySendBatch(final Collection<String> commands) {
        if (commands == null || commands.isEmpty()) {
            return true;
        }
        initialize();
        final long now = System.nanoTime();
        synchronized (ClientCommandQueue.class) {
            final LinkedHashMap<String, TimedValue<String>> candidate = new LinkedHashMap<>(COMMANDS);
            for (String command : commands) {
                if (command == null || command.isBlank()) {
                    continue;
                }
                final String normalized = trimSlash(command.trim());
                candidate.put(coalescingKey(normalized), new TimedValue<>(normalized, now));
            }
            if (candidate.size() > MAX_PENDING) {
                return false;
            }
            COMMANDS.clear();
            COMMANDS.putAll(candidate);
            return true;
        }
    }

    public static boolean runCoalesced(final String key, final Runnable action) {
        if (key == null || key.isBlank() || action == null) {
            return false;
        }
        initialize();
        synchronized (ClientCommandQueue.class) {
            if (!ACTIONS.containsKey(key) && ACTIONS.size() >= MAX_PENDING) {
                return false;
            }
            ACTIONS.put(key, new TimedValue<>(action, System.nanoTime()));
            return true;
        }
    }

    public static synchronized void cancelCoalesced(final String key) {
        if (key != null) {
            ACTIONS.remove(key);
        }
    }

    public static void flush() {
        drain(Minecraft.getInstance(), true);
    }

    public static synchronized void clear() {
        COMMANDS.clear();
        ACTIONS.clear();
    }

    private static void drain(final Minecraft client, final boolean force) {
        final ClientPacketListener connection = client.getConnection();
        if (connection == null) {
            clear();
            return;
        }

        final long now = System.nanoTime();
        final List<String> dueCommands;
        final List<Runnable> dueActions;
        synchronized (ClientCommandQueue.class) {
            dueCommands = QueuedWorkDrain.removeDue(COMMANDS, now, QUIET_PERIOD_NANOS, force);
            dueActions = QueuedWorkDrain.removeDue(ACTIONS, now, QUIET_PERIOD_NANOS, force);
        }
        for (String command : dueCommands) {
            try {
                connection.sendCommand(command);
            } catch (RuntimeException exception) {
                SmartResourceDrops.LOGGER.warn("Could not send queued client command", exception);
            }
        }
        for (Runnable action : dueActions) {
            try {
                action.run();
            } catch (RuntimeException exception) {
                SmartResourceDrops.LOGGER.warn("Queued client action failed", exception);
            }
        }
    }

    private static String trimSlash(final String command) {
        return command.startsWith("/") ? command.substring(1) : command;
    }

    private static String coalescingKey(final String command) {
        final String[] parts = command.toLowerCase(Locale.ROOT).split("\\s+");
        if (parts.length < 3) {
            return command;
        }
        final String joined = String.join(" ", parts);
        final String last = parts[parts.length - 1];
        final boolean valueToken = last.matches("-?\\d+(?:\\.\\d+)?")
            || last.equals("true") || last.equals("false")
            || last.equals("on") || last.equals("off")
            || last.equals("natural_only") || last.equals("all")
            || last.equals("player_placed_only")
            || last.equals("blacklist") || last.equals("whitelist")
            || last.equals("inherit");
        final boolean setter = valueToken || joined.contains(" set ")
            || joined.contains(" multiplier ")
            || joined.contains(" protection ")
            || joined.contains(" mode ")
            || joined.contains(" enabled ");
        return setter ? String.join(" ", Arrays.copyOf(parts, parts.length - 1)) : command;
    }
}
