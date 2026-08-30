package com.hbm.blockentity;

import net.minecraft.network.RegistryFriendlyByteBuf;

/**
 * Ported from CE's {@code com.hbm.tileentity.IBufPacketReceiver}. Implemented by
 * {@link LoadedBaseBlockEntity} so {@code com.hbm.packet.toclient.BufPacket} can hand a raw
 * {@link RegistryFriendlyByteBuf} payload back to whichever block entity sent it, without either
 * side needing to know the concrete block entity type - the packet only carries a {@code BlockPos}
 * and a byte payload, and {@link #deserialize(RegistryFriendlyByteBuf)} is resolved dynamically
 * against whatever block entity is found at that position when the packet is handled.
 *
 * <p>Cross-checked against Neo Edition's real (confirmed running) interface of the same name and
 * shape - {@code RegistryFriendlyByteBuf} replaces CE's plain {@code ByteBuf} because modern
 * network codecs need registry access to encode/decode registry-backed types (not used by the base
 * class's own {@code muffled}/{@code tilted} booleans, but required so subclasses that do
 * serialize {@code ItemStack}/{@code Holder}-bearing fields through this same channel can).
 */
public interface IBufPacketReceiver {

    void serialize(RegistryFriendlyByteBuf buf);

    void deserialize(RegistryFriendlyByteBuf buf);
}
