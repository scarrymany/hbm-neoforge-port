package com.hbm.packet.toserver;

import com.hbm.handler.HbmKeybinds;
import com.hbm.items.IKeybindReceiver;
import com.hbm.main.MainRegistry;
import com.hbm.util.EnumUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S "this keybind's press/release state just changed" sync payload - the wire format
 * {@link com.hbm.handler.HbmKeybindInputEvents} (client-side keybind-state diffing, already
 * committed) sends on every {@code EnumKeybind} edge, and the counterpart that dispatches to
 * {@link IKeybindReceiver#handleKeybind} server-side.
 * <p>
 * <b>Pre-existing gap this class fixes:</b> {@code HbmKeybindInputEvents}/{@code HbmKeybinds} already
 * imported and constructed {@code com.hbm.packet.KeybindPacket} (see both classes' own source), but
 * no such class existed anywhere in this tree and it was never registered in {@link
 * com.hbm.packet.HbmNetwork} - meaning every {@link IKeybindReceiver} implementor (CE:
 * {@code ItemToolAbility}, and now every {@code ItemGunBaseNT} gun this package adds) could never
 * actually receive a keybind press/release server-side; the client-side half of the pipeline sent a
 * packet type that would have failed to encode/route. Fixed here rather than stubbed because the
 * shape is small, self-contained, and exactly mirrors {@link ItemControlPacket}'s already-committed
 * C2S pattern (this port's other C2S payload) - not a new mechanism, just the missing half of an
 * existing one. {@code HbmKeybindInputEvents}'s import is corrected to this class's real package
 * (was {@code com.hbm.packet.KeybindPacket}, now {@code com.hbm.packet.toserver.KeybindPacket}) as
 * part of the same fix; the required {@code HbmNetwork} registration line is reported as this task's
 * wiring snippet for that shared file.
 */
public record KeybindPacket(HbmKeybinds.EnumKeybind keybind, boolean state) implements CustomPacketPayload {

    public static final Type<KeybindPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "keybind"));

    public static final StreamCodec<RegistryFriendlyByteBuf, KeybindPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(
                    ordinal -> EnumUtil.grabEnumSafely(HbmKeybinds.EnumKeybind.VALUES, ordinal),
                    HbmKeybinds.EnumKeybind::ordinal),
            KeybindPacket::keybind,
            ByteBufCodecs.BOOL, KeybindPacket::state,
            KeybindPacket::new
    );

    /**
     * Resolves the sender's held item in both hands (matching {@link ItemControlPacket}'s own
     * both-hands scan, rather than trusting a hand/slot index off the wire) and dispatches to
     * {@link IKeybindReceiver#handleKeybind} when it implements that interface and {@link
     * IKeybindReceiver#canHandleKeybind} agrees this keybind is relevant to it - mirroring CE's own
     * server-side keybind-packet handler, which performs the same held-item resolution and
     * {@code canHandleKeybind} gate before dispatching.
     */
    public static void handleServer(KeybindPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();

            ItemStack mainHand = player.getMainHandItem();
            if (mainHand.getItem() instanceof IKeybindReceiver receiver
                    && receiver.canHandleKeybind(player, mainHand, packet.keybind())) {
                receiver.handleKeybind(player, mainHand, packet.keybind(), packet.state());
            }

            ItemStack offHand = player.getOffhandItem();
            if (offHand.getItem() instanceof IKeybindReceiver receiver
                    && receiver.canHandleKeybind(player, offHand, packet.keybind())) {
                receiver.handleKeybind(player, offHand, packet.keybind(), packet.state());
            }
        });
    }

    @Override
    public Type<KeybindPacket> type() {
        return TYPE;
    }
}
