package com.hbm.explosion.vanillant.interfaces;

import com.hbm.explosion.vanillant.ExplosionVNT;
import net.minecraft.world.entity.Entity;

/**
 * CE: {@code ICustomDamageHandler}. Extra per-entity side effect applied by {@code EntityProcessorCross}/
 * {@code EntityProcessorStandard} on top of ordinary blast damage (e.g. radiation from an antimatter
 * blast). Signature identical to CE's.
 */
public interface ICustomDamageHandler {

    void handleAttack(ExplosionVNT explosion, Entity entity, double distanceScaled);
}
