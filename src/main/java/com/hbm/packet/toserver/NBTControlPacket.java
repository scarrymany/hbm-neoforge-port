package com.hbm.packet.toserver;

import com.hbm.interfaces.IControlReceiver;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * NeoForge port of CE {@code NBTControlPacket}.
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/packet/toserver/NBTControlPacket.java
 * <p>
 * Sends BlockPos + NBT data to server, which calls {@link IControlReceiver#receiveControl(CompoundTag)}
 * on the BlockEntity at that position if it implements the interface and the player has permission.
 */
public record NBTControlPacket(BlockPos pos, CompoundTag nbt) implements CustomPacketPayload {

    public static final Type<NBTControlPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("hbm", "nbt_control"));

    public static final StreamCodec<ByteBuf, NBTControlPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public NBTControlPacket decode(ByteBuf buf) {
            BlockPos pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
            return new NBTControlPacket(pos, ByteBufCodecs.COMPOUND_TAG.decode(buf));
        }

        @Override
        public void encode(ByteBuf buf, NBTControlPacket packet) {
            buf.writeInt(packet.pos.getX());
            buf.writeInt(packet.pos.getY());
            buf.writeInt(packet.pos.getZ());
            ByteBufCodecs.COMPOUND_TAG.encode(buf, packet.nbt);
        }
    };

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Server-side handler for NBTControlPacket.
     * <p>
     * CE: {@code NBTControlPacket.Handler} (:50-78)
     */
    public static void handle(NBTControlPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                BlockEntity be = player.level().getBlockEntity(packet.pos);
                if (be instanceof IControlReceiver receiver) {
                    if (receiver.hasPermission(player)) {
                        // CE NBTControlPacket.Handler :65-66 — player overload then nbt
                        receiver.receiveControl(player, packet.nbt);
                        receiver.receiveControl(packet.nbt);
                    }
                }
            }
        });
    }
}
