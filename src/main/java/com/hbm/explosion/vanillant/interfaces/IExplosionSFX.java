package com.hbm.explosion.vanillant.interfaces;

import com.hbm.explosion.vanillant.ExplosionVNT;
import net.minecraft.world.level.Level;

/**
 * CE: {@code IExplosionSFX}. Sound/particle/client-effect callback; {@link ExplosionVNT} calls every
 * registered instance in order after block/entity processing completes. {@code World} -&gt;
 * {@link Level}, otherwise identical to CE's shape.
 */
public interface IExplosionSFX {

    void doEffect(ExplosionVNT explosion, Level level, double x, double y, double z, float size);
}
