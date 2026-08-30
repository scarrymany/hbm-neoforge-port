package com.hbm.entity.effect;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Ported from CE's {@code com.hbm.entity.effect.EntityEMPBlast} (89 lines, read in full) - the
 * spherical EMP shockwave VFX entity spawned by {@code BombFloat}'s {@code emp_bomb} variant (and
 * several gun/discharge weapons outside this pass's scope). CE's own {@code onUpdate} is pure
 * age/despawn bookkeeping with zero world-mutation (the actual EMP block-mutation effect is
 * {@link com.hbm.explosion.ExplosionNukeGeneric#empBlast}, called by the spawning bomb/weapon
 * separately, not by this entity) - fully ported as-is, nothing to stub.
 */
public class EntityEMPBlast extends Entity {

    private static final EntityDataAccessor<Integer> MAXAGE = SynchedEntityData.defineId(EntityEMPBlast.class, EntityDataSerializers.INT);

    public int age;
    public float scale = 0;

    public EntityEMPBlast(EntityType<? extends EntityEMPBlast> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(MAXAGE, 0);
    }

    @Override
    public void tick() {
        this.age++;

        if (this.age >= this.getMaxAge()) {
            this.age = 0;
            this.discard();
        }

        this.scale++;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.scale = tag.getFloat("scale");
        this.age = tag.getInt("age");
        this.setMaxAge(tag.getInt("maxage"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("scale", scale);
        tag.putInt("age", age);
        tag.putInt("maxage", getMaxAge());
    }

    public void setMaxAge(int i) {
        this.entityData.set(MAXAGE, i);
    }

    public int getMaxAge() {
        return this.entityData.get(MAXAGE);
    }

    /**
     * Convenience factory mirroring CE's {@code new EntityEMPBlast(world, maxAge)} two-argument
     * constructor (every real CE call site uses it - {@code BombFloat}, {@code EntityMissileTier0},
     * {@code EntityBulletBaseNT}/{@code EntityDischarge}/{@code EntityBulletBase}, none of which
     * exist in this port yet). Does not spawn the entity - matches CE's constructor-only shape.
     */
    public static EntityEMPBlast create(Level level, double x, double y, double z, int maxAge) {
        EntityEMPBlast blast = new EntityEMPBlast(EffectEntityTypes.EMP_BLAST.get(), level);
        blast.setMaxAge(maxAge);
        blast.setPos(x, y, z);
        return blast;
    }
}
