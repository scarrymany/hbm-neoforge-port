package com.hbm.packet.toserver;

import com.hbm.blockentity.machine.chem.ElectrolyserBlockEntity;
import com.hbm.main.MainRegistry;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** C2S dual-GUI toggle for CE {@code GUIElectrolyserMetal}/{@code GUIElectrolyserFluid} ({@code sgf}/{@code sgm}). */
public record ElectrolyserControlPacket(BlockPos pos, CompoundTag data) implements CustomPacketPayload {

    public static final Type<ElectrolyserControlPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "electrolyser_control"));

    public static final StreamCodec<ByteBuf, ElectrolyserControlPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ElectrolyserControlPacket decode(ByteBuf buf) {
            BlockPos pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
            CompoundTag data = ByteBufCodecs.COMPOUND_TAG.decode(buf);
            return new ElectrolyserControlPacket(pos, data);
        }

        @Override
        public void encode(ByteBuf buf, ElectrolyserControlPacket packet) {
            buf.writeInt(packet.pos.getX());
            buf.writeInt(packet.pos.getY());
            buf.writeInt(packet.pos.getZ());
            ByteBufCodecs.COMPOUND_TAG.encode(buf, packet.data);
        }
    };

    public static void handleServer(ElectrolyserControlPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player
                    && player.level().getBlockEntity(packet.pos()) instanceof ElectrolyserBlockEntity electrolyser) {
                electrolyser.receiveControl(player, packet.data());
            }
        });
    }

    @Override
    public Type<ElectrolyserControlPacket> type() {
        return TYPE;
    }
}
