package com.hbm.packet.toclient;

import com.hbm.client.ClientPackets;
import com.hbm.main.MainRegistry;
import com.hbm.saveddata.satellites.Satellite;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * S2C live satellite-panel stream, ported from CE's {@code com.hbm.packet.toclient.SatPanelPacket}
 * - the one genuinely new payload {@code docs/phase3/missile_launch_infra.md}'s scope needs (per
 * its own Key design/API decisions section). {@link com.hbm.items.tool.ItemSatInterface} sends one
 * of these to its holder every 2 ticks while it's the active hotbar item.
 * <p>
 * CE's version serializes the whole {@code Satellite} object via a hand-rolled {@code ByteBuf}
 * {@code serialize()}. That does not map onto a 1.21 {@link StreamCodec} one-to-one (an open-ended
 * polymorphic object isn't codec-friendly) - this record instead carries a deliberate, fixed field
 * list covering what the panel screen actually renders: frequency, satellite type name, RGB color,
 * the {@code satIface}/{@code ifaceAcs}/{@code coordAcs} flags that drive which controls the panel
 * shows, the current target coordinate, the free-text {@code tx} scratch field commands write into,
 * and the info lines ({@link Satellite#getInfo}, flattened to plain strings - formatting is lost,
 * acceptable for a status readout).
 */
public record SatPanelPayload(int freq, String satType, float colorR, float colorG, float colorB,
                               int satIface, int ifaceActionsMask, int coordActionsMask,
                               int targetX, int targetZ, String tx, List<String> infoLines) implements CustomPacketPayload {

    public static final Type<SatPanelPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "sat_panel"));

    /**
     * Hand-rolled rather than {@link StreamCodec#composite}: this record has 12 fields, more than
     * the small fixed arity {@code composite}'s overload set supports (confirmed by checking this
     * port's own {@code BufPacket}, which uses this exact same manual-anonymous-class shape for the
     * identical reason - not a new pattern invented here).
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, SatPanelPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SatPanelPayload decode(RegistryFriendlyByteBuf buf) {
            int freq = ByteBufCodecs.VAR_INT.decode(buf);
            String satType = ByteBufCodecs.STRING_UTF8.decode(buf);
            float colorR = buf.readFloat();
            float colorG = buf.readFloat();
            float colorB = buf.readFloat();
            int satIface = ByteBufCodecs.VAR_INT.decode(buf);
            int ifaceActionsMask = ByteBufCodecs.VAR_INT.decode(buf);
            int coordActionsMask = ByteBufCodecs.VAR_INT.decode(buf);
            int targetX = ByteBufCodecs.VAR_INT.decode(buf);
            int targetZ = ByteBufCodecs.VAR_INT.decode(buf);
            String tx = ByteBufCodecs.STRING_UTF8.decode(buf);
            List<String> infoLines = ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).decode(buf);
            return new SatPanelPayload(freq, satType, colorR, colorG, colorB, satIface, ifaceActionsMask,
                    coordActionsMask, targetX, targetZ, tx, infoLines);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, SatPanelPayload packet) {
            ByteBufCodecs.VAR_INT.encode(buf, packet.freq);
            ByteBufCodecs.STRING_UTF8.encode(buf, packet.satType);
            buf.writeFloat(packet.colorR);
            buf.writeFloat(packet.colorG);
            buf.writeFloat(packet.colorB);
            ByteBufCodecs.VAR_INT.encode(buf, packet.satIface);
            ByteBufCodecs.VAR_INT.encode(buf, packet.ifaceActionsMask);
            ByteBufCodecs.VAR_INT.encode(buf, packet.coordActionsMask);
            ByteBufCodecs.VAR_INT.encode(buf, packet.targetX);
            ByteBufCodecs.VAR_INT.encode(buf, packet.targetZ);
            ByteBufCodecs.STRING_UTF8.encode(buf, packet.tx);
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).encode(buf, packet.infoLines);
        }
    };

    /** Bit-packs a {@link Satellite#ifaceAcs}/{@link Satellite#coordAcs}-style small fixed enum list. */
    public static int maskOf(List<? extends Enum<?>> flags) {
        int mask = 0;
        for (Enum<?> flag : flags) mask |= (1 << flag.ordinal());
        return mask;
    }

    public static boolean hasFlag(int mask, int ordinal) {
        return (mask & (1 << ordinal)) != 0;
    }

    /** Built server-side by {@link com.hbm.items.tool.ItemSatInterface}'s per-2-tick sender, which has the {@link Level} {@link Satellite#getInfo} needs. */
    public static SatPanelPayload of(int freq, Satellite sat, Level level) {
        float[] color = sat.getColor();
        List<String> info = new ArrayList<>();
        for (Component line : sat.getInfo(level)) info.add(line.getString());

        return new SatPanelPayload(freq, sat.getType(), color[0], color[1], color[2], sat.satIface.ordinal(),
                maskOf(sat.ifaceAcs), maskOf(sat.coordAcs), sat.targetX, sat.targetZ, sat.tx, info);
    }

    public static void handleClient(SatPanelPayload packet, IPayloadContext context) {
        ClientPackets.satPanel(packet, context);
    }

    @Override
    public Type<SatPanelPayload> type() {
        return TYPE;
    }
}
