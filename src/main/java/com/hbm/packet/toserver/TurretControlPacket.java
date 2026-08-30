package com.hbm.packet.toserver;

import com.hbm.blockentity.turret.TurretBaseBlockEntity;
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
 * C2S "apply this NBT to the turret at this position" control payload, ported from CE's
 * {@code NBTControlPacket} as used by {@code GUITurretMobFilter} (see
 * {@code docs/phase3/turret_system.md} decision 6 for why this needs its own payload rather than
 * the existing {@link com.hbm.packet.toserver.ItemControlPacket}: that packet dispatches to whatever
 * item the sending player is holding, but the mob-filter screen mutates a remote
 * {@link TurretBaseBlockEntity} at a fixed world position with no backing
 * {@code AbstractContainerMenu} at all - a genuinely different target shape).
 * <p>
 * Same size-bounded {@link ByteBufCodecs#COMPOUND_TAG} choice as {@code ItemControlPacket} for the
 * same reason: the sender is an untrusted client.
 */
public record TurretControlPacket(BlockPos pos, CompoundTag data) implements CustomPacketPayload {

    public static final Type<TurretControlPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "turret_control"));

    /**
     * Manual {@code encode}/{@code decode} over the plain {@link ByteBuf} supertype, matching
     * {@code ItemControlPacket}'s own confirmed shape - {@code BlockPos} has no plain-{@link ByteBuf}
     * helper (that is a {@code FriendlyByteBuf} extension {@link com.hbm.packet.toclient.BufPacket}'s
     * {@code RegistryFriendlyByteBuf}-typed codec uses instead), so the three coordinates are written
     * as plain ints here rather than widening this payload's buffer type unnecessarily.
     */
    public static final StreamCodec<ByteBuf, TurretControlPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public TurretControlPacket decode(ByteBuf buf) {
            BlockPos pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
            CompoundTag data = ByteBufCodecs.COMPOUND_TAG.decode(buf);
            return new TurretControlPacket(pos, data);
        }

        @Override
        public void encode(ByteBuf buf, TurretControlPacket packet) {
            buf.writeInt(packet.pos.getX());
            buf.writeInt(packet.pos.getY());
            buf.writeInt(packet.pos.getZ());
            ByteBufCodecs.COMPOUND_TAG.encode(buf, packet.data);
        }
    };

    public static void handleServer(TurretControlPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().level().getBlockEntity(packet.pos()) instanceof TurretBaseBlockEntity turret) {
                turret.receiveControl(packet.data());
            }
        });
    }

    @Override
    public Type<TurretControlPacket> type() {
        return TYPE;
    }
}
