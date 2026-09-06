package com.chedidandrew.smartresourcedrops.legacy.client;

import java.util.Collections;
import java.util.List;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

final class OpenGuiCommand extends CommandBase {
    @Override
    public String getCommandName() { return "smartdropsgui"; }

    @Override
    public String getCommandUsage(ICommandSender sender) { return "/smartdropsgui"; }

    @Override
    public int getRequiredPermissionLevel() { return 0; }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) { return true; }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        // GuiChat unconditionally closes itself after executing a command in
        // 1.7.10, so opening here would be immediately undone. Defer one tick.
        LegacyClientGuiQueue.requestOpen();
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List getCommandAliases() { return Collections.singletonList("srm"); }
}
