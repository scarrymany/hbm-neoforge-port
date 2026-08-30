package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.IExplosionSFX;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/**
 * CE: {@code ExplosionEffectTiny} - a quieter, smaller-scale SFX set (used by small ordnance rather
 * than full warheads): one dedicated "tiny explosion" sound plus a compact particle-cloud packet.
 * <p>
 * The sound half is ported directly ({@code HBMSoundHandler.explosion_tiny} is a confirmed real,
 * already-registered {@code DeferredHolder<SoundEvent, SoundEvent>} in this port, usable directly as
 * the {@code Holder<SoundEvent>} modern {@code Level#playSound} expects - the same pattern this port's
 * own {@code HazardTypeUnstable} already uses). The particle half
 * ({@code com.hbm.packet.toclient.AuxParticlePacketNT} / {@code com.hbm.particle.helper.HbmEffectNT} /
 * {@code com.hbm.handler.threading.PacketThreading}) is CE's generic networked particle-effect
 * framework - out of this package's scope (confirmed not to exist anywhere in this port yet; belongs
 * to Phase 5's client/rendering work) - and is left as a documented forward reference rather than
 * invented.
 */
public class ExplosionEffectTiny implements IExplosionSFX {

    @Override
    public void doEffect(ExplosionVNT explosion, Level level, double x, double y, double z, float size) {
        if (level.isClientSide) return;

        level.playSound(null, x, y, z, HBMSoundHandler.explosion_tiny, SoundSource.BLOCKS, 15.0F, 1.0F);

        // forward reference: com.hbm.particle.helper.HbmEffectNT.VanillaExt_LargeExplode via
        // com.hbm.packet.toclient.AuxParticlePacketNT / com.hbm.handler.threading.PacketThreading -
        // CE's generic networked particle-effect framework, Phase 5 scope, not created this wave.
    }
}
