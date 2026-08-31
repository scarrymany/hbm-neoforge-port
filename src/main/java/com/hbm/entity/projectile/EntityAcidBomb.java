package com.hbm.entity.projectile;

import com.hbm.damage.ModDamageTypes;
import com.hbm.entity.mob.glyphid.EntityGlyphid;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * CE {@code com.hbm.entity.projectile.EntityAcidBomb} (60 lines) —
 * {@code @AutoRegister(name = "entity_acid_bomb", trackingRange = 1000)}.
 * Glyphid hits are ignored (CE: {@code !(result.entityHit instanceof EntityGlyphid)}).
 */
public class EntityAcidBomb extends EntityThrowableNT {

    public float damage = 1.5F;

    public EntityAcidBomb(EntityType<? extends EntityAcidBomb> type, Level level) {
        super(type, level);
    }

    public EntityAcidBomb(Level level, double x, double y, double z) {
        super(Phase9TailEntityTypes.ACID_BOMB.get(), level, x, y, z);
    }

    public EntityAcidBomb(Level level, LivingEntity thrower) {
        super(Phase9TailEntityTypes.ACID_BOMB.get(), level, thrower);
    }

    @Override
    public double getGravityVelocity() {
        return 0.04D;
    }

    @Override
    protected float getAirDrag() {
        return 1.0F;
    }

    @Override
    protected void onImpact(HitResult result) {
        if (this.level().isClientSide) {
            return;
        }
        if (result instanceof EntityHitResult ehr) {
            if (!(ehr.getEntity() instanceof EntityGlyphid)) {
                ehr.getEntity().hurt(this.damageSources().source(ModDamageTypes.ACID, this, this.getOwner()), damage);
                this.discard();
            }
            return;
        }
        if (result instanceof BlockHitResult) {
            this.discard();
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("damage", damage);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.damage = tag.getFloat("damage");
    }
}
