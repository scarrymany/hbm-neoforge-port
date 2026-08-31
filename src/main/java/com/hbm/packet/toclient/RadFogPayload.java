package com.hbm.packet.toclient;

import com.hbm.main.MainRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The one real custom network payload {@code docs/phase4/chunk_radiation_system.md}'s survey found
 * anywhere in CE's whole chunk-radiation system: a decorative "there is dangerous radiation here"
 * particle cue, sent when a pocket's density exceeds {@code RadiationConfig.FOG_THRESHOLD}
 * ({@code RadiationSystemNT.WorldRadiationData.spawnFog} in CE, broadcasting a {@code RadFog}
 * {@code AuxParticlePacketNT} via {@code PacketThreading.createAllAroundThreadedPacket}). CE's payload
 * carries no numeric radiation data at all - just "spawn the named particle effect here" - so this
 * port's version is likewise just a bare position.
 * <p>
 * Built as its own narrow, single-purpose payload rather than waiting on CE's generic
 * {@code AuxParticlePacketNT}/{@code HbmEffectNT} named-particle-broadcast system, which is confirmed
 * absent anywhere in this port (see {@code GrenadeFillingActions}'s own javadoc) - exactly the
 * documented fallback this area's research report calls out. Follows this port's own already-real
 * {@link ExplosionEffectSyncPacket}/{@code ExplosionEffectStandard}
 * {@code CustomPacketPayload}/{@code PacketDistributor.sendToPlayersNear} pattern verbatim.
 * <p>
 * <b>Visual substitution, documented not silently invented</b>: CE's real {@code RadFog} particle
 * (a custom haze-puff sprite, {@code HbmEffectNT.RadFog}) has no 1.21 equivalent registered anywhere
 * in this port yet (no custom particle-type registry exists at all in this pass - confirmed by
 * repo-wide search). This client handler spawns a small burst of vanilla {@link ParticleTypes#CLOUD}
 * instead as a stand-in visual cue, matching this port's own precedent elsewhere (Phase 3's
 * {@code XFactoryEnergy}/{@code ExplosionNukeSmall} leave "TODO(phase5-particles)" markers for
 * identical missing-custom-particle gaps) - whichever future pass lands a real custom particle-type
 * registry should swap this stand-in for the genuine {@code RadFog} sprite.
 */
public record RadFogPayload(double x, double y, double z) implements CustomPacketPayload {

    public static final Type<RadFogPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "rad_fog"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RadFogPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public RadFogPayload decode(RegistryFriendlyByteBuf buf) {
            return new RadFogPayload(buf.readDouble(), buf.readDouble(), buf.readDouble());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, RadFogPayload packet) {
            buf.writeDouble(packet.x);
            buf.writeDouble(packet.y);
            buf.writeDouble(packet.z);
        }
    };

    @OnlyIn(Dist.CLIENT)
    public static void handleClient(RadFogPayload packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Level level = Minecraft.getInstance().level;
            if (level == null) return;

            for (int i = 0; i < 6; i++) {
                double ox = packet.x + (level.random.nextDouble() - 0.5D) * 2.0D;
                double oy = packet.y + (level.random.nextDouble() - 0.5D) * 1.0D;
                double oz = packet.z + (level.random.nextDouble() - 0.5D) * 2.0D;
                level.addParticle(ParticleTypes.CLOUD, ox, oy, oz, 0.0D, 0.01D, 0.0D);
            }
        });
    }

    @Override
    public Type<RadFogPayload> type() {
        return TYPE;
    }
}
