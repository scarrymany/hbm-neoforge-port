package com.hbm.packet.toserver;

import com.hbm.main.MainRegistry;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteSavedData;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S satellite-panel control payload, sent by {@link com.hbm.inventory.gui.SatInterfaceScreen}/
 * {@link com.hbm.inventory.gui.SatCoordScreen} (both containerless {@code Screen}s, see
 * {@code docs/phase3/missile_launch_infra.md}'s Key design/API decisions for why those need a
 * direct {@code Minecraft#setScreen} rather than a {@code Menu}). Addressed by satellite frequency
 * (looked up server-side, matching {@link com.hbm.items.tool.ItemSatDesignator}'s own
 * frequency-based lookup) rather than by held-item-slot, since the dispatch targets ({@code
 * Satellite#onClick}/{@code onCoordAction}/{@code onCommand}) all need the sending
 * {@link ServerPlayer} directly - the existing generic {@link ItemControlPacket}/
 * {@code IItemControlReceiver} mechanism does not thread a player through to its receiver, so this
 * package needs its own small payload rather than reusing that one (unlike
 * {@link com.hbm.items.tool.ItemDesignatorManual}, whose "Save" action needs no player context and
 * fits {@code ItemControlPacket} directly).
 */
public record SatPanelActionPayload(int freq, CompoundTag data) implements CustomPacketPayload {

    public static final Type<SatPanelActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "sat_panel_action"));

    public static final StreamCodec<ByteBuf, SatPanelActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SatPanelActionPayload::freq,
            ByteBufCodecs.COMPOUND_TAG, SatPanelActionPayload::data,
            SatPanelActionPayload::new
    );

    public static void handleServer(SatPanelActionPayload packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!(player.level() instanceof ServerLevel level)) return;

            Satellite sat = SatelliteSavedData.getData(level).getSatFromFreq(packet.freq());
            if (sat == null) return;

            CompoundTag data = packet.data();
            if (data.contains("satClickX") && data.contains("satClickZ")) {
                sat.onClick(level, player, data.getInt("satClickX"), data.getInt("satClickZ"));
            } else if (data.contains("satCoordX") && data.contains("satCoordZ")) {
                int y = data.contains("satCoordY") ? data.getInt("satCoordY") : -1;
                sat.onCoordAction(level, player, data.getInt("satCoordX"), y, data.getInt("satCoordZ"));
            } else if (data.contains("satCommand")) {
                String raw = data.getString("satCommand");
                sat.onCommand(level, raw.isEmpty() ? new String[0] : raw.split(" "));
            }
            SatelliteSavedData.getData(level).setDirty();
        });
    }

    @Override
    public Type<SatPanelActionPayload> type() {
        return TYPE;
    }
}
