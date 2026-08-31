package com.hbm.packet.toserver;

import com.hbm.blockentity.machine.MachineCrucibleBlockEntity;
import com.hbm.main.MainRegistry;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S "apply this NBT to the Crucible at this position" control payload, ported from CE's
 * {@code NBTControlPacket} as used by {@code GUICrucible}'s recipe-picker click zone. Same shape/
 * rationale as {@link LaunchPadRustedControlPacket} (its own javadoc explains in full why a
 * position-targeted NBT control payload needs its own {@link CustomPacketPayload} rather than
 * reusing {@link ItemControlPacket}, which dispatches to the sending player's held item, not a
 * remote block entity): {@link MachineCrucibleBlockEntity#receiveControl} needs a fixed world
 * position, not "whatever the player is holding". A separate payload from
 * {@link LaunchPadRustedControlPacket}/{@link TurretControlPacket} (rather than generalizing one of
 * those to any {@code IControlReceiver}) to avoid modifying a file owned by a different,
 * already-committed pass.
 */
public record CrucibleControlPacket(BlockPos pos, CompoundTag data) implements CustomPacketPayload {

    public static final Type<CrucibleControlPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "crucible_control"));

    /** Manual {@code encode}/{@code decode} over the plain {@link ByteBuf} supertype, matching {@link LaunchPadRustedControlPacket}'s own confirmed shape. */
    public static final StreamCodec<ByteBuf, CrucibleControlPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public CrucibleControlPacket decode(ByteBuf buf) {
            BlockPos pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
            CompoundTag data = ByteBufCodecs.COMPOUND_TAG.decode(buf);
            return new CrucibleControlPacket(pos, data);
        }

        @Override
        public void encode(ByteBuf buf, CrucibleControlPacket packet) {
            buf.writeInt(packet.pos.getX());
            buf.writeInt(packet.pos.getY());
            buf.writeInt(packet.pos.getZ());
            ByteBufCodecs.COMPOUND_TAG.encode(buf, packet.data);
        }
    };

    public static void handleServer(CrucibleControlPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().level().getBlockEntity(packet.pos()) instanceof MachineCrucibleBlockEntity crucible) {
                crucible.receiveControl(packet.data());
            }
        });
    }

    @Override
    public Type<CrucibleControlPacket> type() {
        return TYPE;
    }
}
