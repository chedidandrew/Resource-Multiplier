package com.chedidandrew.smartresourcedrops.legacy;

import java.util.Collections;
import java.util.List;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

public final class SmartDropsCommand extends CommandBase {
    @Override
    public String getName() {
        return "smartdrops";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/smartdrops <status|reload|setglobal|entities|blockxp>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0 || "status".equalsIgnoreCase(args[0])) {
            LegacyConfig config = SmartResourceMultiplier.config();
            reply(sender, "Smart Resource Multiplier " + SmartResourceMultiplier.VERSION
                    + ": global " + config.globalMultiplier + "x, block XP "
                    + state(config.multiplyExperience) + ", entity drops " + state(config.entityDropsEnabled));
            return;
        }
        if ("reload".equalsIgnoreCase(args[0])) {
            SmartResourceMultiplier.reloadConfig();
            reply(sender, "Smart Resource Multiplier configuration reloaded.");
            return;
        }
        LegacyConfig edited = SmartResourceMultiplier.config().copy();
        if ("setglobal".equalsIgnoreCase(args[0]) && args.length == 2) {
            edited.globalMultiplier = parseInt(args[1], 0, edited.maximumMultiplier);
        } else if ("entities".equalsIgnoreCase(args[0]) && args.length == 2) {
            edited.entityDropsEnabled = parseToggle(args[1]);
        } else if ("blockxp".equalsIgnoreCase(args[0]) && args.length == 2) {
            edited.multiplyExperience = parseToggle(args[1]);
        } else {
            throw new CommandException(getUsage(sender));
        }
        if (!SmartResourceMultiplier.replaceConfig(edited)) {
            throw new CommandException("Could not save smart_resource_drops.json");
        }
        reply(sender, "Smart Resource Multiplier settings saved.");
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
            BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "status", "reload", "setglobal", "entities", "blockxp");
        }
        if (args.length == 2 && ("entities".equalsIgnoreCase(args[0]) || "blockxp".equalsIgnoreCase(args[0]))) {
            return getListOfStringsMatchingLastWord(args, "true", "false");
        }
        return Collections.emptyList();
    }

    private static boolean parseToggle(String value) throws CommandException {
        if ("true".equalsIgnoreCase(value) || "on".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value) || "off".equalsIgnoreCase(value)) return false;
        throw new CommandException("Expected true/on or false/off");
    }

    private static String state(boolean value) {
        return value ? "ON" : "OFF";
    }

    private static void reply(ICommandSender sender, String text) {
        sender.sendMessage(new TextComponentString(text));
    }
}
