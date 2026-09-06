package com.chedidandrew.smartresourcedrops.legacy.client;

import java.util.Collections;
import java.util.List;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

final class OpenGuiCommand extends CommandBase {
    @Override
    public String getName() {
        return "smartdropsgui";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/smartdropsgui";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        // GuiChat closes its current screen after client-command execution.
        // Defer opening until the next client tick so it remains visible.
        LegacyClientGuiQueue.requestOpen();
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("srm");
    }
}
