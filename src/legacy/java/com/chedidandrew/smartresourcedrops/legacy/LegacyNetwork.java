package com.chedidandrew.smartresourcedrops.legacy;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import java.nio.charset.StandardCharsets;

/** Minimal server-authoritative configuration protocol for the legacy GUI. */
public final class LegacyNetwork {
    private static final int MAX_JSON_BYTES = 262144;
    public static final int REQUEST = 0;
    public static final int SNAPSHOT = 1;
    public static final int APPLY = 2;
    public static final int REJECTED = 3;
    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("smartdrops");

    private LegacyNetwork() {}

    public static void initServerPackets() {
        CHANNEL.registerMessage(ServerHandler.class, ConfigMessage.class, 0, Side.SERVER);
    }

    public static void initClientPackets(IMessageHandler<ConfigMessage, IMessage> handler) {
        CHANNEL.registerMessage(handler, ConfigMessage.class, 0, Side.CLIENT);
    }

    public static final class ConfigMessage implements IMessage {
        public int action;
        public boolean editable;
        public String json = "";

        public ConfigMessage() {}

        public ConfigMessage(int action, boolean editable, String json) {
            this.action = action;
            this.editable = editable;
            this.json = json == null ? "" : json;
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            action = buffer.readUnsignedByte();
            editable = buffer.readBoolean();
            int length = buffer.readInt();
            if (length < 0 || length > MAX_JSON_BYTES || length > buffer.readableBytes()) {
                throw new IllegalArgumentException("Invalid Smart Resource Multiplier config packet length");
            }
            byte[] bytes = new byte[length];
            buffer.readBytes(bytes);
            json = new String(bytes, StandardCharsets.UTF_8);
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeByte(action);
            buffer.writeBoolean(editable);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            if (bytes.length > MAX_JSON_BYTES) {
                throw new IllegalArgumentException("Smart Resource Multiplier config packet is too large");
            }
            buffer.writeInt(bytes.length);
            buffer.writeBytes(bytes);
        }
    }

    public static final class ServerHandler implements IMessageHandler<ConfigMessage, IMessage> {
        @Override
        public IMessage onMessage(final ConfigMessage message, final MessageContext context) {
            final EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    boolean editable = player.canUseCommand(2, "smartdrops");
                    if (message.action == APPLY && editable) {
                        try {
                            LegacyConfig replacement = LegacyConfig.fromJson(message.json);
                            if (!SmartResourceMultiplier.replaceConfig(replacement)) {
                                CHANNEL.sendTo(new ConfigMessage(REJECTED, true,
                                        SmartResourceMultiplier.config().toJson()), player);
                                return;
                            }
                        } catch (RuntimeException exception) {
                            CHANNEL.sendTo(new ConfigMessage(REJECTED, true,
                                    SmartResourceMultiplier.config().toJson()), player);
                            return;
                        }
                    }
                    CHANNEL.sendTo(new ConfigMessage(SNAPSHOT, editable,
                            SmartResourceMultiplier.config().toJson()), player);
                }
            });
            return null;
        }
    }
}
