package com.hbm.packet.toclient;

import com.hbm.main.MainRegistry;
import com.hbm.particle.HbmEffect;
import com.hbm.util.EnumUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Modern replacement for CE's {@code com.hbm.packet.toclient.AuxParticlePacketNT} (112 lines) - the
 * single generic "spawn this named VFX at this point" S2C payload {@code docs/phase5/
 * particle_engine_and_generic_vfx.md}'s "Recommended architecture" point 1 designs, carrying the
 * modern triple {@code {HbmEffect effect, x, y, z, CompoundTag data}} CE's real (non-deprecated)
 * {@code AuxParticlePacketNT(HbmEffectNT, NBTTagCompound, x, y, z)} 5-arg constructor uses - CE's
 * legacy 4-arg string-keyed constructor/{@code EffectNTLegacyAdapter} companion is deliberately not
 * ported (that report's Finding 4: no CE 1.12.2 save data exists in this from-scratch port to stay
 * backward-compatible with).
 * <p>
 * Follows this port's own already-real {@code CustomPacketPayload}/{@code StreamCodec} template
 * verbatim - same shape as {@link RadFogPayload}/{@link ExplosionEffectSyncPacket}. The
 * {@link HbmEffect} enum field uses a var-int-ordinal-plus-bounds-checked-lookup codec, copying
 * {@code com.hbm.packet.toserver.KeybindPacket.java:41-48}'s already-real pattern verbatim (this
 * report's Key risk #2) rather than CE's Forge-only {@code PacketBuffer.writeEnumValue}/
 * {@code readEnumValue} or a raw unchecked {@code values()[ordinal]}.
 */
public record HbmEffectPacket(HbmEffect effect, double x, double y, double z, CompoundTag data) implements CustomPacketPayload {

    public static final Type<HbmEffectPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "hbm_effect"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HbmEffectPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(
                    ordinal -> EnumUtil.grabEnumSafely(HbmEffect.VALUES, ordinal),
                    HbmEffect::ordinal),
            HbmEffectPacket::effect,
            ByteBufCodecs.DOUBLE, HbmEffectPacket::x,
            ByteBufCodecs.DOUBLE, HbmEffectPacket::y,
            ByteBufCodecs.DOUBLE, HbmEffectPacket::z,
            ByteBufCodecs.COMPOUND_TAG, HbmEffectPacket::data,
            HbmEffectPacket::new
    );

    @OnlyIn(Dist.CLIENT)
    public static void handleClient(HbmEffectPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) return;
            packet.effect().summonParticle(level, packet.x(), packet.y(), packet.z(), packet.data());
        });
    }

    @Override
    public Type<HbmEffectPacket> type() {
        return TYPE;
    }
}
