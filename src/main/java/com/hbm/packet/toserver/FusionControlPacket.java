package com.hbm.packet.toserver;

import com.hbm.interfaces.IControlReceiver;
import com.hbm.main.MainRegistry;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** C2S {@code IControlReceiver} for torus recipe pick + klystron outputTarget. */
public record FusionControlPacket(BlockPos pos, CompoundTag data) implements CustomPacketPayload {

    public static final Type<FusionControlPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "fusion_control"));

    public static final StreamCodec<ByteBuf, FusionControlPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public FusionControlPacket decode(ByteBuf buf) {
            BlockPos pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
            return new FusionControlPacket(pos, ByteBufCodecs.COMPOUND_TAG.decode(buf));
        }

        @Override
        public void encode(ByteBuf buf, FusionControlPacket packet) {
            buf.writeInt(packet.pos.getX());
            buf.writeInt(packet.pos.getY());
            buf.writeInt(packet.pos.getZ());
            ByteBufCodecs.COMPOUND_TAG.encode(buf, packet.data);
        }
    };

    public static void handleServer(FusionControlPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().level().getBlockEntity(packet.pos()) instanceof IControlReceiver receiver
                    && receiver.hasPermission(context.player())) {
                receiver.receiveControl(packet.data());
            }
        });
    }

    @Override
    public Type<FusionControlPacket> type() {
        return TYPE;
    }
}
