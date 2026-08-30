package com.hbm.entity.effect;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;

/**
 * Ported from CE's {@code com.hbm.entity.effect.EntityCloudFleija} (96 lines, read in full) - the
 * companion cloud VFX spawned alongside {@code NukeFleija}/{@code NukePrototype}/{@code
 * NukeCustom}'s antimatter-tier detonations (same role as {@link EntityNukeTorex} for the
 * mk3/Fleija family). CE's own logic (every tick: age up, spawn a visual-only lightning bolt far
 * above the cloud, despawn at {@code maxAge}) is not purely client-rendering, so it's ported in
 * full rather than stubbed - only the {@code isWarDim} gate is dropped, per this port's documented
 * always-true default.
 * <p>
 * CE's no-arg/size-{@code (1,4)} constructor is never actually called anywhere in CE (every real
 * spawn site uses the {@code (World, int maxAge)}/size-{@code (20,40)} constructor - confirmed by
 * grep), so this port registers only the size actually used and exposes {@link #setMaxAge} as a
 * plain post-construction setter instead of a second constructor overload.
 */
public class EntityCloudFleija extends Entity {

    public int maxAge = 100;
    public int age;
    public float scale = 0;

    public EntityCloudFleija(EntityType<? extends EntityCloudFleija> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        this.age++;

        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level());
        if (bolt != null) {
            bolt.setVisualOnly(true);
            bolt.moveTo(getX(), getY() + 200, getZ());
            level().addFreshEntity(bolt);
        }

        if (this.age >= this.maxAge) {
            this.age = 0;
            this.discard();
        }

        this.scale++;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.age = tag.getShort("age");
        this.scale = tag.getShort("scale");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putShort("age", (short) age);
        tag.putShort("scale", (short) scale);
    }

    public void setMaxAge(int maxAge) {
        this.maxAge = maxAge;
    }

    public int getMaxAge() {
        return this.maxAge;
    }

    /**
     * Convenience factory mirroring CE's actually-used {@code new EntityCloudFleija(world, maxAge)}
     * two-argument constructor, for whichever pass ports the {@code NukePrototype}/{@code
     * NukeFleija}/{@code NukeCustom}/{@code ItemCell}/{@code ItemGrenadeFilling} call sites that
     * used it (none of which exist in this port yet). Does not spawn the entity - matches CE's own
     * constructor-only shape, leaving {@code level.addFreshEntity(...)} to the caller.
     */
    public static EntityCloudFleija create(Level level, double x, double y, double z, int maxAge) {
        EntityCloudFleija cloud = new EntityCloudFleija(EffectEntityTypes.CLOUD_FLEIJA.get(), level);
        cloud.setMaxAge(maxAge);
        cloud.setPos(x, y, z);
        return cloud;
    }
}
