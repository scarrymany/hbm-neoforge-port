package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.IExplosionSFX;
import net.minecraft.world.level.Level;

/**
 * CE: {@code ExplosionEffectAmat} - the antimatter detonation's dedicated visual effect, driven
 * entirely by a networked {@code HbmEffectNT.AmatExplosion} particle packet carrying just the blast
 * scale (no per-block position list, no server-side sound call - unlike {@link ExplosionEffectStandard}
 * this class's CE original doesn't even gate on {@code world.isRemote}, since the whole effect is a
 * single broadcast-and-forget packet).
 * <p>
 * {@code com.hbm.particle.helper.HbmEffectNT} / {@code com.hbm.packet.toclient.AuxParticlePacketNT} /
 * {@code com.hbm.handler.threading.PacketThreading} do not exist in this port yet (see
 * {@link ExplosionEffectTiny}'s javadoc - the same generic networked particle-effect framework, Phase
 * 5 scope). Left as a documented forward reference.
 */
public class ExplosionEffectAmat implements IExplosionSFX {

    @Override
    public void doEffect(ExplosionVNT explosion, Level level, double x, double y, double z, float size) {
        // forward reference: com.hbm.particle.helper.HbmEffectNT.AmatExplosion via
        // com.hbm.packet.toclient.AuxParticlePacketNT / com.hbm.handler.threading.PacketThreading -
        // CE's generic networked particle-effect framework, Phase 5 scope, not created this wave.
    }
}
