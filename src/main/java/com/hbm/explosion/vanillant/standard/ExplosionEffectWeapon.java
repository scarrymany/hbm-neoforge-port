package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.IExplosionSFX;
import net.minecraft.world.level.Level;

/**
 * CE: {@code ExplosionEffectWeapon} - a configurable smoke-cloud SFX used by several grenade/warhead
 * weapons, driven entirely by {@code com.hbm.particle.helper.ExplosionSmallCreator.composeEffect}.
 * <p>
 * That helper does not exist in this port yet (confirmed - it is CE's client particle-cloud composer,
 * Phase 5 client/rendering scope; Neo Edition's own parallel port references the same not-yet-built
 * class). Left as a documented forward reference rather than invented - this class's constructor
 * parameters ({@code cloudCount}/{@code cloudScale}/{@code cloudSpeedMult}) are preserved so weapon
 * definitions that configure this SFX can be ported now without waiting on the particle system.
 */
public class ExplosionEffectWeapon implements IExplosionSFX {

    int cloudCount;
    float cloudScale;
    float cloudSpeedMult;

    public ExplosionEffectWeapon(int cloudCount, float cloudScale, float cloudSpeedMult) {
        this.cloudCount = cloudCount;
        this.cloudScale = cloudScale;
        this.cloudSpeedMult = cloudSpeedMult;
    }

    @Override
    public void doEffect(ExplosionVNT explosion, Level level, double x, double y, double z, float size) {
        if (level.isClientSide) return;

        // forward reference: com.hbm.particle.helper.ExplosionSmallCreator.composeEffect(level, x, y, z,
        // cloudCount, cloudScale, cloudSpeedMult) - Phase 5 client/rendering scope, not created this wave.
    }
}
