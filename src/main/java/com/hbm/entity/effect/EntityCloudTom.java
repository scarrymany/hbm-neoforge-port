package com.hbm.entity.effect;

import com.hbm.entity.logic.SatellitePayloadEntityTypes;
import com.hbm.interfaces.IConstantRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;

/**
 * Ported from CE's {@code com.hbm.entity.effect.EntityCloudTom} (72 lines, read in full) -
 * {@code EntityTom}'s cosmetic mushroom-cloud companion, spawned alongside {@code EntityTomBlast}.
 * CE has two constructors with two different sizes ({@code (World)}: 1x4; {@code (World, int
 * maxAge)}: 20x40) - only the second is ever actually called by any real CE spawn site
 * ({@code EntityTom.onUpdate()}'s own spawn), so - matching this port's own sibling
 * {@code EntityCloudFleija}'s identical precedent for the identical situation - only the real, used
 * size is registered (see {@code SatellitePayloadEntityTypes}) and the unused no-arg constructor is
 * not ported as a second overload.
 * <p>
 * CE's {@code world.setLastLightningBolt(2)} (a pure ambient "flash the sky" call, no real bolt
 * entity) has no 1.21.1 {@link Level} equivalent - substituted with a real, visual-only
 * {@link LightningBolt} entity high above the cloud, matching this port's own sibling
 * {@code EntityCloudFleija}/{@code EntityCloudSolinium} precedent for the exact same CE call.
 */
public class EntityCloudTom extends Entity implements IConstantRenderer {

    public int maxAge = 100;
    public int age;

    public EntityCloudTom(EntityType<? extends EntityCloudTom> entityType, Level level) {
        super(entityType, level);
    }

    /** CE: {@code new EntityCloudTom(world, maxAge)} - the only constructor any real CE call site uses. */
    public EntityCloudTom(Level level, int maxAge) {
        this(SatellitePayloadEntityTypes.CLOUD_TOM.get(), level);
        this.maxAge = maxAge;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // Plain field, not synced - matching EntityCloudFleija's own precedent (client rendering of
        // this cosmetic entity is Phase 5 scope; no consumer needs maxAge client-side yet).
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
            this.discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.age = tag.getShort("age");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putShort("age", (short) age);
    }
}
