package com.hbm.particle;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Generic NBT-blob-carrying {@link ParticleOptions} implementation - the modern per-spawn-data
 * vehicle for the small subset of {@link ModParticleTypes}'s custom entries whose CE original needs
 * real constructor arguments beyond "spawn here" (color, velocity, scale, ...), instead of a bespoke
 * options record per particle. See {@link ModParticleTypes}'s class javadoc for the full registry
 * table and exactly which entries use this (currently {@code PLASMA_BLAST} and {@code GAS_FLAME}).
 * <p>
 * CE 1.12.2 has no {@code ParticleOptions}/{@code ParticleType} concept at all - that is a
 * Minecraft-1.13+ addition ({@code docs/phase5/custom_particle_types_registry.md}'s "CE's real
 * particle catalog" section confirms every one of CE's 87 particle classes is spawned by direct Java
 * construction, dispatched by a hand-rolled enum, never a Forge registry). CE's real equivalent of
 * "per-spawn data" is the arbitrary {@code NBTTagCompound} every {@code HbmEffectNT} handler lambda
 * reads keyed values out of - e.g. {@code upstream/hbm-ce/.../particle/helper/HbmEffectNT.java:449-454}
 * (the {@code PlasmaBlast} handler) reads {@code data.getFloat("r"/"g"/"b"/"pitch"/"yaw")}, and
 * {@code HbmEffectNT.java:466-473} (the {@code GasFlame} handler) reads
 * {@code data.getDouble("mX"/"mY"/"mZ")}/{@code data.getFloat("scale")}. This class is the direct,
 * deliberately un-typed 1.21.1 analogue of that same NBT-keyed convention (each {@link #tag}'s key
 * names are chosen to match CE's own key names verbatim, see the two {@code Particle*.Provider}
 * classes under {@code com.hbm.client.particle} that read them), not an invented mechanism.
 * <p>
 * Confirmed as a real, compiling {@code MapCodec}/{@code StreamCodec} API shape (not merely a design
 * idea) by cross-checking Neo Edition's independently-written but structurally identical
 * {@code com.hbm.particle.vanilla.NbtParticleOptions} - used strictly for that shape, per this
 * project's standing rule that Neo Edition is never a source of behavior/content; the class name,
 * field layout, and every call site here are this port's own.
 */
public final class HbmParticleOptions implements ParticleOptions {

    private final ParticleType<HbmParticleOptions> type;
    public final CompoundTag tag;

    public HbmParticleOptions(ParticleType<HbmParticleOptions> type, CompoundTag tag) {
        this.type = type;
        this.tag = tag;
    }

    @Override
    public ParticleType<HbmParticleOptions> getType() {
        return type;
    }

    public static MapCodec<HbmParticleOptions> codec(ParticleType<HbmParticleOptions> type) {
        return CompoundTag.CODEC.xmap(tag -> new HbmParticleOptions(type, tag), options -> options.tag).fieldOf("tag");
    }

    public static StreamCodec<? super RegistryFriendlyByteBuf, HbmParticleOptions> streamCodec(ParticleType<HbmParticleOptions> type) {
        return ByteBufCodecs.COMPOUND_TAG.map(tag -> new HbmParticleOptions(type, tag), options -> options.tag);
    }
}
