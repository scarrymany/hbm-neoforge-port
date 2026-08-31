package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.IExplosionSFX;
import com.hbm.particle.HbmEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

/**
 * CE: {@code ExplosionEffectAmat} - the antimatter detonation's dedicated visual effect, driven
 * entirely by a networked {@code HbmEffectNT.AmatExplosion} particle packet carrying just the blast
 * scale (no per-block position list, no server-side sound call - unlike {@link ExplosionEffectStandard}
 * this class's CE original doesn't even gate on {@code world.isRemote}, since the whole effect is a
 * single broadcast-and-forget packet).
 * <p>
 * Now wired via {@link com.hbm.particle.HbmEffect#AMAT_EXPLOSION}, radius 200, matching CE's own
 * scale-carrying single-field payload and lack of an {@code isRemote} gate 1:1 (see
 * {@link ExplosionEffectTiny}'s javadoc for the shared dispatch mechanism).
 */
public class ExplosionEffectAmat implements IExplosionSFX {

    @Override
    public void doEffect(ExplosionVNT explosion, Level level, double x, double y, double z, float size) {
        CompoundTag data = new CompoundTag();
        data.putFloat("scale", size);
        HbmEffect.sendPacket(level, HbmEffect.AMAT_EXPLOSION, x, y, z, 200, data);
    }
}
