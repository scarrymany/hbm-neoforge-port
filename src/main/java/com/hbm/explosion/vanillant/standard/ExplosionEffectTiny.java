package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.IExplosionSFX;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.particle.HbmEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/**
 * CE: {@code ExplosionEffectTiny} - a quieter, smaller-scale SFX set (used by small ordnance rather
 * than full warheads): one dedicated "tiny explosion" sound plus a compact particle-cloud packet.
 * <p>
 * The sound half is ported directly ({@code HBMSoundHandler.explosion_tiny} is a confirmed real,
 * already-registered {@code DeferredHolder<SoundEvent, SoundEvent>} in this port, usable directly as
 * the {@code Holder<SoundEvent>} modern {@code Level#playSound} expects - the same pattern this port's
 * own {@code HazardTypeUnstable} already uses). The particle half (CE's networked
 * {@code AuxParticlePacketNT(HbmEffectNT.VanillaExt_LargeExplode, ...)}) is now wired via
 * {@link com.hbm.particle.HbmEffect#VANILLA_EXT_LARGE_EXPLODE}, matching CE's own size/count/radius
 * values 1:1 ({@code upstream/hbm-ce/.../ExplosionEffectTiny.java}).
 */
public class ExplosionEffectTiny implements IExplosionSFX {

    @Override
    public void doEffect(ExplosionVNT explosion, Level level, double x, double y, double z, float size) {
        if (level.isClientSide) return;

        level.playSound(null, x, y, z, HBMSoundHandler.explosion_tiny, SoundSource.BLOCKS, 15.0F, 1.0F);

        CompoundTag data = new CompoundTag();
        data.putFloat("size", 1.5F);
        data.putInt("count", 1);
        HbmEffect.sendPacket(level, HbmEffect.VANILLA_EXT_LARGE_EXPLODE, x, y, z, 100, data);
    }
}
