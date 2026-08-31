package com.hbm.packet.toclient;

import com.hbm.client.ClientPackets;
import com.hbm.main.MainRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-sync payload for the mk5 ray-based nuke explosion's batched block removal (see
 * {@code com.hbm.explosion.ExplosionNukeRayBatched}/its {@code NukeChunkBlockRemoval} helper).
 * <p>
 * Deliberately a <b>separate</b> payload type from the sibling {@code explosion_vanillant_core}
 * package's {@code ExplosionRemovalSyncPacket}, even though the two are functionally identical in
 * shape (one packet per touched chunk, carrying every position set to air), rather than reusing
 * that class directly: {@code ExplosionRemovalSyncPacket} is `final`/package-private support code
 * owned by a concurrently-developed sibling package in this same wave, and this port's
 * orchestrator applies every agent's {@code HbmNetwork} wiring snippet serially after the wave -
 * two agents independently proposing the same {@code registrar.playToClient(...)} line for the
 * same {@code Type} risks a duplicate registration (NeoForge's payload registrar rejects
 * registering the same {@link CustomPacketPayload.Type} twice). A second, distinctly-named
 * payload with its own id costs one extra small class and guarantees no such collision; a later
 * cleanup pass is free to consolidate the two once both packages have landed, if desired.
 */
public record NukeExplosionRemovalSyncPacket(List<BlockPos> positions) implements CustomPacketPayload {

    public static final Type<NukeExplosionRemovalSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "nuke_explosion_removal_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NukeExplosionRemovalSyncPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public NukeExplosionRemovalSyncPacket decode(RegistryFriendlyByteBuf buf) {
            int count = buf.readVarInt();
            List<BlockPos> positions = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                positions.add(buf.readBlockPos());
            }
            return new NukeExplosionRemovalSyncPacket(positions);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, NukeExplosionRemovalSyncPacket packet) {
            buf.writeVarInt(packet.positions.size());
            for (BlockPos pos : packet.positions) {
                buf.writeBlockPos(pos);
            }
        }
    };

    public static void handleClient(NukeExplosionRemovalSyncPacket packet, IPayloadContext context) {
        ClientPackets.nukeExplosionRemoval(packet, context);
    }

    @Override
    public Type<NukeExplosionRemovalSyncPacket> type() {
        return TYPE;
    }
}
