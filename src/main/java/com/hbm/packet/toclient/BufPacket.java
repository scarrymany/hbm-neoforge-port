package com.hbm.packet.toclient;

import com.hbm.blockentity.IBufPacketReceiver;
import com.hbm.main.MainRegistry;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Generic ByteBuf-carrying sync payload, replacing CE's {@code com.hbm.packet.toclient.BufPacket}
 * (itself paired with CE's own {@code PacketThreading.createAllAroundThreadedPacket}). Carries a
 * raw byte payload plus the sender's {@link BlockPos}; on receipt the client resolves whichever
 * {@link BlockEntity} is actually loaded at that position and, if it implements
 * {@link IBufPacketReceiver}, hands the payload to {@link IBufPacketReceiver#deserialize}.
 * <p>
 * CE's own thread-hop ({@code PacketThreading}, an IO-thread pre-compilation cache for high-volume
 * machine packets) is not ported here - it has no NeoForge equivalent and no other packet in this
 * port needs it yet (see {@code com.hbm.packet.HbmNetwork}'s own javadoc: no concrete packets
 * existed before this one). NeoForge's own network stack already dispatches payload encode/handling
 * off the caller's thread internally, so nothing is lost in practice; if a future high-frequency
 * sender needs CE's dedicated pre-compile-and-cache behavior back, it can be reintroduced as its own
 * wrapper around this same payload without changing the payload shape.
 * <p>
 * Shape (record layout, {@code Type} id, {@code StreamCodec}, and the client-side resolution/error
 * handling) is a direct, confirmed cross-check against Neo Edition's real, running
 * {@code com.hbm.network.toclient.BufPacket} - this is pure NeoForge networking plumbing, not CE
 * gameplay behavior, so Neo Edition is authoritative here rather than merely shape-checked.
 */
public record BufPacket(BlockPos pos, byte[] data) implements CustomPacketPayload {

    public static final Type<BufPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "buf_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BufPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public BufPacket decode(RegistryFriendlyByteBuf buf) {
            BlockPos pos = buf.readBlockPos();
            byte[] data = buf.readByteArray();
            return new BufPacket(pos, data);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, BufPacket packet) {
            buf.writeBlockPos(packet.pos);
            buf.writeByteArray(packet.data);
        }
    };

    public static void handleCommon(BufPacket packet, IPayloadContext context) {
        handleClient(packet, context);
    }

    @OnlyIn(Dist.CLIENT)
    public static void handleClient(BufPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Level level = Minecraft.getInstance().level;
            if (level == null) return;

            BlockEntity be = level.getBlockEntity(packet.pos);
            if (!(be instanceof IBufPacketReceiver receiver)) return;

            RegistryFriendlyByteBuf buf =
                    new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(packet.data), level.registryAccess(), ConnectionType.OTHER);
            try {
                receiver.deserialize(buf);
            } catch (Exception e) {
                MainRegistry.logger.warn("A ByteBuf sync packet failed to deserialize (buffer underflow - more data was" +
                        " read than the packet actually contained). Block: {}", be.getBlockState().getBlock());
                MainRegistry.logger.warn(e.getMessage(), e);
            } finally {
                buf.release();
            }
        });
    }

    @Override
    public Type<BufPacket> type() {
        return TYPE;
    }
}
