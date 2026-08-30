package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.ICustomDamageHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * CE: {@code CustomDamageHandlerAmat} - antimatter explosions additionally contaminate every hit
 * living entity with radiation, scaled by distance and blast size, via
 * {@code ContaminationUtil.contaminate(entity, HazardType.RADIATION, ContaminationType.CREATIVE, rad)}.
 * <p>
 * {@code com.hbm.util.ContaminationUtil} does not exist in this port yet (confirmed - it belongs to
 * Phase 4's world/simulation/fallout system, which this package's own research report explicitly
 * scoped out; Neo Edition's own parallel port assumes the same not-yet-built class). Left as a
 * documented forward-reference no-op rather than substituted with a simplified stand-in (e.g. this
 * port's {@code HbmLivingAttachment.increaseRads(double)}), since a direct substitute would silently
 * skip CE's real hazmat/armor resistance mitigation layer that {@code ContaminationUtil.contaminate}
 * applies before the rad value ever reaches the player - a real behavior gap that should be a stated
 * Phase 4 decision, not an accidental default made here.
 */
public class CustomDamageHandlerAmat implements ICustomDamageHandler {

    protected float radiation;

    public CustomDamageHandlerAmat(float radiation) {
        this.radiation = radiation;
    }

    @Override
    public void handleAttack(ExplosionVNT explosion, Entity entity, double distanceScaled) {
        if (entity instanceof LivingEntity) {
            // forward reference: com.hbm.util.ContaminationUtil.contaminate(LivingEntity, HazardType.RADIATION,
            // ContaminationType.CREATIVE, radiation * (1 - distanceScaled) * explosion.size) - Phase 4, not
            // created this wave. See this class's javadoc for why this is left a no-op rather than a
            // simplified stand-in.
        }
    }
}
