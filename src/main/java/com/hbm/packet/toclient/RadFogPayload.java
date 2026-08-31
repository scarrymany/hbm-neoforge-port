package com.hbm.packet.toclient;

import com.hbm.main.MainRegistry;
import com.hbm.particle.ModParticleTypes;
import net.minecraft.client.Minecraft;
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
 * <b>Real {@code RadFog} particle now wired</b> (Phase 5 {@code c14-custom-particle-content} task):
 * this handler originally spawned a vanilla {@link net.minecraft.core.particles.ParticleTypes#CLOUD}
 * stand-in, documented at the time as waiting on "whichever future pass lands a real custom
 * particle-type registry" - that registry ({@code com.hbm.particle.ModParticleTypes}, the sibling
 * {@code f4-particle-registry-and-events} task) and this type's real render
 * ({@code com.hbm.client.particle.RadiationFogParticle}, CE's own {@code ParticleRadiationFog}
 * transcribed in full - see that class's javadoc) both now exist, so this handler spawns
 * {@link ModParticleTypes#RADIATION_FOG} directly instead.
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

            // CE: HbmEffectNT.RadFog spawns exactly one ParticleRadiationFog at the broadcast point
            // (HbmEffectNT.java:141-148) - that single instance's own 25-quad static offset cluster
            // (see RadiationFogParticle's class javadoc) is what produces the sprawling haze look, not
            // a manual multi-spawn burst at this call site.
            level.addParticle(ModParticleTypes.RADIATION_FOG.get(), packet.x, packet.y, packet.z, 0.0D, 0.0D, 0.0D);
        });
    }

    @Override
    public Type<RadFogPayload> type() {
        return TYPE;
    }
}
