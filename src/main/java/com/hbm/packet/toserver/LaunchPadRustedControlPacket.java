package com.hbm.packet.toserver;

import com.hbm.blockentity.bomb.LaunchPadRustedBlockEntity;
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
 * C2S "apply this NBT to the rusted launch pad at this position" control payload, ported from CE's
 * {@code NBTControlPacket} as used by {@code GUILaunchPadRusted}'s "Release Missile" button (see
 * {@code docs/phase5/gui_screens_survey_weapons_storage_special.md} Headline finding 4 - the "safe
 * to build now" item 5). {@link LaunchPadRustedBlockEntity} already implements
 * {@code IControlReceiver} and its {@code receiveControl}'s {@code "release"} key already exists
 * (used server-side, e.g. by world-gen silo loot delivery), but nothing client-side could reach it
 * before this GUI's Release button existed.
 * <p>
 * Same shape/rationale as {@link TurretControlPacket} (its own javadoc explains in full why a
 * position-targeted NBT control payload needs its own {@link CustomPacketPayload} rather than
 * reusing {@link ItemControlPacket}, which dispatches to the sending player's held item, not a
 * remote block entity): {@link LaunchPadRustedBlockEntity#receiveControl} needs a fixed world
 * position, not "whatever the player is holding". This is a separate payload from
 * {@code TurretControlPacket} (rather than generalizing that one to any {@code IControlReceiver})
 * to avoid modifying a file owned by a different, already-committed Phase 3 turret-system pass.
 */
public record LaunchPadRustedControlPacket(BlockPos pos, CompoundTag data) implements CustomPacketPayload {

    public static final Type<LaunchPadRustedControlPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "launch_pad_rusted_control"));

    /** Manual {@code encode}/{@code decode} over the plain {@link ByteBuf} supertype, matching {@code TurretControlPacket}'s own confirmed shape. */
    public static final StreamCodec<ByteBuf, LaunchPadRustedControlPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public LaunchPadRustedControlPacket decode(ByteBuf buf) {
            BlockPos pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
            CompoundTag data = ByteBufCodecs.COMPOUND_TAG.decode(buf);
            return new LaunchPadRustedControlPacket(pos, data);
        }

        @Override
        public void encode(ByteBuf buf, LaunchPadRustedControlPacket packet) {
            buf.writeInt(packet.pos.getX());
            buf.writeInt(packet.pos.getY());
            buf.writeInt(packet.pos.getZ());
            ByteBufCodecs.COMPOUND_TAG.encode(buf, packet.data);
        }
    };

    public static void handleServer(LaunchPadRustedControlPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().level().getBlockEntity(packet.pos()) instanceof LaunchPadRustedBlockEntity pad) {
                pad.receiveControl(packet.data());
            }
        });
    }

    @Override
    public Type<LaunchPadRustedControlPacket> type() {
        return TYPE;
    }
}
