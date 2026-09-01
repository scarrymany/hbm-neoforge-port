package com.hbm.packet.toserver;

import com.hbm.interfaces.IKeypadHandler;
import com.hbm.main.MainRegistry;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** CE {@code KeypadServerPacket} — type 0 = button click. */
public record KeypadServerPacket(BlockPos pos, int kind, int data) implements CustomPacketPayload {

    public static final Type<KeypadServerPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "keypad_server"));

    public static final StreamCodec<ByteBuf, KeypadServerPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public KeypadServerPacket decode(ByteBuf buf) {
            return new KeypadServerPacket(
                    new BlockPos(buf.readInt(), buf.readInt(), buf.readInt()),
                    buf.readInt(),
                    buf.readInt());
        }

        @Override
        public void encode(ByteBuf buf, KeypadServerPacket packet) {
            buf.writeInt(packet.pos.getX());
            buf.writeInt(packet.pos.getY());
            buf.writeInt(packet.pos.getZ());
            buf.writeInt(packet.kind);
            buf.writeInt(packet.data);
        }
    };

    public static void handleServer(KeypadServerPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Level level = context.player().level();
            if (!level.isLoaded(packet.pos())) return;
            BlockEntity te = level.getBlockEntity(packet.pos());
            if (te instanceof IKeypadHandler handler && packet.kind() == 0) {
                handler.getKeypad().buttonClicked(packet.data());
            }
        });
    }

    @Override
    public Type<KeypadServerPacket> type() {
        return TYPE;
    }
}
