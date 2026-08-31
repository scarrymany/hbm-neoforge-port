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
 * New payload (this package has no CE equivalent - CE's {@code BlockProcessorStandard} ran entirely
 * server-authoritatively via per-block {@code World#setBlockToAir}/{@code onBlockExploded}, which
 * always kept 1.12 clients in sync for free as a side effect of that same call).
 * <p>
 * This port's {@code BlockProcessorStandard} instead removes affected blocks via a batched, direct
 * {@code LevelChunkSection} write per touched chunk (see {@code ChunkBatchedBlockRemoval}) - a
 * deliberate bypass of {@code Level#setBlock}'s normal per-block client-sync bookkeeping, done
 * specifically to avoid its cost (see the class javadoc on {@code ChunkBatchedBlockRemoval} for why).
 * Because that bypass means vanilla's own per-tick dirty-chunk-section broadcast never learns about
 * the change, this payload is the explicit replacement: one packet per touched chunk, carrying every
 * position in that chunk that was set to air, so clients apply the same removal locally.
 * <p>
 * Deliberately separate from {@code ExplosionEffectSyncPacket} (the {@code IExplosionSFX} role's own
 * particle-broadcast payload) even though both eventually travel over similar affected-position lists
 * - {@link com.hbm.explosion.vanillant.interfaces.IBlockProcessor} and
 * {@link com.hbm.explosion.vanillant.interfaces.IExplosionSFX} are independently pluggable roles in
 * {@code ExplosionVNT} and must stay decoupled (an explosion can run one without the other).
 */
public record ExplosionRemovalSyncPacket(List<BlockPos> positions) implements CustomPacketPayload {

    public static final Type<ExplosionRemovalSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "explosion_removal_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ExplosionRemovalSyncPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ExplosionRemovalSyncPacket decode(RegistryFriendlyByteBuf buf) {
            int count = buf.readVarInt();
            List<BlockPos> positions = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                positions.add(buf.readBlockPos());
            }
            return new ExplosionRemovalSyncPacket(positions);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, ExplosionRemovalSyncPacket packet) {
            buf.writeVarInt(packet.positions.size());
            for (BlockPos pos : packet.positions) {
                buf.writeBlockPos(pos);
            }
        }
    };

    /** Kept on both dists so {@code HbmNetwork} method-ref survives DistCleaner. Body is client-only. */
    public static void handleClient(ExplosionRemovalSyncPacket packet, IPayloadContext context) {
        ClientPackets.explosionRemoval(packet, context);
    }

    @Override
    public Type<ExplosionRemovalSyncPacket> type() {
        return TYPE;
    }
}
