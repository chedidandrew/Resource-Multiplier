package com.chedidandrew.smartresourcedrops.legacy.client;

import com.chedidandrew.smartresourcedrops.legacy.CommonProxy;
import com.chedidandrew.smartresourcedrops.legacy.LegacyNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class ClientProxy extends CommonProxy {
    @Override
    public void preInit() {
        ClientCommandHandler.instance.registerCommand(new OpenGuiCommand());
        LegacyNetwork.initClientPackets(new ClientPacketHandler());
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !LegacyClientGuiQueue.consumeOpenRequest()) return;

        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.displayGuiScreen(new LegacyConfigScreen(null));
        LegacyNetwork.CHANNEL.sendToServer(new LegacyNetwork.ConfigMessage(
                LegacyNetwork.REQUEST, false, ""));
    }

    private static final class ClientPacketHandler
            implements IMessageHandler<LegacyNetwork.ConfigMessage, IMessage> {
        @Override
        public IMessage onMessage(final LegacyNetwork.ConfigMessage message, MessageContext context) {
            Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    if (Minecraft.getMinecraft().currentScreen instanceof LegacyConfigScreen) {
                        ((LegacyConfigScreen) Minecraft.getMinecraft().currentScreen).acceptServer(message);
                    }
                }
            });
            return null;
        }
    }
}
