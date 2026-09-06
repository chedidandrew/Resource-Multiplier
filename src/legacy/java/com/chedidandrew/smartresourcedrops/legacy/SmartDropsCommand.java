package com.chedidandrew.smartresourcedrops.legacy;

import java.util.Collections;
import java.util.List;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

public final class SmartDropsCommand extends CommandBase {
    @Override
    public String getCommandName() { return "smartdrops"; }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/smartdrops <status|reload|setglobal|entities|blockxp>";
    }

    @Override
    public int getRequiredPermissionLevel() { return 0; }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0 || "status".equalsIgnoreCase(args[0])) {
            LegacyConfig config = SmartResourceMultiplier.config();
            reply(sender, "Smart Resource Multiplier " + SmartResourceMultiplier.VERSION
                    + ": global " + config.globalMultiplier + "x, block XP "
                    + state(config.multiplyExperience) + ", entity drops " + state(config.entityDropsEnabled));
            return;
        }
        if (!sender.canCommandSenderUseCommand(2, getCommandName())) {
            throw new CommandException("You do not have permission to change server settings.");
        }
        if ("reload".equalsIgnoreCase(args[0])) {
            SmartResourceMultiplier.reloadConfig();
            reply(sender, "Smart Resource Multiplier configuration reloaded.");
            return;
        }
        LegacyConfig edited = SmartResourceMultiplier.config().copy();
        if ("setglobal".equalsIgnoreCase(args[0]) && args.length == 2) {
            edited.globalMultiplier = parseIntBounded(sender, args[1], 0, edited.maximumMultiplier);
        } else if ("entities".equalsIgnoreCase(args[0]) && args.length == 2) {
            edited.entityDropsEnabled = parseToggle(args[1]);
        } else if ("blockxp".equalsIgnoreCase(args[0]) && args.length == 2) {
            edited.multiplyExperience = parseToggle(args[1]);
        } else {
            throw new CommandException(getCommandUsage(sender));
        }
        if (!SmartResourceMultiplier.replaceConfig(edited)) {
            throw new CommandException("Could not save smart_resource_drops.json");
        }
        reply(sender, "Smart Resource Multiplier settings saved.");
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "status", "reload", "setglobal", "entities", "blockxp");
        }
        if (args.length == 2 && ("entities".equalsIgnoreCase(args[0]) || "blockxp".equalsIgnoreCase(args[0]))) {
            return getListOfStringsMatchingLastWord(args, "true", "false");
        }
        return Collections.emptyList();
    }

    private static boolean parseToggle(String value) {
        if ("true".equalsIgnoreCase(value) || "on".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value) || "off".equalsIgnoreCase(value)) return false;
        throw new CommandException("Expected true/on or false/off");
    }

    private static String state(boolean value) { return value ? "ON" : "OFF"; }
    private static void reply(ICommandSender sender, String text) { sender.addChatMessage(new ChatComponentText(text)); }
}
