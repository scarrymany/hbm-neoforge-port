package com.hbm.entity.effect;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;

/**
 * Ported from CE's {@code com.hbm.entity.effect.EntityCloudSolinium} (97 lines, read in full) - the
 * companion cloud VFX for {@code NukeSolinium}/{@code NukeCustom}'s solinium-tier detonations.
 * Structurally identical to {@link EntityCloudFleija} - see that class's javadoc for the same
 * "not purely client-rendering, ported in full" and "only the actually-used constructor overload
 * is registered" notes, which apply here unchanged.
 */
public class EntityCloudSolinium extends Entity {

    public int maxAge = 100;
    public int age;
    public float scale = 0;

    public EntityCloudSolinium(EntityType<? extends EntityCloudSolinium> entityType, Level level) {
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
     * Convenience factory mirroring CE's actually-used {@code new EntityCloudSolinium(world,
     * maxAge)} two-argument constructor - see {@link EntityCloudFleija#create}'s javadoc for why.
     */
    public static EntityCloudSolinium create(Level level, double x, double y, double z, int maxAge) {
        EntityCloudSolinium cloud = new EntityCloudSolinium(EffectEntityTypes.CLOUD_SOLINIUM.get(), level);
        cloud.setMaxAge(maxAge);
        cloud.setPos(x, y, z);
        return cloud;
    }
}
