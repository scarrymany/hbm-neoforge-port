package com.hbm.packet.toserver;

import com.hbm.blockentity.machine.dummyable.MassStorageBlockEntity;
import com.hbm.main.MainRegistry;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** C2S provide/toggle for CE {@code GUIMassStorage} (same shape as {@link CrucibleControlPacket}). */
public record MassStorageControlPacket(BlockPos pos, CompoundTag data) implements CustomPacketPayload {

    public static final Type<MassStorageControlPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "mass_storage_control"));

    public static final StreamCodec<ByteBuf, MassStorageControlPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public MassStorageControlPacket decode(ByteBuf buf) {
            BlockPos pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
            CompoundTag data = ByteBufCodecs.COMPOUND_TAG.decode(buf);
            return new MassStorageControlPacket(pos, data);
        }

        @Override
        public void encode(ByteBuf buf, MassStorageControlPacket packet) {
            buf.writeInt(packet.pos.getX());
            buf.writeInt(packet.pos.getY());
            buf.writeInt(packet.pos.getZ());
            ByteBufCodecs.COMPOUND_TAG.encode(buf, packet.data);
        }
    };

    public static void handleServer(MassStorageControlPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().level().getBlockEntity(packet.pos()) instanceof MassStorageBlockEntity storage) {
                storage.receiveControl(packet.data());
            }
        });
    }

    @Override
    public Type<MassStorageControlPacket> type() {
        return TYPE;
    }
}
