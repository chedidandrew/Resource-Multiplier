package com.chedidandrew.smartresourcedrops.legacy.client;

import com.chedidandrew.smartresourcedrops.legacy.CommonProxy;
import com.chedidandrew.smartresourcedrops.legacy.LegacyNetwork;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ClientCommandHandler;

public final class ClientProxy extends CommonProxy {
    private static final AtomicReference<LegacyNetwork.ConfigMessage> LATEST_SERVER_MESSAGE =
            new AtomicReference<LegacyNetwork.ConfigMessage>();

    @Override
    public void preInit() {
        ClientCommandHandler.instance.registerCommand(new OpenGuiCommand());
        LegacyNetwork.initClientPackets(new ClientPacketHandler());
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (LegacyClientGuiQueue.consumeOpenRequest()) {
            LATEST_SERVER_MESSAGE.set(null);
            Minecraft minecraft = Minecraft.getMinecraft();
            minecraft.displayGuiScreen(LegacyConfigScreen.serverSettings(null));
            LegacyNetwork.CHANNEL.sendToServer(new LegacyNetwork.ConfigMessage(
                    LegacyNetwork.REQUEST, false, ""));
        }
        LegacyNetwork.ConfigMessage message = LATEST_SERVER_MESSAGE.getAndSet(null);
        if (message != null && Minecraft.getMinecraft().currentScreen instanceof LegacyConfigScreen) {
            ((LegacyConfigScreen) Minecraft.getMinecraft().currentScreen).acceptServer(message);
        }
    }

    private static final class ClientPacketHandler
            implements IMessageHandler<LegacyNetwork.ConfigMessage, IMessage> {
        @Override
        public IMessage onMessage(final LegacyNetwork.ConfigMessage message, MessageContext context) {
            if (message.action == LegacyNetwork.SNAPSHOT || message.action == LegacyNetwork.REJECTED) {
                // Coalesce snapshots so a remote endpoint cannot grow an
                // unbounded client-thread handoff queue.
                LATEST_SERVER_MESSAGE.set(message);
            }
            return null;
        }
    }
}
