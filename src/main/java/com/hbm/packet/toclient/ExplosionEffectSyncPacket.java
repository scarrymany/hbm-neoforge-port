package com.hbm.packet.toclient;

import com.hbm.explosion.vanillant.standard.ExplosionEffectStandard;
import com.hbm.main.MainRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * CE: the collaborator packet {@code ExplosionEffectStandard} sends alongside its sound
 * ({@code ExplosionVanillaNewTechnologyCompressedAffectedBlockPositionDataForClientEffectsAndParticle
 * HandlingPacket} in CE - worth a smile, not a design decision) - carries the explosion center/size
 * plus every affected block position so the client can spawn the same per-block particle spray CE
 * does. See {@link com.hbm.packet.toclient.ExplosionRemovalSyncPacket}'s javadoc for why this stays a
 * separate payload from the actual block-removal sync despite both traveling over similar position
 * lists.
 */
public record ExplosionEffectSyncPacket(double x, double y, double z, float size, List<BlockPos> affectedBlocks) implements CustomPacketPayload {

    public static final Type<ExplosionEffectSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "explosion_effect_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ExplosionEffectSyncPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ExplosionEffectSyncPacket decode(RegistryFriendlyByteBuf buf) {
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            float size = buf.readFloat();
            int count = buf.readVarInt();
            List<BlockPos> positions = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                positions.add(buf.readBlockPos());
            }
            return new ExplosionEffectSyncPacket(x, y, z, size, positions);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, ExplosionEffectSyncPacket packet) {
            buf.writeDouble(packet.x);
            buf.writeDouble(packet.y);
            buf.writeDouble(packet.z);
            buf.writeFloat(packet.size);
            buf.writeVarInt(packet.affectedBlocks.size());
            for (BlockPos pos : packet.affectedBlocks) {
                buf.writeBlockPos(pos);
            }
        }
    };

    @OnlyIn(Dist.CLIENT)
    public static void handleClient(ExplosionEffectSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().level == null) return;
            ExplosionEffectStandard.performClient(Minecraft.getInstance().level, packet.x, packet.y, packet.z, packet.size, packet.affectedBlocks);
        });
    }

    @Override
    public Type<ExplosionEffectSyncPacket> type() {
        return TYPE;
    }
}
