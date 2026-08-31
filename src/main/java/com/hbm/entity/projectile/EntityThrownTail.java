package com.hbm.entity.projectile;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Thin CE leftover projectile: hurt-on-hit + discard. Used for remaining {@code @AutoRegister}
 * tails that have no dedicated port yet (beams, shells, debris, legacy bullets).
 */
public class EntityThrownTail extends EntityThrowableNT {

    public float damage = 10.0F;

    public EntityThrownTail(EntityType<? extends EntityThrownTail> type, Level level) {
        super(type, level);
    }

    public EntityThrownTail(EntityType<? extends EntityThrownTail> type, Level level, LivingEntity thrower) {
        super(type, level, thrower);
    }

    @Override
    protected void onImpact(HitResult result) {
        if (this.level().isClientSide) {
            return;
        }
        if (result instanceof EntityHitResult ehr) {
            ehr.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), damage);
        }
        this.discard();
    }
}
