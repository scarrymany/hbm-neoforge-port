package com.hbm.packet.toserver;

import com.hbm.items.IItemControlReceiver;
import com.hbm.main.MainRegistry;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Generic C2S "apply this NBT to whatever the player is holding" control payload, replacing CE's
 * {@code com.hbm.packet.toserver.NBTItemControlPacket} (see
 * {@code docs/phase3/scattered_military_items.md}'s "Key design/API decisions" section, read in
 * full). This is this port's first {@code toserver} payload - confirmed by directory listing that
 * {@code com.hbm.packet} had no {@code toserver} package at all before this class, and
 * {@code HbmNetwork}'s own javadoc previously noted zero concrete packets existed beyond the one
 * {@code toclient} {@link com.hbm.packet.toclient.BufPacket}.
 * <p>
 * Deliberately generic rather than one-off per feature: {@link com.hbm.items.IItemControlReceiver}
 * already exists in this port and is already implemented once
 * ({@code com.hbm.items.tool.ItemToolAbility}, for its ability-preset configuration), and several
 * later Phase 3 packages (the turret mob-filter GUI, the RTTY pager's channel-selection GUI, the
 * designator's manual-target GUI) all need the exact same "GUI screen writes a {@link CompoundTag},
 * send it to the server, dispatch to whichever held item knows what to do with it" round trip. This
 * is that one shared mechanism, built once rather than once per item - resolving the sender's held
 * item server-side (checking both hands) rather than encoding a slot/hand index that could desync
 * from what the server thinks is actually held.
 * <p>
 * Uses the size-bounded {@link ByteBufCodecs#COMPOUND_TAG} rather than the unlimited-size
 * {@code TRUSTED_COMPOUND_TAG} variant (appropriate only for tags a mod's own trusted server code
 * authored itself) because the sender here is an untrusted client - a malicious or broken client
 * sending an oversized tag should hit the codec's built-in size cap instead of being able to
 * allocate an unbounded amount of server memory per packet.
 */
public record ItemControlPacket(CompoundTag data) implements CustomPacketPayload {

    public static final Type<ItemControlPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "item_control"));

    public static final StreamCodec<ByteBuf, ItemControlPacket> STREAM_CODEC =
            ByteBufCodecs.COMPOUND_TAG.map(ItemControlPacket::new, ItemControlPacket::data);

    /**
     * Reads the sending player's held item in both hands and dispatches to whichever one
     * implements {@link IItemControlReceiver} - mirroring CE's own
     * {@code NBTItemControlPacket} handler, which likewise resolves the receiver from the player's
     * current inventory state server-side rather than trusting a stack reference off the wire.
     * Both hands are checked (not just main-hand) since a future receiver (e.g. an off-hand-carried
     * pager or detonator) should work identically to CE's own behavior, which never restricted
     * this to the main hand.
     */
    public static void handleServer(ItemControlPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();

            ItemStack mainHand = player.getMainHandItem();
            if (mainHand.getItem() instanceof IItemControlReceiver receiver) {
                receiver.receiveControl(mainHand, packet.data());
            }

            ItemStack offHand = player.getOffhandItem();
            if (offHand.getItem() instanceof IItemControlReceiver receiver) {
                receiver.receiveControl(offHand, packet.data());
            }
        });
    }

    @Override
    public Type<ItemControlPacket> type() {
        return TYPE;
    }
}
